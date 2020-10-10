/*
 * PersonnelReport.java
 *
 * Copyright (c) 2013 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.report;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JSplitPane;
import javax.swing.JTextPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;

/**
 * @author Jay Lawson
 */
public class PersonnelReport extends Report {


    public PersonnelReport(Campaign c) {
        super(c);
    }

    public String getTitle() {
        return "Personnel Report";
    }

    public JTextPane getCombatPersonnelReport() {
    	// Load combat personnel
        JTextPane txtCombat = new JTextPane();
        txtCombat.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtCombat.setText(getCombatPersonnelDetails());
        return txtCombat;
    }

    public JTextPane getSupportPersonnelReport() {
    	// Load support personnel
        JTextPane txtSupport = new JTextPane();
        txtSupport.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtSupport.setText(getSupportPersonnelDetails());
        return txtSupport;
    }

    public JTextPane getReport() {
        // SplitPane them
        JSplitPane splitOverviewPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getCombatPersonnelReport(), getSupportPersonnelReport());
		splitOverviewPersonnel.setName("splitOverviewPersonnel");
		splitOverviewPersonnel.setOneTouchExpandable(true);
		splitOverviewPersonnel.setResizeWeight(0.5);

		// Actual report pane
		JTextPane txtReport = new JTextPane();
        txtReport.setMinimumSize(new Dimension(800, 500));
        txtReport.insertComponent(splitOverviewPersonnel);
        return txtReport;
    }

    public String getCombatPersonnelDetails() {
        int[] countPersonByType = new int[Person.T_NUM];
        int countTotal = 0;
        int countInjured = 0;
        int countMIA = 0;
        int countKIA = 0;
        int countDead = 0;
        int countRetired = 0;
        Money salary = Money.zero();

        for (Person p : getCampaign().getPersonnel()) {
            if (!p.hasPrimaryCombatRole() || !p.getPrisonerStatus().isFree()) {
                continue;
            }

            // Add them to the total count
            if (p.getStatus().isActive()) {
                countPersonByType[p.getPrimaryRole()]++;
                countTotal++;
                if (getCampaign().getCampaignOptions().useAdvancedMedical() && (p.getInjuries().size() > 0)) {
                    countInjured++;
                } else if (p.getHits() > 0) {
                    countInjured++;
                }
                salary = salary.plus(p.getSalary());
            } else if (p.getStatus().isRetired()) {
                countRetired++;
            } else if (p.getStatus().isMIA()) {
                countMIA++;
            } else if (p.getStatus().isKIA()) {
                countKIA++;
                countDead++;
            } else if (p.getStatus().isDead()) {
                countDead++;
            }
        }

        StringBuilder sb = new StringBuilder("Combat Personnel\n\n");

        sb.append(String.format("%-30s        %4s\n", "Total Combat Personnel", countTotal));

        for (int i = 0; i < Person.T_NUM; i++) {
            if (Person.isCombatRole(i)) {
                sb.append(String.format("    %-30s    %4s\n",
                        Person.getRoleDesc(i, getCampaign().getFaction().isClan()), countPersonByType[i]));
            }
        }

        sb.append("\n")
                .append(String.format("%-30s        %4s\n", "Injured Combat Personnel", countInjured))
                .append(String.format("%-30s        %4s\n", "MIA Combat Personnel", countMIA))
                .append(String.format("%-30s        %4s\n", "KIA Combat Personnel", countKIA))
                .append(String.format("%-30s        %4s\n", "Retired Combat Personnel", countRetired))
                .append(String.format("%-30s        %4s\n", "Dead Combat Personnel", countDead))
                .append("\nMonthly Salary For Combat Personnel: ").append(salary.toAmountAndSymbolString());

        return sb.toString();
    }

    public String getSupportPersonnelDetails() {
        int[] countPersonByType = new int[Person.T_NUM];
        int countTotal = 0;
        int countInjured = 0;
        int countMIA = 0;
        int countKIA = 0;
        int countDead = 0;
        int countRetired = 0;
        Money salary = Money.zero();
        int prisoners = 0;
        int bondsmen = 0;
        int dependents = 0;

        for (Person p : getCampaign().getPersonnel()) {
            // Add them to the total count
            boolean primarySupport = p.hasPrimarySupportRole(false);

            if (primarySupport && p.getPrisonerStatus().isFree() && p.getStatus().isActive()) {
                countPersonByType[p.getPrimaryRole()]++;
                countTotal++;
                if ((p.getInjuries().size() > 0) || (p.getHits() > 0)) {
                    countInjured++;
                }
                salary = salary.plus(p.getSalary());
            } else if (p.getPrisonerStatus().isPrisoner() && p.getStatus().isActive()) {
                prisoners++;
                if (p.getInjuries().size() > 0 || p.getHits() > 0) {
                    countInjured++;
                }
            } else if (p.getPrisonerStatus().isBondsman() && p.getStatus().isActive()) {
                bondsmen++;
                if (p.getInjuries().size() > 0 || p.getHits() > 0) {
                    countInjured++;
                }
            } else if (primarySupport && p.getStatus().isRetired()) {
                countRetired++;
            } else if (primarySupport && p.getStatus().isMIA()) {
                countMIA++;
            } else if (primarySupport && p.getStatus().isKIA()) {
                countKIA++;
                countDead++;
            } else if (primarySupport && p.getStatus().isDead()) {
                countDead++;
            }

            if (p.isDependent() && p.getStatus().isActive() && p.getPrisonerStatus().isFree()) {
                dependents++;
            }
        }

        StringBuilder sb = new StringBuilder("Support Personnel\n\n");

        sb.append(String.format("%-30s        %4s\n", "Total Support Personnel", countTotal));

        for (int i = 0; i < Person.T_NUM; i++) {
            if (Person.isSupportRole(i)) {
                sb.append(String.format("    %-30s    %4s\n",
                        Person.getRoleDesc(i, getCampaign().getFaction().isClan()), countPersonByType[i]));
            }
        }

        sb.append("\n")
                .append(String.format("%-30s        %4s\n", "Injured Support Personnel", countInjured))
                .append(String.format("%-30s        %4s\n", "MIA Support Personnel", countMIA))
                .append(String.format("%-30s        %4s\n", "KIA Support Personnel", countKIA))
                .append(String.format("%-30s        %4s\n", "Retired Support Personnel", countRetired))
                .append(String.format("%-30s        %4s\n", "Dead Support Personnel", countDead))
                .append("\nMonthly Salary For Support Personnel: ").append(salary.toAmountAndSymbolString())
                .append(String.format("\nYou have " + dependents + " %s", (dependents == 1) ? "dependent" : "dependents"))
                .append(String.format("\nYou have " + prisoners + " prisoner%s", (prisoners == 1) ? "" : "s"))
                .append(String.format("\nYou have " + bondsmen + " %s", (bondsmen == 1) ? "bondsman" : "bondsmen"));

        return sb.toString();
    }
}
