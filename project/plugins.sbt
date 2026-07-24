addSbtPlugin("org.openapitools" % "sbt-openapi-generator" % "7.24.0")

// Packaging / Docker
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.7")
// Fat JAR
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
// Formatting & lint
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")

// Coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
// CI release (optional)
// Note: 1.11.0+ uses Central Portal instead of Legacy OSSRH — update CI secrets accordingly
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.7.0")
