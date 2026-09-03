/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import static mekhq.gui.enums.PersonnelTableModelColumn.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;

public enum PersonnelTabView {
    GRAPHIC("PersonnelTabView.GRAPHIC.text", "PersonnelTabView.GRAPHIC.toolTipText",
          Set.of(PERSON_GRAPHICAL, FORCE_GRAPHICAL, UNIT_ASSIGNMENT_GRAPHICAL)),
    GENERAL("PersonnelTabView.GENERAL.text", "PersonnelTabView.GENERAL.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, SKILL_LEVEL, PERSONNEL_ROLE, FORCE, DEPLOYED, INJURIES,
                UNIT_ASSIGNMENT, XP)),
    COMBAT("PersonnelTabView.COMBAT.text", "PersonnelTabView.COMBAT.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, AGGREGATE_COMBAT, ARTILLERY, SCOUTING, LEADERSHIP,
                TACTICS, STRATEGY)),
    TECH("PersonnelTabView.TECH.text", "PersonnelTabView.TECH.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, AGGREGATE_TECH, TECH_UNIT_ASSIGNMENT,
                REMAINING_TECH_MINUTES, MAINTENANCE_TECH_MINUTES, MAX_TECH_MINUTES)),
    HEALTHCARE("PersonnelTabView.HEALTHCARE.text", "PersonnelTabView.HEALTHCARE.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, INJURIES, TOUGHNESS, BODY, COVER_MEDICAL_EXPENSES,
                DUE_DATE, MODIFICATION_COUNT),
          Map.of(TOUGHNESS,
                options -> options.get(CampaignOption.USE_TOUGHNESS),
                MODIFICATION_COUNT,
                options -> options.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL),
                DUE_DATE,
                options -> options.get(CampaignOption.USE_MANUAL_PROCREATION) || !options.get(CampaignOption.RANDOM_PROCREATION_METHOD).isNone())),
    GUNNERY_PILOT_SKILLS("PersonnelTabView.GUNNERY_PILOT_SKILLS.text",
          "PersonnelTabView.GUNNERY_PILOT_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, MEK, GROUND_VEHICLE, NAVAL_VEHICLE, VTOL)),
    GUNNERY_PILOT_SKILLS_II("PersonnelTabView.GUNNERY_PILOT_SKILLS_II.text",
          "PersonnelTabView.GUNNERY_PILOT_SKILLS_II.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, AEROSPACE, CONVENTIONAL_AIRCRAFT, VESSEL, NAVIGATION)),
    INFANTRY_SKILLS("PersonnelTabView.INFANTRY_SKILLS.text", "PersonnelTabView.INFANTRY_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, PROTOMEK, BATTLE_ARMOUR, SMALL_ARMS, ANTI_MEK)),
    TECHNICAL_SKILLS("PersonnelTabView.TECHNICAL_SKILLS.text", "PersonnelTabView.TECHNICAL_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, ASTECH, TECH_MEK, TECH_AERO, TECH_MECHANIC, TECH_BA,
                TECH_VESSEL, ZERO_G, NEVER_ASSIGN_AUTO_MAINTENANCE, SALVAGE_SUPERVISOR)),
    MEDICAL_SKILLS("PersonnelTabView.MEDICAL_SKILLS.text", "PersonnelTabView.MEDICAL_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, MEDTECH, MEDICAL, MEDICAL_CAPACITY)),
    ADMINISTRATIVE_SKILLS("PersonnelTabView.ADMINISTRATIVE_SKILLS.text",
          "PersonnelTabView.ADMINISTRATIVE_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, ADMINISTRATION, NEGOTIATION, TRAINING, APPRAISAL)),
    TRAITS("PersonnelTabView.TRAITS.text", "PersonnelTabView.TRAITS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, CONNECTIONS, WEALTH, EXTRA_INCOME, FAME, UNLUCKY, BLOODMARK)),
    ATTRIBUTES("PersonnelTabView.ATTRIBUTES.text", "PersonnelTabView.ATTRIBUTES.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, STRENGTH, BODY, REFLEXES, DEXTERITY, INTELLIGENCE, WILLPOWER, CHARISMA,
                EDGE),
          Map.of(EDGE, options -> options.get(CampaignOption.USE_EDGE))),
    TURNOVER_AND_RETENTION("PersonnelTabView.TURNOVER_AND_RETENTION.text",
          "PersonnelTabView.TURNOVER_AND_RETENTION.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, SKILL_LEVEL, AGE, FATIGUE, LOYALTY, LAST_RANK_CHANGE_DATE,
                MANAGEMENT_MODIFIER, FACTION_MODIFIER),
          Map.of(FATIGUE, options -> options.get(CampaignOption.USE_FATIGUE),
                MANAGEMENT_MODIFIER, options -> options.get(CampaignOption.USE_MANAGEMENT_SKILL),
                FACTION_MODIFIER, options -> options.get(CampaignOption.USE_FACTION_MODIFIERS),
                LAST_RANK_CHANGE_DATE, options -> options.get(CampaignOption.USE_TIME_IN_RANK),
                LOYALTY, options -> options.get(CampaignOption.USE_LOYALTY_MODIFIERS) && !options.get(CampaignOption.USE_HIDE_LOYALTY))),
    PERSONALITY("PersonnelTabView.PERSONALITY.text", "PersonnelTabView.PERSONALITY.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, HIDE_PERSONALITY, AGGRESSION, AMBITION, GREED, SOCIAL, REASONING),
          Map.of(AGGRESSION, options -> options.get(CampaignOption.USE_RANDOM_PERSONALITIES),
                AMBITION, options -> options.get(CampaignOption.USE_RANDOM_PERSONALITIES),
                GREED, options -> options.get(CampaignOption.USE_RANDOM_PERSONALITIES),
                SOCIAL, options -> options.get(CampaignOption.USE_RANDOM_PERSONALITIES),
                REASONING, options -> options.get(CampaignOption.USE_RANDOM_TALENT))),
    SERVICE_RECORD("PersonnelTabView.SERVICE_RECORD.text", "PersonnelTabView.SERVICE_RECORD.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, COMMAND_STATUS, FOUNDER, RECRUITMENT_DATE,
                RETIREMENT_DATE, SALARY, KILLS, REPUTATION),
          Map.of(RECRUITMENT_DATE, options -> options.get(CampaignOption.USE_TIME_IN_SERVICE),
                REPUTATION, options -> options.get(CampaignOption.USE_CHAOS_REPUTATION) &&
                                             !options.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION))),
    BIOGRAPHICAL("PersonnelTabView.BIOGRAPHICAL.text", "PersonnelTabView.BIOGRAPHICAL.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, AGE, DEATH_DATE, PERSONNEL_STATUS, BLOODNAME, ORIGIN),
          Map.of(ORIGIN, options -> options.get(CampaignOption.SHOW_ORIGIN_FACTION),
                SALARY, options -> options.get(CampaignOption.PAY_FOR_SALARIES))),
    FLUFF("PersonnelTabView.FLUFF.text", "PersonnelTabView.FLUFF.toolTipText",
          Set.of(RANK, PRE_NOMINAL, GIVEN_NAME, SURNAME, SURNAME_GROUPED_BY_UNIT, POST_NOMINAL, CALLSIGN,
                GENDER, PERSONNEL_ROLE)),
    FAMILY("PersonnelTabView.FAMILY.text", "PersonnelTabView.FAMILY.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PREFERENCE, IS_MARRIED, WANTS_CHILDREN, FORMER_SPOUSES,
                IMMEDIATE_FAMILY, EXTENDED_FAMILY, TOTAL_RELATIVES)),
    TRANSPORT("PersonnelTabView.TRANSPORT.text", "PersonnelTabView.TRANSPORT.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, SKILL_LEVEL, PERSONNEL_ROLE, SHIP_TRANSPORT, TACTICAL_TRANSPORT,
                UNIT_ASSIGNMENT)),
    EDUCATION("PersonnelTabView.EDUCATION.text", "PersonnelTabView.EDUCATION.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, HIGHEST_EDUCATION, CURRENT_EDUCATION, ACADEMY, COURSE,
                ACADEMY_DURATION)),
    LOCATION("PersonnelTabView.LOCATION.text", "PersonnelTabView.LOCATION.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, LOCATION_SYSTEM_AND_PLANET, LOCATION_NAME,
                DESTINATION_SYSTEM_AND_PLANET, DESTINATION_NAME)),
    ARMOR_KITS("PersonnelTabView.ARMOR_KITS.text", "PersonnelTabView.ARMOR_KITS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, UNIT_ASSIGNMENT, ARMOR_KIT, ARMOR_KIT_INTENDED)),
    COMBAT_ROLES("PersonnelTabView.COMBAT_ROLES.text", "PersonnelTabView.COMBAT_ROLES.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, UNIT_ASSIGNMENT, FORCE, COMBAT_ROLE)),
    OTHER("PersonnelTabView.OTHER.text", "PersonnelTabView.OTHER.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, SPA_COUNT, IMPLANT_COUNT, QUICK_TRAIN_IGNORE, CLAN_PERSONNEL,
                UNDER_PROTECTION, IMMORTAL, BLOCK_MATERNITY_LEAVE),
          Map.of(SPA_COUNT, options -> options.get(CampaignOption.USE_ABILITIES),
                IMPLANT_COUNT, options -> options.get(CampaignOption.USE_IMPLANTS)));

    private final String name;
    private final String toolTipText;
    private final Set<PersonnelTableModelColumn> columns;
    private final EnumMap<PersonnelTableModelColumn, Function<CampaignOptions, Boolean>> optionalColumns;

    /**
     * Defines a personnel table view.
     *
     * @param name            View name
     * @param toolTipText     View tooltip
     * @param columns         Columns this view includes; must include all optional columns
     * @param optionalColumns An override for column visibility based on {@link CampaignOptions}
     */
    PersonnelTabView(String name, String toolTipText, Set<PersonnelTableModelColumn> columns,
          @Nullable Map<PersonnelTableModelColumn, Function<CampaignOptions, Boolean>> optionalColumns) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.columns = columns;
        if (optionalColumns == null) {
            this.optionalColumns = null;
        } else {
            this.optionalColumns = new EnumMap<>(optionalColumns);
        }
    }

    PersonnelTabView(String name, String toolTipText, Set<PersonnelTableModelColumn> columns) {
        this(name, toolTipText, columns, null);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    @Override
    public String toString() {
        return name;
    }

    public Set<PersonnelTableModelColumn> getVisibleColumns(CampaignOptions campaignOptions) {
        if (optionalColumns == null) {
            return columns;
        }
        return columns.stream().filter(column -> {
            Function<CampaignOptions, Boolean> condition = optionalColumns.get(column);
            return (condition == null) || condition.apply(campaignOptions);
        }).collect(Collectors.toSet());
    }
}
