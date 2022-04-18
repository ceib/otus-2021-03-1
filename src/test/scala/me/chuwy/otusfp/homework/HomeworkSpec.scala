package me.chuwy.otusfp.homework

import cats.effect.{IO, Ref}
import cats.effect.testing.specs2.CatsEffect
import me.chuwy.otusfp.homework.Server.ByteToSend
import org.http4s.Status.{BadRequest, Ok}
import org.http4s.Request
import org.http4s.implicits.http4sLiteralsSyntax
import org.specs2.mutable.Specification

class HomeworkSpec extends Specification with CatsEffect {

  val sampleCounterRequest: Request[IO] = Request[IO]().withUri(uri"http://localhost:8080/counter")

  val sampleSlowRequest: Request[IO] = Request[IO]().withUri(uri"http://localhost:8080/slow/3/7/1")
  val sampleSlowBadRequest: Request[IO] = Request[IO]().withUri(uri"http://localhost:8080/slow/3/-7/1")
  val sampleSlowBadRequest2: Request[IO] = Request[IO]().withUri(uri"http://localhost:8080/slow/3asd/7/1")

  val slowReqeustResult: Array[Byte] = (for (_ <- 1 to 7) yield ByteToSend).toArray

  "HomeworkSpec" should {

    "Check Counter value" in {
      val result: IO[(Counter, Counter, Counter)] = for {
        counter <- Ref.of[IO, Counter](Counter(0))
        res1 <- Server.router(counter).run(sampleCounterRequest).value.flatMap {
          case Some(resp) => resp.as[Counter]
        }
        res2 <- Server.router(counter).run(sampleCounterRequest).value.flatMap {
          case Some(resp) => resp.as[Counter]
        }
        res3 <- Server.router(counter).run(sampleCounterRequest).value.flatMap {
          case Some(resp) => resp.as[Counter]
        }
      } yield (res1, res2, res3)

      result.map{
        case (res1, res2, res3) =>
          res1 must beEqualTo(Counter(1)) and (res2 must beEqualTo(Counter(2))) and (res3 must beEqualTo(Counter(3)))
      }
    }

    "Check slow result" in {
      val result = for {
        counter <- Ref.of[IO, Counter](Counter(0))
        res <- Server.router(counter).run(sampleSlowRequest).value.flatMap {
          case Some(resp) => resp.as[Array[Byte]].map(str => (resp.status, str) )
        }
      } yield res

      result.map{
        case (status, resBytes) =>
          status must beEqualTo(Ok) and (resBytes must beEqualTo(slowReqeustResult))
      }
    }

    "Check slow for BadRequests" in {
      val result = for {
        counter <- Ref.of[IO, Counter](Counter(0))
        resBRWithNegativeNumber <- Server.router(counter).run(sampleSlowBadRequest).value.flatMap {
          case Some(resp) => IO.pure(resp.status)
        }
        resBRWithLetters <- Server.router(counter).run(sampleSlowBadRequest2).value.flatMap {
          case Some(resp) => IO.pure(resp.status)
        }
      } yield (resBRWithNegativeNumber, resBRWithLetters)

      result.map{
        case (status1, status2) =>
          status1 must beEqualTo(BadRequest) and (status2 must beEqualTo(BadRequest))
      }
    }

  }

}
