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

  def index = Action.async {
  	loadData().map { case (apps, servers) => 
      Ok(views.html.mp.index(apps, servers))
  	}
  }

  def fragment = Action.async {
  	loadData().map { case (apps, servers) => 
      Ok(views.html.mp.fragment(apps, servers))
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

  def loadData() : Future[(List[Block], List[Block])] = {
    val events = List("Email Opened", "results")
    val eventsJson = JsArray(events.map(JsString(_)))
    
    val customParams = Map[String, String](
        "interval" -> "7",
        "type" -> "general",
        "event" -> eventsJson.toString(),
        "unit" -> "day"
    )
    val allParams = addRequiredParams(customParams)
    val responseF = WS.url("http://mixpanel.com/api/2.0/events/")
                      .withQueryString(allParams: _*)
                      .get()
    
    val response = Await.result(responseF, Duration.Inf)
    println(response.status)
    println(response.body)
    ???
  }
}
