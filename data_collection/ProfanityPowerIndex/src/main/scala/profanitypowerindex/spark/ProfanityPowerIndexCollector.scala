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
    
    /** Extracts tweets from Twitter's public timeline using Spark Streaming and
     *  associates profanity with the targets. Output is tab-separated as 
     *  follows:
     * 
     *  <Tweet ID>
     *  <ReTweet ID> (Same as Tweet ID if not a retweet).
     *  <time> The time of the tweet.
     *  <subject> The target.
     *  <word> The profanity associated with the target.
     *  
     *  The output is dropped into a text file based on the parameter set up
     *  in the JSON configuration file provided to main.
     * 
     * @author Timothy Renner
     */
    object ProfanityPowerIndexCollector {
        
        /** Gets the filtered stream based on the provided configuration file.
         * 
         * @param args: First arg - JSON configuration file - needs to be on the
         *                  classpath (so /src/main/resources).
         *              Second arg - Twitter consumer key.
         *              Third arg - Twitter consumer secret key.
         *              Fourth arg - Twitter access key.
         *              Fifth arg - Twitter access secret key.
         */
        def main(args: Array[String]) {
            
            // Validate that all of the args are present.
            if(args.length != 5) {
                System.err.println("Usage: ProfanityPowerIndexCollector " ++
                    "<config.json> <consumer key> <consumer secret> " ++
                    "<access key> <access secret>")
                System.exit(1)
            }
            
            val (twitterConsumerKey,
                 twitterConsumerSecret,
                 twitterAccessKey,
                 twitterAccessSecret) = (args(1), args(2), args(3), args(4))
            
            // Configure the system with the credentials.
            System.setProperty(
                "twitter4j.oauth.consumerKey",
                twitterConsumerKey)
            System.setProperty(
                "twitter4j.oauth.consumerSecret",
                twitterConsumerSecret)
            System.setProperty(
                "twitter4j.oauth.accessToken",
                twitterAccessKey)
            System.setProperty(
                "twitter4j.oauth.accessTokenSecret",
                twitterAccessSecret)
                                    
            // Extract the configuration from the JSON file. Pretend not to 
            // know how much easier this is in Clojure and Python.
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
            val accum = ssc.sparkContext.accumulator(0, "tweet-counter")
            
            val stream = TwitterUtils.createStream(ssc, None, tracking)
            
            // Process the stream.
            stream.flatMap(x => {
                    accum += 1 // Counts the total tweets.
                    processTweet(x, targets)
                }).saveAsTextFiles(filePrefix)
            
            ssc.start()
            
            if(time > 0L) {
                Thread.sleep(time * 1000)
                ssc.stop(true, true)
            } else {
                ssc.awaitTermination()
            }// Close if/else statement on stopping the context.
            
        } // Close main.
    } // Close ProfanityPowerIndexCollector.
} // Close package.