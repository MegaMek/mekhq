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
package mekhq.utilities.spaUtilities.enums;

/**
 * Enum {@code AbilityCategory} represents the categories abilities can belong to. Categories available:
 * <ul>
 *     <li>{@code COMBAT_ABILITIES}: Abilities related to combat actions</li>
 *     <li>{@code MANEUVERING_ABILITIES}: Abilities related to movement and maneuvering</li>
 *     <li>{@code UTILITY_ABILITIES}: Abilities providing utility or non-combat benefits</li>
 *     <li>{@code CHARACTER_FLAW}: Abilities that inflict penalties in exchange for XP</li>
 *     <li>{@code CHARACTER_CREATION_ONLY}: Abilities that can only be selected when the character is created</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.06
 */
public enum AbilityCategory {
    COMBAT_ABILITY, MANEUVERING_ABILITY, UTILITY_ABILITY, CHARACTER_FLAW, CHARACTER_CREATION_ONLY
}
