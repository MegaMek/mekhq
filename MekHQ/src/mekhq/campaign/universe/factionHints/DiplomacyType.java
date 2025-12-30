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
package mekhq.campaign.universe.factionHints;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;

import java.text.MessageFormat;

import mekhq.campaign.universe.Faction;
import mekhq.utilities.ReportingUtilities;

/**
 * Represents the high-level diplomatic relationship categories that may exist between two {@link Faction} entities in
 * the campaign universe.
 *
 * <p>Each constant defines:</p>
 * <ul>
 *   <li>a lookup key used to retrieve localized text, and</li>
 *   <li>a default color value used for formatted output in UI contexts.</li>
 * </ul>
 *
 * <p>Diplomacy types are implemented as predefined styled values intended for daily report output formatting.
 * Display strings retrieved from resource bundles may contain format specifiers for injecting faction names and
 * color tags.</p>
 *
 * @author Illiani
 * @since 0.50.11
 */
public enum DiplomacyType {
    WAR("WAR", getNegativeColor()),
    ALLIANCE("ALLIANCE", getAmazingColor()),
    RIVALRY("RIVALRY", getWarningColor()),
    NEUTRALITY("NEUTRALITY", getPositiveColor());

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DiplomacyType";

    private final String lookupName;
    private final String displayText;
    private final String displayColor;

    /**
     * Constructs a new diplomacy type instance.
     *
     * @param lookupName   key suffix used when resolving localized text
     * @param displayColor base color value used to generate a UI presentation span
     *
     * @author Illiani
     * @since 0.50.11
     */
    DiplomacyType(String lookupName, String displayColor) {
        this.lookupName = lookupName;
        this.displayColor = ReportingUtilities.spanOpeningWithCustomColor(displayColor);
        this.displayText = generateDisplayText();
    }

    /**
     * Resolves and caches the localized diplomacy text associated with this type.
     *
     * @return localized base diplomacy descriptor text
     *
     * @author Illiani
     * @since 0.50.11
     */
    private String generateDisplayText() {
        return getTextAt(RESOURCE_BUNDLE,
              "DiplomacyType." + lookupName + ".text");
    }

    /**
     * Returns a formatted representation of this diplomacy relationship when applied between two factions.
     *
     * @param factionName1 the initiating or primary faction
     * @param factionName2 the counterpart or referenced faction
     *
     * @return fully formatted diplomacy display string suitable for the Daily Report
     *
     * @author Illiani
     * @since 0.50.11
     */
    public String getDisplayText(String factionName1, String factionName2) {
        return MessageFormat.format(displayText, factionName1, factionName2, displayColor, CLOSING_SPAN_TAG);
    }
}
