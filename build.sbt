import Dependencies._

ThisBuild / scalaVersion := "3.3.6"

ThisBuild / version := "0.1.0-SNAPSHOT"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "fintech",
    libraryDependencies += munit % Test
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %% "core" % "4.0.11",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.38.2",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.38.2" % "provided"
    )
  ) //.dependsOn(codegenOpenBanking)          // ⬅️ THIS puts codegen classes on root's classpath
// .aggregate(codegenOpenBanking)

// lazy val codegenOpenBanking = (project in file("modules/codegen-openbanking"))
//   .enablePlugins(OpenApiGeneratorPlugin)
//   .settings(
//     name := "codegen-openbanking",

//     // Use the same JSON so CLI and SBT stay in sync
//     openApiConfigFile := ((ThisBuild / baseDirectory).value /
//       "modules" / "codegen-openbanking" / "openapi" / "config.json").getPath,

//     // Put generated sources where SBT expects managed sources
//     openApiOutputDir := ((Compile / sourceManaged).value / "openapi").getAbsolutePath,

//     // Flatten the source tree inside that folder (no src/main/scala nesting)
//     //openApiConfigOptions ++= Map("sourceFolder" -> ""),

//     // Fail fast on bad specs (optional but recommended)
//     openApiValidateSpec := Some(true),

//     Compile / sourceGenerators += openApiGenerate.taskValue,
//   )

// lazy val openbankingApp = project
//   .in(file("modules/openbanking-app"))
//   .settings(
//     Compile / sourceGenerators += (codegenOpenBanking / openApiGenerate).taskValue,
//     libraryDependencies ++= Seq(
//       "com.softwaremill.sttp.client4" %% "core" % "4.0.11",
//       "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.38.2",
//       "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.38.2" % "provided"
//     )
//   )

// =================== COMMON SETTINGS & DEPENDENCIES ===================

// ============ Global ============

//ThisBuild / organization               := "com.domain"
ThisBuild / resolvers ++= Seq(Resolver.mavenCentral)
ThisBuild / versionScheme := Some("early-semver")

// Align Java toolchain & container (set to 17 if you deploy on 17)
//ThisBuild / javacOptions := Seq("-release", "21")

ThisBuild / javacOptions := Seq("-source", "21", "-target", "21")
//It locks your Java source + bytecode compatibility to JDK 17.
//Ensures identical behaviour in CI, local dev, and Docker (17-based runtime).
lazy val isCi = sys.env.get("CI").contains("true")

lazy val V = new {
  val catsEffect = "3.5.4"
  val http4s = "0.23.26"
  val tapir = "1.10.6"
  val jsoniter = "2.38.2"
  val sttp4 = "4.0.11"
  val doobie = "1.0.0-RC5"
  val flyway = "10.18.2"
  val pureconfig = "0.17.6"
  val logback = "1.5.6"
  val slf4j = "2.0.16"
  val scalatest = "3.2.19"
  val scalacheck = "1.17.0"
  val testcontainers = "1.19.7"
  val tcScala = "0.41.4" // optional scala wrappers
  val wiremock = "2.35.2"
}

def deps(ms: ModuleID*) = libraryDependencies ++= ms

// ================= PRODUCTION DEFAULTS ==============
lazy val prodSettings = Seq(
  Compile / scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-Yretain-trees"
  ) ++ (if (isCi) Seq("-Xfatal-warnings") else Nil),
  Test / fork := true,
  Test / parallelExecution := false,
  Test / logBuffered := false,
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
  scalafmtOnCompile := true,

  // reproducible metadata
  Compile / packageOptions += Package.ManifestAttributes(
    "Implementation-Title" -> name.value,
    "Implementation-Version" -> version.value
  ),

  // speed CI; enable scaladoc in releases if needed
  Compile / doc / sources := Seq.empty
)

// =================== COVERAGE =======================
ThisBuild / coverageMinimumStmtTotal := 80
ThisBuild / coverageFailOnMinimum := isCi
ThisBuild / coverageHighlighting := true

// =================== ASSEMBLY =======================
lazy val assemblySettings = Seq(
  assembly / test := {},
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", xs @ _*) =>
      xs.map(_.toLowerCase) match {
        case ("manifest.mf" :: Nil)     => MergeStrategy.discard
        case ("index.list" :: Nil)      => MergeStrategy.discard
        case ("dependencies" :: Nil)    => MergeStrategy.discard
        case ("spring.schemas" :: Nil)  => MergeStrategy.concat
        case ("spring.handlers" :: Nil) => MergeStrategy.concat
        case _                          => MergeStrategy.first
      }
    case "module-info.class" => MergeStrategy.discard
    case _                   => MergeStrategy.first
  }
)

// ==================== DOCKER ========================
lazy val dockerSettings = Seq(
  Docker / packageName := s"domain/${name.value}",
  Docker / version := version.value,
  dockerBaseImage := "eclipse-temurin:21-jre",
  dockerExposedPorts := Seq(8080),
  Universal / javaOptions ++= Seq(
    "-J-XX:MaxRAMPercentage=75.0",
    "-J-XX:+UseG1GC",
    "-J-XX:MaxGCPauseMillis=200",
    "-J-Dlogback.configurationFile=/opt/docker/conf/logback.xml"
  )
  // Docker / labels := Map(
  //   "org.opencontainers.image.source"  -> "https://github.com/your-org/your-repo",
  //   "org.opencontainers.image.version" -> version.value
  // )
)

