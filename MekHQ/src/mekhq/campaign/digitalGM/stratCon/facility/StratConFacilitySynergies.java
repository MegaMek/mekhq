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
package mekhq.campaign.digitalGM.stratCon.facility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;

/**
 * Facility pairings that do more together. When a facility has a partner held by the same side in the same sector, a
 * fight at the facility draws on the partner's shared modifiers too, whether or not the partner would have lent them
 * anyway. The pairings are an Artillery Base with a Command Center, an Air Base with an Early Warning System, and a Data
 * Center with any Mek, Tank or Air Base, or a Base of Operations. A crippled facility, or one cut off from its supply
 * lines, is no partner.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilitySynergies {
    private static final Set<FacilityType> BASE_TYPES = Set.of(FacilityType.MekBase,
          FacilityType.TankBase,
          FacilityType.AirBase,
          FacilityType.BaseOfOperations);

    private StratConFacilitySynergies() {}

    /**
     * @param first  a facility type
     * @param second another facility type
     *
     * @return {@code true} if the two types are partners, in either order
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isPairing(FacilityType first, FacilityType second) {
        return isOneWayPairing(first, second) || isOneWayPairing(second, first);
    }

    private static boolean isOneWayPairing(FacilityType first, FacilityType second) {
        return ((first == FacilityType.ArtilleryBase) && (second == FacilityType.CommandCenter))
                     || ((first == FacilityType.AirBase) && (second == FacilityType.EarlyWarningSystem))
                     || ((first == FacilityType.DataCenter) && BASE_TYPES.contains(second));
    }

    /**
     * @param track  the sector
     * @param coords a facility's hex
     *
     * @return the facilities in the sector partnering the facility there, in no particular order; empty if there is no
     *       facility there, or it is crippled or cut off itself
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConFacility> getPartners(StratConTrackState track, StratConCoords coords) {
        List<StratConFacility> partners = new ArrayList<>();
        StratConFacility facility = track.getFacility(coords);
        if ((facility == null) || !isAbleToPartner(track, coords, facility)) {
            return partners;
        }

        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            StratConFacility candidate = entry.getValue();
            if ((candidate != facility)
                      && (candidate.isOwnerAlliedToPlayer() == facility.isOwnerAlliedToPlayer())
                      && isPairing(facility.getFacilityType(), candidate.getFacilityType())
                      && isAbleToPartner(track, entry.getKey(), candidate)) {
                partners.add(candidate);
            }
        }
        return partners;
    }

    private static boolean isAbleToPartner(StratConTrackState track, StratConCoords coords,
          StratConFacility facility) {
        return (facility.getFacilityType() != null)
                     && (facility.getCondition() != FacilityCondition.CRIPPLED)
                     && !StratConFacilitySupply.isCutOff(track, coords);
    }
}
