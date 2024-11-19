package mekhq.campaign.autoResolve.scenarioResolver.components;

import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;

import java.util.List;
import java.util.Map;

public class AutoResolveResult {

    private final boolean team1Victory;
    private final boolean controlTheBattlefield;
    private final Scenario scenario;

    private final Map<Integer, List<UnitStrength>> survivingUnits;
    private final Map<Integer, List<UnitStrength>> defeatedUnits;

    public AutoResolveResult(boolean team1Victory, boolean controlTheBattlefield, AtBScenario scenario, Map<Integer, List<UnitStrength>> survivingUnits,
                             Map<Integer, List<UnitStrength>> defeatedUnits) {
        this.team1Victory = team1Victory;
        this.controlTheBattlefield = controlTheBattlefield;
        this.survivingUnits = survivingUnits;
        this.defeatedUnits = defeatedUnits;
        this.scenario = scenario;
    }

    public boolean controlTheBattlefieldAfterBattle() {
        return controlTheBattlefield;
    }

    public boolean isTeam1Victory() {
        return team1Victory;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public Map<Integer, List<UnitStrength>> getSurvivingUnits() {
        return survivingUnits;
    }

    public Map<Integer, List<UnitStrength>> getDefeatedUnits() {
        return defeatedUnits;
    }
}
