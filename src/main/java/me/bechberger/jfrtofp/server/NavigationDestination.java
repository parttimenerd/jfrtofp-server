package me.bechberger.jfrtofp.server;

import org.jetbrains.annotations.Nullable;

public class NavigationDestination {
    public final String name;
    @Nullable
    public final String file;
    public final int line;
    public final int column;

    public NavigationDestination(String name, @Nullable String file, int line, int column) {
        this.name = name;
        this.file = file;
        this.line = line;
        this.column = column;
    }
}