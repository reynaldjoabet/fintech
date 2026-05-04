import Dependencies.*
import com.typesafe.sbt.packager.docker.*
import com.typesafe.sbt.packager.docker.DockerChmodType

import java.nio.file.Path
import scala.sys.process.*
import scala.util.Try
ThisBuild / scalaVersion := "3.3.7"

ThisBuild / version := "0.1.0-SNAPSHOT"

// define task to get version from git tags

lazy val gitTagVersion = taskKey[String]("Get version from git tags")

gitTagVersion := {
  val tag = "git describe --tags --abbrev=0".!!.trim
  tag
}

lazy val hash = "git rev-parse HEAD".!!.trim

lazy val tag = "git describe --tags --exact-match"

lazy val version2 = "git describe --tags".!!.stripLineEnd.stripPrefix("v")

lazy val version3 =
  "git describe --tags --dirty --always".!!.stripPrefix("v").trim

lazy val description = Try("git describe --tags --match v*".!!.trim).toOption

lazy val buildId = Def.task {
  val log = streams.value.log
  def runCommand(cmd: String): Option[String] = {
    import scala.sys.process._
    val sb = new StringBuilder
    val code = cmd ! ProcessLogger(sb append _)
    val text = sb.toString()
    if (code == 0) {
      Some(text)
    } else {
      log.warn(s"Can`t launch `$cmd` to determine buildId")
      log.warn(s"  code=$code text=$text")
      None
    }
  }
  runCommand("git describe --tags") orElse runCommand(
    "git log -n1 --pretty=%h"
  ) getOrElse "unknown"
}

def currentVersion = ("git describe --tags --match v*" !!).trim.substring(1)

lazy val latestGitTag: String =
  Try("git describe --tags --abbrev=0".!!.trim)
    .map(_.stripPrefix("v"))
    .getOrElse("latest")

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = project
  .in(file("."))
  .settings(
    name := "fintech",
    libraryDependencies ++= Seq(
      munit % Test,
      bouncycastle,
      password4j,
      nimbusdsJoseJwt,
      nimbusdsOauth2OidcSdk,
      catsEffect
    )
  )
  .settings(
    libraryDependencies ++= Seq(
      sttpCore,
      jsoniter,
      jsoniterMacros
    )
  )
  .aggregate(
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
    api,
    testkit
  )
  .dependsOn(
    `finicity-codegen` % "compile->compile"
    // `clickbank-codegen` % "compile->compile"
  )
  .settings(publish / skip := true)

val generate = taskKey[Unit]("generate code from APIs")
val cleanGenerated = taskKey[Unit]("delete previously generated api/model code")

// =================== COMMON SETTINGS & DEPENDENCIES ===================

// ============ Global ============

//ThisBuild / organization               := "com.domain"
ThisBuild / resolvers ++= Seq(Resolver.mavenCentral)
ThisBuild / versionScheme := Some("early-semver")

// Align Java toolchain & container (set to 17 if you deploy on 17)
//ThisBuild / javacOptions := Seq("-release", "21")

ThisBuild / javacOptions := Seq("-source", "17", "-target", "17")
//It locks your Java source + bytecode compatibility to JDK 17.
//Ensures identical behaviour in CI, local dev, and Docker (17-based runtime).
lazy val isCi = false //sys.enVersion.get("CI").contains("true")

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
// entrypoint.sh is automatically copied into /opt/docker by sbt-native-packager.
//ExecCmd("RUN", "chmod", "u+x", "/opt/docker/entrypoint.sh")
// Docker / labels := Map(
//   "org.opencontainers.image.source"  -> "https://github.com/your-org/your-repo",
//   "org.opencontainers.image.version" -> version.value
// )

// ==================== DOCKER ========================
lazy val dockerSettings = Seq(
  Docker / packageName := s"domain/${name.value}",
  Docker / version := version.value,
  Docker / daemonUserUid := None,
  Docker / daemonUser := "root",
  dockerExposedVolumes := Seq("/data"),
  dockerUpdateLatest := true,
  dockerChmodType := DockerChmodType.UserGroupWriteExecute,
  dockerBaseImage := "eclipse-temurin:21-jre",
  // dockerBaseImage := "amazoncorretto:17"
  // dockerBaseImage := "openj"
  Universal / mappings += file(
    "entrypoint.sh"
  ) -> "entrypoint.sh", // When creating the Universal package, include the file entrypoint.sh from my project root, and place it in the root of the packaged archive
  dockerEntrypoint := Seq("/opt/docker/entrypoint.sh"),
  dockerExposedPorts := Seq(8080),
  // dockerRepository := Some("docker.io"),
  Universal / javaOptions ++= Seq(
    "-J-XX:MaxRAMPercentage=75.0",
    "-J-XX:+UseG1GC",
    "-J-XX:MaxGCPauseMillis=200",
    "-J-Dlogback.configurationFile=/opt/docker/conf/logback.xml"
  )
)

