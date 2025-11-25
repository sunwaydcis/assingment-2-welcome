import com.github.tototoshi.csv._

object CsvReader {
  def read(): Unit =
    val reader = CSVReader.open("src/main/resources/Hotel_Dataset.csv")
    val rows: List[Map[String, String]] = reader.allWithHeaders()
    reader.close()

    println(rows.head)
    println(rows.length)
  end read
}





object Main extends App:
  CsvReader.read()
end Main