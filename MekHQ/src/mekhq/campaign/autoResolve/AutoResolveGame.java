/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package mekhq.campaign.autoResolve;

import megamek.client.ui.swing.sbf.SelectDirection;
import megamek.common.*;
import megamek.common.actions.EntityAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.options.BasicGameOptions;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.SBFRuleOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.weapons.AreaEffectHelper;
import megamek.logging.MMLogger;
import megamek.server.totalwarfare.TWGameManager;
import megamek.server.victory.VictoryHelper;
import megamek.server.victory.VictoryResult;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.autoResolve.damageHandler.DamageHandlerChooser;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.AcsActionHandler;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormationTurn;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsTurn;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.unit.Unit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Luana Coppio
 */
public class AutoResolveGame extends AbstractGame implements PlanetaryConditionsUsing {
    private static final MMLogger logger = MMLogger.create(AutoResolveGame.class);
    /**
     * Information that is necessary for the setup of the game.
     */
    private final Campaign campaign;
    private final List<Unit> units;
    private final AtBScenario scenario;

    /**
     * Game Phase and rules
     */
    private final SBFRuleOptions options = new SBFRuleOptions();
    private GamePhase phase = GamePhase.UNKNOWN;
    private GamePhase lastPhase = GamePhase.UNKNOWN;
    private final PlanetaryConditions planetaryConditions = new PlanetaryConditions();

    private int lastEntityId;
    /**
     * Report and turnlist
     */
    private final List<AcsTurn> turnList = new ArrayList<>();

    private boolean gameEnded = false;

    /**
     * Tools for the game
     */
    private final List<AcsActionHandler> actionHandlers = new ArrayList<>();
    private MapSettings mapSettings;

    /**
     * Contains all units that have left the game by any means.
     */
    private Vector<Entity> graveyard = new Vector<>();

    private final Map<String, Object> victoryContext = new HashMap<>();
    private final VictoryHelper victoryHelper = new VictoryHelper(getOptions());
    private int victoryPlayerId = Player.PLAYER_NONE;
    private int victoryTeam = Player.TEAM_NONE;
    private Player localPlayer;

    public AutoResolveGame(MekHQ app, List<Unit> units, AtBScenario scenario) {
        setBoard(0, new Board());
        this.campaign = app.getCampaign();
        this.localPlayer = campaign.getPlayer();
        this.units = units;
        this.scenario = scenario;
    }

    public MapSettings getMapSettings() {
        if (mapSettings == null) {
            mapSettings = MapSettings.getInstance();
        }
        return mapSettings;
    }

    public void setMapSettings(MapSettings mapSettings) {
        this.mapSettings = mapSettings;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public void addUnit(InGameObject unit) {
        int id = unit.getId();
        if (inGameObjects.containsKey(id) || isOutOfGame(id) || (Entity.NONE == id)) {
            id = getNextEntityId();
            unit.setId(id);
        }
        inGameObjects.put(id, unit);
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public AtBScenario getScenario() {
        return scenario;
    }

    /** @return The TW Units (Entity) currently in the game. */
    public List<Entity> inGameTWEntities() {
        return filterToEntity(inGameObjects.values());
    }

    private List<Entity> filterToEntity(Collection<? extends BTObject> objects) {
        return objects.stream().filter(Entity.class::isInstance).map(o -> (Entity) o).toList();
    }

    @Override
    public void addAction(EntityAction action) {
        pendingActions.add(action);
    }

    @Override
    protected List<Deployable> deployableInGameObjects() {
        return inGameObjects.values().stream()
            .filter(unit -> unit instanceof Deployable)
            .filter(unit -> unit instanceof AcsFormation)
            .map(unit -> (Deployable) unit)
            .collect(Collectors.toList());
    }

    public int getNoOfEntities() {
        return inGameTWEntities().size();
    }

    public int getSelectedEntityCount(EntitySelector selector) {
        int retVal = 0;

        // If no selector was supplied, return the count of all game entities.
        if (null == selector) {
            retVal = getNoOfEntities();
        }

        // Otherwise, count the entities that meet the selection criteria.
        else {
            Iterator<Entity> iter = inGameTWEntities().iterator();
            while (iter.hasNext()) {
                if (selector.accept(iter.next())) {
                    retVal++;
                }
            }

        } // End use-selector

        // Return the number of selected entities.
        return retVal;
    }

    /**
     * Returns an enumeration of salvageable entities.
     */
    public List<Entity> getGraveyardEntities() {
        List<Entity> destroyed = new ArrayList<>();
        for (Entity entity : this.graveyard) {
            if ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)) {
                destroyed.add(entity);
            }
        }

        return destroyed;
    }

