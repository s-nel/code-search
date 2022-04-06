package co.elastic.codesearch

object model {
  final case class Version(value: String) extends AnyVal
  final case class FileName(value: String) extends AnyVal

  trait Language {
    val name: String
    val version: Option[Version]
  }

  final case class SourceFile(
    version: Version,
    language: Language,
    fileName: FileName,
    path: String,
    source: String,
    spans: Set[SourceSpan]
  )

  final case class SourceSpan(
    start: Int,
    end: Int,
    element: LanguageElement
  )

  final case class LanguageElementKind(value: String) extends AnyVal

  trait LanguageElement {
    val kind: LanguageElementKind
  }
}
