/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.io.migration;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.BotForce;

import java.util.Objects;

@Deprecated(since = "0.50.04", forRemoval = true)
public class FactionMigrator {

    /**
     * Currently this just handles the migration of PIND. For SUCS migration, replace the PIND method
     * with the SUCS one across the project and increase the versions in each migration location
     * for both uses of this method and the PIND method.
     */
    public static void migrateFactionCode(final Campaign campaign) {
        // Campaign
        campaign.setFactionCode(migrateCodePINDRemoval(campaign.getFactionCode()));
        campaign.setRetainerEmployerCode(migrateCodePINDRemoval(campaign.getRetainerEmployerCode()));

        // All Contracts
        campaign.getActiveContracts().stream()
                .flatMap(contract -> contract.getCurrentScenarios().stream())
                .flatMap(scenario -> scenario.getBotForces().stream())
                .map(BotForce::getBotForceRandomizer)
                .filter(Objects::nonNull)
                .forEach(botForceRandomizer -> botForceRandomizer.setFactionCode(migrateCodePINDRemoval(
                        botForceRandomizer.getFactionCode())));

        // AtB Contracts
        campaign.getAtBContracts().forEach(contract -> {
            contract.setEmployerCode(migrateCodePINDRemoval(contract.getEmployerCode()), contract.getStartDate());
            contract.setEnemyCode(migrateCodePINDRemoval(contract.getEnemyCode()));
        });

        // Contract Market
        campaign.getContractMarket().getContracts().stream()
                .filter(contract -> contract instanceof AtBContract)
                .map(contract -> (AtBContract) contract)
                .forEach(contract -> {
                    contract.setEmployerCode(migrateCodePINDRemoval(contract.getEmployerCode()), campaign.getLocalDate());
                    contract.setEnemyCode(migrateCodePINDRemoval(contract.getEnemyCode()));
                });
    }

    /**
     * This migrates the PIND faction code to IND, which occurred in 0.49.7
     */
    public static String migrateCodePINDRemoval(final String originalCode) {
        return "PIND".equals(originalCode) ? "IND" : originalCode;
    }

    public static String migrateCodeToAlignWithSUCS(final String originalCode) {
        switch (originalCode) {
            case "PIND":
            case "IND":
                return "I";
            case "DoL":
                return "DL";
            case "SCW":
                return "SCo";
            case "SSUP":
                return "SS";
            case "CTL":
                return "CTF";
            case "ABN":
                return "A";
            case "ARD":
                return "AuD";
            case "DIS":
                return "D";
            case "CCO":
                return "CCY";
            case "RWR":
                return "RW";
            case "RIM":
                return "RC";
            case "CIR":
                return "CF";
            case "CB":
                return "CBR";
            case "CLAN":
                return "C";
            case "CWI":
                return "CWM";
            case "FR":
                return "FrR";
            case "CWOV":
                return "CWV";
            case "WOB":
                return "WB";
            case "CEI":
                return "EI";
            case "AXP":
                return "AP";
            case "FoO":
                return "FO";
            case "MRep":
                return "MR";
            case "CMG":
                return "CMN";
            case "FVC":
                return "FvC";
            case "TB":
                return "RB";
            case "CWIE":
                return "CWX";
            case "CW":
                return "CWF";
            case "CWE":
                return "WE";
            case "ARC":
                return "AuC";
            case "Stone":
                return "CoF";
            case "ROS":
                return "RS";
            case "UND":
                return "U";
            case "RR":
                return "TR";
            case "ME":
                return "MuC";
            case "Mara":
                return "MarA";
            default:
                return originalCode;
        }
    }
}
