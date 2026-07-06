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

import static java.lang.Math.clamp;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.mission.enums.AtBContractType.*;
import static mekhq.campaign.personnel.PersonnelOptions.EDGE_TRAINING;
import static mekhq.campaign.personnel.skills.SkillType.S_INVESTIGATION;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.ConnectionsLevel;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class MissionObjectiveTypeDetermination {
    private final MMLogger LOGGER = MMLogger.create(MissionObjectiveTypeDetermination.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.MissionObjectiveTypeDetermination";

    /**
     * CamOps pg 20 rev 5th printing states that Clan campaigns cannot generate Covert or Special missions.
     */
    private static final List<Integer> PROHIBITED_CLAN_ROLLS = List.of(2, 3, 12);

    private final String skillCheckReason = getTextAt(RESOURCE_BUNDLE,
          "MissionObjectiveTypeDetermination.skillCheck.reason");

    private final Campaign campaign;
    private final boolean isPirateCampaign;
    private final HiringHallLevel hiringHallLevel;
    private final Person negotiator;
    private final ConnectionsLevel connectionsLevel;
    private final Faction employerFaction;
    private final GlobalEmployerTableValue employerTableValue;
    private final IndependentEmployerTableValue independentEmployerTableValue;

    // TODO player must be able to pick negotiator
    public MissionObjectiveTypeDetermination(Campaign campaign, HiringHallLevel hiringHallLevel, Person negotiator,
          Faction employerFaction, GlobalEmployerTableValue employerTableValue,
          IndependentEmployerTableValue independentEmployerTableValue) {
        this.campaign = campaign;
        this.isPirateCampaign = campaign.isPirateCampaign();
        this.negotiator = negotiator;
        this.hiringHallLevel = hiringHallLevel;
        this.employerFaction = employerFaction;
        this.employerTableValue = employerTableValue;
        this.independentEmployerTableValue = independentEmployerTableValue;

        int adjustedConnections = negotiator.getAdjustedConnections(false);
        connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(adjustedConnections);
    }

    public List<AtBContractType> getObjectiveType() {
        int roll = getMissionRollAdjustedByFaction();

        if (isPirateCampaign) {
            return getPirateObjectiveType(roll);
        }

        return switch (employerTableValue) {
            case INDEPENDENT -> getIndependentObjectiveType(roll);
            case MINOR_POWER, SUPER_POWER, MAJOR_POWER -> getObjectiveTypeForInnerSphereOrClan(roll);
        };
    }

    private List<AtBContractType> getIndependentObjectiveType(int roll) {
        return switch (independentEmployerTableValue) {
            case NOBLE, PLANETARY_GOVERNMENT, MERCENARY, MAJOR_PERIPHERY, MINOR_PERIPHERY ->
                  getObjectiveTypeForIndependent(roll);
            case CORPORATION -> getObjectiveTypeForCorporation(roll);
        };
    }

    private int getMissionRollAdjustedByFaction() {
        boolean shouldReroll = true;
        boolean isClan = employerFaction.isClan();

        int roll = 7;
        while (shouldReroll) {
            roll = getMissionRoll();
            shouldReroll = isClan && PROHIBITED_CLAN_ROLLS.contains(roll);
        }

        return roll;
    }

    private List<AtBContractType> getObjectiveTypeForInnerSphereOrClan(int roll) {
        return switch (roll) {
            case 2 -> getConvertObjectiveType();
            case 3, 12 -> getSpecialObjectiveType();
            case 4 -> List.of(PIRATE_HUNTING);
            case 5 -> List.of(PLANETARY_ASSAULT);
            case 6, 7 -> List.of(OBJECTIVE_RAID);
            case 8 -> List.of(EXTRACTION_RAID);
            case 9 -> List.of(RECON_RAID);
            case 10 -> List.of(GARRISON_DUTY);
            case 11 -> List.of(CADRE_DUTY);
            default -> {
                LOGGER.error("Unexpected roll value (getObjectiveTypeForInnerSphereOrClan): {}", roll);
                yield null;
            }
        };
    }

    private List<AtBContractType> getObjectiveTypeForIndependent(int roll) {
        return switch (roll) {
            case 2 -> getConvertObjectiveType();
            case 3, 12 -> getSpecialObjectiveType();
            case 4 -> List.of(PLANETARY_ASSAULT);
            case 5, 9 -> List.of(OBJECTIVE_RAID);
            case 6 -> List.of(EXTRACTION_RAID);
            case 7 -> List.of(PIRATE_HUNTING);
            case 8 -> List.of(SECURITY_DUTY);
            case 10 -> List.of(GARRISON_DUTY);
            case 11 -> List.of(CADRE_DUTY);
            default -> {
                LOGGER.error("Unexpected roll value (getObjectiveTypeForIndependent): {}", roll);
                yield null;
            }
        };
    }

    private List<AtBContractType> getSpecialObjectiveType() {
        int newRoll = getMissionRoll();
        return getSpecialObjectiveType(newRoll);
    }

    private List<AtBContractType> getObjectiveTypeForCorporation(int roll) {
        return switch (roll) {
            case 2, 3 -> getConvertObjectiveType();
            case 4, 12 -> getSpecialObjectiveType();
            case 5, 8 -> List.of(OBJECTIVE_RAID);
            case 6 -> List.of(EXTRACTION_RAID);
            case 7 -> List.of(RECON_RAID);
            case 9 -> List.of(SECURITY_DUTY);
            case 10 -> List.of(GARRISON_DUTY);
            case 11 -> pickAlternatingObjectiveType(CADRE_DUTY);
            default -> {
                LOGGER.error("Unexpected roll value (getObjectiveTypeForCorporation): {}", roll);
                yield null;
            }
        };
    }

    private List<AtBContractType> getConvertObjectiveType() {
        int newRoll = getMissionRoll();
        return getCovertObjectiveType(newRoll);
    }

    private static List<AtBContractType> pickAlternatingObjectiveType(AtBContractType alternateOption) {
        int newRoll = randomInt(1);
        if (newRoll == 0) {
            return List.of(alternateOption);
        } else {
            return List.of(GARRISON_DUTY);
        }
    }

    private List<AtBContractType> getSpecialObjectiveType(int roll) {
        return switch (roll) {
            case 2 -> getConvertObjectiveType();
            case 3 -> List.of(GUERRILLA_WARFARE, PLANETARY_ASSAULT);
            case 4 -> List.of(GUERRILLA_WARFARE);
            case 5 -> List.of(RECON_RAID, PLANETARY_ASSAULT);
            case 6 -> List.of(EXTRACTION_RAID);
            case 7 -> List.of(RETAINER);
            case 8 -> List.of(RECON_RAID);
            case 9 -> List.of(RELIEF_DUTY);
            case 10 -> List.of(DIVERSIONARY_RAID, PLANETARY_ASSAULT);
            case 11 -> pickAlternatingObjectiveType(RIOT_DUTY);
            case 12 -> pickAlternatingObjectiveType(CADRE_DUTY);
            default -> {
                LOGGER.error("Unexpected roll value (getSpecialObjectiveType): {}", roll);
                yield null;
            }
        };
    }

    private List<AtBContractType> getCovertObjectiveType(int roll) {
        return switch (roll) {
            case 2 -> List.of(TERRORISM);
            case 3, 4 -> List.of(ASSASSINATION);
            case 5 -> List.of(ESPIONAGE);
            case 6 -> List.of(SABOTAGE);
            case 7 -> List.of(GUERRILLA_WARFARE);
            case 8 -> List.of(RECON_RAID);
            case 9 -> List.of(DIVERSIONARY_RAID);
            case 10 -> List.of(OBSERVATION_RAID);
            case 11 -> List.of(MOLE_HUNTING);
            case 12 -> List.of(SECURITY_DUTY);
            default -> {
                LOGGER.error("Unexpected roll value (getCovertObjectiveType): {}", roll);
                yield null;
            }
        };
    }

    private List<AtBContractType> getPirateObjectiveType(int roll) {
        return switch (roll) {
            case 2, 3, 4, 5 -> List.of(RECON_RAID);
            case 6, 7, 8, 9, 10, 11, 12 -> List.of(OBJECTIVE_RAID);
            default -> {
                LOGGER.error("Unexpected roll value (getPirateObjectiveType): {}", roll);
                yield null;
            }
        };
    }

    private int getMissionRoll() {
        int negotiationModifier = getSkillModifier();
        int connectionsModifier = connectionsLevel.getEquipLevel();

        // CamOps pg 40 rev 5th printing states that the player can apply their negotiations and connections
        // modifiers negatively to produce more covert results. As we cannot spam the player with requests, we're
        // instead going to tie this polarity to the hiring hall type. With a questionable hiring hall more likely to
        // give covert mission objectives.

        if (hiringHallLevel == HiringHallLevel.QUESTIONABLE) {
            negotiationModifier = -negotiationModifier;
            connectionsModifier = -connectionsModifier;
        }

        int roll = d6(2);
        return clamp(roll + negotiationModifier + connectionsModifier, 2, 12);
    }

    private int getSkillModifier() {
        String skill = isPirateCampaign ? S_INVESTIGATION : S_NEGOTIATION;
        SkillCheck skillCheck = negotiator.checkSkill(skill, campaign);

        // TODO replace with actual edge option, current is just a placeholder
        boolean isUseEdge = negotiator.getOptions().booleanOption(EDGE_TRAINING);

        String reason = getTextAt(RESOURCE_BUNDLE, skillCheckReason);
        ActionCheckResult result = skillCheck.resolve(isUseEdge, reason);

        campaign.addReport(DailyReportType.SKILL_CHECKS, result.getReport(true));

        return result.getMarginOfSuccess();
    }
}
