name := "play2-graylog2"

scalaVersion := "2.12.8"

organization := "ru.tochkak"

version := "1.3.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:deprecation")

libraryDependencies ++= Seq(
  guice,
  "org.graylog2" % "gelfclient" % "1.4.4"
)

publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := {
  _ => false
}

pomExtra := {
  <url>https://github.com/tochkak/play-graylog2</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:tochkak/play-graylog2.git</url>
      <connection>scm:git:git@github.com:tochkak/play-graylog2.git</connection>
    </scm>
    <developers>
      <developer>
        <id>panshin</id>
        <name>Gleb Panshin</name>
        <url>http://panshin.pro</url>
      </developer>
    </developers>
}
