#Play 2 Scala REST API + ReactiveMongo Tutorial

This app is a simple example that shows a basic CRUD REST API built with Play2 Scala + Reactive Mongo.

---

##Setup

The tutorial assumes you have Play 2.1.x and Mongo installed on your local machine

http://www.playframework.com/download
http://www.mongodb.org/downloads

###Create the app

    play new bbcdzREST
    
###Add the ReactiveMongo Play Plugin Dependencies

project/Build.scala

```scala

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.9"
)

```

###Register the plugin with Play

*conf/play.plugins*

```
400:play.modules.reactivemongo.ReactiveMongoPlugin
```

###Add the DB config

*conf/application.conf*

```
mongodb.uri ="mongodb://username:password@localhost:27017/your_db_name"
```
---

##Code

###Model

First we are gonna create our basic model and we will be using Play automatic JSON Combinators to serialize case classes from and to JSON. 

*app/models/Beer.scala*

```scala

package models

import reactivemongo.bson.BSONObjectID
import play.api.libs.json.Json

case class Beer(
                 _id : String = BSONObjectID.generate.toString(), 
                 title : String, 
                 description : Option[String] = None
                 )

object Beer {
  implicit val beersFormat = Json.format[Beer]
}

```

###Utils

These are just handy wrappers used in Play action composition to validate JSON and provide a not so secure layer that can give you some hints on how to implement your own API security and validate the incoming data conforms to a model.

*app/utils/JsonUtils.scala*

This trait function is used to validate incoming JSON in a generic reusable way so we can focus our controller actions in the actual CRUD and not so much in validating each time.

```scala
package utils

import play.api.libs.json.{JsValue, Reads}
import scala.concurrent.Future
import play.api.mvc.{Results, Result}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

trait JsonUtils extends Results {

  def validateJson[T](json: JsValue, success: (T, JsValue) => Future[Result])(implicit reads: Reads[T]) = {
    json.validate[T].asEither match {
      case Left(errors) => {
        Logger.warn(s"Bad request : $errors")
        Future(BadRequest(errors.mkString(",")))
      }
      case Right(valid) => success(valid, json)
    }
  }

}
```

*app/utils/SecureActions.scala*

This trait helps us with enforcing security and json validation on each action that requires it. **It is incomplete and does not actually validate the header tokens or keys agains't anything**

```scala
package utils

import play.api.mvc._
import play.api.libs.json.Reads
import play.api.Logger
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

trait SecureActions extends JsonUtils {

  val REST_API_KEY_HEADER = "REST-API-KEY-HEADER"

  def SimpleAuthenticatedAction(f: (Request[AnyContent]) => Future[Result]) = Action {
    implicit request => Async {
      Logger.debug(s"received request : $request")
      request.headers.get(REST_API_KEY_HEADER).map {
        key => f(request)
      } getOrElse {
        Future(Unauthorized(s"Missing authentication headers: $REST_API_KEY_HEADER"))
      }
    }
  }

  def JsonAuthenticatedAction[T](f: (T, Request[AnyContent]) => Future[Result])(implicit reads: Reads[T]) = SimpleAuthenticatedAction {
    (request) =>
      request.body.asJson match {
        case Some(json) => validateJson[T](json, (t, validJson) => f(t, request))
        case None => Future(BadRequest("no json found"))
      }
  }

}
```

### REST API 

The REST API is implemented with a simple Play controller that uses the above mentioned utils and it's configured in the play routes file to each one of the HTTP verbs representing the CRUD.

*conf/routes*

```
# Routes
# This file defines all application routes (Higher priority routes first)
# ~~~~

# Home page
GET           /                    controllers.Application.index

# Map static resources from the /public folder to the /assets URL path
GET           /assets/*file        controllers.Assets.at(path="/public", file)

# Lists beers
GET           /beers               controllers.Application.listBeers

# Gets a beer by id
GET           /beers/:id           controllers.Application.findBeer(id)

# Adds a beer
POST          /beers               controllers.Application.addBeer

# Partially updates the content of a beer
PUT           /beers/:id           controllers.Application.updateBeer(id)

# Deletes a beer
DELETE        /beers/:id           controllers.Application.deleteBeer(id)
```

app/controllers/Application.scala

```scala
package controllers

import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.{JsArray, JsObject, Json}
import utils.SecureActions
import models.Beer

object Application extends Controller with MongoController with SecureActions {

  private def beersCollection = db.collection[JSONCollection]("beers")

  private def asJson(v: Option[JsObject]) = v.map(Ok(_)).getOrElse(NotFound)

  def index = Action {
    Ok(views.html.index("bbcdzREST!"))
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
```
#License

Copyright (C) 2013 47 Degrees, LLC
http://47deg.com
hello@47deg.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
