
package local {
    import twitter4j.TwitterStreamFactory
    import twitter4j.TwitterStream
    import scala.io.Source
    import org.json4s._
    import org.json4s.native.JsonMethods._
    
    import util.ProfanityPowerIndexListener
   
    object ProfanityPowerIndexCollectLocal {
        
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
            
            // Add our profanity listener.
            twitterStream.addListener(new ProfanityPowerIndexListener(targets))
            // Initiate the stream with the tracking filter.
            twitterStream.filter(tracking:_*)
            
            // FOR DEBUGGING ONLY.
            //Thread.sleep((3 * 60 + 30) * 60 * 1000)
            Thread.sleep(30 * 1000)
            
            // Close the stream.
            twitterStream.cleanUp
            twitterStream.shutdown
        } // Close main.
    } // Close ProfanityPowerIndexCollectLocal.
} // Close package.