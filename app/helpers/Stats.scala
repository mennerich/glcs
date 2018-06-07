package helpers

import scala.annotation.tailrec
import scala.math.BigDecimal
import models.{ Entry, EntryRepo }

case class Averages(total: Double, morning: Double, noon: Double, evening: Double)
case class Counts(morning: Int, noon: Int, evening: Int)

trait StatsSupport {
  
  def getAverages(xs: List[Entry]): Averages = {
    val size = xs.size
    
    @tailrec
    def inner(entries: List[Entry], averages: Averages, counts: Counts): Averages = {
      entries match {
        case entry :: tail => { 
          entry.readingTime match {
            case 1 => {
              inner(
                tail, 
                new Averages(averages.total + entry.reading, averages.morning + entry.reading, averages.noon, averages.evening),
                new Counts(counts.morning + 1, counts.noon, counts.evening))
            }
            
            case 2 => {
              inner(
                tail, 
                new Averages(averages.total + entry.reading, averages.morning, averages.noon + entry.reading, averages.evening),
                new Counts(counts.morning, counts.noon + 1, counts.evening))
            }

            case 3 => {
              inner(
                tail, 
                new Averages(averages.total + entry.reading, averages.morning, averages.noon, averages.evening + entry.reading),
                new Counts(counts.morning, counts.noon, counts.evening + 1))    
            }

            case _ => throw new Exception
          }
        }

        case Nil => new Averages(
          round(averages.total,size), 
          round(averages.morning,counts.morning), 
          round(averages.noon,counts.noon), 
          round(averages.evening,counts.evening))
      }
    }
    
    inner(xs, Averages(0.0, 0.0, 0.0, 0.0), Counts(0,0,0))
  }

  private def round(sum: Double, count: Int): Double = {
    count match {
      case 0 => 0.00
      case _ => BigDecimal(sum.toDouble / count.toDouble).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble 
    }
  }

}

