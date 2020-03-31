package id.nukuba.partition

import zio._
import zio.console._
import zio.clock._
import zio.duration._

import id.nukuba.domain._
import scala.collection.mutable
 
final class Worker(
  idWorker: Int, 
  queue: Queue[(PublisherState, List[String])],
  validList: List[Valid],
  invalidList: List[Invalid]) {

  def takeTask = putStrLn(s"WORKER $idWorker: take task") *> queue.take //will suspend fiber if queue is empty

  def processLine(lines: (PublisherState, List[String])): ZIO[Console, Nothing, Unit] = {
    var validListParser = List.empty[Valid]
    var invalidListParser = List.empty[Invalid]

    def parseLine(line: String, separator: String) = Task.effect(line.trim.split(separator))
    
    def toInt(s: String) = Task.effect(s.toInt)

    def toCensus(arr: Array[String]): URIO[Any, Either[String, Census]] = (for {
      year    <- toInt(arr(0))
      age     <- toInt(arr(1))
      ethnic  <- toInt(arr(2))
      sex     <- toInt(arr(3))
      area    <- toInt(arr(4))
      count   <- toInt(arr(5))
    } yield Census(year, age, ethnic, sex, area, count))
      .fold(
        err    => Left(err.toString.split("\n").take(3).mkString), 
        census => Right(census))

    def insertList(state: Either[String, Census]) = state match {
      case Left(err)     => invalidListParser = Invalid("err") :: invalidListParser
      case Right(census) => validListParser = Valid(census.area, census.count) :: validListParser
    }
    
    def program(line: String, separator: String) = (for {
      words   <- parseLine(line, separator)
      state   <- toCensus(words)
      _       <- UIO(insertList(state))
    } yield ())

    for {
      _ <- putStrLn(s"WORKER ${idWorker}: processing ${lines._2.size} begin")
      _ <- ZIO.foreach(lines._2)(line => program(line, """,""")).sandbox.catchAll(debug)
      _ <- putStrLn(s"WORKER $idWorker: FINSISHED with success : ${validListParser.length}, err : ${invalidListParser.length}")
    } yield ()
    
  }

  def debug(cause: Cause[Throwable]): ZIO[Console, Nothing, Unit] =
    putStrLn(s"WORKER $idWorker : hey we got problem ${cause.prettyPrint}")

  def decideProcess(lineState: (PublisherState, List[String])) = lineState._1 match {
    case FINSISHED  => UIO(true) 
    case CONTINUE   => processLine(lineState) *> UIO(false)
  }

  //execute task on different fiber
  def execute = 
    putStrLn("WORKER: start execute") *> 
    (takeTask flatMap decideProcess).doUntil(_ == true).unit.fork

}

object Worker {
  //create worker, associate it with its tasks
  def apply(
    id: Int, 
    task: Queue[(PublisherState, List[String])], 
    validList: List[Valid], 
    invalidList: List[Invalid]) = 
      new Worker(id, task, validList, invalidList)

  def getWorkers(workerTask: Queues[(PublisherState, List[String])]) =
    workerTask.get.map( mapWorkerTask => mapWorkerTask.map {
      case (key, task) => apply(key, task, List.empty, List.empty)
    })

  def init(
    workerTask: Queues[(PublisherState, List[String])]
  ): ZIO[Console with Clock,Nothing,List[Fiber[Nothing, Unit]]] = for {
    workers       <- getWorkers(workerTask)
    workerfibers  <- ZIO.foreach(workers)(_.execute) //execute workers
  } yield workerfibers

}
