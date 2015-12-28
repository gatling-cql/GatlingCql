/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Mikhail Stepura
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
import java.util.concurrent.{ThreadFactory, Executors}

import com.google.common.util.concurrent.Futures

import akka.actor.ActorRef
import io.gatling.core.action.Failable
import io.gatling.core.action.Interruptable
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.Validation

object CqlRequestAction {
  lazy val executor = Executors.newCachedThreadPool(new ThreadFactory {
    val threadNum: AtomicInteger = new AtomicInteger()
    override def newThread(r: Runnable): Thread = {
      val thread = new Thread(r, "cql-request#" + threadNum.incrementAndGet())
      thread.setDaemon(true)
      thread
    }
  })
}

class CqlRequestAction(val next: ActorRef, protocol: CqlProtocol, attr: CqlAttributes)
  extends Interruptable
  with Failable {

  def executeOrFail(session: Session): Validation[Unit] = {
    val start = nowMillis
    val stmt = attr.statement(session)
    stmt.map{ stmt =>
      stmt.setConsistencyLevel(attr.cl)
      val result = protocol.session.executeAsync(stmt)
      Futures.addCallback(result, new CqlResponseHandler(next, session, start, attr.tag, stmt, attr.checks, attr.postProcessors), CqlRequestAction.executor)
    }
  }
}
