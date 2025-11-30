import com.github.tototoshi.csv.*
import scala.language.postfixOps

case class HotelDataset(
  bookingID: String,
  dateOfBooking: String,
  time: String,
  customerID: String,
  gender: String,
  age: Int,
  originCountry: String,
  state: String,
  location: String,
  destinationCountry: String,
  destinationCity: String,
  numberOfPeople: Int,
  checkInDate: String,
  numberOfDays: Int,
  checkOutDate: String,
  rooms: Int,
  hotelName: String,
  hotelRating: Double,
  paymentMode: String,
  bankName: String,
  bookingPrice: Double,
  discount: Double,
  gst: Double,
  profitMargin: Double
)

class CsvReader[T](val filePath: String):
  private val reader = CSVReader.open(filePath)
  private val rows: List[Map[String, String]] = reader.allWithHeaders()

  private def parseIntoCaseClass(dataset: Map[String, String]): HotelDataset =
    val parsedDataset: HotelDataset = new HotelDataset(
      bookingID          = dataset("Booking ID"),
      dateOfBooking      = dataset("Date of Booking"),
      time               = dataset("Time"),
      customerID         = dataset("Customer ID"),
      gender             = dataset("Gender"),
      age                = dataset("Age").toInt,
      originCountry      = dataset("Origin Country"),
      state              = dataset("State"),
      location           = dataset("Location"),
      destinationCountry = dataset("Destination Country"),
      destinationCity    = dataset("Destination City"),
      numberOfPeople     = dataset("No. Of People").toInt,
      checkInDate        = dataset("Check-in date"),
      numberOfDays       = dataset("No of Days").toInt,
      checkOutDate       = dataset("Check-Out Date"),
      rooms              = dataset("Rooms").toInt,
      hotelName          = dataset("Hotel Name"),
      hotelRating        = dataset("Hotel Rating").toDouble,
      paymentMode        = dataset("Payment Mode"),
      bankName           = dataset("Bank Name"),
      bookingPrice       = dataset("Booking Price[SGD]").toDouble,
      discount           = dataset("Discount").stripSuffix("%").toDouble / 100,
      gst                = dataset("GST").toDouble,
      profitMargin       = dataset("Profit Margin").toDouble
    )
    parsedDataset
  end parseIntoCaseClass

  private val caseClassDataset: List[HotelDataset] = rows.map(parseIntoCaseClass)

  def recordData: List[HotelDataset] = caseClassDataset
end CsvReader

trait FilteringDatasets:
  val rows: List[HotelDataset]

  def filterColumn[T](filteredKey: HotelDataset => T): List[T] = rows.map(filteredKey)
end FilteringDatasets

abstract class HotelEDA(val rows: List[HotelDataset]) extends FilteringDatasets:
  def rankingDataset(): Map[String, Double]
  def printResult(): Unit = println(rankingDataset().maxBy(_._2))
end HotelEDA

class MaxBookCount(rows: List[HotelDataset]) extends HotelEDA(rows):
  private val filteredList: List[String] = filterColumn(_.destinationCountry)

  override def rankingDataset(): Map[String, Double] =
    val countryCount: Map[String, Double] = filteredList.groupBy(identity).view.mapValues(_.size.toDouble).toMap
    countryCount
end MaxBookCount

class MaxEconomic(rows: List[HotelDataset]) extends HotelEDA(rows):
  private val filteredList: List[(String, Double, Double, Double)] = filterColumn(row => (row.hotelName, row.bookingPrice, row.discount, row.profitMargin))
  private val sortedList: List[(String, Double, Double, Double)] = filteredList.sortBy(_._2).sortBy(_._1)

  private val minPrice: Double = sortedList.map(_._2).min
  private val maxPrice: Double = sortedList.map(_._2).max
  private val minDiscount: Double = sortedList.map(_._3).min
  private val maxDiscount: Double = sortedList.map(_._3).max

  private val minMargin: Double = sortedList.map(_._4).min
  private val maxMargin: Double = sortedList.map(_._4).max

  private val normalized: List[(String, Double, Double, Double)] = sortedList.map { case (name, v1, v2, v3) => (
    name,
    1 - ((v1 - minPrice) / (maxPrice - minPrice)),
    (v2 - minDiscount) / (maxDiscount - minDiscount),
    1 - (v3 - minMargin) / (maxMargin - minMargin)
  )}

  override def rankingDataset(): Map[String, Double] =
    val ranking: Map[String, Double] = normalized.groupBy(_._1).map { case (name, tuples) =>
      val totalScore = tuples.map { case (_, v1, v2, v3) => v1 + v2 + v3 }.sum
      name -> totalScore
    }
    ranking
end MaxEconomic

class MaxProfit(rows: List[HotelDataset]) extends HotelEDA(rows):
  private val filteredList: List[(String, Double, Double)] = filterColumn(row => (row.hotelName, row.numberOfPeople, row.profitMargin))
  private val sortedList: List[(String, Double, Double)] = filteredList.sortBy(_._3).sortBy(_._2).sortBy(_._1)

  private val minPeople: Double = sortedList.map(_._2).min
  private val maxPeople: Double = sortedList.map(_._2).max

  private val minMargin: Double = sortedList.map(_._3).min
  private val maxMargin: Double = sortedList.map(_._3).max

  private val normalized: List[(String, Double, Double)] = sortedList.map { case (name, v1, v2) => (
    name,
    (v1 - minPeople) / (maxPeople - minPeople),
    (v2 - minMargin) / (maxMargin - minMargin)
  )}

  override def rankingDataset(): Map[String, Double] =
    val ranking: Map[String, Double] = normalized.groupBy(_._1).map { case (name, tuples) =>
      val totalScore = tuples.map { case (_, v1, v2) => v1 + v2 }.sum
      name -> totalScore
    }
    ranking
end MaxProfit

object Main extends App:
  val dataset = new CsvReader("src/main/resources/Hotel_Dataset.csv").recordData

  val analytics: List[HotelEDA] = List(
    new MaxBookCount(dataset),
    new MaxEconomic(dataset),
    new MaxProfit(dataset)
  )

  analytics.foreach(_.printResult())
end Main