/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve;

import megamek.common.*;
import megamek.common.actions.EntityAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.enums.SkillLevel;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.StaticGameOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.logging.MMLogger;
import megamek.server.victory.VictoryHelper;
import megamek.server.victory.VictoryResult;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormationTurn;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcTurn;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.handler.AcActionHandler;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ScenarioObjective;

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
    private final AtBScenario scenario;

    /**
     * Objectives that must be considered during the game
     */
    private final List<String> forceMustBePreserved = new ArrayList<>();


    /**
     * Game Phase and rules
     */
    private GamePhase phase = GamePhase.UNKNOWN;
    private GamePhase lastPhase = GamePhase.UNKNOWN;
    private final PlanetaryConditions planetaryConditions = new PlanetaryConditions();
    private final Map<Integer, SkillLevel> playerSkillLevels = new HashMap<>();
    private int lastEntityId;
    /**
     * Report and turnlist
     */
    private final List<AcTurn> turnList = new ArrayList<>();

    /**
     * Tools for the game
     */
    private final List<AcActionHandler> actionHandlers = new ArrayList<>();
    private MapSettings mapSettings;

    /**
     * Contains all units that have left the game by any means.
     */
    private final Vector<Entity> graveyard = new Vector<>();
    private final IGameOptions options;
    private final Map<String, Object> victoryContext = new HashMap<>();
    private final VictoryHelper victoryHelper = new VictoryHelper(this);
    private int victoryPlayerId = Player.PLAYER_NONE;
    private int victoryTeam = Player.TEAM_NONE;

    public AutoResolveGame(AtBScenario scenario, IGameOptions gameOptions) {
        this.scenario = scenario;
        this.options = StaticGameOptions.create(gameOptions);

        this.setupScenarioObjectives();
        setBoard(0, new Board());
    }

    private void setupScenarioObjectives() {
        forceMustBePreserved.clear();
        scenario.getScenarioObjectives().forEach(objective -> {
            if (objective.getObjectiveCriterion().equals(ScenarioObjective.ObjectiveCriterion.Preserve)) {
                forceMustBePreserved.addAll(objective.getAssociatedForceNames());
            }
        });
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

    public void addUnit(InGameObject unit) {
        int id = unit.getId();
        if (inGameObjects.containsKey(id) || isOutOfGame(id) || (Entity.NONE == id)) {
            id = getNextEntityId();
            unit.setId(id);
        }
        inGameObjects.put(id, unit);
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
            .filter(unit -> unit instanceof AcFormation)
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
            for (Entity entity : inGameTWEntities()) {
                if (selector.accept(entity)) {
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
            mek.setAutoEject(true);
            mek.setCondEjectAmmo(!entity.hasCase() && !entity.hasCASEII());
            mek.setCondEjectEngine(true);
            mek.setCondEjectCTDest(true);
            mek.setCondEjectHeadshot(true);
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
    public List<AcTurn> getTurnsList() {
        return Collections.unmodifiableList(turnList);
    }


    @Override
    public IGameOptions getOptions() {
        if (options != null) {
            return options;
        }
        return StaticGameOptions.empty();
    }

    @Override
    public GamePhase getPhase() {
        return phase;
    }

    public void addActionHandler(AcActionHandler handler) {
        if (actionHandlers.contains(handler)) {
            logger.error("Tried to re-add action handler {}!", handler);
        } else {
            actionHandlers.add(handler);
        }
    }

    @Override
    public AcTurn getTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnList.size())) {
            return null;
        }
        return turnList.get(turnIndex);
    }

    public Optional<AcTurn> getCurrentTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnList.size())) {
            return Optional.empty();
        }
        return Optional.of(turnList.get(turnIndex));
    }

    @Override
    public boolean hasMoreTurns() {
        return getTurnsList().size() > turnIndex + 1;
    }

    public void setTurns(List<AcTurn> turns) {
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
//        return currentRound > 50;
        return false;
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
        var res = getActiveFormations(player).stream()
            .filter(AcFormation::isDeployed)
            .count();

        return (int) res;
    }

    @Override
    public ReportEntry getNewReport(int messageId) {
        return new SBFReportEntry(messageId);
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
        return getRoundCount() >= 1000;
    }

    private int getRoundCount() {
        return currentRound;
    }

    @Override
    public int getLiveCommandersOwnedBy(Player player) {
        return 0;
    }

    @Override
    public Optional<Player> playerForPlayername(String playerName) {
        // not implemented
        return Optional.empty();
    }

    @Override
    public Optional<Integer> idForPlayername(String playerName) {
        // not implemented
        return Optional.empty();
    }

    public List<AcActionHandler> getActionHandlers() {
        return actionHandlers;
    }

    public Optional<AcTurn> changeToNextTurn() {
        turnIndex++;
        return getCurrentTurn();
    }

    public boolean hasEligibleFormation(AcFormationTurn turn) {
        return (turn != null) && getActiveFormations().stream().anyMatch(f -> turn.isValidEntity(f, this));
    }

    /**
     * Returns the formation of the given ID, if one can be found.
     *
     * @param formationID the ID to look for
     * @return The formation or an empty Optional
     */
    public Optional<AcFormation> getFormation(int formationID) {
        Optional<InGameObject> unit = getInGameObject(formationID);
        if (unit.isPresent() && unit.get() instanceof AcFormation formation) {
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

    // check current turn, phase, formation
    private boolean isEligibleForAction(AcFormation formation) {
        return (getTurn() instanceof AcFormationTurn)
            && getTurn().isValidEntity(formation, this);
    }

    /**
     * Returns the list of formations that are in the game's InGameObject list, i.e.
     * that aren't destroyed
     * or otherwise removed from play.
     *
     * @return The currently active formations
     */
    public List<AcFormation> getActiveFormations() {
        return inGameObjects.values().stream()
            .filter(u -> u instanceof AcFormation)
            .map(u -> (AcFormation) u)
            .toList();
    }

    public List<AcFormation> getActiveFormations(Player player) {
        return getActiveFormations().stream()
            .filter(f -> f.getOwnerId() == player.getId())
            .toList();
    }

    public VictoryResult getVictoryResult() {
        return victoryHelper.checkForVictory(this, victoryContext);
    }

    public void addUnitToGraveyard(Entity entity) {
        var entityInGame = getEntity(entity.getId());
        if (entityInGame.isPresent()) {
            inGameObjects.remove(entity.getId());
            graveyard.add(entity);
        }
    }

    public void removeFormation(AcFormation formation) {
        inGameObjects.remove(formation.getId());
    }

    public void setPlayerSkillLevel(int playerId, SkillLevel averageSkillLevel) {
        playerSkillLevels.put(playerId, averageSkillLevel);
    }

    public Player getLocalPlayer() {
        return getPlayer(0);
    }
}
