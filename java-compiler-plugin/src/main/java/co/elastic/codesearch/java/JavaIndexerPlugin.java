package co.elastic.codesearch.java;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JavaIndexerPlugin implements Plugin {
    @Override
    public String getName() {
        return "code-indexer-java";
    }

    @Override
    public void init(JavacTask task, String... args) {
        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent e) {
                TaskListener.super.started(e);
            }

            @Override
            public void finished(TaskEvent e) {
                TaskListener.super.finished(e);
                if (e.getKind().equals(TaskEvent.Kind.ANALYZE)) {
                    try {
                        Set<Class> classes = e.getCompilationUnit().accept(new TreeVisitor<Set<Class>, Set<Class>>() {

                            @Override
                            public Set<Class> visitAnnotatedType(AnnotatedTypeTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitAnnotation(AnnotationTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitMethodInvocation(MethodInvocationTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitAssert(AssertTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitAssignment(AssignmentTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitCompoundAssignment(CompoundAssignmentTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitBinary(BinaryTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitBlock(BlockTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitBreak(BreakTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitCase(CaseTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitCatch(CatchTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitClass(ClassTree node, Set<Class> sourceSpans) {
                                Set<String> names = new HashSet<>();
                                names.add(node.getSimpleName().toString());
                                sourceSpans.add(new Class(names));
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitConditionalExpression(ConditionalExpressionTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitContinue(ContinueTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitDoWhileLoop(DoWhileLoopTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitErroneous(ErroneousTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitExpressionStatement(ExpressionStatementTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitEnhancedForLoop(EnhancedForLoopTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitForLoop(ForLoopTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitIdentifier(IdentifierTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitIf(IfTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitImport(ImportTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitArrayAccess(ArrayAccessTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitLabeledStatement(LabeledStatementTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitLiteral(LiteralTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitMethod(MethodTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitModifiers(ModifiersTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitNewArray(NewArrayTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitNewClass(NewClassTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitLambdaExpression(LambdaExpressionTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitPackage(PackageTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitParenthesized(ParenthesizedTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitReturn(ReturnTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitMemberSelect(MemberSelectTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitMemberReference(MemberReferenceTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitEmptyStatement(EmptyStatementTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitSwitch(SwitchTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitSynchronized(SynchronizedTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitThrow(ThrowTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitCompilationUnit(CompilationUnitTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitTry(TryTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitParameterizedType(ParameterizedTypeTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitUnionType(UnionTypeTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitIntersectionType(IntersectionTypeTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitArrayType(ArrayTypeTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitTypeCast(TypeCastTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitPrimitiveType(PrimitiveTypeTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitTypeParameter(TypeParameterTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitInstanceOf(InstanceOfTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitUnary(UnaryTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitVariable(VariableTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitWhileLoop(WhileLoopTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitWildcard(WildcardTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitModule(ModuleTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitExports(ExportsTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitOpens(OpensTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitProvides(ProvidesTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitRequires(RequiresTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitUses(UsesTree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }

                            @Override
                            public Set<Class> visitOther(Tree node, Set<Class> sourceSpans) {
                                return sourceSpans;
                            }
                        }, new HashSet<>());
                        SourceFile sourceFile = new SourceFile("1.0.0", "Java", e.getCompilationUnit().getSourceFile().getName(), e.getCompilationUnit().getSourceFile().getName(), e.getCompilationUnit().getSourceFile().getCharContent(true).toString(), classes);
                        RestClient restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();
                        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
                        ElasticsearchClient client = new ElasticsearchClient(transport);
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        ElasticsearchSourceIndexer indexer = new ElasticsearchSourceIndexer(client, executor, "11", "1.0.0", "c-code-search-%s-%s%s", "c-code-search-%s-%s%s");
                        indexer.setup().get();
                        //indexer.indexFile(sourceFile).get();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                return;
            }
        });
    }
}
