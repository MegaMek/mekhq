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
package mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs;

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR_COMMAND;
import static mekhq.campaign.personnel.enums.PersonnelRole.BROKER;
import static mekhq.campaign.personnel.enums.PersonnelRole.LAWYER;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MERCHANT;
import static mekhq.campaign.personnel.ranks.Rank.RO_MIN;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.universe.enums.HiringHallLevel.QUESTIONABLE;

import java.util.List;

import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonUtility;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.SingleSpecialAbilityGenerator;
import mekhq.campaign.personnel.ranks.AutomaticRankAssigner;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.jspecify.annotations.NonNull;

/**
 * Generates the employer's negotiator for a contract. The negotiator's role is chosen from the campaign type, employer
 * faction, and hiring hall level, and the generated person is then brought up to the CamOps negotiator baseline
 * (veteran experience, minimum charisma, and minimum negotiation skill) before being assigned a special ability and,
 * for ranked roles, a military rank. This is a static utility class and is not instantiable.
 */
public class EmployerNegotiator {
    /**
     * This level is taken from CamOps rev 5th printing, pg 41. CamOps states that Negotiators have a Charisma modifier
     * of 1. 7 Charisma is the minimum score necessary to reach that modifier.
     */
    private final static int MINIMUM_CHARISMA = 7;

    /**
     * This level is taken from CamOps rev 5th printing, pg 41. CamOps states that Negotiators have a Negotiation level
     * of 4.
     */
    private final static int MINIMUM_NEGOTIATION = 4;

    private final static PersonnelRole PIRATE_NEGOTIATOR_ROLE = BROKER;
    private final static PersonnelRole GOVERNMENT_NEGOTIATOR_NORMAL = ADMINISTRATOR_COMMAND;
    private final static PersonnelRole GOVERNMENT_NEGOTIATOR_CLAN = MEKWARRIOR;
    private final static PersonnelRole MERCENARY_NEGOTIATOR_NORMAL = LAWYER;
    private final static PersonnelRole MERCENARY_NEGOTIATOR_CLAN = MERCHANT;

    /** These negotiators do not receive military ranks */
    private final static List<PersonnelRole> UNRANKED_ROLES = List.of(PIRATE_NEGOTIATOR_ROLE,
          MERCENARY_NEGOTIATOR_NORMAL,
          MERCENARY_NEGOTIATOR_CLAN);

    private EmployerNegotiator() {}

    // TODO have a way for the player to get intel on the opposing negotiator

    /**
     * Generates the employer's negotiator, choosing an appropriate role and raising the person to the CamOps negotiator
     * baseline.
     *
     * @param campaign        the current campaign, used to create and adjust the person
     * @param searchType      the campaign type, which drives the negotiator's role
     * @param employerFaction the employer faction (its Clan status affects the role and rank system)
     * @param hiringHallLevel the local hiring hall level, which can affect the mercenary negotiator's role
     *
     * @return the generated negotiator
     */
    public static Person generateNegotiator(Campaign campaign, ContractSearchType searchType,
          Faction employerFaction, HiringHallLevel hiringHallLevel) {
        PersonnelRole role = getNegotiatorRole(hiringHallLevel, searchType, employerFaction);

        Person negotiator = campaign.getPlayerForce()
                                  .getHumanResources()
                                  .newPerson(campaign, role, employerFaction.getShortName(), Gender.RANDOMIZE);

        adjustExperienceLevel(campaign, negotiator, role, employerFaction.isClan());
        adjustCharisma(negotiator);
        adjustNegotiation(negotiator);
        giveSPA(campaign, negotiator);

        assignRank(negotiator);

        return negotiator;
    }

    private static void giveSPA(Campaign campaign, Person negotiator) {
        SingleSpecialAbilityGenerator singleSpecialAbilityGenerator = new SingleSpecialAbilityGenerator();
        singleSpecialAbilityGenerator.rollSPA(campaign, negotiator);
    }

    private static PersonnelRole getNegotiatorRole(HiringHallLevel hiringHallLevel,
          ContractSearchType campaignType, Faction employerFaction) {
        boolean employerIsClan = employerFaction.isClan();

        return switch (campaignType) {
            case PIRATE -> PIRATE_NEGOTIATOR_ROLE;
            // TOURNAMENT contacts (arena games organizers) reuse the mercenary-hall broker role until dedicated
            // tournament generation lands.
            case MERCENARY, TOURNAMENT -> getMercenaryNegotiatorType(hiringHallLevel, employerIsClan);
            case GOVERNMENT -> employerIsClan ? GOVERNMENT_NEGOTIATOR_CLAN : GOVERNMENT_NEGOTIATOR_NORMAL;
        };
    }

    private static @NonNull PersonnelRole getMercenaryNegotiatorType(HiringHallLevel hiringHallLevel,
          boolean employerIsClan) {
        if (employerIsClan) {
            return MERCENARY_NEGOTIATOR_CLAN;
        }

        if (hiringHallLevel == QUESTIONABLE) {
            return PIRATE_NEGOTIATOR_ROLE;
        }

        return MERCENARY_NEGOTIATOR_NORMAL;
    }

    private static void adjustExperienceLevel(Campaign campaign, Person negotiator, PersonnelRole role,
          boolean isClanForce) {
        int experienceLevel = negotiator.getExperienceLevel(campaign.getCampaignOptions(),
              isClanForce,
              campaign.getLocalDate(),
              false,
              false);

        if (experienceLevel <= SkillLevel.VETERAN.getExperienceLevel()) {
            PersonUtility.overrideSkills(campaign, negotiator, role, SkillLevel.VETERAN, true);
        }
    }

    private static void adjustCharisma(Person negotiator) {
        int charisma = negotiator.getAttributeScore(SkillAttribute.CHARISMA);
        if (charisma < MINIMUM_CHARISMA) {
            negotiator.setAttributeScore(SkillAttribute.CHARISMA, MINIMUM_CHARISMA);
        }
    }

    private static void adjustNegotiation(Person negotiator) {
        Skill negotiationSkill = negotiator.getSkill(S_NEGOTIATION);

        if (negotiationSkill == null) { // Unskilled
            negotiator.addSkill(S_NEGOTIATION, MINIMUM_NEGOTIATION, 0);
            return;
        }

        int currentLevel = negotiationSkill.getLevel();
        if (currentLevel < MINIMUM_NEGOTIATION) {
            negotiationSkill.setLevel(MINIMUM_NEGOTIATION);
        }
    }

    private static void assignRank(Person negotiator) {
        final PersonnelRole primaryRole = negotiator.getPrimaryRole();
        if (UNRANKED_ROLES.contains(primaryRole)) {
            return;
        }

        AutomaticRankAssigner.assignRankSystemFromFaction(negotiator, RO_MIN);
    }
}
