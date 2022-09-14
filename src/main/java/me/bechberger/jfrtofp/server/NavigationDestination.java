package me.bechberger.jfrtofp.server;

import org.jetbrains.annotations.Nullable;

public class NavigationDestination {
    public final String packageName;
    public final String className;
    @Nullable
    public final String methodNameAndDescriptor;
    public final int lineNumber;

    public NavigationDestination(String packageName, String className, @Nullable String methodNameAndDescriptor, int lineNumber) {
        this.packageName = packageName;
        this.className = className;
        this.methodNameAndDescriptor = methodNameAndDescriptor;
        this.lineNumber = lineNumber;
    }
}