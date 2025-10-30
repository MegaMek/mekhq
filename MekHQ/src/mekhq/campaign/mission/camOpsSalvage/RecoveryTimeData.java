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
package mekhq.campaign.mission.camOpsSalvage;

import static mekhq.campaign.mission.camOpsSalvage.RecoveryTimeCalculations.BASE_MULTIPLIER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Immutable data record containing recovery time calculation results and environmental multipliers.
 *
 * <p>This record encapsulates all environmental factors and their corresponding multipliers that affect
 * entity recovery time calculations, along with the base and final calculated recovery times. It is primarily used as a
 * return type from {@link RecoveryTimeCalculations} methods.</p>
 *
 * <p>The record provides a method to generate a human-readable breakdown of the recovery time calculation,
 * showing how each environmental factor contributes to the final result.</p>
 *
 * @param weatherMultiplier     The multiplier applied for weather conditions (e.g., storms, precipitation).
 * @param windMultiplier        The multiplier applied for wind conditions (e.g., tornadoes, hurricanes).
 * @param temperatureMultiplier The multiplier applied for extreme temperature conditions.
 * @param gravityMultiplier     The multiplier applied for non-standard gravity conditions.
 * @param atmosphereMultiplier  The multiplier applied for atmospheric conditions (e.g., vacuum, tainted air).
 * @param lightMultiplier       The multiplier applied for lighting conditions (e.g., darkness, solar flares).
 * @param baseRecoveryTime      The base recovery time in minutes before environmental modifiers are applied.
 * @param totalRecoveryTime     The final calculated recovery time in minutes after all multipliers are applied.
 *
 * @author Illiani
 * @see RecoveryTimeCalculations
 * @since 0.50.10
 */
public record RecoveryTimeData(
      double weatherMultiplier,
      double windMultiplier,
      double temperatureMultiplier,
      double gravityMultiplier,
      double atmosphereMultiplier,
      double lightMultiplier,
      double baseRecoveryTime,
      double totalRecoveryTime
) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    /**
     * Generates a detailed breakdown of the recovery time calculation as a formatted string.
     *
     * <p>This method constructs a human-readable string that displays:</p>
     * <ul>
     *   <li>The base recovery time</li>
     *   <li>Each environmental multiplier (weather, wind, temperature, gravity, atmosphere, light)</li>
     *   <li>The combined total multiplier</li>
     *   <li>The final total recovery time</li>
     * </ul>
     *
     * <p>All text is retrieved from the resource bundle for internationalization support. The output can optionally
     * be wrapped in HTML tags for display in UI components that support HTML rendering.</p>
     *
     * <p><strong>Example output (without HTML):</strong></p>
     * <pre>
     * Base Recovery Time: 60 minutes
     * Weather Multiplier: +0.25
     * Wind Multiplier: +0.00
     * Temperature Multiplier: +0.25
     * Gravity Multiplier: +0.00
     * Atmosphere Multiplier: +0.50
     * Light Multiplier: +0.25
     * Total Multiplier: 2.25
     * Total Recovery Time: 135 minutes
     * </pre>
     *
     * @param includeHTMLTags If {@code true}, wraps the output in {@code <html>} and {@code </html>} tags for display
     *                        in HTML-capable UI components. If {@code false}, returns plain text.
     *
     * @return A formatted string containing the complete breakdown of the recovery time calculation, optionally wrapped
     *       in HTML tags.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public String getRecoveryTimeBreakdownString(boolean includeHTMLTags) {
        StringBuilder breakdown = new StringBuilder(includeHTMLTags ? "<html>" : "");

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.baseRecoveryTime",
              baseRecoveryTime));

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.weatherMultiplier",
              weatherMultiplier));

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.windMultiplier",
              windMultiplier));

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.temperatureMultiplier",
              temperatureMultiplier));

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.gravityMultiplier",
              gravityMultiplier));

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.atmosphereMultiplier",
              atmosphereMultiplier));

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.lightMultiplier",
              lightMultiplier));

        double totalMultiplier = BASE_MULTIPLIER +
                                       weatherMultiplier +
                                       windMultiplier +
                                       temperatureMultiplier +
                                       gravityMultiplier +
                                       atmosphereMultiplier +
                                       lightMultiplier;
        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.totalMultiplier",
              totalMultiplier));

        breakdown.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "RecoveryTimeData.recoveryTimeBreakdown.totalRecoveryTime",
              totalRecoveryTime));

        breakdown.append(includeHTMLTags ? "</html>" : "");
        return breakdown.toString();
    }
}
