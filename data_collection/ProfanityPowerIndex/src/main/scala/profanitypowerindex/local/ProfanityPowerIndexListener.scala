package profanitypowerindex.local {
    import profanitypowerindex.util.ProfanityPowerIndexUtils.processTweet
    
    import twitter4j.StatusListener
    import twitter4j.StatusDeletionNotice
    import twitter4j.StallWarning
    import twitter4j.Status

    import org.joda.time.format.ISODateTimeFormat
    
    /** A Twitter4j listener that writes the events to STDOUT and total
     *  tweets processed to STDERR.
     *
     * @author Timothy Renner
     */
    class ProfanityPowerIndexListener(targets: Map[String, String])
    extends StatusListener {
        
        var tweetCounter = 0
        
        val dateFormat = ISODateTimeFormat.dateTime()

        def onStatus(status: Status) {
            
            processTweet(status, targets).map {
                case (id, rtid, time, t, p) =>
                    (id, rtid, dateFormat.print(time), t, p)
            }.foreach {
                t => println(t.productIterator.toList.mkString("\t")) 
            }
        
            tweetCounter += 1
            
            if(tweetCounter % 100 == 0) {
                Console.err.println("Total number of tweets: %d."
                    .format(tweetCounter))
            }
            
        } // Close onStatus.
        
        def onDeletionNotice(notice: StatusDeletionNotice) { }
        
        def onTrackLimitationNotice(numLimitedStatus: Int) { }
        
        def onStallWarning(warning: StallWarning) { }
        
        def onScrubGeo(userId: Long, upToStatusId: Long) { }
        
        def onException(ex: Exception) { 
            // Print, unless a null pointer.
            ex match {
                case e: NullPointerException => {}
                case _ => ex.printStackTrace
            } // Close match.
        } // Close onException
    } // Close ProfanityPowerIndexListener.
} // Close util.
