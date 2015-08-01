package profanitypowerindex.util {
    import ProfanityPowerIndexUtils.processTweet
    import twitter4j.StatusListener
    import twitter4j.StatusDeletionNotice
    import twitter4j.StallWarning
    import twitter4j.Status
    
    class ProfanityPowerIndexListener(targets: Map[String, String])
    extends StatusListener {
        
        def onStatus(status: Status) {
            
            processTweet(status, targets).foreach(println)
            
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