package controllers

import java.sql.Date
import java.util.Calendar
import javax.inject.Inject
import models.{UserRepo, User}
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.I18nSupport
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

case class Auth(email: String, password: String)

class Authentication @Inject()
  (implicit ec: ExecutionContext, 
  userRepo: UserRepo, 
  val controllerComponents: ControllerComponents) 
  extends BaseController with I18nSupport {

  val authForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText)
    (Auth.apply)(Auth.unapply)
  )

    def create() = Action {  implicit request =>
      Ok(views.html.users.create(authForm))
    }

    def login() = Action { implicit request => 
      Ok(views.html.users.login(authForm))  
    }

    def logout() = Action { implicit request  =>
      Redirect(routes.Instrument.listEntries).withNewSession
    }

    def submit() = Action.async { implicit request =>
      authForm.bindFromRequest.fold(
          formWithErrors => { 
            Future(Ok("wrong")) 
          },
          user => { userRepo.create(user.email, user.password) 
        .map(id => Redirect(routes.Instrument.listEntries))
          }
      )
    }

    def authenticate() = Action.async { implicit request =>
      authForm.bindFromRequest.fold(
          formWithErrors => {
            Future(Ok("wrong"))
          },
          user => {
            userRepo.authenticate(user.email, user.password) match {
              case true => Future(Redirect(routes.Instrument.listEntries)
                .withSession("session-token" -> "true"))
              case false => Future(Ok("false"))
            }
          } 
      )
    }
}

