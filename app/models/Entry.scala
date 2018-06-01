package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio
import slick.dbio.Effect.Read
import slick.jdbc.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.sql.Date

case class Entry (id: Long, reading: Int, nutrition: Int, readingTime: Int, readingDate: Date, exercise: Boolean, userId: Long)

case class EntryData(reading: Int, nutrition: Int, readingTime: Int, readingDate: String, exercise: Boolean)

class EntryRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db
  import dbConfig.profile.api._  
  private[models] val Entries = TableQuery[EntriesTable]

  def all: Future[List[Entry]] = db.run(
    Entries.sortBy(_.readingDate.desc)
    .sortBy(m => (m.readingDate.desc, m.readingTime.desc))
    .to[List].result
  )

  private def _findById(id: Long): DBIO[Option[Entry]] = Entries.filter(_.id === id).result.headOption

  def findById(id: Long): Future[Option[Entry]] = db.run(_findById(id))

  def create(reading: Int, nutrition: Int, readingTime: Int, readingDate: Date, exercise: Boolean, userId: Long): Future[Long] = {
    val entry = Entry(0, reading, nutrition, readingTime, readingDate, exercise, userId)
    db.run(Entries returning Entries.map(_.id) += entry)
  }

  def delete(id: Long): Future[Unit] = db.run(Entries.filter(_.id === id).delete).map(_ => ())
  

  private[models] class EntriesTable(tag: Tag) extends Table[Entry](tag, "ENTRY") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def reading = column[Int]("READING")
    def nutrition = column[Int]("NUTRITION")
    def readingTime = column[Int]("READING_TIME")
    def readingDate = column[Date]("READING_DATE")
    def exercise = column[Boolean]("EXERCISE")
    def userId = column[Long]("USER_ID")
    def * = (id, reading, nutrition, readingTime, readingDate, exercise, userId) <> (Entry.tupled, Entry.unapply)
  }

}