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

  override val name: String = "code-search"
  override lazy val components: List[PluginComponent] = List(Component)
  override val description: String = "Formats documents for indexing"

  override def processOptions(
    options: List[String],
    error: String => Unit
  ): Unit = {
    println(s"options = ${options}")
    options.map(_.split(":").toList).foreach {
      case "output" :: "file" :: path :: Nil =>
        val f = new File(path)
        f.delete()
        f.getParentFile.mkdirs()
        f.createNewFile()
        Component.outputFile = Some(f)
      case "output" :: "es" :: "url" :: url =>
        println(s"hit es url = ${url.mkString(":")}")
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
                case ScalaLanguageElement.Class(name) => Map("name" -> name.value)
                case ScalaLanguageElement.Def(name, _, _) => Map("name" -> name.value)
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
        println(s"source indexer = ${sourceIndexer}")
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
//        def processTree(tree: Tree, acc: Set[SourceSpan], excludeIfPos: Option[Int] = None): Set[SourceSpan] = {
////          val childSpans = if (tree.children.nonEmpty) {
////            tree.children.foldLeft(acc) {
////              case (acc, child) =>
////                acc ++ processTree(child, acc)
////            }
////          } else {
////            acc
////          }
//          acc ++ (tree match {
//            case PackageDef(pid, stats) =>
////              println(s"PackageDef found ${pid.toString()}")
//              stats.map(t => processTree(t, Set.empty)).toSet.flatten
//            case tree @ ClassDef(mods, name, tparams, impl) =>
////              println(s"ClassDef found ${name.toString()}")
//              Set(SourceSpan(tree.pos.start, tree.pos.start + name.length(), Class(Name(tree.symbol.fullName)))) ++ impl.body
//                .map(t => processTree(t, Set.empty, Some(tree.pos.start)))
//                .toSet
//                .flatten
//            case ModuleDef(mods, name, impl) =>
////              println(s"ModuleDef found ${name.toString()}")
//              impl.body.map(t => processTree(t, Set.empty, Some(tree.pos.start))).toSet.flatten
//            case tree @ ValDef(mods, name, tbt, rhs) =>
//              val thisSpans = if (tree.pos.isRange) {
//                Set(
//                  SourceSpan(
//                    start = tree.pos.start,
//                    end = tree.pos.end,
//                    element = Val(Name(tree.symbol.fullName), Type(tbt.toString))
//                  )
//                )
//              } else {
//                Set.empty
//              }
//              thisSpans ++ processTree(
//                rhs,
//                Set.empty,
//                Some(tree.pos.start)
//              )
////              println(s"ValDef found ${name.toString()}")
//            case tree @ DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
//              excludeIfPos match {
//                case Some(pos) if pos == tree.pos.start =>
//                  Set.empty
//                case _ if tree.name.toString().contains("$default$") =>
//                  Set.empty
//                case _ =>
//                  val params = vparamss.flatten.map { v =>
//                    Val(Name(v.name.toString), Type(v.tpt.toString))
//                  }
//                  Set(
//                    SourceSpan(
//                      start = tree.pos.start,
//                      end = tree.pos.start + name.length(),
//                      element = Def(name = Name(tree.symbol.fullName), params = params, tpe = Type(tpt.toString()))
//                    )
//                  )
//              }
//            case TypeDef(mods, name, tparams, rhs) =>
////              println(s"TypeDef found ${name.toString()}")
//              Set.empty[SourceSpan]
//            case LabelDef(name, params, rhs) =>
////              println(s"LabelDef found ${name.toString()}")
//              Set.empty[SourceSpan]
//            case Import(expr, selectors) =>
////              println(s"Import found")
//              Set.empty[SourceSpan]
//            case Template(parents, self, body) =>
//              //println(s"Template found")
//              Set.empty[SourceSpan]
//            case Block(stats, expr) => Set.empty[SourceSpan]
//            case CaseDef(pat, guard, body) => Set.empty[SourceSpan]
//            case Alternative(trees) => Set.empty[SourceSpan]
//            case Star(elem) => Set.empty[SourceSpan]
//            case Bind(name, body) => Set.empty[SourceSpan]
//            case UnApply(fun, args) => Set.empty[SourceSpan]
//            case ArrayValue(elemtpt, elems) => Set.empty[SourceSpan]
//            case Function(vparams, body) => Set.empty[SourceSpan]
//            case Assign(lhs, rhs) => Set.empty[SourceSpan]
//            case AssignOrNamedArg(lhs, rhs) => Set.empty[SourceSpan]
//            case If(cond, thenp, elsep) => Set.empty[SourceSpan]
//            case Match(selector, cases) => Set.empty[SourceSpan]
//            case Return(expr) => Set.empty[SourceSpan]
//            case Try(block, catches, finalizer) => Set.empty[SourceSpan]
//            case Throw(expr) => Set.empty[SourceSpan]
//            case New(tpt) => Set.empty[SourceSpan]
//            case Typed(expr, tpt) => Set.empty[SourceSpan]
//            case TypeApply(fun, args) => Set.empty[SourceSpan]
//            case Apply(fun, args) => Set.empty[SourceSpan]
//            case ApplyDynamic(qual, args) => Set.empty[SourceSpan]
//            case Super(qual, mix) => Set.empty[SourceSpan]
//            case This(qual) => Set.empty[SourceSpan]
//            case Select(qualifier, name) => Set.empty[SourceSpan]
//            case Ident(name) => Set.empty[SourceSpan]
//            case ReferenceToBoxed(ident) => Set.empty[SourceSpan]
//            case Literal(value) => Set.empty[SourceSpan]
//            case Annotated(annot, arg) => Set.empty[SourceSpan]
//            case SingletonTypeTree(ref) => Set.empty[SourceSpan]
//            case SelectFromTypeTree(qualifier, name) => Set.empty[SourceSpan]
//            case CompoundTypeTree(templ) => Set.empty[SourceSpan]
//            case AppliedTypeTree(tpt, args) => Set.empty[SourceSpan]
//            case TypeBoundsTree(lo, hi) => Set.empty[SourceSpan]
//            case ExistentialTypeTree(tpt, whereClauses) => Set.empty[SourceSpan]
//            case TypeTree() => Set.empty[SourceSpan]
//            case EmptyTree => Set.empty[SourceSpan]
//          })
//        }

        def processTree(tree: Tree, prefix: String, parent: Option[Tree] = None): Set[SourceSpan] = {

          val spans: Set[SourceSpan] = tree match {
            case tree: SymTree =>
              tree match {
                case tree @ RefTree(qualifier, name) =>
                  println(s"${prefix}ref ${tree.getClass.getSimpleName} ${tree.symbol.fullName} ${tree.pos.show}")
                  Set.empty[SourceSpan]
                case tree: DefTree =>
                  tree match {
                    case tree @ PackageDef(pid, stats) =>
                      println(s"${prefix}package ${tree.symbol.fullName} ${tree.pos.show}")
                      Set.empty[SourceSpan]
                    case ClassDef(mods, name, tparams, impl) =>
                      println(s"${prefix}class ${tree.symbol.fullName} ${tree.pos.show}")
                      Set(
                        SourceSpan(
                          tree.pos.start,
                          tree.pos.end,
                          ScalaLanguageElement.Class(ScalaLanguageElement.Name(tree.symbol.fullName))
                        )
                      )
                    case ModuleDef(mods, name, impl) =>
                      println(s"${prefix}object ${tree.symbol.fullName}$$ ${tree.pos.show}")
                      Set(
                        SourceSpan(
                          tree.pos.start,
                          tree.pos.end,
                          ScalaLanguageElement.Class(ScalaLanguageElement.Name(s"${tree.symbol.fullName}$$"))
                        )
                      )
                    case ValDef(mods, name, tpt, rhs) =>
                      println(
                        s"${prefix}val ${tree.symbol.fullName}: ${tree.symbol.tpe.typeSymbol.fullName} ${tree.pos.show}"
                      )
                      Set.empty[SourceSpan]
                    case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
                      println(
                        s"${prefix}def ${tree.symbol.fullName}: ${tree.symbol.tpe.typeSymbol.fullName} ${tree.pos.show}"
                      )
                      Set(
                        SourceSpan(
                          tree.pos.start,
                          tree.pos.end,
                          ScalaLanguageElement.Def(
                            ScalaLanguageElement.Name(tree.symbol.fullName),
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
          val symbols = if (tree.hasSymbolField && tree.symbol.pos.isRange) {
            Set((tree.pos.source.file.path, tree.symbol.pos.start.to(tree.symbol.pos.end), tree.symbol))
          } else {
            Set.empty
          }
          val allSpans = spans ++ tree.children.flatMap(t => processTree(t, s"${prefix}\t", parent = Some(tree))).toSet

          allSpans
        }

        val spans = processTree(unit.body, "\t")

        Component.sourceFiles.append(
          SourceFile(
            Version(version.getOrElse("1.0.0")),
            Scala2_12,
            FileName(unit.source.file.name),
            new String(unit.source.content),
            spans
          )
        )
      }
    }

    override val runsAfter: List[String] = List.empty
  }
}
