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
package mekhq.campaign.personnel.training;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * Why a {@link TrainingPurchase} can't be made.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum TrainingRefusal {
    /** The person isn't active, so they can't train. */
    NOT_ACTIVE,
    /** The campaign doesn't use this kind of purchase, or the skill or ability doesn't exist. */
    DISABLED,
    /** The person doesn't have the skill or flaw the purchase needs. */
    NOT_HELD,
    /** The person already has the ability or aptitude. */
    ALREADY_HELD,
    /** The skill, attribute or trait can't go any further in that direction. */
    AT_LIMIT,
    /** The person doesn't have enough XP. */
    NOT_ENOUGH_XP,
    /** The person doesn't meet the ability's prerequisites. */
    NOT_ELIGIBLE,
    /** The person already has as many abilities as the campaign allows. */
    SPA_CAP,
    /** Only the GM can grant this ability. */
    GM_ONLY,
    /** The ability needs a choice, such as a weapon, and none was given or none fits this person. */
    NO_CHOICE,
    /** There is nothing to refill. */
    NOT_NEEDED,
    /** The campaign can't pay the training fees for everything staged. */
    NOT_ENOUGH_FUNDS;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.TrainingConsole";

    /**
     * @return a short explanation for the player, such as "Not enough XP"
     */
    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, "TrainingRefusal." + name());
    }
}
