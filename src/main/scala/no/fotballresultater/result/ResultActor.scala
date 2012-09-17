package no.fotballresultater.result

import akka.actor.Actor
import no.fotballresultater.domain.Result
import org.slf4j.{Logger, LoggerFactory}
import no.fotballresultater.domain.repository.InMemoryResultRepository
import no.fotballresultater.Save

class ResultActor extends Actor {

  private val logger: Logger = LoggerFactory.getLogger(classOf[ResultActor])

  protected def receive = {
    case result@Result(event, homeScore, awayScore, time) => {
      Actor.registry.actorsFor[InMemoryResultRepository].head ! Save(result)
    }
    case _ => println("jalla")
  }
}