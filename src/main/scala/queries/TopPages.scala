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

// Analytics Thrift objects.
import thrift.AnalyticsData

object TopPages extends Query {
  def colsNeeded = Seq("post_pagename")

  def run(c: QueryConf) = {
    val allData = c.sc.union(c.data)
    val numAllRows = c.dailyRows.reduce(_+_)
    val numPartitions = math.max((numAllRows/c.targetPartitionSize).toInt, 1)
    val queryResult = allData.collect{
        case (root) if !root.post_pagename.isEmpty => (root.post_pagename, 1)
      }
      .reduceByKey(_+_,numPartitions)
      .top(10) {
        Ordering.by((entry: ((String, Int))) => entry._2)
      }.toSeq
    if (c.profile)
      "[" + queryResult.map("\"" + _.toString + "\"").mkString(", ") + "]"
    else {
      html.TopPages("TopPages", c.daysInRange, queryResult).toString
    }
  }
}
