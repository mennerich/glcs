package controllers

import helpers.{ StatsSupport, Averages, SlackSupport }
import java.sql.Date
import java.util.Calendar
import javax.inject.Inject
import models.{ EntryRepo, Entry, EntryData, SessionKeyRepo }
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.I18nSupport
import play.api.inject.ApplicationLifecycle
import play.api.Configuration
import play.api.libs.ws._
import play.api.Logger
import play.api.mvc._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.Duration

class Instrument @Inject()
  (implicit ec: ExecutionContext, 
  entryRepo: EntryRepo, 
  sessionKeyRepo: SessionKeyRepo,
  ws: WSClient,
  config: Configuration,
  lifecycle: ApplicationLifecycle,
  val controllerComponents: ControllerComponents) 
  extends BaseController 
  with I18nSupport 
  with StatsSupport 
  with SlackSupport {

  lifecycle.addStopHook { () =>
    Future.successful(Logger.info("Dumping Session Table"))
    Future.successful(sessionKeyRepo.deleteAll)
  }

  val entryForm = Form(
    mapping(
      "reading" -> number,
      "nutrition" -> number,
      "readingTime" -> number,
      "readingDate" -> nonEmptyText,
      "exercise" -> boolean )
    (EntryData.apply)(EntryData.unapply)
  )
  
  def create() = Action {  implicit request =>
    request.session.get("glcs-session").map { sessionKey =>
      sessionKeyRepo.keyExists(sessionKey) match {
        case true => {
          Ok(views.html.create(entryForm))
        }
        case false => {
          Redirect(routes.Instrument.listEntries).flashing("error" -> "you must be logged in to create an entry")
        }
      }
    }.getOrElse {
        Redirect(routes.Instrument.listEntries).flashing("error" -> "you must be logged in to create an entry")
    }
  }

  def submit() = Action.async { implicit request => 
    val now = new java.sql.Date(Calendar.getInstance().getTime().getTime())

    request.session.get("glcs-session").map { sessionKey =>
      entryForm.bindFromRequest.fold(
        formWithErrors => { 
          Future(Redirect(routes.Instrument.listEntries).flashing("error" -> "invalid form"))
        },
        entry => { 
          val t = entry.readingDate.split(" ")(0).split("/")

          entryRepo.create(
            entry.reading, 
            entry.nutrition, 
            entry.readingTime, 
            Date.valueOf(t(2) + "-" + t(0) + "-" + t(1)),
            entry.exercise,
            sessionKeyRepo.findIdBySessionKey(sessionKey).getOrElse(throw new Exception)

          ).map(_ => {
            
            config.getBoolean("glcs.slack.enabled").get match {
              case true => postToSlack(ws, entry, config.getString("glcs.slack.url").get)
              case false => 
            }
            
            Redirect(routes.Instrument.listEntries).flashing("success" -> "glcs entry created")
          
          })
        }
      )
    }.getOrElse {
        Future(Redirect(routes.Instrument.listEntries).flashing("error" -> "you must be logged in to create an entry"))
    }
  }

  def listEntries = Action.async { implicit request =>
    
    request.session.get("glcs-session").map { sessionKey =>
      val sessionValid = sessionKeyRepo.keyExists(sessionKey)
      val action = entryRepo.all
      val entries = Await.result(action, Duration.Inf)
      val averages: Averages = getAverages(entries)
      
      Future(Ok(views.html.entries(entries, sessionValid, averages)))


    }.getOrElse {
      entryRepo.all.map(
        entries => {
          val averages: Averages = getAverages(entries)
          Ok(views.html.entries(entries, false, averages))
        }
      )
    }
  }

  def delete(id: Long) = Action.async { implicit request =>
    entryRepo.delete(id)
    Future(Redirect(routes.Instrument.listEntries).flashing("success" -> "entry deleted"))
  }

    def about() = Action {  implicit request =>
    request.session.get("glcs-session").map { sessionKey =>
      sessionKeyRepo.keyExists(sessionKey) match {
        case true => Ok(views.html.about(true))
        case false => Ok(views.html.about(false))
      }
    }.getOrElse {
      Ok(views.html.about(false))
    }
  }
  
}
