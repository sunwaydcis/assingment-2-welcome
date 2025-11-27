import CsvReader.rows
import com.github.tototoshi.csv.*
import scala.collection.immutable.ListMap

object CsvReader {
  val reader = CSVReader.open("src/main/resources/Hotel_Dataset.csv")
  val rows: List[Map[String, String]] = reader.allWithHeaders()

  def stop(): Unit = reader.close()
}

trait FilteringDatasets:
  def filter(filteredMap: List[String]): List[Map[String, String]] =
    rows.map { row =>
      row.filter { case (key, _) => filteredMap.contains(key) }
    }
end FilteringDatasets

class BookCount extends FilteringDatasets:
  val filteredList: List[String] = filter(List("Destination Country")).flatMap(_.values)
  val countryCount: Map[String, Int] = filteredList.groupBy(identity).view.mapValues(_.size).toMap

  def highestBookingCount(): Unit = println(countryCount.maxBy(_._2))
end BookCount

class MaxEconomic extends FilteringDatasets:
  //is the booking price discounted already or prediscount
  //if so, then we use (booking price / 1 + discount) * profit margin
  val filteredList: List[Map[String, String]] = filter(List("Hotel Name", "Booking Price[SGD]", "Discount", "Profit Margin"))
  val groupedList: Map[String, List[Map[String, String]]] = filteredList.groupBy(row => row("Hotel Name"))
  var listOfHotel: List[Map[String, Double]] = List()
  for ((hotelName, dataRows) <- groupedList) {
    val economicRanking = dataRows.map { row =>
      (row("Booking Price[SGD]").toDouble / (1 - row("Discount").stripSuffix("%").toDouble / 100)) * row("Profit Margin").toDouble
    }.sum
    val _economicHotel: Map[String, Double] = Map(hotelName -> economicRanking)
    listOfHotel = listOfHotel :+ _economicHotel
  }

  def mostEconomicalHotel(): Unit = println(listOfHotel.map(_.head).minBy(_._2))
end MaxEconomic

class MaxProfit extends FilteringDatasets:
  //most profitable logic is sum of (visitor[default price of 100SGD] * profit margin) group by each hotel due to not considering booking price
  //if count booking price, then can sum of (booking price * profit margin) for each visitor then group by hotel to get most profitable hotel
  val filteredList: List[Map[String, String]] = filter(List("Hotel Name", "No. Of People", "Profit Margin"))
  val groupedList: Map[String, List[Map[String, String]]] = filteredList.groupBy(row => row("Hotel Name"))
  var listOfHotel: List[Map[String, Double]] = List()
  for ((hotelName, dataRows) <- groupedList) {
    val totalProfit = dataRows.map { row =>
      row("No. Of People").toDouble * row("Profit Margin").toDouble
    }.sum
    val _hotelProfit: Map[String, Double] = Map(hotelName -> totalProfit)
    listOfHotel = listOfHotel :+ _hotelProfit
  }

  def mostProfitableHotel(): Unit = println(listOfHotel.map(_.head).maxBy(_._2))
end MaxProfit

object Main extends App:
  val question1 = new BookCount
  question1.highestBookingCount()

  val question2 = new MaxEconomic
  question2.mostEconomicalHotel()

  val question3 = new MaxProfit
  question3.mostProfitableHotel()
end Main