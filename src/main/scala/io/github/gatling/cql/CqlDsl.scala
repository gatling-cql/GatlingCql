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
package io.github.gatling.cql

import io.gatling.core.action.builder.ActionBuilder
import io.github.gatling.cql.checks.CqlCheckSupport
import io.github.gatling.cql.request.{CqlProtocol, CqlProtocolBuilder, CqlRequestBuilder, CqlRequestBuilderBase}

trait CqlDsl extends CqlCheckSupport {
  val cql = CqlProtocolBuilder

  def cql(tag: String) = CqlRequestBuilderBase(tag)

  implicit def cqlProtocolBuilder2cqlProtocol(builder: CqlProtocolBuilder): CqlProtocol = builder.build
  implicit def cqlRequestBuilder2ActionBuilder(builder: CqlRequestBuilder): ActionBuilder = builder.build()
}
