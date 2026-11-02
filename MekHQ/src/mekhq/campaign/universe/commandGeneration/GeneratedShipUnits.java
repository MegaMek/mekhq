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

import jakarta.annotation.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.AutomaticRankAssigner;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

/**
 * Turns the ship designs a generation stage drew into campaign units: added to the hangar, crewed, and priced like
 * anything else the build produces, so a player who asks for lift pays for the hulls that give it.
 */
final class GeneratedShipUnits {

    private static final MMLogger LOGGER = MMLogger.create(GeneratedShipUnits.class);

    private GeneratedShipUnits() {
    }

    /**
     * Adds one hull to the hangar with a crew. The crew are put on the command's faction's rank system, as the rest
     * of the command's personnel are; they hold no rank, so this shows only in the rank system the roster offers for
     * them.
     *
     * @param campaign    the campaign that receives the ship
     * @param hull        the design
     * @param factionCode the faction the command is generated for, or {@code null} for none, in which case the crew
     *                    keep the campaign's rank system
     * @param logTag      the stage's log tag, so a failure reads as part of that stage
     *
     * @return the new unit, or {@code null} when the design could not be loaded
     */
    static @Nullable Unit addCrewedShip(Campaign campaign, MekSummary hull, @Nullable String factionCode,
          String logTag) {
        try {
            PartQuality quality = campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_UNIT_QUALITIES)
                  ? UnitOrder.getRandomUnitQuality(0)
                  : PartQuality.QUALITY_D;
            // allowNewPilots = true: the ship arrives crewed, like every other generated unit.
            Unit ship = campaign.addNewUnit(hull.loadEntity(), true, 0, quality);
            if (ship != null) {
                putCrewOnFactionRankSystem(ship, factionFor(factionCode), logTag);
            }
            return ship;
        } catch (Exception exception) {
            LOGGER.error(exception, "{} could not load ship '{}' from {}", logTag, hull.getName(),
                  hull.getSourceFile());
            return null;
        }
    }

    /**
     * Adds every hull that loads, skipping and logging the ones that do not.
     *
     * @param campaign    the campaign that receives the ships
     * @param hulls       the designs, in the order they were drawn
     * @param factionCode the faction the command is generated for, or {@code null} for none
     * @param logTag      the stage's log tag
     *
     * @return the new units, in the same order less any that failed to load
     */
    static List<Unit> addCrewedShips(Campaign campaign, List<MekSummary> hulls, @Nullable String factionCode,
          String logTag) {
        List<Unit> ships = new ArrayList<>();
        for (MekSummary hull : hulls) {
            Unit ship = addCrewedShip(campaign, hull, factionCode, logTag);
            if (ship != null) {
                ships.add(ship);
            }
        }
        return ships;
    }

    /**
     * Puts a ship's crew on the faction's rank system, so the rank system the roster offers for them matches the
     * command's other personnel. The crew's ranks are left as they are.
     *
     * @param ship    the crewed ship
     * @param faction the faction the command is generated for, or {@code null} to leave the crew as they are
     * @param logTag  the stage's log tag
     */
    static void putCrewOnFactionRankSystem(Unit ship, @Nullable Faction faction, String logTag) {
        List<Person> crew = ship.getCrew();
        int moved = AutomaticRankAssigner.putOnFactionRankSystem(crew, faction);
        LOGGER.debug("{} '{}': {} of {} crew moved to the rank system of {}, as the command's other personnel are",
              logTag, ship.getName(), moved, crew.size(), (faction == null) ? "no faction" : faction.getShortName());
    }

    /**
     * @param factionCode a faction code, or {@code null}
     *
     * @return the faction, or {@code null} when no code was given
     */
    private static @Nullable Faction factionFor(@Nullable String factionCode) {
        return (factionCode == null) ? null : Factions.getInstance().getFaction(factionCode);
    }
}
