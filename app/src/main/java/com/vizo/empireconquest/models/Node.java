package com.vizo.empireconquest.models;

/**
 * Created by Ben Vissotzky on 3/8/2017.
 */

public class Node {
    private int id;
    private String type;
    private int value;
    private Player playerOwned;


    public Node(int id, String type, int value) {
        this.id = id;
        this.type = type;
        this.value = value;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Player getPlayerOwned() {
        return playerOwned;
    }

    public void setPlayerOwned(Player playerOwned) {
        this.playerOwned = playerOwned;
    }
}
