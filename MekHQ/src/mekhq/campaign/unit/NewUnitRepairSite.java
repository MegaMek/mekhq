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

import static mekhq.campaign.unit.Unit.SITE_FACILITY_MAINTENANCE;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.utilities.ContractRepairLocation;
import mekhq.campaign.mission.rentals.FacilityRentals;

/**
 * Picks the repair site for a unit that has just joined the force: bought, added by the GM, salvaged or delivered. The
 * new unit goes where the rest of the force is being worked on, so if the player has rented Factory Conditions for the
 * whole force on a garrison contract, a salvaged Locust joins them there instead of sitting at a basic facility.
 *
 * <p>The force's site is the one most of its present, active units use; on a tie the better site wins. With no such
 * units the contract's own site applies. A rented site (maintenance bays or factory conditions) is only used where bays
 * can be rented; elsewhere the new unit gets the contract's site. A unit at a rented site pays its daily rent like the
 * rest of the force.</p>
 */
public final class NewUnitRepairSite {
    private static final MMLogger LOGGER = MMLogger.create(NewUnitRepairSite.class);

    private NewUnitRepairSite() {}

    /**
     * Returns the repair site for a unit joining the force now. Call it before the new unit is added to the hangar, so
     * the unit does not count itself.
     *
     * @param campaign the campaign the unit joins
     *
     * @return one of the {@code Unit.SITE_*} constants
     */
    public static int getSite(Campaign campaign) {
        return getSite(campaign, campaign.getPlayerForce().getHangar().getUnits());
    }

    /**
     * Returns the repair site for a unit joining the given units, such as a unit arriving at a base.
     *
     * @param campaign    the campaign the unit joins
     * @param hangarUnits the units already where the new unit arrives
     *
     * @return one of the {@code Unit.SITE_*} constants
     */
    public static int getSite(Campaign campaign, Collection<Unit> hangarUnits) {
        int contractSite = ContractRepairLocation.getBestRepairLocation(campaign.getActiveContracts());
        Integer forceSite = getMostUsedSite(hangarUnits);
        if (forceSite == null) {
            LOGGER.debug("[NewUnitSite] No active units in the hangar; using the contract site {}", contractSite);
            return contractSite;
        }

        boolean isRentedSite = forceSite >= SITE_FACILITY_MAINTENANCE;
        if (isRentedSite && !FacilityRentals.isBayRentalAllowed(campaign)) {
            LOGGER.debug("[NewUnitSite] The force uses rented site {}, but bays cannot be rented here; using the "
                  + "contract site {}", forceSite, contractSite);
            return contractSite;
        }

        LOGGER.debug("[NewUnitSite] Joining the force at site {}", forceSite);
        return forceSite;
    }

    /**
     * @return the site most present, active units use, the better site on a tie, or {@code null} if there are none
     */
    static @Nullable Integer getMostUsedSite(Collection<Unit> units) {
        Map<Integer, Integer> unitsBySite = new TreeMap<>();
        for (Unit unit : units) {
            if (unit.isPresent() && !unit.isMothballed()) {
                unitsBySite.merge(unit.getSite(), 1, Integer::sum);
            }
        }

        Integer mostUsedSite = null;
        int mostUnits = 0;
        // Sites are visited worst first, so a later site with as many units replaces an earlier one
        for (Map.Entry<Integer, Integer> siteCount : unitsBySite.entrySet()) {
            if (siteCount.getValue() >= mostUnits) {
                mostUsedSite = siteCount.getKey();
                mostUnits = siteCount.getValue();
            }
        }
        return mostUsedSite;
    }
}
