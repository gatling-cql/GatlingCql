/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 GatlingCql developers
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
package io.github.gatling.cql.request

import akka.actor.ActorSystem
import com.datastax.driver.core._
import io.gatling.commons.stats.KO
import io.gatling.commons.util.DefaultClock
import io.gatling.commons.validation.{FailureWrapper, SuccessWrapper}
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{Session => GSession}
import io.gatling.core.stats.StatsEngine
import io.github.gatling.cql.CqlStatement
import io.github.gatling.cql.checks.CqlCheck
import org.easymock.Capture
import org.easymock.EasyMock.{anyObject, anyString, capture, reset, eq => eqAs}
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.easymock.EasyMockSugar

class CqlRequestActionSpec extends AnyFlatSpec with EasyMockSugar with Matchers with BeforeAndAfter {
  val config = GatlingConfiguration.loadForTest()
  val cassandraSession = mock[Session]
  val statement = mock[CqlStatement]
  val system = mock[ActorSystem]
  val statsEngine = mock[StatsEngine]
  val nextAction = mock[Action]
  val coreComponents = CoreComponents(system, null, null, statsEngine, new DefaultClock, null, config)
  val session = GSession("scenario", 1, System.currentTimeMillis)

  val target =
    new CqlRequestAction("some-name", nextAction,
      CqlComponents(coreComponents, CqlProtocol(cassandraSession)),
      CqlAttributes("test", statement, ConsistencyLevel.ANY, ConsistencyLevel.SERIAL, List.empty[CqlCheck]))

  before {
    reset(statement, cassandraSession, statsEngine)
  }

  it should "fail if expression is invalid and return the error" in {
    val errorMessageCapture = Capture.newInstance[Some[String]]()
    expecting {
      statement.apply(session).andReturn("OOPS".failure)
      statsEngine.logResponse(eqAs(session), anyString, anyObject[Long], anyObject[Long], eqAs(KO), eqAs(None), capture(errorMessageCapture))
    }

    whenExecuting(statement, statsEngine) {
      target.execute(session)
    }
    val captureErrorMessage = errorMessageCapture.getValue
    captureErrorMessage.get should be("Error setting up statement: OOPS")
  }

  it should "execute a valid statement" in {
    val statementCapture = Capture.newInstance[RegularStatement]()
    expecting {
      statement.apply(session).andReturn(new SimpleStatement("select * from test").success)
      cassandraSession.executeAsync(capture(statementCapture)) andReturn mock[ResultSetFuture]
    }
    whenExecuting(statement, cassandraSession) {
      target.execute(session)
    }
    val capturedStatement = statementCapture.getValue
    capturedStatement shouldBe a[SimpleStatement]
    capturedStatement.getConsistencyLevel shouldBe ConsistencyLevel.ANY
    capturedStatement.getQueryString should be("select * from test")
  }
}
