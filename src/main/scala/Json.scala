import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object Json {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .master("local[*]")
      .appName("StructuredKafkaJSON")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

//     Define schema of json
    val schema = StructType(Array(StructField("nombre", StringType, false),
      StructField("edad", IntegerType, false),
      StructField("peso", FloatType, false),
      StructField("direccion", StringType, false)))

//     Create DataSet representing the stream of input lines from kafka
//     Es necesario de antemano haber creado el topic llamado json_topic
    val lines = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")
      .option("subscribe", "json_topic")
      .option("startingOffsets", "earliest")
      .load()
      .selectExpr("CAST(value AS STRING)")
      .select(from_json(col("value"), schema).as("parsed_value"))
      .select("parsed_value.*")

//     Una vez iniciado el procesamiento del flujo empezar a insertar elementos al topic de kafka
//     Ver resultados en la consola donde se ejecuta el objeto
    val query = lines.writeStream
      .outputMode("append")
      .format("console")
      .option("truncate","false")
      .start()

    query.awaitTermination()
  }
}
