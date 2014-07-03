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

package utils

import play.api.mvc._
import play.api.libs.json.Reads
import play.api.Logger
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

/**
 * A not so secure wrapper provided as an example on how you can use action composition
 * to lookup headers and other auth intercepting controller actions
 */
trait SecureActions extends JsonUtils {

  val REST_API_KEY_HEADER = "REST-API-KEY-HEADER"

  /**
   * Used in async actions
   * @param f the underlying action if everything goes well
   * @return a promise of an action result
   */
  def SimpleAuthenticatedAction(f: (Request[AnyContent]) => Future[SimpleResult]) =
  Action.async { implicit request =>
      Logger.debug(s"received request : $request")
      request.headers.get(REST_API_KEY_HEADER).map {
        key => f(request)
      } getOrElse {
        Future(Unauthorized(s"Missing authentication headers: $REST_API_KEY_HEADER"))
      }
  }

  /**
   * Used in async actions that require json validation of any incoming json via POST or PUT
   * @param f the underlying action if everything goes well
   * @param reads an expected implicit JSON Reads in order to marshall json into T
   * @tparam T a generic type parameter indicating the type that the JSON representation conforms to
   * @return a promise of an action result
   */
  def JsonAuthenticatedAction[T](f: (T, Request[AnyContent]) =>
    Future[SimpleResult])(implicit reads: Reads[T]) = SimpleAuthenticatedAction {
    (request) =>
      request.body.asJson match {
        case Some(json) => validateJson[T](json, (t, validJson) => f(t, request))
        case None => Future(BadRequest("no json found"))
      }
  }

}
