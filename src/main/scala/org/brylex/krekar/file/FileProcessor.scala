package org.brylex.krekar.file

import akka.actor.Actor
import akka.dispatch.MessageDispatcher
import org.brylex.krekar.Process
import java.io.File
import org.apache.commons.io.{FileUtils}
import org.slf4j.{LoggerFactory, Logger}

class FileProcessor(val dispatcher: MessageDispatcher) extends Actor {

  private val logger: Logger = LoggerFactory.getLogger(this.toString)

  self.dispatcher = dispatcher

  protected def receive = {
    case Process(file: File) => {

      if (file.exists && !file.isDirectory) {

        val workDir: File = new File(file.getParentFile,  ".krekar/")
        FileUtils.moveFileToDirectory(file, workDir, true)

        val sleepTime: Long = (1000 * scala.math.random).asInstanceOf[Int]
        Thread.sleep(sleepTime)
        logger.info("File [{}] has successfully been processed ({} ms.).", file, sleepTime)

      } else {
        if (file.isDirectory) {
          logger.info("Directory [%s] cannot be processed.".format(file))
        } else {
          logger.info("The file [%s] is no longer available.".format(file))
        }
      }

    }
    case _ => {
      println("processing...")
    }
  }
}