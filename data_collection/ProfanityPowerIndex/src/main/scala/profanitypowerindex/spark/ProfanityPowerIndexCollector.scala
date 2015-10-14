package profanitypowerindex.spark {
    
    import org.apache.spark.streaming.{StreamingContext, Seconds}
    import org.apache.spark.SparkContext._
    import org.apache.spark.streaming.twitter._
    import org.apache.spark.SparkConf
    import org.apache.spark.storage.StorageLevel
    
    import org.json4s._
    import org.json4s.native.JsonMethods._
    
    import scala.io.Source
    
    import com.datastax.spark.connector.SomeColumns
    import com.datastax.spark.connector.streaming._
    import com.datastax.spark.connector.cql.CassandraConnector
    
    import twitter4j.json.DataObjectFactory
    
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
        
        /** Sets up the cassandra database for tweet collection.
         * 
         * @param sparkConf The spark configuration. Needs 
         * spark.cassandra.connection.host and spark.connection.cassandra.port
         * set.
         * 
         */
        def cassandraInit(sparkConf: SparkConf) {
            
            CassandraConnector(sparkConf).withSessionDo { session =>
                // Create the keyspace.
                session.execute("CREATE KEYSPACE ppi WITH REPLICATION = " ++
                                "{'class': 'SimpleStrategy', " ++
                                "'replication_factor':2}")
                // Create the table for all tweets.
                session.execute("CREATE TABLE ppi.tweets (id TEXT, " ++
                                "time TIMESTAMP, tweet TEXT, PRIMARY KEY (id))")
                // Create table for extracted profanity.
                session.execute("CREATE TABLE ppi.profanity (id TEXT, " ++
                                "rt_id TEXT, time TEXT, word TEXT, " ++
                                "subject TEXT, PRIMARY KEY(id, time))")
            }
        } // Close cassandraInit.
        
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
            val cassandraHost = (config \ "cassandraHost").extract[String]
            val cassandraPort = (config \ "cassandraPort").extract[String]
            
            
            // Set up spark.
            val sparkConf = new SparkConf()
                                .setAppName("ProfanityPowerIndexCollector")
                                .set("spark.cassandra.connection.host",
                                     cassandraHost) 
                                .set("spark.cassandra.connection.port",
                                     cassandraPort)
            
            val ssc = new StreamingContext(sparkConf, Seconds(batchLength))
            
            // Initialize Cassandra.
            cassandraInit(sparkConf)
            
            val stream = TwitterUtils.createStream(ssc, None, tracking)
            
            // Process the stream.
            // Save the entire tweet to Cassandra.
            stream.map(x => (x.getId.toString, x.getCreatedAt ,x.getText))
                  .saveToCassandra("ppi", "tweets", 
                                   SomeColumns("id", "time", "tweet"))
            // Save the extracted info to Cassandra.
            stream.flatMap(x => processTweet(x, targets))
                  .saveToCassandra("ppi", "profanity", 
                                   SomeColumns("id", "rt_id", "time", 
                                               "word", "subject"))
            
            
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