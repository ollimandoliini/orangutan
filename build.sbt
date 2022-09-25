val scala3Version = "3.1.3"
val Http4sVersion = "0.23.14"
val CirceVersion = "0.14.1"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-server" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.11"
)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps"
)

enablePlugins(
  JavaAppPackaging,
  DockerPlugin
)


lazy val root = project
  .in(file("."))
  .settings(
    name := "orangutan-game",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )


