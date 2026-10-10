package com.redmellon.common.disk

import com.redmellon.common.util.Logging

import java.io.{File, FileOutputStream, PrintWriter}
import java.nio.file.Files
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class QueueBackedCsvAppender[T](filePath: String, converter: CsvConvertible[T]) extends Logging {

  private val file = new File(filePath)
  if (!file.exists()) {
    file.getParentFile.mkdirs()
    file.createNewFile()
  }
  private val fileOutputStream = new FileOutputStream(filePath, true)
  private val fileChannel = fileOutputStream.getChannel
  private val writer = new PrintWriter(fileOutputStream, true)
  private val queue = mutable.Queue.empty[String]
  private val lock = new ReentrantLock(true)

  QueueBackedCsvAppender.executor.scheduleAtFixedRate(
    () =>
      withLock {
        val rows = ListBuffer.empty[String]
        while (queue.nonEmpty) rows.addOne(queue.dequeue())
        val data = rows.filterNot(_.trim.isEmpty).mkString(System.lineSeparator())
        if (data.trim.nonEmpty) {
          writeToDisk(data)
          fileChannel.force(true)
        }
      },
    0,
    500,
    TimeUnit.MILLISECONDS
  )

  if (Files.size(file.toPath) == 0)
    withLock(writeToDisk(converter.toHeader))

  private def withLock(f: => Unit): Unit = {
    lock.lock()
    try f
    finally lock.unlock()
  }

  private def writeToDisk(data: String): Unit =
    try writer.println(data)
    catch { case e: Exception => warn(s"Error writing to $filePath: ${e.getMessage}") }

  def add(record: String): Unit = withLock(queue.addOne(record))
}

object QueueBackedCsvAppender {
  private[disk] val executor = Executors.newScheduledThreadPool(20)
}
