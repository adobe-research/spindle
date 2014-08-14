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

// SiteCatalyst Thrift objects.
import thrift.{SiteCatalyst,SiteCatalystEvar,SiteCatalystProp}

object TopPagesByBrowser extends Query {
  def colsNeeded = Seq("post_pagename", "user_agent")
  def run(c: QueryConf) = {
    val allData = c.sc.union(c.data).cache()
    val numAllRows = c.dailyRows.reduce(_+_)
    val numPartitions = (numAllRows/c.targetPartitionSize).toInt
    val topPagenames = allData.collect{
        case (root) if !root.post_pagename.isEmpty => (root.post_pagename, 1)
      }
      .reduceByKey(_+_,numPartitions)
      .top(5) {
        Ordering.by((entry: ((String, Int))) => entry._2)
      }.toSeq
    val queryResult = topPagenames.map{
      case (pagename, count) =>
        val pageResults = allData.collect{
            case (root) if root.post_pagename == pagename =>
              (getBrowserFromUseragent(root.user_agent), 1)
          }
          .reduceByKey(_+_) // Not obvious how to partition.
          .top(3) {
            Ordering.by((entry: ((String, Int))) => entry._2)
          }.toSeq
        (pagename, count, pageResults.toSeq)
    }.seq
    allData.unpersist()
    if (c.profile)
      "[" + queryResult.map("\"" + _.toString + "\"").mkString(", ") + "]"
    else {
      html.TopPagesBreakdowns("TopPagesByBrowser", c.daysInRange, queryResult)
        .toString
    }
  }

  def getBrowserFromUseragent(ua: String) = {
    if (ua.contains("Firefox") && !ua.contains("Seamonkey")) "Firefox"
    else if (ua.contains("Seamonkey")) "Seamonkey"
    else if (ua.contains("Chrome") && !ua.contains("Chromium")) "Chrome"
    else if (ua.contains("Chromium")) "Chromium"
    else if (ua.contains("OPR") || ua.contains("Opera")) "Opera"
    else if (ua.contains(";MSIE")) "InternetExplorer"
    else "Unknown"
  }
}
