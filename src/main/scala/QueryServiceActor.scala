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

// Akka
import akka.actor.{Actor,ActorRefFactory}
import akka.pattern.ask
import akka.util.Timeout

// Spray
import spray.http._
import spray.routing.HttpService
import MediaTypes._

// Scala
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import queries._

// Actor accepting Http requests for the Scala collector.
class QueryServiceActor extends Actor with HttpService {
  implicit val timeout: Timeout = 1000.second // For the actor 'asks'
  import context.dispatcher
  def actorRefFactory = context

  // Use QueryService so the same route can be accessed differently
  // in the testing framework.
  private val collectorService = new QueryService(context)

  // Message loop for the Spray service.
  def receive = handleTimeouts orElse runRoute(collectorService.collectorRoute)

  def handleTimeouts: Receive = {
    case Timedout(_) => sender !
      HttpResponse(status = 408, entity = "Error: Page timed out.")
  }
}

class QueryService(context: ActorRefFactory) extends HttpService {
  implicit def actorRefFactory = context
  val collectorRoute = {
    get {
      pathSingleSlash {
        redirect("/index", StatusCodes.Found)
      }~
      path("index") {
        respondWithMediaType(`text/html`) {
          complete{
            val unzippedMap = QueryMeta.info.unzip
            html.index(unzippedMap._1.toSeq).toString
          }
        }
      }~
      path("favicon.ico") {
        complete(StatusCodes.NotFound)
      }~
      path(Rest) { path =>
        getFromResource("bootstrap/%s" format path)
      }~
      path(Rest) { path =>
        getFromResource("bootstrap-daterangepicker/%s" format path)
      }~
      path(Rest) { path =>
        getFromResource("terminus/%s" format path)
      }~
      path("query") {
        parameters('name.as[String], 'profile ? false, 'cache ? false,
            'start_ymd.as[String], 'finish_ymd.as[String],
            'targetPartitionSize ? 100000L) {
        (name, profile, cache, start_ymd, finish_ymd, targetPartitionSize) =>
          respondWithMediaType(if (profile) `text/plain` else `text/html`) {
            complete(
              future {
                QueryHandler.handle(name, profile, cache,
                  start_ymd, finish_ymd, targetPartitionSize)
              }
            )
          }
        }
      }~
      path("adhoc") {
        parameters('start_ymd.as[String], 'finish_ymd.as[String]) {
            (start_ymd, finish_ymd) =>
          respondWithMediaType(`text/html`) {
            complete(
              future {
                QueryHandler.generateAdhocPage(start_ymd, finish_ymd)
              }
            )
          }
        }
      }~
      path("adhoc-sql") {
        parameters('sqlQuery.as[String], 'numResults.as[Int],
          'start_ymd.as[String], 'finish_ymd.as[String]) {
        (sqlQuery, numResults, start_ymd, finish_ymd) =>
          respondWithMediaType(`text/plain`) {
            complete(
              future {
                QueryHandler.handleSQL(sqlQuery, numResults,
                  start_ymd, finish_ymd)
              }
            )
          }
        }
      }~
      path("stats") {
        complete{
          QueryHandler.getStats()
        }
      }~
      path("loadTime") {
        complete{
          QueryHandler.loadTime()
        }
      }~
      path("kill") {
        complete{
          QueryHandler.stopSparkContext()
          System.exit(0)
          ""
        }
      }~
      path("restartSparkContext") {
        parameters('memory ? "1g", 'cores ? 4) { (memory, cores) =>
          respondWithMediaType(`text/plain`) {
            complete(
              future {
                QueryHandler.restartSparkContext(memory,cores)
              }
            )
          }
        }
      }
    }~
    complete(HttpResponse(status = 404, entity = "404 Not found"))
  }
}
