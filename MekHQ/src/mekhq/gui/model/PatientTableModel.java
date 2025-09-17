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
package mekhq.gui.model;

import static java.lang.Math.round;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import megamek.client.ui.util.UIUtil;
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
            final int maximumWidth = UIUtil.scaleForGUI(300);
            final int maximumHeight = UIUtil.scaleForGUI(100);

            Person person = getElementAt(index);
            setImage(person.getPortrait().getImage(UIUtil.scaleForGUI(50)));
            if (getCampaign().getCampaignOptions().isUseAdvancedMedical()) {
                setHtmlText(getInjuriesDesc(person, (int) round(maximumWidth * 0.75) - 50));
            } else {
                setHtmlText(getPatientDesc(person));
            }
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            setBackground(list.getBackground());
            setForeground(list.getForeground());

            setPreferredSize(new Dimension(maximumWidth, maximumHeight));
            return this;
        }
    }

    /**
     * Generates a styled HTML string describing the injuries of a person, formatted with a specified maximum width.
     *
     * <p>The generated string includes the person's full title in bold and a list of their injuries,
     * each formatted with descriptive text and the time required to heal. Injuries are separated by commas, and the
     * list is wrapped at the specified width to allow proper text wrapping.</p>
     *
     * @param person       The person whose injury description is to be generated.
     * @param maximumWidth The maximum width (in pixels) for the HTML content, ensuring text wrapping.
     *
     * @return A styled HTML string describing the person's injuries.
     */
    private String getInjuriesDesc(Person person, int maximumWidth) {
        StringBuilder toReturn = new StringBuilder("<html><div style='width:")
                                       .append(maximumWidth).append("px'><b>").append(person.getFullTitle())
                                       .append("</b>");

        List<Injury> injuries = person.getInjuries();

        for (Injury injury : injuries) {
            if (injuries.indexOf(injury) != 0) {
                toReturn.append(", ");
            } else {
                toReturn.append("<br>");
            }

            toReturn.append(injury.getFluff()).append(" (");

            if (injury.isPermanent()) {
                toReturn.append('\u221E');
            } else {
                toReturn.append(injury.getTime());
            }

            toReturn.append(')');
        }
        toReturn.append("</div></html>");
        return toReturn.toString();
    }

    private String getPatientDesc(Person p) {
        String toReturn = "<html><font><b>" + p.getFullTitle() + "</b><br/>";
        toReturn += p.getHits() + " hit(s)<br>[next check in " + p.getDaysToWaitForHealing() + " days]";
        toReturn += "</font></html>";
        return toReturn;
    }
}
