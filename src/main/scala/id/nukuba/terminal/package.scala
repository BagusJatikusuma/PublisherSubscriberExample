package id.nukuba

import zio._

package object terminal {
  object Terminal {
    trait Service {
      def getCommand: ZIO[Any, Throwable, String]
      def print(input: String): ZIO[Any, Nothing, Unit]
    }
  }

  type HasTerminal = Has[Terminal.Service]

  def getCommand: ZIO[HasTerminal, Throwable, String] =
    ZIO.accessM(_.get.getCommand)

  def print(input: String): ZIO[HasTerminal, Nothing, Unit] =
    ZIO.accessM(_.get.print(input))

}
