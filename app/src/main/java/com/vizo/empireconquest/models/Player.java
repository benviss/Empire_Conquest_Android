package com.vizo.empireconquest.models;

import java.util.ArrayList;

/**
 * Created by Guest on 12/21/16.
 */

//TODO Google Play Services contains player abstract class, implement later
public class Player {
    public ArrayList<Territory> territories;
    private int reinforcements;
    public String name;
    private String playerId;
    private boolean turnReady = false;
    private String playerNumber;

    public Player(String name, String playerId, int playerNumber) {
        this.playerNumber = "player" + playerNumber;
        this.playerId = playerId;
        this.name = name;
        territories = new ArrayList<>();
    }

    public ArrayList<Territory> getTerritories() {
        return territories;
    }

    public void newTerritory(Territory territory) {
        this.territories.add(territory);
    }

    public int getReinforcements() {
        return reinforcements;
    }

    public void setReinforcements(int reinforcements) {
        this.reinforcements = reinforcements;
    }

    public String getName() {
        return name;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNumber() {
        return playerNumber;
    }


}
