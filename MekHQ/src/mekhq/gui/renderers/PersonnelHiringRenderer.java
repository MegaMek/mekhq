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

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.dialogs.PortraitChooserDialog;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.CustomizePersonDialog;
import mekhq.gui.panels.PersonnelHiringDetailPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class PersonnelHiringRenderer extends JPanel implements ListCellRenderer<Person> {
    //region Variable Declarations
    private final JFrame frame;
    private PersonnelHiringDetailPanel personnelHiringDetailPanel;
    private JButton btnRandomName;
    private JButton btnRandomPortrait;
    private JButton btnRandomOrigin;
    private JButton btnRandomCallsign;
    private JButton btnChoosePortrait;
    private JButton btnEditPerson;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public PersonnelHiringRenderer(final JFrame frame, final @Nullable CampaignGUI gui,
                                   final Campaign campaign, final boolean gm) {
        this.frame = frame;
        initialize(gui, campaign, gm);
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

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

    public JButton getBtnRandomCallsign() {
        return btnRandomCallsign;
    }

    public void setBtnRandomCallsign(final JButton btnRandomCallsign) {
        this.btnRandomCallsign = btnRandomCallsign;
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
    private void initialize(final @Nullable CampaignGUI gui, final Campaign campaign, final boolean gm) {
        setPersonnelHiringDetailPanel(new PersonnelHiringDetailPanel(gui, campaign, null, true));

        setBtnRandomName(new JButton(resources.getString("btnRandomName.text")));
        getBtnRandomName().setToolTipText(resources.getString("btnRandomName.toolTipText"));
        getBtnRandomName().setName("btnRandomName");
        getBtnRandomName().addActionListener(evt -> {
            final Person person = getPersonnelHiringDetailPanel().getPerson();
            if (person == null) {
                return;
            }
            String factionCode = campaign.getCampaignOptions().useOriginFactionForNames()
                    ? person.getOriginFaction().getShortName()
                    : RandomNameGenerator.getInstance().getChosenFaction();
            String[] name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                    person.getGender(), person.isClanner(), factionCode);
            person.setGivenName(name[0]);
            person.setSurname(name[1]);
            getPersonnelHiringDetailPanel().updateValues();
        });

        setBtnRandomPortrait(new JButton(resources.getString("btnRandomPortrait.text")));
        getBtnRandomPortrait().setToolTipText(resources.getString("btnRandomPortrait.toolTipText"));
        getBtnRandomPortrait().setName("btnRandomPortrait");
        getBtnRandomPortrait().addActionListener(evt -> {
            final Person person = getPersonnelHiringDetailPanel().getPerson();
            if (person != null) {
                campaign.assignRandomPortraitFor(person);
                getPersonnelHiringDetailPanel().updateValues();
            }
        });

        setBtnRandomOrigin(new JButton(resources.getString("btnRandomOrigin.text")));
        getBtnRandomOrigin().setToolTipText(resources.getString("btnRandomOrigin.toolTipText"));
        getBtnRandomOrigin().setName("btnRandomOrigin");
        getBtnRandomOrigin().addActionListener(evt -> {
            final Person person = getPersonnelHiringDetailPanel().getPerson();
            if (person != null) {
                campaign.assignRandomOriginFor(person);
                getPersonnelHiringDetailPanel().updateValues();
            }
        });

        setBtnRandomCallsign(new JButton(resources.getString("btnRandomCallsign.text")));
        getBtnRandomCallsign().setToolTipText(resources.getString("btnRandomCallsign.toolTipText"));
        getBtnRandomCallsign().setName("btnRandomCallsign");
        getBtnRandomCallsign().addActionListener(evt -> {
            final Person person = getPersonnelHiringDetailPanel().getPerson();
            if (person != null) {
                person.setCallsign(RandomCallsignGenerator.getInstance().generate());
                getPersonnelHiringDetailPanel().updateValues();
            }
        });

        setBtnChoosePortrait(new JButton(resources.getString("btnChoosePortrait.text")));
        getBtnChoosePortrait().setToolTipText(resources.getString("btnChoosePortrait.toolTipText"));
        getBtnChoosePortrait().setName("btnChoosePortrait");
        getBtnChoosePortrait().addActionListener(evt -> {
            final Person person = getPersonnelHiringDetailPanel().getPerson();
            if (person == null) {
                return;
            }
            final PortraitChooserDialog portraitDialog = new PortraitChooserDialog(getFrame(), person.getPortrait());
            if (portraitDialog.showDialog().isConfirmed() && (portraitDialog.getSelectedItem() != null)) {
                person.setPortrait(portraitDialog.getSelectedItem());
                getPersonnelHiringDetailPanel().updateValues();
            }
        });

        if (gm) {
            setBtnEditPerson(new JButton(resources.getString("btnEditPerson.text")));
            getBtnEditPerson().setToolTipText(resources.getString("btnEditPerson.toolTipText"));
            getBtnEditPerson().setName("btnEditPerson");
            getBtnEditPerson().addActionListener(evt -> {
                final Person person = getPersonnelHiringDetailPanel().getPerson();
                if (person == null) {
                    return;
                }
                final Gender gender = person.getGender();
                new CustomizePersonDialog(getFrame(), true, person, campaign).setVisible(true);
                if (gender != person.getGender()) {
                    campaign.assignRandomPortraitFor(person);
                }
                getPersonnelHiringDetailPanel().updateValues();
            });
        }
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
        /*
        getDescription().setForeground(foreground);
        getDescription().setBackground(background);

        getLblTitle().setText(value.getTitle());
        getDescription().setText(value.getDescription());
         */

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
