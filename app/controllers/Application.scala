package controllers

import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.libs.ws._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

class Application extends Controller {

  val config = Play.application.configuration

  def index = Action {
    Ok(views.html.index())
  }
}

