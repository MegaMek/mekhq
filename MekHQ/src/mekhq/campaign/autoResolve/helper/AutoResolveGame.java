package mekhq.campaign.autoResolve.helper;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;
import megamek.common.force.Forces;
import megamek.common.options.BasicGameOptions;
import megamek.server.scriptedevent.TriggeredEvent;
import mekhq.campaign.Campaign;

import java.util.List;
import java.util.Map;

public class AutoResolveGame implements IGame {

    private final Campaign campaign;

    public AutoResolveGame(Campaign campaign) {
        this.campaign = campaign;
    }

    @Override
    public PlayerTurn getTurn() {
        return null;
    }

    @Override
    public int getTurnIndex() {
        return 0;
    }

    @Override
    public List<? extends PlayerTurn> getTurnsList() {
        return List.of();
    }

    @Override
    public BasicGameOptions getOptions() {
        return null;
    }

    @Override
    public int getCurrentRound() {
        return 0;
    }

    @Override
    public void setCurrentRound(int currentRound) {

    }

    @Override
    public void incrementCurrentRound() {

    }

    @Override
    public GamePhase getPhase() {
        return null;
    }

    @Override
    public void setPhase(GamePhase phase) {

    }

    @Override
    public void setLastPhase(GamePhase lastPhase) {

    }

    @Override
    public boolean isCurrentPhasePlayable() {
        return false;
    }

    @Override
    public void fireGameEvent(GameEvent event) {

    }

    @Override
    public void addGameListener(GameListener listener) {

    }

    @Override
    public void removeGameListener(GameListener listener) {

    }

    @Override
    public boolean isForceVictory() {
        return false;
    }

    @Override
    public Forces getForces() {
        return null;
    }

    @Override
    public void setForces(Forces forces) {

    }

    @Override
    public Player getPlayer(int id) {
        var campaignPlayer = campaign.getPlayer();
        var player = new Player(campaignPlayer.getId(), campaignPlayer.getName());
        player.setTeam(1);
        return player;
    }

    @Override
    public List<Player> getPlayersList() {
        return List.of();
    }

    @Override
    public void addPlayer(int id, Player player) {

    }

    @Override
    public void setPlayer(int id, Player player) {

    }

    @Override
    public void removePlayer(int id) {

    }

    @Override
    public int getNoOfPlayers() {
        return 0;
    }

    @Override
    public List<Team> getTeams() {
        return List.of();
    }

    @Override
    public int getNoOfTeams() {
        return 0;
    }

    @Override
    public void setupTeams() {

    }

    @Override
    public int getNextEntityId() {
        return 0;
    }

    @Override
    public List<InGameObject> getInGameObjects() {
        return List.of();
    }

    @Override
    public void replaceUnits(List<InGameObject> units) {

    }

    @Override
    public List<InGameObject> getGraveyard() {
        return List.of();
    }

    @Override
    public void setBoard(int boardId, Board board) {

    }

    @Override
    public Map<Integer, Board> getBoards() {
        return Map.of();
    }

    @Override
    public void receiveBoard(int boardId, Board board) {

    }

    @Override
    public void receiveBoards(Map<Integer, Board> boards) {

    }

    @Override
    public ReportEntry getNewReport(int messageId) {
        return null;
    }

    @Override
    public List<TriggeredEvent> scriptedEvents() {
        return List.of();
    }

    @Override
    public void addScriptedEvent(TriggeredEvent event) {

    }
}
