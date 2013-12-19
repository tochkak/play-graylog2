name := "scala-logging"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.graylog2" % "play2-graylog2_2.10" % "1.0"
)     

play.Project.playScalaSettings
