package org.brylex.krekar

import file.FileProcessor
import java.io.File
import akka.actor.Actor.actorOf
import akka.dispatch.Dispatchers
import akka.config.Supervision.{SupervisorConfig, AllForOneStrategy, Supervise, Permanent}
import akka.actor.{Supervisor, Actor}
import org.slf4j.{Logger, LoggerFactory}

class FileEndpoint(val numHandlers: Int) extends Actor {

  private val logger: Logger = LoggerFactory.getLogger(classOf[FileEndpoint])

  val workStealingDispatcher = Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher("pooled-dispatcher")
    .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(5)
    .setCorePoolSize(numHandlers)
    .build

  val actors: List[Supervise] = (for (i <- 0 until numHandlers) yield Supervise(actorOf(new FileProcessor(workStealingDispatcher)), Permanent)).toList

  val supervisor = Supervisor(
    SupervisorConfig(
      AllForOneStrategy(List(classOf[Exception]), 3, 1000),
      actors
    )).start

  val idempotentCache: scala.collection.mutable.Set[File] = scala.collection.mutable.Set[File]()

  protected def receive = {
    case Poll(dir: File) => {

      logger.info("Received Poll request for directory [{}].", dir)

      val files: Array[File] = dir.listFiles()
      if (files == null) {
        logger.info("Directory [{}] does not exist.", dir)
      }

      val batch: Array[File] = files.filterNot(f => idempotentCache.contains(f)).slice(0, scala.math.min(80, files.length))
      logger.info("Found {} files ready for processing.", batch.size)

      batch.foreach {
        f =>
          actors.head.actorRef ! Process(f)
          idempotentCache.add(f)
      }

      logger.info("Done polling directory [{}] for files to process.", dir)
    }
    case _ => {
      println("JALLA, BALLA!")
    }
  }
}