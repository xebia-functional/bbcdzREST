package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.{JsArray, JsObject, Json}
import utils.SecureActions
import models.Beer

object Application extends Controller with MongoController with SecureActions {

  def beersCollection = db.collection[JSONCollection]("beers")

  def asJson(v: Option[JsObject]) = v.map(Ok(_)).getOrElse(NotFound)

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def listBeers() = SimpleAuthenticatedAction {
    _ =>
      beersCollection
        .find(Json.obj())
        .cursor[JsObject]
        .toList() map {
        beers =>
          Ok(JsArray(beers))
      }
  }

  def findBeer(id: String) = SimpleAuthenticatedAction {
    _ =>
      beersCollection
        .find(Json.obj("_id" -> id))
        .one[JsObject] map asJson
  }

  def addBeer() = JsonAuthenticatedAction[Beer] {
    (beer, _) =>
      beersCollection.insert(beer) map {
        _ => Ok(Json.toJson(beer))
      }
  }

  def updateBeer(id: String) = JsonAuthenticatedAction[JsObject] {
    (json, _) =>
      for {
        _ <- beersCollection.update(Json.obj("_id" -> id), Json.obj("$set" -> json))
        newBeer <- beersCollection.find(Json.obj("_id" -> id)).one[JsObject]
      } yield asJson(newBeer)
  }

  def deleteBeer(id: String) = SimpleAuthenticatedAction {
    _ =>
      for {
        newBeer <- beersCollection.find(Json.obj("_id" -> id)).one[JsObject]
        _ <- beersCollection.remove(Json.obj("_id" -> id))
      } yield asJson(newBeer)
  }

}