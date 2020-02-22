organization in ThisBuild := "com.codingkapoor"
version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.8"

val pac4jVersion = "3.6.1"

val mysql = "mysql" % "mysql-connector-java" % "8.0.17"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % "6.0"
val courier = "com.github.daddykotex" %% "courier" % "2.0.0"
val lagomPac4j = "org.pac4j" %% "lagom-pac4j" % "2.0.0"
val pac4jHttp = "org.pac4j" % "pac4j-http" % pac4jVersion
val pac4jJwt = "org.pac4j" % "pac4j-jwt" % pac4jVersion
val expoServerSdk = "com.kinoroy.expo.push" % "expo-push-sdk" % "0.1.3"

lazy val `intimations` = (project in file("."))
  .aggregate(`employee-api`, `employee-impl`, `holiday-api`, `holiday-impl`, `audit`, `passwordless-api`, `passwordless-impl`)

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
      scalaTest,
      pac4jHttp,
      pac4jJwt,
      lagomPac4j
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
      scalaTest,
      pac4jHttp,
      pac4jJwt,
      lagomPac4j
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`holiday-api`, `employee-api`)

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

lazy val `passwordless-api` = (project in file("passwordless-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `passwordless-impl` = (project in file("passwordless-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      nimbusJoseJwt,
      lagomScaladslTestKit,
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaClient,
      macwire,
      scalaTest,
      mysql
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`passwordless-api`, `employee-api`, `common`)

lazy val `notifier-api` = (project in file("notifier-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )
  .dependsOn(`employee-api`)

lazy val `notifier-impl` = (project in file("notifier-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      lagomScaladslPersistenceJdbc,
      lagomScaladslKafkaClient,
      macwire,
      scalaTest,
      mysql,
      expoServerSdk,
      pac4jHttp,
      pac4jJwt,
      lagomPac4j
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`notifier-api`, `employee-api`, `common`)

lazy val `common` = (project in file("common"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      macwire,
      courier,
    )
  )
  .settings(lagomForkedTestSettings)

lagomServiceGatewayAddress in ThisBuild := "0.0.0.0"

lagomCassandraCleanOnStart in ThisBuild := true
