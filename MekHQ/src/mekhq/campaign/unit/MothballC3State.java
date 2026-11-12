/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.unit;

import static mekhq.campaign.force.Formation.FORMATION_NONE;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import jakarta.annotation.Nullable;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.util.C3Util;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.NetworkChangedEvent;
import mekhq.campaign.force.AbstractForce;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Remembers how a unit was wired into its C3 networks when it was mothballed, so the wiring can be put back when the
 * unit is activated again.
 *
 * <p>Mothballing takes a unit out of the TOE, and leaving the TOE detaches the unit from every C3 network. That
 * detachment also severs links held <i>by other units</i>: a mothballed C3 master releases all of its slaves, and a
 * mothballed C3i, Naval C3, or Nova CEWS node leaves the shared network. This class captures those links up front,
 * keyed by {@link Unit} ID (entity C3 UUIDs are not stable across entity swaps) and persisted with {@link MothballInfo}.
 * </p>
 *
 * <p>Restoration never trusts the snapshot. The world may have changed while the unit sat mothballed - partners may
 * have been mothballed, reassigned, destroyed, or refitted so they no longer carry the right equipment, and masters
 * and networks may have been filled by other units. Every link is therefore re-validated against the current campaign
 * state and re-applied through the same capacity and equipment rules MegaMek enforces when a player builds a network
 * by hand ({@link C3Util#connect} for hierarchical C3, the maximum network size for C3i, Naval C3, and Nova CEWS). A
 * link that is no longer legal is skipped rather than forced, and the player is told how many were dropped.</p>
 */
public class MothballC3State {
    private static final MMLogger LOGGER = MMLogger.create(MothballC3State.class);

    /** The unit that was this unit's C3 master, if it had one that was not itself. */
    private UUID masterUnitId;
    /** Whether the unit was a company-level master (slaved to itself). */
    private boolean companyMaster;
    /** Units that were slaved to this unit (slaves and lance-level masters). */
    private final List<UUID> nodeUnitIds = new ArrayList<>();
    /** Other units that shared this unit's C3i, Naval C3, or Nova CEWS network. */
    private final List<UUID> networkMemberUnitIds = new ArrayList<>();

    /**
     * Parameterless constructor, used for deserialization.
     */
    MothballC3State() {
    }

    /**
     * Captures the C3 wiring of a unit that is about to leave the TO&E. Must be called <b>before</b> the unit is
     * removed from its formation, because that removal is what severs the links.
     *
     * @param unit     the unit about to be mothballed
     * @param campaign the campaign the unit belongs to
     */
    MothballC3State(Unit unit, Campaign campaign) {
        Entity entity = unit.getEntity();
        // C3 networks only exist among units in the TO&E, and only units leaving it get detached
        if ((entity == null) || (campaign == null) || (unit.getFormationId() == FORMATION_NONE)) {
            return;
        }

        AbstractForce force = campaign.getPlayerForce();
        if (force == null) {
            return;
        }

        String masterUuid = entity.hasC3() ? entity.getC3MasterIsUUIDAsString() : null;
        if (masterUuid != null) {
            if (masterUuid.equals(entity.getC3UUIDAsString())) {
                companyMaster = true;
            } else {
                for (Unit other : force.allUnits()) {
                    Entity otherEntity = other.getEntity();
                    if ((other != unit) && (otherEntity != null) && masterUuid.equals(otherEntity.getC3UUIDAsString())) {
                        masterUnitId = other.getId();
                        break;
                    }
                }
            }
        }

        if (entity.hasC3M()) {
            for (Unit node : force.getC3Nodes(unit)) {
                nodeUnitIds.add(node.getId());
            }
        }

        if (entity.hasNhC3()) {
            String networkId = entity.getC3NetId();
            for (Unit other : force.allUnits()) {
                Entity otherEntity = other.getEntity();
                if ((other != unit) &&
                          (otherEntity != null) &&
                          C3Util.sameNhC3System(entity, otherEntity) &&
                          (networkId != null) &&
                          networkId.equals(otherEntity.getC3NetId())) {
                    networkMemberUnitIds.add(other.getId());
                }
            }
        }
    }

    /**
     * @return {@code true} if there is nothing to restore
     */
    public boolean isEmpty() {
        return (masterUnitId == null) && !companyMaster && nodeUnitIds.isEmpty() && networkMemberUnitIds.isEmpty();
    }

    /**
     * Re-applies whatever of the captured wiring is still legal. Must be called <b>after</b> the unit is back in the
     * TO&E; a unit that is not in a formation is left alone, because C3 networks do not exist outside the TO&E.
     *
     * @param unit     the unit that has just been activated
     * @param campaign the campaign the unit belongs to
     *
     * @return the number of captured links that could not be restored
     */
    int restore(Unit unit, Campaign campaign) {
        Entity entity = unit.getEntity();
        Game game = campaign.getGame();
        if (isEmpty() || (entity == null) || (game == null) || (entity.getGame() == null) ||
                  (unit.getFormationId() == FORMATION_NONE)) {
            return 0;
        }

        AbstractForce force = campaign.getPlayerForce();
        Set<Unit> changedUnits = new LinkedHashSet<>();
        int dropped = 0;

        // Company-level master first: it changes how many slaves the unit can take, so it must precede them
        boolean companyLevelRestored = true;
        if (companyMaster && !entity.isC3CompanyCommander()) {
            companyLevelRestored = restoreCompanyMaster(entity);
            if (companyLevelRestored) {
                changedUnits.add(unit);
            } else {
                dropped++;
            }
        }

        if ((masterUnitId != null) && (entity.getC3MasterId() == Entity.NONE)) {
            Unit master = getEligiblePartner(campaign, unit, masterUnitId);
            if ((master != null) && connect(game, entity, master.getEntity())) {
                changedUnits.add(unit);
                changedUnits.add(master);
            } else {
                dropped++;
            }
        }

        for (UUID nodeId : nodeUnitIds) {
            Unit node = getEligiblePartner(campaign, unit, nodeId);
            // A node that is already slaved to something else has been reassigned and is not ours to take back, and
            // if the company-level role could not be restored this unit is not the master those nodes were slaved to
            if ((node == null) ||
                      !companyLevelRestored ||
                      (node.getEntity().getC3MasterId() != Entity.NONE) ||
                      !connect(game, node.getEntity(), entity)) {
                dropped++;
            } else {
                changedUnits.add(node);
                changedUnits.add(unit);
            }
        }

        if (!networkMemberUnitIds.isEmpty() && (entity.hasNhC3())) {
            if (rejoinNetwork(campaign, unit, force)) {
                changedUnits.add(unit);
            } else {
                dropped++;
            }
        }

        if (!changedUnits.isEmpty()) {
            force.refreshNetworks(game);
            MekHQ.triggerEvent(new NetworkChangedEvent(new ArrayList<>(changedUnits)));
        }

        return dropped;
    }

    /**
     * Makes the entity a company-level master.
     *
     * @return {@code true} if the entity is now a company-level master
     */
    private boolean restoreCompanyMaster(Entity entity) {
        try {
            C3Util.setCompanyMaster(List.of(entity));
        } catch (C3Util.MissingC3MException ex) {
            LOGGER.warn("{} can no longer be a company-level C3 master: it has no operable C3 master computer",
                  entity.getShortName());
            return false;
        }
        entity.setC3MasterIsUUIDAsString(entity.getC3UUIDAsString());
        return true;
    }

    /**
     * Slaves {@code node} to {@code master} if MegaMek's equipment and capacity rules allow it, and records the link
     * so it survives a save.
     *
     * @return {@code true} if the link was made
     */
    private boolean connect(Game game, Entity node, Entity master) {
        try {
            C3Util.connect(game, new ArrayList<>(List.of(node)), master.getId(), false);
        } catch (C3Util.MismatchingC3MException | C3Util.C3CapacityException ex) {
            LOGGER.info("Could not restore C3 link from {} to master {}: {}",
                  node.getShortName(),
                  master.getShortName(),
                  ex.getClass().getSimpleName());
            return false;
        }
        node.setC3MasterIsUUIDAsString(master.getC3UUIDAsString());
        return true;
    }

    /**
     * Puts the unit back on the first of its former network partners whose network still has room for it.
     *
     * @return {@code true} if the unit rejoined a network
     */
    private boolean rejoinNetwork(Campaign campaign, Unit unit, AbstractForce force) {
        Entity entity = unit.getEntity();
        for (UUID memberId : networkMemberUnitIds) {
            Unit member = getEligiblePartner(campaign, unit, memberId);
            if ((member == null) || !C3Util.sameNhC3System(entity, member.getEntity())) {
                continue;
            }

            String networkId = member.getEntity().getC3NetId();
            if (networkId == null) {
                continue;
            }

            int maximumSize = member.getEntity().hasNovaCEWS() ? Entity.MAX_NOVA_CEWS_NODES : Entity.MAX_C3i_NODES;
            int currentSize = 0;
            for (Unit other : force.allUnits()) {
                Entity otherEntity = other.getEntity();
                if ((other != unit) &&
                          (otherEntity != null) &&
                          C3Util.sameNhC3System(entity, otherEntity) &&
                          networkId.equals(otherEntity.getC3NetId())) {
                    currentSize++;
                }
            }

            if (currentSize < maximumSize) {
                Vector<Unit> joining = new Vector<>();
                joining.add(unit);
                force.addUnitsToNetwork(joining, networkId, campaign.getGame());
                return true;
            }
        }

        return false;
    }

    /**
     * Looks up a former partner and confirms it can take part in a C3 network right now: it exists, has an entity, is
     * in the TO&E (C3 networks only exist there), and is not itself mothballed or being mothballed.
     *
     * @return the partner, or {@code null} if it can no longer be linked to
     */
    private static @Nullable Unit getEligiblePartner(Campaign campaign, Unit self, UUID partnerId) {
        Unit partner = campaign.getUnit(partnerId);
        if ((partner == null) ||
                  (partner == self) ||
                  (partner.getEntity() == null) ||
                  (partner.getFormationId() == FORMATION_NONE) ||
                  partner.isMothballed() ||
                  partner.isMothballing()) {
            return null;
        }
        return partner;
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        if (isEmpty()) {
            return;
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "c3State");
        if (masterUnitId != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "masterUnitId", masterUnitId);
        }
        if (companyMaster) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "companyMaster", true);
        }
        for (UUID nodeId : nodeUnitIds) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nodeUnitId", nodeId);
        }
        for (UUID memberId : networkMemberUnitIds) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "networkMemberUnitId", memberId);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "c3State");
    }

    static MothballC3State generateInstanceFromXML(Node wn) {
        MothballC3State retVal = new MothballC3State();

        NodeList nl = wn.getChildNodes();
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("masterUnitId")) {
                    retVal.masterUnitId = UUID.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("companyMaster")) {
                    retVal.companyMaster = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("nodeUnitId")) {
                    retVal.nodeUnitIds.add(UUID.fromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("networkMemberUnitId")) {
                    retVal.networkMemberUnitIds.add(UUID.fromString(wn2.getTextContent().trim()));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }

        return retVal;
    }
}
