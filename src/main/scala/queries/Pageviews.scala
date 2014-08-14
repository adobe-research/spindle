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

object Pageviews extends Query {
  def colsNeeded = Seq("post_pagename")

  def run(c: QueryConf) = {
    val queryResult = c.data.map{dayData =>
      dayData.filter(root=>(root.post_pagename != null) &&
          !root.post_pagename.isEmpty())
      .count()
    }
    if (c.profile) "[" + queryResult.map(_.toString).mkString(", ") + "]"
    else {
      html.PageViews("Pageviews", c.daysInRange.zip(queryResult)).toString
    }
  }
}
