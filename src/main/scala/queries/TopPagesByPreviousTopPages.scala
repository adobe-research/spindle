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
package queries

// Spark.
import org.apache.spark.{SparkConf,SparkContext}
import org.apache.spark.SparkContext._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.rdd.RDD

import scala.math

// Scala.
import scala.collection.mutable.ArrayBuffer

// Analytics Thrift objects.
import thrift.AnalyticsData

object TopPagesByPreviousTopPages extends Query {
  def colsNeeded = Seq("post_pagename", "post_visid_high", "post_visid_low",
    "visit_num", "hit_time_gmt")
  def run(c: QueryConf) = {
    val allData = c.sc.union(c.data).cache()
    val numAllRows = c.dailyRows.reduce(_+_)
    val numPartitions = math.max((numAllRows/c.targetPartitionSize).toInt, 1)
    val topPagenames = allData.collect{
        case (root) if !root.post_pagename.isEmpty => (root.post_pagename, 1)
      }
      .reduceByKey(_+_,numPartitions)
      .top(5) {
        Ordering.by((entry: ((String, Int))) => entry._2)
      }.toSeq
    val visitPagePaths: RDD[Seq[String]] = allData.collect{
        case (root) if !root.post_pagename.isEmpty =>
          (BigInt(root.post_visid_high+root.post_visid_low+root.visit_num),
            (root.post_pagename, root.hit_time_gmt.toLong))
      }
      .groupByKey(numPartitions)
      .map{
        case (visId, pagesAndTimes: Seq[(String,Long)]) =>
          pagesAndTimes.sortWith(_._2 < _._2).map(_._1).seq
      }.cache()
    val queryResult = topPagenames.map{
      case (pagename, overallCount) =>
        val pageResults = visitPagePaths.flatMap{
            case (pages) =>
              val pageNamesBeforeX = pages.sliding(2).collect{
                case ArrayBuffer(x,y)
                  if (y == pagename) && (x != pagename) => (x,1)
              }
              pageNamesBeforeX.toList
          }
          .reduceByKey(_+_) // Not obvious how to partition.
          .top(5) {
            Ordering.by((entry: ((String, Int))) => entry._2)
          }.toSeq
        (pagename, overallCount, pageResults)
    }.seq
    visitPagePaths.unpersist()
    allData.unpersist()
    if (c.profile)
      "[" + queryResult.map("\"" + _.toString + "\"").mkString(", ") + "]"
    else {
      html.TopPagesBreakdowns("TopPagesByPreviousTopPages",
        c.daysInRange, queryResult).toString
    }
  }
}
