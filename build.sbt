name := "akka-http-max-connection-test-stuff"

version := "0.0.1"

scalaVersion := Version.scala

libraryDependencies ++= List(
  Library.akkaActor,
  Library.akkaHttp,
  Library.akkaStream,
  Library.akkaSse,
  Library.akkaTestkit % "test",
  Library.scalaTest % "test"
)


enablePlugins(JavaAppPackaging)
