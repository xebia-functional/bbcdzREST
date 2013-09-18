package utils

import play.api.libs.json.{Json, JsValue, Reads, JsObject}
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
