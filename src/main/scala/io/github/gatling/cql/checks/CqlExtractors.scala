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
package io.github.gatling.cql.checks

import com.datastax.driver.core.{ExecutionInfo, ResultSet}
import io.gatling.commons.validation._
import io.gatling.core.check._
import io.github.gatling.cql.response.CqlResponse

object CqlExtractors {

  private[checks] type CqlExtractor[X] = Extractor[CqlResponse, X]

  /*Extract a field from CqlResponse */
  private type CqlResponseExtractor[X] = FindExtractor[CqlResponse, X]

  private[checks] val ExecutionInfoExtractor = new CqlResponseExtractor[ExecutionInfo](
    "executionInfo",
    prepared => Some(prepared.resultSet.getExecutionInfo).success
  )

  private[checks] val ResultSetExtractor = new CqlResponseExtractor[ResultSet](
    "resultSet",
    prepared => Some(prepared.resultSet).success
  )

  private[checks] val RowCountExtractor = new CqlResponseExtractor[Int](
    "rowCount",
    prepared => Some(prepared.rowCount).success
  )

  private[checks] val AppliedExtractor = new CqlResponseExtractor[Boolean](
    "applied",
    prepared => Some(prepared.resultSet.wasApplied()).success
  )

  private[checks] val ExhaustedExtractor = new CqlResponseExtractor[Boolean](
    "exhausted",
    prepared => Some(prepared.resultSet.isExhausted).success
  )

  /* Extract a record from CqlResponse */
  private type CqlColumnValueExtractor[X] = CriterionExtractor[CqlResponse, String, X]

  private[checks] def singleRecordExtractor(columnName: String, occurrence: Int): CqlColumnValueExtractor[Any] =
    new FindCriterionExtractor(
      "columnValue",
      columnName,
      occurrence,
      prepared => prepared.column(columnName).lift(occurrence).success
    )

  private[checks] def allRecordsExtractor(columnName: String): CqlColumnValueExtractor[Seq[Any]] =
    new FindAllCriterionExtractor(
      "columnValue",
      columnName,
      prepared => prepared.column(columnName).liftSeqOption.success
    )

  private[checks] def countRecordsExtractor(columnName: String): CqlColumnValueExtractor[Int] =
    new CountCriterionExtractor(
      "columnValue",
      columnName,
      prepared => prepared.column(columnName).liftSeqOption.map(_.size).success
    )
}


