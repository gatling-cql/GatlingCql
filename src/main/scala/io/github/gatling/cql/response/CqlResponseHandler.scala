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
package io.github.gatling.cql.response

import java.util.{HashMap => JHashMap}

import com.datastax.oss.driver.api.core.DriverException
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.session.Request
import com.datastax.oss.driver.internal.core.cql.ResultSets
import com.datastax.oss.driver.shaded.guava.common.util.concurrent.FutureCallback
import com.typesafe.scalalogging.StrictLogging
import io.gatling.commons.stats._
import io.gatling.commons.validation.Failure
import io.gatling.core.action.Action
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.github.gatling.cql.checks.CqlCheck
import io.github.gatling.cql.request.CqlComponents


class CqlResponseHandler(next: Action, session: Session, cqlComponents: CqlComponents, start: Long, tag: String,
                         stmt: Request,
                         checks: List[CqlCheck])
  extends FutureCallback[AsyncResultSet] with StrictLogging { //FIXME: remove FutureCallback

  private def writeData(status: Status, startTs: Long, endTs: Long, message: Option[String]) =
    cqlComponents.coreComponents.statsEngine.logResponse(session.scenario, session.groups, tag, startTs, endTs,
      status, None,
      message)

  override def onSuccess(resultSet: AsyncResultSet): Unit = {
    val response = CqlResponse(ResultSets.newInstance(resultSet))
    val endTs = cqlComponents.coreComponents.clock.nowMillis

    cqlComponents.coreComponents.actorSystem.dispatcher.execute(() => {
      val preparedCache: JHashMap[Any, Any] = new JHashMap()

      val checkRes: (Session, Option[Failure]) = Check.check(response, session, checks, preparedCache)

      if (checkRes._2.isEmpty) {
        writeData(OK, start, endTs, None)

        next ! checkRes._1.markAsSucceeded
      }
      else {
        val errors = checkRes._2.get
        writeData(KO, start, endTs, Some(s"Error verifying results: $errors"))

        next ! checkRes._1.markAsFailed
      }
    })
  }

  override def onFailure(t: Throwable): Unit = {
    val endTs = cqlComponents.coreComponents.clock.nowMillis

    cqlComponents.coreComponents.actorSystem.dispatcher.execute(() => {
      if (t.isInstanceOf[DriverException]) {
        val msg = tag + ": c.d.d.c.e." + t.getClass.getSimpleName + ": " + t.getMessage
        writeData(KO, start, endTs, Some(msg))
      }
      else {
        logger.error(s"$tag: Error executing statement $stmt", t)
        writeData(KO, start, endTs, Some(tag + ": " + t.toString))
      }

      next ! session.markAsFailed
    })
  }
}
