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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;

/**
 * Turns the ship designs a generation stage drew into campaign units: added to the hangar, crewed, and priced like
 * anything else the build produces, so a player who asks for lift pays for the hulls that give it.
 */
final class GeneratedShipUnits {

    private static final MMLogger LOGGER = MMLogger.create(GeneratedShipUnits.class);

    private GeneratedShipUnits() {
    }

    /**
     * Adds one hull to the hangar with a crew.
     *
     * @param campaign the campaign that receives the ship
     * @param hull     the design
     * @param logTag   the stage's log tag, so a failure reads as part of that stage
     *
     * @return the new unit, or {@code null} when the design could not be loaded
     */
    static @Nullable Unit addCrewedShip(Campaign campaign, MekSummary hull, String logTag) {
        try {
            PartQuality quality = campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_UNIT_QUALITIES)
                  ? UnitOrder.getRandomUnitQuality(0)
                  : PartQuality.QUALITY_D;
            // allowNewPilots = true: the ship arrives crewed, like every other generated unit.
            return campaign.addNewUnit(hull.loadEntity(), true, 0, quality);
        } catch (Exception exception) {
            LOGGER.error(exception, "{} could not load ship '{}' from {}", logTag, hull.getName(),
                  hull.getSourceFile());
            return null;
        }
    }

    /**
     * Adds every hull that loads, skipping and logging the ones that do not.
     *
     * @param campaign the campaign that receives the ships
     * @param hulls    the designs, in the order they were drawn
     * @param logTag   the stage's log tag
     *
     * @return the new units, in the same order less any that failed to load
     */
    static List<Unit> addCrewedShips(Campaign campaign, List<MekSummary> hulls, String logTag) {
        List<Unit> ships = new ArrayList<>();
        for (MekSummary hull : hulls) {
            Unit ship = addCrewedShip(campaign, hull, logTag);
            if (ship != null) {
                ships.add(ship);
            }
        }
        return ships;
    }
}
