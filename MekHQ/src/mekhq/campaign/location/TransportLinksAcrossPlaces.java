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
package mekhq.campaign.location;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.unit.ITransportAssignment;
import mekhq.campaign.unit.Unit;

/**
 * Releases the transport links that join units at different places. A unit can only ride in, or be towed by, a carrier
 * at the same place, so when a unit is sent away without its carrier, or a carrier without its cargo, the cargo is
 * unloaded and stays behind.
 */
public final class TransportLinksAcrossPlaces {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    private TransportLinksAcrossPlaces() {}

    /**
     * Unloads every unit that rides in, or is towed by, a carrier at a different place, looking at the given units both
     * as cargo and as carriers. Each broken link is reported in the daily report.
     *
     * @param campaign the campaign
     * @param units    the units to check, such as the units just dispatched
     */
    public static void release(Campaign campaign, Collection<Unit> units) {
        for (CampaignTransportType campaignTransportType : CampaignTransportType.values()) {
            Set<Unit> cargoToUnload = new LinkedHashSet<>();
            for (Unit unit : units) {
                ITransportAssignment assignment = unit.getTransportAssignment(campaignTransportType);
                Unit carrier = (assignment == null) ? null : assignment.getTransport();
                if ((carrier != null) && !LocationUtils.areSameEffectiveLocation(unit, carrier)) {
                    cargoToUnload.add(unit);
                }
                for (Unit cargo : unit.getTransportedUnits(campaignTransportType)) {
                    if (!LocationUtils.areSameEffectiveLocation(unit, cargo)) {
                        cargoToUnload.add(cargo);
                    }
                }
            }
            unload(campaign, campaignTransportType, cargoToUnload);
        }
    }

    private static void unload(Campaign campaign, CampaignTransportType campaignTransportType,
          Collection<Unit> cargoToUnload) {
        Set<Unit> carriersToUpdate = new LinkedHashSet<>();
        List<Unit> changedUnits = new ArrayList<>();
        for (Unit cargo : cargoToUnload) {
            Unit carrier = cargo.unloadFromTransport(campaignTransportType);
            if (carrier == null) {
                continue;
            }
            carriersToUpdate.add(carrier);
            changedUnits.add(cargo);
            campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "TransportLinksAcrossPlaces.unloaded.report",
                  cargo.getHyperlinkedName(),
                  carrier.getHyperlinkedName()));
        }

        for (Unit carrier : carriersToUpdate) {
            carrier.initializeTransportSpace(campaignTransportType);
            campaign.updateTransportInTransports(campaignTransportType, carrier);
            if (!changedUnits.contains(carrier)) {
                changedUnits.add(carrier);
            }
        }

        for (Unit changedUnit : changedUnits) {
            MekHQ.triggerEvent(new UnitChangedEvent(changedUnit));
        }
    }
}
