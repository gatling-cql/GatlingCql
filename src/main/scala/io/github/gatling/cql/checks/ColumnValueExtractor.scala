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

import io.gatling.commons.validation.{SuccessWrapper, Validation}
import io.gatling.core.check.extractor._
import io.github.gatling.cql.response.CqlResponse


abstract class ColumnValueExtractor[X] extends CriterionExtractor[CqlResponse, Any, X] {
  val criterionName = "columnValue"
}

class SingleColumnValueExtractor(val criterion: String, val occurrence: Int) extends ColumnValueExtractor[Any] with FindArity {

  def apply(prepared: CqlResponse): Validation[Option[Any]] =
    prepared.column(criterion).lift(occurrence).success
}

class MultipleColumnValueExtractor(val criterion: String) extends ColumnValueExtractor[Seq[Any]] with FindAllArity {

  def apply(prepared: CqlResponse): Validation[Option[Seq[Any]]] =
    prepared.column(criterion).liftSeqOption.success
}

class CountColumnValueExtractor(val criterion: String) extends ColumnValueExtractor[Int] with CountArity {

  def apply(prepared: CqlResponse): Validation[Option[Int]] =
    prepared.column(criterion).liftSeqOption.map(_.size).success
}
