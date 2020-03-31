package id.nukuba.partition

import zio._
import zio.console._
import zio.clock._

import id.nukuba.domain._
import id.nukuba.file._

final class PartitionLive extends Service {
  def initWorker(
    workerTask: Queues[(PublisherState, List[String])]
  ): URIO[Console with Clock, List[Fiber[Nothing, Unit]]] =
    Worker.init(workerTask)

  def initPublisher(
    workerTasks: Queues[(PublisherState, List[String])]
    , maxPending: Int
    , conf: ReadFileConf
    , fileName: String
    , byteBufferSize: Int
  ): ZIO[HasFile with Console with Clock, Throwable, Unit] = 
    Publisher.init(workerTasks, maxPending, conf, fileName, byteBufferSize)
}

object Partition {
  val live: Layer[Nothing, HasPartition] = ZLayer.succeed(new PartitionLive)
}
