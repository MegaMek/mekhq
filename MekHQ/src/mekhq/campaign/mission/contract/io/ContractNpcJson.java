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
package mekhq.campaign.mission.contract.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.common.enums.Gender;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.units.Crew;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;

/**
 * A JSON snapshot of one of a contract's NPCs - the employer's negotiator or liaison, or the opposing commander.
 *
 * <p>A contract offer carries these as full {@link Person} records, but only part of a person matters to the offer:
 * who they are (name, gender, role, rank and portrait) and what they bring to the negotiating table (attributes,
 * skills and special abilities). The snapshot keeps exactly that. On import the NPC is rebuilt the way the market
 * generates one - a fresh person of the same role and faction - and the snapshot is layered on top, so anything not
 * captured here (biography, personality, and so on) is simply regenerated.</p>
 *
 * @param givenName    the NPC's given name
 * @param surname      the NPC's surname
 * @param gender       the NPC's {@link Gender} name
 * @param role         the NPC's primary {@link PersonnelRole} name
 * @param factionCode  the NPC's origin faction short name, or {@code null} if they have none
 * @param phenotype    the NPC's {@link Phenotype} name
 * @param clan         whether the NPC is Clan personnel
 * @param rankSystem   the code of the NPC's rank system, or {@code null} if they have none
 * @param rank         the NPC's numeric rank
 * @param rankLevel    the NPC's rank sublevel
 * @param portrait     the NPC's portrait, or {@code null} for the default
 * @param attributes   the NPC's raw attribute scores, keyed by {@link SkillAttribute} name
 * @param skills       the NPC's skills, keyed by skill name
 * @param advantages   the NPC's special abilities, in the {@code name[ value]} form a {@link Person} saves them in
 * @param edgeTriggers the NPC's enabled Edge triggers, in the same form
 *
 * @author Illiani
 * @since 0.51.01
 */
public record ContractNpcJson(String givenName, String surname, String gender, String role,
      @Nullable String factionCode, String phenotype, boolean clan, @Nullable String rankSystem, int rank,
      int rankLevel, @Nullable IconJson portrait, Map<String, Integer> attributes, Map<String, SkillJson> skills,
      List<String> advantages, List<String> edgeTriggers) {
    private static final MMLogger LOGGER = MMLogger.create(ContractNpcJson.class);

    /** The separator {@link Person#getOptionList(String, String)} places between abilities. */
    private static final String OPTION_DELIMITER = "::";

    /**
     * A skill's level and bonus.
     *
     * @param level           the skill level
     * @param bonus           the skill bonus
     * @param naturalAptitude whether the NPC has a Natural Aptitude in the skill
     */
    public record SkillJson(int level, int bonus, boolean naturalAptitude) {}

    /**
     * A portrait or camouflage, by its directory category and file name.
     *
     * @param category the icon's category
     * @param filename the icon's file name
     */
    public record IconJson(@Nullable String category, @Nullable String filename) {}

    /**
     * Captures an NPC.
     *
     * @param person the NPC, or {@code null}
     *
     * @return the snapshot, or {@code null} if {@code person} is {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable ContractNpcJson from(@Nullable Person person) {
        if (person == null) {
            return null;
        }

        Map<String, Integer> attributes = new LinkedHashMap<>();
        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute != SkillAttribute.NO_ATTRIBUTE) {
                attributes.put(attribute.name(), person.getATOWAttributes().getAttribute(attribute));
            }
        }

        Map<String, SkillJson> skills = new LinkedHashMap<>();
        for (Skill skill : person.getSkills().getSkills()) {
            skills.put(skill.getType().getName(),
                  new SkillJson(skill.getLevel(), skill.getBonus(), skill.getHasNaturalAptitude()));
        }

        Faction origin = person.getOriginFaction();
        RankSystem rankSystem = person.getRankSystem();
        Portrait portrait = person.getPortrait();

        return new ContractNpcJson(person.getGivenName(),
              person.getSurname(),
              person.getGender().name(),
              person.getPrimaryRole().name(),
              origin == null ? null : origin.getShortName(),
              person.getPhenotype().name(),
              person.isClanPersonnel(),
              rankSystem == null ? null : rankSystem.getCode(),
              person.getRankNumeric(),
              person.getRankLevel(),
              portrait == null ? null : new IconJson(portrait.getCategory(), portrait.getFilename()),
              attributes,
              skills,
              splitOptions(person.getOptionList(OPTION_DELIMITER, PersonnelOptions.LVL3_ADVANTAGES)),
              splitOptions(person.getOptionList(OPTION_DELIMITER, PersonnelOptions.EDGE_ADVANTAGES)));
    }

    /**
     * Rebuilds the NPC in the given campaign: a freshly generated person of the snapshot's role and faction, with the
     * snapshot's identity, attributes, skills and abilities applied over the top.
     *
     * @param campaign the campaign the NPC is being imported into
     *
     * @return the rebuilt NPC
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Person toPerson(Campaign campaign) {
        PersonnelRole primaryRole = PersonnelRole.fromString(role);
        Person person = campaign.getPlayerForce()
                              .getHumanResources()
                              .newPerson(campaign, primaryRole, factionCode, Gender.parseFromString(gender));

        person.setGivenName(givenName);
        person.setSurname(surname);
        person.setPhenotype(Phenotype.fromString(phenotype));
        person.setClanPersonnel(clan);
        if (portrait != null) {
            person.setPortrait(new Portrait(portrait.category(), portrait.filename()));
        }

        // A rank system the importing campaign does not know (a custom one, say) leaves the generated one in place.
        RankSystem importedRankSystem = rankSystem == null ? null : Ranks.getRankSystemFromCode(rankSystem);
        if (importedRankSystem != null) {
            person.setRankSystem(new RankValidator(), importedRankSystem);
        }
        person.setRank(rank);
        person.setRankLevel(rankLevel);

        // Abilities go on before attributes, as they can change the caps an attribute score is held to.
        person.getOptions().getOptionsList().forEach(IOption::clearValue);
        applyOptions(person, advantages);
        applyOptions(person, edgeTriggers);

        person.getSkills().clear();
        for (Map.Entry<String, SkillJson> entry : safe(skills).entrySet()) {
            SkillJson skill = entry.getValue();
            person.addSkill(entry.getKey(), skill.level(), skill.bonus());
            Skill added = person.getSkill(entry.getKey());
            if (added != null) {
                added.setHasNaturalAptitude(skill.naturalAptitude());
            }
        }

        for (Map.Entry<String, Integer> entry : safe(attributes).entrySet()) {
            try {
                person.setAttributeScore(SkillAttribute.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Unknown NPC attribute '{}' ignored.", entry.getKey());
            }
        }
        person.setCurrentEdge(person.getEdge());

        return person;
    }

    private static List<String> splitOptions(String optionList) {
        List<String> options = new ArrayList<>();
        for (String option : optionList.split(OPTION_DELIMITER)) {
            if (!option.isBlank()) {
                options.add(option.trim());
            }
        }
        return options;
    }

    private static void applyOptions(Person person, @Nullable List<String> options) {
        if (options == null) {
            return;
        }
        for (String option : options) {
            String name = Crew.parseAdvantageName(option);
            try {
                person.getOptions().getOption(name).setValue(Crew.parseAdvantageValue(option));
            } catch (Exception ex) {
                LOGGER.warn("Unknown NPC ability '{}' ignored.", option);
            }
        }
    }

    private static <K, V> Map<K, V> safe(@Nullable Map<K, V> map) {
        return map == null ? Map.of() : map;
    }
}
