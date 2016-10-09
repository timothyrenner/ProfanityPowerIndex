package profanitypowerindex.influx {
    import profanitypowerindex.util.ProfanityPowerIndexUtils.processTweet
    
    import twitter4j.StatusListener
    import twitter4j.StatusDeletionNotice
    import twitter4j.StallWarning
    import twitter4j.Status
    
    import com.paulgoldbaum.influxdbclient.InfluxDB
    import com.paulgoldbaum.influxdbclient.Point
    import com.paulgoldbaum.influxdbclient.Parameter.Precision
    
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.util.Random
    import scala.math.abs

    import org.joda.time.DateTime

    /** A Twitter4j listener that writes events to InfluxDB.
     *
     * @author Timothy Renner
     */
    class ProfanityPowerIndexListenerInflux(targets: Map[String, String],
        influxdb: InfluxDB)
    extends StatusListener {

        val database = influxdb.selectDatabase("profanity")
        val rng = new Random()

        /** Convert to microseconds and add a random subsecond-level noise 
         *  factor.
         *
         * @param The time in milliseconds.
         * @return The time in microseconds with subsecond-level noise added.
         */
        def jitter(time: Long):Long = 
            (time * 1000) + (abs(rng.nextLong) % 1000000L)
         
        def onStatus(status: Status) {
            
            val profanityPoints = 
                processTweet(status, targets, false).map {
                    case (id, rtid, time, t, p) =>
                        // We jitter the time to prevent duplicates.
                        // Twitter timeline produces data at the second-level
                        // only, which results in overwrites into InfluxDB
                        // unless the tweet ID is indexed. To avoid that, I'm
                        // jittering the time at the subsecond level by a
                        // random factor and writing at the microsecond level.
                        // Microsecond-level jitters are probably a bit much,
                        // but it does work.
                        Point("profanity", jitter(time.getMillis))
                            .addTag("word", p)
                            .addTag("subject", t)
                            .addField("id", id)
                            .addField("retweet_id", rtid) 
                    }

            database.bulkWrite(profanityPoints,
                               precision=Precision.MICROSECONDS)

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
        } // Close onException.
    } // Close ProfanityPowerIndexListenerInflux.
} // Close profanitypowerindex.influx
