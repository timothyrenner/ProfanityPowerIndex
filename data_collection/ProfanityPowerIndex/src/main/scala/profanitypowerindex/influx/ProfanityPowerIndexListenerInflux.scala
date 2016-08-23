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

    import org.joda.time.DateTime

    class ProfanityPowerIndexListenerInflux(targets: Map[String, String])
    extends StatusListener {

        val influxdb = InfluxDB.connect("localhost", 8086)
        val database = influxdb.selectDatabase("profanity")

        def onStatus(status: Status) {
            
            val profanityPoints = 
                processTweet(status, targets).map {
                    case (id, rtid, time, t, p) =>
                        // DateTime String constructor reads ISO8601 by
                        // default.
                        Point("profanity", new DateTime(time).getMillis)
                            .addTag("word", p)
                            .addTag("subject", t)
                            .addField("id", id)
                            .addField("retweet_id", rtid) 
                    }

            database.bulkWrite(profanityPoints,
                               precision=Precision.MILLISECONDS)

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
