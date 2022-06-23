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
package io.github.gatling.cql.request

import com.datastax.oss.driver.api.core.cql._
import io.gatling.commons.stats.KO
import io.gatling.commons.util.Clock
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.github.gatling.cql.response.CqlResponseHandler

class CqlRequestAction(val name: String, val next: Action, components: CqlComponents, attr: CqlAttributes) extends ExitableAction { //TODO: Reimplement as Request action?

  override def execute(session: Session): Unit = {
    val stmt = attr.statement(session)

    stmt.onFailure(err => {
      statsEngine.logResponse(session.scenario, session.groups, name, clock.nowMillis, clock.nowMillis, KO, None, Some("Error" +
        " setting up statement: " + err))
      next ! session.markAsFailed
    })

    stmt.onSuccess({ stmt =>
      val start = clock.nowMillis
      val result = components.cqlProtocol.session.executeAsync(applyAttributes(stmt).build())
      val handler = new CqlResponseHandler(next, session, components, start, attr.tag, stmt, attr.checks)
      result.whenCompleteAsync((r: AsyncResultSet, e: Throwable) => {
        if (e != null) handler.onFailure(e)
        else handler.onSuccess(r)
      })
    })
  }

  override def clock: Clock = components.coreComponents.clock

  override def statsEngine: StatsEngine = components.coreComponents.statsEngine

  // java types for Statement classes are somewhat crazy, so this is the only why I've found to deal with those in Scala
  // For adventurous: try to annotate this function with a Scala type
  private def applyAttributes(s: Statement[_]) = s match {
    case simple: SimpleStatement => new SimpleStatementBuilder(simple).setConsistencyLevel(attr.cl).setSerialConsistencyLevel(attr.serialCl)
    case bound: BoundStatement => new BoundStatementBuilder(bound).setConsistencyLevel(attr.cl).setSerialConsistencyLevel(attr.serialCl)
  }
}
