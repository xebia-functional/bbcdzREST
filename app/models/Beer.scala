package models

import reactivemongo.bson.BSONObjectID
import play.api.libs.json.Json

case class Beer(_id : String = BSONObjectID.generate.toString(), title : String, description : Option[String] = None)

object Beer {
  implicit val beersFormat = Json.format[Beer]
}