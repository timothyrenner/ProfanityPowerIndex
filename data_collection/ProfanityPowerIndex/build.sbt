name := "profanity-power-index"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "4.0.4"

libraryDependencies += "org.apache.spark" % "spark-streaming_2.10" % "1.4.1"

libraryDependencies += "org.apache.spark" % "spark-streaming-twitter_2.10" % "1.4.1"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "3.0.0-M7" % "test"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.11"