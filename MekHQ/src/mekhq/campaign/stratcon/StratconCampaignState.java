package mekhq.campaign.stratcon;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;

public class StratconCampaignState {
    private HashMap<Integer, HashMap<Integer, StratconScenario>> scenarios;
    private List<Integer> missionIDs;
    private Campaign campaign;

    // these are all state variables that affect the current Stratcon Campaign
    private double globalOpforBVMultiplier;
    private int supportPoints;
    private int victoryPoints;
    private int strategicObjectivePoints;
    private List<Integer> scenarioOddsByTrack;
    private List<List<Integer>> scenarioOddsModifiers;

    public StratconCampaignState(Campaign campaign) {
        scenarios = new HashMap<>();
        this.campaign = campaign; 

        globalOpforBVMultiplier = 0.05;
        supportPoints = 5;
        victoryPoints = 2;
        strategicObjectivePoints = 1;
        scenarioOddsByTrack = Arrays.asList(20, 40, 80, 40);

        addNewScenario(0, 0);
        addNewScenario(1, 2);
        addNewScenario(1, 3);
        addNewScenario(7, 2);

        getScenario(0, 0).setCurrentState(ScenarioState.UNRESOLVED);
        getScenario(1, 2).setCurrentState(ScenarioState.DEFEATED);
        getScenario(1, 3).setCurrentState(ScenarioState.COMPLETED);
        getScenario(7, 2).setCurrentState(ScenarioState.IGNORED);
    }

    public int getWidth() {
        return 12;
    }

    public int getHeight() {
        return 4;
    }

    public Color getHexColor(int x, int y) {
        return Color.GRAY;
    }

    public void addNewScenario(int x, int y) {
        if(!scenarios.containsKey(x)) {
            scenarios.put(x, new HashMap<>());
        }

        StratconScenario sts = new StratconScenario();
        sts.initializeScenario(campaign);
        scenarios.get(x).put(y, sts);
    }

    public StratconScenario getScenario(int x, int y) {
        if(!scenarios.containsKey(x) || !scenarios.get(x).containsKey(y)) {
            return null;
        }

        return scenarios.get(x).get(y);
    }

    public List<Integer> getMissionIDs() {
        return missionIDs;
    }

    /**
     * The opfor BV multiplier. Intended to be additive.
     * @return The additive opfor BV multiplier.
     */
    public double getGlobalOpforBVMultiplier() {
        return globalOpforBVMultiplier;
    }
}