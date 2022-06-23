/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 GatlingCql developers
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
package cassandra

import com.datastax.oss.driver.api.core.{ConsistencyLevel, CqlSession}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

import scala.concurrent.duration.DurationInt


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
