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
package mekhq.campaign.parts;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;

/**
 * Decides whether a tech can work on a refit. The refit screen asks it before a refit starts, and the daily refit work
 * asks it again before each day's work, so a refit never goes ahead with a tech who cannot do the job.
 *
 * <p>A tech cannot work on a refit when they are not where the unit is (the same test crew assignment uses), or when
 * the refit's target number is Impossible (for example the wrong type of tech, or no tool kit when tool kits are
 * required).</p>
 */
public final class RefitWorkCheck {
    private static final MMLogger LOGGER = MMLogger.create(RefitWorkCheck.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    private RefitWorkCheck() {
    }

    /**
     * @param campaign the campaign the refit belongs to
     * @param refit    the refit to work on
     * @param tech     the tech who would do the work
     *
     * @return {@code null} if the tech can work on the refit, otherwise the reason they cannot
     */
    public static @Nullable String reasonTechCannotWork(Campaign campaign, Refit refit, Person tech) {
        if (!LocationUtils.areSameEffectiveLocation(refit.getUnit(), tech)) {
            LOGGER.debug("[Refit] {} cannot work on {}: not at the unit's location", tech.getFullName(),
                  refit.getDesc());
            return getFormattedTextAt(RESOURCE_BUNDLE, "RefitWorkCheck.notAtUnitLocation");
        }

        TargetRoll target = campaign.getTargetFor(refit, tech);
        if (target.getValue() == TargetRoll.IMPOSSIBLE) {
            LOGGER.debug("[Refit] {} cannot work on {}: {}", tech.getFullName(), refit.getDesc(), target.getDesc());
            return target.getDesc();
        }
        return null;
    }
}
