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
package mekhq.campaign.work;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * Decides which techs a repair or salvage task is offered to. The Repair tab and the Warehouse tab's repair bench both
 * ask here, so the two lists follow the same rules, and each refusal comes with its reason.
 */
public final class RepairTechEligibility {
    private RepairTechEligibility() {}

    /**
     * The tech list's two switches.
     *
     * @param isShowingOnlyUnitTechs only techs of the profession that suits the unit, such as mechanics for vehicles
     * @param isShowingAllTechs      everyone with the right skill, not only personnel in a technician role
     */
    public record TechListToggles(boolean isShowingOnlyUnitTechs, boolean isShowingAllTechs) {}

    /** Why a tech is not offered a task. */
    public enum Refusal {
        /** The task needs no work. */
        TASK_NOT_NEEDED,
        /** The tech is not where the task is. */
        AT_ANOTHER_LOCATION,
        /** The tech is crew on a large craft and only works on that craft. */
        CREW_OF_ANOTHER_CRAFT,
        /** The tech's profession does not suit the unit, and the list shows only suitable techs. */
        WRONG_PROFESSION,
        /** The tech lacks the skill the task needs. */
        WRONG_SKILL,
        /** The campaign requires a tool kit and the tech has none. */
        NO_TOOL_KIT,
        /** Self-maintaining infantry are repaired only by their own engineer. */
        NOT_THE_UNITS_ENGINEER,
        /** A self-crewed unit is repaired only by its own crew. */
        NOT_THE_UNITS_CREW,
        /** Vessel crew do not work on other units. */
        VESSEL_CREW_ON_ANOTHER_UNIT,
        /** The tech is not in a technician role, and the list shows only technicians. */
        NOT_A_TECHNICIAN,
        /** No skill applies to the task. */
        NO_SKILL,
        /** The task can no longer be done by anyone. */
        TASK_IMPOSSIBLE,
        /** The tech has no time left today. */
        NO_TIME_LEFT,
        /** The task needs a better tech than this one. */
        SKILL_TOO_LOW
    }

    /**
     * @param campaign the campaign
     * @param partWork the task
     * @param unit     the unit the task is on, or {@code null} for a spare on the warehouse bench
     * @param tech     the tech
     * @param toggles  the tech list's switches
     *
     * @return why the tech is not offered the task, or {@code null} if the tech can take it
     */
    public static @Nullable Refusal findRefusal(Campaign campaign, IPartWork partWork, @Nullable Unit unit,
          Person tech, TechListToggles toggles) {
        if (!partWork.needsFixing() && !partWork.isSalvaging()) {
            return Refusal.TASK_NOT_NEEDED;
        }
        if (!RepairLocationCheck.isTechAtTask(tech, partWork)) {
            return Refusal.AT_ANOTHER_LOCATION;
        }
        if (isCrewOfAnotherLargeCraft(tech)) {
            return Refusal.CREW_OF_ANOTHER_CRAFT;
        }
        boolean isProfessionChecked = toggles.isShowingOnlyUnitTechs() && (unit != null);
        if (isProfessionChecked && !tech.isRightTechProfessionFor(unit)) {
            return Refusal.WRONG_PROFESSION;
        }
        // Every tech, a self-crewed unit's engineer included, needs the task's skill and, where the campaign demands
        // one, a tool kit, whatever the switches say
        if (!tech.isRightTechTypeFor(partWork)) {
            return Refusal.WRONG_SKILL;
        }
        boolean isToolKitRequired = EquipmentKitCatalog.isToolKitRequired(campaign.getCampaignOptions(), unit);
        if (isToolKitRequired && !EquipmentKitCatalog.hasToolKit(tech)) {
            return Refusal.NO_TOOL_KIT;
        }
        Refusal crewRefusal = findCrewRefusal(unit, tech, toggles);
        if (crewRefusal != null) {
            return crewRefusal;
        }
        return findSkillRefusal(campaign, partWork, tech);
    }

    /** Vessel crew on a large craft repair only as that craft's engineer. */
    private static boolean isCrewOfAnotherLargeCraft(Person tech) {
        Unit assignedUnit = tech.getUnit();
        boolean isOnLargeCraft = (assignedUnit != null) && (assignedUnit.getEntity() != null)
                                       && assignedUnit.getEntity().isLargeCraft();
        return tech.getPrimaryRole().isVesselCrew() && isOnLargeCraft && !tech.equals(assignedUnit.getEngineer());
    }

    private static @Nullable Refusal findCrewRefusal(@Nullable Unit unit, Person tech, TechListToggles toggles) {
        boolean isVesselCrew = tech.getPrimaryRole().isVesselCrew();
        if ((unit != null) && unit.isSelfMaintainedInfantry()) {
            // Self-maintaining infantry repair their own gear, led by the unit's engineer (its ranking soldier)
            return tech.equals(unit.getEngineer()) ? null : Refusal.NOT_THE_UNITS_ENGINEER;
        }
        if ((unit != null) && unit.isSelfCrewed()) {
            boolean isThisUnitsCrew = isVesselCrew && unit.equals(tech.getUnit());
            return isThisUnitsCrew ? null : Refusal.NOT_THE_UNITS_CREW;
        }
        if (isVesselCrew && (unit != null)) {
            return Refusal.VESSEL_CREW_ON_ANOTHER_UNIT;
        }
        if (!toggles.isShowingAllTechs() && !tech.isTechExpanded()) {
            return Refusal.NOT_A_TECHNICIAN;
        }
        return null;
    }

    private static @Nullable Refusal findSkillRefusal(Campaign campaign, IPartWork partWork, Person tech) {
        Skill skill = tech.getSkillForWorkingOn(partWork);
        if (skill == null) {
            return Refusal.NO_SKILL;
        }
        if (partWork.getSkillMin() > SkillType.EXP_LEGENDARY) {
            return Refusal.TASK_IMPOSSIBLE;
        }
        if (tech.getMinutesLeft() <= 0) {
            return Refusal.NO_TIME_LEFT;
        }
        // With destroy-by-margin a failed roll damages the part by how much it failed, so any tech may try
        if (campaign.getCampaignOptions().get(CampaignOption.DESTROY_BY_MARGIN)) {
            return null;
        }
        int effectiveLevel = skill.getExperienceLevel(tech.getSkillModifierData()) - partWork.getMode().expReduction;
        return (partWork.getSkillMin() <= effectiveLevel) ? null : Refusal.SKILL_TOO_LOW;
    }
}
