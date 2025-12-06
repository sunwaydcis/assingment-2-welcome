import com.github.tototoshi.csv.*
import java.net.URL
import scala.language.postfixOps

//case class can ensure the access to dataset even after modification
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

//CsvReader class to encapsulate Csv file reading
class CsvReader(val datasetURL: URL):
  //totoshi CSV don't support java.net.URL, thus need to parse it.
  //ISO-8859-1 is used to prevent java.nio.charset.MalformedInputException: Input length = 1 bc dataset contain special character
  private val reader = CSVReader.open(scala.io.Source.fromURL(datasetURL, "ISO-8859-1"))
  private val rows = reader.allWithHeaders()

  //parsing the dataset into case class
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


//a trait for filtering specific columns of the case class
trait FilteringDatasets:
  val rows: List[HotelDataset]

  //it takes a function that turns HotelDataset into any type
  def filterColumn[T](filteredKey: HotelDataset => T): List[T] = rows.map(filteredKey)
end FilteringDatasets

//a trait that grants subtype with normalizing logic
trait Normalization:
  //upperbound K, V to product to gain better access to tuples
  def normalize[K, V <: Product](data: Map[K, V]): Map[K, List[Double]] =
    val listData: Map[K, List[Double]] = data.map { case (k, v) =>
      //using productIterator to iterate through each tuple and cast them to a list
      //List provides a better indexing calculation
      k -> v.productIterator.map(_.asInstanceOf[Double]).toList
    }

    //transpose to turn the row into column, i.e. (price, margin), (price, margin) into (price, price), (margin, margin)
    val columns: List[List[Double]] = listData.values.toList.transpose

    val mins: List[Double] = columns.map(_.min)
    val maxs: List[Double] = columns.map(_.max)

    listData.map { case (k, values) =>
      //indexing the list to allow easier calculation
      val normalizedValues = values.zipWithIndex.map { case (v, i) =>
        (v - mins(i)) / (maxs(i) - mins(i))
      }
      k -> normalizedValues
    }
end Normalization


abstract class HotelEDA(val rows: List[HotelDataset]) extends FilteringDatasets:
  //create an output with universal key, as key can be in any type
  def rankingDataset(): Map[_, Double]
  def printResult(): Unit = println(rankingDataset().maxBy(_._2))
end HotelEDA



class MaxBookCount(rows: List[HotelDataset]) extends HotelEDA(rows):
  private val filteredList: List[String] = filterColumn(_.destinationCountry)

  override def rankingDataset(): Map[String, Double] =
    //directly group by country name and count them
    val countryCount: Map[String, Double] = filteredList.groupBy(identity).view.mapValues(_.size.toDouble).toMap
    countryCount

  override def printResult(): Unit =
    val ranking = rankingDataset()
    val (country, count) = ranking.maxBy(_._2)
    println("=== Country with Highest Number of Bookings ===")
    println(s"Country : $country")
    println(s"Bookings: ${count.toLong}")
    println()
end MaxBookCount


class MaxEconomic(rows: List[HotelDataset]) extends HotelEDA(rows) with Normalization:
  private val filteredList: List[(String, String, String, Double, Double, Double)] =
    filterColumn(row => (row.hotelName, row.destinationCountry, row.destinationCity, row.bookingPrice, row.profitMargin, row.discount))

  private val groupedAndAggregatedList: Map[(String, String, String), (Double, Double, Double)] = {
    filteredList.groupBy { case (hotel, country, city, _, _, _) =>
      (hotel, country, city)
    }.view.mapValues { row =>
      val avgPrice = row.map(_._4).sum / row.size
      val avgDiscount = row.map(_._5).sum / row.size
      val avgProfit = row.map(_._6).sum / row.size
      (avgPrice, avgDiscount, avgProfit)
    }.toMap
  }

  private val normalizedList: Map[(String, String, String), List[Double]] =
    normalize(groupedAndAggregatedList).map { case (k, values) =>
      val reversed = values.zipWithIndex.map { case (v, i) =>
        if (i < 2) 1.0 - v else v
      }
      k -> reversed
    }

  override def rankingDataset(): Map[(String, String, String), Double] =
    normalizedList.view.mapValues { values =>
      val sum = values.sum
      sum / values.length
    }.toMap

  override def printResult(): Unit =
    val ranking = rankingDataset()
    val (hotelKey, score) = ranking.maxBy(_._2)
    val (hotelName, country, city) = hotelKey
    val (avgPrice, avgDiscountRaw, avgProfitRaw) = groupedAndAggregatedList.getOrElse(hotelKey, (0.0, 0.0, 0.0))

    println("=== Most Economical Hotel ===")
    println(s"Hotel  : $hotelName")
    println(s"Country: $country")
    println(s"City   : $city")
    println()
    println(f"Average booking price : SGD ${avgPrice}%.2f")
    println(f"Average discount      : ${avgDiscountRaw * 100}%.2f%%")
    println(f"Average profit margin : ${avgProfitRaw}%.4f")
    println(f"Economy Score         : $score%.4f")
    println()
  end printResult

end MaxEconomic


class MaxProfit(rows: List[HotelDataset]) extends HotelEDA(rows) with Normalization:
  private val filteredList: List[(String, String, String, Double, Double)] = filterColumn(row =>
    (row.hotelName, row.destinationCountry, row.destinationCity, row.numberOfPeople, row.profitMargin))

  private val groupedAndAggregatedList: Map[(String, String, String), (Double, Double)] =
    filteredList.groupBy { case (hotel, country, city, _, _) =>
      (hotel, country, city)
    }.view.mapValues { row =>
      val totalPeople = row.map(_._4).sum
      val avgProfit = row.map(_._5).sum / row.size
      (totalPeople, avgProfit)
    }.toMap

  private val normalizedList: Map[(String, String, String), List[Double]] =
    normalize(groupedAndAggregatedList)

  override def rankingDataset(): Map[(String, String, String), Double] =
    normalizedList.view.mapValues { values =>
      val sum = values.sum
      sum / values.length
    }.toMap

  override def printResult(): Unit =
    val ranking = rankingDataset()
    val (hotelKey, score) = ranking.maxBy(_._2)
    val (hotelName, country, city) = hotelKey
    val (totalPeople, avgProfit) = groupedAndAggregatedList(hotelKey)

    println("=== Most Profitable Hotel ===")
    println(s"Hotel  : $hotelName")
    println(s"Country: $country")
    println(s"City   : $city")
    println()
    println(s"Total Visitors      : ${totalPeople.toLong}")
    println(f"Average Profit Margin: $avgProfit%.4f")
    println(f"Profitability Score  : $score%.4f")
    println()
end MaxProfit


object Main extends App:
  val datasetURL: URL = getClass.getResource("/Hotel_Dataset.csv")
  val dataset = new CsvReader(datasetURL).recordData

  val analytics: List[HotelEDA] = List(
    new MaxBookCount(dataset),
    new MaxEconomic(dataset),
    new MaxProfit(dataset)
  )

  analytics.foreach(_.printResult())
end Main