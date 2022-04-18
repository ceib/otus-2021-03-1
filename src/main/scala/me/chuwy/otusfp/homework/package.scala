package me.chuwy.otusfp

import cats.effect.IO
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

package object homework {

  case class Counter(counter: Int)

  implicit val counterEncoder: Encoder[Counter] = deriveEncoder[Counter]
  implicit val counterEntityEncoder: EntityEncoder[IO, Counter] = jsonEncoderOf[IO, Counter]

  implicit val counterDecoder: Decoder[Counter] = deriveDecoder[Counter]
  implicit val counterEntityDecoder: EntityDecoder[IO, Counter] = jsonOf[IO, Counter]

}
