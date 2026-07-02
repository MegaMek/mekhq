/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.annotation.Nonnull;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.enums.PersonnelRole;

class SalariesOptionsModel {
    boolean disableSecondaryRoleSalary;
    double salaryAntiMekMultiplier;
    double salarySpecialistInfantryMultiplier;
    final Map<SkillLevel, Double> salaryXpMultipliers = new EnumMap<>(SkillLevel.class);
    final Map<PersonnelRole, Double> roleBaseSalaries = new EnumMap<>(PersonnelRole.class);

    SalariesOptionsModel(@Nonnull CampaignOptions options) {
        disableSecondaryRoleSalary = options.isDisableSecondaryRoleSalary();
        salaryAntiMekMultiplier = options.getSalaryAntiMekMultiplier();
        salarySpecialistInfantryMultiplier = options.getSalarySpecialistInfantryMultiplier();
        salaryXpMultipliers.putAll(options.getSalaryXPMultipliers());

        Money[] baseSalaryTable = options.getRoleBaseSalaries();
        for (PersonnelRole personnelRole : PersonnelRole.values()) {
            int ordinal = personnelRole.ordinal();
            roleBaseSalaries.put(personnelRole, baseSalaryTable[ordinal].getAmount().doubleValue());
        }
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.setDisableSecondaryRoleSalary(disableSecondaryRoleSalary);
        options.setSalaryAntiMekMultiplier(salaryAntiMekMultiplier);
        options.setSalarySpecialistInfantryMultiplier(salarySpecialistInfantryMultiplier);

        for (final Entry<SkillLevel, Double> entry : salaryXpMultipliers.entrySet()) {
            options.getSalaryXPMultipliers().put(entry.getKey(), entry.getValue());
        }

        for (final Entry<PersonnelRole, Double> entry : roleBaseSalaries.entrySet()) {
            options.setRoleBaseSalary(entry.getKey(), entry.getValue());
        }
    }
}
