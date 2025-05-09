/*
 * Copyright (c) 2013 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.report;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * @author Jay Lawson
 */
public class PersonnelReport extends AbstractReport {
    //region Constructors
    public PersonnelReport(final Campaign campaign) {
        super(campaign);
    }
    //endregion Constructors

    public String getCombatPersonnelDetails() {
        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        int[] countPersonByType = new int[personnelRoles.length];
        int countTotal = 0;
        int countInjured = 0;
        int countMIA = 0;
        int countKIA = 0;
        int countDead = 0;
        int countStudents = 0;
        int countRetired = 0;
        Money salary = Money.zero();

        for (Person p : getCampaign().getPersonnel()) {
            if ((!p.getPrimaryRole().isCombat()) || (!p.getPrisonerStatus().isFreeOrBondsman())) {
                continue;
            }

            // Add them to the total count
            if (p.getStatus().isActive()) {
                countPersonByType[p.getPrimaryRole().ordinal()]++;
                countTotal++;
                if (getCampaign().getCampaignOptions().isUseAdvancedMedical() && !p.getInjuries().isEmpty()) {
                    countInjured++;
                } else if (p.getHits() > 0) {
                    countInjured++;
                }
                salary = salary.plus(p.getSalary(getCampaign()));
            } else if ((p.getPrisonerStatus().isBondsman()) && (p.getStatus().isActive())) {
                if (!p.getInjuries().isEmpty() || (p.getHits() > 0)) {
                    countInjured++;
                }
            } else if (p.getStatus().isRetired()) {
                countRetired++;
            } else if (p.getStatus().isMIA()) {
                countMIA++;
            } else if (p.getStatus().isKIA()) {
                countKIA++;
                countDead++;
            } else if (p.getStatus().isDead()) {
                countDead++;
            } else if (p.getStatus().isStudent()) {
                countStudents++;
            }

        }

        StringBuilder sb = new StringBuilder("Combat Personnel\n\n");

        sb.append(String.format("%-30s        %4s\n", "Total Combat Personnel", countTotal));

        for (PersonnelRole role : personnelRoles) {
            if (role.isCombat()) {
                sb.append(String.format("    %-30s    %4s\n",
                      role.getLabel(getCampaign().getFaction().isClan()),
                      countPersonByType[role.ordinal()]));
            }
        }

        sb.append('\n')
              .append(String.format("%-30s        %4s\n", "Injured Combat Personnel", countInjured))
              .append(String.format("%-30s        %4s\n", "MIA Combat Personnel", countMIA))
              .append(String.format("%-30s        %4s\n", "KIA Combat Personnel", countKIA))
              .append(String.format("%-30s        %4s\n", "Retired Combat Personnel", countRetired))
              .append(String.format("%-30s        %4s\n", "Dead Combat Personnel", countDead))
              .append(String.format("%-30s        %4s\n", "Student Combat Personnel", countStudents))
              .append("\nMonthly Salary For Combat Personnel: ")
              .append(salary.toAmountAndSymbolString());

        return sb.toString();
    }

