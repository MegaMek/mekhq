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
package mekhq.campaign.mission.mission.contractGeneration;

import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.universe.Faction.COMSTAR_FACTION_CODE;
import static mekhq.campaign.universe.Faction.WORD_OF_BLAKE_FACTION_CODE;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.personnel.enums.ConnectionsLevel;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class ContractEmployerDetermination {
    private static final MMLogger LOGGER = MMLogger.create(ContractEmployerDetermination.class);

    private static final int COMSTAR_EMPLOYER_CHANCE = 100;
    private static final int WORD_OF_BLAKE_EMPLOYER_CHANCE = 40;

    private final LocalDate currentDate;
    private final Faction campaignFaction;
    private final CampaignTypeForContractDetermination campaignType;
    private final HiringHallLevel hiringHallLevel;
    private final int forceReputationModifier;
    private final ConnectionsLevel connectionsLevel;
    private final AbstractLocation currentLocation;

    public ContractEmployerDetermination(LocalDate currentDate, Faction campaignFaction,
          HiringHallLevel hiringHallLevel, int forceReputationModifier, int adjustedConnectionsLevel,
          AbstractLocation currentLocation) {
        this.currentDate = currentDate;
        this.campaignFaction = campaignFaction;
        this.campaignType = getCampaignTypeFromFaction();
        this.hiringHallLevel = hiringHallLevel;
        this.forceReputationModifier = forceReputationModifier;
        this.connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(adjustedConnectionsLevel);
        this.currentLocation = currentLocation;
    }

    private CampaignTypeForContractDetermination getCampaignTypeFromFaction() {
        if (campaignFaction.isPirate()) {
            return CampaignTypeForContractDetermination.PIRATE;
        }

        if (campaignFaction.isMercenary()) {
            return CampaignTypeForContractDetermination.MERCENARY;
        }

        return CampaignTypeForContractDetermination.GOVERNMENT;
    }

    public Faction getContractEmployer() {
        return switch (campaignType) {
            // CampOps pg 39 rev 5th printing states that mercenaries get a semi-random employer. We generate this by
            // looking at all potential employers in the contract search radius. Our generation method is based on
            // the table found in the above-cited page but is not an exact match due to legacy reasons.
            case MERCENARY -> getEmployerUsingMercenaryMethod();
            // Pirate factions are actually picking their victim at this stage. Victim is then used to generate
            // the contract type and then later overwritten by the pirate faction
            // TODO makes sure you did the above
            case PIRATE -> getEmployerUsingMercenaryMethod();
            // Government factions always have themselves as the employer. Government factions as any campaign
            // faction that is not pirate or mercenary.
            case GOVERNMENT -> campaignFaction;
        };
    }

    /**
     * Determines and returns the faction that will serve as the mercenary employer for a contract.
     *
     * <p>We start by determining the {@link GlobalEmployerTableValue}, we also generate an
     * {@link IndependentEmployerTableValue} in case we need it. If {@code globalEmployerType} is
     * {@link GlobalEmployerTableValue#INDEPENDENT} then we generate a second {@link GlobalEmployerTableValue}. This
     * second value is then used to help us pick a faction the 'independent' employer is acting on behalf of.</p>
     *
     * <p>For contract pay, terms, and other clauses, either {@code globalEmployerType} or
     * {@code independentEmployerType} should be used. {@code employerSearchFactionType} only exists to assist
     * searching.</p>
     *
     * @return the faction representing the contract employer
     */
    private @Nullable Faction getEmployerUsingMercenaryMethod() {
        int currentYear = currentDate.getYear();

        Faction specialEmployer = checkForSpecialEmployer(currentYear);
        if (specialEmployer != null) {
            return specialEmployer;
        }

        // CamOps pg 39 rev 5th printing states that a player can pick any employer at or below their roll. This
        // creates a UX issue for MekHQ. To avoid spamming the player, we instead use the exact employer matching the
        // roll
        GlobalEmployerTableValue globalEmployerType = getGlobalEmployer();
        IndependentEmployerTableValue independentEmployerType = getIndependentEmployer();
        GlobalEmployerTableValue employerSearchFactionType = getFinalGlobalFactionTableValue(globalEmployerType,
              independentEmployerType);

        return getEmployer(employerSearchFactionType);
    }

    private static @Nullable Faction checkForSpecialEmployer(int currentYear) {
        Faction specialEmployer = checkForEmployerOverride(currentYear,
              COMSTAR_FACTION_CODE,
              COMSTAR_EMPLOYER_CHANCE,
              true);
        if (specialEmployer != null) {
            return specialEmployer;
        }

        specialEmployer = checkForEmployerOverride(currentYear, WORD_OF_BLAKE_FACTION_CODE,
              WORD_OF_BLAKE_EMPLOYER_CHANCE, false);
        return specialEmployer;
    }

    private static @Nullable Faction checkForEmployerOverride(int currentYear,
          String factionCode, int chance, boolean useSpecialComStarStartYear) {
        Faction faction = Factions.getInstance().getFaction(factionCode);
        int startYear = useSpecialComStarStartYear ? 2788 : faction.getStartYear();
        int endYear = faction.getEndYear();

        if (isFactionOperating(currentYear, startYear, endYear)) {
            int roll = randomInt(chance);

            if (roll == 0) {
                return faction;
            }
        }
        return null;
    }

    private static boolean isFactionOperating(int currentYear, int startYearComStar, int endingYearComStar) {
        return currentYear >= startYearComStar && currentYear <= endingYearComStar;
    }

    private GlobalEmployerTableValue getFinalGlobalFactionTableValue(GlobalEmployerTableValue globalEmployerType,
          IndependentEmployerTableValue independentEmployerType) {
        if (globalEmployerType == GlobalEmployerTableValue.INDEPENDENT) {
            GlobalEmployerTableValue newGlobalEmployerFaction = getSecondaryGlobalEmployerType(independentEmployerType);
            return newGlobalEmployerFaction != null ? newGlobalEmployerFaction : globalEmployerType;
        }

        return globalEmployerType;
    }

    private @Nullable GlobalEmployerTableValue getSecondaryGlobalEmployerType(
          IndependentEmployerTableValue independentEmployerType) {
        boolean isIndependentOverride = isIsIndependentOverride(independentEmployerType);

        GlobalEmployerTableValue secondaryGlobalEmployerType = null;
        if (isIndependentOverride) {
            secondaryGlobalEmployerType = getGlobalEmployer();
        }

        return secondaryGlobalEmployerType;
    }

    private static boolean isIsIndependentOverride(IndependentEmployerTableValue independentEmployerType) {
        return switch (independentEmployerType) {
            case NOBLE, PLANETARY_GOVERNMENT, MERCENARY, CORPORATION -> true;
            // These are terms used by CamOps. In MekHQ we treat them as truly Independent employers, so not acting
            // on behalf of a parent nation
            case MAJOR_PERIPHERY, MINOR_PERIPHERY -> false;
        };
    }

    private @Nullable Faction getEmployer(@Nullable GlobalEmployerTableValue globalEmployerType) {
        while (globalEmployerType != null) {
            Faction employerFaction = RandomFactionGenerator.getInstance()
                                            .getRandomEmployerFaction(currentLocation, currentDate, globalEmployerType);

            if (employerFaction != null) {
                return employerFaction;
            }

            globalEmployerType = globalEmployerType.getNextLowestEmployerType();
        }

        LOGGER.warn("Failed to generate employer. Returning null.");

        return null;
    }

    private IndependentEmployerTableValue getIndependentEmployer() {
        int roll = getEmployerRoll();
        return IndependentEmployerTableValue.getEmployerForRoll(roll);
    }

    private GlobalEmployerTableValue getGlobalEmployer() {
        int roll = getEmployerRoll();
        return GlobalEmployerTableValue.getEmployerForRoll(roll);
    }

    private int getEmployerRoll() {
        int roll = d6(2);
        int hiringHallModifier = hiringHallLevel.getEmployerModifier();
        int connectionsModifier = connectionsLevel.getEquipLevel();

        return roll + hiringHallModifier + connectionsModifier + forceReputationModifier;
    }
}
