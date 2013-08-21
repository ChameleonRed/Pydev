package org.python.pydev.shared_core.structure;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class TreeNodeTest {

    @Test
    public void test() {
        TreeNode<Integer> root = new TreeNode<Integer>(null, 0);
        TreeNode<Integer> c1 = new TreeNode<Integer>(root, 1);
        TreeNode<Integer> c2 = new TreeNode<Integer>(c1, 2);
        TreeNode<Integer> c3 = new TreeNode<Integer>(c1, 3);
        List<TreeNode<Integer>> flattened = root.flatten();
        assertEquals(flattened.size(), 3);
    }

}
