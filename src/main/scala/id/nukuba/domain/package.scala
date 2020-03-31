package id.nukuba

package object domain {

  import zio._

  case class Census(year: Int, age: Int, ethnic: Int, sex: Int, area: Int, count: Int)
  case class ReadFileConf(lineAmount: Int)
  
  case class Valid(area: Int, count: Int)
  case class Invalid(err: String)

  sealed trait PublisherState
  case object FINSISHED extends PublisherState
  case object CONTINUE extends PublisherState

  type Queues[A] = Ref[Map[Int, Queue[A]]]
}
