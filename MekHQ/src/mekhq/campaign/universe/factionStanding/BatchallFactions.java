/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import java.util.List;

import megamek.codeUtilities.MathUtility;
import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;

/**
 * Unused outside deprecated classes and methods
 */
@Deprecated(since = "0.50.07", forRemoval = true)
public class BatchallFactions {

    public static final List<String> BATCHALL_FACTIONS = List.of("CBS", "CB", "CCC", "CCO",
          "CDS", "CFM", "CGB", "CGS", "CHH", "CIH", "CJF", "CMG", "CNC", "CSJ", "CSR", "CSA", "CSV",
          "CSL", "CWI", "CW", "CWE", "CWIE", "CEI", "RD", "RA", "CP", "AML", "CLAN");

    /**
     * Determines whether a given faction engages in batchalling.
     *
     * @param factionCode The faction code to check eligibility for. Must be a non-null {@link String}.
     *
     * @return {@code true} if the faction code engages in batchalling, {@code false} otherwise.
     */
    public static boolean usesBatchalls(String factionCode) {
        if (factionCode == null) {
            return false;
        }

        //        Faction faction = Faction.getFaction(factionCode);

        return BATCHALL_FACTIONS.contains(factionCode);
    }

    /**
     * Retrieves the greeting for faction based on infamy.
     *
     * @param campaign    The campaign for which to retrieve the greeting.
     * @param factionCode The faction code for which to retrieve the greeting.
     *
     * @return The greeting message as a {@link String}.
     */
    public static String getGreeting(Campaign campaign, String factionCode) {
        final int infamy = MathUtility.clamp(campaign.getFameAndInfamy().getFameLevelForFaction(factionCode),
              0, 5);

        // Faction special handlers
        String version = "";
        switch (factionCode) {
            case "CWE" -> factionCode = "CW"; // Wolf Empire uses the lines for Clan Wolf
            case "CGB" -> { // The Ghost Bear Dominion uses the lines for the Rasalhague Dominion
                if (campaign.getGameYear() < 3060) {
                    factionCode = "CGB";
                } else {
                    factionCode = "RD";
                }
            }
            // Alyina Mercantile League isn't big enough to warrant their own lines, so they use the
            // generic fallback
            case "AML" -> factionCode = "CLAN";
            case "CDS" -> { // This handles the switch from Clan Diamond Shark to Clan Sea Fox
                if (campaign.getGameYear() < 3100) {
                    version = "Version 1";
                } else {
                    version = "Version 2";
                }
            }
            default -> {}
        }

        // The rest of the method
        String greeting;
        int type;

        if (infamy == 5) {
            // greeting = resources.getString("greetingCLANLevel5Type0.text");
        } else {
            type = Compute.randomInt(3);
            // String greetingReference = String.format(resources.getString("greetingFormatBatchall.text"),
            //                factionCode, version, infamy, type);
            // greeting = resources.getString(greetingReference);
        }

        return "";
    }
}
