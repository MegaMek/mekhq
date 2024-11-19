package mekhq.campaign.autoResolve.helper;

import megamek.client.ui.swing.sbf.SelectDirection;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.options.BasicGameOptions;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.SBFRuleOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.logging.MMLogger;
import megamek.server.victory.VictoryHelper;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsActionHandler;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormationTurn;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsTurn;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.unit.Unit;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.stream.Collectors.toList;

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
    protected final Map<Integer, Player> players = new HashMap<>();
    protected final CopyOnWriteArrayList<Team> teams = new CopyOnWriteArrayList<>();
    private int lastEntityId;
    /**
     * Report and turnlist
     */
    private final SBFFullGameReport gameReport = new SBFFullGameReport();
    private final List<AcsTurn> turnList = new ArrayList<>();
    private int turnIndex = -1;
    private boolean gameEnded = false;

    /**
     * Tools for the game
     */
    private final List<AcsActionHandler> actionHandlers = new ArrayList<>();
    private MapSettings mapSettings;

    /**
     * Contains all units that have left the game by any means.
     */
    private Vector<Entity> vOutOfGame = new Vector<>();

    private final Map<String, Object> victoryContext = new HashMap<>();
    private final VictoryHelper victoryHelper = new VictoryHelper(getOptions());

    public AutoResolveGame(MekHQ app, List<Unit> units, AtBScenario scenario) {
        setBoard(0, new Board());
        this.campaign = app.getCampaign();
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

    public void addUnit(InGameObject unit) { // This is a server-side method!
        int id = unit.getId();
        if (inGameObjects.containsKey(id)) {
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
        List<Entity> graveyard = new ArrayList<>();

        for (Entity entity : vOutOfGame) {
            if ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)) {
                graveyard.add(entity);
            }
        }

        return graveyard;
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
        for (Entity entity : vOutOfGame) {
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
    public @Nullable AcsTurn getTurn() {
        if ((turnIndex < 0) || (turnIndex >= turnList.size())) {
            return null;
        } else {
            return turnList.get(turnIndex);
        }
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
        return switch (phase) {
            case INITIATIVE, END, PREMOVEMENT, DEPLOY_MINEFIELDS, SET_ARTILLERY_AUTOHIT_HEXES, PREFIRING, TARGETING, PHYSICAL, OFFBOARD, OFFBOARD_REPORT, SBF_DETECTION, SBF_DETECTION_REPORT ->
                false;
            case DEPLOYMENT, MOVEMENT, FIRING ->
                hasMoreTurns();
            default -> true;
        };
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
        List<Team> initTeams = new ArrayList<>();

        // Now, go through all the teams, and add the appropriate player
        for (int t = Player.TEAM_NONE + 1; t < Player.TEAM_NAMES.length; t++) {
            Team tempTeam = null;
            for (Player player : getPlayersList()) {
                if (player.getTeam() == t) {
                    if (tempTeam == null) {
                        tempTeam = new Team(t);
                    }
                    tempTeam.addPlayer(player);
                }
            }

            if (tempTeam != null) {
                initTeams.add(tempTeam);
            }
        }

        for (Team newTeam : initTeams) {
            for (Team oldTeam : teams) {
                if (newTeam.equals(oldTeam)) {
                    newTeam.setInitiative(oldTeam.getInitiative());
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
    public ReportEntry getNewReport(int messageId) {
        return null;
    }

    public List<AcsActionHandler> getActionHandlers() {
        return actionHandlers;
    }

    public List<InGameObject> getFullyVisibleUnits(Player viewer) {
        return getInGameObjects();
    }

    public AcsTurn changeToNextTurn() {
        return null;
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
    public Optional<SBFFormation> getFormation(int formationID) {
        Optional<InGameObject> unit = getInGameObject(formationID);
        if (unit.isPresent() && unit.get() instanceof SBFFormation formation) {
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


    public boolean areHostile(SBFFormation formation, Player player) {
        return player.isEnemyOf(getPlayer(formation.getOwnerId()));
    }

    // check current turn, phase, formation
    private boolean isEligibleForAction(SBFFormation formation) {
        return (getTurn() instanceof AcsFormationTurn)
            && getTurn().isValidEntity(formation, this);
    }

    /**
     * @return the first formation in the list of formations that is alive and
     *         eligible for the current game phase.
     */
    public Optional<SBFFormation> getNextEligibleFormation() {
        return getNextEligibleFormation(BTObject.NONE);
    }

    /**
     * @return the preceding formation in the list of formations that is alive and
     *         eligible for the current game phase.
     */
    public Optional<SBFFormation> getPreviousEligibleFormation() {
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
    public Optional<SBFFormation> getNextEligibleFormation(int currentFormationID) {
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
    public Optional<SBFFormation> getPreviousEligibleFormation(int currentFormationID) {
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
    private Optional<SBFFormation> getEligibleFormationImpl(int currentFormationID, GamePhase phase,
                                                            SelectDirection direction) {
        List<SBFFormation> eligibleFormations = getActiveFormations().stream()
            .filter(this::isEligibleForAction)
            .toList();
        if (eligibleFormations.isEmpty()) {
            return Optional.empty();
        } else {
            Optional<SBFFormation> currentFormation = getFormation(currentFormationID);
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
    public List<SBFFormation> getActiveFormations() {
        return inGameObjects.values().stream()
            .filter(u -> u instanceof SBFFormation)
            .map(u -> (SBFFormation) u)
            .toList();
    }

    public List<SBFFormation> getActiveFormations(Player player) {
        return getActiveFormations().stream()
            .filter(f -> f.getOwnerId() == player.getId())
            .toList();
    }

    public boolean gameHasEnded() {
        return gameEnded || isForceVictory();
    }
}
