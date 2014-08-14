///////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
///////////////////////////////////////////////////////////////////////////

package com.adobe

// Scala Collections.
import scala.collection.mutable.MutableList

// Joda.
import org.joda.time.{DateTime,Days,format}
import format.DateTimeFormat

// Spark.
import org.apache.spark.{SparkConf,SparkContext,SparkEnv}
import org.apache.spark.SparkContext._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.rdd.RDD

// Spray
import spray.http.{Uri,Timedout,HttpRequest,HttpResponse}
import spray.routing.HttpService

// Map Reduce.
import org.apache.hadoop.{conf,fs,mapreduce}
import fs.{FileSystem,Path}
import mapreduce.Job
import conf.Configuration

// Parquet.
import parquet.format.FileMetaData
import parquet.hadoop.{ParquetInputFormat,ParquetFileReader,Footer}
import parquet.hadoop.metadata.BlockMetaData
import parquet.hadoop.thrift.{
  ParquetThriftInputFormat,ThriftReadSupport
}

// Analytics Queries.
import Helpers._
import queries._

// Analytics Thrift objects.
import org.apache.thrift.TDeserializer
import thrift.AnalyticsData

// Guava.
import com.google.common.io.ByteStreams

// Serialization.
import com.esotericsoftware.kryo.Kryo
import org.apache.spark.serializer.KryoRegistrator
class MyRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.register(classOf[AnalyticsData])
  }
}

// Java conversions.
import scala.collection.JavaConversions._

object QueryHandler {
  var cores = 143
  var memory = "21g"
  var sc = createSparkContext(memory, cores)
  var adhocHandler = new AdhocQueryHandler(sc)
  var lastCachedQuery = ""
  var cachedData: Array[RDD[AnalyticsData]] = null
  var cachedDays: Seq[String] = null
  var cachedDailyRows: Seq[Long] = null
  val td = new TDeserializer()

  def restartSparkContext(memory: String, cores: Int): String = {
    this.memory = memory
    this.cores = cores
    if (cachedData != null) cachedData.map(_.unpersist())
    sc.stop()
    sc = createSparkContext(memory, cores)
    adhocHandler = new AdhocQueryHandler(sc)
    lastCachedQuery = ""
    cachedData = null
    cachedDays = null
    cachedDailyRows = null
    "Successful"
  }

  def stopSparkContext() = sc.stop()

  def handle(name: String, profile: Boolean, cache: Boolean,
      startYMD: String, finishYMD: String,
      targetPartitionSize: Long): String = {
    val sb = new StringBuilder()
    if (profile) sb ++= Profiling.getTime("EnterHandle")
    val queryToRun = QueryMeta.info(name)
    val (data, days, dailyRows) =
      if (cache) {
        if (lastCachedQuery == name + startYMD + finishYMD) {
          (cachedData, cachedDays, cachedDailyRows)
        } else {
          if (cachedData != null) cachedData.map(_.unpersist())
          val (newData, newDays, newDailyRows) =
            loadData(sc, queryToRun.colsNeeded, startYMD, finishYMD)
          newData.map{_.cache()}
          cachedData = newData
          cachedDays = newDays
          cachedDailyRows = newDailyRows
          lastCachedQuery = name + startYMD + finishYMD
          (newData, newDays, newDailyRows)
        }
      } else loadData(sc, queryToRun.colsNeeded, startYMD, finishYMD)
    val queryConf = QueryConf(
      sc, data, profile, days, dailyRows, targetPartitionSize
    )
    val resultStr = queryToRun.run(queryConf)
    if (profile) sb ++= Profiling.getResult(name, resultStr)
    if (profile) sb ++= Profiling.getTime("ExitHandle")
    if (profile) sb.toString else resultStr
  }

  def generateAdhocPage(start_ymd: String, finish_ymd: String): String = {
    html.adhoc(getDaysInRange(start_ymd, finish_ymd)).toString
  }

