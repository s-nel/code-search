package co.elastic.codesearch.lang.scala

import co.elastic.codesearch.model.{Language, LanguageElement, LanguageElementKind, SourceFile, SourceSpan, Version}

object model {
  sealed trait ScalaLanguage extends Language {
    val name = "Scala"
  }

  case object Scala2_12 extends ScalaLanguage {
    override val version: Option[Version] = Some(Version("2.12"))
  }

  sealed trait ScalaLanguageElement extends LanguageElement
  object ScalaLanguageElement {
    final case class Name(value: String) extends AnyVal

    final case class Type(value: String) extends AnyVal

    final case class Def(names: Set[Name], params: List[Val], tpe: Type) extends ScalaLanguageElement {
      override val kind: LanguageElementKind = LanguageElementKind("def")
    }

    final case class Val(names: Set[Name], tpe: Type) extends ScalaLanguageElement {
      override val kind: LanguageElementKind = LanguageElementKind("val")
    }

    final case class Class(names: Set[Name]) extends ScalaLanguageElement {
      override val kind: LanguageElementKind = LanguageElementKind("class")
    }

    final case class Object(names: Set[Name]) extends ScalaLanguageElement {
      override val kind: LanguageElementKind = LanguageElementKind("object")
    }
  }
}
