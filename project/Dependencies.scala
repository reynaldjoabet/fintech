import sbt.*

object Dependencies {

  private object Versions {

    // --- ZIO ecosystem ---
    val zio        = "2.1.26"
    val zioJson    = "0.9.2"
    val zioHttp    = "3.11.6"
    val zioLogging = "2.5.3"
    val zioConfig  = "4.0.7"
    val zioSchema  = "1.8.5"
    val zioKafka   = "3.8.0"

    // --- HTTP ---
    val http4s  = "0.23.37"
    val sttp4   = "4.0.27"
    val tapir   = "1.13.32"
    val jsonRpc = "0.2.0"

    // --- JSON ---
    val jsoniter = "2.41.2"
    val circe    = "0.14.16"

    // --- FP ---
    val catsEffect      = "3.7.1"
    val fs2             = "3.14.0"
    val fs2Kafka        = "4.1.3"
    val chimney         = "2.1.0"
    val iron            = "3.3.2"
    val hedgehog        = "0.13.1"
    val scalacheck      = "1.20.0"
    val munit           = "1.3.6"
    val munitCatsEffect = "2.2.1"

    // --- DB ---
    val quill    = "4.8.6"
    val magnum   = "2.0.0-M3"
    val skunk    = "2.0.0-RC3"
    val hikaricp = "7.1.0"
    val flyway   = "13.10.3"
    val postgres = "42.7.14"

    // --- Security ---
    val jwtScala         = "11.0.4"
    val bouncycastle     = "1.86"
    val password4j       = "1.8.4"
    val auth0            = "4.6.2"
    val nimbusJoseJwt    = "10.10"
    val nimbusOauth2Oidc = "11.38.2"
    val vault            = "5.1.0"

    // --- Logging ---
    val scribe  = "3.19.0"
    val slf4j   = "2.0.20"
    val logback = "1.6.5"

    // --- Cache ---
    val caffeine = "3.3.1"

    // --- Observability ---
    val datadog = "2.62.0"
    val kamon   = "2.8.1"
    val otel4s  = "1.0.1"

    // --- Config ---
    val pureconfig = "0.17.10"

    // --- Cloud ---
    val awsV2         = "2.47.5"
    val azureIdentity = "1.18.7"
    val azureKv       = "4.11.1"

  }

  // Helpers
  private def http4s(a: String) =
    "org.http4s" %% s"http4s-$a" % Versions.http4s

  private def tapirDep(a: String) =
    "com.softwaremill.sttp.tapir" %% s"tapir-$a" % Versions.tapir

  private def sttpDep(a: String) =
    "com.softwaremill.sttp.client4" %% a % Versions.sttp4

  private def circeDep(a: String) =
    "io.circe" %% s"circe-$a" % Versions.circe

  // Reusable across modules as-is
  val catsEffect = Seq("org.typelevel" %% "cats-effect" % Versions.catsEffect)
  val fs2        = Seq("co.fs2" %% "fs2-core" % Versions.fs2)
  val fs2Kafka   = Seq("org.typelevel" %% "fs2-kafka" % Versions.fs2Kafka)
  val chimney    = Seq("io.scalaland" %% "chimney" % Versions.chimney)
  val caffeine   = Seq("com.github.ben-manes.caffeine" % "caffeine" % Versions.caffeine)
  val jsonRpc    = Seq("tech.neander" %% "jsonrpclib-fs2" % Versions.jsonRpc)

  object zio {

    val core = Seq(
      "dev.zio" %% "zio"          % Versions.zio,
      "dev.zio" %% "zio-test"     % Versions.zio % Test,
      "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
    )

    val json = Seq(
      "dev.zio" %% "zio-json"        % Versions.zioJson,
      "dev.zio" %% "zio-json-golden" % Versions.zioJson % Test
    )

    val http = Seq("dev.zio" %% "zio-http" % Versions.zioHttp)

    val logging = Seq(
      "dev.zio" %% "zio-logging"       % Versions.zioLogging,
      "dev.zio" %% "zio-logging-slf4j" % Versions.zioLogging
    )

