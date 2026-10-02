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
package mekhq.campaign.parts;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignLocationManager;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.Part.PartRef;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Turns a saved link from one part to another back into the part it names, as a campaign loads.
 *
 * <p>A link saved by identity names exactly one part in the campaign. A link from an older save names a part number,
 * and every warehouse numbers its parts from 1, so the same number can name a part at a base and a different part in
 * the main force. Such a link is looked for first in the warehouse that holds the linking part, then in the warehouse
 * of its unit, then in the main warehouse and the bases', and a part found there is used only if the link makes sense
 * from its end as well. A link that fits no part is dropped and logged rather than tied to the wrong part.</p>
 */
final class PartLinkResolver {
    private static final MMLogger LOGGER = MMLogger.create(PartLinkResolver.class);

    /** The kinds of link one part keeps to another. */
    enum LinkKind {
        /** A spare set aside to replace a missing part */
        REPLACEMENT,
        /** The part this one belongs to, such as the bay a door belongs to */
        PARENT,
        /** A part that belongs to this one */
        CHILD,
        /** A part a refit takes off its unit: it is on that unit */
        REFIT_OLD_PART,
        /** A part a refit puts on its unit: it is on that unit, or a spare reserved for the refit */
        REFIT_NEW_PART,
        /** The armor set aside for a refit: spare armor reserved for it */
        REFIT_ARMOR_SUPPLIES,
        /** A large craft ammunition bin a refit changes: it is on the refit's unit */
        REFIT_LARGE_CRAFT_BIN
    }

    private PartLinkResolver() {}

    /**
     * @param campaign    the campaign being loaded
     * @param linkingPart the part that keeps the link
     * @param link        the saved link
     * @param kind        what the link is for
     *
     * @return the linked part, or {@code null} if no part fits the link
     */
    static @Nullable Part resolve(Campaign campaign, Part linkingPart, PartRef link, LinkKind kind) {
        CampaignLocationManager locationManager = campaign.getCampaignLocationManager();
        UUID linkedUniqueId = link.getLinkedUniqueId();
        if (linkedUniqueId != null) {
            Part linkedPart = locationManager.findPartAnywhere(campaign, linkedUniqueId);
            if (linkedPart == null) {
                LOGGER.error("[PartIdentity] {} links to a {} part {} that is not in the campaign",
                      describe(linkingPart), kind, linkedUniqueId);
            }
            return linkedPart;
        }

        int legacyNumber = link.getId();
        if (legacyNumber <= 0) {
            return null;
        }
        for (LocalWarehouse warehouse : legacySearchOrder(campaign, linkingPart)) {
            Part candidate = warehouse.getPart(legacyNumber);
            boolean isOtherPart = (candidate != null) && (candidate != linkingPart);
            if (isOtherPart && isConsistent(linkingPart, candidate, kind)) {
                return candidate;
            }
        }
        LOGGER.warn("[PartIdentity] {}: no part numbered {} fits as its {} part, so the link is dropped",
              describe(linkingPart), legacyNumber, kind);
        return null;
    }

    /** Names the linking part in the log; a refit is named by the unit it refits. */
    private static String describe(Part linkingPart) {
        if (linkingPart instanceof Refit refit) {
            Unit refittingUnit = refit.getUnit();
            return "the refit of " + ((refittingUnit == null) ? "an unknown unit" : refittingUnit.getName());
        }
        return linkingPart.getName() + " #" + linkingPart.getId();
    }

    /**
     * The warehouses to look in for a part number from an older save: the one holding the linking part, where the
     * save numbered its links, then its unit's, then the main force's, then each base's, for a unit whose parts are
     * kept somewhere other than where it is.
     */
    private static Set<LocalWarehouse> legacySearchOrder(Campaign campaign, Part linkingPart) {
        Set<LocalWarehouse> searchOrder = new LinkedHashSet<>();
        LocalWarehouse holdingWarehouse = campaign.getCampaignLocationManager()
                                                .findHoldingWarehouse(campaign, linkingPart);
        if (holdingWarehouse != null) {
            searchOrder.add(holdingWarehouse);
        }
        searchOrder.add(linkingPart.getWarehouse());
        searchOrder.add(campaign.getPlayerForce().getWarehouse());
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            searchOrder.add(base.getBaseWarehouse());
        }
        return searchOrder;
    }

    /**
     * Whether the link also makes sense from the candidate's end.
     */
    private static boolean isConsistent(Part linkingPart, Part candidate, LinkKind kind) {
        return switch (kind) {
            case REPLACEMENT -> isReservedFor(linkingPart, candidate);
            case PARENT -> isOnSameUnit(linkingPart, candidate) && listsAsChild(candidate, linkingPart);
            case CHILD -> isOnSameUnit(linkingPart, candidate) && pointsAt(candidate.getParentPart(), linkingPart);
            case REFIT_OLD_PART, REFIT_LARGE_CRAFT_BIN -> isOn(candidate.getUnit(), linkingPart.getUnit());
            case REFIT_NEW_PART -> isOn(candidate.getUnit(), linkingPart.getUnit())
                                         || isReservedForRefit(candidate, linkingPart.getUnit());
            case REFIT_ARMOR_SUPPLIES -> (candidate instanceof Armor)
                                               && isReservedForRefit(candidate, linkingPart.getUnit());
        };
    }

    /** Whether the part's unit is the given unit. */
    private static boolean isOn(@Nullable Unit partUnit, @Nullable Unit unit) {
        return (partUnit != null) && (unit != null) && partUnit.getId().equals(unit.getId());
    }

    /** A spare set aside for the given unit's refit. */
    private static boolean isReservedForRefit(Part candidate, @Nullable Unit refitUnit) {
        return (candidate.getUnit() == null) && isOn(candidate.getRefitUnit(), refitUnit);
    }

    /** A replacement is a spare reserved by the tech who is working on the missing part. */
    private static boolean isReservedFor(Part missingPart, Part candidate) {
        boolean isSpare = candidate.getUnit() == null;
        Person reservedBy = candidate.getReservedBy();
        Person tech = missingPart.getTech();
        boolean isReservedByThatTech = (reservedBy != null) && ((tech == null) || tech.getId()
                                                                                         .equals(reservedBy.getId()));
        return isSpare && isReservedByThatTech;
    }

    /** Both parts are on the same unit, or both are spares. */
    private static boolean isOnSameUnit(Part linkingPart, Part candidate) {
        Unit linkingUnit = linkingPart.getUnit();
        Unit candidateUnit = candidate.getUnit();
        if ((linkingUnit == null) || (candidateUnit == null)) {
            return (linkingUnit == null) && (candidateUnit == null);
        }
        return linkingUnit.getId().equals(candidateUnit.getId());
    }

    private static boolean listsAsChild(Part parent, Part child) {
        for (Part listedChild : parent.getChildParts()) {
            if (pointsAt(listedChild, child)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a link, resolved or still as saved, is to this part. */
    private static boolean pointsAt(@Nullable Part link, Part part) {
        if (link == part) {
            return true;
        }
        boolean isSavedNumberLink = (link instanceof PartRef partRef) && (partRef.getLinkedUniqueId() == null);
        return isSavedNumberLink && (link.getId() == part.getId());
    }
}
