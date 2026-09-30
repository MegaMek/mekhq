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
import java.util.Collections;
import java.util.List;

import megamek.common.annotations.Nullable;

/**
 * What a facility type does while one side holds it: a description for the player and the effects that apply. A
 * {@link StratConFacilityDefinition} carries one profile for when the player's side holds the facility and one for
 * when the enemy does; capturing the facility swaps which one applies.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConFacilityProfile {
    private String description;
    private List<IStratConFacilityEffect> effects = new ArrayList<>();

    public StratConFacilityProfile() {
    }

    /**
     * @param description what the facility does, in the player's words; may be {@code null}
     * @param effects     the effects that apply while this profile does
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacilityProfile(@Nullable String description, List<IStratConFacilityEffect> effects) {
        this.description = description;
        this.effects = new ArrayList<>(effects);
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * @return the effects, never {@code null}
     */
    public List<IStratConFacilityEffect> getEffects() {
        return (effects == null) ? Collections.emptyList() : Collections.unmodifiableList(effects);
    }

    public void setEffects(List<IStratConFacilityEffect> effects) {
        this.effects = new ArrayList<>(effects);
    }

    /**
     * @return every local scenario modifier ID the effects name, in order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public List<String> getLocalModifierIds() {
        List<String> modifierIds = new ArrayList<>();
        for (IStratConFacilityEffect effect : getEffects()) {
            modifierIds.addAll(effect.getLocalModifierIds());
        }
        return modifierIds;
    }

    /**
     * @return every shared scenario modifier ID the effects name, in order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public List<String> getSharedModifierIds() {
        List<String> modifierIds = new ArrayList<>();
        for (IStratConFacilityEffect effect : getEffects()) {
            modifierIds.addAll(effect.getSharedModifierIds());
        }
        return modifierIds;
    }

    /**
     * @return the total scan range increase of the effects
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getScanRangeIncrease() {
        int total = 0;
        for (IStratConFacilityEffect effect : getEffects()) {
            total += effect.getScanRangeIncrease();
        }
        return total;
    }

    /**
     * @return the total scenario odds modifier of the effects
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getScenarioOddsModifier() {
        int total = 0;
        for (IStratConFacilityEffect effect : getEffects()) {
            total += effect.getScenarioOddsModifier();
        }
        return total;
    }

    /**
     * @return the total monthly support points of the effects
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getMonthlySupportPoints() {
        int total = 0;
        for (IStratConFacilityEffect effect : getEffects()) {
            total += effect.getMonthlySupportPoints();
        }
        return total;
    }

    /**
     * @return whether any effect reveals the sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isRevealingTrack() {
        for (IStratConFacilityEffect effect : getEffects()) {
            if (effect.isRevealingTrack()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether any effect keeps air and space scenarios out of the sector
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isPreventingAerospace() {
        for (IStratConFacilityEffect effect : getEffects()) {
            if (effect.isPreventingAerospace()) {
                return true;
            }
        }
        return false;
    }
}
