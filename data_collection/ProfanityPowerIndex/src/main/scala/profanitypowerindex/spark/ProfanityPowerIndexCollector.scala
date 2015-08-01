package profanitypowerindex.spark {
    
    import org.apache.spark.streaming.{StreamingContext, Seconds}
    import org.apache.spark.SparkContext._
    import org.apache.spark.streaming.twitter._
    import org.apache.spark.SparkConf
    import org.apache.spark.storage.StorageLevel
    
    import org.json4s._
    import org.json4s.native.JsonMethods._
    
    import scala.io.Source
    
    import profanitypowerindex.util.ProfanityPowerIndexUtils._
    
    object ProfanityPowerIndexCollector {
        
        def main(args: Array[String]) {
            
            // Parse the configuration while pretending not to know how much
            // easier this is in Clojure and Python.
            val config = parse(
                Source.fromURL(getClass.getResource("/"++args(0))).mkString)
            
            implicit val formats = DefaultFormats
            
            val tracking = (config \ "tracking").extract[List[String]]
            val targets = (config \ "targets").extract[Map[String, String]]
            val time = (config \ "time").extractOrElse(0L)
            val batchLength = (config \ "batchLength").extractOrElse(1)
            val filePrefix = (config \ "filePrefix").extract[String]
            
            // Set up spark.
            val sparkConf = new SparkConf()
                                .setAppName("ProfanityPowerIndexCollector")
            val ssc = new StreamingContext(sparkConf, Seconds(batchLength))
            val stream = TwitterUtils.createStream(ssc, None, tracking)
            
            // Process the stream.
            stream.flatMap(x => processTweet(x, targets))
                  .saveAsTextFiles(filePrefix)
            
            ssc.start()
            // ssc.awaitTermination()
            
            if(time > 0L) {
                Thread.sleep(time * 1000)
                ssc.stop(true, true)
            } else {
                ssc.awaitTermination()
            }// Close if/else statement on stopping the context.
            
        } // Close main.
    } // Close ProfanityPowerIndexCollector.
} // Close package.