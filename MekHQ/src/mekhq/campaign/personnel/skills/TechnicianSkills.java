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
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.SkillType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

public class TechnicianSkills {
    private static final List<String> TECH_GENERAL_SKILLS = List.of(S_TECH_ELECTRONIC,
          S_TECH_MECHANICAL,
          S_TECH_NUCLEAR,
          S_TECH_WEAPONS);
    private static final List<String> TECH_MEK_SKILLS = List.of(S_TECH_ELECTRONIC,
          S_TECH_JETS,
          S_TECH_MECHANICAL,
          S_TECH_MYOMER,
          S_TECH_NUCLEAR);
    private static final List<String> TECH_BA_SKILLS = List.of(S_TECH_ELECTRONIC, S_TECH_MECHANICAL, S_TECH_MYOMER);
    private static final List<String> TECH_AERO_SKILLS = List.of(S_TECH_AERONAUTICS, S_TECH_NUCLEAR, S_TECH_JETS);
    private static final List<String> TECH_VEHICLE_SKILLS = List.of(S_TECH_ELECTRONIC,
          S_TECH_MECHANICAL,
          S_TECH_NUCLEAR);

    public static @Nonnull List<String> getTechSupplementalSkills(Person person, boolean isSecondary) {
        PersonnelRole role = isSecondary ? person.getSecondaryRole() : person.getPrimaryRole();

        return getSupplementalSkills(role);
    }

    private static @Nonnull ArrayList<String> getSupplementalSkills(PersonnelRole role) {
        Set<String> supplementalSkills = new HashSet<>();

        if (role.isTech()) {
            supplementalSkills.addAll(getSpecificProfessionSkills(role));
            supplementalSkills.addAll(TECH_GENERAL_SKILLS);
        }

        return new ArrayList<>(supplementalSkills);
    }

    private static @Nonnull List<String> getSpecificProfessionSkills(PersonnelRole role) {
        return switch (role) {
            case MEK_TECH -> TECH_MEK_SKILLS;
            case BA_TECH -> TECH_BA_SKILLS;
            case AERO_TEK, VESSEL_CREW -> TECH_AERO_SKILLS;
            case MECHANIC -> TECH_VEHICLE_SKILLS;
            default -> Collections.emptyList();
        };
    }

    public static void addMissingSkills(Person person) {
        addMissingSkills(person, false);
        addMissingSkills(person, true);
    }

    private static void addMissingSkills(Person person, boolean isSecondary) {
        PersonnelRole role = isSecondary ? person.getSecondaryRole() : person.getPrimaryRole();

        String sampleSkill = getProfessionSampleSkill(role);
        Skill sample = person.getSkill(sampleSkill);
        if (sample == null) {
            return;
        }

        List<String> supplementalSkills = getSupplementalSkills(role);

        int targetLevel = sample.getLevel();
        for (String skillName : supplementalSkills) {
            if (!person.hasSkill(skillName)) {
                person.addSkill(skillName, targetLevel, 0);
            }
        }
    }

    private static @Nonnull String getProfessionSampleSkill(PersonnelRole role) {
        return switch (role) {
            case MEK_TECH -> S_TECH_MEK;
            case BA_TECH -> S_TECH_BA;
            case AERO_TEK -> S_TECH_AERO;
            case VESSEL_CREW -> S_TECH_VESSEL;
            case MECHANIC -> S_TECH_VEHICLE;
            default -> "";
        };
    }
}
