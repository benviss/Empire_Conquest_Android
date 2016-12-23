package com.vizo.empireconquest.models;



/**
 * Created by Guest on 12/21/16.
 */
public class Territory {
    private String name;
    private int troops;
    private Player playerOwned;
    private int index;


    public Territory(String name, Player playerOwned, int index) {
        this.name = name;
        this.troops = 1;
        this.playerOwned = playerOwned;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTroops() {
        return troops;
    }

    public void setTroops(int troops) {
        this.troops = troops;
    }

    public Player getPlayerOwned() {
        return playerOwned;
    }

    public void setPlayerOwned(Player playerOwned) {
        this.playerOwned = playerOwned;
    }

    public int getIndex() {
        return index;
    }

    public void addReinforcements() {
        troops += playerOwned.getReinforcements();
    }

    public static void territoryBattle(Territory one, Territory two) {
        if (one.getTroops() > two.getTroops()) {
            two.setPlayerOwned(one.getPlayerOwned());
            one.setTroops(one.getTroops() / 2);
            two.setTroops(one.getTroops() / 2);
        } else {
            one.setPlayerOwned(two.getPlayerOwned());
            two.setTroops(two.getTroops() / 2);
            one.setTroops(two.getTroops() / 2);
        }
    }
}
