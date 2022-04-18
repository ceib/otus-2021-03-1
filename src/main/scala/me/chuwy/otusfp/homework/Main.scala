package me.chuwy.otusfp.homework

import cats.effect.{IO, IOApp, Ref}
import org.http4s.Request

import scala.language.postfixOps
import org.http4s.implicits.http4sLiteralsSyntax

object Main extends IOApp.Simple {

  val sampleSlowRequest: Request[IO] = Request[IO]().withUri(uri"http://localhost:8080/slow/3/10/1")

  override def run: IO[Unit] = {
    for {
      counter <- Ref.of[IO, Counter](Counter(0))
      _ <- Server.router(counter).run(sampleSlowRequest).value.flatMap {
        case Some(resp) => resp.as[String].flatMap(IO.println)
        case None => IO.println("Wrong request")
      }
    } yield ()
  }

}
