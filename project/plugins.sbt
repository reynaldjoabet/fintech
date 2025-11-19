addSbtPlugin("org.openapitools" % "sbt-openapi-generator" % "7.16.0")

// Packaging / Docker
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.1")
// Fat JAR
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.2.0")
// Hot reload
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")
// Formatting & lint
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.1")
// Coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.12")
// CI release (optional)
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

// Dependency hygiene
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
