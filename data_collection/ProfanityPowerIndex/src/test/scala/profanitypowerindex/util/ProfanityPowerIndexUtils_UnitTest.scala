package profanitypowerindex.util {
   
   import org.scalatest.FunSuite
   import org.scalatest.PrivateMethodTester
   import java.util.Date
   
   /** Unit tests the ProfanityPowerIndexUtils class.
    * 
    * @author Timothy Renner
    */
   class ProfanityPowerIndexUtils_UnitTest extends FunSuite
         with PrivateMethodTester {
       
        val matchAss = 
            PrivateMethod[Boolean]('matchAss)
            
        test("matchAss matches \"ass\"") {
            assert((ProfanityPowerIndexUtils 
                    invokePrivate
                    matchAss("ass")) == true)
        } // Close test on "ass".
        
        test("matchAss matches \"asshole\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchAss("asshole")) == true)
        } // Close test on "asshole".
        
        test("matchAss matches \"asshat\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchAss("asshat")) == true)
        } // Close test on "asshat".
        
        test("matchAss matches \"jackass\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchAss("jackass")) == true)
        } // Close test on "jackass".
        
        test("matchAss matches \"fuckass\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchAss("fuckass")) == true)
        } // Close test on "fuckass".
       
       /** A helper for calling the `matchProfanity` method. */
        val matchProfanity = 
            PrivateMethod[Option[String]]('matchProfanity)
        
        test("matchProfanity matches \"fuck\"") {
            assert((ProfanityPowerIndexUtils 
                    invokePrivate 
                    matchProfanity("motherfucker")) == 
                        Some("fuck"))
        } // Close test on "fuck".
        
        test("matchProfanity matches \"shit\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchProfanity("shithead")) ==
                        Some("shit"))
        } // Close test on "shit."
        
        test("matchProfanity matches \"damn\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchProfanity("damned")) ==
                        Some("damn"))
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchProfanity("goddammit")) ==
                        Some("damn"))
        } // Close test on "damn."
        
        test("matchProfanity matches \"ass\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchProfanity("asshole")) ==
                        Some("ass"))
        } // Close test on "ass."
        
        test("matchProfanity matches \"dick\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchProfanity("dickhead")) ==
                        Some("dick"))
        } // Close test on "dick."
        
        test("matchProfanity matches \"douche\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchProfanity("douchebag")) ==
                        Some("douche"))
        } // Close test on "douche."
        
        test("matchProfanity matches \"bitch\"") {
            assert((ProfanityPowerIndexUtils
                    invokePrivate
                    matchProfanity("bitching")) ==
                        Some("bitch"))
        } // Close test on "bitch."
       
       /** A helper for testing the extractProfanity private method. */
       val extractProfanity = PrivateMethod[List[String]]('extractProfanity)
               
       test("extractProfanity returns the correct value") {
           assert(
               (ProfanityPowerIndexUtils
               invokePrivate
               extractProfanity(
                   List("jeb","bush","what","a","goddam","douche."))) 
               equals
               List("damn","douche"))
       } // Close test on extractProfanity.
       
       /** A helper for testing the extractTargets private method. */
       val extractTargets = PrivateMethod[List[String]]('extractTargets)
       
       test("extractTargets returns the correct value") {
           assert(
               (ProfanityPowerIndexUtils
               invokePrivate
               extractTargets(
                   List("rubio","and","clinton","will","win"),
                   Map("rubio" -> "Marco Rubio", 
                        "clinton" -> "Hillary Clinton")) equals
               List("Marco Rubio", "Hillary Clinton")))
       } // Close test on extractTargets.
       
       test("parseTweetText returns the correct value") {
           assert(
               ProfanityPowerIndexUtils.parseTweetText(
                   "Jeb Bush and Bernie Sanders are goddam fucking idiots.",
                   Map("bush" -> "Jeb Bush", "sanders" -> "Bernie Sanders"))
               equals
               List(("Jeb Bush", "damn"),
                    ("Jeb Bush", "fuck"),
                    ("Bernie Sanders", "damn"),
                    ("Bernie Sanders", "fuck")))
       } // Close test on parseTweetText.
       
       test("processTweetTime returns the correct value.") {
           assert(
               ProfanityPowerIndexUtils.processTweetTime(
                   new Date(1437661282123L)) ==
                new Date(1437661260000L))
       } // Close test on processTweetTime.
           
   } // Close ProfanityPowerIndexUtils_UnitTest.
} // Close package.