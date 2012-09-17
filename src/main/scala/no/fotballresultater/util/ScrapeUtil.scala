package no.fotballresultater.util

import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Calendar, Date}
import org.apache.commons.lang.time.{FastDateFormat, DateUtils}
import org.apache.commons.lang.StringUtils

object ScrapeUtil {

  val DATE_PARSER: DateFormat = new SimpleDateFormat("MMMMM dd yyyy, HH:mm")
  val GAMETIME_PARSER: DateFormat = new SimpleDateFormat("MMMMM dd yyyy")
  val DATE_FORMAT: FastDateFormat = FastDateFormat.getInstance("MMMMM dd yyyy")

  def toDate(date: String, time: String): Date = {

    val nowCalendar: Calendar = Calendar.getInstance
    nowCalendar.setTime(new Date)

    val year: Int = nowCalendar.get(Calendar.YEAR)

    val parsedCalendar: Calendar = Calendar.getInstance
    parsedCalendar.setTime(DATE_PARSER.parse(date + " " + year + ", " + time))

    if (DateUtils.truncatedCompareTo(parsedCalendar, nowCalendar, Calendar.DATE) < 1) {
      DateUtils.addYears(parsedCalendar.getTime, 1)
    } else {
      parsedCalendar.getTime
    }

  }

  def isPendingGameTime(gameTime: String): Boolean = {
    if (StringUtils.isEmpty(gameTime)) {
      false
    } else {
      GAMETIME_PARSER.parse(gameTime) != null
    }
  }

  def toGameTime(date: String, time: String): String = {

    if (time.contains(":")) {

      val parsedDate: Date = toDate(date, time)
      DATE_FORMAT.format(parsedDate)

    } else {
      time
    }

  }

}