    /** @return The entity with the given id number, if any. */
    public Optional<Entity> getEntity(final int id) {
        InGameObject possibleEntity = inGameObjects.get(id);
        if (possibleEntity instanceof Entity) {
            return Optional.of((Entity) possibleEntity);
        }
        return Optional.empty();
    }

    public void addEntity(Entity entity) {
        // entity.setGame(this); Cant set the game!
        if (entity instanceof Mek) {
            ((Mek) entity).setBAGrabBars();
            ((Mek) entity).setProtoMekClampMounts();
        } else if (entity instanceof Tank) {
            ((Tank) entity).setBAGrabBars();
            ((Tank) entity).setTrailerHitches();
        }

        // Add magnetic clamp mounts
        if ((entity instanceof Mek) && !entity.isOmni() && !entity.hasBattleArmorHandles()) {
            entity.addTransporter(new ClampMountMek());
        } else if ((entity instanceof Tank) && !entity.isOmni()
            && !entity.hasBattleArmorHandles()) {
            entity.addTransporter(new ClampMountTank());
        }

        entity.setGameOptions();
        if (entity.getC3UUIDAsString() == null) {
            // We don't want to be resetting a UUID that exists already!
            entity.setC3UUID();
        }
        // Add this Entity, ensuring that its id is unique
        int id = entity.getId();
        if (isIdUsed(id)) {
            id = getNextEntityId();
            entity.setId(id);
        }
        inGameObjects.put(id, entity);

        if (id > lastEntityId) {
            lastEntityId = id;
        }

        if (entity instanceof Mek mek) {
            if (getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)) {
                mek.setAutoEject(true);
                mek.setCondEjectAmmo(!entity.hasCase() && !entity.hasCASEII());
                mek.setCondEjectEngine(true);
                mek.setCondEjectCTDest(true);
                mek.setCondEjectHeadshot(true);
            } else {
                mek.setAutoEject(!entity.hasCase() && !entity.hasCASEII());
            }
        }

