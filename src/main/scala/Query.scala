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

// Spark.
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

// Scala.
import scala.collection.mutable.StringBuilder
import scala.collection.immutable.ListMap

// Adobe analytics Thrift objects.
import thrift.AnalyticsData

// Java/Guava.
import java.net.URL
import com.google.common.net.InternetDomainName

// Analytics Queries.
import queries._

object QueryMeta {
  def info = ListMap[String,Query](
    "Pageviews"->Pageviews,
    "TopPages"->TopPages,
    "TopPagesByBrowser"->TopPagesByBrowser,
    "TopPagesByPreviousTopPages"->TopPagesByPreviousTopPages,
    "Revenue"->Revenue,
    "TopReferringDomains"->TopReferringDomains,
    "RevenueFromTopReferringDomains"->RevenueFromTopReferringDomains,
    "RevenueFromTopReferringDomainsFirstVisitGoogle"->
      RevenueFromTopReferringDomainsFirstVisitGoogle
  )

  def getDomainFromReferrer(r: String): String = {
    try {
      val fullUrl = if (!r.startsWith("http")) "http://" + r else r
      val host = new URL(fullUrl).getHost()
      val ret = InternetDomainName.from(host).topPrivateDomain().toString()
      ret.stripPrefix("InternetDomainName{name=").stripSuffix("}")
    } catch {
      case e: Exception => ""
    }
  }

  def getRevenueFromProductList(pl: String) = {
    pl.split(",").map(_.split(";"))
      .collect{
        case (product) if product.size >= 4 =>
          product(2).toInt*product(3).toFloat
      }
      .foldLeft(0.0)(_+_)
  }
}

case class QueryConf(
  sc: SparkContext,
  data: Array[RDD[AnalyticsData]],
  profile: Boolean,
  daysInRange: Seq[String],
  dailyRows: Seq[Long],
  targetPartitionSize: Long
)

trait Query {
  def colsNeeded: Seq[String]

  def run(conf: QueryConf): String
}
