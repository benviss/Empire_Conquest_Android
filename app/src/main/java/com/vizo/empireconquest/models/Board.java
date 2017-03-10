package com.vizo.empireconquest.models;

import com.vizo.empireconquest.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Guest on 12/19/16.
 */
public class Board {
    private ArrayList<Node> nodes = new ArrayList<>();

    private ArrayList<Integer> diamonds = new ArrayList<>(Arrays.asList(R.id.diamond1, R.id.diamond2,R.id.diamond3,R.id.diamond4,R.id.diamond5,R.id.diamond6,R.id.diamond7,R.id.diamond8,R.id.diamond9,R.id.diamond10,R.id.diamond11,R.id.diamond12,R.id.diamond13,R.id.diamond14,R.id.diamond15,R.id.diamond16,R.id.diamond17,R.id.diamond18,R.id.diamond19,R.id.diamond20, R.id.diamond21));
    private ArrayList<Integer> octagons = new ArrayList<>(Arrays.asList(R.id.octagon1,R.id.octagon3,R.id.octagon4,R.id.octagon2));


    public void assignNodes(ArrayList<Player> players) {
        int index = 0;
        for (int i = 0; i < players.size(); i++) {
            Node newNode = new Node(octagons.get(i), "Octagon", 5, index);
            newNode.setPlayerOwned(players.get(i));
            newNode.setIncrement(3);
            nodes.add(newNode);
            index++;
            octagons.remove(i);
        }
        for (int i = 0; i < octagons.size(); i++) {
            Node newNode = new Node(octagons.get(i), "Octagon", 5, index);
            newNode.setIncrement(3);
            nodes.add(newNode);
            index++;
        }
        for (int i = 0; i < diamonds.size(); i++) {
            Node newNode = new Node(diamonds.get(i), "Diamond", 1, index);
            newNode.setIncrement(1);
            nodes.add(newNode);
            index++;
        }
    }

    public boolean canNodeAttack(Node sourceNode, Node targetNode) {
        int[] sourceCombos = nodeCombos[sourceNode.getIndex()];
        for (int i : sourceCombos) {
            if (i - 1 == targetNode.getIndex()) {
                return true;
            }
        }
        return false;
    }
    public void nodeAttack(Node sourceNode, Node targetNode) {
        if (sourceNode.getPlayerOwned() == targetNode.getPlayerOwned()) {
            targetNode.setValue(targetNode.getValue() + sourceNode.getValue() - 1);
            sourceNode.setValue(1);
        } else {
            int targetInt = targetNode.getValue();
            int sourceInt = sourceNode.getValue() - 1;
            if (sourceInt == targetInt) {
                return;
            }
            targetNode.setValue(targetInt - sourceInt);
            if (targetNode.getValue() <= 0) {
                targetNode.setPlayerOwned(sourceNode.getPlayerOwned());
                targetNode.setValue(sourceInt - targetInt);
            }
            sourceNode.setValue(1);
        }
    }

    public Node findByID(int id) {
        for (Node n : nodes) {
            if (n.getId() == id) {
                return n;
            }
        }
        return null;
    }

    public Node findByIndex(int index) {
        for (Node n : nodes) {
            if (n.getIndex() == index) {
                return n;
            }
        }
        return null;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public boolean playerDead(Player player) {
        for (Node n : nodes) {
            if (n.getPlayerOwned() == player) {
                return false;
            }
        }
        return true;
    }

    private int[][] nodeCombos = {{5},{25},{23},{7},{8,6,9,1},{5,8,9,7,10},{6,9,10,4},{5,6,9,12,11},{5,8,11,12,13,10,7,6},{7,6,9,12,13},{8,9,12,15,14},{8,9,10,11,13,14,15,16},{9,10,12,15,16},{11,12,15,17,18},{11,12,13,14,16,17,18,19},{12,13,15,18,19},{14,15,18,20,21},{14,15,16,17,19,20,21,22},{15,16,18,21,22},{17,18,21,23,24},{17,18,19,20,22,23,24,25},{18,19,21,24,25},{20,21,24,3},{20,21,22,23,25},{21,22,24,2}};

}
