import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object DStream {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .master("local[*]")
      .appName("StructuredKafkaWordCount")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

//    Create DataSet representing the stream of input lines from kafka
//    Es necesario de antemano haber creado el topic llamado wordcount_topic

    val lines = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")//,0.0.0.0:19093,0.0.0.0:19094")
      .option("subscribe", "wordcount_topic")
      .option("startingOffsets", "earliest")
      .load()
      .selectExpr("CAST(value AS STRING)")

//    lines.printSchema()
//    val query1=lines.writeStream
//      .outputMode("append")
//      .format("console").start()

//    Split the lines into words
//    explode turns each item in an array into a separate row
    val words = lines.select(explode(
        split(lines("value"), " ")
      ).as("word")
    )

//    Generate running word count
    val wordCounts = words.groupBy("word").count()

//    Start running the query that prints the running counts to the console
//    Una vez iniciado el procesamiento del flujo empezar a insertar elementos al topic de kafka
//    Ver resultados en la consola donde se ejecuto el scala object
    val query = wordCounts
      .writeStream
      .outputMode("complete")
      .format("console").start()

//    query1.awaitTermination()
    query.awaitTermination()
  }
}
