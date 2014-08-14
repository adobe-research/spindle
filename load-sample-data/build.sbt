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

import AssemblyKeys._
import com.github.bigtoast.sbtthrift.ThriftPlugin

assemblySettings

jarName in assembly := "LoadSampleToHDFS.jar"

name := "SparkParquetThrift"

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  // Spark dependencies.
  // Mark as provided if distributing to clusters.
  // Don't use 'provided' if running the program locally with `sbt run`.
  "org.apache.spark" %% "spark-core" % "1.0.0" % "provided",
  "org.apache.hadoop" % "hadoop-client" % "2.4.0" % "provided" excludeAll(
    ExclusionRule(organization = "org.jboss.netty"),
    ExclusionRule(organization = "io.netty"),
    ExclusionRule(organization = "org.eclipse.jetty"),
    ExclusionRule(organization = "org.mortbay.jetty"),
    ExclusionRule(organization = "org.ow2.asm"),
    ExclusionRule(organization = "asm")
  ),
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "org.apache.thrift" % "libthrift" % "0.9.1",
  "com.twitter" % "parquet-thrift" % "1.5.0"
)

resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Twitter" at "http://maven.twttr.com/",
  "bigtoast-github" at "http://bigtoast.github.com/repo/"
)

seq(ThriftPlugin.thriftSettings: _*)
