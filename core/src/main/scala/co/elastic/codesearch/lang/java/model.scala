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

package co.elastic.codesearch.lang.java

import co.elastic.codesearch.model._

sealed trait JavaLanguage extends Language {
  val name = "Java"
}

case object Java11 extends JavaLanguage {
  override val version: Option[Version] = Some(Version("11"))
}

sealed trait JavaLanguageElement extends LanguageElement

final case class Class(names: Set[String]) extends JavaLanguageElement {
  override val kind: LanguageElementKind = LanguageElementKind("class")
}
