name := "play2-graylog2"

scalaVersion := "2.11.8"

organization := "ru.tochkak"

version := "1.1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies ++= Seq(
  "org.graylog2" % "gelfclient" % "1.4.1"
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
  <url>https://github.com/Panshin/play2-graylog2</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:Panshin/play2-graylog2.git</url>
      <connection>scm:git:git@github.com:Panshin/play2-graylog2.git</connection>
    </scm>
    <developers>
      <developer>
        <id>panshin</id>
        <name>Gleb Panshin</name>
        <url>http://panshin.pro</url>
      </developer>
    </developers>
}
