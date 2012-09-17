package org.brylex.krekar

import java.io.File

sealed trait KrekarMessage

case class Poll(file: File) extends KrekarMessage
case class Process(file: File) extends KrekarMessage