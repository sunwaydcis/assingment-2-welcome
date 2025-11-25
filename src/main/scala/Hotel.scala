import com.github.tototoshi.csv._

object CsvReader {
  val reader = CSVReader.open("src/main/resources/Hotel_Dataset.csv")
  val rows: List[Map[String, String]] = reader.allWithHeaders()

  def read(): Unit =
    val reader = CSVReader.open("src/main/resources/Hotel_Dataset.csv")
    val rows: List[Map[String, String]] = reader.allWithHeaders()
    reader.close()

    println(rows.head)
    println(rows.length)
  end read
  def stop(): Unit = reader.close()
}

class BookCount:
  val destinatedCountry: List[String] = rows.map(row => row("Destination Country"))
  val countryCount: Map[String, Int] = destinatedCountry.groupBy(identity).view.mapValues(_.size).toMap
  def highestBookingCount(): Unit = println(countryCount.head)
end BookCount

object Main extends App:

end Main