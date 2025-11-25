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
  def highestBookingCount(): Unit = println(sortedCountryCount.tail)
end BookCount

object Main extends App:
  val question1 = new BookCount
  question1.highestBookingCount()
end Main