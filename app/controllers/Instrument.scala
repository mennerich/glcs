package controllers

import javax.inject.Inject
import models.{EntryRepo}
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }


class Instrument @Inject()
  (implicit ec: ExecutionContext, 
  entryRepo: EntryRepo, 
  val controllerComponents: ControllerComponents) 
  extends BaseController {

  def listEntries = Action.async { implicit rs =>
  	entryRepo.all
    .map(entries => Ok(views.html.entries(entries)))
  }

  def createEntry(reading: Int, nutrition: String, readingTime: Int)= Action.async { implicit rs =>
    entryRepo.create(reading, nutrition, readingTime)
      .map(id => Ok(s"entry $id created") )
  }

  def delete(id: Long) = Action.async { implicit rs =>
    entryRepo.delete(id)
    Future(Redirect("/"))
  }


  def show(id: Long) = Action.async { implicit rs =>
    for {
      Some(entry) <-  entryRepo.findById(id)
    } yield Ok(views.html.entry(entry))
  }
}
