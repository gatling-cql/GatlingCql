/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 GatlingCql developers
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

import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}
import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.core.session.el._
import org.mockito.BDDMockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.mockito.MockitoSugar

class PreparedCqlStatementSpec extends AnyFlatSpec with MockitoSugar with Matchers {
  val e1 = "${foo}".el[AnyRef]
  val e2 = "${baz}".el[AnyRef]
  val prepared = mock[PreparedStatement]
  val target = BoundCqlStatement(prepared, e1, e2)

  "BoundCqlStatement" should "correctly bind values to a prepared statement" in {
    //given
    val session = Session("name", 1, null).set("foo", Integer.valueOf(5)).set("baz", "BaZ")
    given(prepared.bind(Integer.valueOf(5), "BaZ")).willReturn(mock[BoundStatement])

    //then
    target(session) shouldBe a[Success[_]]
  }

  it should "fail if the expression is wrong and return the 1st error" in {
    //given
    val session = Session("name", 1, null).set("fu", Integer.valueOf(5)).set("buz", "BaZ")

    //then
    target(session) shouldBe "No attribute named 'foo' is defined".failure
  }

  it should "handle null parameters correctly" in {
    //given
    val session = Session("name", 1, null)
    val statementWithNull = BoundCqlStatement(prepared, null)

    //then
    statementWithNull(session) shouldBe a[Success[_]]
  }
}
