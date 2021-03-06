package id.nukuba

import zio._
import zio.console._
import zio.clock._
import zio.duration._

import file._
import file.FileLive
import partition._
import domain._

object Main extends scala.App {

  def createEmptyQueues = Ref.make(Map.empty[Int, Queue[(PublisherState, List[String])]])

  def createPartition(
    workerAmount: Int, maxPending: Int
  ): UIO[Queues[(PublisherState, List[String])]] = for {
    ref   <- createEmptyQueues
    _     <- ZIO.foreach((0 until workerAmount))(id => setWorker(id + 1, ref, maxPending))
  } yield ref

  def setWorker(id: Int, ref: Queues[(PublisherState, List[String])], maxPending: Int) = for {
    newQueue   <- Queue.bounded[(PublisherState, List[String])](maxPending)
    _          <- ref.update(_.updated(id, newQueue))
  } yield ()


  val pubsubFileProgram = (for {
    _             <- putStrLn("Bismillah")
    _             <- putStrLn("Welcome to the PubSubFile program")
    _             <- putStr("input worker amount: ")
    wkAmount      <- getStrLn
    _             <- putStr("input max task per worker: ")
    maxPending    <- getStrLn
    _             <- putStr("input file path: ")
    filePath      <- getStrLn
    _             <- putStr("input how many lines per worker : ")
    lineAmount    <- getStrLn
    _             <- putStr("input byte buffer per task: ")
    byteBuffer    <- getStrLn
    refQueues     <- createPartition(wkAmount.toInt, maxPending.toInt)
    readFileConf  <- Task.effect(ReadFileConf(lineAmount.toInt))
    workerFibers  <- initWorker(refQueues)
    _             <- initPubliser(refQueues, maxPending.toInt, readFileConf, filePath, byteBuffer.toInt)
    _             <- ZIO.foreach(workerFibers)(_.await)
  } yield ())
    .provideSomeLayer[ZEnv](Partition.live ++ FileLive.live ++ Console.live ++ Clock.live)

  val runtime = Runtime.default
  runtime.unsafeRun(
    pubsubFileProgram.foldM(
      err   => putStrLn(s"Execution failed with: $err") *> IO.succeed(1)
      , _   => IO.succeed(0)
    )
  )

}
