lazy val `sample-akka-basket` = project
  .in(file("."))
  .aggregate(`akka-basket`, `akka-shopper`)

lazy val `akka-basket` = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
      Library.AkkaPersistence,
      Library.AkkaStream,
      Library.AkkaHttp,
      Library.SprayJson,
      Library.LevelDB,
      Library.LevelDBAll,
      Library.AkkaHttpTestKit,
      Library.ScalaTest
    )
  )

lazy val `akka-shopper` = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
      Library.AkkaStream,
      Library.AkkaHttp)
  )

lazy val commonSettings = Seq(
  organization := "de.cboerner",
  scalaVersion := "2.12.4"
)

lazy val Library = new {

  object Version {
    val Akka = "2.5.11"
    val AkkaHttp = "10.1.1"
    val ScalaTest = "3.0.5"
  }

  val akkaGroup = "com.typesafe.akka"

  val AkkaPersistence = akkaGroup %% "akka-persistence" % Version.Akka
  val AkkaStream = akkaGroup %% "akka-stream" % Version.Akka
  val AkkaHttp = akkaGroup %% "akka-http" % Version.AkkaHttp
  val SprayJson = akkaGroup %% "akka-http-spray-json" % "10.1.2"
  val LevelDB = "org.iq80.leveldb" % "leveldb" % "0.7"
  val LevelDBAll = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"

  // Test
  val AkkaTestKit = akkaGroup %% "akka-testkit" % Version.AkkaHttp % Test
  val AkkaHttpTestKit = akkaGroup %% "akka-http" % Version.AkkaHttp % Test
  val ScalaTest = "org.scalatest" %% "scalatest" % Version.ScalaTest % Test
}
