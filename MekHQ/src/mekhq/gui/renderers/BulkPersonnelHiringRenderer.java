/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.renderers;

import megamek.client.generator.RandomNameGenerator;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.panels.PersonnelHiringDetailPanel;

import javax.swing.*;
import java.awt.*;

public class BulkPersonnelHiringRenderer extends JPanel implements ListCellRenderer<Person> {
    //region Variable Declarations
    private PersonnelHiringDetailPanel personnelHiringDetailPanel;
    private JButton btnRandomName;
    private JButton btnRandomPortrait;
    private JButton btnRandomOrigin;
    private JButton btnChoosePortrait;
    private JButton btnEditPerson;
    //endregion Variable Declarations

    //region Constructors
    public BulkPersonnelHiringRenderer(final Campaign campaign, final boolean gm) {
        initialize(campaign, gm);
    }
    //endregion Constructors

    //region Getters/Setters
    public PersonnelHiringDetailPanel getPersonnelHiringDetailPanel() {
        return personnelHiringDetailPanel;
    }

    public void setPersonnelHiringDetailPanel(final PersonnelHiringDetailPanel personnelHiringDetailPanel) {
        this.personnelHiringDetailPanel = personnelHiringDetailPanel;
    }

    public JButton getBtnRandomName() {
        return btnRandomName;
    }

    public void setBtnRandomName(final JButton btnRandomName) {
        this.btnRandomName = btnRandomName;
    }

    public JButton getBtnRandomPortrait() {
        return btnRandomPortrait;
    }

    public void setBtnRandomPortrait(final JButton btnRandomPortrait) {
        this.btnRandomPortrait = btnRandomPortrait;
    }

    public JButton getBtnRandomOrigin() {
        return btnRandomOrigin;
    }

    public void setBtnRandomOrigin(final JButton btnRandomOrigin) {
        this.btnRandomOrigin = btnRandomOrigin;
    }

    public JButton getBtnChoosePortrait() {
        return btnChoosePortrait;
    }

    public void setBtnChoosePortrait(final JButton btnChoosePortrait) {
        this.btnChoosePortrait = btnChoosePortrait;
    }

    public JButton getBtnEditPerson() {
        return btnEditPerson;
    }

    public void setBtnEditPerson(final JButton btnEditPerson) {
        this.btnEditPerson = btnEditPerson;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize(final Campaign campaign, final boolean gm) {
        JButton button = new JButton();
        button.addActionListener(evt -> {
            String factionCode = campaign.getCampaignOptions().useOriginFactionForNames()
                    ? getPerson().getOriginFaction().getShortName()
                    : RandomNameGenerator.getInstance().getChosenFaction();
            String[] name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                    getPerson().getGender(), getPerson().isClanner(), factionCode);
            getPerson().setGivenName(name[0]);
            getPerson().setSurname(name[1]);
        });

    }
    //endregion Initialization

    @Override
    public Component getListCellRendererComponent(final JList<? extends Person> list,
                                                  final Person value, final int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
        // JTextArea::setForeground and JTextArea::setBackground don't work properly with the default
        // return, but by recreating the colour it works properly
        final Color foreground = new Color(UIManager.getColor(isSelected
                ? "Table.selectionForeground" : "Table.foreground").getRGB());
        final Color background = new Color(UIManager.getColor(isSelected
                ? "Table.selectionBackground" : "Table.background").getRGB());
        setForeground(foreground);
        setBackground(background);
        getDescription().setForeground(foreground);
        getDescription().setBackground(background);

        getLblTitle().setText(value.getTitle());
        getDescription().setText(value.getDescription());

        return this;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(300, 100);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 120);
    }
}