// ==================== TESTKIT =======================
lazy val testkit = project
  .in(file("modules/testkit"))
  .settings(prodSettings, name := "testkit", publish / skip := true)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % V.scalatest,
      "org.scalacheck" %% "scalacheck" % V.scalacheck,
      "org.testcontainers" % "testcontainers" % V.testcontainers,
      "org.testcontainers" % "postgresql" % V.testcontainers,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % V.tcScala, // optional, handy
      "com.dimafeng" %% "testcontainers-scala-postgresql" % V.tcScala, // optional, handy
      "com.github.tomakehurst" % "wiremock-jre8" % V.wiremock,
      "ch.qos.logback" % "logback-classic" % V.logback % Test
    )
  )

// ==================== MODULES =======================

// common: plumbing (config/logging/concurrency utils)
lazy val common = project
  .in(file("modules/common"))
  .settings(prodSettings, name := "common")
  .dependsOn(testkit % "test->test")
  .settings(
    deps(
      "org.typelevel" %% "cats-effect" % V.catsEffect,
      "com.github.pureconfig" %% "pureconfig-core" % V.pureconfig,
      "org.slf4j" % "slf4j-api" % V.slf4j,
      "ch.qos.logback" % "logback-classic" % V.logback % Runtime,
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// protocols: DTOs + jsoniter codecs
lazy val protocols = project
  .in(file("modules/protocols"))
  .settings(prodSettings, name := "protocols")
  .dependsOn(common, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % V.jsoniter,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % V.jsoniter,
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// core: DB access shared services (if you centralize repositories)
lazy val core = project
  .in(file("modules/core"))
  .settings(prodSettings, name := "core")
  .dependsOn(common, protocols, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core" % V.doobie,
      "org.tpolecat" %% "doobie-hikari" % V.doobie,
      "org.tpolecat" %% "doobie-postgres" % V.doobie,
      "org.flywaydb" % "flyway-core" % V.flyway,
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// customers: identity & KYC state
lazy val customers = project
  .in(file("modules/customers"))
  .settings(prodSettings, name := "customers")
  .dependsOn(common, protocols, core, testkit % "test->test")

// accounts: product engine, lifecycle, derived balances (reads ledger)
lazy val accounts = project
  .in(file("modules/accounts"))
  .settings(prodSettings, name := "accounts")
  .dependsOn(common, protocols, core, customers, testkit % "test->test")

// ledger: immutable postings (isolated from business modules)
lazy val ledger = project
  .in(file("modules/ledger"))
  .settings(prodSettings, name := "ledger")
  .dependsOn(common, protocols, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core" % V.doobie,
      "org.tpolecat" %% "doobie-postgres" % V.doobie,
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// payments: orchestration; talks to rails; posts to ledger
lazy val payments = project
  .in(file("modules/payments"))
  .settings(prodSettings, name := "payments")
  .dependsOn(common, protocols, ledger, accounts, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %% "core" % V.sttp4,
      "com.softwaremill.sttp.client4" %% "cats" % V.sttp4,
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// compliance: screening/TM; reads customers (and optionally postings metadata)
lazy val compliance = project
  .in(file("modules/compliance"))
  .settings(prodSettings, name := "compliance")
  .dependsOn(common, protocols, customers, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// loans / credit domain
lazy val loans = project
  .in(file("modules/loans"))
  .settings(prodSettings, name := "loans")
  .dependsOn(common, protocols, core, accounts, ledger, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core" % V.doobie,
      "org.tpolecat" %% "doobie-postgres" % V.doobie,
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// webhooks: endpoint mgmt & delivery signing
lazy val webhooks = project
  .in(file("modules/webhooks"))
  .settings(prodSettings, name := "webhooks")
  .dependsOn(common, protocols, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// api: http4s + Tapir (jsoniter); wires everything; builds Docker & fat-jar
lazy val api = project
  .in(file("modules/api"))
  .enablePlugins(JavaAppPackaging)
  .settings(prodSettings, assemblySettings, dockerSettings, name := "api")
  .dependsOn(
    common,
    protocols,
    core,
    customers,
    accounts,
    ledger,
    payments,
    compliance,
    loans,
    webhooks,
    testkit % "test->test"
  )
  .settings(
    Compile / mainClass := Some("com.domain.api.Main"),
    deps(
      "org.typelevel" %% "cats-effect" % V.catsEffect,
      "org.http4s" %% "http4s-ember-server" % V.http4s,
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "com.softwaremill.sttp.tapir" %% "tapir-core" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % V.tapir,
      "org.scalatest" %% "scalatest" % V.scalatest % Test
    )
  )

// root aggregator (no publish)
// lazy val root = project.in(file("."))
//   .aggregate(
//     common, protocols, core, customers, accounts, ledger,
//     payments, compliance, loans, webhooks, api, testkit
//   )
//   .settings(name := "bank", publish / skip := true)
