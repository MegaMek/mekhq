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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.MonthlySupportPointsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.PreventAerospaceEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.RevealTrackEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScanRangeEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScenarioOddsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.SharedModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.UnknownEffect;

/**
 * One effect a facility has on play, as listed in a {@link StratConFacilityProfile}. Each effect answers the hooks it
 * cares about and leaves the rest at their neutral defaults, so the rules can simply total every effect in a profile.
 *
 * <p>Effects are read from facility definition files by their {@code effect} name. A name this version does not know
 * loads as an {@link UnknownEffect}, which does nothing, rather than failing the whole file.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "effect", defaultImpl = UnknownEffect.class)
@JsonSubTypes({
      @JsonSubTypes.Type(value = LocalModifiersEffect.class, name = LocalModifiersEffect.NAME),
      @JsonSubTypes.Type(value = SharedModifiersEffect.class, name = SharedModifiersEffect.NAME),
      @JsonSubTypes.Type(value = ScanRangeEffect.class, name = ScanRangeEffect.NAME),
      @JsonSubTypes.Type(value = ScenarioOddsEffect.class, name = ScenarioOddsEffect.NAME),
      @JsonSubTypes.Type(value = MonthlySupportPointsEffect.class, name = MonthlySupportPointsEffect.NAME),
      @JsonSubTypes.Type(value = RevealTrackEffect.class, name = RevealTrackEffect.NAME),
      @JsonSubTypes.Type(value = PreventAerospaceEffect.class, name = PreventAerospaceEffect.NAME)
})
public interface IStratConFacilityEffect {

    /**
     * @return scenario modifier IDs applied to scenarios fought on the facility itself
     *
     * @author Illiani
     * @since 0.51.01
     */
    default List<String> getLocalModifierIds() {
        return Collections.emptyList();
    }

    /**
     * @return scenario modifier IDs the facility may lend to scenarios elsewhere in its sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    default List<String> getSharedModifierIds() {
        return Collections.emptyList();
    }

    /**
     * @return hexes added to the scan range of every force scouting the facility's sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    default int getScanRangeIncrease() {
        return 0;
    }

    /**
     * @return the change to the scenario odds of the facility's sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    default int getScenarioOddsModifier() {
        return 0;
    }

    /**
     * @return support points gained (or lost, if negative) at the start of each month
     *
     * @author Illiani
     * @since 0.51.01
     */
    default int getMonthlySupportPoints() {
        return 0;
    }

    /**
     * @return whether the facility reveals its whole sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    default boolean isRevealingTrack() {
        return false;
    }

    /**
     * @return whether the facility keeps air and space scenarios out of its sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    default boolean isPreventingAerospace() {
        return false;
    }
}
