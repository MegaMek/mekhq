package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import mekhq.campaign.autoResolve.damageHandler.DamageHandlerChooser;
import mekhq.campaign.autoResolve.helper.UnitStrengthHelper;
import mekhq.campaign.autoResolve.scenarioResolver.ScenarioResolver;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.AutoResolveConcludedEvent;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.AutoResolveForce;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.UnitResult;
import mekhq.campaign.autoResolve.scenarioResolver.unitsMatter.UnitStrength;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ScenarioObjective;

import java.util.*;

public class AcsSimpleScenarioResolver extends ScenarioResolver {

    private static final int MAX_ROUNDS = 100;

    private final Map<TeamID, List<UnitResult>> combatRoundResults = new HashMap<>();
    private final Map<TeamID, List<UnitStrength>> survivingUnits = new HashMap<>();
    private final Map<TeamID, List<UnitStrength>> roundDefeatedUnits = new HashMap<>();
    private final Map<TeamID, List<UnitStrength>> roundCapturedUnits = new HashMap<>();
    private final Map<TeamID, List<UnitStrength>> defeatedUnits = new HashMap<>();
    private final Map<UnitStrength, Integer> unitLossCount = new HashMap<>();
    private final Random random = new Random();

    private final Map<ForceID, String> forcesNames = new HashMap<>();
    private final List<String> forceMustBePreserved = new ArrayList<>();

    private final Map<TeamID, List<ForceID>> forcesPerTeam = new HashMap<>();
    private final Map<ForceID, HashSet<EffectOrSituation>> effectsOrSituationByForce = new HashMap<>();

