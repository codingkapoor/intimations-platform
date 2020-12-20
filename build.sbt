organization in ThisBuild := "com.iamsmkr"

scalaVersion in ThisBuild := "2.12.8"

val pac4jVersion = "3.6.1"

val mysql = "mysql" % "mysql-connector-java" % "8.0.17"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % "6.0"
val courier = "com.github.daddykotex" %% "courier" % "2.0.0"
val play = "com.typesafe.play" %% "play" % "2.7.9"
val lagomPac4j = "org.pac4j" %% "lagom-pac4j" % "2.0.0"
val pac4jHttp = "org.pac4j" % "pac4j-http" % pac4jVersion
val pac4jJwt = "org.pac4j" % "pac4j-jwt" % pac4jVersion
val expoServerSdk = "com.kinoroy.expo.push" % "expo-push-sdk" % "0.1.3"

val akkaDiscoveryServiceLocator = "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % "1.0.0"
val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.0"

def dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := "adoptopenjdk/openjdk8",
  dockerUsername := sys.props.get("docker.username"),
  dockerRepository := sys.props.get("docker.registry")
)

version in ThisBuild ~= (_.replace('+', '-'))
dynver in ThisBuild ~= (_.replace('+', '-'))

lazy val `intimations` = (project in file("."))
  .aggregate(`employee-api`, `employee-impl`, `holiday-api`, `holiday-impl`, `audit`, `passwordless-api`, `passwordless-impl`, `notifier-api`, `notifier-impl`)

lazy val `common` = (project in file("common"))
  .settings(
    libraryDependencies ++= Seq(
      macwire,
      courier,
      play
    )
  )

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
      lagomPac4j,
      akkaDiscoveryServiceLocator,
      akkaDiscoveryKubernetesApi
    )
  )
  .settings(lagomForkedTestSettings)
  .settings(dockerSettings)
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
      lagomPac4j,
      akkaDiscoveryServiceLocator,
      akkaDiscoveryKubernetesApi
    )
  )
  .settings(lagomForkedTestSettings)
  .settings(dockerSettings)
  .dependsOn(`holiday-api`, `employee-api`)

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
      mysql,
      akkaDiscoveryServiceLocator,
      akkaDiscoveryKubernetesApi
    )
  )
  .settings(lagomForkedTestSettings)
  .settings(dockerSettings)
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
      lagomPac4j,
      akkaDiscoveryServiceLocator,
      akkaDiscoveryKubernetesApi
    )
  )
  .settings(lagomForkedTestSettings)
  .settings(dockerSettings)
  .dependsOn(`notifier-api`, `employee-api`, `common`)

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

lagomCassandraCleanOnStart in ThisBuild := true
