package org.checkerframework.framework.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;

import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * TreePathCacher is a TreeScanner that creates and caches a TreePath for a target Tree.
 *
 * <p>This class replicates some logic from {@code com.sun.source.util.TreePath.getPath} but caches
 * results so that lookups with overlapping paths reuse each other's work.
 *
 * <p>The search is <em>lazy</em> about allocation: {@link #scan} only pushes and pops the trees it
 * visits on {@link #currentStack} and allocates no {@link TreePath} during traversal. Only when the
 * target is reached does {@link #buildPathForStack} materialize (and cache) the {@link TreePath}
 * nodes along the root-to-target path. Off-path trees visited during a search are therefore not
 * cached, which keeps {@link #foundPaths} far smaller than caching every visited node would. See
 * the "TreePathCacher" entries in {@code docs/developer/performance-notes.md} for the allocation
 * measurements that motivated this design.
 */
public class TreePathCacher extends TreeScanner<TreePath, Tree> {

    /**
     * Caches the {@link TreePath} of every {@link Tree} scanned so far, including the intermediate
     * trees on the way to a search target; these are reused when later targets share a prefix. A
     * {@code null} value is meaningful: it records that a target was searched for via {@link
     * #getPath} but is not present in the compilation unit, so the unit is not re-scanned for it.
     * See {@link #getCachedPath} for how the two readings of a {@code null} lookup are
     * distinguished.
     *
     * <p>This is an {@link IdentityHashMap} rather than a {@link java.util.HashMap} because javac
     * {@link Tree}s do not override {@code Object}'s identity {@code equals}/{@code hashCode};
     * keying by identity is therefore equivalent in behavior while avoiding the per-entry node
     * allocation and virtual {@code equals}/{@code hashCode} dispatch.
     */
    private final IdentityHashMap<Tree, @Nullable TreePath> foundPaths = new IdentityHashMap<>(32);

    /** The compilation unit tree currently being scanned. */
    private @Nullable CompilationUnitTree currentRoot;

    /** The stack of nodes currently being traversed. */
    private final List<Tree> currentStack = new ArrayList<>();

    /** Construct a TreePathCacher. */
    public TreePathCacher() {}

    /**
     * Returns true if the tree is cached.
     *
     * @param target the tree to search for
     * @return true if the tree is cached
     */
    public boolean isCached(@FindDistinct Tree target) {
        return foundPaths.containsKey(target);
    }

    /**
     * Returns the cached path for the given tree, without scanning. Performs a single map lookup.
     *
     * <p>Returns null in two distinct cases that this method does not distinguish: (a) {@code
     * target} is not cached at all, or (b) {@code target} was previously searched for via {@link
     * #getPath} and not found in the compilation unit (in which case {@link #getPath} cached {@code
     * null} to avoid re-scanning). Callers that need to distinguish these cases must follow up with
     * {@link #isCached}.
     *
     * @param target the tree to look up
     * @return the cached path for {@code target}, or null if {@code target} is not cached or is
     *     cached as not-in-unit
     */
    public @Nullable TreePath getCachedPath(@FindDistinct Tree target) {
        return foundPaths.get(target);
    }

    /**
     * Adds the given key and value to the cache.
     *
     * @param target the tree to add
     * @param path the path to cache
     */
    public void addPath(Tree target, TreePath path) {
        foundPaths.put(target, path);
    }

    /**
     * Return the TreePath for a Tree.
     *
     * @param root the compilation unit to search in
     * @param target the target tree to look for
     * @return the TreePath corresponding to target, or null if target is not found in the
     *     compilation root
     */
    public @Nullable TreePath getPath(CompilationUnitTree root, @FindDistinct Tree target) {
        // This method uses try/catch and the private {@code Result} exception for control flow to
        // stop the superclass from scanning other subtrees when target is found.

        TreePath cached = foundPaths.get(target);
        if (cached != null) {
            return cached;
        }
        if (foundPaths.containsKey(target)) {
            // target was previously searched for and not found in the compilation unit.
            return null;
        }

        if (root == target) {
            return new TreePath(root);
        }

        CompilationUnitTree prevRoot = this.currentRoot;
        int prevStackSize = this.currentStack.size();
        this.currentRoot = root;
        try {
            this.scan(root, target);
        } catch (Result result) {
            return result.path;
        } finally {
            this.currentRoot = prevRoot;
            while (this.currentStack.size() > prevStackSize) {
                this.currentStack.remove(this.currentStack.size() - 1);
            }
        }
        // If a path wasn't found, cache null so the whole compilation unit isn't searched again.
        foundPaths.put(target, null);
        return null;
    }

    /**
     * Return the TreePath for {@code target}, searching the compilation unit that {@code rootPath}
     * belongs to, but starting the search at {@code rootPath}'s leaf and expanding outward.
     *
     * <p>This searches the <em>whole</em> compilation unit, not just {@code rootPath}'s subtree
     * (despite taking a path argument, it does not have the subtree-only semantics of {@code
     * com.sun.source.util.TreePath.getPath(TreePath, Tree)}). The reason to pass {@code rootPath}
     * rather than just the compilation unit is <strong>locality</strong>: callers pass the path of
     * the tree they are currently visiting, and the target is almost always inside that subtree, so
     * starting the scan there finds it without rescanning the whole unit. On a class with very many
     * top-level members this turns an O(members) rescan per lookup into a local one.
     *
     * @param rootPath a path within the compilation unit to search; its leaf's subtree is searched
     *     first, and its node chain seeds the path prefix (see the body)
     * @param target the target tree to look for
     * @return the TreePath corresponding to target, or null if target is not in the compilation
     *     unit
     */
    public @Nullable TreePath getPath(TreePath rootPath, @FindDistinct Tree target) {
        TreePath cached = foundPaths.get(target);
        if (cached != null) {
            return cached;
        }
        if (foundPaths.containsKey(target)) {
            // target was previously searched for and not found in the compilation unit.
            return null;
        }

        if (rootPath.getLeaf() == target) {
            return rootPath;
        }

        CompilationUnitTree prevRoot = this.currentRoot;
        int prevStackSize = this.currentStack.size();
        this.currentRoot = rootPath.getCompilationUnit();

        // Seed currentStack with rootPath's node chain (root-to-leaf order, excluding the
        // compilation unit). buildPathForStack walks currentStack starting from currentRoot, so
        // these entries supply the prefix that turns a match found deep under rootPath's leaf into
        // a full root-to-target path. The loop stops before the compilation-unit-level path (the
        // one whose getParentPath() is null), since that node is currentRoot.
        int startIdx = this.currentStack.size();
        TreePath curr = rootPath;
        while (curr != null && curr.getParentPath() != null) {
            this.currentStack.add(startIdx, curr.getLeaf());
            curr = curr.getParentPath();
        }

        try {
            // rootPath is an Iterable<Tree> that yields its leaf first, then each ancestor, then
            // the compilation unit; TreeScanner.scan(Iterable, P) visits them in that order. So
            // this scans rootPath's leaf subtree FIRST, then expands outward to ancestors'
            // subtrees, then the whole unit -- a nearest-first search, which is the locality win
            // described above.
            //
            // Do NOT narrow this to scan only the leaf subtree (e.g. super.scan(rootPath.getLeaf(),
            // target)). The outward expansion is what (a) finds targets that are not under the leaf
            // and (b) makes the foundPaths.put(target, null) below sound: without it, null would
            // mean only "absent from this subtree", and caching that would wrongly suppress a later
            // whole-unit search.
            super.scan(rootPath, target);
        } catch (Result result) {
            return result.path;
        } finally {
            this.currentRoot = prevRoot;
            while (this.currentStack.size() > prevStackSize) {
                this.currentStack.remove(this.currentStack.size() - 1);
            }
        }
        // The scan above covered the whole compilation unit, so the target is genuinely absent:
        // cache null so the unit is not re-searched for it. (Sound only because of the outward
        // expansion noted above.)
        foundPaths.put(target, null);
        return null;
    }

    /** The result of {@link #getPath}. This exception is used for control flow. */
    private static class Result extends Error {
        /** Unique identifier for serialization. */
        private static final long serialVersionUID = 4948452207518392627L;

        /** The result of {@link #getPath}. */
        @SuppressWarnings("serial") // I do not intend to serialize Result objects
        private final TreePath path;

        /**
         * Create a {@link #getPath} result.
         *
         * @param path the result of {@link #getPath}
         */
        Result(TreePath path) {
            // Disable stack trace writing and suppression: this exception is used purely for
            // control flow inside TreePathCacher#getPath, is caught two frames above, and is
            // never logged or rethrown.
            super(null, null, false, false);
            this.path = path;
        }
    }

    public void clear() {
        foundPaths.clear();
    }

    /**
     * Visit one tree as part of a search. Pushes {@code tree} onto {@link #currentStack} for the
     * duration of the visit and allocates no {@link TreePath} here; if {@code tree} is the target,
     * {@link #buildPathForStack} materializes the path and it is thrown out via {@link Result}.
     * This is the lazy counterpart of eagerly extending a {@code TreePath} field on every node.
     */
    @SuppressWarnings("interning:not.interned") // assertion
    @Override
    public TreePath scan(Tree tree, Tree target) {
        if (tree == null) {
            return null;
        }
        currentStack.add(tree);
        if (tree == target) {
            TreePath path = buildPathForStack();
            throw new Result(path);
        }
        try {
            return super.scan(tree, target);
        } finally {
            currentStack.remove(currentStack.size() - 1);
        }
    }

    /**
     * Materialize the {@link TreePath} for the leaf of {@link #currentStack}, building it from
     * {@link #currentRoot} down through each stacked node and caching every node's path in {@link
     * #foundPaths}. A node already in the cache is reused rather than re-allocated -- whether from
     * a shared prefix of an earlier lookup, or because {@link #getPath(TreePath, Tree)} seeded the
     * stack with a node that the scan then visits again. Reusing the cached entry both avoids the
     * duplicate allocation and collapses such a repeated node back onto its canonical path, so the
     * returned chain is correct regardless of seeding.
     *
     * @return the TreePath corresponding to the leaf of the stack
     */
    private TreePath buildPathForStack() {
        if (currentRoot == null) {
            throw new IllegalStateException("currentRoot is null");
        }
        TreePath lastPath = foundPaths.get(currentRoot);
        if (lastPath == null) {
            lastPath = new TreePath(currentRoot);
            foundPaths.put(currentRoot, lastPath);
        }
        for (int i = 0; i < currentStack.size(); ++i) {
            Tree node = currentStack.get(i);
            TreePath nodePath = foundPaths.get(node);
            if (nodePath == null) {
                nodePath = new TreePath(lastPath, node);
                foundPaths.put(node, nodePath);
            }
            lastPath = nodePath;
        }
        return lastPath;
    }
}
