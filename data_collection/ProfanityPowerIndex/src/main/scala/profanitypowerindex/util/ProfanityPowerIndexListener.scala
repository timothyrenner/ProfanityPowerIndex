package profanitypowerindex.util {
    import ProfanityPowerIndexUtils.processTweet
    import twitter4j.StatusListener
    import twitter4j.StatusDeletionNotice
    import twitter4j.StallWarning
    import twitter4j.Status
    
    class ProfanityPowerIndexListener(targets: Map[String, String])
    extends StatusListener {
        
        var tweetCounter = 0
        
        def onStatus(status: Status) {
            
            processTweet(status, targets).foreach {
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