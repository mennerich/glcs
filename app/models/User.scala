package models

import javax.inject.Inject
import org.apache.commons.codec.digest.DigestUtils
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio
import slick.dbio.Effect.Read
import slick.jdbc.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.util.Random


case class User(id: Long, email: String, hash: String, salt: String)

class UserRepo @Inject()(sessionKeyRepo: SessionKeyRepo, protected val dbConfigProvider: DatabaseConfigProvider) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val db = dbConfig.db
  import dbConfig.profile.api._  

  private[models] val Users = TableQuery[UsersTable]

  def all: Future[List[User]] = db.run(Users.to[List].result)

  private def _findById(id: Long): DBIO[Option[User]] = Users.filter(_.id === id).result.headOption

  def findById(id: Long): Future[Option[User]] = db.run(_findById(id))

  def create(email: String, password: String): Future[Long] = {
  	val salt = Random.alphanumeric.take(10).mkString
  	val hash = DigestUtils.md5Hex(password + salt)
    val entry = User(0, email, hash, salt)
    db.run(Users returning Users.map(_.id) += entry)
  }

  def findByEmail(email: String): Future[Option[User]] = db.run(_findByEmail(email))

  private def _findByEmail(email: String): DBIO[Option[User]] = Users.filter(_.email === email).result.headOption

  def authenticate(email: String, password: String): Option[String] = {
      val action = findByEmail(email)
      val result = Await.result(action, Duration.Inf)
      result match {
        case Some(user) => {
          val hash = DigestUtils.md5Hex(password + user.salt)
          (hash == user.hash) match {
            case true =>  {
              val sessionKey = DigestUtils.md5Hex(Random.alphanumeric.take(10).mkString)
              sessionKeyRepo.create(user.id, sessionKey)
              Some(sessionKey)
            }
            case false => None
          }
        }
        case None => None
      }
  }

  private[models] class UsersTable(tag: Tag) extends Table[User](tag, "USER") {

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def email = column[String]("EMAIL")
    def hash = column[String]("HASH")
    def salt = column[String]("SALT")

    def * = (id, email, hash, salt) <> (User.tupled, User.unapply)
    def ? = (id.?, email.?, hash.?, salt.?)
      .shaped.<>( { r => import 
        r._; _1.map(_ => User.tupled((_1.get, _2.get, _3.get, _4.get))) 
    }, 
    (_: Any) => throw new Exception("Inserting into ? projection not supported."))
  }

}