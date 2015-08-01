
package profanitypowerindex.local {
    import twitter4j.TwitterStreamFactory
    import twitter4j.TwitterStream
    import twitter4j.FilterQuery
    import scala.io.Source
    import org.json4s._
    import org.json4s.native.JsonMethods._
    
    import profanitypowerindex.util.ProfanityPowerIndexListener
   
    object ProfanityPowerIndexCollectorLocal {
        
        /** Grabs the filtered stream specified by the json file provided as
         *  the first argument. That file needs to be in /src/main/resources .
         */
        def main(args: Array[String]) = {
            
            // Annoying Java pattern.
            val twitterStream = new TwitterStreamFactory().getInstance
            
            // Grab the arg'd file.
            val config = parse(
                Source.fromURL(getClass.getResource("/"++args(0))).mkString)
            
            // Because we can't just make JSON parsing 
            // (-> file slurp parse-string) can we?
            implicit val formats = DefaultFormats
            
            // Get the tracking keywords and the target map.
            val tracking = (config \ "tracking").extract[List[String]]
            val targets  = (config \ "targets").extract[Map[String, String]]
            val time = (config \ "time").extractOrElse(0L)
            
            // Add our profanity listener.
            twitterStream.addListener(new ProfanityPowerIndexListener(targets))
            // Initiate the stream with the tracking filter.
            twitterStream.filter(new FilterQuery().track(tracking.toArray))
            
            if(time > 0L) {
                Thread.sleep(time * 1000)
            
                // Close the stream.
                    twitterStream.cleanUp
                    twitterStream.shutdown
            }
        } // Close main.
    } // Close ProfanityPowerIndexCollectLocal.
} // Close package.