import CsvReader.rows
import com.github.tototoshi.csv.*

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
  val hotel: List[String] = rows.map(row => row("Hotel Name"))
  val hotelCount: Map[String, Int] = hotel.groupBy(identity).view.mapValues(_.size).toMap
  val sortedHotelCount = ListMap(hotelCount.toSeq.sortBy(_._2): _*)

  val profitMargin: List[String] = rows.map(row => row("Profit Margin"))
  val profitMarginCount: Map[String, Int] = profitMargin.groupBy(identity).view.mapValues(_.size).toMap
  val sortedProfitMarginCount = ListMap(profitMarginCount.toSeq.sortBy(_._2): _*)

  def highestHotelCount(): Unit = println(sortedHotelCount.maxBy(_._2))
  def highestProfitMarginCount(): Unit = println(sortedProfitMarginCount.maxBy(_._2))
end MaxProfit

object Main extends App:
  val question1 = new BookCount
  question1.highestBookingCount()
end Main