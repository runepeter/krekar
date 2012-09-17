package org.brylex.krekar

import akka.actor.Actor.actorOf
import akka.actor.Scheduler
import java.io.File
import java.util.concurrent.TimeUnit

object Balla extends App {

  val dir: File = new File("target/input")

  val jalla = actorOf(new FileEndpoint(5)).start()
  Scheduler.schedule(jalla, Poll(dir), 10, 5, TimeUnit.SECONDS)

  Thread.sleep(Long.MaxValue)
}