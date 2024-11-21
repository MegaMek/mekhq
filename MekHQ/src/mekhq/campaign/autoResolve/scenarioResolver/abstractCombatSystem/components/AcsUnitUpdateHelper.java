package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.*;
import megamek.server.sbf.SBFGameManager;
import megamek.server.sbf.SBFGameManagerHelper;

public record AcsUnitUpdateHelper(AcsGameManager gameManager) implements AcsGameManagerHelper {

    /**
     * Updates all units to all players, taking into account visibility.
     *
     * @see #sendUnitUpdate(Player, InGameObject)
     */
    void sendAllUnitUpdate() {
        game().getPlayersList().forEach(this::sendAllUnitUpdate);
    }

    /**
     * Updates all units to the given recipient, taking into account visibility.
     *
     * @param player The recipient of the update
     * @see #sendUnitUpdate(Player, InGameObject)
     */
    void sendAllUnitUpdate(Player player) {

    }

    /**
     * @return A packet instructing the client to replace any previous unit of the same id with the given unit.
     */
    private Packet createUnitPacket(InGameObject unit) {
        return new Packet(PacketCommand.ENTITY_UPDATE, unit);
    }

    /**
     * @return A packet instructing the client to forget the invisible unit
     */
    private Packet createUnitInvisiblePacket(InGameObject unit) {
        return new Packet(PacketCommand.UNIT_INVISIBLE, unit.getId());
    }

    /**
     * Sends the given unit to all players, considering the unit's visibility, if it is
     * a formation. If it is visible, the formation is sent as is. If it is less than visible,
     * a replacement object is sent instead. If the formation is invisible to the player, the
     * client is sent a packet instead that instructs it to remove this id.
     * Units other than formations are not checked for visibility but sent as is.
     *
     * @param unit The unit to send
     */
    void sendUnitUpdate(InGameObject unit) {
        // do nothing
    }

    private InGameObject getReplacement(SBFFormation formation, SBFVisibilityStatus visibility) {
        return switch (visibility) {
            case VISIBLE, UNKNOWN -> formation;
            case INVISIBLE -> null;
            case BLIP, SENSOR_PING, SENSOR_GHOST -> new SBFUnitPlaceHolder(formation);
            case SOMETHING_OUT_THERE -> new SBFSomethingOutThereUnitPlaceHolder(formation);
            case I_GOT_SOMETHING -> new SBFIGotSomethingUnitPlaceholder(formation);
            case PARTIAL_SCAN_RECON, EYES_ON_TARGET, PARTIAL_SCAN -> new SBFPartialScanUnitPlaceHolder(formation);
        };
    }
}
