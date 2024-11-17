package mekhq.campaign.autoResolve.scenarioResolver.unitsMatter;

import megamek.common.*;
import mekhq.campaign.autoResolve.damageHandler.DamageHandlerChooser;
import mekhq.campaign.autoResolve.helper.UnitStrengthHelper;
import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ScenarioObjective;

import java.util.List;
import java.util.Map;
import java.util.*;

public class UnitsMatterSimpleScenarioResolver extends ScenarioResolver {
    private static final int DEFEATS_FOR_UNIT_DESTRUCTION = 100;

    private final Map<Integer, List<UnitResult>> combatRoundResults = new HashMap<>();
    private final Map<Integer, List<UnitStrength>> survivingUnits = new HashMap<>();
    private final Map<Integer, List<UnitStrength>> roundDefeatedUnits = new HashMap<>();
    private final Map<Integer, List<UnitStrength>> roundCapturedUnits = new HashMap<>();
    private final Map<Integer, List<UnitStrength>> defeatedUnits = new HashMap<>();
    private final Map<UnitStrength, Integer> unitLossCount = new HashMap<>();
    private final Random random = new Random();
    private final Map<Integer, String> forcesNames = new HashMap<>();
    private final List<String> forceMustBePreserved = new ArrayList<>();

    public UnitsMatterSimpleScenarioResolver(AtBScenario scenario) {
        super(scenario);
    }

    private void initializeState(Map<Integer, List<AutoResolveForce>> teams) {
        combatRoundResults.clear();
        survivingUnits.clear();
        roundDefeatedUnits.clear();
        roundCapturedUnits.clear();
        forcesNames.clear();
        defeatedUnits.clear();
        unitLossCount.clear();
        forceMustBePreserved.clear();
        scenario.getScenarioObjectives().forEach(objective -> {
            if (objective.getObjectiveCriterion().equals(ScenarioObjective.ObjectiveCriterion.Preserve)) {
                forceMustBePreserved.addAll(objective.getAssociatedForceNames());
            }
        });
        teams.forEach((teamId, forces) -> {
            survivingUnits.put(teamId, new ArrayList<>());
            forces.stream()
                .peek(force -> forcesNames.put(force.id(), force.forceName()))
                .flatMap(force -> force.units().stream())
                .forEach(unit -> {
                    survivingUnits.get(teamId).add(UnitStrengthHelper.getUnitStrength(unit));
                    unitLossCount.put(UnitStrengthHelper.getUnitStrength(unit), 0);
                });
        });
    }

    @Override
    public AutoResolveConcludedEvent resolveScenario(Map<Integer, List<AutoResolveForce>> teams) {
        initializeState(teams);

        while (true) {
            // Clear previous combat round results
            simpleCombatResolution();
            simplePostCombatResolution();
            printRoundResult();

            var numberOfSurvivingTeams = getNumberOfSurvivingTeams();
            if (numberOfSurvivingTeams <= 1) {
                break;
            }
        }

        return new AutoResolveConcludedEvent(
            isTeam1Victory(),
            defeatedUnits.values().stream().flatMap(List::stream).map(UnitStrength::entity).toList(),
            survivingUnits.values().stream().flatMap(List::stream).map(UnitStrength::entity).toList()
        );
    }

    protected boolean isTeam1Victory() {
        var team1Victory = survivingUnits.entrySet().stream().anyMatch(e -> e.getKey() == 1);
        return team1Victory;
    }

    protected long getNumberOfSurvivingTeams() {
        var numberOfTeamsWithSurvivingUnits = survivingUnits.entrySet().stream().filter(e -> !e.getValue().isEmpty()).count();
        return numberOfTeamsWithSurvivingUnits;
    }

    protected void printRoundResult() {
        printUnitsByCategory("Surviving Units:", survivingUnits);
        printUnitsByCategory("Defeated Units:", defeatedUnits);
    }

    private void printUnitsByCategory(String category, Map<Integer, List<UnitStrength>> units) {
        System.out.println("\n" + category);
        units.forEach(UnitsMatterSimpleScenarioResolver::printTeamUnits);
    }

    private static void printTeamUnits(int teamID, List<UnitStrength> teamUnits) {
        System.out.println("\tTeam " + teamID + ":");
        for (var unit : teamUnits) {
            System.out.println("\t\t- " + unit.entity().getDisplayName());
        }
    }

    private void simpleCombatResolution() {
        combatRoundResults.clear();
        // Roll dice for all units in each team
        survivingUnits.forEach((teamId, units) ->
            units.forEach(unit ->
                combatRoundResults.computeIfAbsent(teamId, k -> new ArrayList<>()).add(new UnitResult(unit, unit.diceRoll()))
            )
        );

        // Sort units by their dice result in descending order
        combatRoundResults.values().forEach(unitResults -> unitResults.sort(Collections.reverseOrder()));


        while (true) {
            List<Integer> teamIds = new ArrayList<>(combatRoundResults.keySet());
            // Stop pairing dices if condition is true
            if (lessThanTwoTeamsLeft(teamIds)) break;

            SelectedUnit firstUnit = getUnitFromTeam(teamIds);
            SelectedUnit secondUnit = getUnitFromTeam(teamIds);
            compareUnits(firstUnit, secondUnit);
        }
    }

    private void compareUnits(SelectedUnit firstUnit, SelectedUnit secondUnit) {
        var comparisonResult = firstUnit.unit().compareTo(secondUnit.unit());
        if (comparisonResult > 0) {
            handleUnitDefeat(secondUnit, firstUnit);
        } else if (comparisonResult < 0) {
            handleUnitDefeat(firstUnit, secondUnit);
        }
    }