    public String getSupportPersonnelDetails() {
        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        int[] countPersonByType = new int[personnelRoles.length];
        int countTotal = 0;
        int countInjured = 0;
        int countMIA = 0;
        int countKIA = 0;
        int countDead = 0;
        int countStudents = 0;
        int countRetired = 0;
        Money salary = Money.zero();
        int prisoners = 0;
        int bondsmen = 0;
        int dependents = 0;
        int dependentStudents = 0;
        int children = 0;
        int childrenStudents = 0;
        Money civilianSalaries = Money.zero();

        for (Person person : getCampaign().getPersonnel()) {
            // Add them to the total count
            final boolean primarySupport = person.getPrimaryRole().isSupport(true);

            if (primarySupport && person.getPrisonerStatus().isFree() && person.getStatus().isActive()) {
                countPersonByType[person.getPrimaryRole().ordinal()]++;
                countTotal++;
                if (!person.getInjuries().isEmpty() || (person.getHits() > 0)) {
                    countInjured++;
                }
                salary = salary.plus(person.getSalary(getCampaign()));
            } else if (person.getPrisonerStatus().isCurrentPrisoner() && person.getStatus().isActive()) {
                prisoners++;
            } else if (person.getPrisonerStatus().isBondsman() && person.getStatus().isActive()) {
                bondsmen++;
                if (!person.getInjuries().isEmpty() || (person.getHits() > 0)) {
                    countInjured++;
                }
            } else if (primarySupport && person.getStatus().isRetired()) {
                countRetired++;
            } else if (primarySupport && person.getStatus().isMIA()) {
                countMIA++;
            } else if (primarySupport && person.getStatus().isKIA()) {
                countKIA++;
                countDead++;
            } else if (primarySupport && person.getStatus().isDead()) {
                countDead++;
            } else if (primarySupport && person.getStatus().isStudent()) {
                countStudents++;
            }

            LocalDate today = getCampaign().getLocalDate();
            if (person.getPrimaryRole().isDependent() &&
                      !person.getStatus().isDepartedUnit() &&
                      person.getPrisonerStatus().isFree()) {
                if (person.isChild(today)) {
                    if (person.getStatus().isStudent()) {
                        childrenStudents++;
                    }
                    children++;
                } else {
                    if (person.getStatus().isStudent()) {
                        dependentStudents++;
                    }
                    dependents++;
                }

                if (person.getStatus().isActive()) {
                    civilianSalaries = civilianSalaries.plus(person.getSalary(getCampaign()));
                }
            }
        }

        //Add Salaries of Temp Workers
        salary = salary.plus(getCampaign().getCampaignOptions()
                                   .getRoleBaseSalaries()[PersonnelRole.ASTECH.ordinal()].getAmount().doubleValue() *
                                   getCampaign().getAstechPool());
        salary = salary.plus(getCampaign().getCampaignOptions()
                                   .getRoleBaseSalaries()[PersonnelRole.MEDIC.ordinal()].getAmount().doubleValue() *
                                   getCampaign().getMedicPool());

        StringBuilder sb = new StringBuilder("Support Personnel\n\n");

        sb.append(String.format("%-30s        %4s\n", "Total Support Personnel", countTotal));

        for (PersonnelRole role : personnelRoles) {
            if (role.isSupport(true)) {
                sb.append(String.format("    %-30s    %4s\n",
                      role.getLabel(getCampaign().getFaction().isClan()),
                      countPersonByType[role.ordinal()]));
            }
        }

        //Add Temp Medics and Astechs to Support List
        sb.append(String.format("    %-30s    %4s\n", "Temp Medics", getCampaign().getMedicPool()));
        sb.append(String.format("    %-30s    %4s\n", "Temp Astechs", getCampaign().getAstechPool()));

        sb.append('\n')
              .append(String.format("%-30s        %4s\n", "Injured Support Personnel", countInjured))
              .append(String.format("%-30s        %4s\n", "MIA Support Personnel", countMIA))
              .append(String.format("%-30s        %4s\n", "KIA Support Personnel", countKIA))
              .append(String.format("%-30s        %4s\n", "Retired Support Personnel", countRetired))
              .append(String.format("%-30s        %4s\n", "Dead Support Personnel", countDead))
              .append(String.format("%-30s        %4s\n", "Student Support Personnel", countStudents))
              .append("\nMonthly Salary For Support Personnel: ")
              .append(salary.toAmountAndSymbolString())
              .append(String.format("\nYou have " + dependents + " adult %s ",
                    (dependents == 1) ? "civilian" : "civilians") + "of which " + dependentStudents + " are students")
              .append(String.format("\nYou have " + children + " %s ", (children == 1) ? "child" : "children") +
                            "of which " +
                            childrenStudents +
                            " are students")
              .append("\nMonthly Salary For Civilians: ")
              .append(civilianSalaries.toAmountAndSymbolString())
              .append(String.format("\nYou have " + prisoners + " prisoner%s", prisoners == 1 ? "" : "s"))
              .append(String.format("\nYou have " + bondsmen + " %s", (bondsmen == 1) ? "bondsman" : "bondsmen"));

        return sb.toString();
    }
}
