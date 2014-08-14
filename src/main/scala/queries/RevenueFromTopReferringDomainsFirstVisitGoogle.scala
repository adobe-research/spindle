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

object RevenueFromTopReferringDomainsFirstVisitGoogle extends Query {
  def colsNeeded = Seq("visit_referrer", "first_hit_referrer",
    "post_visid_high", "post_visid_low", "visit_num",
    "post_purchaseid", "post_product_list")
  def run(c: QueryConf) = {
    val allData = c.sc.union(c.data)
    val numAllRows = c.dailyRows.reduce(_+_)
    val numPartitions = (numAllRows/c.targetPartitionSize).toInt
    val topReferrers = allData.collect{
        case (root) if !root.visit_referrer.isEmpty =>
          (BigInt(root.post_visid_high + root.post_visid_low + root.visit_num),
            QueryMeta.getDomainFromReferrer(root.visit_referrer))
      }
      .distinct(numPartitions) {
        Ordering.by((entry: (BigInt,String)) => entry._1)
      }
      .collect{
        case (visit_id, ref) if !ref.isEmpty() =>
          (ref, 1)
      }
      .reduceByKey(_+_,numPartitions)
      .top(5) {
        Ordering.by((entry: ((String, Int))) => entry._2)
      }.toSeq
    val topReferrersWithRevenue = topReferrers.map{ case (referrer,count) =>
      val revenueForReferrer = allData.collect{
        case (root)
          if root.visit_referrer.contains(referrer) &&
             !root.post_purchaseid.isEmpty() &&
             root.first_hit_referrer.contains("google.com") =>
          QueryMeta.getRevenueFromProductList(root.post_product_list)
      }
      .reduce(_+_)
      (referrer, count, revenueForReferrer)
    }.seq
    allData.unpersist()
    if (c.profile)
      "[" + topReferrers.map("\"" + _.toString + "\"").mkString(", ") + "]"
    else {
      html.RevenueFromTopReferringDomains(
        "RevenueFromTopReferringDomainsFirstVisitGoogle",
        c.daysInRange, topReferrersWithRevenue).toString
    }
  }
}