    private void handleUnitDefeat(SelectedUnit defeated, SelectedUnit winner) {
        if (forceMustBePreserved.contains(forcesNames.get(defeated.unit().unitStrength().entity().getForceId())) && winner.teamId() == 1) {
            roundCapturedUnits.computeIfAbsent(defeated.teamId(), k -> new ArrayList<>()).add(defeated.unit().unitStrength());
        } else {
            unitLostRollOff(defeated.unit(), defeated.teamId(), winner.unit());
            System.out.printf(">>> [%d]%s is hit by [%d]%s: [%d] %d < %d [%d]%n",
                defeated.teamId(),
                defeated.unit().unitStrength().entity().getDisplayName(),
                winner.teamId(),
                winner.unit().unitStrength().entity().getDisplayName(),
                defeated.teamId(),
                defeated.unit().diceResult(),
                winner.unit().diceResult(),
                winner.teamId());
        }
    }

    /**
     * Get a unit from a team and removes that team from the pick list
     * this is a shortcut to avoid having to shuffle the list
     *
     * @param teamIds   List of team ids
     * @return SelectedUnit
     */
    private SelectedUnit getUnitFromTeam(List<Integer> teamIds) {
        int teamId = teamIds.remove(random.nextInt(teamIds.size()));
        List<UnitResult> teamUnits = combatRoundResults.get(teamId);
        UnitResult unit = teamUnits.remove(0);
        return new SelectedUnit(teamId, unit);
    }

    private record SelectedUnit(int teamId, UnitResult unit) {
    }

    private boolean lessThanTwoTeamsLeft(List<Integer> teamIds) {
        teamIds.removeIf(id -> combatRoundResults.get(id).isEmpty());
        return teamIds.size() < 2;
    }

    private void unitLostRollOff(UnitResult unit, int teamId, UnitResult aggressor) {
        unitLossCount.put(unit.unitStrength(), Compute.computeTotalDamage(aggressor.unitStrength.entity().getTotalWeaponList()));
        roundDefeatedUnits.computeIfAbsent(teamId, k -> new ArrayList<>()).add(unit.unitStrength());
    }

    private void simplePostCombatResolution() {
        for (var entry : roundDefeatedUnits.entrySet()) {
            int teamId = entry.getKey();
            List<UnitStrength> defeatedList = entry.getValue();
//            int unitsToRemove = defeatedList.size() % DEFEATS_FOR_UNIT_DESTRUCTION;
            for (UnitStrength unitHit : defeatedList) {
                applyDamageToUnit(teamId, unitHit);
                if (unitHit.entity().isCrippled() && !unitHit.entity().isDestroyed()) {
                    retreatUnit(teamId, unitHit);
                }
            }
        }
        roundDefeatedUnits.clear();

        for (var entry : roundCapturedUnits.entrySet()) {
            int teamId = entry.getKey();
            List<UnitStrength> capturedList = entry.getValue();
            for (UnitStrength unitHit : capturedList) {
                unitDefeated(teamId, unitHit);
                unitHit.entity().setRemovalCondition(IEntityRemovalConditions.REMOVE_CAPTURED);
                System.out.println("<<<<<< Unit " + unitHit.entity().getDisplayName() + " has been captured.");
            }
        }
        roundCapturedUnits.clear();
    }

    private static final int[] weightedRemovalConditions = {
        IEntityRemovalConditions.REMOVE_DEVASTATED,
        IEntityRemovalConditions.REMOVE_EJECTED,
        IEntityRemovalConditions.REMOVE_EJECTED,
        IEntityRemovalConditions.REMOVE_SALVAGEABLE,
        IEntityRemovalConditions.REMOVE_SALVAGEABLE,
        IEntityRemovalConditions.REMOVE_SALVAGEABLE,
    };

    private void unitDestroyed(int teamId, UnitStrength unitToRemove) {
        unitDefeated(teamId, unitToRemove);
        var randomlySelectedRemovalCondition = weightedRemovalConditions[random.nextInt(weightedRemovalConditions.length-1)];
        unitToRemove.entity().setRemovalCondition(randomlySelectedRemovalCondition);
        unitToRemove.entity().setSalvage(randomlySelectedRemovalCondition != IEntityRemovalConditions.REMOVE_DEVASTATED);
        System.out.println("XXXXXXX Unit " + unitToRemove.entity().getDisplayName() + " has been destroyed.");
    }

    private void retreatUnit(int teamId, UnitStrength unitToRemove) {
        unitDefeated(teamId, unitToRemove);
        unitToRemove.entity().setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
        System.out.println("<<<<<<< Unit " + unitToRemove.entity().getDisplayName() + " has retreated.");
    }

    private void applyDamageToUnit(int teamId, UnitStrength unitToRemove) {
        var entity = unitToRemove.entity();
        DamageHandlerChooser.chooseHandler(entity).applyDamage(unitLossCount.getOrDefault(unitToRemove, 5));
        System.out.println("@@@@@@@ Unit " + entity.getDisplayName() + " has taken damage.");
        if (entity.isDestroyed()) {
            unitDestroyed(teamId, unitToRemove);
        }
    }

    private void unitDefeated(int teamId, UnitStrength unitToRemove) {
        survivingUnits.get(teamId).remove(unitToRemove);
        defeatedUnits.computeIfAbsent(teamId, k -> new ArrayList<>()).add(unitToRemove);
    }
}
