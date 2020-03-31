package id.nukuba.partition

import zio._
import zio.console._
import zio.duration._

import id.nukuba.file._
import id.nukuba.domain._
import java.io.BufferedReader

final class Publisher(
  workerTasks: Queues[(PublisherState, List[String])]
  , maxPending: Int
  , reader: BufferedReader
  , conf: ReadFileConf) {

  def pushTask(
    availWorker: (Int, Queue[(PublisherState, List[String])])
  ): RIO[HasFile with Console, ReadState] = for {
    linesState <- takeLines(conf.lineAmount, reader)
    _          <- availWorker._2.offer((CONTINUE, linesState._1)) //check bagaimana jika file kosong??
  } yield linesState._2

  def setWorkerTask(workersMap: (Int, Queue[(PublisherState, List[String])])) = for {
    size    <- workersMap._2.size
    state   <- if (size < maxPending) 
                putStrLn(s"PUBLISHER: push task to worker: ${workersMap._1}") *> 
                pushTask(workersMap) 
               else UIO(OnGoing)
  } yield state

  def publish = for {
    workers     <- workerTasks.get
    stateList   <- ZIO.foreach(workers.toList)(setWorkerTask) 
  } yield stateList.last
  
  def debug(cause: Cause[Throwable]): ZIO[Console, Nothing, Unit] =
    putStrLn(s"PUBLISHER: hey we got problem: ${cause.prettyPrint}")

  def EOFState(state: ReadState) = state match {
    case EOF     => true
    case OnGoing => false
  }
  //start publisher on different fiber
  def execute = 
    putStrLn("PUBLISHER: start publisher") *> 
    publish
      .doUntil(EOFState)
      .sandbox
      .catchAll(debug)
      .fork

}

object Publisher {
  
  def init(
    workerTasks: Queues[(PublisherState, List[String])]
    , maxPending: Int
    , conf: ReadFileConf
    , fileName: String
    , byteBufferSize: Int
  ) = for {
    readerM   <- createReaderM(fileName, byteBufferSize)
    _         <- readerM.use { reader =>
                  startPublisher(
                    reader, 
                    workerTasks, 
                    maxPending, 
                    conf, 
                    fileName, 
                    byteBufferSize)
                } //close resource when publisher finished
    _         <- notifWorkers(workerTasks) //notif workers that publisher is finished
    _         <- putStrLn(s"PUBLISHER: PROCESS FINISHED")
  } yield ()
  
  def notifWorkers(
    workerTasks: Queues[(PublisherState, List[String])]
  ) = for {
    workers   <- workerTasks.get
    _         <- ZIO.foreach(workers)(_._2.offer((FINSISHED, List.empty[String])))
  } yield ()

  def startPublisher(
    reader: BufferedReader
    , workerTasks: Queues[(PublisherState, List[String])]
    , maxPending: Int
    , conf: ReadFileConf
    , fileName: String
    , byteBufferSize: Int
  ) = for {
    _         <- dropLines(1, reader)//drop first line
    publisher <- UIO.succeed(
                  new Publisher(
                    workerTasks,
                    maxPending,
                    reader,
                    conf)
                )
    fib       <- publisher.execute 
    _         <- fib.await //wait process until it reached EOF
  } yield ()

}
