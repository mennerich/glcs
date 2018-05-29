package helpers

import scala.annotation.tailrec
import scala.math.BigDecimal
import models.Entry

case class Averages(total: Double, morning: Double, noon: Double, evening: Double)

trait StatsSupport {
  def getAverages(xs: List[Entry]): Averages = {
    val size = xs.size.toDouble
    @tailrec
    def inner(xs: List[Entry], accum: Int): Averages = {
      xs match {
        case x :: tail => {
          inner(tail, accum + x.reading)
        }
        case Nil => new Averages((round(accum.toDouble / size)), 0.0, 0.0, 0.0)
      }
    }
    inner(xs, 0)
  }

  private def round(double: Double): Double = { BigDecimal(double).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble }
}