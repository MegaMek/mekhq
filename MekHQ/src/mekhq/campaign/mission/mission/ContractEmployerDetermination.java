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
package mekhq.campaign.mission.mission;

import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import jakarta.annotation.Nullable;
import megamek.common.util.weightedMaps.AbstractWeightedMap;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.personnel.enums.ConnectionsLevel;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class ContractEmployerDetermination {
    private static final MMLogger LOGGER = MMLogger.create(ContractEmployerDetermination.class);

    private final Faction campaignFaction;
    private final CampaignTypeForContractDetermination campaignType;
    private final HiringHallLevel hiringHallLevel;
    private final int forceReputationModifier;
    private final ConnectionsLevel connectionsLevel;
    private final AbstractLocation currentLocation; // TODO added for use once we support multi-locational contracts

    public ContractEmployerDetermination(Faction campaignFaction, HiringHallLevel hiringHallLevel,
          int forceReputationModifier, ConnectionsLevel connectionsLevel,
          AbstractLocation currentLocation) {
        this.campaignFaction = campaignFaction;
        this.campaignType = getCampaignTypeFromFaction();
        this.hiringHallLevel = hiringHallLevel;
        this.forceReputationModifier = forceReputationModifier;
        this.connectionsLevel = connectionsLevel;
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
            // Pirate factions always have themselves as the employer
            case PIRATE -> Factions.getInstance().getFaction(PIRATE_FACTION_CODE);
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
        // CamOps pg 39 rev 5th printing states that a player can pick any employer at or below their roll. This
        // creates a UX issue for MekHQ. To avoid spamming the player, we instead use the exact employer matching the
        // roll
        GlobalEmployerTableValue globalEmployerType = getGlobalEmployer();
        IndependentEmployerTableValue independentEmployerType = getIndependentEmployer();
        GlobalEmployerTableValue employerSearchFactionType = getFinalGlobalFactionTableValue(globalEmployerType,
              independentEmployerType);

        return getEmployer(employerSearchFactionType);
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

    private static @Nullable Faction getEmployer(@Nullable GlobalEmployerTableValue globalEmployerType) {
        while (globalEmployerType != null) {
            // TODO once we have the tech to have multiple combat forces scattered about the galaxy we should update
            //  this to no longer use campaign location - Illiani, 29/Jun/26
            AbstractWeightedMap<Integer, Faction> employerMap = RandomFactionGenerator.getInstance().getEmployerMap(
                  globalEmployerType);

            if (!employerMap.isEmpty()) {
                return employerMap.randomItem();
            }

            globalEmployerType = globalEmployerType.getNextLowestEmployerType();
        }

        LOGGER.warn("Failed to generate employer of type {}", globalEmployerType);

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
