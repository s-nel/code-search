package co.elastic.codesearch.sbt

import sbt._
import Keys._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CodeSearchPlugin extends AutoPlugin {

  final case class CodeSearchConfiguration(
    endpoint: String,
    username: String,
    password: String
  )

  lazy val index = taskKey[Unit]("Indexes code to the code-search cluster")

  lazy val configuration = settingKey[CodeSearchConfiguration]("The configuration used for code-search")

  override lazy val projectSettings = Seq(
    index := {
      val log = streams.value.log
      val config = configuration.value
      log.info(s"Indexing sources to ${config.endpoint}")
      Future.traverse(sources.value) { source =>
        source.file
      }
    }
  )
}
