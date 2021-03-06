package models


import java.sql.Date
import java.util.Calendar

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
  val now = Date.valueOf("1977-01-26")

  override def afterEach() = EvolutionHelper.clean()

  "An item " should {

    "be inserted during the first test case" in  {
        val action = entryRepo.create(Entry(1, 131, 1, 1, now, true, 1, Some(100))).flatMap(_ => entryRepo.all)
        val result = Await.result(action, Duration.Inf)
        result mustBe List(Entry(1, 131, 1, 1, now, true, 1, Some(100)))
    }

    "and not exist in the second test case" in  {
        val action = entryRepo.all
        val result = Await.result(action, Duration.Inf)
        result mustBe List.empty
    }

    "and be updated in the third test case" in {
      val create = entryRepo.create(Entry(1, 131, 1, 1, now, true, 1, Some(100)))
      Await.result(create, Duration.Inf)
      val update = entryRepo.update(Entry(1, 231, 1, 1, now, true, 1, Some(100))).flatMap(_ => entryRepo.all)
      val result = Await.result(update, Duration.Inf)
      result mustBe List(Entry(1, 231, 1, 1, now, true, 1, Some(100)))
    }

    "and be added and deleted in the fourth case" in {
        val action = entryRepo.create(Entry(1, 131, 1, 1, now, true, 1, Some(100)))
          .flatMap(_ => entryRepo.all)

        Await.result(action, Duration.Inf)
        
        Await.result(entryRepo.delete(1), Duration.Inf)

        Await.result(entryRepo.all, Duration.Inf) mustBe List.empty
    }
  }

}
