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
package io.github.gatling.cql.checks

import com.datastax.driver.core.{ExecutionInfo, ResultSet}
import io.gatling.commons.validation.{SuccessWrapper, Validation}
import io.gatling.core.check.extractor.{Extractor, SingleArity}
import io.gatling.core.check.{FindCheckBuilder, ValidatorCheckBuilder}
import io.gatling.core.session.{Expression, ExpressionSuccessWrapper}
import io.github.gatling.cql.response.CqlResponse

class CqlResponseFindCheckBuilder[X](extractor: Expression[Extractor[CqlResponse, X]])
  extends FindCheckBuilder[CqlCheckType, CqlResponse, X] {

  def find: ValidatorCheckBuilder[CqlCheckType, CqlResponse, X] = ValidatorCheckBuilder(extractor, true)
}

object CqlCheckBuilder {

  val ExecutionInfoExtractor = new Extractor[CqlResponse, ExecutionInfo] with SingleArity {
    val name = "executionInfo"
    def apply(prepared: CqlResponse): Validation[Option[ExecutionInfo]] = {
      Some(prepared.resultSet.getExecutionInfo).success
    }
  }.expressionSuccess

  val ExecutionInfo = new CqlResponseFindCheckBuilder[ExecutionInfo](ExecutionInfoExtractor)



  val ResultSetExtractor = new Extractor[CqlResponse, ResultSet] with SingleArity {
    val name = "resultSet"
    def apply(prepared: CqlResponse): Validation[Option[ResultSet]] = {
      Some(prepared.resultSet).success
    }
  }.expressionSuccess

  val ResultSet = new CqlResponseFindCheckBuilder[ResultSet](ResultSetExtractor)



  val RowCountExtractor = new Extractor[CqlResponse, Int] with SingleArity {
    val name = "rowCount"
    def apply(prepared: CqlResponse): Validation[Option[Int]] = {
      Some(prepared.rowCount).success
    }
  }.expressionSuccess

  val RowCount = new CqlResponseFindCheckBuilder[Int](RowCountExtractor)

  val AppliedExtractor = new Extractor[CqlResponse, Boolean] with SingleArity {
    val name = "applied"
    def apply(prepared: CqlResponse): Validation[Option[Boolean]] = {
      Some(prepared.resultSet.wasApplied()).success
    }
  }.expressionSuccess

  val Applied = new CqlResponseFindCheckBuilder[Boolean](AppliedExtractor)



  val ExhaustedExtractor = new Extractor[CqlResponse, Boolean] with SingleArity {
    val name = "exhausted"
    def apply(prepared: CqlResponse): Validation[Option[Boolean]] = {
      Some(prepared.resultSet.isExhausted).success
    }
  }.expressionSuccess

  val Exhausted = new CqlResponseFindCheckBuilder[Boolean](ExhaustedExtractor)
}

