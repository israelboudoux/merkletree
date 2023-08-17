package org.boudoux;

public class TestMerkleTree {

    public static void main(String[] args) {
        System.out.println("Testing DENSE MT");

        // Testing DENSE MT
        for (int totalLevels = 1; totalLevels <= 4; totalLevels++) {
            MerkleTree merkleTree = new MerkleTree(totalLevels);

            addElements(merkleTree, totalLevels);

            System.out.println(merkleTree + "\n");
        }

        System.out.println("Testing SPARSE MT");

        // Testing SPARSE MT
        for (int totalLevels = 1; totalLevels <= 4; totalLevels++) {
            MerkleTree merkleTree = new MerkleTree(totalLevels, MerkleTree.Type.SPARSE);

            addElements(merkleTree, totalLevels);

            System.out.println(merkleTree + "\n");
        }
    }

    private static void addElements(MerkleTree merkleTree, int totalLevels) {
        long totalElements = (long) Math.pow(2, totalLevels);

        for (long i = totalElements - 1; i >= 0; i--) {
            MerkleTree.Node node = merkleTree.add(Long.toHexString(i));
            System.out.println(node.getProof() + " - Root: " + merkleTree.getRootHash() + " - Matches: " + MerkleTree.rebuildRoot(node.getProof()).equals(merkleTree.getRootHash()));
        }
    }
}
