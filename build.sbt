organization in ThisBuild := "com.codingkapoor"
version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.8"

val mysql = "mysql" % "mysql-connector-java" % "8.0.17"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lazy val `intimations` = (project in file("."))
  .aggregate(`employee-api`, `employee-impl`)

lazy val `employee-api` = (project in file("employee-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val `employee-impl` = (project in file("employee-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      mysql,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`employee-api`)

lazy val `holiday-api` = (project in file("holiday-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val `holiday-impl` = (project in file("holiday-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceJdbc,
      lagomScaladslTestKit,
      mysql,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`holiday-api`)

lazy val `audit` = (project in file("audit"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      lagomScaladslKafkaClient,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`employee-api`)

lagomServiceGatewayAddress in ThisBuild := "0.0.0.0"

//lagomCassandraCleanOnStart in ThisBuild := true
