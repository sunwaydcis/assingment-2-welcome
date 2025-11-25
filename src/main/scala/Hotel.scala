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





object Main extends App:
  CsvReader.read()
end Main