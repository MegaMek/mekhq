/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.gui.BasicInfo;

/**
 * A table model for displaying personnel in the infirmary
 */
public class PatientTableModel extends AbstractListModel<Person> {
    private ArrayList<Person> patients;
    private final Campaign campaign;

    public PatientTableModel(Campaign c) {
        patients = new ArrayList<>();
        campaign = c;
    }

    // fill table with values
    public void setData(ArrayList<Person> data) {
        patients = data;
        this.fireContentsChanged(this, 0, patients.size());
        // fireTableDataChanged();
    }

    @Override
    public Person getElementAt(int index) {
        if (index < 0 || index >= patients.size()) {
            return null;
        }
        return patients.get(index);
    }

    @Override
    public int getSize() {
        return patients.size();
    }

    private Campaign getCampaign() {
        return campaign;
    }

    public PatientTableModel.Renderer getRenderer() {
        return new PatientTableModel.Renderer();
    }

    public class Renderer extends BasicInfo implements ListCellRenderer<Object> {
        public Renderer() {
            super();
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            Person p = getElementAt(index);
            setImage(p.getPortrait().getImage(54));
            if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                setHtmlText(getInjuriesDesc(p));
            } else {
                setHtmlText(getPatientDesc(p));
            }
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            return this;
        }
    }

    private String getInjuriesDesc(Person p) {
        StringBuilder toReturn = new StringBuilder("<html><font><b>").append(p.getFullTitle())
                .append("</b><br/>").append("&nbsp;&nbsp;&nbsp;Injuries:");
        String sep = "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
        for (Injury injury : p.getInjuries()) {
            toReturn.append(sep).append(injury.getFluff());
            if (sep.contains("<br/>")) {
                sep = ", ";
            } else {
                sep = ",<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
            }
        }
        toReturn.append("</font></html>");
        return toReturn.toString();
    }

    private String getPatientDesc(Person p) {
        String toReturn = "<html><font><b>" + p.getFullTitle() + "</b><br/>";
        toReturn += p.getHits() + " hit(s)<br>[next check in " + p.getDaysToWaitForHealing() + " days]";
        toReturn += "</font></html>";
        return toReturn;
    }
}
