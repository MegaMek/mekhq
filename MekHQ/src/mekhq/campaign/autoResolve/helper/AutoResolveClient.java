package mekhq.campaign.autoResolve.helper;

import megamek.client.AbstractClient;
import megamek.client.IClient;
import megamek.common.IGame;
import megamek.common.Player;

import java.util.Map;

public class AutoResolveClient implements IClient {

    private final IGame game;
    private final Player localPlayer;

    public AutoResolveClient(IGame game, Player player) {
        this.game = game;
        this.localPlayer = player;
    }

    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getHost() {
        return "";
    }

    @Override
    public void die() {

    }

    @Override
    public IGame getGame() {
        return game;
    }

    @Override
    public int getLocalPlayerNumber() {
        return localPlayer.getId();
    }

    @Override
    public Player getLocalPlayer() {
        return localPlayer;
    }

    @Override
    public boolean isMyTurn() {
        return false;
    }

    @Override
    public void setLocalPlayerNumber(int localPlayerNumber) {

    }

    @Override
    public Map<String, AbstractClient> getBots() {
        return Map.of();
    }

    @Override
    public void sendDone(boolean done) {

    }

    @Override
    public void sendChat(String message) {

    }
}
