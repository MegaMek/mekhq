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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Which person wears which suit of a battle armor unit. Each suit is a trooper slot, numbered as MegaMek numbers the
 * trooper locations, and holds at most one person. A person keeps their suit until they leave the unit, can no longer
 * fight, or the suit can no longer be worn; anyone without a suit takes the best free one.
 */
public class TrooperSlots {
    private static final MMLogger LOGGER = MMLogger.create(TrooperSlots.class);

    private static final String TAG_TROOPER_SLOTS = "trooperSlots";
    private static final String TAG_TROOPER_SLOT = "trooperSlot";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_PERSON_ID = "personId";

    private final Map<Integer, UUID> occupantBySlot = new TreeMap<>();

    /**
     * @param slot the trooper slot
     *
     * @return the id of the person wearing that suit, or {@code null} if nobody does
     */
    public @Nullable UUID getOccupant(int slot) {
        return occupantBySlot.get(slot);
    }

    /**
     * @param personId the person
     *
     * @return the trooper slot the person wears, or {@code null} if they wear none
     */
    public @Nullable Integer findSlot(UUID personId) {
        for (Map.Entry<Integer, UUID> entry : occupantBySlot.entrySet()) {
            if (entry.getValue().equals(personId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Takes the person out of their suit, if they wear one.
     *
     * @param personId the person
     */
    public void release(UUID personId) {
        occupantBySlot.values().removeIf(personId::equals);
    }

    /**
     * Puts the crew in the suits. A person keeps the suit they already wear while it can still be worn; anyone else
     * takes the first free suit in {@code usableSlots}. People not in {@code crewIds} lose their suit.
     *
     * @param crewIds     the people able to wear a suit, in the order they choose one
     * @param usableSlots the suits that can be worn, best first
     *
     * @return the suits now worn, in the order of {@code usableSlots}
     */
    public List<Integer> seat(List<UUID> crewIds, List<Integer> usableSlots) {
        int seatedBefore = occupantBySlot.size();
        occupantBySlot.entrySet()
              .removeIf(entry -> !crewIds.contains(entry.getValue()) || !usableSlots.contains(entry.getKey()));
        int keptTheirSuit = occupantBySlot.size();

        for (UUID personId : crewIds) {
            if (occupantBySlot.containsValue(personId)) {
                continue;
            }
            for (Integer slot : usableSlots) {
                if (!occupantBySlot.containsKey(slot)) {
                    occupantBySlot.put(slot, personId);
                    break;
                }
            }
        }

        List<Integer> wornSlots = new ArrayList<>();
        for (Integer slot : usableSlots) {
            if (occupantBySlot.containsKey(slot)) {
                wornSlots.add(slot);
            }
        }
        LOGGER.debug("[TrooperSlots] {} of {} people kept their suit, {} newly seated, {} suits usable",
              keptTheirSuit, seatedBefore, wornSlots.size() - keptTheirSuit, usableSlots.size());
        return wornSlots;
    }

    /**
     * Finds the people whose suit was lost in a battle: they wore a suit that came back with no living trooper in it.
     *
     * @param battleEntity the squad as it came back from the battle
     *
     * @return the ids of the people in the lost suits
     */
    public Set<UUID> findWearersOfLostSuits(BattleArmor battleEntity) {
        Set<UUID> wearersOfLostSuits = new HashSet<>();
        for (Map.Entry<Integer, UUID> entry : occupantBySlot.entrySet()) {
            if (!battleEntity.isTrooperActive(entry.getKey())) {
                wearersOfLostSuits.add(entry.getValue());
            }
        }
        return wearersOfLostSuits;
    }

    /**
     * Writes the suit assignments, if there are any.
     *
     * @param printWriter where to write
     * @param indent      the indent of the opening tag
     */
    public void writeToXML(PrintWriter printWriter, int indent) {
        if (occupantBySlot.isEmpty()) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(printWriter, indent++, TAG_TROOPER_SLOTS);
        for (Map.Entry<Integer, UUID> entry : occupantBySlot.entrySet()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(printWriter, indent++, TAG_TROOPER_SLOT);
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, TAG_SLOT, entry.getKey());
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, TAG_PERSON_ID, entry.getValue());
            MHQXMLUtility.writeSimpleXMLCloseTag(printWriter, --indent, TAG_TROOPER_SLOT);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(printWriter, --indent, TAG_TROOPER_SLOTS);
    }

    /**
     * @param nodeName the name of a child node of a saved unit
     *
     * @return {@code true} if the node holds the suit assignments
     */
    public static boolean isTrooperSlotsNode(String nodeName) {
        return TAG_TROOPER_SLOTS.equalsIgnoreCase(nodeName);
    }

    /**
     * Reads the suit assignments written by {@link #writeToXML}. An entry that cannot be read is skipped and logged;
     * the person is then seated again the next time the crew is placed.
     *
     * @param trooperSlotsNode the {@code trooperSlots} node
     */
    public void readFromXML(Node trooperSlotsNode) {
        occupantBySlot.clear();
        NodeList trooperSlotNodes = trooperSlotsNode.getChildNodes();
        for (int index = 0; index < trooperSlotNodes.getLength(); index++) {
            Node trooperSlotNode = trooperSlotNodes.item(index);
            if (TAG_TROOPER_SLOT.equalsIgnoreCase(trooperSlotNode.getNodeName())) {
                readTrooperSlot(trooperSlotNode.getChildNodes());
            }
        }
    }

    private void readTrooperSlot(NodeList fieldNodes) {
        String slotText = null;
        String personIdText = null;
        for (int index = 0; index < fieldNodes.getLength(); index++) {
            Node fieldNode = fieldNodes.item(index);
            if (TAG_SLOT.equalsIgnoreCase(fieldNode.getNodeName())) {
                slotText = fieldNode.getTextContent().trim();
            } else if (TAG_PERSON_ID.equalsIgnoreCase(fieldNode.getNodeName())) {
                personIdText = fieldNode.getTextContent().trim();
            }
        }
        if ((slotText == null) || (personIdText == null)) {
            LOGGER.warn("[TrooperSlots] Skipping a saved suit assignment missing its slot or person");
            return;
        }
        try {
            int slot = Integer.parseInt(slotText);
            UUID personId = UUID.fromString(personIdText);
            if (occupantBySlot.containsValue(personId)) {
                LOGGER.warn("[TrooperSlots] Skipping a second saved suit, slot {}, for person {}", slot, personId);
                return;
            }
            occupantBySlot.put(slot, personId);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("[TrooperSlots] Skipping an unreadable saved suit assignment: slot {}, person {}",
                  slotText, personIdText);
        }
    }
}
