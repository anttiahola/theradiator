package controllers

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.Play.current
import java.security.MessageDigest
import java.nio.charset.Charset
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.net.URLEncoder
import scala.util.Random
import java.time.LocalDate

object MD5 {
  /** @return the MD5 hash of a sequence of bytes as a String */
  def md5Hex(bytes: Array[Byte]): String = md5Bytes(bytes).map("%02X".format(_)).mkString.toLowerCase

  /** @return the MD5 hash of a sequence of bytes as bytes */
  def md5Bytes(bytes: Array[Byte]): Seq[Byte] = MessageDigest.getInstance("MD5").digest(bytes)
}

class MixPanel extends Controller {
  val config = Play.application.configuration
  
  val APIKEY = config.getString("mixpanel.apikey").getOrElse("No key.")
  val TOKEN  = config.getString("mixpanel.token").getOrElse("No token.")
  val SECRET = config.getString("mixpanel.secret").getOrElse("No secret.")

  def index = Action {
    Ok(views.html.mp.index())
  }

  def fragment = Action {
    Ok(views.html.mp.fragment())
  }
  
  def data(eventNames: String) = Action.async {
    val events = eventNames.split(",").toList
    val eventDataF = loadData(events)
    eventDataF.map { eventData =>
      Ok(createJson(eventData))
    }
  }

  private def createSig(params: Map[String, String]): String = {
    val argsSorted = params.toList.sortWith((p1,p2) => p1._1 < p2._1)
    val argsConcat = argsSorted.map{ case (k,v)=> k + "=" + v }.mkString
    
    val sig = MD5.md5Hex((argsConcat + SECRET).getBytes(Charset.forName("UTF-8")))
    sig
  }
  
  private def addRequiredParams(params: Map[String, String]): List[(String, String)] = {
    val expire = "" + (System.currentTimeMillis() / 1000 + 100)
    val allParams = params + ("api_key" -> APIKEY) + ("expire" -> expire)
    (allParams + ("sig" -> createSig(allParams))).toList
  }
  
  case class EventWithValues(event: String, values: List[(String, Int)])
  case class EventData(labels: List[String], valuesForEvent: List[EventWithValues])
  
  
  private def cleanUpDateLabels(labels: List[String]): List[String] = {
    val localDates = labels.map(LocalDate.parse(_))
    val previousDates = List(None) ++ localDates.map(Some(_))
    
    localDates.zip(previousDates).map { case (current, previous) =>
      cleanUpDateLabel(previous, current)
    }
  }
  
  private def cleanUpDateLabel(previousDateOpt: Option[LocalDate], current: LocalDate): String = {
    previousDateOpt match {
      case Some(prev) => cleanUpDateLabel(prev, current)
      case None => s"${current.getYear}-${current.getMonthValue}-${current.getDayOfMonth}"
    }
  }

  private def cleanUpDateLabel(prev: LocalDate, cur: LocalDate): String = {
    def sameYear(d1: LocalDate, d2: LocalDate): Boolean = 
      d1.getYear == d2.getYear

    def sameMonth(d1: LocalDate, d2: LocalDate): Boolean = 
      d1.getMonthValue == d2.getMonthValue && sameYear(d1, d2)

    
    val yearPart = if(sameYear(prev, cur)) { "" } else { ""+cur.getYear+"-" }
    val monthPart = if(sameMonth(prev, cur)) { "" } else { ""+cur.getMonthValue+"-" }
    val dayPart = "" + cur.getDayOfMonth
    
    yearPart + monthPart + dayPart
  }
  
  private def loadData(events: List[String]) : Future[EventData] = {
    val eventsJson = JsArray(events.map(JsString(_)))
    
    val customParams = Map[String, String](
        "interval" -> "16",
        "type" -> "general",
        "event" -> eventsJson.toString(),
        "unit" -> "day"
    )
    val allParams = addRequiredParams(customParams)
    val responseF = WS.url("http://mixpanel.com/api/2.0/events/")
                      .withQueryString(allParams: _*)
                      .get()
    
    /*
     * {
     * "legend_size": 2, 
     * "data": {
     * 		"series": [
     * 				"2016-04-03", "2016-04-04", "2016-04-05", "2016-04-06", "2016-04-07", "2016-04-08", "2016-04-09"
     * 		], 
     * 		"values": {
     *			"results": {
     *	 			"2016-04-05": 3166, "2016-04-03": 778, "2016-04-02": 628, "2016-04-07": 2613, 
     * 				"2016-04-06": 3028, "2016-04-04": 3770, "2016-04-09": 6, "2016-04-08": 2506}, 
     * 			"Email Opened": {
     * 				"2016-04-05": 10708, "2016-04-03": 5799, "2016-04-02": 5806, "2016-04-07": 8456, 
     * 				"2016-04-06": 9577, "2016-04-04": 21932, "2016-04-09": 0, "2016-04-08": 7318}
     * 			}
     * 		}
     * }
     */
    
    responseF.map { response => 
      val json = response.json
      
      val labels = (json \ "data" \ "series").as[JsArray].value.map(_.as[JsString].value).toList
      
      val valuesObject = (json \ "data" \ "values").as[JsObject]
      val events = valuesObject.fields.map { case (event, labelsAndValues) =>
        val labelsToValues = labelsAndValues.as[JsObject].fields.map { case (label, value) =>
          (label, value.as[JsNumber].value.toInt)
        }.toList
        EventWithValues(event, labelsToValues)
      }.toList
      
      EventData(labels, events)
    }
  }
  
  
  
  private def createJson(eventData: EventData): JsObject = {
    
    val eventJsons = eventData.valuesForEvent.zipWithIndex.map { case (eventWithValues, index) =>
      val valueMap = eventWithValues.values.toMap.withDefaultValue(0)
      val valueList = eventData.labels.map { label => valueMap(label) }
      
      Json.obj(
        "label" -> eventWithValues.event,
        "fillColor" -> color(index, 0.35),
        "strokeColor" -> color(index, 1),
        "pointColor" -> color(index, 1),
        "pointStrokeColor" -> "#fff",
        "pointHighlightFill" -> "#fff",
        "pointHighlightStroke" -> color(index, 1),
        "data" -> JsArray(valueList.map(JsNumber(_)))
      )
    }
    
    val labelsAy = JsArray(cleanUpDateLabels(eventData.labels).map(JsString(_)))
    Json.obj("labels" -> labelsAy, "datasets" -> JsArray(eventJsons))
  }
  
  private val colors = Vector(/*(151, 187, 205),(151, 205, 187),(187, 151, 205),(187, 205, 151),*/(33, 140, 141),(108, 206, 203), (249, 229, 89), ( 239, 113, 38), (142, 220, 157), (71, 62, 63),(205, 151, 187))
  private val rand = new Random(System.currentTimeMillis())
  private val number = Math.abs(rand.nextInt())
  private def color(index: Int, opacity: Double): String = {
    val tuple = colors(index % colors.length)
    s"rgba(${tuple._1},${tuple._2},${tuple._3},$opacity)"
  }
}
