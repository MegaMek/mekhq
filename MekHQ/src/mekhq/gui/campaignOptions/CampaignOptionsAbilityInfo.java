/*
 * Copyright (c) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.campaignOptions;

import mekhq.campaign.personnel.SpecialAbility;

/**
 * The {@code AbilityInfo} class represents information about a specific ability,
 * encapsulating its name, the associated {@link SpecialAbility}, its active status,
 * and its category.
 */
public class CampaignOptionsAbilityInfo {
    private String name;
    private SpecialAbility ability;
    private boolean isEnabled;
    private AbilityCategory category;

    /**
     * Enum {@code AbilityCategory} represents the categories abilities can belong to.
     * Categories available:
     * <ul>
     *     <li>{@code COMBAT_ABILITIES}: Abilities related to combat actions</li>
     *     <li>{@code MANEUVERING_ABILITIES}: Abilities related to movement and maneuvering</li>
     *     <li>{@code UTILITY_ABILITIES}: Abilities providing utility or non-combat benefits</li>
     * </ul>
     */
    public enum AbilityCategory {
        COMBAT_ABILITY, MANEUVERING_ABILITY, UTILITY_ABILITY
    }

    /**
     * Constructs an {@code AbilityInfo} object with all fields initialized.
     *
     * @param name      the name of the ability
     * @param ability   the {@link SpecialAbility} associated with this ability
     * @param isEnabled {@code true} if the ability is enabled, otherwise {@code false}
     * @param category  the category of the ability, represented as an {@link AbilityCategory}
     */
    public CampaignOptionsAbilityInfo(String name, SpecialAbility ability, boolean isEnabled, AbilityCategory category) {
        this.name = name;
        this.ability = ability;
        this.isEnabled = isEnabled;
        this.category = category;
    }

    /**
     * Returns the name of the ability.
     *
     * @return the name of the ability
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the ability.
     *
     * @param name the new name of the ability
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the {@link SpecialAbility} object associated with this ability.
     *
     * @return the associated {@link SpecialAbility}
     */
    public SpecialAbility getAbility() {
        return ability;
    }

    /**
     * Sets the {@link SpecialAbility} object associated with this ability.
     *
     * @param ability the new {@link SpecialAbility} object to associate
     */
    public void setAbility(SpecialAbility ability) {
        this.ability = ability;
    }

    /**
     * Returns whether the ability is enabled or active.
     *
     * @return {@code true} if the ability is enabled, otherwise {@code false}
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Sets the enabled/active status of the ability.
     *
     * @param enabled {@code true} to enable the ability, {@code false} to disable it
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * Returns the category of the ability.
     *
     * @return the {@link AbilityCategory} of the ability
     */
    public AbilityCategory getCategory() {
        return category;
    }

    /**
     * Sets the category of the ability.
     *
     * @param category the new {@link AbilityCategory} for the ability
     */
    public void setCategory(AbilityCategory category) {
        this.category = category;
    }

    /**
     * Returns a string representation of the ability, displaying only its name.
     *
     * @return the name of the ability
     */
    @Override
    public String toString() {
        return name;
    }
}
