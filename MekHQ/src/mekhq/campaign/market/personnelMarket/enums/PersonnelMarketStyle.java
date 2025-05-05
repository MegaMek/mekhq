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
package mekhq.campaign.market.personnelMarket.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

public enum PersonnelMarketStyle {
    NONE("", ""),
    MEKHQ("clanMarketMekHQ.yaml", "innerSphereMarketMekHQ.yaml"),
    CAMPAIGN_OPERATIONS("clanMarketCamOps.yaml", "innerSphereMarketCamOps.yaml");

    final private static String RESOURCE_BUNDLE = "mekhq.resources." + PersonnelMarketStyle.class.getSimpleName();

    private final String fileNameClan;
    private final String fileNameInnerSphere;

    PersonnelMarketStyle(String fileNameClan, String fileNameInnerSphere) {
        this.fileNameClan = fileNameClan;
        this.fileNameInnerSphere = fileNameInnerSphere;
    }

    public String getFileNameClan() {
        return fileNameClan;
    }

    public String getFileNameInnerSphere() {
        return fileNameInnerSphere;
    }

    public static PersonnelMarketStyle fromString(String text) {
        try {
            // Attempt to parse as string with case/space adjustments.
            return PersonnelMarketStyle.valueOf(text.trim().toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        try {
            // Attempt to parse as an integer and use as ordinal.
            return PersonnelMarketStyle.values()[MathUtility.parseInt(text)];
        } catch (Exception ignored) {
        }

        // Log error if parsing fails and return default value.
        MMLogger logger = MMLogger.create(PersonnelMarketStyle.class);
        logger.error("Unknown PersonnelMarketStyle ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }

    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, name() + ".label");
    }
}
