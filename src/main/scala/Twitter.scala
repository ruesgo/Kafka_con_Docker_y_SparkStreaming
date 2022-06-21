import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.streaming.Trigger

object Twitter {
  def main(args: Array[String]): Unit = {
//    establecemos la duración de las ventanas y deslizamientos
    val windowSize = "60"
    val slideSize = "10"

    val windowDuration = windowSize + " seconds"
    val slideDuration = slideSize + " seconds"
    val monitoring_dir = "twitter-data/source_csv_data/"

    val spark = SparkSession
      .builder
      .appName("InteractionCount")
      .config("spark.eventLog.enabled","true")
      .config("spark.eventLog.dir","applicationHistory")
      .master("local[*]")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    val userSchema = StructType(Array(StructField("userA", StringType),
      StructField("userB", StringType),
      StructField("timestamp", TimestampType),
      StructField("interaction", StringType)))

    val twitterIDSchema  = StructType(Array(StructField("userA", StringType)))

    val twitterIDs = spark.read.schema(twitterIDSchema).csv("twitter-data/twitterIDs.csv")
    val csvDF = spark
      .readStream
      .schema(userSchema)
      .csv(monitoring_dir)

    val joinedDF = csvDF.join(twitterIDs,"userA")

    val interactions = joinedDF.select("userA","interaction","timestamp")

    val windowedCounts = interactions
      .groupBy(
        window(
          interactions("timestamp"),
          windowDuration,
          slideDuration),
        interactions("userA"))
      .count()
    val query = windowedCounts
      .writeStream
      .outputMode("update")
      .format("console")
      .option("truncate","false")
      .option("numRows","10000")
      .trigger(Trigger.ProcessingTime("15 seconds"))
      .start()

    query.awaitTermination()
  }
}
