/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 GatlingCql developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.gatling.cql


import java.util.concurrent.atomic.AtomicInteger

import com.datastax.driver.core.Cluster
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

import scala.concurrent.duration.DurationInt

class CheckCompileTest extends Simulation
{
  val keyspace = "gatlingcql"
  val table_name = "test_table"

  val cluster = Cluster.builder().addContactPoint("127.0.0.1")
    .build()
  val session = cluster.connect()

  session.execute(
    s"""CREATE KEYSPACE IF NOT EXISTS $keyspace
                      WITH replication = { 'class' : 'SimpleStrategy',
                                          'replication_factor': '1'}""")
  session.execute(s"USE $keyspace")

  session.execute(
    s"""CREATE TABLE IF NOT EXISTS $table_name (
          id int,
          str text,
          PRIMARY KEY (id)
        );
      """)

  // clear table before the test
  session.execute(s"""TRUNCATE TABLE $table_name""")

  val cqlConfig = cql.session(session) //Initialize Gatling DSL with your session

  val sequence = new AtomicInteger
  val feeder = Iterator.continually(
    // this feader will "feed" random data into our Sessions
    Map(
      "sequenceNum" -> sequence.incrementAndGet
    ))

  val preparedInsert = session.prepare(
    s"""INSERT INTO $table_name (id, str) VALUES (?, ?)""")

  val preparedUpdate = session.prepare(
    s"""UPDATE $table_name SET str = ? WHERE id = ? IF str = ?""")

  val preparedSelect = session.prepare(
    s"""SELECT * FROM test_table WHERE id = ?""")

  val select = cql("simple SELECT")
    .execute(preparedSelect)
    .withParams("${sequenceNum}")

  val selectAgain = cql("SELECT again")
    .execute(preparedSelect)
    .withParams("${sequenceNum}")

  val insert = cql("prepared INSERT")
    .execute(preparedInsert)
    .withParams("${sequenceNum}", "foobar")

  val lwtUpdate = cql("LWT UPDATE")
    .execute(preparedUpdate)
    .withParams("foobar${sequenceNum}", "${sequenceNum}", "foobar")

  val lwtUpdateFailing = cql("Failing LWT UPDATE")
    .execute(preparedUpdate)
    .withParams("foobar${sequenceNum}", "${sequenceNum}", "foobar")

  val scn = scenario("Two statements").repeat(1)
  {
    feed(feeder)
      .exec(insert
              .check(exhausted is true)
              // "normal" INSERTs don't return anything
              .check(rowCount is 0))
      .exec(lwtUpdate
              // LWT operations return a row
              .check(exhausted is false)
              .check(rowCount is 1)
              .check(applied is true)
              .check(resultSet satisfies { rs => rs.wasApplied })
              .check(executionInfo satisfies { ei => ei.isSchemaInAgreement })
              .check(simpleCheck(rs => rs.getExecutionInfo.isSchemaInAgreement))
      )
      .exec(lwtUpdateFailing
              // 2nd LWT operation needs to fail
              // NOTE: on a 1-node "cluster" on localhost this 2nd LWT may still _succeed_ with a "low" probabiltiy,
              // because of same timestamps used to compare the cells can be the same - the faster your computer,
              // the higher the probabiltiy ;)
              .check(exhausted is false,
                     rowCount is 1,
                     applied is false,
                     columnValue("str") is "foobar${sequenceNum}"
              )) // why the LWT update failed
      .exec(select
              .check(exhausted is false)
              .check(rowCount is 1,
                     columnValue("id").find.is("${sequenceNum}"),
                     columnValue("id").find.saveAs("foo_baz"),
                     columnValue("id").find.is("${foo_baz}"),
                     columnValue("str").find.is("foobar${foo_baz}")))
      .exec(selectAgain
              .check(exhausted is false)
              .check(rowCount is 1,
                     columnValue("id").find.is("${foo_baz}"),
                     columnValue("str").find.is("foobar${foo_baz}")))
  }

  setUp(
    scn.inject(rampUsersPerSec(10) to 500 during (30 seconds), constantUsersPerSec(500) during(30 seconds))
  ).protocols(cqlConfig)

  after(cluster.close())

}
