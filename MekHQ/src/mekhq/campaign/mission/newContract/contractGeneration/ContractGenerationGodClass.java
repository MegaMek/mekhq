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
package mekhq.campaign.mission.newContract.contractGeneration;

import java.time.LocalDate;

import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.newContract.AbstractContractManager;
import mekhq.campaign.mission.newContract.NormalContractManager;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;

public class ContractGenerationGodClass {
    private static final MMLogger LOGGER = MMLogger.create(ContractGenerationGodClass.class);

    public ContractGenerationGodClass() {
    }

    public void generateContract(Campaign campaign, double forceReputationFactor, AbstractLocation currentLocation,
          Person negotiator) {
        AbstractContractManager parentContractManager = new NormalContractManager();

        LocalDate currentDate = campaign.getLocalDate();

        ContractGenerationStage1.generateEmployerContractTypeEnemyAndLocation(campaign,
              forceReputationFactor,
              currentLocation,
              negotiator,
              currentDate,
              parentContractManager);

        Faction employerFaction = parentContractManager.getEmployerFaction();
        EmployerModifierData employerModifierData = new EmployerModifierData();
        int currentYear = currentDate.getYear();
        int reputationRating = campaign.getReputation().getReputationRating();
        EmployerNegotiationsModifier.getNegotiationsModifier(employerFaction, currentYear, employerModifierData);
        UnitReputationNegotiationsModifier.getNegotiationsModifier(reputationRating, employerModifierData);


    }
}