  def handleSQL(sqlQuery: String, numResults: Int,
      start_ymd: String, finish_ymd: String): String = {
    adhocHandler.handle(sqlQuery, numResults,
      getDaysInRange(start_ymd, finish_ymd))
  }

  def getDaysInRange(startYMD: String, finishYMD: String): Seq[String] = {
    val startYMDArr = startYMD.split("-").map(_.toInt)
    val finishYMDArr = finishYMD.split("-").map(_.toInt)
    val startDate = new DateTime(
      startYMDArr(0), startYMDArr(1), startYMDArr(2), 0, 0, 0, 0
    )
    val finishDate = new DateTime(
      finishYMDArr(0), finishYMDArr(1), finishYMDArr(2), 23, 59, 59, 999
    )
    getDaysInRange(startDate, finishDate)
  }

  def getDaysInRange(startDate: DateTime, finishDate: DateTime): Seq[String] = {
    val days = new MutableList[String]()
    var d = startDate
    while (!(d.getYear == finishDate.getYear &&
            d.getMonthOfYear == finishDate.getMonthOfYear &&
            d.getDayOfMonth == finishDate.getDayOfMonth)) {
      days += Seq(d.getYear, "%02d".format(d.getMonthOfYear),
        "%02d".format(d.getDayOfMonth)).mkString("-")
      d = d.plusDays(1)
    }
    days += Seq(d.getYear, "%02d".format(d.getMonthOfYear),
      "%02d".format(d.getDayOfMonth)).mkString("-")
    days.toSeq
  }

  def createSparkContext(memory: String, maxCores: Int): SparkContext = {
    val conf = new SparkConf()
      .setAppName("Spindle")
      .setMaster("spark://spark_master_address:7077")
      .setSparkHome("/usr/lib/spark")
      .setJars(Seq("/tmp/Spindle.jar"))
      .set("spark.executor.memory", memory)
      .set("spark.cores.max", maxCores.toString)
      .set("spark.kryo.registrator", "com.adobe.MyRegistrator")
      .set("spark.default.parallelism", (maxCores).toString)
    new SparkContext(conf)
  }

  val ymd_fmt = DateTimeFormat.forPattern("yyyy-MM-dd")
  def loadData(sc: SparkContext, columnNames: Seq[String],
      startYMD: String, finishYMD: String):
      (Array[RDD[AnalyticsData]], Seq[String], Seq[Long]) = {
    val startYMDArr = startYMD.split("-").map(_.toInt)
    val finishYMDArr = finishYMD.split("-").map(_.toInt)
    val startDate = new DateTime(
      startYMDArr(0), startYMDArr(1), startYMDArr(2), 0, 0, 0, 0
    )
    val finishDate = new DateTime(
      finishYMDArr(0), finishYMDArr(1), finishYMDArr(2), 23, 59, 59, 999
    )
    val job = new Job()
    ParquetInputFormat.setReadSupportClass(
      job,
      classOf[ThriftReadSupport[AnalyticsData]]
    )
    job.getConfiguration.set(
      "parquet.thrift.column.filter",
      columnNames.mkString(";")
    )
    val numDays = Days.daysBetween(startDate, finishDate).getDays+1
    if (numDays > 0) {
      val (allData,dailyRows) = Array.tabulate(numDays) { i =>
        val currentDay = startDate.plusDays(i)
        val currentDayPath = "/spindle-sample-data/" +
          ymd_fmt.print(currentDay)
        val conf = new Configuration()
        val hdfs = "hdfs://hdfs_master_address:8020/"
        val hdfsPath = hdfs + currentDayPath
        conf.set("fs.default.name", hdfs);
        val fs = FileSystem.get(conf);
        if (fs.exists(new Path(hdfsPath))) {
          val o = sc.newAPIHadoopFile(
            hdfsPath,
            classOf[ParquetThriftInputFormat[AnalyticsData]],
            classOf[Void],
            classOf[AnalyticsData],
            job.getConfiguration
          ).map{case (void,obj) => obj}
          val metadataFooters: Seq[Footer] = ParquetFileReader
            .readFooters(conf, new Path(hdfsPath))
          val rows = metadataFooters.foldLeft(0L) {
            (res: Long, footer: Footer) =>
              res + footer.getParquetMetadata.getBlocks.foldLeft(0L) {
                (inner_res: Long, block: BlockMetaData) =>
                  inner_res+block.getRowCount
            }
          }
          (o, rows)
        } else {
          (sc.parallelize(Seq[AnalyticsData]()), 0L)
        }
      }.unzip
      (allData.toArray, getDaysInRange(startDate, finishDate), dailyRows.toSeq)
    } else {
      (Array(sc.parallelize(Seq[AnalyticsData]())), Seq[String](), Seq[Long]())
    }
  }

