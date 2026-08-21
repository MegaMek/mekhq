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
package mekhq.campaign.mission.contract.utilities;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.OVERWHELMING;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class ContractEmergencyExtension {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractEmergencyExtension";

    public static boolean contractExtended(Campaign campaign, AbstractContract contract) {
        Factions factions = Factions.getInstance();
        // We use anchor faction to allow for the breadth of faction political interactions
        Faction employerFaction = factions.getFaction(contract.getEmployerData().anchorFactionCode());
        // An enemy fielded by a covert sponsor fights that sponsor's war, so check against the sponsor when there is
        // one; otherwise against the enemy itself.
        Faction enemySponsor = contract.getEnemySponsorFaction();
        Faction enemyFaction = enemySponsor == null ? contract.getEnemyFaction() : enemySponsor;
        LocalDate localDate = campaign.getLocalDate();
        final String warName = RandomFactionGenerator.getInstance()
                                     .getFactionHints()
                                     .getCurrentWar(employerFaction, enemyFaction, localDate);
        if (warName == null) {
            return false;
        }

        final int extension;
        int roll = d6();

        if (roll == 1) {
            extension = max(1, contract.getLengthInMonths() / 2);
        } else if (roll == 2) {
            extension = 1;
        } else {
            return false;
        }

        LocalDate endingDate = contract.getEndingDate();
        if (endingDate == null) {
            return false;
        }

        LocalDate newEndDate = endingDate.plusMonths(extension);
        contract.updateScheduleData(null, newEndDate);

        // We spike morale to create a jump in contract difficulty - essentially the reason why the employer is using
        // the emergency clause.
        int moraleOrdinal = contract.getMoraleLevel().ordinal();
        roll = d6(2) / 2;
        moraleOrdinal = min(moraleOrdinal + roll, OVERWHELMING.ordinal());
        contract.changeMorale(ContractMoraleLevel.values()[moraleOrdinal]);

        MekHQ.triggerEvent(new MissionChangedEvent(contract));

        triggerDialog(campaign, contract, extension, warName);

        return true;
    }

    private static void triggerDialog(Campaign campaign, AbstractContract contract, int extension,
          String warName) {
        String inCharacter = getFormattedTextAt(RESOURCE_BUNDLE,
              "ContractEmergencyExtension.ic",
              campaign.getCommanderAddress(),
              extension);

        String outOfCharacter = getFormattedTextAt(RESOURCE_BUNDLE,
              "ContractEmergencyExtension.ooc",
              warName);

        new ImmersiveDialogSimple(campaign,
              contract.getEmployerLiaison(),
              null,
              inCharacter,
              null,
              outOfCharacter,
              null,
              false);
    }
}
