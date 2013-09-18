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
