package no.fotballresultater.domain

import org.apache.commons.lang.Validate
import org.neo4j.graphdb.Node

case class Event(league: String, date: String, home: String, away: String) {
  Validate.notEmpty(league)
  Validate.notEmpty(date)
  Validate.notEmpty(home)
  Validate.notEmpty(away)
}

object Event {
  def apply(node: Node): Event = {
    Event(
      node.getProperty("league").asInstanceOf[String],
      node.getProperty("date").asInstanceOf[String],
      node.getProperty("home").asInstanceOf[String],
      node.getProperty("away").asInstanceOf[String])
  }
}
