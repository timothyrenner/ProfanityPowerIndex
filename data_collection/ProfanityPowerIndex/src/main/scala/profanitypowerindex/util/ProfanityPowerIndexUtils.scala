package profanitypowerindex.util {
    
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
            
        /** Matches the word "ass" against several variants.
         * 
         * @param word The word to match.
         * 
         * @return true if any match is found, false otherwise.
         */
        private def matchAss(word: String) = {
            (word == "ass") ||
            (word == "asshole") ||
            (word == "asshat") ||
            (word == "jackass")
        } // Close matchAss.
        
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
                case w if matchAss(w) => Some("ass")
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
        
        /** Processes the tweet to obtain the profanity associated with
         *  the targets, the tweet time and the original tweet ID (if it's a 
         *  retweet).
         *
         * @param tweet The tweet to process.
         * @param targets A map from the keyword name (for obtaining the 
         *  target from the tweet) to the full name.
         * @param truncateTime Truncate the tweet time to the nearest minute.
         *  This is handy for SQLite post processing, but not necessary for
         *  InfluxDB ingestion.
         *
         * @return A 5-tuple containing the tweet ID, retweet ID (if retweet,
         *  otherwise the tweet ID again), the time as an ISO 8601 formatted
         *  string (truncated to the minute if selected), the full name of the
         *  target, and the profanity.
         */
        def processTweet(tweet: Status, targets: Map[String, String],
                         truncateTime: Boolean = true) =
        {
            val time = dateFormat.format(
                        if(truncateTime) 
                            processTweetTime(tweet.getCreatedAt)
                        else tweet.getCreatedAt).toString
            val id = tweet.getId.toString
            val rtid = if(tweet.isRetweet) 
                        tweet.getRetweetedStatus.getId.toString
                       else id.toString
            
            for((t,p) <- parseTweetText(tweet.getText, targets)) 
                yield (id, rtid, time, t, p)
        } // Close processTweet.
    } // Close ProfanityPowerIndexUtils.
} // Close package.
