package co.elastic.codesearch.index

import co.elastic.codesearch.index.SourceIndexer.{IndexError, SetupError}
import co.elastic.codesearch.model.SourceFile

import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}

trait SourceIndexer {
  def setup(): Future[Either[SetupError, Unit]]

  def indexFile(file: SourceFile): Future[Either[IndexError, Unit]]
}

object SourceIndexer {
  sealed trait SetupError
  sealed trait IndexError

  class FileSourceIndexer(fos: FileOutputStream)(implicit ec: ExecutionContext) extends SourceIndexer {
    override def indexFile(file: SourceFile): Future[Either[IndexError, Unit]] = Future {
      fos.write(file.toString.getBytes(StandardCharsets.UTF_8))
      Right(())
    }

    override def setup(): Future[Either[SetupError, Unit]] = Future.successful(Right(()))
  }
}
