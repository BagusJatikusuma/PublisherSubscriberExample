package id.nukuba.terminal

import zio._
import zio.console.Console
import id.nukuba.terminal.Terminal.Service

object TerminalService {
  def live: ZLayer[Console, Nothing, HasTerminal] = ZLayer.fromFunction { console =>
    new Service {
      override def getCommand: ZIO[Any,Throwable,String] = 
        for {
          _       <- console.get.putStr(s"${ (scala.Console.WHITE_B ++ scala.Console.BLACK).mkString } COMMAND >>${scala.Console.RESET} ")
          input   <- console.get.getStrLn
        } yield input

      override def print(input: String): ZIO[Any,Nothing,Unit] = console.get.putStrLn(input)
    }
  }
}