// ==================== TESTKIT =======================
lazy val testkit = project
  .in(file("modules/testkit"))
  .settings(prodSettings, name := "testkit", publish / skip := true)
  .settings(
    libraryDependencies ++= Seq(
      scalaTest % Test,
      logback % Test,
      slf4j % Test
    )
  )

// ==================== MODULES =======================

// common: plumbing (config/logging/concurrency utils)
lazy val common = project
  .in(file("modules/common"))
  .settings(prodSettings, name := "common")
  .dependsOn(testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      catsEffect,
      pureconfig,
      slf4j,
      logback % Runtime,
      scalaTest % Test
    ),
    javacOptions ++= Seq(
      "-Xlint",
      "-J-Xss256M",
      "-encoding",
      "UTF-8",
      "-XDignore.symbol.file"
    ),
    javaOptions ++= Seq(
      "-Djdk.internal.httpclient.debug=false",
      "-Djdk.httpclient.HttpClient.log=errors"
    )
  )

// protocols: DTOs + jsoniter codecs
lazy val protocols = project
  .in(file("modules/protocols"))
  .settings(prodSettings, name := "protocols")
  .dependsOn(common, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      sttpJsoniter,
      jsoniter,
      jsoniterMacros,
      scalaTest % Test
    )
  )

// core: DB access shared services (if you centralize repositories)
lazy val core = project
  .in(file("modules/core"))
  .settings(prodSettings, name := "core")
  .dependsOn(common, protocols, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      catsEffect,
      postgres,
      skunkCore,
      flyway,
      scalaTest % Test
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
      catsEffect,
      postgres,
      skunkCore,
      flyway,
      scalaTest % Test
    )
  )

// payments: orchestration; talks to rails; posts to ledger
lazy val payments = project
  .in(file("modules/payments"))
  .settings(prodSettings, name := "payments")
  .dependsOn(common, protocols, ledger, accounts, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      sttpCore,
      sttpCats,
      scalaTest % Test
    )
  )

// compliance: screening/TM; reads customers (and optionally postings metadata)
lazy val compliance = project
  .in(file("modules/compliance"))
  .settings(prodSettings, name := "compliance")
  .dependsOn(common, protocols, customers, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      scalaTest % Test
    )
  )

// loans / credit domain
lazy val loans = project
  .in(file("modules/loans"))
  .settings(prodSettings, name := "loans")
  .dependsOn(common, protocols, core, accounts, ledger, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      catsEffect,
      postgres,
      skunkCore,
      flyway,
      scalaTest % Test
    )
  )

// webhooks: endpoint mgmt & delivery signing
lazy val webhooks = project
  .in(file("modules/webhooks"))
  .settings(prodSettings, name := "webhooks")
  .dependsOn(common, protocols, testkit % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      scalaTest % Test
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
    libraryDependencies ++= Seq(
      catsEffect,
      `http4s-dsl`,
      emberServer,
      emberClient,
      tapirCore,
      tapirHttp4sServer,
      tapirJsoniterScala,
      tapirOpenAPIDocs,
      tapirSwaggerUIBundle,
      scalaTest % Test
    )
  )

// Define a custom task
lazy val parTestGroup = inputKey[Unit]("Runs a single test group")
parTestGroup := (Def.inputTaskDyn {
  // Takes two parameters: groupId (which group to run) and numberOfGroups (total groups)
  val List(groupId, numberOfGroups) = complete.DefaultParsers
    .spaceDelimited("<arg>")
    .parsed
    .map(_.toInt)

  // Retrieves all available tests
  val allTests = (Test / definedTests).value

  // Calculates how many tests should be in each group
  val numberOfTests = allTests.size
  val numberOfTestsPerGroup =
    if (numberOfTests % numberOfGroups == 0) {
      numberOfTests / numberOfGroups
    } else { (numberOfTests / numberOfGroups) + 1 }

  // Divides tests into groups
  val groups = allTests.grouped(numberOfTestsPerGroup).toArray

  val groupToRun = groups(groupId - 1)
  val argForTestOnly = " " + groupToRun.map(_.name).mkString(" ")

  streams.value.log.info(s"Running testOnly:$argForTestOnly")

  // Runs only the specified group using SBT's testOnly task
  Def.taskDyn {
    (Test / testOnly).toTask(argForTestOnly)
  }
}).evaluated