    val config = Seq(
      "dev.zio" %% "zio-config"          % Versions.zioConfig,
      "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
      "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig
    )

    val schema = Seq(
      "dev.zio" %% "zio-schema"            % Versions.zioSchema,
      "dev.zio" %% "zio-schema-json"       % Versions.zioSchema,
      "dev.zio" %% "zio-schema-derivation" % Versions.zioSchema,
      "dev.zio" %% "zio-schema-protobuf"   % Versions.zioSchema
    )

    val kafka = Seq("dev.zio" %% "zio-kafka" % Versions.zioKafka)

  }

  object http4s {

    // used by `api`: http4s server/client wiring
    val core  = Seq(http4s("dsl"), http4s("ember-server"), http4s("ember-client"))
    val circe = Seq(http4s("circe"))

  }

  object sttp {

    // used by `root`, `payments`, `examples`: send the generated sttp requests
    val core = Seq(sttpDep("core"))
    val cats = Seq(sttpDep("cats"))
    val fs2  = Seq(sttpDep("fs2"))
    // used by `protocols`, the codegen modules: jsoniter body serialization
    val jsoniter = Seq(sttpDep("jsoniter"))
    val circe    = Seq(sttpDep("circe"))

    val backends = Seq(
      sttpDep("slf4j-backend"),
      sttpDep("okhttp-backend"),
      sttpDep("prometheus-backend"),
      sttpDep("scribe-backend"),
      sttpDep("async-http-client-backend-fs2"),
      sttpDep("http4s-backend"),
      sttpDep("zio")
    )

  }

  object tapir {

    // used by `api`
    val core = Seq(
      tapirDep("core"),
      tapirDep("http4s-server"),
      tapirDep("jsoniter-scala"),
      tapirDep("openapi-docs")
    )

    val extras = Seq(tapirDep("swagger-ui-bundle"), tapirDep("iron"))

  }

  object json {

    // used by `root`, `protocols`, the codegen modules
    val jsoniter = Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % Versions.jsoniter,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % Versions.jsoniter % Provided
    )

