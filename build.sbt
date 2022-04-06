scalaVersion := "2.12.10"
//version := "1.0.0"

lazy val root = (project in file(".")).aggregate(example, core, scalaCompilerPlugin, elasticsearchIndexer)

lazy val example = (project in file("example"))
  .settings(
    scalaVersion := "2.12.10",
    fork := true,
    javaOptions ++= Seq(
      "-Delastic.apm.service_name=foo",
      "-Delastic.apm.server_urls=http://localhost:8200",
      "-Delastic.apm.secret_token=mysecrettoken122",
      "-Delastic.apm.application_packages=foo,bar"
    ),
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.0.1",
      "co.elastic.apm" % "apm-agent-api" % "1.28.4",
      "co.elastic.apm" % "apm-agent-attach" % "1.28.4",
      "co.elastic.logging" % "logback-ecs-encoder" % "1.3.2",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
    ),
    scalacOptions ++= Seq(
      s"-Xplugin:${assembly.in(Compile).in(scalaCompilerPlugin).value}",
      s"-P:code-search:output:file:${target.value / "code-search" / "output.log"}",
      s"-P:code-search:version:${version.value}",
      s"-P:code-search:output:es:url:http://localhost:9200",
      "-Yrangepos"
    )
  )
  .enablePlugins(CodeSearchPlugin)

lazy val core = (project in file("core")).settings(
  scalaVersion := "2.12.10",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.1.0",
    "com.sksamuel.elastic4s" %% "elastic4s-core" % "7.3.5",
    "io.circe" %% "circe-core" % "0.12.3",
    "io.circe" %% "circe-generic" % "0.12.3",
    "io.circe" %% "circe-parser" % "0.12.3"
  )
)

lazy val javaCompilerPlugin = (project in file("java-compiler-plugin")).enablePlugins(SbtJdiTools)

lazy val elasticsearchIndexer = (project in file("elasticsearch-indexer")).settings(
  scalaVersion := "2.12.10",
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.1",
    "com.sksamuel.elastic4s" %% "elastic4s-core" % "7.17.2",
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.17.2",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
  )
).dependsOn(core)

lazy val scalaCompilerPlugin = (project in file("scala-compiler-plugin"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    ),
    assemblyJarName in assembly := "scala-compiler-plugin.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  )
  .dependsOn(core, elasticsearchIndexer)
