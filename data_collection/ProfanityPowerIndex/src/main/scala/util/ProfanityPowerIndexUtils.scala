package util {
    
    import java.util.Date
    import java.util.TimeZone
    import java.text.SimpleDateFormat
    import twitter4j.Status
    
    /** A helper class that parses the tweet text factored for reuse in local
     * and distributed streaming modes.
     * 
     * @author Timothy Renner
     */
    object ProfanityPowerIndexUtils {
        
        /** Create a formatter for Eastern time zone in ISO8601 format. */
        val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")
        val timeZone = TimeZone.getTimeZone("America/New_York")
        dateFormat.setTimeZone(timeZone)
            
        /** Matches the profanity, returning the root profanity, or None.
         * 
         * @param word The word to match.
         * 
         * @return The root profanity, or None if there's no match.
         */
        private def matchProfanity(word: String)  = 
            word match {
                case w if w.contains("fuck") => Some("fuck")
                case w if w.contains("shit") => Some("shit")
                case w if (w.contains("damn") || w.contains("goddam")) =>
                    Some("damn")
                case w if w.contains("ass") => Some("ass")
                case w if w.contains("dick") => Some("dick")
                case w if w.contains("douche") => Some("douche")
                case w if w.contains("bitch") => Some("bitch")
                case _ => None
            } // Close match on word.
        
        /** Extracts the profanity from the split text.
         * 
         * @param text: The text as a list.
         * 
         * @return A list containing the profanity roots.
         */
        private def extractProfanity(text: List[String])  = 
            text.flatMap(matchProfanity)
        
        /** Extracts the targets from the split text.
         * 
         * @param text: The text as a list.
         * @param targets: A map from the keyword name to the full name, i.e.
         * "trump" -> "Donald Trump".
         * 
         * @return A list containing the full names of the targets in the 
         * text.
         */
        private def extractTargets(text: List[String], 
            targets: Map[String,String]) = 
                text.filter(t => targets.contains(t)).map(t => targets(t))
        
        /** Parses the tweet text for the profanity and targets.
         * 
         * @param tweetText: The text for the tweet.
         * @param targets: A map from the keyword name to the full name, i.e.
         * "sanders" -> "Bernie Sanders". 
         * 
         * @return A list containing all (target, profanity) pairs in the tweet.
         */
        def parseTweetText(tweetText: String, targets: Map[String,String]) = 
        {
            val text = tweetText.toLowerCase.split(" ").toList
            
            for { t <- extractTargets(text, targets)
                  p <- extractProfanity(text) } yield (t,p)
        } // Close parseTweetText.
        
        /** Rounds the tweet time to the nearest minute.
         * 
         * @param tweetTime: The time of the tweet.
         * 
         * @return The time rounded to the nearest minute.
         */
        def processTweetTime(tweetTime: Date): Date = {
            tweetTime.setTime((tweetTime.getTime/60000)*60000) 
            return tweetTime
        } // Close processTweetTime.
        
        def processTweet(tweet: Status, targets:Map[String, String]) =
        {
            val time = dateFormat.format(processTweetTime(tweet.getCreatedAt))
            val id = tweet.getId
            val rt = tweet.isRetweet
            val rtid = if(rt) { tweet.getRetweetedStatus.getId } else id
            
            for((t,p) <- parseTweetText(tweet.getText, targets)) 
                yield List(id, rt, rtid, time.toString, t, p).mkString("\t")
        } // Close processTweet.
    } // Close ProfanityPowerIndexUtils.
} // Close package.