  def loadTime(): String = {
    val cols = Seq("post_pagename", "user_agent", "visit_referrer",
      "post_visid_high", "post_visid_low", "visit_num",
      "visit_referrer", "hit_time_gmt", "post_purchaseid",
      "post_product_list", "first_hit_referrer")
    val sb = new StringBuilder()
    sb ++= Profiling.getTime("EnterHandle")
    val (testData, testDays, testDailyRows) =
      loadData(sc, cols, "2014-01-01", "2014-01-07")
    testData.map{case dayData=>
      dayData.cache().foreach(x => {})
    }
    sb ++= Profiling.getTime("ExitHandle")
    testData.map{case dayData=>
      dayData.unpersist()
    }
    sb.toString
  }

  def getStats() = {
    val colsNeeded = Seq("post_pagename", "post_visid_high", "post_visid_low",
      "visit_num", "hit_time_gmt", "visit_referrer", "post_purchaseid",
      "post_product_list", "first_hit_referrer")
    val (data, days, dailyRows) = loadData(
      sc, colsNeeded, "2014-01-01", "2014-01-07"
    )
    val sb = new StringBuilder()

    // Number of filtered pagename and referrer fields.
    sb ++= "days: " + days.mkString(", ") + "\n"
    sb ++= "dailyRows: " + dailyRows.mkString(", ") + "\n"
    val totalRows = dailyRows.reduce(_+_)
    sb ++= "totalRows: " + totalRows + "\n"
    val numFilteredPagenameRows = data.map{case dayData =>
      dayData.filter{case root => root.post_pagename.isEmpty}
        .count()
    }
    sb ++= "numFilteredPagenameRows: " +
      numFilteredPagenameRows.mkString(", ") + "\n"
    val totalFPR = numFilteredPagenameRows.reduce(_+_)
    sb ++= "totalFPR: " + totalFPR + "\n"
    val numFilteredReferrerRows = data.map{case dayData =>
      dayData.filter{case root => root.visit_referrer.isEmpty}
        .count()
    }
    sb ++= "numFilteredReferrerRows: " +
      numFilteredReferrerRows.mkString(", ") + "\n"
    val totalFRR = numFilteredReferrerRows.reduce(_+_)
    sb ++= "totalFRR: " + totalFRR + "\n\n"

    // Size of fields.
    val allData = sc.union(data)
    colsNeeded.map{ case c =>
      val b = allData
        .map{
          case root =>
            c match {
              case "post_pagename" => root.post_pagename.length
              case "post_visid_high" => root.post_visid_high.length
              case "post_visid_low" => root.post_visid_low.length
              case "visit_num" => root.visit_num.length
              case "hit_time_gmt" => root.hit_time_gmt.length
              case "visit_referrer" => root.visit_referrer.length
              case "post_purchaseid" => root.post_purchaseid.length
              case "post_product_list" => root.post_product_list.length
              case "first_hit_referrer" => root.first_hit_referrer.length
            }
        }
        .reduce(_+_)
      sb ++= c + "Bytes: " + b + "\n"
    }

    sb.toString
  }
}
