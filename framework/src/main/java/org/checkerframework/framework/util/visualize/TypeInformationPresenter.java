package org.checkerframework.framework.util.visualize;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreePath;

/**
 * Presents formatted type information for various AST nodes in a class.
 *
 * <p>The formatted type information can be useful for debugging and is designed to be visualized by
 * editors and IDEs that support Language Server Protocol (LSP) {@link LspTypeInformationPresenter}
 * .
 */
public interface TypeInformationPresenter {
    /**
     * The entry point for presenting type information of trees in the given class.
     *
     * @param tree a ClassTree that has been type-checked by the factory
     * @param treePath a {@link TreePath} to {@code tree}
     */
    void process(ClassTree tree, TreePath treePath);
}
