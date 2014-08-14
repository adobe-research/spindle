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

import Helpers._

object Profiling {
  def getTime(tag: String, payload: Option[(String,String)] = None): String = {
    val sb = new StringBuilder()
    sb.appendLn("- Desc: " + tag)
    if (payload.isDefined) {
      sb.appendLn("  - " + payload.get._1 + ": " + payload.get._2)
    }
    sb.appendLn("  TimeMillis: " + System.currentTimeMillis)
    sb.toString
  }

  def getResult(tag: String, result: String): String = {
    val sb = new StringBuilder()
    sb.appendLn("- Result: " + result)
    sb.appendLn("  TimeMillis: " + System.currentTimeMillis)
    sb.toString
  }

  def getMemoryUtilization(sc: SparkContext): String = {
    val sb = new StringBuilder()
    val executorMemoryStatus = sc.getExecutorMemoryStatus
    sb.appendLn("  ExecutorMemoryStatus:")
    for ((name, mem) <- executorMemoryStatus) {
      sb.appendLn("    - Name: '" + name + "'")
      sb.appendLn("      MaxMemForCaching: " + mem._1)
      sb.appendLn("      RemainingMemForCaching: " + mem._2)
    }
    val storageInfo = sc.getRDDStorageInfo
    sb.appendLn("  StorageInfoLength: " + storageInfo.size)
    if (storageInfo.size > 0) {
      sb.appendLn("  StorageInfo:")
      for (info <- storageInfo) {
        sb.appendLn(s"    - name: ${info.name}")
        sb.appendLn(s"      memSize: ${info.memSize}")
        sb.appendLn(s"      diskSize: ${info.diskSize}")
        sb.appendLn(s"      numPartitions: ${info.numPartitions}")
        sb.appendLn(s"      numCachedPartitions: ${info.numCachedPartitions}")
        sb.appendLn(s"      storageLevel: ${info.storageLevel}")
      }
    }
    sb.toString
  }
}
