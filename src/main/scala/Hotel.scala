import CsvReader.rows
import com.github.tototoshi.csv.*
import scala.collection.immutable.ListMap

object CsvReader {
  val reader = CSVReader.open("src/main/resources/Hotel_Dataset.csv")
  val rows: List[Map[String, String]] = reader.allWithHeaders()

  def stop(): Unit = reader.close()
}

class BookCount:
  val destinatedCountry: List[String] = rows.map(row => row("Destination Country"))
  val countryCount: Map[String, Int] = destinatedCountry.groupBy(identity).view.mapValues(_.size).toMap
  val sortedCountryCount = ListMap(countryCount.toSeq.sortBy(_._2):_*)

  def highestBookingCount(): Unit = println(sortedCountryCount.maxBy(_._2))
end BookCount

class MaxProfit:
  //most profitable logic is sum of (visitor[default price of 100SGD] * profit margin) group by each hotel due to not considering booking price
  //if count booking price, then can sum of (booking price * profit margin) for each visitor then group by hotel to get most profitable hotel
  val filteredList: List[Map[String, String]] = rows.map { row =>
    row.filter { case (key, _) => List("Hotel Name", "No. Of People", "Profit Margin").contains(key) }
  }
  val groupedList: Map[String, List[Map[String, String]]] = filteredList.groupBy(row => row("Hotel Name"))
  var listOfHotelProfit: List[Map[String, Double]] = List()
  for ((hotelName, dataRows) <- groupedList) {
    val totalProfit = dataRows.map { row =>
      row("No. Of People").toDouble * row("Profit Margin").toDouble
    }.sum
    val _hotelProfit: Map[String, Double] = Map(hotelName -> totalProfit)
    listOfHotelProfit = listOfHotelProfit :+ _hotelProfit
  }
  listOfHotelProfit.foreach(println)
end MaxProfit

object Main extends App:
  val question1 = new BookCount
  question1.highestBookingCount()

  val question3 = new MaxProfit
end Main