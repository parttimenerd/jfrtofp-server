package me.bechberger.jfrtofp.server;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a method in a file, that can be navigated to
 */
public class NavigationDestination {
    public final String pkg;
    public final String klass;
    public final String method;
    public final int line;

    public NavigationDestination(String pkg, String klass, String method, int line) {
        this.pkg = pkg;
        this.klass = klass;
        this.method = method;
        this.line = line;
    }

    public String toString() {
        return "NavigationDestination(pkg=" + this.pkg + ", klass=" + this.klass + ", method=" + this.method + ", " +
                "line=" + this.line + ")";
    }
}