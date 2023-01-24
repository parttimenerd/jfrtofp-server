package me.bechberger.jfrtofp.server;

/**
 * Represents a method in a file, that can be requested
 */
public class ClassLocation {
    public final String pkg;
    public final String klass;

    public ClassLocation(String pkg, String klass) {
        this.pkg = pkg;
        this.klass = klass;
    }

    public String toString() {
        return "ClassLocation(pkg=" + this.pkg + ", klass=" + this.klass + ")";
    }
}