lazy val `finicity-codegen` = project
  .in(file("modules/finicity-codegen"))
  .enablePlugins(OpenApiGeneratorPlugin)
  .settings(
    name := "finicity-codegen",
    // openApiInputSpec := "src/main/resources/swagger.json",
    // openApiGeneratorName := "sclala-sttp-client4",
    openApiModelNamePrefix := "",
    openApiModelNameSuffix := "",
    openApiGenerateMetadata := Some(false),
    openApiGenerateMetadata := SettingDisabled,
    // Use the same JSON so CLI and SBT stay in sync
    openApiConfigFile := ((Compile / baseDirectory).value / "config.json").getPath,
    openApiIgnoreFileOverride := s"${baseDirectory.value.getPath}/openapi-ignore-file",

    // Put generated sources where SBT expects managed sources
    openApiOutputDir := ((Compile / baseDirectory).value / "src/main/scala").getAbsolutePath,
    openApiGenerateModelTests := SettingDisabled,
    openApiGenerateApiTests := SettingDisabled,
    openApiValidateSpec := SettingDisabled,
    // Fail fast on bad specs (optional but recommended)
    // openApiValidateSpec := Some(true),
    // Compile / sourceGenerators += openApiGenerate.taskValue,
    (Compile / compile) := ((Compile / compile) dependsOn generate).value,
    // (Compile/compile) := ((compile in Compile) dependsOn openApiGenerate).value

    // Define the simple generate command to generate full client codes
    cleanGenerated := {
      val outputDir = file(openApiOutputDir.value)
      val dirsToClean =
        Seq(outputDir / "finicity" / "api", outputDir / "finicity" / "models")
      dirsToClean.foreach(dir => IO.delete(dir))
    },
    generate := Def
      .sequential(
        cleanGenerated,
        Def.task {
          val _ = openApiGenerate.value
          // Delete the generated build.sbt file so that it is not used for our sbt config
          val buildSbtFile = file(openApiOutputDir.value) / "build.sbt"
          if (buildSbtFile.exists()) {
            buildSbtFile.delete()
          }
        }
      )
      .value,
    libraryDependencies ++= Seq(
      sttpJsoniter,
      jsoniter,
      jsoniterMacros,
      jsoniterCirce
    )
  )

lazy val `clickbank-codegen` = project
  .in(file("modules/clickbank-codegen"))
  .enablePlugins(OpenApiGeneratorPlugin)
  .settings(
    name := "clickbank-codegen",
    // openApiInputSpec := "src/main/resources/swagger.json",
    // openApiGeneratorName := "sclala-sttp-client4",
    openApiModelNamePrefix := "",
    openApiModelNameSuffix := "",
    openApiGenerateMetadata := Some(false),
    openApiRemoveOperationIdPrefix := Some(true),
    openApiGenerateMetadata := SettingDisabled,
    // Use the same JSON so CLI and SBT stay in sync
    openApiConfigFile := ((Compile / baseDirectory).value / "config.json").getPath,
    openApiIgnoreFileOverride := s"${baseDirectory.value.getPath}/openapi-ignore-file",

    // Put generated sources where SBT expects managed sources
    openApiOutputDir := ((Compile / baseDirectory).value / "src/main/scala").getAbsolutePath,
    openApiGenerateModelTests := SettingDisabled,
    openApiGenerateApiTests := SettingDisabled,
    openApiValidateSpec := SettingDisabled,
    // Fail fast on bad specs (optional but recommended)
    // openApiValidateSpec := Some(true),
    // Compile / sourceGenerators += openApiGenerate.taskValue,
    (Compile / compile) := ((Compile / compile) dependsOn generate).value,
    // (Compile/compile) := ((compile in Compile) dependsOn openApiGenerate).value

    // Define the simple generate command to generate full client codes
    cleanGenerated := {
      val outputDir = file(openApiOutputDir.value)
      val dirsToClean =
        Seq(outputDir / "clickbank" / "api", outputDir / "clickbank" / "models")
      dirsToClean.foreach(dir => IO.delete(dir))
    },
    generate := Def
      .sequential(
        cleanGenerated,
        Def.task {
          val _ = openApiGenerate.value
          // Delete the generated build.sbt file so that it is not used for our sbt config
          val buildSbtFile = file(openApiOutputDir.value) / "build.sbt"
          if (buildSbtFile.exists()) {
            buildSbtFile.delete()
          }
        }
      )
      .value,
    libraryDependencies ++= Seq(
      sttpJsoniter,
      jsoniter,
      jsoniterMacros,
      jsoniterCirce
    )
  )
