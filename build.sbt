import AssemblyKeys._
import com.github.bigtoast.sbtthrift.ThriftPlugin

assemblySettings

jarName in assembly := "QueryApp.jar"

// Load "provided" libraries with `sbt run`.
run in Compile <<= Defaults.runTask(
  fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)
)

name := "QueryApp"

version := "1.0"

scalaVersion := "2.10.3"

fork in run := true

unmanagedResourceDirectories in Compile <<= Seq(
  baseDirectory / "src/main/webapp",
  baseDirectory / "src/main/resources"
).join

libraryDependencies ++= Seq(
  // Spark dependencies.
  // Mark as provided if distributing to clusters.
  // Don't use 'provided' if running the program locally with `sbt run`.
  "org.apache.spark" %% "spark-core" % "1.0.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "1.0.0" % "provided",
  // "org.slf4j" % "slf4j-simple" % "1.7.7", // Logging.
  "org.json4s" %% "json4s-native" % "3.2.10", // JSON parsing.
  "org.apache.hadoop" % "hadoop-client" % "2.4.0" % "provided" excludeAll(
    ExclusionRule(organization = "org.jboss.netty"),
    ExclusionRule(organization = "io.netty"),
    ExclusionRule(organization = "org.eclipse.jetty"),
    ExclusionRule(organization = "org.mortbay.jetty"),
    ExclusionRule(organization = "org.ow2.asm"),
    ExclusionRule(organization = "asm")
  ),
  "io.spray" % "spray-can" % "1.2.1",
  "io.spray" % "spray-routing" % "1.2.1",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "org.apache.thrift" % "libthrift" % "0.9.1",
  "com.twitter" % "parquet-thrift" % "1.5.0",
  "com.google.guava" % "guava" % "17.0",
  "org.joda" % "joda-convert" % "1.6",
  "joda-time" % "joda-time" % "2.3"
)

resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Spray repo" at "http://repo.spray.io",
  "sonatype" at "https://oss.sonatype.org/content/groups/public",
  "Twitter" at "http://maven.twttr.com/"
)

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

seq(ThriftPlugin.thriftSettings: _*)
