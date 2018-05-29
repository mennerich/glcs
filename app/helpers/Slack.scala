package helpers

import javax.inject.Inject
import models.EntryData
import play.api.libs.ws._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.libs.json._

trait SlackSupport {

  val readingTime = Map(1 -> "morning", 2 -> "noon", 3 -> "evening")
  val nutrition = Map(1 -> "poor", 2 -> "adequate", 3 -> "good")
  val exercise = Map(false -> "did not exercise", true -> "exercised")

  def postToSlack(ws: WSClient, entry: EntryData, slackUrl: String) {
  
  	val request: WSRequest = ws
  	  .url(slackUrl)
      .addHttpHeaders("Content-Type" -> "application/json")


    val response = request.post(JsObject(
      Seq("text" -> 
      JsString(
        "Don added a blood glucose reading to glcs.ch\n" 
        + s"*date:* ${entry.readingDate}, *time:* ${readingTime(entry.readingTime)}, *mg/dl:* ${entry.reading}, *nutrition:* ${nutrition(entry.nutrition)}, ${exercise(entry.exercise)}")
	
	)))

  }

}