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

import java.util

import com.datastax.driver.core.{ExecutionInfo, ResultSet}
import io.gatling.core.session._
import io.gatling.commons.validation._
import io.gatling.core.check.extractor.{Extractor, SingleArity}
import io.gatling.core.check.{CheckResult, DefaultMultipleFindCheckBuilder, FindCheckBuilder, Matcher, Validator, ValidatorCheckBuilder}
import io.github.gatling.cql.response.CqlResponse

class CqlCheckBuilder[X](extractor: Expression[Extractor[CqlResponse, X]]) extends FindCheckBuilder[CqlCheckType,
  CqlResponse, X] {

  def find: ValidatorCheckBuilder[CqlCheckType, CqlResponse, X] = ValidatorCheckBuilder(extractor, displayActualValue = true)

  def satisfies(predicate: X => Boolean) = find.validate(CqlCheckBuilder.satisfies(predicate))
}

object CqlCheckBuilder {

  private val ExecutionInfoExtractor = new Extractor[CqlResponse, ExecutionInfo] with SingleArity {
    val name = "executionInfo"

    def apply(prepared: CqlResponse): Validation[Option[ExecutionInfo]] = {
      Some(prepared.resultSet.getExecutionInfo).success
    }
  }.expressionSuccess

  private val ResultSetExtractor = new Extractor[CqlResponse, ResultSet] with SingleArity {
    val name = "resultSet"

    def apply(prepared: CqlResponse): Validation[Option[ResultSet]] = {
      Some(prepared.resultSet).success
    }
  }.expressionSuccess

  private val RowCountExtractor = new Extractor[CqlResponse, Int] with SingleArity {
    val name = "rowCount"

    def apply(prepared: CqlResponse): Validation[Option[Int]] = {
      Some(prepared.rowCount).success
    }
  }.expressionSuccess

  private val AppliedExtractor = new Extractor[CqlResponse, Boolean] with SingleArity {
    val name = "applied"

    def apply(prepared: CqlResponse): Validation[Option[Boolean]] = {
      Some(prepared.resultSet.wasApplied()).success
    }
  }.expressionSuccess

  private val ExhaustedExtractor = new Extractor[CqlResponse, Boolean] with SingleArity {
    val name = "exhausted"

    def apply(prepared: CqlResponse): Validation[Option[Boolean]] = {
      Some(prepared.resultSet.isExhausted).success
    }
  }.expressionSuccess

  private def satisfies[X](predicate: X => Boolean): Expression[Validator[X]] = new Matcher[X] {
    val name = "predicate"

    override protected def doMatch(actual: Option[X]): Validation[Option[X]] = actual match {
      case Some(r) =>
        if (predicate(r)) actual.success
        else s"{$r} doesn't satisfy the predicate".failure
      case _ => Validator.FoundNothingFailure
    }
  }.expressionSuccess

  val ExecutionInfo = new CqlCheckBuilder[ExecutionInfo](ExecutionInfoExtractor)
  val ResultSet = new CqlCheckBuilder[ResultSet](ResultSetExtractor)
  val RowCount = new CqlCheckBuilder[Int](RowCountExtractor)
  val Applied = new CqlCheckBuilder[Boolean](AppliedExtractor)
  val Exhausted = new CqlCheckBuilder[Boolean](ExhaustedExtractor)

  /**
    * Get a column by name returned by the CQL statement.
    * Note that this statement implicitly fetches <b>all</b> rows from the result set!
    */

  def columnValue(columnName: Expression[String]) = new DefaultMultipleFindCheckBuilder[CqlCheckType, CqlResponse, Any](true) {
      def findExtractor(occurrence: Int) = columnName.map(new SingleColumnValueExtractor(_, occurrence))

      def findAllExtractor = columnName.map(new MultipleColumnValueExtractor(_))

      def countExtractor = columnName.map(new CountColumnValueExtractor(_))
    }

  def simpleCheck(predicate: ResultSet => Boolean): CqlCheck = new CqlCheck {
    override def check(response: CqlResponse, session: Session)(implicit preparedCache: util.Map[Any, Any]): Validation[CheckResult] = {
      if (predicate(response.resultSet)) CheckResult.NoopCheckResultSuccess
      else "CQL check failed".failure
    }
  }
}

