package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio
import slick.dbio.Effect.Read
import slick.jdbc.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import java.sql.Date
import helpers.{ Averages, StatsSupport }
import scala.concurrent.duration.Duration

case class Entry (id: Int, reading: Int, nutrition: Int, readingTime: Int, readingDate: Date, exercise: Boolean, userId: Int, weight: Option[Int])

class EntryRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends StatsSupport {
  
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db
  import dbConfig.profile.api._  
  private[models] val Entries = TableQuery[EntriesTable]

  def all: Future[List[Entry]] = db.run(
    Entries.sortBy(m => (m.readingDate.desc, m.readingTime.desc))
    .to[List].result
  )

  def listEntries(offset: Int, size: Int): Future[List[Entry]] = {
    db.run(
      Entries.sortBy(m => (m.readingDate.desc, m.readingTime.desc))
      .drop(offset).take(size)
      .to[List].result
    )
  }

  def entryAverages(): Averages = {
    val action = all
    getAverages(Await.result(action, Duration.Inf))
  }

  def findById(id: Int): Future[Option[Entry]] = db.run(Entries.filter(_.id === id).result.headOption)

  def create(entry: Entry): Future[Int] = db.run(Entries += entry) 

  def delete(id: Int): Future[Int] = db.run(Entries.filter(_.id === id).delete)
  
  def update(entry: Entry): Future[Int] = db.run(Entries.update(entry)) 

  private[models] class EntriesTable(tag: Tag) extends Table[Entry](tag, "ENTRY") {
    def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def reading = column[Int]("READING")
    def nutrition = column[Int]("NUTRITION")
    def readingTime = column[Int]("READING_TIME")
    def readingDate = column[Date]("READING_DATE")
    def exercise = column[Boolean]("EXERCISE")
    def userId = column[Int]("USER_ID")
    def weight = column[Option[Int]]("WEIGHT")
    def * = (id, reading, nutrition, readingTime, readingDate, exercise, userId, weight) <> (Entry.tupled, Entry.unapply)
  }

}