package co.elastic.codesearch.java;

import java.util.Set;

public class SourceFile {
    public final String version;
    public final String language;
    public final String fileName;
    public final String path;
    public final String source;
    public final Set<Class> classes;

    public SourceFile(String version, String language, String fileName, String path, String source, Set<Class> classes) {
        this.version = version;
        this.language = language;
        this.fileName = fileName;
        this.path = path;
        this.source = source;
        this.classes = classes;
    }
}
