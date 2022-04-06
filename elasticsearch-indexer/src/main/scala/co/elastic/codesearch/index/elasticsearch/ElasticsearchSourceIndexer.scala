/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  Copyright Elasticsearch B.V. All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch B.V. and its suppliers, if any.
 * The intellectual and technical concepts contained herein
 * are proprietary to Elasticsearch B.V. and its suppliers and
 * may be covered by U.S. and Foreign Patents, patents in
 * process, and are protected by trade secret or copyright
 * law.  Dissemination of this information or reproduction of
 * this material is strictly forbidden unless prior written
 * permission is obtained from Elasticsearch B.V.
 */

package co.elastic.codesearch.index.elasticsearch

import co.elastic.codesearch.index.SourceIndexer
import co.elastic.codesearch.model
import co.elastic.codesearch.model.{Language, LanguageElement}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.{properties, _}
import com.sksamuel.elastic4s.fields.{ElasticField, IntegerField, KeywordField, NestedField, ObjectField, TextField}
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

class ElasticsearchSourceIndexer(
  client: ElasticClient,
  codeSearchVersion: String,
  language: Language,
  languageElementTemplate: Seq[ElasticField],
  languageElementFields: LanguageElement => Map[String, Any],
  indexFmt: String = "code-search-%s-%s%s",
  templateFmt: String = "code-search-%s-%s%s"
)(implicit ec: ExecutionContext)
  extends SourceIndexer {

  val logger = Logger[ElasticsearchSourceIndexer]

  lazy val indexName =
    String
      .format(indexFmt, codeSearchVersion, language.name, language.version.map(v => s"-${v.value}").getOrElse(""))
      .toLowerCase
  lazy val templateName =
    String
      .format(
        templateFmt,
        codeSearchVersion,
        language.name,
        language.version.map(v => s"-${v.value}").getOrElse("")
      )
      .toLowerCase

  override def indexFile(file: model.SourceFile): Future[Either[SourceIndexer.IndexError, Unit]] = {
    logger.info(s"Indexing file [${file.fileName.value}]...")
    for {
      resp <- client.execute {
        indexInto(indexName).withId(file.path).fields(
          "version" -> file.version.value,
          "language" -> Map(
            "name" -> language.name,
            "version" -> language.version.map(_.value)
          ),
          "file_name" -> file.fileName.value,
          "path" -> file.path,
          "source" -> Map(
            "content" -> file.source
          ),
          "spans" -> file.spans.toList.map { span =>
            Map(
              "start" -> span.start,
              "end" -> span.end,
              "element" -> (Map(
                "kind" -> span.element.kind.value
              ) ++ languageElementFields(span.element))
            )
          }
        )
      }
      _ <- if (resp.status >= 400) {
        Future.failed(new Exception(s"Unexpected status [${resp.status}] from Elasticsearch: ${resp.error}"))
      } else {
        Future.successful(())
      }
      _ = logger.info(s"Indexed file [${file.fileName.value}]")
    } yield {
      Right(())
    }
  }

  override def setup(): Future[Either[SourceIndexer.SetupError, Unit]] = {
    logger.info(s"Setting up Elasticsearch template for Code Search ${codeSearchVersion}")
    for {
      resp <- client.execute {
        createIndexTemplate(templateName, List(indexName)).mappings(
          properties(
            KeywordField("version"),
            ObjectField(
              name = "language",
              properties = Seq(
                KeywordField("name"),
                KeywordField("version")
              )
            ),
            KeywordField("file_name"),
            KeywordField("path"),
            ObjectField(
              name = "source",
              enabled = Some(false),
              properties = Seq(
                TextField("contents")
              )
            ),
            NestedField(
              name = "spans",
              properties = Seq(
                IntegerField("start"),
                IntegerField("end"),
                ObjectField(
                  name = "element",
                  properties = (Seq(
                    KeywordField("kind")
                  ) ++ languageElementTemplate)
                )
              )
            )
          )
        )
      }
      _ <- if (resp.status >= 400) {
        Future.failed(new Exception(s"Unexpected status [${resp.status}] from Elasticsearch: ${resp.error}"))
      } else {
        Future.successful(())
      }
      _ = logger.info(s"Finished setting up Elasticsearch template for Code Search ${codeSearchVersion}")
    } yield {
      Right(())
    }
  }
}

object ElasticsearchSourceIndexer {
  sealed trait CreateTemplateError
}
