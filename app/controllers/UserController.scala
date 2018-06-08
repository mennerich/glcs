package controllers

import java.sql.Date
import java.util.Calendar
import javax.inject.Inject
import models.{UserRepo, UserForm, User, SessionKeyRepo }
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
  sessionKeyRepo: SessionKeyRepo,
  val controllerComponents: ControllerComponents) 
  extends BaseController with I18nSupport {

  val authForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText)
    (Auth.apply)(Auth.unapply)
  )

  val userForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText,
      "nick" -> nonEmptyText)
    (UserForm.apply)(UserForm.unapply)
  )

  def create() = Action {  implicit request =>
    request.session.get("glcs-session").map { sessionKey =>
      sessionKeyRepo.keyExists(sessionKey) match {
        case true => Ok(views.html.users.create(userForm))
        case false => Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "you must be logged in to create an user")
      }
    }.getOrElse {
      Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "you must be logged in to create a user")
    }
  }

  def submit() = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
        formWithErrors => { 
          Future(Ok("wrong")) 
        },
        user => { userRepo.create(user.email, user.password, user.nick) 
          .map(id => Redirect(routes.Instrument.listEntries(0)).flashing("success" -> "user created"))
        }
    )
  }

  def edit() = Action { implicit request  =>
    request.session.get("glcs-session").map { sessionKey =>
      sessionKeyRepo.keyExists(sessionKey) match {
        case true => Ok(views.html.users.edit(userForm))
        case false => Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "you must be logged in to create an user")
      }
    }.getOrElse {
      Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "you must be logged in to create a user")
    }
  }

  def update() = Action.async { implicit request => 
    userForm.bindFromRequest.fold(
      formWithErrors => { 
          Future(Ok("wrong")) 
      },
      user => { userRepo.update(user) 
        .map(id => Redirect(routes.Instrument.listEntries(0)).flashing("success" -> "user updated"))
      }
    )
  }

  def login() = Action { implicit request => 
    Ok(views.html.users.login(authForm))  
  }

  def logout() = Action { implicit request  =>
    request.session.get("glcs-session").map { session =>
      sessionKeyRepo.delete(session)
      Redirect(routes.Instrument.listEntries(0)).withNewSession
    }.getOrElse {
      Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "session not available")
    }    
  }

  def authenticate() = Action.async { implicit request =>
    authForm.bindFromRequest.fold(
      formWithErrors => {
        Future(Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "invalid form"))
      },
      user => {
        userRepo.authenticate(user.email, user.password) match {
          case Some(s) => {
            Future(Redirect(routes.Instrument.listEntries(0))
              .withSession("glcs-session" -> s)
              .flashing("success" -> "successfuly logged in")
            )
          }
          case None => Future(Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "login failed"))
        }
      } 
    )
  }
}

