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

import java.util.Objects;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.BotForce;

@Deprecated(since = "0.50.04", forRemoval = true)
public class FactionMigrator {

    /**
     * Currently this just handles the migration of PIND. For SUCS migration, replace the PIND method with the SUCS one
     * across the project and increase the versions in each migration location for both uses of this method and the PIND
     * method.
     *
     * @since 0.50.04
     * @deprecated no Indicated uses
     */
    @Deprecated(since = "0.50.04", forRemoval = true)
    public static void migrateFactionCode(final Campaign campaign) {
        // Campaign
        campaign.setFactionCode(migrateCodePINDRemoval(campaign.getFactionCode()));
        campaign.setRetainerEmployerCode(migrateCodePINDRemoval(campaign.getRetainerEmployerCode()));

        // All Contracts
        campaign.getActiveContracts()
              .stream()
              .flatMap(contract -> contract.getCurrentScenarios().stream())
              .flatMap(scenario -> scenario.getBotForces().stream())
              .map(BotForce::getBotForceRandomizer)
              .filter(Objects::nonNull)
              .forEach(botForceRandomizer -> botForceRandomizer.setFactionCode(migrateCodePINDRemoval(botForceRandomizer.getFactionCode())));

        // AtB Contracts
        campaign.getAtBContracts().forEach(contract -> {
            contract.setEmployerCode(migrateCodePINDRemoval(contract.getEmployerCode()), contract.getStartDate());
            contract.setEnemyCode(migrateCodePINDRemoval(contract.getEnemyCode()));
        });

        // Contract Market
        campaign.getContractMarket()
              .getContracts()
              .stream()
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

    /**
     * @since 0.50.04
     * @deprecated No indicated uses
     */
    @Deprecated(since = "0.50.04", forRemoval = true)
    public static String migrateCodeToAlignWithSUCS(final String originalCode) {
        return switch (originalCode) {
            case "PIND", "IND" -> "I";
            case "DoL" -> "DL";
            case "SCW" -> "SCo";
            case "SSUP" -> "SS";
            case "CTL" -> "CTF";
            case "ABN" -> "A";
            case "ARD" -> "AuD";
            case "DIS" -> "D";
            case "CCO" -> "CCY";
            case "RWR" -> "RW";
            case "RIM" -> "RC";
            case "CIR" -> "CF";
            case "CB" -> "CBR";
            case "CLAN" -> "C";
            case "CWI" -> "CWM";
            case "FR" -> "FrR";
            case "CWOV" -> "CWV";
            case "WOB" -> "WB";
            case "CEI" -> "EI";
            case "AXP" -> "AP";
            case "FoO" -> "FO";
            case "MRep" -> "MR";
            case "CMG" -> "CMN";
            case "FVC" -> "FvC";
            case "TB" -> "RB";
            case "CWIE" -> "CWX";
            case "CW" -> "CWF";
            case "CWE" -> "WE";
            case "ARC" -> "AuC";
            case "Stone" -> "CoF";
            case "ROS" -> "RS";
            case "UND" -> "U";
            case "RR" -> "TR";
            case "ME" -> "MuC";
            case "Mara" -> "MarA";
            default -> originalCode;
        };
    }
}
