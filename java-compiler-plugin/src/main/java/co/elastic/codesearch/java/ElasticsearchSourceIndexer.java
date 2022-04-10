package co.elastic.codesearch.java;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.ObjectProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ElasticsearchSourceIndexer {
    private final ElasticsearchClient client;
    private final ExecutorService executorService;
    private String javaVersion;
    private String codeSearchVersion;
    private final String indexFmt;
    private final String templateFmt;

    public ElasticsearchSourceIndexer(ElasticsearchClient client, ExecutorService executorService, String javaVersion, String codeSearchVersion, String indexFmt, String templateFmt) {
        this.client = client;
        this.executorService = executorService;
        this.javaVersion = javaVersion;
        this.codeSearchVersion = codeSearchVersion;
        this.indexFmt = indexFmt;
        this.templateFmt = templateFmt;
    }

    public Future<Void> indexFile(SourceFile file) {
//        return executorService.submit(new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//                client.index(i -> i.index(getIndexName()).)
//            }
//        })
        return null;
    }

    private String getTemplateName() {
        return String.format(templateFmt, codeSearchVersion, "java", javaVersion);
    }

    private String getIndexName() {
        return String.format(indexFmt, codeSearchVersion, "java", javaVersion);
    }

    public Future<Void> setup() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                client.indices().putIndexTemplate(i ->
                        i.name(getTemplateName()).indexPatterns(getIndexName()).template(t ->
                                t.mappings(m ->
                                        m.properties("version", p -> p.keyword(KeywordProperty.of(k -> k)))
                                                .properties("language", p -> p.object(ObjectProperty.of(o ->
                                                        o.properties("name", pp -> pp.keyword(KeywordProperty.of(k -> k)))
                                                                .properties("version", pp -> pp.keyword(KeywordProperty.of(k -> k))))))
                                                .properties("file_name", p -> p.keyword(k -> k))
                                                .properties("path", p -> p.keyword(k -> k))
                                                .properties("source", p -> p.object(o ->
                                                        o.enabled(false).properties("contents", pp -> pp.text(tt -> tt))))
                                                .properties("spans", p -> p.nested(n ->
                                                        n.properties("start", pp -> pp.integer(ii -> ii))
                                                                .properties("end", pp -> pp.integer(ii -> ii))
                                                                .properties("element", pp -> pp.object(o ->
                                                                        o.properties("kind", ppp -> ppp.keyword(k -> k))
                                                                ))
                                                ))
                                )
                        )
                );
                return null;
            }
        });
    }
}
