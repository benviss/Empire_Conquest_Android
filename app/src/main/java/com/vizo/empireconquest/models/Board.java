package com.vizo.empireconquest.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by Guest on 12/19/16.
 */
public class Board {
    private ArrayList<Territory> territories = new ArrayList<>();
//    private ArrayList<String> unmappedTerritories = new ArrayList<>(Arrays.asList("Alaska", "Northwest Territory", "Greenland", "Alberta", "Ontario", "Western United States", "Eastern United States", "Central America"));
    private ArrayList<String> unmappedTerritories = new ArrayList<>(Arrays.asList("Alaska", "Northwest Territory", "Greenland", "Alberta", "Ontario","Quebec" ,"Western United States", "Eastern United States", "Central America", "Venezuela","Brazil", "Peru", "Argentina", "South Africa", "Madagascar", "Congo", "East Africa", "Egypt", "North Africa", "Western Europe","Southern Europe", "Northern Europe", "Great Britain", "Ukraine", "Scandinavia", "Iceland", "Middle East", "Afghanistan", "Ural","Siberia", "India", "China", "Mongolia", "Irkutsk", "Yakutsk", "Kamchatka", "Japan", "Siam", "Indonesia","New Guinea", "Western Australia", "Eastern Australia"));
    private ArrayList<String> territoriesIndexed = new ArrayList<>(Arrays.asList("Alaska", "Northwest Territory", "Greenland", "Alberta", "Ontario","Quebec" ,"Western United States", "Eastern United States", "Central America", "Venezuela","Brazil", "Peru", "Argentina", "South Africa", "Madagascar", "Congo", "East Africa", "Egypt", "North Africa", "Western Europe","Southern Europe", "Northern Europe", "Great Britain", "Ukraine", "Scandinavia", "Iceland", "Middle East", "Afghanistan", "Ural","Siberia", "India", "China", "Mongolia", "Irkutsk", "Yakutsk", "Kamchatka", "Japan", "Siam", "Indonesia","New Guinea", "Western Australia", "Eastern Australia"));


    public void assignTerritories(ArrayList<Player> players) {
        //Increments through available players
        int playerIndex = 0;
        int length = unmappedTerritories.size();
        Random randomGenerator = new Random();
        for (int i = 0; i < length; i++) {

            //Resets to 0 to stay within players Array bounds
            if (playerIndex == players.size()) playerIndex = 0;
            int randomInt = randomGenerator.nextInt(unmappedTerritories.size());

            //assign player and increment playerIndex to complete player assignment randomization
            Territory newTerritory = new Territory(unmappedTerritories.get(randomInt), players.get(playerIndex), randomInt);
            players.get(playerIndex).newTerritory(newTerritory);
            playerIndex++;

            territories.add(newTerritory);
            unmappedTerritories.remove(randomInt);
        }

    }

    public ArrayList<Territory> getTerritories() {
        return territories;
    }
    public ArrayList<String> getIndexedTerritories() {
        return territoriesIndexed;
    }
}
