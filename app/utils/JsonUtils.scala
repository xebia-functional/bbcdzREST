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

import play.api.libs.json.{JsValue, Reads}
import scala.concurrent.Future
import play.api.mvc.{Results, SimpleResult}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Trait used to validation incoming Json
 */
trait JsonUtils extends Results {

  def validateJson[T](json: JsValue, success: (T, JsValue) =>
    Future[SimpleResult])(implicit reads: Reads[T]) = {
    json.validate[T].asEither match {
      case Left(errors) => {
        Logger.warn(s"Bad request : $errors")
        Future(BadRequest(errors.mkString(",")))
      }
      case Right(valid) => success(valid, json)
    }
  }

}
