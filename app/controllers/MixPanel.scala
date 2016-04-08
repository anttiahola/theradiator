package controllers

import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.libs.ws._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import java.security.MessageDigest

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


  def loadData() : Future[(List[Block], List[Block])] = {
    MD5.md5Hex()
    ???
  }
}
