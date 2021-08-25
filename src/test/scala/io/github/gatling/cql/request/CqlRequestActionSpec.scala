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

import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, SimpleStatement}
import com.datastax.oss.driver.api.core.{ConsistencyLevel, CqlSession}
import io.gatling.commons.stats.KO
import io.gatling.commons.util.DefaultClock
import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.github.gatling.cql.CqlStatement
import io.github.gatling.cql.checks.CqlCheck
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyLong, anyString, argThat, eq => eqAs}
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.mockito.internal.matchers.CapturingMatcher
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.mockito.MockitoSugar

import java.util.concurrent.CompletionStage

class CqlRequestActionSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  val config = GatlingConfiguration.loadForTest()
  val cassandraSession = mock[CqlSession]
  val statement = mock[CqlStatement]
  val statsEngine = mock[StatsEngine]
  val nextAction = mock[Action]
  val coreComponents = new CoreComponents(null, null, null, None, statsEngine, new DefaultClock, null, config)
  val session = Session("scenario", 1, null)

  val target =
    new CqlRequestAction("some-name", nextAction,
      CqlComponents(coreComponents, CqlProtocol(cassandraSession)),
      CqlAttributes("test", statement, ConsistencyLevel.ANY, ConsistencyLevel.SERIAL, List.empty[CqlCheck]))

  it should "fail if expression is invalid and return the error" in {
    //given
    val errorMessageCapture: ArgumentCaptor[Option[String]] = ArgumentCaptor.forClass(classOf[Option[String]])
    given(statement.apply(session)).willReturn("OOPS".failure)

    //when
    target.execute(session)

    //then
    `then`(statsEngine).should(times(1)).logResponse(eqAs(session.scenario), eqAs(session.groups), anyString(), anyLong(), anyLong(), eqAs(KO), eqAs(None), errorMessageCapture.capture())
    val captureErrorMessage = errorMessageCapture.getValue
    captureErrorMessage should contain("Error setting up statement: OOPS")
  }

  it should "build and execute a valid statement" in {
    //given
    val capture = new CapturingMatcher[SimpleStatement]()
    val result = mock[CompletionStage[AsyncResultSet]]
    given(statement.apply(session)).willReturn(SimpleStatement.newInstance("select * from test").success)
    given(cassandraSession.executeAsync(argThat(capture))).willReturn(result)

    //when
    target.execute(session)

    //then
    `then`(result).should(times(1)).whenCompleteAsync(any())
    val capturedStatement = capture.getLastValue
    capturedStatement shouldBe a[SimpleStatement]
    capturedStatement.getConsistencyLevel shouldBe ConsistencyLevel.ANY
    capturedStatement.getSerialConsistencyLevel shouldBe ConsistencyLevel.SERIAL
    capturedStatement.getQuery shouldBe "select * from test"
  }
}
