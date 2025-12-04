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

class CsvReader(val filePath: String):
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

trait Normalization:
  def normalize(min: Double, max: Double, value: Double): Double = (value - min) / (max - min)
end Normalization



abstract class HotelEDA(val rows: List[HotelDataset]) extends FilteringDatasets:
  def rankingDataset(): Map[_, Double]
  def printResult(): Unit = println(rankingDataset().maxBy(_._2))
end HotelEDA




class MaxBookCount(rows: List[HotelDataset]) extends HotelEDA(rows):
  private val filteredList: List[String] = filterColumn(_.destinationCountry)

  override def rankingDataset(): Map[String, Double] =
    val countryCount: Map[String, Double] = filteredList.groupBy(identity).view.mapValues(_.size.toDouble).toMap
    countryCount
end MaxBookCount

class MaxEconomic(rows: List[HotelDataset]) extends HotelEDA(rows) with Normalization:
  private val filteredList: List[(String, String, String, Double, Double, Double)] = filterColumn(row => (row.hotelName, row.destinationCountry, row.destinationCity, row.bookingPrice, row.discount, row.profitMargin))

  private val groupedAndAggregatedList: Map[(String, String, String), (Double, Double, Double)] = filteredList.groupBy { case (hotel, country, city, _, _, _) =>
    (hotel, country, city)
  }.view.mapValues { row =>
    val avgPrice = row.map(_._4).sum / row.size
    val avgDiscount = row.map(_._5).sum / row.size
    val avgProfit = row.map(_._6).sum / row.size
    (avgPrice, avgDiscount, avgProfit)
  }.toMap

  private val minPrice: Double = groupedAndAggregatedList.values.map(_._1).min
  private val maxPrice: Double = groupedAndAggregatedList.values.map(_._1).max

  private val minDiscount: Double = groupedAndAggregatedList.values.map(_._2).min
  private val maxDiscount: Double = groupedAndAggregatedList.values.map(_._2).max

  private val minMargin: Double = groupedAndAggregatedList.values.map(_._3).min
  private val maxMargin: Double = groupedAndAggregatedList.values.map(_._3).max

  private val normalizedList: Map[(String, String, String), (Double, Double, Double)] =
    groupedAndAggregatedList.view.mapValues { case (bookingPrice, discount, profitMargin) =>
      val normalizedPrice = 1 - normalize(minPrice, maxPrice, bookingPrice)
      val normalizedDiscount = normalize(minDiscount, maxDiscount, discount)
      val normalizedMargin = 1 - normalize(minMargin, maxMargin, profitMargin)
      (normalizedPrice, normalizedDiscount, normalizedMargin)
    }.toMap

  override def rankingDataset(): Map[(String, String, String), Double] =
    val ranking: Map[(String, String, String), Double] =
      normalizedList.view.mapValues { case (bookingPrice, discount, profitMargin) =>
        val score = (bookingPrice + discount + profitMargin) / 3
        score
      }.toMap
    ranking
end MaxEconomic

class MaxProfit(rows: List[HotelDataset]) extends HotelEDA(rows) with Normalization:
  private val filteredList: List[(String, String, String, Double, Double)] = filterColumn(row =>
    (row.hotelName, row.destinationCountry, row.destinationCity, row.numberOfPeople, row.profitMargin))

  private val groupedAndAggregatedList: Map[(String, String, String), (Double, Double)] = filteredList.groupBy { case (hotel, country, city, _, _) =>
    (hotel, country, city)
  }.view.mapValues { row =>
    val totalPeople = row.map(_._4).sum
    val avgProfit = row.map(_._5).sum / row.size
    (totalPeople, avgProfit)
  }.toMap

  private val minPeople: Double = groupedAndAggregatedList.values.map(_._1).min
  private val maxPeople: Double = groupedAndAggregatedList.values.map(_._1).max

  private val minMargin: Double = groupedAndAggregatedList.values.map(_._2).min
  private val maxMargin: Double = groupedAndAggregatedList.values.map(_._2).max

  private val normalizedList: Map[(String, String, String), (Double, Double)] =
    groupedAndAggregatedList.view.mapValues { case (numOfPeople, profitMargin) =>
      val normalizedPeople = normalize(minPeople, maxPeople, numOfPeople)
      val normalizedMargin = normalize(minMargin, maxMargin, profitMargin)
      (normalizedPeople, normalizedMargin)
    }.toMap

  override def rankingDataset(): Map[(String, String, String), Double] =
    val ranking: Map[(String, String, String), Double] =
      normalizedList.view.mapValues { case (numOfPeople, profitMargin) =>
        val score = (numOfPeople + profitMargin) / 2
        score
      }.toMap
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