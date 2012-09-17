package no.fotballresultater

import akka.config.Supervision.{SupervisorConfig, AllForOneStrategy, Supervise, Permanent}
import akka.actor.Actor.actorOf
import domain.repository.{ResultRepository, InMemoryResultRepository}
import domain.{Event, Result}
import result.ResultActor
import scrape.ScrapeActor
import java.util.concurrent.TimeUnit
import akka.actor.{Actor, Scheduler, Supervisor}
import sys.ShutdownHookThread
import org.neo4j.kernel.EmbeddedGraphDatabase

object Boot extends App {

    val neo4j = new EmbeddedGraphDatabase("target/neo4j")
    ShutdownHookThread {
      neo4j.shutdown()
    }

  val supervisor = Supervisor(
    SupervisorConfig(
      AllForOneStrategy(List(classOf[Exception]), 3, 1000),
      Supervise(actorOf(new ScrapeActor), Permanent)
        :: Supervise(actorOf(new ResultActor), Permanent)
        :: Supervise(actorOf(new InMemoryResultRepository(neo4j)), Permanent)
        :: Nil
    )
  ).start

  Scheduler.schedule(Actor.registry.actorsFor[ScrapeActor].head, Scrape("http://livescore.com/soccer/soccer"), 10, 30, TimeUnit.SECONDS)

  Actor.registry.actorsFor[InMemoryResultRepository].head ! Save(Result(new Event("a", "b", "c", "d"), "1", "1", "2'"))
  Actor.registry.actorsFor[InMemoryResultRepository].head ! Save(Result(new Event("a", "b", "c", "d"), "1", "1", "5'"))
  Actor.registry.actorsFor[InMemoryResultRepository].head ! Save(Result(new Event("a", "b", "c", "d"), "1", "1", "8'"))

  Actor.registry.actorsFor[InMemoryResultRepository].head ! Query4League("a")

  Thread.sleep(Long.MaxValue)
}
