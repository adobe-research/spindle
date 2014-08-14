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

Grunt deploy

# Data Format
Adobe Analytics events data have 250 columns, and
most queries use less than 7 columns.
Spindle stores event data in the [Parquet][parquet] columnar store
on the [Hadoop Distributed File System][hdfs] (HDFS).

Parquet can be used with [Avro][avro] or [Thrift][thrift] schemas.
[Matt Massie's article][spark-parquet-avro] provides an example of
using Parquet with Avro.
[adobe-research/spark-parquet-thrift-example][spark-parquet-thrift-example]
is a complete [Scala][scala]/[sbt][sbt] project
using Thrift for data serialization and shows how to only load the
specified columnar subset.

The entire Adobe Analytics schema cannot be published,
but [AnalyticsData.thrift][AnalyticsData.thrift] provides
a schema with fields common to every analytics events.

```Thrift
struct AnalyticsData {
  1: string post_pagename;
  2: string user_agent;
  3: string visit_referrer;
  4: string post_visid_high;
  5: string post_visid_low;
  6: string visit_num;
  7: string hit_time_gmt;
  8: string post_purchaseid;
  9: string post_product_list;
  10: string first_hit_referrer;
}
```

This data is separated by day on disk of format `YYYY-MM-DD`,
which provides the advantage of not having to filter to separate
data by day.

## Loading Sample Data
The `load-sample-data` directory contains a Scala program
to load the following sample data into [HDFS][hdfs]
modeled after
[adobe-research/spark-parquet-thrift-example][spark-parquet-thrift-example].
See [adobe-research/spark-parquet-thrift-example][spark-parquet-thrift-example]
for more information on running this application.


### hdfs://hdfs_server_address:8020/spindle-sample-data/2014-08-14
| post_pagename | user_agent | visit_referrer | post_visid_high | post_visid_low | visit_num | hit_time_gmt | post_purchaseid | post_product_list | first_hit_referrer |
|---|---|---|---|---|---|---|---|---|---|
| Page A | Chrome | http://facebook.com | 111 | 111 | 1 | 1408007374 | | | http://google.com
| Page B | Chrome | http://facebook.com | 111 | 111 | 1 | 1408007377 | | | http://google.com
| Page C | Chrome | http://facebook.com | 111 | 111 | 1 | 1408007380 | purchase1 | ;ProductID1;1;40;,;ProductID2;1;20; | http://google.com
| Page B | Chrome | http://google.com | 222 | 222 | 1 | 1408007379 | | | http://google.com
| Page C | Chrome | http://google.com | 222 | 222 | 1 | 1408007381 | | | http://google.com
| Page A | Firefox | http://google.com | 222 | 222 | 1 | 1408007382 | | | http://google.com
| Page A | Safari | http://google.com | 333 | 333 | 1 | 1408007383 | | | http://facebook.com
| Page B | Safari | http://google.com | 333 | 333 | 1 | 1408007386 | | | http://facebook.com

### hdfs://hdfs_server_address:8020/spindle-sample-data/2014-08-15
| post_pagename | user_agent | visit_referrer | post_visid_high | post_visid_low | visit_num | hit_time_gmt | post_purchaseid | post_product_list | first_hit_referrer |
|---|---|---|---|---|---|---|---|---|---|
| Page A | Chrome | http://facebook.com | 111 | 111 | 1 | 1408097374 | | | http://google.com
| Page B | Chrome | http://facebook.com | 111 | 111 | 1 | 1408097377 | | | http://google.com
| Page C | Chrome | http://facebook.com | 111 | 111 | 1 | 1408097380 | purchase1 | ;ProductID1;1;60;,;ProductID2;1;100; | http://google.com
| Page B | Chrome | http://google.com | 222 | 222 | 1 | 1408097379 | | | http://google.com
| Page A | Safari | http://google.com | 333 | 333 | 1 | 1408097383 | | | http://facebook.com
| Page B | Safari | http://google.com | 333 | 333 | 1 | 1408097386 | | | http://facebook.com

### hdfs://hdfs_server_address:8020/spindle-sample-data/2014-08-16
| post_pagename | user_agent | visit_referrer | post_visid_high | post_visid_low | visit_num | hit_time_gmt | post_purchaseid | post_product_list | first_hit_referrer |
|---|---|---|---|---|---|---|---|---|---|
| Page A | Chrome | http://facebook.com | 111 | 111 | 1 | 1408187380 | purchase1 | ;ProductID1;1;60;,;ProductID2;1;100; | http://google.com
| Page B | Chrome | http://facebook.com | 111 | 111 | 1 | 1408187380 | purchase1 | ;ProductID1;1;200; | http://google.com
| Page D | Chrome | http://google.com | 222 | 222 | 1 | 1408187379 | | | http://google.com
| Page A | Safari | http://google.com | 333 | 333 | 1 | 1408187383 | | | http://facebook.com
| Page B | Safari | http://google.com | 333 | 333 | 1 | 1408187386 | | | http://facebook.com
| Page C | Safari | http://google.com | 333 | 333 | 1 | 1408187388 | | | http://facebook.com

# Configuration
TODO

# Building

This is developed on CentOS 6.5 with
sbt 0.13.5, Spark 1.0.0, Hadoop 2.0.0-cdh4.7.0,
and parquet-thrift 1.5.0.

| Command | Output |
|---|---|
| `cat /etc/centos-release` | CentOS release 6.5 (Final) |
| `sbt --version` | sbt launcher version 0.13.5 |
| `thrift --version` | Thrift version 0.9.1 |
| `hadoop version` | Hadoop 2.0.0-cdh4.7.0 |
| `cat /usr/lib/spark/RELEASE` | Spark 1.0.0 built for Hadoop 2.0.0-cdh4.7.0 |

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
[hdfs]: http://hadoop.apache.org/
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

[AnalyticsData.thrift]: https://git.corp.adobe.com/amos/spindle/blob/master/src/main/thrift/AnalyticsData.thrift
[spark-parquet-thrift-example]: https://github.com/adobe-research/spark-parquet-thrift-example
