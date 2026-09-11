/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.Version;
import mekhq.MHQConstants;

/**
 * Shared starting points for the Life Path tests.
 *
 * <p>A {@link LifePath} needs all 55 of its components before its constructor will accept it, so almost every test
 * starts from a builder that is already complete and changes the one thing it is about.</p>
 *
 * @since 0.50.11
 */
final class LifePathTestFixtures {
    /** A version older than anything {@link LifePath} will load. */
    static final Version ANCIENT_VERSION = new Version("0.1.0");

    /** A version newer than anything {@link LifePath} will load. */
    static final Version FUTURE_VERSION = new Version("99.0.0");

    private LifePathTestFixtures() {
        // Utility class: not instantiable.
    }

    /**
     * Returns a builder that will produce a valid, empty Life Path.
     *
     * <p>Every scalar is set and every collection is empty, so a test can change exactly the component it is
     * interested in and know nothing else is at fault.</p>
     *
     * @return a complete builder
     *
     * @since 0.50.11
     */
    static LifePathBuilder validBuilder() {
        return new LifePathBuilder().id(UUID.randomUUID())
                     .version(MHQConstants.VERSION)
                     .xpCost(0)
                     .source("Test Source")
                     .name("Test Life Path")
                     .flavorText("Flavor text.")
                     .age(1)
                     .xpDiscount(0)
                     .minimumYear(3000)
                     .maximumYear(3100)
                     .randomWeight(1.0)
                     .lifeStages(Set.of(ATOWLifeStage.EARLY_CHILDHOOD))
                     .categories(Set.of(LifePathCategory.GENERAL_INNER_SPHERE))
                     .isPlayerRestricted(false)
                     .flexibleXPPickCount(0);
    }

    /**
     * Returns a valid, empty Life Path.
     *
     * @return the Life Path
     *
     * @since 0.50.11
     */
    static LifePath validLifePath() {
        return validBuilder().build();
    }

    /**
     * Returns a single-group map holding one skill award.
     *
     * @param groupIndex the group to put it in
     * @param skillName  the skill's name
     * @param amount     the XP awarded
     *
     * @return the group map
     *
     * @since 0.50.11
     */
    static Map<Integer, Map<String, Integer>> skillGroup(int groupIndex, String skillName, int amount) {
        return Map.of(groupIndex, Map.of(skillName, amount));
    }
}
