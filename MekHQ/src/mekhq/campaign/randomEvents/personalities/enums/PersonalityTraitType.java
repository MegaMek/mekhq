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
package mekhq.campaign.randomEvents.personalities.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Represents the four primary types of personality characteristics a character can possess.
 *
 * <p>This enum is used to categorize and manage the key personality traits
 * of a character, which include:</p>
 * <ul>
 *     <li>{@link #AGGRESSION} - Reflecting the character's tendency toward hostility or
 *     assertiveness.</li>
 *     <li>{@link #AMBITION} - Representing the character's drive to achieve goals or power.</li>
 *     <li>{@link #GREED} - Indicating the character's desire for wealth or material possessions.</li>
 *     <li>{@link #SOCIAL} - Showcasing the character's inclination towards sociability and
 *     interpersonal relationships.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.06
 */
public enum PersonalityTraitType {
    AGGRESSION,
    AMBITION,
    GREED,
    SOCIAL,
    REASONING,
    PERSONALITY_QUIRK;

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    /**
     * Retrieves the formatted label text associated with this enum instance.
     *
     * <p>Constructs a resource key using the enum's name and the ".label" suffix,
     * then fetches the corresponding label from the resource bundle.</p>
     *
     * @return the formatted label string for this enum instance.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }
}
