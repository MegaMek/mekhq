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
package mekhq.gui.companyGeneration.components;

/**
 * Shared helpers for the Company Generation dialog's styled components.
 *
 * <p>This is the Company Generation counterpart to {@code CampaignOptionsUtilities}. The two packages
 * share their visual conventions but each reads from its own resource bundle, so a key like
 * {@code "lblSparesArmor.text"} lives next to the dialog that uses it instead of polluting the larger
 * Campaign Options bundle.</p>
 */
public final class CompanyGenerationUtilities {

    /**
     * Resource bundle holding all {@code lbl*.text} / {@code lbl*.tooltip} / {@code lbl*.border} keys
     * read by the styled components in this package. Mirrors the {@code mekhq.resources.CompanyGenerationDialog}
     * properties file ({@code MekHQ/resources/mekhq/resources/CompanyGenerationDialog.properties}).
     */
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CompanyGenerationDialog";

    private CompanyGenerationUtilities() {
        // utility class
    }

    /**
     * Returns the resource-bundle identifier the styled components in this package read from. Use this
     * with {@link mekhq.utilities.MHQInternationalization#getTextAt(String, String)} the same way
     * {@code CampaignOptionsUtilities.getCampaignOptionsResourceBundle} is used in the Campaign Options
     * package.
     */
    public static String getCompanyGenerationResourceBundle() {
        return RESOURCE_BUNDLE;
    }
}