    // used by the codegen modules, alongside `jsoniter`
    val jsoniterCirce = Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-circe" % Versions.jsoniter
    )

    val circe = Seq(circeDep("core"), circeDep("generic"), circeDep("parser"))

  }

  object iron {

    val core       = Seq("io.github.iltotore" %% "iron" % Versions.iron)
    val zioJson    = Seq("io.github.iltotore" %% "iron-zio-json" % Versions.iron)
    val jsoniter   = Seq("io.github.iltotore" %% "iron-jsoniter" % Versions.iron)
    val chimney    = Seq("io.github.iltotore" %% "iron-chimney" % Versions.iron)
    val doobie     = Seq("io.github.iltotore" %% "iron-doobie" % Versions.iron)
    val skunk      = Seq("io.github.iltotore" %% "iron-skunk" % Versions.iron)
    val pureconfig = Seq("io.github.iltotore" %% "iron-pureconfig" % Versions.iron)

  }

  object database {

    // used by `core`, `ledger`, `loans`: compose with `Dependencies.catsEffect`
    val postgres = Seq(
      "org.postgresql" % "postgresql"  % Versions.postgres,
      "org.tpolecat"  %% "skunk-core"  % Versions.skunk,
      "org.flywaydb"   % "flyway-core" % Versions.flyway
    )

    val quill    = Seq("io.getquill" %% "quill-jdbc-zio" % Versions.quill)
    val magnum   = Seq("com.augustnagro" %% "magnum" % Versions.magnum)
    val hikaricp = Seq("com.zaxxer" % "HikariCP" % Versions.hikaricp)

  }

  object aws {

    val core = Seq(
      "software.amazon.awssdk" % "ec2"            % Versions.awsV2,
      "software.amazon.awssdk" % "iam"            % Versions.awsV2,
      "software.amazon.awssdk" % "rds"            % Versions.awsV2,
      "software.amazon.awssdk" % "kms"            % Versions.awsV2,
      "software.amazon.awssdk" % "secretsmanager" % Versions.awsV2,
      "software.amazon.awssdk" % "sts"            % Versions.awsV2
    )

  }

  object azure {

    val core = Seq(
      "com.azure" % "azure-identity"                  % Versions.azureIdentity,
      "com.azure" % "azure-security-keyvault-keys"    % Versions.azureKv,
      "com.azure" % "azure-security-keyvault-secrets" % Versions.azureKv
    )

  }

  object security {

    // used by `root`
    val core = Seq(
      "org.bouncycastle" % "bcpkix-jdk18on"  % Versions.bouncycastle,
      "com.password4j"   % "password4j"      % Versions.password4j,
      "com.nimbusds"     % "nimbus-jose-jwt" % Versions.nimbusJoseJwt,
      "com.nimbusds"     % "oauth2-oidc-sdk" % Versions.nimbusOauth2Oidc
    )

    val bouncycastleProvider = Seq("org.bouncycastle" % "bcprov-jdk18on" % Versions.bouncycastle)
    val pemKeystore          = Seq("de.dentrassi.crypto" % "pem-keystore" % "3.0.0")
    val vault                = Seq("com.bettercloud" % "vault-java-driver" % Versions.vault)
    val auth0                = Seq("com.auth0" % "java-jwt" % Versions.auth0)

    val jwt = Seq(
      "com.github.jwt-scala" %% "jwt-zio-json" % Versions.jwtScala,
      "com.github.jwt-scala" %% "jwt-circe"    % Versions.jwtScala
    )

  }

  object observability {

    val datadog = Seq(
      ("com.datadoghq" % "datadog-api-client" % Versions.datadog).classifier("shaded-jar")
    )

    val kamon = Seq(
      "io.kamon" %% "kamon-bundle"     % Versions.kamon,
      "io.kamon" %% "kamon-prometheus" % Versions.kamon
    )

    val otel = Seq(
      "org.typelevel" %% "otel4s-core"     % Versions.otel4s,
      "org.typelevel" %% "otel4s-otlp"     % Versions.otel4s,
      "org.typelevel" %% "otel4s-otelJava" % Versions.otel4s
    )

  }

  object logging {

    // used by `common`
    val runtime = Seq(
      "org.slf4j"      % "slf4j-api"       % Versions.slf4j,
      "ch.qos.logback" % "logback-classic" % Versions.logback % Runtime
    )

    // used by `testkit`
    val test = Seq(
      "org.slf4j"      % "slf4j-api"       % Versions.slf4j   % Test,
      "ch.qos.logback" % "logback-classic" % Versions.logback % Test
    )

    val scribe = Seq(
      "com.outr" %% "scribe"       % Versions.scribe,
      "com.outr" %% "scribe-slf4j" % Versions.scribe,
      "com.outr" %% "scribe-cats"  % Versions.scribe
    )

  }

  object config {

    // used by `common`
    val pureconfig = Seq("com.github.pureconfig" %% "pureconfig-core" % Versions.pureconfig)

    val pureconfigGeneric = Seq(
      "com.github.pureconfig" %% "pureconfig-generic-scala3" % Versions.pureconfig
    )

  }

  object testing {

    // used by `root`
    val munit = Seq("org.scalameta" %% "munit" % Versions.munit % Test)

    val munitCatsEffect = Seq(
      "org.typelevel" %% "munit-cats-effect" % Versions.munitCatsEffect % Test
    )

    val hedgehog = Seq(
      "qa.hedgehog" %% "hedgehog-core" % Versions.hedgehog % Test,
      "qa.hedgehog" %% "hedgehog-sbt"  % Versions.hedgehog % Test
    )

    val scalacheck = Seq("org.scalacheck" %% "scalacheck" % Versions.scalacheck % Test)

  }

  // Tasks
  lazy val generate = taskKey[Seq[File]]("generate code from APIs")

}
