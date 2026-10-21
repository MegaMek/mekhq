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

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.events.RepairStatusChangedEvent;
import mekhq.campaign.mission.rentals.FacilityRentals;

/**
 * Moves units to another repair site, the one way both the Hangar's right-click menu and the start of a refit do it.
 * For example, moving a Wolverine into a maintenance facility first rents the bay when the campaign charges for repair
 * bays, and leaves the unit where it is if no bay can be had.
 */
public final class UnitSiteChange {
    private static final MMLogger LOGGER = MMLogger.create(UnitSiteChange.class);

    private UnitSiteChange() {}

    /**
     * Moves the units to a repair site. A maintenance facility or factory is rented first when the campaign charges
     * for repair bays, and the rental dialogs tell the player when none is available. Deployed units stay where they
     * are.
     *
     * @param campaign the campaign
     * @param units    the units to move
     * @param site     the repair site, one of the {@code Unit.SITE_*} constants below {@link Unit#SITE_UNKNOWN}
     *
     * @return {@code true} if the units were moved, {@code false} if the bay rental was refused
     */
    public static boolean moveToSite(Campaign campaign, Unit[] units, int site) {
        boolean isBayRented = (site >= Unit.SITE_FACILITY_MAINTENANCE)
              && (campaign.getCampaignOptions().get(CampaignOption.RENTED_FACILITIES_COST_REPAIR_BAYS) > 0);
        if (isBayRented && !FacilityRentals.processBayChangeRequest(campaign, units, site)) {
            LOGGER.debug("[RepairSite] {} units not moved to {}: the bay rental was refused", units.length,
                  Unit.getSiteName(site));
            return false;
        }
        int unitsMoved = 0;
        for (Unit unit : units) {
            if (!unit.isDeployed()) {
                unit.setSite(site);
                MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
                unitsMoved++;
            }
        }
        LOGGER.debug("[RepairSite] {} of {} units moved to {}", unitsMoved, units.length, Unit.getSiteName(site));
        return true;
    }
}
