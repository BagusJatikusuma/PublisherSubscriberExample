package id.nukuba

import zio._
import zio.console._
import zio.clock._

import id.nukuba.file._
import id.nukuba.domain._

package object partition {

  trait Service {
    def initWorker(
      workerTask: Queues[(PublisherState, List[String])] 
    ): URIO[Console with Clock, List[Fiber[Nothing, Unit]]] 

    def initPublisher(
      workerTasks: Queues[(PublisherState, List[String])]
      , maxPending: Int
      , conf: ReadFileConf
      , fileName: String
      , byteBufferSize: Int
    ): ZIO[HasFile with Console with Clock, Throwable, Unit]

  }

  type HasPartition = Has[Service]

  def initWorker(
    workerTask: Queues[(PublisherState, List[String])]
  ): URIO[HasPartition with Console with Clock, List[Fiber[Nothing, Unit]]] =
    ZIO.accessM(_.get.initWorker(workerTask))

  def initPubliser(
    workerTasks: Queues[(PublisherState, List[String])]
    , maxPending: Int
    , conf: ReadFileConf
    , fileName: String
    , byteBufferSize: Int
  ): ZIO[HasPartition with HasFile with Console with Clock, Throwable, Unit] =
    ZIO.accessM(_.get.initPublisher(workerTasks, maxPending, conf, fileName, byteBufferSize))

}
