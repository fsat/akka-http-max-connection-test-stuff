import sbt._
import sbt.Resolver.bintrayRepo

object Version {
  val akka                   = "2.3.11"
  val akkaHttp               = "2.0.1"
  val akkaStream             = "2.0.1"
  val akkaSse                = "1.4.1"
  val scala                  = "2.11.7"
  val scalaTest              = "2.2.4"
}

object Library {
  val akkaActor               = "com.typesafe.akka"      %% "akka-actor"                     % Version.akka
  val akkaHttp                = "com.typesafe.akka"      %% "akka-http-experimental"         % Version.akkaHttp
  val akkaStream              = "com.typesafe.akka"      %% "akka-stream-experimental"       % Version.akkaStream
  val akkaSse                 = "de.heikoseeberger"      %% "akka-sse"                       % Version.akkaSse
  val akkaTestkit             = "com.typesafe.akka"      %% "akka-testkit"                   % Version.akka
  val scalaTest               = "org.scalatest"          %% "scalatest"                      % Version.scalaTest
}

object Resolver {
}
