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

import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.MonthlySupportPointsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.PreventAerospaceEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.RevealTrackEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScanRangeEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScenarioOddsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.SharedModifiersEffect;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * A facility in the format used before 0.51.01: one side's view of one facility type, with its effects as flat fields
 * and the file of its other side named as its captured definition. Read from old-format definition files (a user's
 * own manifest may still list some), and filled from the copy each placed facility held in older saves.
 *
 * @author Illiani
 * @since 0.51.01
 */
class LegacyStratConFacilityData {
    ForceAlignment owner;
    String displayableName;
    FacilityType facilityType;
    String userDescription;
    List<String> sharedModifiers;
    List<String> localModifiers;
    String capturedDefinition;
    boolean revealTrack;
    boolean increaseScanRange;
    int scenarioOddsModifier;
    int monthlySPModifier;
    boolean preventAerospace;
    List<StratConBiome> biomes;

    /**
     * @return the effects the old fields describe, in the order the old rules applied them
     *
     * @author Illiani
     * @since 0.51.01
     */
    StratConFacilityProfile toProfile() {
        List<IStratConFacilityEffect> effects = new ArrayList<>();
        if ((localModifiers != null) && !localModifiers.isEmpty()) {
            effects.add(new LocalModifiersEffect(localModifiers));
        }
        if ((sharedModifiers != null) && !sharedModifiers.isEmpty()) {
            effects.add(new SharedModifiersEffect(sharedModifiers));
        }
        if (revealTrack) {
            effects.add(new RevealTrackEffect());
        }
        if (increaseScanRange) {
            effects.add(new ScanRangeEffect(1));
        }
        if (scenarioOddsModifier != 0) {
            effects.add(new ScenarioOddsEffect(scenarioOddsModifier));
        }
        if (monthlySPModifier != 0) {
            effects.add(new MonthlySupportPointsEffect(monthlySPModifier));
        }
        if (preventAerospace) {
            effects.add(new PreventAerospaceEffect());
        }
        return new StratConFacilityProfile(userDescription, effects);
    }

    /**
     * @return whether this data describes the facility as held by the player's side
     *
     * @author Illiani
     * @since 0.51.01
     */
    boolean isAllied() {
        return StratConFacilityDefinition.isAlliedToPlayer(owner);
    }

    /**
     * @param id the ID to give the definition
     *
     * @return a definition with only this side's profile
     *
     * @author Illiani
     * @since 0.51.01
     */
    StratConFacilityDefinition toDefinition(String id) {
        StratConFacilityProfile profile = toProfile();
        String name = (displayableName == null) ? String.valueOf(facilityType) : displayableName;
        StratConFacilityDefinition definition = new StratConFacilityDefinition(id,
              name,
              facilityType,
              isAllied() ? profile : null,
              isAllied() ? null : profile);
        if (biomes != null) {
            definition.setBiomes(biomes);
        }
        return definition;
    }
}
