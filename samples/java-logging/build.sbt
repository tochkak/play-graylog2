name := "java-logging"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.graylog2" % "play2-graylog2_2.10" % "1.0"
)     

play.Project.playJavaSettings
