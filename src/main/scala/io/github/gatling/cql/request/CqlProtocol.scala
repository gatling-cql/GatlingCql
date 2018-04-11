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
package io.github.gatling.cql.request

import akka.actor.ActorSystem
import com.datastax.driver.core.{Session => CqlSession}
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{Protocol, ProtocolComponents, ProtocolKey}
import io.gatling.core.session.Session

object CqlProtocol {
  val CqlProtocolKey = new ProtocolKey {

    type Protocol = CqlProtocol
    type Components = CqlComponents

    def protocolClass: Class[io.gatling.core.protocol.Protocol] = classOf[CqlProtocol].asInstanceOf[Class[io.gatling.core.protocol.Protocol]]

    def defaultProtocolValue(configuration: GatlingConfiguration): CqlProtocol = throw new IllegalStateException("Can't provide a default value for CqlProtocol")

    def newComponents(system: ActorSystem, coreComponents: CoreComponents): CqlProtocol => CqlComponents = cqlProtocol => CqlComponents(cqlProtocol)
  }
}

//holds reference to a cluster, just settings
case class CqlProtocol(session: CqlSession) extends Protocol

case class CqlComponents(cqlProtocol: CqlProtocol) extends ProtocolComponents {

  def onStart: Option[Session => Session] = None

  def onExit: Option[Session => Unit] = None
}
