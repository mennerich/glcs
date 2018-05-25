package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import org.apache.commons.codec.digest.DigestUtils
import slick.dbio
import slick.dbio.Effect.Read
import slick.jdbc.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

case class SessionKey(id: Long, sessionKey: String, userId: Int)

class SessionKeyRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
  
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db
  import dbConfig.profile.api._  
  private[models] val SessionKeys = TableQuery[SessionKeysTable]

  def all: Future[List[SessionKey]] = db.run(SessionKeys.to[List].result)

  private def _findById(id: Long): DBIO[Option[SessionKey]] = SessionKeys.filter(_.id === id).result.headOption

  def findById(id: Long): Future[Option[SessionKey]] = db.run(_findById(id))

  def create(userId: Int): Future[Long] = {
    val seed = Random.alphanumeric.take(10).mkString
    val sKey = DigestUtils.md5Hex(seed)
    val sessionKey = SessionKey(0, sKey, userId)
    db.run(SessionKeys returning SessionKeys.map(_.id) += sessionKey)
  }

  def delete(id: Long): Future[Unit] = db.run(SessionKeys.filter(_.id === id).delete).map(_ => ())

  private[models] class SessionKeysTable(tag: Tag) extends Table[SessionKey](tag, "SESSION") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def sessionKey = column[String]("SESSION_KEY")
    def userId = column[Int]("USER_ID")
    def * = (id, sessionKey, userId) <> (SessionKey.tupled, SessionKey.unapply)
    def ? = (id.?, sessionKey.?, userId.?)
      .shaped.<>( { r => import 
        r._; _1.map(_ => SessionKey.tupled((_1.get, _2.get, _3.get))) 
    }, 
    (_: Any) => throw new Exception("Inserting into ? projection not supported."))
  }

}