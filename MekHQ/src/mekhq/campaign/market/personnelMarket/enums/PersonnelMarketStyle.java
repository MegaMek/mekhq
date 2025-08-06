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

/**
 * Enum representing various recruitment styles for the Personnel Market.
 *
 * <p>Each style defines specific files for Clan and Inner Sphere market configurations.</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public enum PersonnelMarketStyle {
    PERSONNEL_MARKET_DISABLED("", ""),
    MEKHQ("clanMarketMekHQ.yaml", "innerSphereMarketMekHQ.yaml"),
    CAMPAIGN_OPERATIONS_REVISED("clanMarketCamOpsRevised.yaml", "innerSphereMarketCamOpsRevised.yaml"),
    CAMPAIGN_OPERATIONS_STRICT("clanMarketCamOpsStrict.yaml", "innerSphereMarketCamOpsStrict.yaml");

    final private static String RESOURCE_BUNDLE = "mekhq.resources." + PersonnelMarketStyle.class.getSimpleName();

    private final String fileNameClan;
    private final String fileNameInnerSphere;

    /**
     * Constructor for the {@link PersonnelMarketStyle} enum.
     *
     * @param fileNameClan        filename for the Clan personnel market configuration
     * @param fileNameInnerSphere filename for the Inner Sphere personnel market configuration
     *
     * @author Illiani
     * @since 0.50.06
     */
    PersonnelMarketStyle(String fileNameClan, String fileNameInnerSphere) {
        this.fileNameClan = fileNameClan;
        this.fileNameInnerSphere = fileNameInnerSphere;
    }

    /**
     * Gets the filename for the Clan market configuration.
     *
     * @return the Clan market configuration filename
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getFileNameClan() {
        return fileNameClan;
    }

    /**
     * Gets the filename for the Inner Sphere market configuration.
     *
     * @return the Inner Sphere market configuration filename
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getFileNameInnerSphere() {
        return fileNameInnerSphere;
    }

    /**
     * Returns the {@link PersonnelMarketStyle} corresponding to the provided text.
     *
     * <p>Tries to match by enum name, string value, or ordinal/index. If resolution fails,
     * {@link #PERSONNEL_MARKET_DISABLED}
     * is returned and an error is logged.</p>
     *
     * @param text the {@link String} representation or ordinal of the market style
     *
     * @return the corresponding {@link PersonnelMarketStyle} or {@link #PERSONNEL_MARKET_DISABLED} if unknown
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static PersonnelMarketStyle fromString(String text) {
        try {
            // Attempt to parse as a string with case/space adjustments.
            return PersonnelMarketStyle.valueOf(text.trim().toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        try {
            // Attempt to parse as a string based on the return value of toString().
            for (PersonnelMarketStyle style : PersonnelMarketStyle.values()) {
                if (style.toString().equalsIgnoreCase(text)) {
                    return style;
                }
            }
            return PersonnelMarketStyle.values()[MathUtility.parseInt(text)];
        } catch (Exception ignored) {
        }

        try {
            // Attempt to parse as an integer and use as ordinal.
            return PersonnelMarketStyle.values()[MathUtility.parseInt(text)];
        } catch (Exception ignored) {
        }

        // Log error if parsing fails and return default value.
        MMLogger logger = MMLogger.create(PersonnelMarketStyle.class);
        logger.error("Unknown PersonnelMarketStyle ordinal: {} - returning {}.", text, PERSONNEL_MARKET_DISABLED);

        return PERSONNEL_MARKET_DISABLED;
    }

    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, name() + ".label");
    }
}
