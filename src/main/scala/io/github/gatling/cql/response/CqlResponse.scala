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
package io.github.gatling.cql.response

import com.datastax.oss.driver.api.core.cql.{ResultSet, Row}

import scala.jdk.CollectionConverters._

/**
 * A holder for a [[com.datastax.oss.driver.api.core.cql.ResultSet resultSet]] which provides
 * a couple of convenient functions
 */
case class CqlResponse(resultSet: ResultSet) {

  // implicit cache of all rows of the result set
  private lazy val allRows: Seq[Row] = resultSet.all().asScala.toSeq

  /**
   * Get the number of all rows returned by the CQL statement.
   * Note that this statement implicitly fetches '''all''' rows from the result set!
   */
  def rowCount: Int = allRows.length

  /**
   * Get a column by name returned by the CQL statement.
   * Note that this statement implicitly fetches '''all''' rows from the result set!
   */
  def column(name: String): Seq[Any] = {
    allRows.flatMap( row => {
      val idx = row.getColumnDefinitions.firstIndexOf(name)
      // idx == -1 means: "column not in result set"
      if (idx == -1 || row.isNull(idx))
        None
      else
        Some(row.getObject(idx))
    })
  }
}
