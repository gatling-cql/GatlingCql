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
package io.github.gatling.cql.checks

import com.datastax.oss.driver.api.core.cql.{ExecutionInfo, ResultSet, Row}
import io.gatling.commons.validation.Validation.NoneSuccess
import io.gatling.commons.validation._
import io.github.gatling.cql.checks.CqlExtractors._
import io.github.gatling.cql.response.CqlResponse
import org.mockito.BDDMockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.mockito.MockitoSugar

import scala.jdk.CollectionConverters._

class CqlExtractorsSpec extends AnyFlatSpec with MockitoSugar with Matchers {

  val rs: ResultSet = mock[ResultSet]
  val ei: ExecutionInfo = mock[ExecutionInfo]
  val target: CqlResponse = mock[CqlResponse]
  val rows = List(1, 2, 3, 4, 5)
  val column = "foo"

  "ExecutionInfoExtractor" should "correctly extract ExecutionInfo" in {
    given(rs.getExecutionInfo).willReturn(ei)

    //then
    ExecutionInfoExtractor(CqlResponse(rs)) shouldBe Some(ei).success
  }

  "RowCountExtractor" should "correctly extract RowCount" in {
    given(rs.all()).willReturn(List(mock[Row]).asJava)

    //then
    RowCountExtractor(CqlResponse(rs)) shouldBe Some(1).success
  }

  "ResultSetExtractor" should "correctly extract ResultSet" in {
    ResultSetExtractor(CqlResponse(rs)) shouldBe Some(rs).success
  }

  "singleRecordExtractor" should "correctly extract a single record" in {
    given(target.column(column)).willReturn(rows)

    //then
    singleRecordExtractor(column, 3)(target) shouldBe Some(rows(3)).success
  }

  "allRecordsExtractor" should "correctly return all records" in {
    given(target.column(column)).willReturn(rows)

    //then
    allRecordsExtractor(column)(target) shouldBe Some(rows).success
  }

  "countRecordsExtractor" should "correctly extract a count of records" in {
    given(target.column(column)).willReturn(rows)

    //then
    countRecordsExtractor(column)(target) shouldBe Some(rows.length).success
  }

  "singleColumnValueExtractor" should "not fail if a column is not in the result" in {
    given(target.column(column)).willReturn(Nil)

    //then
    singleRecordExtractor(column, 3)(target) shouldBe NoneSuccess
  }

  "allRecordsExtractor" should "not fail if a column is not in the result" in {
    given(target.column(column)).willReturn(Nil)

    //then
    allRecordsExtractor(column)(target) shouldBe NoneSuccess
  }

  "countRecordsExtractor" should "not fail if a column is not in the result" in {
    given(target.column(column)).willReturn(Nil)

    //then
    countRecordsExtractor(column)(target) shouldBe NoneSuccess
  }
}
