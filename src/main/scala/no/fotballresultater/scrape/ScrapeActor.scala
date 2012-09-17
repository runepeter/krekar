package no.fotballresultater.scrape

import akka.actor.Actor
import org.apache.commons.io.IOUtils
import org.xml.sax.InputSource
import xml.parsing.TagSoupFactoryAdapter
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.HttpClient
import org.slf4j.{Logger, LoggerFactory}
import org.apache.http.HttpResponse
import no.fotballresultater.{Mine, Parse, Scrape}
import xml.{NodeSeq, Node}
import java.util.regex.{Matcher, Pattern}
import no.fotballresultater.util.ScrapeUtil
import org.apache.commons.lang.StringUtils
import no.fotballresultater.domain.{Result, Event}
import no.fotballresultater.result.ResultActor
import java.io.InputStream

class ScrapeActor extends Actor {

  private val logger: Logger = LoggerFactory.getLogger(classOf[ScrapeActor])
  private val http: HttpClient = new DefaultHttpClient()

  protected def receive = {
    case Scrape(url) => retrieveHtml(url)
    case Mine(node) => doMining(node)
    case msg@_ => logger.warn("Unknown message received [{}].", msg)
  }

  def retrieveHtml(url: String) {

    val get: HttpGet = new HttpGet(url)
    val response: HttpResponse = http.execute(get)
    logger.info(response.getStatusLine.toString)

    parseHtml(response.getEntity.getContent)
    logger.info("Successfully parsed HTML from URL [{}].", url)
  }

  def parseHtml(htmlStream: InputStream) {

    val source: InputSource = new InputSource(htmlStream)
    source.setEncoding("iso-8859-1")
    val node: Node = new TagSoupFactoryAdapter().load(source)

    (self ! Mine(node))

    logger.info("Successfully parsed HTML as XML node.")
  }

  def doMining(html: Node) {

    val table: Node = (((html \ "body" \ "table" \ "tr")(3) \ "td" \ "table" \ "tr" \ "td")(4) \ "table").head
    val rows: List[Node] = eventRows(table)

    var head: Node = rows.head
    var tail: List[Node] = rows.tail

    var league: String = ""
    var time: String = ""
    var date: String = ""

    while (head != null) {

      val parts: NodeSeq = head \ "td"

      if (hasAttribute("bgcolor", "#333333", head)) {

        if (hasGameCell(head)) {
          league = strip(parts(0).text)
        }

        val seq: NodeSeq = head \ "td"
        if (seq.size > 1) {
          time = strip(parts(0).text)
          date = strip(parts(1).text)
        }
      }

      if (hasAttribute("bgcolor", "#cfcfcf", head) || hasAttribute("bgcolor", "#dfdfdf", head)) {
        val gameTime: String = ScrapeUtil.toGameTime(date, strip(parts(0).text))
        val homeTeam = strip(parts(1).text)
        val result = strip(parts(2).text)
        val awayTeam = strip(parts(3).text)

        var homeScore: String = "?"
        var awayScore: String = "?"

        val matcher: Matcher = ScrapeActor.PATTERN.matcher(result)
        if (matcher.matches) {
          homeScore = matcher.group(1)
          awayScore = matcher.group(2)
        }

        (Actor.registry.actorsFor[ResultActor].head ! Result(Event(league, date, homeTeam, awayTeam), homeScore, awayScore, gameTime))
      }

      if (!tail.isEmpty) {
        head = tail.head
        tail = tail.tail
      } else {
        head = null
      }

    }

  }

  def eventRows(table: Node): List[Node] = {
    var rows: NodeSeq = table \ "tr"
    rows = rows.filterNot(row => hasAttribute("bgcolor", "#111111", row))
    rows = rows.filterNot(row => row.attribute("bgcolor") == None)
    rows.toList
  }

  def hasAttribute(name: String, value: String, node: Node): Boolean = {
    node.attribute(name) match {
      case Some(values) => values.exists(attributeValue => attributeValue.text == value)
      case None => false
    }
  }

  def hasGameCell(row: Node): Boolean = {
    (row \ "td").exists(cell => hasAttribute("class", "title", cell))
  }

  def strip(text: String): String = {
    val array: Array[Char] = Array[Char](0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 160)
    val strippers: String = String.valueOf(array)
    var stripped: String = StringUtils.stripStart(text, strippers)
    StringUtils.stripEnd(stripped, strippers)
  }

}

object ScrapeActor {
  val PATTERN: Pattern = Pattern.compile(".*?([0-9]+).*?([0-9]+).*")
}