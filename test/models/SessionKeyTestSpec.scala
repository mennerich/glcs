package models

import java.sql.Date
import java.util.Calendar
import org.apache.commons.codec.digest.DigestUtils
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

class SessionKeyTestSpec extends PlaySpec with GuiceOneAppPerTest  with BeforeAndAfterEach {

  val sessionKeyRepo = Injector.inject[SessionKeyRepo]

  override def afterEach() = EvolutionHelper.clean()

  "A session key " should {
    "be inserted during the first test case" in  {
      val action = sessionKeyRepo.create(1).flatMap(_ => sessionKeyRepo.all)
      val result = Await.result(action, Duration.Inf)
      result(0).id mustBe 1
    }

    "and be deleted during the second test case" in {
      val action = sessionKeyRepo.delete(1).flatMap(_ => sessionKeyRepo.all)
      val result = Await.result(action, Duration.Inf)
      result.size mustBe 0
    }
  }
}