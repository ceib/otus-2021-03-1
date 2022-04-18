package me.chuwy.otusfp.homework

import cats.effect.{IO, Resource}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client

import scala.concurrent.ExecutionContext.global

object Client {

  val client: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](global).resource

}
