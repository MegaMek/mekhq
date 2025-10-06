/*
 * Copyright (c) 2013 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
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
package mekhq.campaign.report;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.ResourceBundle;

import mekhq.MekHQ;
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

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.PersonnelReport",
          MekHQ.getMHQOptions().getLocale());

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

        StringBuilder sb = new StringBuilder(resources.getString("combat.personnel.header.text") + "\n\n");

        sb.append(String.format("%-30s        %4s\n", resources.getString("combat.personnel.text"), countTotal));

        for (PersonnelRole role : personnelRoles) {
            if (role.isCombat()) {
                sb.append(String.format("    %-30s    %4s\n",
                      role.getLabel(getCampaign().getFaction().isClan()),
                      countPersonByType[role.ordinal()]));
            }
        }

        sb.append(getSecondaryCombatPersonnelDetails());

        sb.append('\n')
              .append(String.format("%-30s        %4s\n", resources.getString("combat.injured.text"), countInjured))
              .append(String.format("%-30s        %4s\n", resources.getString("combat.MIA.text"), countMIA))
              .append(String.format("%-30s        %4s\n", resources.getString("combat.KIA.text"), countKIA))
              .append(String.format("%-30s        %4s\n", resources.getString("combat.retired.text"), countRetired))
              .append(String.format("%-30s        %4s\n", resources.getString("combat.dead.text"), countDead))
              .append(String.format("%-30s        %4s\n", resources.getString("combat.student.text"), countStudents))
              .append("\n").append(resources.getString("combat.salary.text")).append(": ")
              .append(salary.toAmountAndSymbolString());

        return getFormattedTextAt("mekhq.resources.PersonnelReport", "secondary.combat", sb.toString());
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
        int campFollowers = 0;
        int children = 0;
        int childrenStudents = 0;
        Money civilianSalaries = Money.zero();
        LocalDate today = getCampaign().getLocalDate();

        for (Person person : getCampaign().getPersonnel()) {
            if (person.getStatus().isCampFollower() && !person.getPrisonerStatus().isCurrentPrisoner()) {
                campFollowers++;
                continue;
            }

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

                if (person.getStatus().isSalaryEligible()) {
                    civilianSalaries = civilianSalaries.plus(person.getSalary(getCampaign()));
                }
            }
        }

        //Add Salaries of Temp Workers
        salary = salary.plus(getCampaign().getCampaignOptions()
                                   .getRoleBaseSalaries()[PersonnelRole.ASTECH.ordinal()].getAmount().doubleValue() *
                                   getCampaign().getAsTechPool());
        salary = salary.plus(getCampaign().getCampaignOptions()
                                   .getRoleBaseSalaries()[PersonnelRole.MEDIC.ordinal()].getAmount().doubleValue() *
                                   getCampaign().getMedicPool());

        StringBuilder sb = new StringBuilder(resources.getString("support.personnel.header.text") + "\n\n");

        sb.append(String.format("%-30s           %4s\n", resources.getString("support.personnel.text"), countTotal));

        for (PersonnelRole role : personnelRoles) {
            if (role.isSupport(true)) {
                sb.append(String.format("    %-30s       %4s\n",
                      role.getLabel(getCampaign().getFaction().isClan()),
                      countPersonByType[role.ordinal()]));
            }
        }

        //Add Temp Medics and Astechs to Support List
        sb.append(String.format("    %-30s       %4s\n", "Temp Medics", getCampaign().getMedicPool()));
        sb.append(String.format("    %-30s       %4s\n", "Temp Astechs", getCampaign().getAsTechPool()));

        sb.append(getSecondarySupportPersonnelDetails());

        sb.append('\n')
              .append(String.format("%-30s           %4s\n", resources.getString("support.injured.text"), countInjured))
              .append(String.format("%-30s           %4s\n", resources.getString("support.MIA.text"), countMIA))
              .append(String.format("%-30s           %4s\n", resources.getString("support.KIA.text"), countKIA))
              .append(String.format("%-30s           %4s\n", resources.getString("support.retired.text"), countRetired))
              .append(String.format("%-30s           %4s\n", resources.getString("support.dead.text"), countDead))
              .append(String.format("%-30s           %4s\n",
                    resources.getString("support.student.text"),
                    countStudents))
              .append(String.format("%-30s           %4s\n",
                    resources.getString("support.campFollowers.text"),
                    campFollowers))
              .append("\n").append(resources.getString("support.salary.text")).append(": ")
              .append(salary.toAmountAndSymbolString())
              .append((dependents == 1) ?
                            "\n" + getFormattedTextAt("mekhq.resources.PersonnelReport", "support.dependant.text"
                                  , dependents, dependentStudents) :
                            "\n" + getFormattedTextAt("mekhq.resources"
                                                            + ".PersonnelReport",
                                  "support.dependants.text",
                                  dependents,
                                  dependentStudents))
              .append((children == 1) ?
                            "\n" + getFormattedTextAt("mekhq.resources.PersonnelReport", "support.child.text"
                                  , children, childrenStudents) :
                            "\n" + getFormattedTextAt("mekhq.resources"
                                                            + ".PersonnelReport",
                                  "support.children.text",
                                  children,
                                  childrenStudents))
              .append("\n").append(resources.getString("dependant.salary.text")).append(": ")
              .append(civilianSalaries.toAmountAndSymbolString())
              .append("\n").append((prisoners == 1) ? getFormattedTextAt("mekhq.resources"
                                                                               + ".PersonnelReport",
                    "prisoner.text",
                    prisoners) : getFormattedTextAt("mekhq.resources"
                                                          + ".PersonnelReport", "prisoners.text", prisoners)).append(": ")
              .append("\n").append((bondsmen == 1) ? getFormattedTextAt("mekhq.resources"
                                                                              + ".PersonnelReport",
                    "bondsman.text",
                    bondsmen) : getFormattedTextAt("mekhq.resources"
                                                         + ".PersonnelReport", "bondsmen.text", bondsmen)).append(": ");

        return getFormattedTextAt("mekhq.resources.PersonnelReport", "secondary.support", sb.toString());
    }

    public String getSecondarySupportPersonnelDetails() {
        EnumMap<PersonnelRole, Integer> countPersonByType = new EnumMap<>(PersonnelRole.class);
        int countSecondary = 0;
        for (Person person : getCampaign().getPersonnel()) {
            // Add them to the total count
            final boolean secondarySupport = person.getSecondaryRole().isSupport(true);

            if (secondarySupport && person.getPrisonerStatus().isFree() && person.getStatus().isActive()) {
                countPersonByType.put(person.getSecondaryRole(),
                      (countPersonByType.getOrDefault(person.getSecondaryRole(), 0) + 1));
                countSecondary++;
            }
        }

        StringBuilder sb = new StringBuilder("\n" + resources.getString("secondary.support.header.text") + "\n\n");

        sb.append(String.format("%-30s   %4s\n", resources.getString("secondary.support.text"), countSecondary));

        countPersonByType.forEach((role, value) ->
        {
            if (role.isSupport(true) && value >= 0) {
                sb.append(String.format("    %-30s       %4s\n",
                      role.getLabel(getCampaign().getFaction().isClan()),
                      value));
            }
        });

        return getFormattedTextAt("mekhq.resources.PersonnelReport", "secondary.support", sb.toString());
    }

    public String getSecondaryCombatPersonnelDetails() {
        EnumMap<PersonnelRole, Integer> countPersonByType = new EnumMap<>(PersonnelRole.class);

        int countSecondary = 0;
        for (Person person : getCampaign().getPersonnel()) {
            // Add them to the total count
            final boolean secondaryCombat = person.getSecondaryRole().isCombat();

            if (secondaryCombat && person.getPrisonerStatus().isFree() && person.getStatus().isActive()) {
                countPersonByType.put(person.getSecondaryRole(),
                      (countPersonByType.getOrDefault(person.getSecondaryRole(), 0) + 1));
                countSecondary++;
            }
        }

        StringBuilder sb = new StringBuilder("\n" + resources.getString("secondary.combat.header.text") + "\n\n");

        sb.append(String.format("%-30s %4s\n", resources.getString("secondary.combat.text"), countSecondary));

        countPersonByType.forEach((role, value) ->
        {
            if (role.isCombat() && value >= 0) {
                sb.append(String.format("    %-30s    %4s\n",
                      role.getLabel(getCampaign().getFaction().isClan()),
                      value));
            }
        });

        return getFormattedTextAt("mekhq.resources.PersonnelReport", "secondary.combat", sb.toString());
    }
}
