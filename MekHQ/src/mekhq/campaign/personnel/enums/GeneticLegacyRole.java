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
package mekhq.campaign.personnel.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.logging.MMLogger;

/**
 * How a Bloodnamed warrior's genetic legacy is used in their Clan's breeding program.
 *
 * <p>A warrior whose legacy is in use contributes it to a sibko either as the genefather, whose DNA
 * goes into the sperm, or as the genemother, whose DNA goes into the egg. Neither role follows from
 * the warrior's own sex - Clan scientists implant the DNA of either sex into either cell - so this is
 * deliberately independent of {@link mekhq.campaign.personnel.Person#getGender()}.</p>
 *
 * <p>The distinction carries a rule with it: a trueborn may only compete for the Bloodname of their
 * genemother. That is the same fact MekHQ already models as a warrior's Bloodhouse, so a trueborn's
 * Bloodhouse is by definition their genemother's Bloodname.</p>
 */
public enum GeneticLegacyRole {
    /** This warrior's legacy is not currently in use in the breeding program. */
    NONE,

    /** This warrior's DNA is used in the sperm used to produce a sibko's trueborn children. */
    GENEFATHER,

    /** This warrior's DNA is used in the egg used to produce a sibko's trueborn children. */
    GENEMOTHER;

    private static final MMLogger LOGGER = MMLogger.create(GeneticLegacyRole.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GeneticLegacyRole";

    /**
     * @return the localized name of this role, for display
     */
    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, name() + ".label");
    }

    /**
     * @return {@code true} when this warrior's legacy is in use in the breeding program
     */
    public boolean isInUse() {
        return this != NONE;
    }

    /**
     * Reads a role back from a saved campaign.
     *
     * <p>An unreadable or absent value means {@link #NONE}. Campaigns saved before this was recorded
     * simply have no entry, and a warrior whose legacy is not in use is the ordinary case anyway.</p>
     *
     * @param text the stored value; may be {@code null} or empty
     *
     * @return the matching role, or {@link #NONE}
     */
    public static GeneticLegacyRole parseFromString(final String text) {
        if ((text == null) || text.isBlank()) {
            return NONE;
        }

        try {
            return valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            LOGGER.warn("Unknown GeneticLegacyRole '{}'; treating the legacy as not in use", text);
            return NONE;
        }
    }
}
