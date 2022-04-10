package co.elastic.codesearch.java;

import java.util.Objects;
import java.util.Set;

public class Class {
    public final Set<String> name;

    public Class(Set<String> name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Class)) return false;
        Class aClass = (Class) o;
        return Objects.equals(name, aClass.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
