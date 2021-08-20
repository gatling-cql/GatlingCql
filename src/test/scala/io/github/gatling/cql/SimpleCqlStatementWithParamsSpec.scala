package io.github.gatling.cql

import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.core.session.el._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SimpleCqlStatementWithParamsSpec extends AnyFlatSpec with Matchers {
  val query = "INSERT INTO ${table_name} (id, num, str) values (now(), ?, ?)".el[String]
  val params = "${arrayOfParams}".el[Seq[AnyRef]]
  val target = SimpleCqlStatementWithParams(query, params)

  "SimpleCqlStatementWithParams" should "correctly return SimpleStatement for a valid expression with params" in {
    //given
    val targetArray = List(5, "foo", null)
    val session = Session("name", 1, null)
      .set("table_name", "fooTable")
      .set("arrayOfParams", targetArray)

    //when
    val result = target(session)

    //then
    result shouldBe a[Success[_]]
    val stmt = result.toOption.get
    stmt.getQuery shouldBe "INSERT INTO fooTable (id, num, str) values (now(), ?, ?)"
    stmt.getPositionalValues should have size 3
    stmt.getPositionalValues should contain theSameElementsAs targetArray
  }

  it should "fail if the expression is wrong" in {
    //given
    val session = Session("name", 1, null).set("test2", "5")

    //then
    target(session) shouldBe "No attribute named 'table_name' is defined".failure
  }

  it should "fail if params are wrong" in {
    //given
    val session = Session("name", 1, null).set("table_name", "fooTable")

    //then
    target(session) shouldBe "No attribute named 'arrayOfParams' is defined".failure
  }
}
