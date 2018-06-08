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

class UserTestSpec extends PlaySpec with GuiceOneAppPerTest  with BeforeAndAfterEach {

  val userRepo = Injector.inject[UserRepo]

  override def afterEach() = EvolutionHelper.clean()

  "A user " should {

    "be inserted during the first test case" in  {
        val action = userRepo.create("email@example.com", "password", "anonymous")
          .flatMap(_ => userRepo.all)

        val result = Await.result(action, Duration.Inf)

        result mustBe List(User(1, "email@example.com", DigestUtils.md5Hex("password" + result(0).salt), result(0).salt, "anonymous"))
    }
  }

  "A user " should {
    "authenticate during the second test case" in {
      val create = userRepo.create("email@example.com", "password", "anonymous")
      Await.result(create, Duration.Inf)

      userRepo.authenticate("email@example.com", "password") match {
        case Some(key) => true
        case None => false
      }
    }
  }

  "A user record " should {
    "be updated during the third test case" in {
      val create = userRepo.create("email@example.com", "password", "anonymous")
      Await.result(create, Duration.Inf)
      
      val updateUser = userRepo.update(UserForm("email@example.com", "password2", "anonymous2"))
      Await.result(updateUser, Duration.Inf)

      val findUser = userRepo.findByEmail("email@example.com") 
      val result = Await.result(findUser, Duration.Inf)

      result match {
        case Some(s) => {
          s.nick == "anonymous2"
        }
        case None => false
      }
    }
  }
}