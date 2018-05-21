package models

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Helpers._
import play.api.test._
import testhelpers.{EvolutionHelper, Injector}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class EntryModelTestSpec extends PlaySpec with GuiceOneAppPerTest  with BeforeAndAfterEach {

  val entryRepo = Injector.inject[EntryRepo]

  override def afterEach() = EvolutionHelper.clean()

  "An item " should {

    "be inserted during the first test case" in  {
        val action = entryRepo.create(131, "poor", 1)
          .flatMap(_ => entryRepo.all)

        val result = Await.result(action, Duration.Inf)

        result mustBe List(Entry(1, 131, "poor", 1))
    }
  }

}
