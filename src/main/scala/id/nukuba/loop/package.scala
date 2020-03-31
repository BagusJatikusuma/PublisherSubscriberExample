package id.nukuba

import zio._
import zio.console._
import id.nukuba.terminal._

package object loop {
  object RunLoop {
    trait Service {
      def runLoop: ZIO[HasTerminal, Throwable, Unit]
    }
  }

  type HasRunLoop = Has[RunLoop.Service]

  def loop: ZIO[HasRunLoop with HasTerminal, Throwable, Unit] =
    ZIO.accessM(_.get.runLoop)

}

import loop._
object RunLoopLive {
  def live: ZLayer[Any, Nothing, HasRunLoop] = ZLayer.succeed {
    new RunLoop.Service {
      override def runLoop: ZIO[HasTerminal,Throwable,Unit] = {
        def loop: ZIO[HasTerminal, Throwable, Unit] = for {
          command <- getCommand
          _       <- if (command.toLowerCase == "exit") 
                       UIO.unit
                     else loop
        } yield ()

        for {
          _   <- print("+++++ WELCOME +++++")
          _   <- loop
        } yield ()

      }

    }
  }

}