    public AcsSimpleScenarioResolver(AtBScenario scenario) {
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
            var teamID = new TeamID(teamId);
            survivingUnits.put(teamID, new ArrayList<>());
            forces.stream()
                .flatMap(force -> {
                    forcesNames.put(new ForceID(force.id()), force.forceName());
                    return force.units().stream();
                })
                .forEach(unit -> {
                    survivingUnits.get(teamID).add(UnitStrengthHelper.getUnitStrength(unit));
                    unitLossCount.put(UnitStrengthHelper.getUnitStrength(unit), 0);
                });
        });
    }

    @Override
    public AutoResolveConcludedEvent resolveScenario(Map<Integer, List<AutoResolveForce>> teams) {
        initializeState(teams);
        var round = 0;
        while (true) {

            // initiativePhase();
            // deploymentPhase();
            // detectionAndReconPhase();
            // movementPhase();
            // combatPhase();
            // endPhase();

            simpleCombatResolution();
            simplePostCombatResolution();
            printRoundResult();
            var numberOfSurvivingTeams = getNumberOfSurvivingTeams();
            if (numberOfSurvivingTeams <= 1 || round >= MAX_ROUNDS) {
                break;
            }

            round++;
        }

        return new AutoResolveConcludedEvent(
            isTeam1Victory(),
            defeatedUnits.values().stream().flatMap(List::stream).map(UnitStrength::entity).toList(),
            survivingUnits.values().stream().flatMap(List::stream).map(UnitStrength::entity).toList()
        );
    }

    /**
     * Currently only forces and teams have effects applied to them.
     * @param unitId of the UNIT
     * @return empty list
     */
    private Set<EffectOrSituation> getUnitEffectOrSituation(UnitID unitId) {
        return Set.of();
    }

    private Set<EffectOrSituation> getForceEffectOrSituation(ForceID forceId) {
        return effectsOrSituationByForce.getOrDefault(forceId, new HashSet<>());
    }

    private Set<EffectOrSituation> getTeamEffectOrSituation(TeamID teamId) {
        var output = new HashSet<EffectOrSituation>();
        for (var forceId : forcesPerTeam.get(teamId)) {
            var forceModifiers = getForceEffectOrSituation(forceId);
            output.addAll(forceModifiers);
        }
        return output;
    }

    private Set<EffectOrSituation> getEffectOrSituation(ID id) {
        if (id instanceof ForceID forceID) {
            return getForceEffectOrSituation(forceID);
        } else if (id instanceof TeamID teamID) {
            return getTeamEffectOrSituation(teamID);
        } else if (id instanceof UnitID unitID) {
            return getUnitEffectOrSituation(unitID);
        }
        return Set.of();
    }

    private int getTargetMovementModifier(UnitID id) {
        // MANUALLY IMPLEMENT THIS
        return 0;
    }


    private int getModifier(ID id, EosType effectOrSituation, Modifier modifier) {
        var modStream = getEffectOrSituation(id).stream().filter(e -> e.type.equals(effectOrSituation));
        int totalSum = modStream.filter(e -> !e.hasSpecialValue())
            .map(
            e -> e.getModifier(modifier)
            ).reduce(0, Integer::sum);

        if (effectOrSituation.equals(EosType.COMBAT_UNIT) &&
            hasEffect(id, EffectOrSituation.TARGET_MOVEMENT_MODIFIER) &&
            id instanceof UnitID) {
            var tmm = getTargetMovementModifier((UnitID) id);
            totalSum += tmm;
        }

        return totalSum;
    }

    private void addEffect(ID id, EffectOrSituation effectOrSituation) {
        getEffectOrSituation(id).add(effectOrSituation);
    }

    private boolean hasEffect(ID id, EffectOrSituation effectOrSituation) {
        return getEffectOrSituation(id).stream().anyMatch(e -> e.equals(effectOrSituation));
    }

    private void initiativePhase() {
        // Each player rolls 2D6 and adds the results together to determine Initiative; re-roll ties. The player with the higher
        // result wins the Initiative for that turn.

    }

    protected void deploymentPhase() {
        // n the first turn of the game and any turn where new Combat Units arrive on the battlefield (SSRM or PCM) players must deploy
        // their forces. The player with the lowest initiative places a Formation following the Deployment Phase rules and then the winner
        // of initiative places a Formation on the map. This repeats until all Formations are deployed.
        //If there are no new forces arriving in the turn, this step is skipped and play moves immediately to
        // Step 3: Detection and Reconnaissance Phase.
    }

    protected void detectionAndReconPhase() {
        // In this phase newly detected Blips are placed on the appropriate map and each side conducts recon to reveal information about
        // hostile Blip Counters on the battlefield. The player who won initiative goes first when conducting recon.
    }

    protected void movementPhase() {
        // The player with the lowest Initiative roll moves one of their Formations first. Presuming an equal number of units on the
        // two sides, the Initiative winner then moves one of their Formations, and the players continue alternating their unit movements
        // until all units have been moved. Recon, Aerospace and Ground movement occurs sequentially with all Recon movement completing
        // before aerospace and all aerospace before ground movement.
        // If, during a sub-phase, the number of Formations per side is unequal, the player with the higher number of Formations must move
        // more units in proportion to that of their opponent. See Unequal Number of Formations (see p. 164) for details.
    }

    protected void combatPhase() {
        // The player with the lowest Initiative roll acts first in the Combat Phase. This player then declares and resolves all of their
        // Formations’ combat actions at this time, followed by the Initiative winner. The Combat Phase only has sub-phases for aerospace
        // and ground combat. Recon Formations may be targeted directly in the Combat Phase.
    }

    protected void endPhase() {
        // Both players may complete the End Phase simultaneously. In this phase, each player checks Fatigue, Morale and expends supply.
        // After resolving all End Phase actions, the turn ends and the players return to Step 1, repeat all these steps until one side
        // meets its victory conditions for the scenario. Once victory has been determined,
        // players may also determine salvage for campaign games.
    }

    protected boolean isTeam1Victory() {
        var team1Victory = survivingUnits.entrySet().stream().anyMatch(e -> e.getKey().getID() == 1);
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

    private void printUnitsByCategory(String category, Map<TeamID, List<UnitStrength>> units) {
        System.out.println("\n" + category);
        units.forEach(AcsSimpleScenarioResolver::printTeamUnits);
    }

    private static void printTeamUnits(TeamID teamID, List<UnitStrength> teamUnits) {
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
            List<TeamID> teamIds = new ArrayList<>(combatRoundResults.keySet());
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
        if (forceMustBePreserved.contains(forcesNames.get(defeated.unit().unitStrength().entity().getForceId())) && winner.teamId().getID() == 1) {
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
    private SelectedUnit getUnitFromTeam(List<TeamID> teamIds) {
        var teamId = teamIds.remove(random.nextInt(teamIds.size()));
        List<UnitResult> teamUnits = combatRoundResults.get(teamId);
        UnitResult unit = teamUnits.remove(0);
        return new SelectedUnit(teamId, unit);
    }

    private record SelectedUnit(TeamID teamId, UnitResult unit) { }

    private boolean lessThanTwoTeamsLeft(List<TeamID> teamIds) {
        teamIds.removeIf(id -> combatRoundResults.get(id).isEmpty());
        return teamIds.size() < 2;
    }

    private void unitLostRollOff(UnitResult unit, TeamID teamId, UnitResult aggressor) {
        unitLossCount.put(unit.unitStrength(), Compute.computeTotalDamage(aggressor.unitStrength.entity().getTotalWeaponList()));
        roundDefeatedUnits.computeIfAbsent(teamId, k -> new ArrayList<>()).add(unit.unitStrength());
    }

    private void simplePostCombatResolution() {
        for (var entry : roundDefeatedUnits.entrySet()) {
            TeamID teamId = entry.getKey();
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
            TeamID teamId = entry.getKey();
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

    private void unitDestroyed(TeamID teamId, UnitStrength unitToRemove) {
        unitDefeated(teamId, unitToRemove);
        var randomlySelectedRemovalCondition = weightedRemovalConditions[random.nextInt(weightedRemovalConditions.length-1)];
        unitToRemove.entity().setRemovalCondition(randomlySelectedRemovalCondition);
        unitToRemove.entity().setSalvage(randomlySelectedRemovalCondition != IEntityRemovalConditions.REMOVE_DEVASTATED);
        System.out.println("XXXXXXX Unit " + unitToRemove.entity().getDisplayName() + " has been destroyed.");
    }

    private void retreatUnit(TeamID teamId, UnitStrength unitToRemove) {
        unitDefeated(teamId, unitToRemove);
        unitToRemove.entity().setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
        System.out.println("<<<<<<< Unit " + unitToRemove.entity().getDisplayName() + " has retreated.");
    }

    private void applyDamageToUnit(TeamID teamId, UnitStrength unitToRemove) {
        var entity = unitToRemove.entity();
        DamageHandlerChooser.chooseHandler(entity).applyDamage(unitLossCount.getOrDefault(unitToRemove, 5));
        System.out.println("@@@@@@@ Unit " + entity.getDisplayName() + " has taken damage.");
        if (entity.isDestroyed()) {
            unitDestroyed(teamId, unitToRemove);
        }
    }

    private void unitDefeated(TeamID teamId, UnitStrength unitToRemove) {
        survivingUnits.get(teamId).remove(unitToRemove);
        defeatedUnits.computeIfAbsent(teamId, k -> new ArrayList<>()).add(unitToRemove);
    }
}
