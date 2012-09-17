package no.fotballresultater

import domain.Result
import xml.Node

sealed trait Msg

case class Scrape(url: String)
case class Parse(html: String)
case class Mine(node: Node)

case class Save(result: Result) extends Msg
case class Query4League(league: String) extends Msg
