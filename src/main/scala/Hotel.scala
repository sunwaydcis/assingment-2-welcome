import com.github.tototoshi.csv.*

class CsvReader(val filePath: String):
  private val reader = CSVReader.open(filePath)
  private val rows: List[Map[String, String]] = reader.allWithHeaders()

  def getData: List[Map[String, String]] = rows
end CsvReader

trait FilteringDatasets:
  val rows: List[Map[String, String]]

  def filterColumn(filteredMap: List[String]): List[Map[String, String]] =
    rows.map { row =>
      row.filter { case (key, _) => filteredMap.contains(key) }
    }
  def filterColumn(filteredMap: String): List[String] = rows.map(row => row(filteredMap))

  def groupByColumn(groupingMap: String): Map[String, List[Map[String, String]]] =
    rows.groupBy(row => row(groupingMap))
end FilteringDatasets


class MaxBookCount(val rows: List[Map[String, String]]) extends FilteringDatasets:
  val filteredList: List[String] = filterColumn("Destination Country")
  val countryCount: Map[String, Int] = filteredList.groupBy(identity).view.mapValues(_.size).toMap

  def highestBookingCount(): Unit = println(countryCount.maxBy(_._2))
end MaxBookCount

class MaxEconomic(val rows: List[Map[String, String]]) extends FilteringDatasets:
  //is the booking price discounted already or prediscount
  //if so, then we use (booking price / 1 + discount) * profit margin
  val filteredList: List[Map[String, String]] = filterColumn(List("Hotel Name", "Booking Price[SGD]", "Discount", "Profit Margin"))
  val groupedList: Map[String, List[Map[String, String]]] = groupByColumn("Hotel Name")
  var listOfHotel: List[Map[String, Double]] = List()
  for ((hotelName, dataRows) <- groupedList) {
    val economicRanking = dataRows.map { row =>
      (row("Booking Price[SGD]").toDouble / (1 + row("Discount").stripSuffix("%").toDouble / 100)) * row("Profit Margin").toDouble
    }.sum
    val _economicHotel: Map[String, Double] = Map(hotelName -> economicRanking)
    listOfHotel = listOfHotel :+ _economicHotel
  }

  def mostEconomicalHotel(): Unit = println(listOfHotel.map(_.head).minBy(_._2))
end MaxEconomic

class MaxProfit(val rows: List[Map[String, String]]) extends FilteringDatasets:
  //most profitable logic is sum of (visitor[default price of 100SGD] * profit margin) group by each hotel due to not considering booking price
  //if count booking price, then can sum of (booking price * profit margin) for each visitor then group by hotel to get most profitable hotel
  val filteredList: List[Map[String, String]] = filterColumn(List("Hotel Name", "No. Of People", "Profit Margin"))
  //cause normal filtering is so short, do i need to purposely put it as a trait
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
  val dataset = new CsvReader("src/main/resources/Hotel_Dataset.csv").getData

  val question1 = new MaxBookCount(dataset)
  val question2 = new MaxEconomic(dataset)
  val question3 = new MaxProfit(dataset)

  question1.highestBookingCount()
  question2.mostEconomicalHotel()
  question3.mostProfitableHotel()
end Main