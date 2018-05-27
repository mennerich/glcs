package controllers

import java.sql.Date
import java.util.Calendar
import javax.inject.Inject
import models.{EntryRepo, Entry, SessionKeyRepo }
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.I18nSupport
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

case class EntryData(reading: Int, nutrition: Int, readingTime: Int, readingDate: String, exercise: Boolean)

class Instrument @Inject()
  (implicit ec: ExecutionContext, 
  entryRepo: EntryRepo, 
  sessionKeyRepo: SessionKeyRepo,
  val controllerComponents: ControllerComponents) 
  extends BaseController with I18nSupport {

  val entryForm = Form(
    mapping(
      "reading" -> number,
      "nutrition" -> number,
      "readingTime" -> number,
      "readingDate" -> nonEmptyText,
      "exercise" -> boolean)
    (EntryData.apply)(EntryData.unapply)
  )
  
  def create() = Action {  implicit request =>
    request.session.get("glcs-session").map { sessionKey =>
      sessionKeyRepo.keyExists(sessionKey) match {
        case true => Ok(views.html.create(entryForm))
        case false => Redirect(routes.Instrument.listEntries).flashing("error" -> "you must be logged in to create an entry")
      }
    }.getOrElse {
        Redirect(routes.Instrument.listEntries).flashing("error" -> "you must be logged in to create an entry")
      }
  }

  def submit() = Action.async { implicit request => 
    val now = new java.sql.Date(Calendar.getInstance().getTime().getTime())

    entryForm.bindFromRequest.fold(
      formWithErrors => { 
        Future(Ok("wrong")) 
      },
      entry => { 
        val t = entry.readingDate.split(" ")(0).split("/")
        println(entry.exercise)
        entryRepo.create(
          entry.reading, 
          entry.nutrition, 
          entry.readingTime, 
          Date.valueOf(t(2) + "-" + t(0) + "-" + t(1)),
          entry.exercise
        )
        .map(id => Redirect(routes.Instrument.listEntries).flashing("success" -> "glcs entry created"))
      }
    )
  }

  def listEntries = Action.async { implicit request =>
    
    request.session.get("glcs-session").map { sessionKey =>
      val sessionValid = sessionKeyRepo.keyExists(sessionKey)
      entryRepo.all
       .map(entries => Ok(views.html.entries(entries, sessionValid)))
    }.getOrElse {
      entryRepo.all
       .map(entries => Ok(views.html.entries(entries, false)))
    }
  }

  def delete(id: Long) = Action.async { implicit request =>
    entryRepo.delete(id)
    Future(Redirect(routes.Instrument.listEntries).flashing("success" -> "entry deleted"))
  }
  
}
