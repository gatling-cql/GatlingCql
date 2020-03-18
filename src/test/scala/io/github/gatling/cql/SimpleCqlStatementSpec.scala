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

import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.core.session.el.ElCompiler
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SimpleCqlStatementSpec extends AnyFlatSpec with Matchers {
    val el = ElCompiler.compile[String]("select * from test where id = ${test}")
    val target = SimpleCqlStatement(el)
    
    "SimpleCqlStatement" should "correctly return SimpleStatement for a valid expression" in {
      val session = new Session("name", 1, System.currentTimeMillis, Map("test" -> "5"))
      val result = target(session) 
      result shouldBe a[Success[_]]
      //result.get.getQueryString() shouldBe "select * from test where id = 5"
      //NOTE: Statement.getQueryString() no longer present in 3.0.2
    }
    
    it should "fail if the expression is wrong" in {
      val session = new Session("name", 1, System.currentTimeMillis, Map("test2" -> "5"))
      target(session) shouldBe "No attribute named 'test' is defined".failure 
    }

}
