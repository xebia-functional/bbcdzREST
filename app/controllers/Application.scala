/*
 * Copyright (C) 2013 47 Degrees, LLC
 * http://47deg.com
 * hello@47deg.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import models.Beer
import models.Beer._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import utils.SecureActions


/**
 * REST API Controller providing CRUD operations for Beers with Reactive Mongo in a full async API
 */
object Application extends Controller with MongoController with SecureActions {

  /**
   * A reference of a JSON style collection in Mongo
   */
  private def beersCollection = db.collection[JSONCollection]("beers")

  /**
   * Convinience helper thar marshalls json or sends a 404 if none found
   */
  private def asJson(v: Option[JsObject]) = v.map(Ok(_)).getOrElse(NotFound)

  /**
   * Default index entry point
   */
  def index = Action {
    Ok(views.html.index("bbcdzREST!"))
  }

  /**
   * Actions that reactively list all beers in the collection
   */
  def listBeers() = SimpleAuthenticatedAction {
    _ =>
      beersCollection
        .find(Json.obj())
        .cursor[JsObject]
        .collect[List]() map {
        beers =>
          Ok(JsArray(beers))
      }
  }

  /**
   * Finds a beer by Id
   */
  def findBeer(id: String) = SimpleAuthenticatedAction {
    _ =>
      beersCollection
        .find(Json.obj("_id" -> id))
        .one[JsObject] map asJson
  }

  /**
   * Adds a beer
   */
  def addBeer() = JsonAuthenticatedAction[Beer] {
    (beer, _) =>
      beersCollection.insert(beer) map {
        _ => Ok(Json.toJson(beer))
      }
  }

  /**
   * Partially updates the properties of a beer
   */
  def updateBeer(id: String) = JsonAuthenticatedAction[JsObject] {
    (json, _) =>
      for {
        _ <- beersCollection.update(Json.obj("_id" -> id), Json.obj("$set" -> json))
        newBeer <- beersCollection.find(Json.obj("_id" -> id)).one[JsObject]
      } yield asJson(newBeer)
  }

  /**
   * Deletes a beer by id
   */
  def deleteBeer(id: String) = SimpleAuthenticatedAction {
    _ =>
      for {
        newBeer <- beersCollection.find(Json.obj("_id" -> id)).one[JsObject]
        _ <- beersCollection.remove(Json.obj("_id" -> id))
      } yield asJson(newBeer)
  }

}