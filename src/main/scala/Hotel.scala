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

abstract class HotelEDA(val rows: List[HotelDataset], val questionTitle: String)
  extends FilteringDatasets:

  def rankingDataset(): Map[String, Double]

  def bestAnswer: (String, Double) = rankingDataset().maxBy(_._2)

  def printResult(): Unit =
    val (label, value) = bestAnswer
    println("========================================")
    println(questionTitle)
    println("----------------------------------------")
    println(s"Answer : $label")
    println(f"Score  : $value%.4f")
    println()
end HotelEDA


class MaxBookCount(rows: List[HotelDataset])
  extends HotelEDA(rows, "Q1: Which country has the highest number of bookings in the dataset?"):

  private val filteredList: List[String] =
    filterColumn(_.destinationCountry)

  override def rankingDataset(): Map[String, Double] =
    val countryCount = filteredList
      .groupBy(identity)
      .view
      .mapValues(_.size.toDouble) // score = number of bookings
      .toMap
    countryCount

  override def printResult(): Unit =
    val (country, countDouble) = bestAnswer
    val count = countDouble.toInt
    println("========================================")
    println("Q1: Which country has the highest number of bookings in the dataset?")
    println("----------------------------------------")
    println(s"Country with the highest bookings : $country")
    println(s"Total number of bookings          : $count")
    println()
end MaxBookCount

class MaxEconomic(rows: List[HotelDataset])
  extends HotelEDA(rows,
    "Q2: Which hotel offers the most economical option (Booking Price, Discount, Profit Margin)?"):

  private val filteredList: List[(String, Double, Double, Double)] =
    filterColumn(row => (row.hotelName, row.bookingPrice, row.discount, row.profitMargin))

  private val sortedList =
    filteredList.sortBy(_._2).sortBy(_._1)

  private val minPrice    = sortedList.map(_._2).min
  private val maxPrice    = sortedList.map(_._2).max
  private val minDiscount = sortedList.map(_._3).min
  private val maxDiscount = sortedList.map(_._3).max
  private val minMargin   = sortedList.map(_._4).min
  private val maxMargin   = sortedList.map(_._4).max

  // Higher score = more economical (low price, high discount, reasonable margin)
  private val normalized: List[(String, Double, Double, Double)] =
    sortedList.map { case (name, price, disc, margin) =>
      (
        name,
        1 - ((price - minPrice) / (maxPrice - minPrice)),           // lower price → higher score
        (disc - minDiscount) / (maxDiscount - minDiscount),         // higher discount → higher score
        1 - ((margin - minMargin) / (maxMargin - minMargin))        // lower margin → higher score
      )
    }

  override def rankingDataset(): Map[String, Double] =
    normalized
      .groupBy(_._1)
      .map { case (name, tuples) =>
        val totalScore = tuples.map { case (_, v1, v2, v3) => v1 + v2 + v3 }.sum
        name -> totalScore
      }

  override def printResult(): Unit =
    val (hotel, score) = bestAnswer
    println("========================================")
    println("Q2: Which hotel offers the most economical option?")
    println("----------------------------------------")
    println(s"Most economical hotel : $hotel")
    println(f"Combined score        : $score%.4f")
    println("(Higher score = lower price, higher discount, and more customer-friendly profit margin.)")
    println()
end MaxEconomic


class MaxProfit(rows: List[HotelDataset])
  extends HotelEDA(rows,
    "Q3: Which hotel is the most profitable considering number of visitors and profit margin?"):

  private val filteredList: List[(String, Double, Double)] =
    filterColumn(row => (row.hotelName, row.numberOfPeople.toDouble, row.profitMargin))

  private val sortedList =
    filteredList.sortBy(_._3).sortBy(_._2).sortBy(_._1)

  private val minPeople = sortedList.map(_._2).min
  private val maxPeople = sortedList.map(_._2).max
  private val minMargin = sortedList.map(_._3).min
  private val maxMargin = sortedList.map(_._3).max

  private val normalized: List[(String, Double, Double)] =
    sortedList.map { case (name, people, margin) =>
      (
        name,
        (people - minPeople) / (maxPeople - minPeople),   // more visitors → higher score
        (margin - minMargin) / (maxMargin - minMargin)    // higher margin → higher score
      )
    }

  override def rankingDataset(): Map[String, Double] =
    normalized
      .groupBy(_._1)
      .map { case (name, tuples) =>
        val totalScore = tuples.map { case (_, v1, v2) => v1 + v2 }.sum
        name -> totalScore
      }

  override def printResult(): Unit =
    val (hotel, score) = bestAnswer
    println("========================================")
    println("Q3: Which hotel is the most profitable (visitors × profit margin)?")
    println("----------------------------------------")
    println(s"Most profitable hotel : $hotel")
    println(f"Profitability score   : $score%.4f")
    println("(Score combines number of visitors and profit margin.)")
    println()
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
