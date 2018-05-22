package controllers

import javax.inject.Inject
import models.{EntryRepo, Entry, EntryData }
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.I18nSupport
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }


class Instrument @Inject()
  (implicit ec: ExecutionContext, 
  entryRepo: EntryRepo, 
  val controllerComponents: ControllerComponents) 
  extends BaseController with I18nSupport {

  val entryForm = Form(
    mapping(
      "reading" -> number,
      "nutrition" -> number,
      "readingTime" -> number)
    (EntryData.apply)(EntryData.unapply)
  )
  
  def create() = Action {  implicit request =>
    Ok(views.html.create(entryForm))
  }

  def submit() = Action.async { implicit request => 
    entryForm.bindFromRequest.fold(
      formWithErrors => { Future(Ok("fuck")) },
      entry => { 
        entryRepo.create(entry.reading, entry.nutrition, entry.readingTime)
          .map(id => Redirect(routes.Instrument.listEntries) )
      }
    )
  }

  def listEntries = Action.async { implicit rs =>
  	entryRepo.all
      .map(entries => Ok(views.html.entries(entries)))
  }

  def delete(id: Long) = Action.async { implicit rs =>
    entryRepo.delete(id)
    Future(Redirect(routes.Instrument.listEntries))
  }


  def show(id: Long) = Action.async { implicit rs =>
    for {
      Some(entry) <-  entryRepo.findById(id)
    } yield Ok(views.html.entry(entry))
  }
}