        entity.setInitialBV(entity.calculateBattleValue(false, false));
    }

    public boolean isOutOfGame(int id) {
        for (Entity entity : graveyard) {
            if (entity.getId() == id) {
                return true;
            }
        }

        return false;
    }


    private boolean isIdUsed(int id) {
        return inGameObjects.containsKey(id) || isOutOfGame(id);
    }

    @Override
    public List<AcsTurn> getTurnsList() {
        return Collections.unmodifiableList(turnList);
    }

    @Override
    public BasicGameOptions getOptions() {
        return new GameOptions();
    }

    @Override
    public GamePhase getPhase() {
        return phase;
    }

    public void forget(int unitId) {
        inGameObjects.remove(unitId);
    }

    public void addActionHandler(AcsActionHandler handler) {
        if (actionHandlers.contains(handler)) {
            logger.error("Tried to re-add action handler {}!", handler);
        } else {
            actionHandlers.add(handler);
        }
    }

    @Override
    public AcsTurn getTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnList.size())) {
            return null;
        }
        return turnList.get(turnIndex);
    }

    public Optional<AcsTurn> getCurrentTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnList.size())) {
            return Optional.empty();
        }
        return Optional.of(turnList.get(turnIndex));
    }

    @Override
    public boolean hasMoreTurns() {
        return getTurnsList().size() > turnIndex + 1;
    }

    public void setTurns(List<AcsTurn> turns) {
        this.turnList.clear();
        this.turnList.addAll(turns);
    }

    @Override
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    @Override
    public void setLastPhase(GamePhase lastPhase) {
        this.lastPhase = this.phase;
    }

    @Override
    public void receivePhase(GamePhase phase) {
        GamePhase oldPhase = this.phase;
        setPhase(phase);
        fireGameEvent(new GamePhaseChangeEvent(this, oldPhase, phase));
    }

    @Override
    public boolean isForceVictory() {
        return currentRound > 100;
    }

    @Override
    public void addPlayer(int id, Player player) { // Server / Client-side?
        super.addPlayer(id, player);
        player.setGame(this);
        setupTeams();

        if ((player.isBot()) && (!player.getSingleBlind())) {
            boolean sbb = getOptions().booleanOption(OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS);
            boolean db = getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
            player.setSingleBlind(sbb && db);
        }
    }

    @Override
    public boolean isCurrentPhasePlayable() {
        return true;
    }

    @Override
    public void setPlayer(int id, Player player) {
        player.setGame(this);
        players.put(id, player);
        setupTeams();
    }

    @Override
    public void removePlayer(int id) {
        // not implemented
    }

    @Override
    public void setupTeams() {
        Vector<Team> initTeams = new Vector<>();

        // Now, go through all the teams, and add the appropriate player
        for (int t = Player.TEAM_NONE + 1; t < Player.TEAM_NAMES.length; t++) {
            Team newTeam = null;
            for (Player player : getPlayersList()) {
                if (player.getTeam() == t) {
                    if (newTeam == null) {
                        newTeam = new Team(t);
                    }
                    newTeam.addPlayer(player);
                }
            }

            if (newTeam != null) {
                initTeams.addElement(newTeam);
            }
        }

        for (Team newTeam : initTeams) {
            for (Team oldTeam : teams) {
                if (newTeam.equals(oldTeam)) {
                    newTeam.setInitiative(oldTeam.getInitiative());
                }
            }
        }

        // Carry over faction settings
        for (Team newTeam : initTeams) {
            for (Team oldTeam : teams) {
                if (newTeam.equals(oldTeam)) {
                    newTeam.setFaction(oldTeam.getFaction());
                }
            }
        }

        teams.clear();
        teams.addAll(initTeams);
    }

    @Override
    public void replaceUnits(List<InGameObject> units) {

    }

    @Override
    public List<InGameObject> getGraveyard() {
        return List.of();
    }

    @Override
    public int getLiveDeployedEntitiesOwnedBy(Player player) {
        var res = getInGameObjects().stream()
            .filter(unit -> unit instanceof AcsFormation)
            .filter(unit -> unit.getOwnerId() == player.getId())
            .filter(unit -> ((AcsFormation) unit).isDeployed())
            .mapToInt(unit -> ((AcsFormation) unit).getUnits().stream().mapToInt(SBFUnit::getCurrentArmor).sum())
            .filter(armor -> armor > 0).count();

        return (int) res;
    }

    @Override
    public ReportEntry getNewReport(int messageId) {
        // NOT IMPLEMENTED
        return null;
    }

    @Override
    public void setVictoryPlayerId(int victoryPlayerId) {
        this.victoryPlayerId = victoryPlayerId;
    }

    @Override
    public void setVictoryTeam(int victoryTeam) {
        this.victoryTeam = victoryTeam;
    }

    @Override
    public void cancelVictory() {
        this.victoryPlayerId = Player.PLAYER_NONE;
        this.victoryTeam = Player.TEAM_NONE;
    }

    @Override
    public int getVictoryPlayerId() {
        return victoryPlayerId;
    }

    @Override
    public int getVictoryTeam() {
        return victoryTeam;
    }

    @Override
    public boolean gameTimerIsExpired() {
        return getOptions().booleanOption(OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT)
            && (getRoundCount() == getOptions().intOption(OptionsConstants.VICTORY_GAME_TURN_LIMIT));
    }

    private int getRoundCount() {
        return currentRound;
    }

    @Override
    public int getLiveCommandersOwnedBy(Player player) {
        return 0;
    }

    public List<AcsActionHandler> getActionHandlers() {
        return actionHandlers;
    }

    public List<InGameObject> getFullyVisibleUnits(Player viewer) {
        return getInGameObjects();
    }

    public Optional<AcsTurn> changeToNextTurn() {
        turnIndex++;
        return getCurrentTurn();
    }

    public boolean hasEligibleFormation(AcsFormationTurn turn) {
        return (turn != null) && getActiveFormations().stream().anyMatch(f -> turn.isValidEntity(f, this));
    }

    /**
     * Returns the formation of the given ID, if one can be found.
     *
     * @param formationID the ID to look for
     * @return The formation or an empty Optional
     */
    public Optional<AcsFormation> getFormation(int formationID) {
        Optional<InGameObject> unit = getInGameObject(formationID);
        if (unit.isPresent() && unit.get() instanceof AcsFormation formation) {
            return Optional.of(formation);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public PlanetaryConditions getPlanetaryConditions() {
        return planetaryConditions;
    }

    @Override
    public void setPlanetaryConditions(final @Nullable PlanetaryConditions conditions) {
        planetaryConditions.alterConditions(conditions);
    }


    public GamePhase getLastPhase() {
        return lastPhase;
    }

    public Optional<Player> getPlayerFor(AcsTurn turn) {
        return Optional.ofNullable(getPlayer(turn.playerId()));
    }


    public void setVictoryContext(Map<String, Object> ctx) {
        victoryContext.clear();
        victoryContext.putAll(ctx);
    }

    @Override
    public Player getPlayer(int id) {
        if (players.containsKey(id)) {
            return players.get(id);
        } else {
            System.out.println("Player not found: " + id);
            return null;
        }
    }

    public boolean areHostile(AcsFormation formation, Player player) {
        return player.isEnemyOf(getPlayer(formation.getOwnerId()));
    }

    // check current turn, phase, formation
    private boolean isEligibleForAction(AcsFormation formation) {
        return (getTurn() instanceof AcsFormationTurn)
            && getTurn().isValidEntity(formation, this);
    }

    /**
     * @return the first formation in the list of formations that is alive and
     *         eligible for the current game phase.
     */
    public Optional<AcsFormation> getNextEligibleFormation() {
        return getNextEligibleFormation(BTObject.NONE);
    }

    /**
     * @return the preceding formation in the list of formations that is alive and
     *         eligible for the current game phase.
     */
    public Optional<AcsFormation> getPreviousEligibleFormation() {
        return getPreviousEligibleFormation(BTObject.NONE);
    }

    /**
     * @return the next in the list of formations that is alive and eligible for the
     *         current game phase,
     *         counting from the given current formation id. If no matching
     *         formation can be found for the given id,
     *         returns the first eligible formation in the list of formations that
     *         is alive and eligible.
     */
    public Optional<AcsFormation> getNextEligibleFormation(int currentFormationID) {
        return getEligibleFormationImpl(currentFormationID, phase, SelectDirection.NEXT_UNIT);
    }

    /**
     * @return the previous in the list of formations that is alive and eligible for
     *         the current game phase,
     *         counting from the given current formation id. If no matching
     *         formation can be found for the given id,
     *         returns the last eligible formation from the list of formations that
     *         is alive and eligible.
     */
    public Optional<AcsFormation> getPreviousEligibleFormation(int currentFormationID) {
        return getEligibleFormationImpl(currentFormationID, phase, SelectDirection.PREVIOUS_UNIT);
    }

    /**
     * Based on the given search direction, returns the formation that precedes or
     * follows the given
     * formation ID in the list of active formations eligible for action in the
     * given game phase.
     *
     * @param currentFormationID the start point of the formation search. Need not
     *                           match an actual formation
     * @param phase              the phase to check
     * @param direction          the selection to seek the next or previous
     *                           formation
     * @return the formation that precedes or follows the given formation ID, if one
     *         can be found
     */
    private Optional<AcsFormation> getEligibleFormationImpl(int currentFormationID, GamePhase phase,
                                                            SelectDirection direction) {
        var eligibleFormations = getActiveFormations().stream()
            .filter(this::isEligibleForAction)
            .toList();
        if (eligibleFormations.isEmpty()) {
            return Optional.empty();
        } else {
            var currentFormation = getFormation(currentFormationID);
            int index = currentFormation.map(eligibleFormations::indexOf).orElse(-1);
            if (index == -1) {
                // when no current unit is found, the next unit is the first, the previous unit
                // is the last
                index = direction.isNextUnit() ? 0 : eligibleFormations.size() - 1;
            } else {
                // must add the list size to safely get the previous unit because -1 % 5 == -1
                // (not 4)
                index += eligibleFormations.size() + (direction.isNextUnit() ? 1 : -1);
                index %= eligibleFormations.size();
            }
            return Optional.ofNullable(eligibleFormations.get(index));
        }
    }

    /**
     * Returns the list of formations that are in the game's InGameObject list, i.e.
     * that aren't destroyed
     * or otherwise removed from play.
     *
     * @return The currently active formations
     */
    public List<AcsFormation> getActiveFormations() {
        return inGameObjects.values().stream()
            .filter(u -> u instanceof AcsFormation)
            .filter(u -> ((AcsFormation) u).getUnits().stream().anyMatch(unit -> unit.getCurrentArmor() > 0))
            .map(u -> (AcsFormation) u)
            .toList();
    }

    public List<AcsFormation> getActiveFormations(Player player) {
        return getActiveFormations().stream()
            .filter(f -> f.getOwnerId() == player.getId())
            .toList();
    }

    public boolean gameHasEnded() {
        return gameEnded || isForceVictory();
    }

    public VictoryResult getVictoryResult() {
        return victoryHelper.checkForVictory(this, victoryContext);
    }

    public void destroyUnits(AcsFormation formation, List<SBFUnit> destroyedUnits) {
        for (SBFUnit unit : destroyedUnits) {
            for (var element : unit.getElements()) {
                var entityOpt = getEntity(element.getId());
                if (entityOpt.isPresent()) {
                    var entity = entityOpt.get();
                    damageEntity(entity);
                    graveyard.add(entity);
                    inGameObjects.remove(entity.getId());
                    var roll = Compute.rollD6(2);
                    switch (roll.getIntValue()) {
                        case 2 -> entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_CAPTURED);
                        case 3, 4 -> entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
                        case 5, 6, 7, 8, 9 -> entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
                        case 10, 11 -> entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_EJECTED);
                        case 12 -> entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
                    }

                }
            }

            formation.getUnits().remove(unit);
            if (formation.getUnits().isEmpty()) {
                // formation destroyed
                inGameObjects.remove(formation.getId());
            }
        }
    }

    private void damageEntity(Entity entity) {
        double damage = switch (entity.getRemovalCondition()) {
            case IEntityRemovalConditions.REMOVE_CAPTURED -> entity.getTotalOArmor() * 0.4;
            case IEntityRemovalConditions.REMOVE_DEVASTATED -> entity.getTotalOArmor() + entity.getTotalOInternal() / 2.0;
            case IEntityRemovalConditions.REMOVE_EJECTED -> (double) entity.getTotalOArmor() / Compute.rollD6(1).getIntValue();
            case IEntityRemovalConditions.REMOVE_IN_RETREAT -> entity.getTotalOArmor() * 0.8;
            default -> entity.getTotalOArmor() * 0.33;
        };
        DamageHandlerChooser.chooseHandler(entity).applyDamage((int) damage);
    }
}
