package org.boudoux;

public class TestMerkleTree {

    public static void main(String[] args) {
        for (int totalLevels = 1; totalLevels <= 4; totalLevels++) {
            MerkleTree merkleTree = new MerkleTree(totalLevels);

            addElements(merkleTree, totalLevels);

            System.out.println(merkleTree + "\n");
        }
    }

    private static void addElements(MerkleTree merkleTree, long totalLevels) {
        long totalElements = (long) Math.pow(2, totalLevels);

        for (int i = 1; i <= totalElements; i++) {
            MerkleTree.Node node = merkleTree.add(String.valueOf(i));
            System.out.println(node.getProof() + " - Root: " + merkleTree.getRoot() + " - Matches: " + MerkleTree.rebuildRoot(node.getProof()).equals(merkleTree.getRoot()));
        }
    }
}

