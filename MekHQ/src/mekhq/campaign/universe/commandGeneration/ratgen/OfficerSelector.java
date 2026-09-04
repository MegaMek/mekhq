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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.campaign.personnel.skills.SkillType.S_STRATEGY;
import static mekhq.campaign.personnel.skills.SkillType.S_TACTICS;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import megamek.common.enums.SkillLevel;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;

/**
 * Ranks the people generated for a command by how well they would lead it.
 *
 * <p>The order is the one the retired AtB Company Generator used. Tactical Genius comes first: a person with
 * the ability outranks anyone without it. After that the caller chooses which matters more, the person's combat
 * experience level or the total of their Leadership, Strategy and Tactics skill levels; the other breaks ties.
 * The comparators here put the best person first.</p>
 */
public final class OfficerSelector {

    private static final List<String> COMMAND_SKILLS = List.of(S_LEADER, S_STRATEGY, S_TACTICS);

    private OfficerSelector() {
    }

    /**
     * Orders people best first for a leadership post.
     *
     * @param campaign            the campaign, whose options and date decide how skills are read
     * @param prioritizeCombat    {@code true} to rank by combat experience before command skills, {@code false} to
     *                            rank by command skills before combat experience; Tactical Genius leads either way
     *
     * @return a comparator placing the best candidate first
     */
    public static Comparator<Person> bestFirst(Campaign campaign, boolean prioritizeCombat) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseAgingEffects = campaignOptions.get(CampaignOption.USE_AGE_EFFECTS);
        boolean isClanCampaign = campaign.getPlayerForce().isClanForce();
        LocalDate today = campaign.getLocalDate();

        Comparator<Person> byTacticalGenius = Comparator.comparing(OfficerSelector::hasTacticalGenius);
        Comparator<Person> byCombat = Comparator.comparingInt(
              person -> person.getExperienceLevel(campaignOptions, isClanCampaign, today, false, true));
        Comparator<Person> byCommand = Comparator.comparingInt(
              person -> commandSkillTotal(person, isUseAgingEffects, isClanCampaign, today));

        Comparator<Person> worstFirst = prioritizeCombat
              ? byTacticalGenius.thenComparing(byCombat).thenComparing(byCommand)
              : byTacticalGenius.thenComparing(byCommand).thenComparing(byCombat);
        return worstFirst.reversed();
    }

    /**
     * Orders people best first for a seat in the leading lances: Tactical Genius, then combat experience.
     *
     * @param campaign the campaign, whose options and date decide how skills are read
     *
     * @return a comparator placing the most skilled pilot first
     */
    public static Comparator<Person> mostSkilledFirst(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isClanCampaign = campaign.getPlayerForce().isClanForce();
        LocalDate today = campaign.getLocalDate();

        Comparator<Person> byTacticalGenius = Comparator.comparing(OfficerSelector::hasTacticalGenius);
        Comparator<Person> byCombat = Comparator.comparingInt(
              person -> person.getExperienceLevel(campaignOptions, isClanCampaign, today, false, true));
        return byTacticalGenius.thenComparing(byCombat).reversed();
    }

    /**
     * A one-line account of what the orders see in a person, for the log: Tactical Genius if they have it, their
     * combat experience, and their command skill total.
     *
     * @param campaign the campaign, whose options and date decide how skills are read
     * @param person   the person
     *
     * @return for example {@code Jane Doe (Tactical Genius, combat Veteran, command 3)}
     */
    public static String describe(Campaign campaign, Person person) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseAgingEffects = campaignOptions.get(CampaignOption.USE_AGE_EFFECTS);
        boolean isClanCampaign = campaign.getPlayerForce().isClanForce();
        LocalDate today = campaign.getLocalDate();
        int combat = person.getExperienceLevel(campaignOptions, isClanCampaign, today, false, true);
        String genius = hasTacticalGenius(person) ? "Tactical Genius, " : "";
        return person.getFullName() + " (" + genius + "combat " + SkillLevel.parseFromInteger(combat)
              + ", command " + commandSkillTotal(person, isUseAgingEffects, isClanCampaign, today) + ")";
    }

    /**
     * @return the total of the person's Leadership, Strategy and Tactics skill levels; a skill they lack counts
     *       as nothing
     */
    static int commandSkillTotal(Person person, boolean isUseAgingEffects, boolean isClanCampaign,
          LocalDate today) {
        int total = 0;
        for (String skillName : COMMAND_SKILLS) {
            if (person.hasSkill(skillName)) {
                total += person.getSkillLevel(skillName, isUseAgingEffects, isClanCampaign, today);
            }
        }
        return total;
    }

    static boolean hasTacticalGenius(Person person) {
        return person.getOptions().booleanOption(OptionsConstants.MISC_TACTICAL_GENIUS);
    }
}
