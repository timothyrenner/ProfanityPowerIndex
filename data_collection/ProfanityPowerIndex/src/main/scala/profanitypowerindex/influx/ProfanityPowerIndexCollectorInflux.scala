package profanitypowerindex.influx {
    
    import twitter4j.TwitterStreamFactory
    import twitter4j.TwitterStream
    import twitter4j.FilterQuery

    import com.paulgoldbaum.influxdbclient.InfluxDB
    
    import scala.io.Source

    import scala.concurrent.ExecutionContext.Implicits.global

    import org.json4s._
    import org.json4s.native.JsonMethods._

    /** Extracts tweets from Twitter's public timeline using a local Twitter4j
     *  listener and associates profanity with the targets. Output is written
     *  to the "profanity" measure in the "profanity" database.
     *
     * @author Timothy Renner
     */
    object ProfanityPowerIndexCollectorInflux {

        /** Grabs the filtered stream based on the provided configuration file.
         *
         * @param args: First arg - JSON configuration file - needs to be on the
         *                  classpath (so /src/main/resources).
         *              Second arg - Twitter consumer key.
         *              Third arg - Twitter consumer secret key.
         *              Fourth arg - Twitter access key.
         *              Fifth arg - Twitter access secret key.
         */
        def main(args: Array[String]) = {

            if(args.length != 5) {
                System.err.println(
                    "Usage: ProfanityPowerIndexCollectorInflux " ++
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

            // Annoying Java pattern.
            val twitterStream = new TwitterStreamFactory().getInstance

            // Grab the arg'd file.
            val config = parse(
                Source.fromURL(getClass.getResource("/"++args(0))).mkString)

            // Because we can't just make JSON parsing
            // (-> file slurp parse-string) can we?
            implicit val formats = DefaultFormats

            // Get the tracking keywords and target map.
            val tracking = (config \ "tracking").extract[List[String]]
            val targets = (config \ "targets").extract[Map[String, String]]
            val time = (config \ "time").extractOrElse(0L)

            // Connect to the database.
            val influxdb = InfluxDB.connect("localhost", 8086)

            // Create a listener instance.
            val listener = 
                new ProfanityPowerIndexListenerInflux(targets, influxdb)

            // Add our profanity listener.
            twitterStream.addListener(listener)
            // Initiate the stream with the tracking filter.
            twitterStream.filter(new FilterQuery().track(tracking.toArray))

            if(time > 0L) {
                Thread.sleep(time * 1000)

                // Close the stream.
                twitterStream.cleanUp
                twitterStream.shutdown

                // Close the connection to influxDB.
                influxdb.close
            }
        } // Close main.
    } // Close ProfanityPowerIndexCollectorInflux.
} // Close profanitypowerindex.influx.
