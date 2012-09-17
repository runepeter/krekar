package no.fotballresultater.domain

import org.apache.commons.lang.Validate

case class Result(event: Event, homeScore: String = "?", awayScore: String = "?", time: String) {
  Validate.notNull(event)
  Validate.notEmpty(homeScore)
  Validate.notEmpty(awayScore)
  Validate.notEmpty(time)
}