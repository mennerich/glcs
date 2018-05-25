package controllers

import java.sql.Date
import java.util.Calendar
import javax.inject.Inject
import models.{EntryRepo, Entry}
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

  case class DateTest(date: String)
  val dateForm = Form(
    mapping( "date" -> nonEmptyText)(DateTest.apply)(DateTest.unapply)
  )
  
  def create() = Action {  implicit request =>
    Ok(views.html.create(entryForm))
  }

  def submit() = Action.async { implicit request => 
    val now = new java.sql.Date(Calendar.getInstance().getTime().getTime())

    entryForm.bindFromRequest.fold(
      formWithErrors => { 
        println(formWithErrors)
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
        .map(id => Redirect(routes.Instrument.listEntries))
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
      Some(entry) <- entryRepo.findById(id)
    } yield Ok(views.html.entry(entry))
  }
}
