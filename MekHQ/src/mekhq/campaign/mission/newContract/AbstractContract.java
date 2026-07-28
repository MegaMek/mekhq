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
package mekhq.campaign.mission.newContract;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.JumpPath;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.newContract.contractData.ContractFinanceData;
import mekhq.campaign.mission.newContract.contractData.ContractObjectiveData;
import mekhq.campaign.mission.newContract.contractData.ContractScheduleData;
import mekhq.campaign.mission.newContract.contractData.EmployerData;
import mekhq.campaign.mission.newContract.contractData.EnemyData;
import mekhq.campaign.mission.newContract.contractData.MoraleData;
import mekhq.campaign.mission.newContract.contractData.RentedFacilitiesData;
import mekhq.campaign.mission.newContract.contractData.SystemsTargetData;
import mekhq.campaign.mission.newContract.contractGeneration.ContractTermsData;

public abstract class AbstractContract {
    private UUID contractId;
    private String contractName;
    private String description;

    private EmployerData employerData;
    private EnemyData enemyData;

    private ContractTermsData contractTerms;
    private ContractObjectiveData objectiveData;
    private ContractFinanceData contractFinanceData;

    private MissionStatus missionStatus;
    private ContractScheduleData scheduleData;
    private SystemsTargetData systemsTargetData;

    private RentedFacilitiesData rentedFacilitiesData;
    private MoraleData moraleData;

    private StratConCampaignState stratConCampaignState;
    private int scale;
    private int trackCount; // TODO future proofing

    private final List<Scenario> scenarios = new ArrayList<>();

    /*
     * This is a transient variable meant to keep track of a single jump path while the contract runs through initial
     * calculations, as the same jump path is referenced multiple times and calculating it each time is expensive. No
     * need to preserve it in save data.
     */
    private transient JumpPath cachedJumpPath;
    private transient int cachedContractDifficulty;
}
