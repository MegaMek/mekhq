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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import mekhq.campaign.unit.Unit;

/**
 * The repair site a refit needs, by Campaign Operations p.211: Class A and B refits are field work that needs nothing
 * beyond a crane and standard tools, Class C and D need a maintenance facility such as a transport bay or field
 * workshop, and Class E and F, like a refurbishment, can only be done at a factory.
 *
 * <p>For example, refitting a Wolverine WVR-6R to the WVR-6M is Class C work, so a unit in an improvised site
 * should move to at least a field workshop first.</p>
 */
public final class RefitFacility {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    private RefitFacility() {}

    /**
     * @param refit a planned refit
     *
     * @return the lowest repair site the refit can be done at, one of the {@code Unit.SITE_*} constants
     */
    public static int getRequiredSite(Refit refit) {
        if (refit.isBeingRefurbished()) {
            return Unit.SITE_FACTORY_CONDITIONS;
        }
        return getRequiredSite(refit.getRefitClass());
    }

    /**
     * @param refitClass a refit class, one of the {@code Refit.CLASS_*} constants
     *
     * @return the lowest repair site work of that class can be done at
     */
    static int getRequiredSite(int refitClass) {
        return switch (refitClass) {
            case Refit.CLASS_C, Refit.CLASS_D -> Unit.SITE_FIELD_WORKSHOP;
            case Refit.CLASS_E, Refit.CLASS_F -> Unit.SITE_FACTORY_CONDITIONS;
            default -> Unit.SITE_IMPROVISED;
        };
    }

    /**
     * @param refit a planned refit
     * @param site  the repair site the unit is at, one of the {@code Unit.SITE_*} constants
     *
     * @return {@code true} if the refit can be done at that site
     */
    public static boolean isSiteGoodEnough(Refit refit, int site) {
        // An unknown site counts as too low, so the player is asked to set one
        return (site < Unit.SITE_UNKNOWN) && (site >= getRequiredSite(refit));
    }

    /**
     * @param refit a planned refit
     *
     * @return the site the refit needs, for example {@code Field Workshop or better}, or {@code Any site}
     */
    public static String describeRequiredSite(Refit refit) {
        int requiredSite = getRequiredSite(refit);
        if (requiredSite == Unit.SITE_IMPROVISED) {
            return getTextAt(RESOURCE_BUNDLE, "RefitFacility.anySite");
        }
        if (requiredSite == Unit.SITE_FACTORY_CONDITIONS) {
            return Unit.getSiteName(requiredSite);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "RefitFacility.orBetter", Unit.getSiteName(requiredSite));
    }
}
