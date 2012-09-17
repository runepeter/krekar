package no.fotballresultater.domain.repository

import org.slf4j.{LoggerFactory, Logger}
import akka.actor.Actor
import no.fotballresultater.domain.{Event, Result}
import no.fotballresultater.{Query4League, Save}
import org.neo4j.kernel.Traversal
import collection.JavaConversions
import org.neo4j.graphdb.traversal.{Traverser, TraversalDescription, Evaluation, Evaluator}
import org.neo4j.graphdb._
import java.util.Iterator

class InMemoryResultRepository(val db: GraphDatabaseService) extends Actor with ResultRepository {

  private val logger: Logger = LoggerFactory.getLogger(classOf[InMemoryResultRepository])

  private class LeagueEvaluator(val league: String) extends Evaluator {
    def evaluate(p: Path) = {

      if (p.startNode() == p.endNode()) {
        Evaluation.EXCLUDE_AND_CONTINUE
      } else {

        val relationship: Relationship = p.relationships().iterator().next()
        println("REL: " + relationship + ", [" + relationship.getPropertyKeys + "]")
        val propertyValue = relationship.getProperty("league")
        println("League: [" + propertyValue + "]")

        if (propertyValue.equals(league)) {
          Evaluation.INCLUDE_AND_CONTINUE
        } else {
          Evaluation.EXCLUDE_AND_CONTINUE
        }
      }
    }
  }


  private class EventEvaluator(val event: Event) extends Evaluator {
    def evaluate(p: Path) = {

      if (p.startNode() == p.endNode()) {
        Evaluation.EXCLUDE_AND_CONTINUE
      } else {

        val relationship: Relationship = p.relationships().iterator().next()

        val leagueValue: String = relationship.getProperty("league").asInstanceOf[String]
        val dateValue: String = relationship.getProperty("date").asInstanceOf[String]
        val homeValue: String = relationship.getProperty("home").asInstanceOf[String]
        val awayValue: String = relationship.getProperty("away").asInstanceOf[String]

        if (Event(leagueValue, dateValue, homeValue, awayValue).equals(event)) {
          Evaluation.INCLUDE_AND_CONTINUE
        } else {
          Evaluation.EXCLUDE_AND_CONTINUE
        }
      }
    }
  }

  protected def receive = {
    case Save(result: Result) => save(result)
    case Query4League(league: String) => {

      val t: TraversalDescription = Traversal.description().
        depthFirst().
        relationships(EventRel()).
        evaluator(new LeagueEvaluator(league))

      val traverser: Traverser = t.traverse(getEventsReferenceNode())

      val iterator: Iterator[Path] = traverser.iterator()
      JavaConversions.asIterator(iterator).foreach(path => println(Event(path.endNode())))

    }
    case msg@_ => {
      logger.info("Received unsupported message [{}].", msg)
      save(Result(Event("a", "b", "c", "d"), "e", "f", "g"))
    }
  }

  private def getEventsReferenceNode(): Node = {
    db.getReferenceNode.getSingleRelationship(EventsReference(), Direction.OUTGOING) match {
      case r: Relationship => {
        logger.info("Events reference node already exists.")
        r.getEndNode
      }
      case _ => {

        val tx: Transaction = db.beginTx()

        try {
          val eventsReference: Node = db.createNode()
          db.getReferenceNode.createRelationshipTo(eventsReference, EventsReference())
          logger.info("Created and linked events reference node.")
          tx.success()

          eventsReference
        }
        finally {
          tx.finish()
        }

      }
    }
  }

  private def getEventNode(event: Event): Node = {

    val eventsNode: Node = getEventsReferenceNode()

    val t: TraversalDescription = Traversal.description().
      depthFirst().
      relationships(EventRel()).
      evaluator(new EventEvaluator(event))

    val traverser: Traverser = t.traverse(getEventsReferenceNode())

    val iterator: Iterator[Path] = traverser.iterator()
    if (iterator.hasNext) {
      iterator.next().endNode()
    } else {
      println("Event node not found for [%s].".format(event))

      val eventNode: Node = db.createNode()
      eventNode.setProperty("league", event.league)
      eventNode.setProperty("date", event.date)
      eventNode.setProperty("home", event.home)
      eventNode.setProperty("away", event.away)

      val rel: Relationship = eventsNode.createRelationshipTo(eventNode, EventRel())
      rel.setProperty("league", event.league)
      rel.setProperty("date", event.date)
      rel.setProperty("home", event.home)
      rel.setProperty("away", event.away)

      logger.info("Event node for [{}] successfully created.", event)

      eventNode
    }
  }

  def save(result: Result) {

    val tx: Transaction = db.beginTx()
    try {

      val eventNode: Node = getEventNode(result.event)

      val resultNode: Node = db.createNode()
      resultNode.setProperty("homeScore", result.homeScore)
      resultNode.setProperty("awayScore", result.awayScore)
      resultNode.setProperty("time", result.time)
      resultNode.createRelationshipTo(eventNode, BelongsTo())

      eventNode.getSingleRelationship(Current(), Direction.OUTGOING) match {
        case r: Relationship => {
          eventNode.createRelationshipTo(r.getEndNode, Previous())
          r.delete()
          eventNode.createRelationshipTo(resultNode, Current())
          logger.info("Updated current status for event [{}].", result.event)
        }
        case _ => {
          eventNode.createRelationshipTo(resultNode, Current())
          logger.info("Initialized event [{}] with current status.", result.event)
        }
      }

      tx.success()

    } finally {
      tx.finish()
    }
  }

}

case class EventsReference() extends RelationshipType {
  def name() = this.getClass.getName
}

case class EventRel() extends RelationshipType {
  def name() = this.getClass.getName
}

case class BelongsTo() extends RelationshipType {
  def name() = this.getClass.getName
}

case class Current() extends RelationshipType {
  def name() = this.getClass.getName
}

case class Previous() extends RelationshipType {
  def name() = this.getClass.getName
}