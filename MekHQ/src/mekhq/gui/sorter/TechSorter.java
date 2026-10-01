/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.sorter;

import java.util.Comparator;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.work.IPartWork;

/**
 * Orders the techs offered a task: the unit's own techs first when the campaign asks for that, then the least skilled
 * tech who can do the job, judged by the skill this task uses, then the tech with the most time left today. With no
 * task selected, techs are ordered by their best tech skill.
 *
 * @author Jay Lawson
 */
public class TechSorter implements Comparator<Person> {
    private IPartWork partWork;
    private boolean isAssignedFirst;

    public TechSorter() {
        this(null);
    }

    public TechSorter(@Nullable IPartWork partWork) {
        this.partWork = partWork;
    }

    @Override
    public int compare(Person firstTech, Person secondTech) {
        if (partWork != null) {
            if (isAssignedFirst) {
                boolean isFirstAssigned = isAssignedToTheTasksUnit(firstTech);
                boolean isSecondAssigned = isAssignedToTheTasksUnit(secondTech);
                if (isFirstAssigned != isSecondAssigned) {
                    return isFirstAssigned ? -1 : 1;
                }
            }
            int skillCompare = Integer.compare(getTaskSkillLevel(firstTech), getTaskSkillLevel(secondTech));
            if (skillCompare != 0) {
                return skillCompare;
            }
        } else {
            int skillCompare = Integer.compare(firstTech.getBestTechLevel(), secondTech.getBestTechLevel());
            if (skillCompare != 0) {
                return skillCompare;
            }
        }
        // More time left first, so the job is most likely to be finished today
        int timeCompare = Integer.compare(secondTech.getMinutesLeft(), firstTech.getMinutesLeft());
        if (timeCompare != 0) {
            return timeCompare;
        }
        // A fixed last resort, so two techs alike in every way keep the same order after a refresh
        return firstTech.getId().compareTo(secondTech.getId());
    }

    private boolean isAssignedToTheTasksUnit(Person tech) {
        return (partWork.getUnit() != null) && tech.getTechUnits().contains(partWork.getUnit());
    }

    /**
     * @return the tech's experience level in the skill this task uses; a tech without that skill sorts last
     */
    private int getTaskSkillLevel(Person tech) {
        Skill skill = tech.getSkillForWorkingOn(partWork);
        return (skill == null) ? Integer.MAX_VALUE : skill.getExperienceLevel(tech.getSkillModifierData());
    }

    /**
     * @param partWork the task the techs are listed for, or {@code null} when no task is selected
     */
    public void setPart(@Nullable IPartWork partWork) {
        this.partWork = partWork;
    }

    public void clearPart() {
        partWork = null;
    }

    /**
     * @param isAssignedFirst {@code true} to list the techs assigned to the task's unit first
     */
    public void setAssignedFirst(boolean isAssignedFirst) {
        this.isAssignedFirst = isAssignedFirst;
    }
}
