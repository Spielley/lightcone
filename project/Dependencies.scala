import sbt._

object Dependencies {
  val slf4jVersion = "1.7.25"
  val logbackVersion = "1.2.3"
  val akkaVersion = "2.5.18"
  val json4sVersion = "3.6.2"
  val scalapbVersion = scalapb.compiler.Version.scalapbVersion

  lazy val testDependency = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test)

  lazy val commonDependency = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "tv.cntt" %% "slf4s-api" % "1.7.25",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    "com.github.scopt" %% "scopt" % "3.7.0",
    "com.github.nscala-time" %% "nscala-time" % "2.20.0")

  lazy val guiceDependency = Seq(
    "com.google.inject" % "guice" % "4.2.2",
    "com.google.inject.extensions" % "guice-assistedinject" % "4.2.2",
    "net.codingwell" %% "scala-guice" % "4.2.1")

  lazy val json4sDependency = Seq(
    "org.json4s" %% "json4s-native" % json4sVersion,
    "org.json4s" %% "json4s-jackson" % json4sVersion,
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.7",
    "org.jsoup" % "jsoup" % "1.11.3")

  lazy val ethereumDependency = Seq(
    "org.web3j" % "core" % "3.4.0",
    "org.ethereum" % "ethereumj-core" % "1.8.2-RELEASE")

  lazy val akkaDependency = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion)

  lazy val httpDependency = Seq(
    "com.typesafe.akka" %% "akka-http" % "10.1.5",
    "de.heikoseeberger" %% "akka-http-json4s" % "1.22.0")

  lazy val driverDependency = Seq(
    "com.github.etaty" %% "rediscala" % "1.8.0",
    "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "0.20",
    "mysql" % "mysql-connector-java" % "5.1.47")

  lazy val scalapbDependency = Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf")

  lazy val dependency4Core = commonDependency ++
    ethereumDependency ++
    testDependency

  lazy val dependency4Ethereum = commonDependency ++
    ethereumDependency ++
    driverDependency ++
    guiceDependency ++
    testDependency

  lazy val dependency4Persistence = commonDependency ++
    driverDependency ++
    guiceDependency ++
    httpDependency ++
    json4sDependency ++
    testDependency

  lazy val dependency4Actors = dependency4Persistence ++
    httpDependency ++
    akkaDependency ++
    json4sDependency ++
    testDependency ++
    Seq("org.jsoup" % "jsoup" % "1.11.3")

  lazy val dependency4Gateway = dependency4Persistence ++
    httpDependency ++
    akkaDependency ++
    json4sDependency ++
    testDependency ++
    Seq("com.corundumstudio.socketio" % "netty-socketio" % "1.7.16")
}