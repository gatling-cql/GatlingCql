[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.gatling-cql/gatling-cql/badge.svg?style=flat-square)](https://search.maven.org/artifact/io.github.gatling-cql/gatling-cql) [![Deploy Snapshot](https://github.com/gatling-cql/GatlingCql/actions/workflows/maven_deploy.yml/badge.svg)](https://github.com/gatling-cql/GatlingCql/actions/workflows/maven_deploy.yml)

# GatlingCql

Gatling DSL support for [Apache Cassandra](https://cassandra.apache.org) CQL. Can be used with [Scylla](https://docs.scylladb.com/using-scylla/cassandra-compatibility/) as well

## Features

Basic Gatling DSL for Apache Cassandra CQL, prepared statements are supported as well.

```scala
import com.datastax.oss.driver.api.core.{ConsistencyLevel, CqlSession}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

class CassandraSimulation extends Simulation {
  val keyspace = "test"
  val table_name = "test_table"
  val session = CqlSession.builder().build()//Your C* session
  session.execute(s"""CREATE KEYSPACE IF NOT EXISTS $keyspace 
                      WITH replication = { 'class' : 'SimpleStrategy', 
                                          'replication_factor': '1'}""")
                                          
  session.execute(s"USE $keyspace")
  val cqlConfig = cql.session(session) //Initialize Gatling DSL with your session

  //Setup
  session.execute(s"""CREATE TABLE IF NOT EXISTS $table_name (
          id timeuuid,
          num int,
          str text,
          PRIMARY KEY (id)
        );
      """)
  //It's generally not advisable to use secondary indexes in you schema
  session.execute(f"""CREATE INDEX IF NOT EXISTS $table_name%s_num_idx 
                      ON $table_name%s (num)""")


  //Prepare your statement, we want to be effective, right?
  val prepared = session.prepare(s"""INSERT INTO $table_name 
                                      (id, num, str) 
                                      VALUES 
                                      (now(), ?, ?)""")

  val random = new util.Random
  val feeder = Iterator.continually( 
      // this feader will "feed" random data into our Sessions
      Map(
          "randomString" -> random.nextString(20), 
          "randomNum" -> random.nextInt()
          ))

  val scn = scenario("Two statements").repeat(1) { //Name your scenario
    feed(feeder)
    .exec(cql("simple SELECT") 
         // 'execute' can accept a string 
         // and understands Gatling expression language (EL), i.e. ${randomNum}
        .execute("SELECT * FROM test_table WHERE num = ${randomNum}")
        .check(rowCount.is(1)))
    .exec(cql("prepared INSERT")
         // alternatively 'execute' accepts a prepared statement
        .execute(prepared)
         // you need to provide parameters for that (EL is supported as well)
        .withParams(Integer.valueOf(random.nextInt()), "${randomString}")
        // and set a ConsistencyLevel optionally
        .consistencyLevel(ConsistencyLevel.ANY)) 
  }

  setUp(scn.inject(rampUsersPerSec(10) to 100 during (30.seconds)))
    .protocols(cqlConfig)

  after(session.close())
}
```

## Installation

### For the `gatling-charts-highcharts-bundle` setup

* Get a release TGZ from the [Maven Central](https://repo1.maven.org/maven2/io/github/gatling-cql/gatling-cql/)
* Unpack into Gatling folder: `tar -xjf gatling-cql-3.7.6-1-bundle.tar.gz -C gatling-charts-highcharts-bundle-3.7.6/`
* Run Gatling and you should see `cassandra.CassandraSimulation` in your simulations list

### As a library for your project

Include `gatling-cql` as a dependency to your project. See the [Maven Central Page](https://search.maven.org/artifact/io.github.gatling-cql/gatling-cql/3.7.6-1/jar) for more information for you build tool.

#### Maven Example

You can run your simulations as a part of your build/CI process. See the [Gatling Maven Plugin](https://gatling.io/docs/current/extensions/maven_plugin/) page for more details, please add the following dependency in your POM file.

```xml
    <dependencies>
        <dependency>
            <groupId>io.github.gatling-cql</groupId>
            <artifactId>gatling-cql</artifactId>
            <version>3.7.6-1</version>
        </dependency>
    </dependencies>
```

# More Information

* https://gatling.io/docs/gatling/tutorials/quickstart/
* https://gatling.io/docs/gatling/reference/3.6/cheat-sheet/
