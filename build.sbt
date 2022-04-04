scalaVersion := "2.12.10"

lazy val root = (project in file(".")).aggregate(sbtPlugin, example, core)

lazy val example = (project in file("example"))
  .settings(
    scalaVersion := "2.12.10",
    scalacOptions ++= Seq(
      s"-Xplugin:${assembly.in(Compile).in(scalaCompilerPlugin).value}",
      s"-P:code-search:output:${target.value / "code-search" / "output.log"}",
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
  .dependsOn(core)

lazy val esIndexer = (project in file("es-indexer")).settings(
  scalaVersion := "2.12.10",
  libraryDependencies ++= Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % "7.17.2"
  )
)
