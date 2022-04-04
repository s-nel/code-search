package co.elastic.codesearch.index

import co.elastic.codesearch.index.SourceIndexer.IndexError
import co.elastic.codesearch.model.SourceFile

import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}

trait SourceIndexer {
  def indexFile(file: SourceFile): Future[Either[IndexError, Unit]]
}

object SourceIndexer {
  sealed trait IndexError

  class FileSourceIndexer(fos: FileOutputStream)(implicit ec: ExecutionContext) extends SourceIndexer {
    override def indexFile(file: SourceFile): Future[Either[IndexError, Unit]] = Future {
      fos.write(file.toString.getBytes(StandardCharsets.UTF_8))
      Right(())
    }
  }
}
