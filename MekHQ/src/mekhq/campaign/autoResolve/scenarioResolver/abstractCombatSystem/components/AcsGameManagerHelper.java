package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.server.GameManagerPacketHelper;
import mekhq.campaign.autoResolve.helper.AutoResolveGame;

public interface AcsGameManagerHelper {


    AcsGameManager gameManager();

    default AutoResolveGame game() {
        return gameManager().getGame();
    }

    default void addReport(SBFReportEntry reportEntry) {
        gameManager().addReport(reportEntry);
    }

    default void send(Packet packet) {
        gameManager().send(packet);
    }

    default void send(int playerId, Packet packet) {
        gameManager().send(playerId, packet);
    }

    default GameManagerPacketHelper packetHelper() {
        return gameManager().getPacketHelper();
    }


}
