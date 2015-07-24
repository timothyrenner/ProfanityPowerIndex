package util {
    import ProfanityPowerIndexUtils._
    import twitter4j.StatusListener
    import twitter4j.StatusDeletionNotice
    import twitter4j.StallWarning
    import twitter4j.Status
    
    class ProfanityPowerIndexListener(targets: Map[String, String])
    extends StatusListener {
        
        def onStatus(status: Status) = {
            val time = processTweetTime(status.getCreatedAt)
            val id = status.getId
            val rt = status.isRetweet
            
            for((t,p) <- parseTweetText(status.getText, targets)) {
                println(List(id, rt, time.toString, t, p).mkString("\t"))
            } // Close for loop on parseTweetText.
        } // Close onStatus.
        
        def onDeletionNotice(notice: StatusDeletionNotice) { }
        
        def onTrackLimitationNotice(numLimitedStatus: Int) { }
        
        def onStallWarning(warning: StallWarning) { }
        
        def onScrubGeo(userId: Long, upToStatusId: Long) { }
        
        def onException(ex: Exception) { ex.printStackTrace }
    } // Close ProfanityPowerIndexListener.
} // Close util.