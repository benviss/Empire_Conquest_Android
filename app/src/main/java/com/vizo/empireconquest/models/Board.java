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
        for (int i = 0; i < players.size(); i++) {
            Node newNode = new Node(octagons.get(i), "Octagon", 5);
            newNode.setPlayerOwned(players.get(i));
            nodes.add(newNode);
            octagons.remove(i);
        }
        for (int i = 0; i < octagons.size(); i++) {
            Node newNode = new Node(octagons.get(i), "Octagon", 5);
            nodes.add(newNode);
        }
        for (int i = 0; i < diamonds.size(); i++) {
            Node newNode = new Node(diamonds.get(i), "Diamond", 1);
            nodes.add(newNode);
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
    public ArrayList<Node> getNodes() {
        return nodes;
    }

}
