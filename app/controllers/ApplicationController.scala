package controllers

import helpers.{ StatsSupport, Averages, SlackSupport }
import java.sql.Date
import java.util.Calendar
import javax.inject.Inject
import models.{ EntryRepo, Entry, SessionKeyRepo, UserRepo }
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
  userRepo: UserRepo,
  ws: WSClient,
  config: Configuration,
  lifecycle: ApplicationLifecycle,
  val controllerComponents: ControllerComponents) 
  extends BaseController 
  with I18nSupport 
  with SlackSupport {

  lifecycle.addStopHook { () =>
    Future.successful(Logger.info("Dumping Session Table"))
    Future.successful(sessionKeyRepo.deleteAll)
  }

  val form = Form(
    mapping(
      "id" -> number,
      "reading" -> number,
      "nutrition" -> number,
      "readingTime" -> number,
      "readingDate" -> sqlDate,
      "exercise" -> boolean,
      "userId" -> number,
      "weight" -> optional(number))
    (Entry.apply)(Entry.unapply)
  )
  
  def index = Action { Redirect(routes.Instrument.listEntries(0)) }

  def listEntries(page: Int) = Action.async { implicit request =>
    val list = entryRepo.listEntries(page * 10, 10)
    val entries = Await.result(list, Duration.Inf)
    val averages = entryRepo.entryAverages
    
    request.session.get("glcs-session").map { sessionKey =>

      sessionKeyRepo.keyExists(sessionKey) match {
        case true => Future(Ok(views.html.index(entries, averages, page, true)))
        case false => Future(Ok(views.html.index(entries, averages, page, false)))
      }
    }.getOrElse {
      entryRepo.listEntries(page * 10, 10).map(
        entries => {
          Ok(views.html.index(entries, averages, page, false))
        }
      )
    }
  }

  def create() = Action {  implicit request =>
    request.session.get("glcs-session").map { sessionKey =>
      sessionKeyRepo.keyExists(sessionKey) match {
        case true => {
          Ok(views.html.create(form))
        }
        case false => {
          Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "you must be logged in to create an entry")
        }
      }
    }.getOrElse {
        Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "you must be logged in to create an entry")
    }
  }

  def submit() = Action.async { implicit request => 
    request.session.get("glcs-session").map { sessionKey =>
      form.bindFromRequest.fold(
        formWithErrors => { 
          Future(Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "invalid form"))
        },
        entry => { 
          entryRepo.create(
            new Entry(
              0,
              entry.reading, 
              entry.nutrition, 
              entry.readingTime, 
              entry.readingDate,
              entry.exercise,
              userRepo.findBySessionKey(sessionKey).getOrElse(throw new Exception),
              entry.weight
            )
          ).map(_ => {
            // check for slack integration and post to            
            config.get[Boolean]("glcs.slack.enabled") match {
              case true => postToSlack(ws, entry, config.get[String]("glcs.slack.url"))
              case false => 
            }
            
            Redirect(routes.Instrument.listEntries(0)).flashing("success" -> "glcs entry created")
          
          })
        }
      )
    }.getOrElse {
        Future(Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "you must be logged in to create an entry"))
    }
  }

  def edit(id: Int) = Action.async { implicit request =>
    request.session.get("glcs-session").map { sessionKey => 
      sessionKeyRepo.keyExists(sessionKey) match {
        case true =>  {
          val entry = entryRepo.findById(id)
          val result = Await.result(entry, Duration.Inf) 

          result match {

            case Some(e) => {

              val filledForm = form.fill(
                Entry(
                  e.id,
                  e.reading, 
                  e.nutrition, 
                  e.readingTime, 
                  e.readingDate,
                  e.exercise, 
                  e.userId,
                  e.weight)
              )

              Future(Ok(views.html.edit(filledForm)))
            }
            case None => Future(Redirect(routes.Instrument.listEntries(0)).flashing("error" -> s"${id} not found in system"))
          }
        }
        case false => Future(Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "you must be logged in to edit an entry"))
      }
    }.getOrElse {
      Future(Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "invalid session, you must be logged in to edit an entry"))
    }
  }

  def update() = Action.async { implicit request =>
    form.bindFromRequest.fold(
      formWithErrors => { 
        Future(Redirect(routes.Instrument.listEntries(0)).flashing("error" -> "invalid form"))
      },
      entry => {
        println(entry)
        entryRepo.update(entry).map(_ => {
          Redirect(routes.Instrument.listEntries(0)).flashing("success" -> s"${entry.id} updated")
        })
      }
    )
  }

  def delete(id: Int) = Action.async { implicit request =>
    entryRepo.delete(id)
    Future(Redirect(routes.Instrument.listEntries(0)).flashing("success" -> "entry deleted"))
  }

  def about() = Action {  implicit request =>
    Ok("#")
  }
  
}
