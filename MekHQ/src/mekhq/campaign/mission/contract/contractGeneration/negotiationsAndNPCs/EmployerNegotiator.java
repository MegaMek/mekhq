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

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.BROKER;
import static mekhq.campaign.personnel.enums.PersonnelRole.LAWYER;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MERCHANT;
import static mekhq.campaign.personnel.ranks.Rank.RO_MIN;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.universe.enums.HiringHallLevel.QUESTIONABLE;

import java.util.List;

import jakarta.annotation.Nonnull;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonUtility;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.SingleSpecialAbilityGenerator;
import mekhq.campaign.personnel.ranks.AutomaticRankAssigner;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HiringHallLevel;

/**
 * Generates the employer's negotiator for a contract. The negotiator's role is chosen from the campaign type, employer
 * faction, and hiring hall level, and the generated person's skills are then set to the tier the employer type fields
 * (see {@link #getEmployerSkillLevel(Faction, ChaosEmployerType)}) before being assigned a special ability and, for
 * ranked roles, a military rank. This is a static utility class and is not instantiable.
 */
public class EmployerNegotiator {
    private final static PersonnelRole PIRATE_NEGOTIATOR_ROLE = BROKER;
    private final static PersonnelRole GOVERNMENT_NEGOTIATOR_NORMAL = ADMINISTRATOR;
    private final static PersonnelRole GOVERNMENT_NEGOTIATOR_CLAN = MEKWARRIOR;
    private final static PersonnelRole MERCENARY_NEGOTIATOR_NORMAL = LAWYER;
    private final static PersonnelRole MERCENARY_NEGOTIATOR_CLAN = MERCHANT;

    /** These negotiators do not receive military ranks */
    private final static List<PersonnelRole> UNRANKED_ROLES = List.of(PIRATE_NEGOTIATOR_ROLE,
          MERCENARY_NEGOTIATOR_NORMAL,
          MERCENARY_NEGOTIATOR_CLAN);

    private EmployerNegotiator() {}

    /**
     * Generates the employer's negotiator, choosing an appropriate role and setting the person's skills to the tier the
     * employer type fields.
     *
     * @param campaign          the current campaign, used to create and adjust the person
     * @param searchType        the campaign type, which drives the negotiator's role
     * @param employerFaction   the employer faction (its Clan status affects the role and rank system)
     * @param hiringHallLevel   the local hiring hall level, which can affect the mercenary negotiator's role
     * @param chaosEmployerType the employer type, which drives the negotiator's skill tier
     *
     * @return the generated negotiator
     */
    public static Person generateNegotiator(Campaign campaign, ContractSearchType searchType,
          Faction employerFaction, HiringHallLevel hiringHallLevel, ChaosEmployerType chaosEmployerType) {
        PersonnelRole role = getNegotiatorRole(hiringHallLevel, searchType, employerFaction);

        Person negotiator = campaign.getPlayerForce()
                                  .getHumanResources()
                                  .newPerson(campaign, role, employerFaction.getShortName(), Gender.RANDOMIZE);

        applySkillLevel(campaign, negotiator, getEmployerSkillLevel(employerFaction, chaosEmployerType));
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

    private static @Nonnull PersonnelRole getMercenaryNegotiatorType(HiringHallLevel hiringHallLevel,
          boolean employerIsClan) {
        if (employerIsClan) {
            return MERCENARY_NEGOTIATOR_CLAN;
        }

        if (hiringHallLevel == QUESTIONABLE) {
            return PIRATE_NEGOTIATOR_ROLE;
        }

        return MERCENARY_NEGOTIATOR_NORMAL;
    }

    /**
     * The skill tier of the negotiator a given employer fields. Major powers, Clans, ComStar, and the Word of Blake send
     * seasoned negotiators; smaller employers send progressively less experienced ones.
     *
     * @param faction           the employer faction
     * @param chaosEmployerType the employer type
     *
     * @return the negotiator's skill tier, before any negotiator contract characteristic is applied
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static SkillLevel getEmployerSkillLevel(Faction faction, ChaosEmployerType chaosEmployerType) {
        return switch (chaosEmployerType) {
            case ANY_SYSTEM_OWNER, LOCAL_SYSTEM_OWNER -> {
                if (faction.isMajorOrSuperPower() || faction.isClan()) {
                    yield SkillLevel.VETERAN;
                } else if (faction.isComStarOrWoB()) {
                    yield SkillLevel.ELITE;
                } else {
                    yield SkillLevel.REGULAR;
                }
            }
            case NOBLE, MERCENARY_SUBCONTRACT, CORPORATION, UNDERWORLD_CONTACT -> SkillLevel.REGULAR;
            case CIVILIAN_ORGANIZATION_BUSINESS,
                 CIVILIAN_ORGANIZATION_MILITIA,
                 LOCAL_PLANETARY_GOVERNMENT,
                 ANY_PLANETARY_GOVERNMENT -> SkillLevel.GREEN;
            case CIVILIAN_ORGANIZATION_REBELS -> SkillLevel.ULTRA_GREEN;
        };
    }

    /**
     * Sets the negotiator's skills to the given tier, raising or lowering them as needed. Negotiation is set to that
     * tier as well, since not every negotiator role (a Clan MekWarrior, or an Administrator when Administrators don't
     * have Negotiation) has it as a profession skill.
     *
     * @param campaign   the current campaign
     * @param negotiator the negotiator to adjust
     * @param skillLevel the tier to set the negotiator's skills to
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void applySkillLevel(Campaign campaign, Person negotiator, SkillLevel skillLevel) {
        PersonUtility.overrideSkills(campaign, negotiator, negotiator.getPrimaryRole(), skillLevel, true);

        Skill negotiationSkill = negotiator.getSkill(S_NEGOTIATION);
        int bonus = (negotiationSkill == null) ? 0 : negotiationSkill.getBonus();
        int level = SkillType.getType(S_NEGOTIATION).getLevelFromExperience(skillLevel.getAdjustedValue());
        negotiator.addSkill(S_NEGOTIATION, level, bonus);
    }

    private static void assignRank(Person negotiator) {
        final PersonnelRole primaryRole = negotiator.getPrimaryRole();
        if (UNRANKED_ROLES.contains(primaryRole)) {
            return;
        }

        AutomaticRankAssigner.assignRankSystemFromFaction(negotiator, RO_MIN);
    }
}
