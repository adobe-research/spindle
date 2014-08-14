# Spindle

![](https://git.corp.adobe.com/amos/spindle/raw/master/images/architecture.png)

Analytics platforms such as [Adobe Analytics][adobe-analytics]
are growing to process petabytes of data in real-time.
Delivering responsive interfaces over this much data is difficult,
and there are many distributed data processing technologies such
as [Hadoop MapReduce][mapreduce], [Apache Spark][spark],
[Apache Drill][drill], and [Cloudera Impala][impala].

**Spindle is a research-based distributed analytics query
engine built with Spark.**
Spark claims 100x speedups over MapReduce for in-memory processing.
This project contains the full Spindle implementation with
benchmarking scripts to tune Spindle and Spark for maximum performance.

# Demo
TODO

# Configuration
TODO

# Database Initialization
TODO

# Building

This is developed on CentOS 6.5 with
sbt 0.13.5, Spark 1.0.0, Hadoop 2.0.0-cdh4.7.0,
and parquet-thrift 1.5.0.

|---|---|
| `cat /etc/centos-release` | CentOS release 6.5 (Final) |
| `sbt --version` | sbt launcher version 0.13.5 |
| `thrift --version` | Thrift version 0.9.1 |
| `hadoop version` | Hadoop 2.0.0-cdh4.7.0 |
| `cat /usr/lib/spark/RELEASE` | Spark 1.0.0 built for Hadoop 2.0.0-cdh4.7.0 |
|---|---|

## Testing Thrift Schema
TODO

## Fat JAR

```Bash
thrift --gen java AnalyticsData.thrift
```


TODO

# Deploying
TODO

# Benchmarking
TODO

# License
TODO

Bootstrap - Apache 2.0
https://github.com/dangrossman/bootstrap-daterangepicker - Apache 2.0
Terminus - MIT license

[adobe-analytics]: http://www.adobe.com/solutions/digital-analytics.html
[mapreduce]: http://wiki.apache.org/hadoop/MapReduce
[drill]: http://incubator.apache.org/drill/
[impala]: http://www.cloudera.com/content/cloudera/en/products-and-services/cdh/impala.html

[spark]: http://spark.apache.org/
[parquet]: http://parquet.io/
[thrift]: https://thrift.apache.org/
[thrift-guide]: http://diwakergupta.github.io/thrift-missing-guide/
[avro]: http://avro.apache.org/
[parquet-cascading]: https://github.com/Parquet/parquet-mr/blob/master/parquet_cascading.md
[parquet-format]: https://github.com/apache/incubator-parquet-format

[scala]: http://scala-lang.org
[sbt]: http://www.scala-sbt.org/
[spark-parquet-avro]: http://zenfractal.com/2013/08/21/a-powerful-big-data-trio/
[rdd]: http://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.rdd.RDD
[sbt-thrift]: https://github.com/bigtoast/sbt-thrift
[sbt-assembly]: https://github.com/sbt/sbt-assembly

[powered-by-spark]: https://cwiki.apache.org/confluence/display/SPARK/Powered+By+Spark
