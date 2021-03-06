/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.detector.api;

import static com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import static com.android.tools.lint.client.api.JavaParser.TypeDescriptor;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintDriver;

import java.io.File;

import lombok.ast.ClassDeclaration;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.MethodDeclaration;
import lombok.ast.Node;
import lombok.ast.Position;

/**
 * A {@link Context} used when checking Java files.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
public class JavaContext extends Context {
    static final String SUPPRESS_COMMENT_PREFIX = "//noinspection "; //$NON-NLS-1$

    /** The parse tree */
    private Node mCompilationUnit;

    /** The parser which produced the parse tree */
    private final JavaParser mParser;

    /**
     * Constructs a {@link JavaContext} for running lint on the given file, with
     * the given scope, in the given project reporting errors to the given
     * client.
     *
     * @param driver the driver running through the checks
     * @param project the project to run lint on which contains the given file
     * @param main the main project if this project is a library project, or
     *            null if this is not a library project. The main project is
     *            the root project of all library projects, not necessarily the
     *            directly including project.
     * @param file the file to be analyzed
     * @param parser the parser to use
     */
    public JavaContext(
            @NonNull LintDriver driver,
            @NonNull Project project,
            @Nullable Project main,
            @NonNull File file,
            @NonNull JavaParser parser) {
        super(driver, project, main, file);
        mParser = parser;
    }

    /**
     * Returns a location for the given node
     *
     * @param node the AST node to get a location for
     * @return a location for the given node
     */
    @NonNull
    public Location getLocation(@NonNull Node node) {
        return mParser.getLocation(this, node);
    }

    @NonNull
    public JavaParser getParser() {
        return mParser;
    }

    @Nullable
    public Node getCompilationUnit() {
        return mCompilationUnit;
    }

    /**
     * Sets the compilation result. Not intended for client usage; the lint infrastructure
     * will set this when a context has been processed
     *
     * @param compilationUnit the parse tree
     */
    public void setCompilationUnit(@Nullable Node compilationUnit) {
        mCompilationUnit = compilationUnit;
    }

    @Override
    public void report(@NonNull Issue issue, @Nullable Location location,
            @NonNull String message, @Nullable Object data) {
        if (mDriver.isSuppressed(this, issue, mCompilationUnit)) {
            return;
        }
        super.report(issue, location, message, data);
    }

    /**
     * Reports an issue applicable to a given AST node. The AST node is used as the
     * scope to check for suppress lint annotations.
     *
     * @param issue the issue to report
     * @param scope the AST node scope the error applies to. The lint infrastructure
     *    will check whether there are suppress annotations on this node (or its enclosing
     *    nodes) and if so suppress the warning without involving the client.
     * @param location the location of the issue, or null if not known
     * @param message the message for this warning
     * @param data any associated data, or null
     */
    public void report(
            @NonNull Issue issue,
            @Nullable Node scope,
            @Nullable Location location,
            @NonNull String message,
            @Nullable Object data) {
        if (scope != null && mDriver.isSuppressed(this, issue, scope)) {
            return;
        }
        super.report(issue, location, message, data);
    }


    @Nullable
    public static Node findSurroundingMethod(Node scope) {
        while (scope != null) {
            Class<? extends Node> type = scope.getClass();
            // The Lombok AST uses a flat hierarchy of node type implementation classes
            // so no need to do instanceof stuff here.
            if (type == MethodDeclaration.class || type == ConstructorDeclaration.class) {
                return scope;
            }

            scope = scope.getParent();
        }

        return null;
    }

    @Nullable
    public static ClassDeclaration findSurroundingClass(Node scope) {
        while (scope != null) {
            Class<? extends Node> type = scope.getClass();
            // The Lombok AST uses a flat hierarchy of node type implementation classes
            // so no need to do instanceof stuff here.
            if (type == ClassDeclaration.class) {
                return (ClassDeclaration) scope;
            }

            scope = scope.getParent();
        }

        return null;
    }

    @Override
    @Nullable
    protected String getSuppressCommentPrefix() {
        return SUPPRESS_COMMENT_PREFIX;
    }

    public boolean isSuppressed(@NonNull Node scope, @NonNull Issue issue) {
        // Check whether there is a comment marker
        String contents = getContents();
        assert contents != null; // otherwise we wouldn't be here
        Position position = scope.getPosition();
        if (position == null) {
            return false;
        }

        int start = position.getStart();
        return isSuppressedWithComment(start, issue);
    }

    @NonNull
    public Location.Handle createLocationHandle(@NonNull Node node) {
        return mParser.createLocationHandle(this, node);
    }

    @Nullable
    public ResolvedNode resolve(@NonNull Node node) {
        return mParser.resolve(this, node);
    }

    @Nullable
    public TypeDescriptor getType(@NonNull Node node) {
        return mParser.getType(this, node);
    }
}
