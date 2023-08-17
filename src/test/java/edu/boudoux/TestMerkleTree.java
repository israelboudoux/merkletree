package edu.boudoux;

import org.junit.Test;

public class TestMerkleTree {
    @Test
    public void testInsertionIntoDenseMerkleTree() {
        for (int totalLevels = 1; totalLevels <= 4; totalLevels++) {
            MerkleTree merkleTree = new MerkleTree(totalLevels);

            addElements(merkleTree, totalLevels);

            System.out.println(merkleTree);
        }
    }

    @Test
    public void testInsertionIntoSparseMerkleTree() {
        for (int totalLevels = 1; totalLevels <= 4; totalLevels++) {
            MerkleTree merkleTree = new MerkleTree(totalLevels, MerkleTree.Type.SPARSE);

            addElements(merkleTree, totalLevels);

            System.out.println(merkleTree + "\n");
        }
    }

    private void addElements(MerkleTree merkleTree, int totalLevels) {
        long totalElements = (long) Math.pow(2, totalLevels);

        for (long i = totalElements - 1; i >= 0; i--) {
            MerkleTree.Node node = merkleTree.add(Long.toHexString(i));
            System.out.println(node.getProof() + " - Root: " + merkleTree.getRootHash() + " - Matches: " + MerkleTree.rebuildRoot(node.getProof()).equals(merkleTree.getRootHash()));
        }
    }
}
