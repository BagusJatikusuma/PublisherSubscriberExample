package id.nukuba.file

import zio._
import zio.duration._
import scala.io._
import java.io.FileReader
import java.io.BufferedReader

final class FileLive extends File.Service {
  
  def initReaderFile(file: String, byteBuffer: Int): UIO[ReaderM] = 
    UIO.succeed(FileLive.createSource(file, byteBuffer))

  def takeLines(lineSize: Int, fileReader: BufferedReader): Task[(List[String], ReadState)] = Task.effect {
    val lines = 
      (0 until lineSize).foldLeft(List.empty[String])((list, _) => {
          val line = fileReader.readLine
          
          if (line != null)
            line :: list
          else 
            list
          
        }
      ).reverse

    if (lines.length < lineSize) { //check apakah list.empty prepend menambah 1 pada slot list!!
      (lines, EOF)
    } else {
      (lines, OnGoing)
    }
  }

  def dropLines(lineSize: Int, fileReader: BufferedReader): Task[Unit] = Task.effect {
    var i  = 0
    while (i < lineSize) {
      fileReader.readLine
      i += 1
    }
    ()
  }

}

object FileLive {

  def live: Layer[Nothing, HasFile] = ZLayer.succeed(new FileLive)  
  
  private def openFile(file: String) = Task.effect {
    new FileReader(file)
  }

  private def bufferFile(reader: FileReader, byteBuffer: Int) = Task.effect {
    new BufferedReader(reader, byteBuffer)
  }

  private val constructBufferedReader = (file: String, byteBuffer: Int) => for {
    reader      <- openFile(file)
    buffReader  <- bufferFile(reader, byteBuffer)
  } yield buffReader

  private val closeFile: BufferedReader => UIO[Unit] = buffReader => Task.effect(buffReader.close).catchAll(_ => UIO.unit)

  def createSource(file: String, byteBufferSize: Int) = Managed.make(constructBufferedReader(file, byteBufferSize))(closeFile) 

}
