package me.chuwy.otusfp.homework

import scala.concurrent.ExecutionContext.global
import cats.effect.{IO, Ref, Resource}
import org.http4s.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.server.{Router, Server}
import fs2.{Chunk, Stream}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Server {

  type ServerCounter[F[_]] = Ref[F, Counter]

  val ByteToSend: Byte = 33.toByte

  def baseRoutes(serverCounter: ServerCounter[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "counter" =>
      serverCounter
        .updateAndGet(cc => Counter(cc.counter + 1))
        .flatMap(counter => Ok(counter))
  }

  def chunkOfData(chunkSize: Int): Option[Chunk[Byte]] = if (chunkSize > 0) Some(Chunk.seq((1 to chunkSize).map(_ => ByteToSend))) else None

  def dataStream(total: Int, chunkSize: Int, time: Int): Stream[IO, Chunk[Byte]] =
    Stream.unfoldEval(0) {
      state =>
        val diff = total - state
        if (diff > 0)
          IO.sleep(time seconds)
            .map {
              _ =>
                  chunkOfData(if (diff < chunkSize) diff else chunkSize).map(f => (f, state + f.size))
            }
        else IO.none
    }

  def toPositiveInteger(str: String): IO[Int] = IO(str.toInt)
    .flatMap(i => if (i < 0) IO.raiseError(new RuntimeException("Should be positive integer")) else IO.pure(i) )

  def slowRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "slow" / chunk / total / time =>
      (for {
        tt <- toPositiveInteger(total)
        c <- toPositiveInteger(chunk)
        t <- toPositiveInteger(time)
        resp <- Ok(dataStream(tt, c, t)
                    .evalTap(IO.println)
                  )
      } yield resp).handleErrorWith(err => BadRequest(err.getMessage))
  }

  def router(serverCounter: ServerCounter[IO]): HttpRoutes[IO] = Router("/" -> baseRoutes(serverCounter), "/" -> slowRoutes)

  val server: Resource[IO, Server] = {
    for {
      counter <- Resource.eval[IO, ServerCounter[IO]](Ref.of(Counter(0)))
      server <- BlazeServerBuilder[IO](global)
        .bindHttp(port = 8080, host = "localhost")
        .withHttpApp(router(counter).orNotFound)
        .resource
    } yield server
  }

}
