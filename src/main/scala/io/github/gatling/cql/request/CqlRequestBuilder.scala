/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 GatlingCql developers
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

import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Expression
import io.github.gatling.cql.checks.CqlCheck
import io.github.gatling.cql.{BoundCqlStatement, SimpleCqlStatement, SimpleCqlStatementWithParams}


case class CqlRequestBuilderBase(tag: String) {
  /**
   * Executes a simple statement without parameters
   * */
  def execute(statement: Expression[String]) = CqlRequestBuilder(CqlAttributes(tag, SimpleCqlStatement(statement)))

  /**
   * Executes a simple statement with parameters
   * */
  def execute(statement: Expression[String], params: Expression[Seq[AnyRef]]) = CqlRequestBuilder(CqlAttributes(tag, SimpleCqlStatementWithParams(statement, params)))
  /**
   * Executes an instance of [[com.datastax.oss.driver.api.core.cql.PreparedStatement]]
   * */
  def execute(prepared: PreparedStatement) = CqlPreparedRequestParamsBuilder(tag, prepared)
}

case class CqlPreparedRequestParamsBuilder(tag: String, prepared: PreparedStatement) {
  /**
   * Provides positional parameters for [[com.datastax.oss.driver.api.core.cql.PreparedStatement]]
   * */
  def withParams(params: Expression[AnyRef]*) = CqlRequestBuilder(CqlAttributes(tag, BoundCqlStatement(prepared, params: _*)))
}

case class CqlRequestBuilder(attr: CqlAttributes) {
  /**
   * Sets `consistencyLevel` ([[com.datastax.oss.driver.api.core.ConsistencyLevel]] )for the action
   * */
  def consistencyLevel(level: ConsistencyLevel) = CqlRequestBuilder(attr.copy(cl= level))

  /**
   * Sets `serialConsistencyLevel` ([[com.datastax.oss.driver.api.core.ConsistencyLevel]]) for the action
   * */
  def serialConsistencyLevel(level: ConsistencyLevel) = CqlRequestBuilder(attr.copy(serialCl= level))

  def build(): ActionBuilder = new CqlRequestActionBuilder(attr)

  /**
   * Stops defining the request and adds [[io.github.gatling.cql.checks#CqlCheck CqlCheck]]s on the response
   *
   * @param checks a sequence of the [[io.github.gatling.cql.checks#CqlCheck CqlCheck]]s which will be
   *               performed on the
   *               response
   */
  def check(checks: CqlCheck*): CqlRequestBuilder = CqlRequestBuilder(attr.copy(checks = attr.checks ::: checks.toList))
}
