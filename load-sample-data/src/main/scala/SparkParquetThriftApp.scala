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
import com.adobe.thrift.AnalyticsData

// Scala collections.
import scala.collection.mutable.ArrayBuffer

// Spark.
import org.apache.spark
import spark.{SparkConf,SparkContext}
import spark.rdd.RDD
import org.apache.spark.SparkContext._

// Map Reduce.
import org.apache.hadoop.{conf,fs,mapreduce}
import fs.{FileSystem,Path}
import mapreduce.Job
import conf.Configuration

// File.
import com.google.common.io.Files
import java.io.File

// Parquet and Thrift support.
import parquet.hadoop.{ParquetOutputFormat, ParquetInputFormat}
import parquet.hadoop.thrift.{
  ParquetThriftInputFormat,ParquetThriftOutputFormat,
  ThriftReadSupport,ThriftWriteSupport
}

object SparkParquetThriftApp {
  def main(args: Array[String]) {
    val mem = "22g"
    println("Initializing Spark context.")
    println("  Memory: " + mem)
    val sparkConf = new SparkConf()
      .setAppName("SparkParquetThrift")
      .setMaster("local[1]")
      .setSparkHome("/usr/lib/spark")
      .setJars(Seq())
      .set("spark.executor.memory", mem)
    val sc = new SparkContext(sparkConf)

    val data1 = Seq(
      new AnalyticsData(
        "Page A",
        "Chrome",
        "http://facebook.com",
        "111",
        "111",
        "1",
        "1408007374",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page B",
        "Chrome",
        "http://facebook.com",
        "111",
        "111",
        "1",
        "1408007377",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page C",
        "Chrome",
        "http://facebook.com",
        "111",
        "111",
        "1",
        "1408007380",
        "purchase1",
        ";ProductID1;1;40;,;ProductID2;1;20;",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page B",
        "Chrome",
        "http://google.com",
        "222",
        "222",
        "1",
        "1408007379",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page C",
        "Chrome",
        "http://google.com",
        "222",
        "222",
        "1",
        "1408007381",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page A",
        "Firefox",
        "http://google.com",
        "222",
        "222",
        "1",
        "1408007382",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page A",
        "Safari",
        "http://google.com",
        "333",
        "333",
        "1",
        "1408007383",
        "",
        "",
        "http://facebook.com"
      ),
      new AnalyticsData(
        "Page B",
        "Safari",
        "http://google.com",
        "333",
        "333",
        "1",
        "1408007386",
        "",
        "",
        "http://facebook.com"
      )
    )
    val data2 = Seq(
      new AnalyticsData(
        "Page A",
        "Chrome",
        "http://facebook.com",
        "111",
        "111",
        "1",
        "1408097374",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page B",
        "Chrome",
        "http://facebook.com",
        "111",
        "111",
        "1",
        "1408097377",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page C",
        "Chrome",
        "http://facebook.com",
        "111",
        "111",
        "1",
        "1408097380",
        "purchase1",
        ";ProductID1;1;60;,;ProductID2;1;100;",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page B",
        "Chrome",
        "http://google.com",
        "222",
        "222",
        "1",
        "1408097379",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page A",
        "Safari",
        "http://google.com",
        "333",
        "333",
        "1",
        "1408097383",
        "",
        "",
        "http://facebook.com"
      ),
      new AnalyticsData(
        "Page B",
        "Safari",
        "http://google.com",
        "333",
        "333",
        "1",
        "1408097386",
        "",
        "",
        "http://facebook.com"
      )
    )
    val data3 = Seq(
      new AnalyticsData(
        "Page A",
        "Chrome",
        "http://facebook.com",
        "111",
        "111",
        "1",
        "1408187380",
        "purchase1",
        ";ProductID1;1;60;,;ProductID2;1;100;",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page B",
        "Chrome",
        "http://facebook.com",
        "111",
        "111",
        "1",
        "1408187380",
        "purchase1",
        ";ProductID1;1;200;",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page D",
        "Chrome",
        "http://google.com",
        "222",
        "222",
        "1",
        "1408187379",
        "",
        "",
        "http://google.com"
      ),
      new AnalyticsData(
        "Page A",
        "Safari",
        "http://google.com",
        "333",
        "333",
        "1",
        "1408187383",
        "",
        "",
        "http://facebook.com"
      ),
      new AnalyticsData(
        "Page B",
        "Safari",
        "http://google.com",
        "333",
        "333",
        "1",
        "1408187386",
        "",
        "",
        "http://facebook.com"
      ),
      new AnalyticsData(
        "Page C",
        "Safari",
        "http://google.com",
        "333",
        "333",
        "1",
        "1408187388",
        "",
        "",
        "http://facebook.com"
      )
    )
    loadDay(sc, "2014-08-14", data1)
    loadDay(sc, "2014-08-15", data2)
    loadDay(sc, "2014-08-16", data3)
  }

  def loadDay(sc: SparkContext, date: String, data: Seq[AnalyticsData]) = {
    val job = new Job()
    val parquetStore=s"hdfs://server_address.com:8020/spindle-sample-data/$date"
    println("Writing sample data to Parquet.")
    println("  - ParquetStore: " + parquetStore)
    ParquetThriftOutputFormat.setThriftClass(job, classOf[AnalyticsData])
    ParquetOutputFormat.setWriteSupportClass(job, classOf[AnalyticsData])
    sc.parallelize(data)
      .map(obj => (null, obj))
      .saveAsNewAPIHadoopFile(
        parquetStore,
        classOf[Void],
        classOf[AnalyticsData],
        classOf[ParquetThriftOutputFormat[AnalyticsData]],
        job.getConfiguration
      )
  }

