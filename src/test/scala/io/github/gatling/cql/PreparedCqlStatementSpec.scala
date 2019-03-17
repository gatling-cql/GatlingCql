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
package io.github.gatling.cql

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.PreparedStatement
import io.gatling.commons.validation._
import io.gatling.core.session.el.ElCompiler
import io.gatling.core.session.Session
import org.easymock.EasyMock._
import org.scalatestplus.easymock.EasyMockSugar

class PreparedCqlStatementSpec extends FlatSpec with EasyMockSugar with Matchers with BeforeAndAfter {
  val e1 = ElCompiler.compile[AnyRef]("${foo}")
  val e2 = ElCompiler.compile[AnyRef]("${baz}")
  val prepared = mock[PreparedStatement]
  val target = BoundCqlStatement(prepared, e1, e2)

  before {
    reset(prepared)
  }

  "BoundCqlStatement" should "correctly bind values to a prepared statement" in {
    val session = new Session("name", 1, System.currentTimeMillis, Map("foo" -> Integer.valueOf(5), "baz" -> "BaZ"))
    expecting {
      prepared.bind(Integer.valueOf(5), "BaZ").andReturn(mock[BoundStatement])
    }
    whenExecuting(prepared) {
      target(session) shouldBe a[Success[_]]
    }
  }

  it should "fail if the expression is wrong and return the 1st error" in {
    val session = new Session("name", 1, System.currentTimeMillis, Map("fu" -> Integer.valueOf(5), "buz" -> "BaZ"))
    target(session) shouldBe "No attribute named 'foo' is defined".failure
  }

  it should "handle null parameters correctly" in {
    val session = new Session("name", 1, System.currentTimeMillis)
    val statementWithNull = BoundCqlStatement(prepared, null)

    statementWithNull(session) shouldBe a[Success[_]]
  }
}
