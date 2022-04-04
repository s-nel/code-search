package co.elastic.codesearch.scala

import co.elastic.codesearch.index.SourceIndexer

import java.io.{File, FileOutputStream, PrintWriter}
import co.elastic.codesearch.lang.scala.model._
import co.elastic.codesearch.lang.scala.model.ScalaLanguageElement._
import co.elastic.codesearch.model.{SourceFile, SourceSpan}

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
    options.map(_.split(":").toList).foreach {
      case "output" :: path :: Nil =>
        val f = new File(path)
        f.delete()
        f.getParentFile.mkdirs()
        f.createNewFile()
        Component.outputFile = f
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
    var outputFile: File = null
    val sourceFiles = ListBuffer.empty[SourceFile]

    override def newPhase(prev: Phase): Phase = new SpecialPhase(prev, outputFile)

    class SpecialPhase(prev: Phase, outputFile: File) extends StdPhase(prev) {
      override def name = ScalaCompilerPlugin.this.name

      override def run() = {
        val fos = new FileOutputStream(outputFile)
        val sourceIndexer = new SourceIndexer.FileSourceIndexer(fos)
        val resultF = for {
          _ <- Future(super.run())
          _ <- Future.traverse(sourceFiles.toList) { sourceFile =>
            sourceIndexer.indexFile(sourceFile)
          }
        } yield {}
        val result = scala.util.Try(Await.result(resultF, 30.minutes))
        fos.close()
        result.get
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
          println(s"Spans = ${allSpans}")
          //println(s"${prefix}Symbol [${tree.symbol.fullName}] ${tree.symbol.pos.show}")
//          parent match {
//            case Some(p) if !p.pos.isDefined || p.pos.start == tree.pos.start =>
//            case _ =>
//              println(s"${prefix}${tree.getClass.getSimpleName}${if(tree.hasSymbolField){s" - ${tree.symbol.fullName}"}else{""}} - ${tree.pos.show}")
//
//          }
          //Set.empty
          allSpans
        }

        println(s"Unit [${unit.source.file.name}]")
        val spans = processTree(unit.body, "\t")
//        println()
//        println()
//        println("Symbols:")
//        symbols.foreach(s => println(s"\t${s.toString}  -  ${s.pos.show}"))
        //val file = SourceFile(language = Scala2_12, source = new String(unit.source.content), spans = spans)

//        println("\n\n\n\n")
//        symbols.toList.sortBy(_._2.start).foreach {
//          case (file, range, symbol) =>
//            println(s"$file [${range.start}, ${range.end}] -> $symbol")
//        }
//        println("\n\n\n")

        // remove overlapping symbols
//        val dedupedSymbols = symbols.foldLeft(Set.empty[(String, Range, Symbol)]) {
//          case (acc, a @ (file, range, symbol)) =>
//            acc.find(tup => !tup._2.intersect(range).isEmpty) match {
//              case Some(b @ (_, otherRange, _)) if otherRange.length >= range.length =>
//                acc.diff(Set(b)).union(Set(a))
//              case Some(_) =>
//                acc
//              case None =>
//                acc.union(Set(a))
//            }
//        }

        // order symbols
//        val orderedSymbols = dedupedSymbols.toList.sortBy(_._2.start)
//
//        println("\n\n\n\n")
//        orderedSymbols.foreach {
//          case (file, range, symbol) =>
//            println(s"$file [${range.start}, ${range.end}] -> $symbol")
//        }
//        println("\n\n\n")

        //file.spans.toList.sortBy(_.start).foreach(println)

        Component.sourceFiles.append(SourceFile(Scala2_12, new String(unit.source.content), spans))
      }
    }

    override val runsAfter: List[String] = List.empty
  }
}
