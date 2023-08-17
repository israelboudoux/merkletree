package edu.boudoux;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MerkleTree {
    private final Node root;
    private final int totalLevels;
    private final Type type;

    private final static MessageDigest hash;

    static {
        try {
            hash = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public MerkleTree(int totalLevels, Type type) {
        assert totalLevels > 0;

        this.root = new Node();
        this.totalLevels = totalLevels; // allows for 2^[totalLevels] elements
        this.type = type;
    }

    public MerkleTree(int totalLevels) {
        this(totalLevels, Type.DENSE);
    }

    public int getTotalLevels() {
        return totalLevels;
    }

    public Node add(String value) {
        Node freeSlot;

        if (this.type == Type.DENSE) {
            freeSlot = this.getNextSlot(this.root);
        } else {
            freeSlot = this.getSlotByAddress(value);
        }

        freeSlot.setValue(value);

        return freeSlot;
    }

    private Node getSlotByAddress(String value) {
        long leafAddress = Long.parseLong(value, 16); // validates it is an hex string and parses to long

        String leafPath = this.getLeafPath(leafAddress);
        Node resultingNode = this.root;

        for (int i = 0; i < leafPath.length(); i++) {
            char bitAddress = leafPath.charAt(i);

            resultingNode = getNodeByBitAddress(resultingNode, bitAddress);
        }

        return resultingNode;
    }

    private Node getNodeByBitAddress(Node currentNode, char bitAddress) {
        if (bitAddress == '0') {
            if (currentNode.getLeftChildNode() == null) {
                return currentNode.addLeftChildNode();
            }

            return currentNode.getLeftChildNode();
        }

        // bitAddress == '1'
        if (currentNode.getRightChildNode() == null) {
            return currentNode.addRightChildNode();
        }

        return currentNode.getRightChildNode();
    }

    /**
     * Returns the leaf path in binary rep.
     *
     * @param leafAddress
     * @return
     */
    private String getLeafPath(long leafAddress) {
        return StringUtils.leftPad(Long.toBinaryString(leafAddress), this.getTotalLevels(), '0');
    }

    public String toString() {
        return this.root.toString();
    }

    private Node getNextSlot(Node rootNode) {
        if (rootNode.isFull()) {
            if (rootNode.getParent() == null) {
                throw new IllegalStateException("The tree is full");
            }

            return getNextSlot(rootNode.getParent());
        }

        if (rootNode.getLeftChildNode() == null) {
            rootNode.addLeftChildNode();
        }

        if (! rootNode.getLeftChildNode().isFull()) {
            if (rootNode.getLeftChildNode().isLeaf()) {
                return rootNode.getLeftChildNode();
            }

            return getNextSlot(rootNode.getLeftChildNode());
        }

        if (rootNode.getRightChildNode() == null) {
            rootNode.addRightChildNode();
        }

        if (rootNode.getRightChildNode().isLeaf()) {
            return rootNode.getRightChildNode();
        }

        return getNextSlot(rootNode.getRightChildNode());
    }

    public String getRootHash() {
        return this.root.getValue();
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String rebuildRoot(List<MerkleTree.Pair> proofList) {
        assert proofList.size() >= 2;

        MerkleTree.Pair pZero = proofList.get(0);
        MerkleTree.Pair pOne = proofList.get(1);

        String leftValue;
        String rightValue;
        if(pZero.getDirection() == MerkleTree.Direction.LEFT) {
            leftValue = pZero.getValue();
            rightValue = pOne.getValue();
        } else {
            rightValue = pZero.getValue();
            leftValue = pOne.getValue();
        }

        String currentHash = MerkleTree.bytesToHex(hash.digest((leftValue + rightValue).getBytes(StandardCharsets.UTF_8)));
        for (int i = 2; i < proofList.size(); i++) {
            MerkleTree.Pair nodeValue = proofList.get(i);

            if (nodeValue.getDirection() == MerkleTree.Direction.LEFT) {
                leftValue = nodeValue.getValue();
                rightValue = currentHash;
            } else {
                leftValue = currentHash;
                rightValue = nodeValue.getValue();
            }

            currentHash = MerkleTree.bytesToHex(hash.digest((leftValue + rightValue).getBytes(StandardCharsets.UTF_8)));
        }

        return currentHash;
    }

    public enum Type {
        DENSE,
        SPARSE;
    }

    public enum Direction {
        LEFT,
        RIGHT
    }

    public static class Pair {
        private final String value;
        private final Direction direction;

        private Pair(String value, Direction direction) {
            this.value = value;
            this.direction = direction;
        }

        public String getValue() {
            return value;
        }

        public Direction getDirection() {
            return direction;
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "value='" + value + '\'' +
                    ", direction=" + direction +
                    '}';
        }
    }

    public class Node {
        private Node parent;
        private final int level;
        private String value;
        private boolean leaf;
        private Direction direction;
        private Node leftChildNode;
        private Node rightChildNode;
        private boolean full;

        private Node() {
            this.level = 0;
        }

        private Node(Node parent, int level, boolean leaf, Direction direction) {
            assert level > 0;

            this.level = level;
            this.parent = parent;
            this.leaf = leaf;
            this.direction = direction;
        }

        public boolean isFull() {
            return this.full || (this.full = !this.isLeaf() && isNodeFull());
        }

        private boolean isNodeFull() {
            return this.getLeftChildNode() != null && this.getLeftChildNode().isFull()
                    && this.getRightChildNode() != null && this.getRightChildNode().isFull();
        }

        public Node getParent() {
            return parent;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            assert this.leaf && this.value == null;

            this.value = value;
            this.full = true;

            updateTree(this.parent);
        }

        public List<Pair> getProof() {
            assert this.isLeaf();

            List<Pair> proofList = new ArrayList<>();

            proofList.add(new Pair(this.value, this.direction));

            this.addMerkleProof(proofList);

            return proofList;
        }

        private void addMerkleProof(List<Pair> proofList) {
            Node siblingNode;
            Direction direction;
            if (this.direction == Direction.LEFT) {
                siblingNode = this.parent.getRightChildNode();
                direction = Direction.RIGHT;
            } else {
                siblingNode = this.parent.getLeftChildNode();
                direction = Direction.LEFT;
            }

            String value = siblingNode == null ? "0" : siblingNode.getValue();

            proofList.add(new Pair(value, direction));

            if (this.parent.level == 0) // ignore for root node
                return;

            this.parent.addMerkleProof(proofList);
        }

        private void updateTree(Node rootNode) {
            assert !this.isLeaf() && rootNode.getLeftChildNode() != null && rootNode.getLeftChildNode().getValue() != null;

            String leftNodeValue = rootNode.getLeftChildNode() != null && rootNode.getLeftChildNode().getValue() != null ? rootNode.getLeftChildNode().getValue() : "0";
            String rightNodeValue = rootNode.getRightChildNode() != null && rootNode.getRightChildNode().getValue() != null ? rootNode.getRightChildNode().getValue() : "0";

            rootNode.value = bytesToHex(hash.digest((leftNodeValue + rightNodeValue).getBytes(StandardCharsets.UTF_8)));
            if (rootNode.getParent() != null) {
                updateTree(rootNode.getParent());
            }
        }

        public boolean isLeaf() {
            return leaf;
        }

        public Node getLeftChildNode() {
            return leftChildNode;
        }

        public Node addLeftChildNode() {
            assert !this.leaf && this.leftChildNode == null;

            this.leftChildNode = new Node(this, this.level + 1, this.level + 1 == MerkleTree.this.getTotalLevels(), Direction.LEFT);

            return this.leftChildNode;
        }

        public Node getRightChildNode() {
            return rightChildNode;
        }

        public Node addRightChildNode() {
            assert !this.leaf && this.rightChildNode == null;

            this.rightChildNode = new Node(this, this.level + 1, this.level + 1 == MerkleTree.this.getTotalLevels(), Direction.RIGHT);

            return this.rightChildNode;
        }

        // TODO
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("\t") // this is added only for non-leaf nodes???
                    .append("[")
                    .append(this.value == null ? "0" : this.value.substring(0, Math.min(this.value.length(), 6)))
                    .append("]");

            if(! this.isLeaf()) {
                sb.append(toStringLeftNode())
                        .append("\t")
                        .append(toStringRightNode());
            }

            return sb.toString();
        }

        private String toStringLeftNode() {
            if (this.getLeftChildNode() != null)
                return this.getLeftChildNode().toString();

            return "";
        }

        private String toStringRightNode() {
            if (this.getRightChildNode() != null)
                return this.getRightChildNode().toString();

            return "";
        }
    }
}
