package id.nukuba

import zio._

package object file {

  import java.io.BufferedReader

  type ReaderM = Managed[Throwable, BufferedReader]

  object File {
    trait Service {
      def initReaderFile(file: String, byteBuffer: Int): UIO[ReaderM]
      def takeLines(lineSize: Int, fileReader: BufferedReader): Task[(List[String], ReadState)]
      def dropLines(lineSize: Int, fileReader: BufferedReader): Task[Unit]
    }
  }

  sealed trait ReadState
  case object OnGoing extends ReadState
  case object EOF extends ReadState

  type HasFile = Has[File.Service]
  
  def createReaderM(file: String, byteBufferSize: Int): ZIO[HasFile, Throwable, ReaderM] =
    ZIO.accessM(_.get.initReaderFile(file, byteBufferSize))

  def takeLines(lineSize: Int, fileReader: BufferedReader): ZIO[HasFile, Throwable, (List[String], ReadState)] =
    ZIO.accessM(_.get.takeLines(lineSize, fileReader))

  def dropLines(lineSize: Int, fileReader: BufferedReader): ZIO[HasFile, Throwable, Unit] =
    ZIO.accessM(_.get.dropLines(lineSize, fileReader))

}
