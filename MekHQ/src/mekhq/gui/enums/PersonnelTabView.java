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
    GUNNERY_PILOT_SKILLS("PersonnelTabView.GUNNERY_PILOT_SKILLS.text",
          "PersonnelTabView.GUNNERY_PILOT_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, MEK, GROUND_VEHICLE, NAVAL_VEHICLE, VTOL)),
    GUNNERY_PILOT_SKILLS_II("PersonnelTabView.GUNNERY_PILOT_SKILLS_II.text",
          "PersonnelTabView.GUNNERY_PILOT_SKILLS_II.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, AEROSPACE, CONVENTIONAL_AIRCRAFT, VESSEL, ARTILLERY)),
    INFANTRY_SKILLS("PersonnelTabView.INFANTRY_SKILLS.text", "PersonnelTabView.INFANTRY_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, PROTOMEK, BATTLE_ARMOUR, SMALL_ARMS, ANTI_MEK)),
    TACTICAL_SKILLS("PersonnelTabView.TACTICAL_SKILLS.text", "PersonnelTabView.TACTICAL_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, TACTICS, STRATEGY, LEADERSHIP, NAVIGATION, SCOUTING)),
    TECHNICAL_SKILLS("PersonnelTabView.TECHNICAL_SKILLS.text", "PersonnelTabView.TECHNICAL_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, ASTECH, TECH_MEK, TECH_AERO, TECH_MECHANIC, TECH_BA,
                TECH_VESSEL, ZERO_G, WORK_MINUTES, TECH_MINUTES)),
    MEDICAL_SKILLS("PersonnelTabView.MEDICAL_SKILLS.text", "PersonnelTabView.MEDICAL_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, MEDTECH, MEDICAL, MEDICAL_CAPACITY)),
    ADMINISTRATIVE_SKILLS("PersonnelTabView.ADMINISTRATIVE_SKILLS.text",
          "PersonnelTabView.ADMINISTRATIVE_SKILLS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, ADMINISTRATION, NEGOTIATION, TRAINING, APPRAISAL)),
    TRAITS("PersonnelTabView.TRAITS.text", "PersonnelTabView.TRAITS.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, CONNECTIONS, WEALTH, EXTRA_INCOME, REPUTATION, UNLUCKY, BLOODMARK)),
    ATTRIBUTES("PersonnelTabView.ATTRIBUTES.text", "PersonnelTabView.ATTRIBUTES.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, STRENGTH, BODY, REFLEXES, DEXTERITY, INTELLIGENCE, WILLPOWER, CHARISMA,
                EDGE),
          Map.of(EDGE, CampaignOptions::isUseEdge)),
    PERSONALITY("PersonnelTabView.PERSONALITY.text", "PersonnelTabView.PERSONALITY.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, AGGRESSION, AMBITION, GREED, SOCIAL, REASONING),
          Map.of(AGGRESSION, CampaignOptions::isUseRandomPersonalities,
                AMBITION, CampaignOptions::isUseRandomPersonalities,
                GREED, CampaignOptions::isUseRandomPersonalities,
                SOCIAL, CampaignOptions::isUseRandomPersonalities,
                REASONING, CampaignOptions::isUseRandomPersonalities)),
    BIOGRAPHICAL("PersonnelTabView.BIOGRAPHICAL.text", "PersonnelTabView.BIOGRAPHICAL.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, AGE, PERSONNEL_STATUS, PERSONNEL_ROLE, HIGHEST_EDUCATION, ORIGIN_FACTION,
                ORIGIN_PLANET, SALARY),
          Map.of(ORIGIN_FACTION, CampaignOptions::isShowOriginFaction,
                ORIGIN_PLANET, CampaignOptions::isShowOriginFaction,
                SALARY, CampaignOptions::isPayForSalaries)),
    FLUFF("PersonnelTabView.FLUFF.text", "PersonnelTabView.FLUFF.toolTipText",
          Set.of(RANK, PRE_NOMINAL, GIVEN_NAME, SURNAME, SURNAME_GROUPED_BY_UNIT, BLOODNAME, POST_NOMINAL, CALLSIGN,
                GENDER, PERSONNEL_ROLE, KILLS)),
    DATES("PersonnelTabView.DATES.text", "PersonnelTabView.DATES.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, BIRTHDAY, DEATH_DATE, RECRUITMENT_DATE, RETIREMENT_DATE,
                LAST_RANK_CHANGE_DATE, DUE_DATE),
          Map.of(RECRUITMENT_DATE, CampaignOptions::isUseTimeInService,
                LAST_RANK_CHANGE_DATE, CampaignOptions::isUseTimeInRank,
                DUE_DATE, options -> options.isUseManualProcreation() || !options.getRandomProcreationMethod().isNone())),
    FAMILY("PersonnelTabView.FAMILY.text", "PersonnelTabView.FAMILY.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, IS_MARRIED, WANTS_CHILDREN, FORMER_SPOUSES,
                IMMEDIATE_FAMILY, EXTENDED_FAMILY, TOTAL_RELATIVES)),
    TRANSPORT("PersonnelTabView.TRANSPORT.text", "PersonnelTabView.TRANSPORT.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, SKILL_LEVEL, PERSONNEL_ROLE, SHIP_TRANSPORT, TACTICAL_TRANSPORT,
                UNIT_ASSIGNMENT)),
    EDUCATION("PersonnelTabView.EDUCATION.text", "PersonnelTabView.EDUCATION.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, HIGHEST_EDUCATION, CURRENT_EDUCATION, ACADEMY, COURSE,
                ACADEMY_DURATION)),
    LOCATION("PersonnelTabView.LOCATION.text", "PersonnelTabView.LOCATION.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PERSONNEL_ROLE, LOCATION_SYSTEM, LOCATION_PLANET, LOCATION_NAME,
                DESTINATION_SYSTEM, DESTINATION_PLANET, DESTINATION_NAME)),
    // Max 7 flags
    FLAGS_A("PersonnelTabView.FLAGS_A.text", "PersonnelTabView.FLAGS_A.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, COMMANDER, SECOND_IN_COMMAND, FOUNDER, CLAN_PERSONNEL, UNDER_PROTECTION,
                IMMORTAL)),
    FLAGS_B("PersonnelTabView.FLAGS_B.text", "PersonnelTabView.FLAGS_B.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, PREFERS_MEN, PREFERS_WOMEN, COVER_MEDICAL_EXPENSES,
                BLOCK_MATERNITY_LEAVE)),
    FLAGS_C("PersonnelTabView.FLAGS_C.text", "PersonnelTabView.FLAGS_C.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, SALVAGE_SUPERVISOR, QUICK_TRAIN_IGNORE, NEVER_ASSIGN_AUTO_MAINTENANCE,
                HIDE_PERSONALITY)),
    OTHER("PersonnelTabView.OTHER.text", "PersonnelTabView.OTHER.toolTipText",
          Set.of(RANK, FIRST_NAME, LAST_NAME, TOUGHNESS, FATIGUE, SPA_COUNT, IMPLANT_COUNT, MODIFICATION_COUNT,
                LOYALTY),
          Map.of(TOUGHNESS, CampaignOptions::isUseToughness,
                FATIGUE, CampaignOptions::isUseFatigue,
                SPA_COUNT, CampaignOptions::isUseAbilities,
                IMPLANT_COUNT, CampaignOptions::isUseImplants,
                MODIFICATION_COUNT, CampaignOptions::isUseAlternativeAdvancedMedical,
                LOYALTY, options -> options.isUseLoyaltyModifiers() && !options.isUseHideLoyalty()));

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
