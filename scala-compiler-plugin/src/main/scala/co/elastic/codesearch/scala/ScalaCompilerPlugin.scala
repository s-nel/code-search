package co.elastic.codesearch.scala

import co.elastic.codesearch.index.SourceIndexer
import co.elastic.codesearch.index.elasticsearch.ElasticsearchSourceIndexer

import java.io.{File, FileOutputStream, PrintWriter}
import co.elastic.codesearch.lang.scala.model._
import co.elastic.codesearch.lang.scala.model.ScalaLanguageElement._
import co.elastic.codesearch.model.{FileName, LanguageElement, SourceFile, SourceSpan, Version}
import com.sksamuel.elastic4s.fields.KeywordField
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.http.JavaClient

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}

class ScalaCompilerPlugin(val global: Global) extends Plugin {
  import global._

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  override val name: String = "code-indexer-scala"
  override lazy val components: List[PluginComponent] = List(Component)
  override val description: String = "Formats documents for indexing"

  override def processOptions(
    options: List[String],
    error: String => Unit
  ): Unit = {
    options.map(_.split(":").toList).foreach {
      case "output" :: "file" :: path :: Nil =>
        val f = new File(path)
        f.delete()
        f.getParentFile.mkdirs()
        f.createNewFile()
        Component.outputFile = Some(f)
      case "output" :: "es" :: "url" :: url =>
        Component.esUrl = Some(url.mkString(":"))
      case "version" :: version :: Nil =>
        Component.version = Some(version)
      case _ =>
    }
  }

  override val optionsHelp: Option[String] =
    Some("""
      |  -P:code-search:output:<path> Sets the output file of the code-search document
      |""".stripMargin)

  private object Component extends PluginComponent {
    override val global: ScalaCompilerPlugin.this.global.type = ScalaCompilerPlugin.this.global
    override val phaseName: String = ScalaCompilerPlugin.this.name
    override val runsRightAfter: Option[String] = Some("typer")
    var outputFile: Option[File] = None
    var version: Option[String] = None
    val sourceFiles = ListBuffer.empty[SourceFile]
    var esUrl: Option[String] = None

    override def newPhase(prev: Phase): Phase = new SpecialPhase(prev)

    class SpecialPhase(prev: Phase) extends StdPhase(prev) {
      override def name = ScalaCompilerPlugin.this.name

      override def run() = {
        val sourceIndexer = (esUrl, outputFile) match {
          case (Some(esUrl), _) =>
            def languageElementFields(el: LanguageElement): Map[String, Any] = {
              el match {
                case ScalaLanguageElement.Class(names) => Map("name" -> names.toList.map(_.value))
                case ScalaLanguageElement.Object(names) => Map("name" -> names.toList.map(_.value))
                case ScalaLanguageElement.Def(names, _, _) => Map("name" -> names.toList.map(_.value))
                case _ => Map.empty[String, Any]
              }
            }

            new ElasticsearchSourceIndexer(
              client = ElasticClient(JavaClient(ElasticProperties(esUrl))),
              codeSearchVersion = "1.0.0",
              language = Scala2_12,
              languageElementTemplate = Seq(
                KeywordField("name")
              ),
              languageElementFields = languageElementFields
            )
          case (_, Some(outputFile)) =>
            val fos = new FileOutputStream(outputFile)
            new SourceIndexer.FileSourceIndexer(fos)
          case _ =>
            throw new Exception("SourceIndexer not configured")
        }
        val resultF = for {
          _ <- Future(super.run())
          _ <- sourceIndexer.setup()
          _ <- Future.traverse(sourceFiles.toList) { sourceFile =>
            sourceIndexer.indexFile(sourceFile)
          }
        } yield {}
        Await.result(resultF, 30.minutes)
      }

      override def apply(unit: CompilationUnit): Unit = {

        def processTree(tree: Tree, prefix: String, parent: Option[Tree] = None): Set[SourceSpan] = {

          val spans: Set[SourceSpan] = tree match {
            case tree: SymTree =>
              tree match {
                case tree @ RefTree(qualifier, name) =>
                  //println(s"${prefix}ref ${tree.getClass.getSimpleName} ${tree.symbol.fullName} ${tree.pos.show}")
                  Set.empty[SourceSpan]
                case tree: DefTree =>
                  tree match {
                    case tree @ PackageDef(pid, stats) =>
                      //println(s"${prefix}package ${tree.symbol.fullName} ${tree.pos.show}")
                      Set.empty[SourceSpan]
                    case ClassDef(mods, name, tparams, impl) =>
                      //println(s"${prefix}class ${tree.symbol.fullName} ${tree.pos.show}")
                      Set(
                        SourceSpan(
                          Some(tree.pos.start),
                          Some(tree.pos.end),
                          ScalaLanguageElement.Class(
                            Set(
                              ScalaLanguageElement.Name(tree.symbol.fullName),
                              ScalaLanguageElement.Name(tree.symbol.nameString)
                            )
                          )
                        )
                      )
                    case ModuleDef(mods, name, impl) =>
                      //println(s"${prefix}object ${tree.symbol.fullName}$$ ${tree.pos.show}")
                      Set(
                        SourceSpan(
                          Some(tree.pos.start),
                          Some(tree.pos.end),
                          ScalaLanguageElement.Object(
                            Set(
                              ScalaLanguageElement.Name(tree.symbol.fullName),
                              ScalaLanguageElement.Name(tree.symbol.nameString),
                              ScalaLanguageElement.Name(s"${tree.symbol.fullName}$$")
                            )
                          )
                        )
                      )
                    case ValDef(mods, name, tpt, rhs) =>
//                      println(
//                        s"${prefix}val ${tree.symbol.fullName}: ${tree.symbol.tpe.typeSymbol.fullName} ${tree.pos.show}"
//                      )
                      Set.empty[SourceSpan]
                    case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
//                      println(
//                        s"${prefix}def ${tree.symbol.fullName}: ${tree.symbol.tpe.typeSymbol.fullName} ${tree.pos.show}"
//                      )
                      Set(
                        SourceSpan(
                          Some(tree.pos.start),
                          Some(tree.pos.end),
                          ScalaLanguageElement.Def(
                            Set(
                              ScalaLanguageElement.Name(tree.symbol.fullName),
                              ScalaLanguageElement.Name(tree.symbol.nameString)
                            ),
                            List.empty,
                            ScalaLanguageElement.Type(tpt.symbol.fullName)
                          )
                        )
                      )
                    case other =>
                      Set.empty[SourceSpan]
                  }
                case other =>
                  Set.empty[SourceSpan]
              }
            case _ =>
              Set.empty[SourceSpan]
          }

          spans ++ tree.children.flatMap(t => processTree(t, s"${prefix}\t", parent = Some(tree))).toSet
        }

        val spans = processTree(unit.body, "\t")

        Component.sourceFiles.append(
          SourceFile(
            Version(version.getOrElse("1.0.0")),
            Scala2_12,
            FileName(unit.source.file.name),
            unit.source.file.path,
            new String(unit.source.content),
            spans
          )
        )
      }
    }

    override val runsAfter: List[String] = List.empty
  }
}
