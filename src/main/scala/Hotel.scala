import scala.io.Source
import java.nio.charset.CodingErrorAction
import scala.io.Codec

object CsvReader {
  def main(args: Array[String]): Unit = {
    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    val src = Source.fromResource("Hotel_Dataset.csv")(codec)

    for (line <- src.getLines()) {
      println(line)
    }

    src.close()


  }
}