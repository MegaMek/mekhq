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
package mekhq.gui.panels;

import megamek.common.annotations.Nullable;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import megamek.common.util.EncodeControl;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.ResourceBundle;

public class PersonnelHiringDetailPanel extends JPanel {
    //region Variable Declarations
    private final CampaignGUI gui;
    private final Campaign campaign;
    private Person person;

    private JLabel lblPortrait;
    private JLabel lblPrimaryRole;
    private JLabel lblGender;
    private JLabel lblBirthday;
    private JLabel lblOrigin;
    private JLabel lblCallsign;
    private JPanel skillsPanel;
    private JPanel abilitiesPanel;
    private JPanel implantsPanel;
    private JLabel lblEdge;
    private JLabel lblToughness;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public PersonnelHiringDetailPanel(final @Nullable CampaignGUI gui, final Campaign campaign,
                                      final @Nullable Person person, final boolean showOrigin) {
        this.gui = gui;
        this.campaign = campaign;
        setPerson(person);
        initialize(showOrigin);
        updateValues();
    }
    //endregion Constructors

    //region Getters/Setters
    public @Nullable CampaignGUI getGUI() {
        return gui;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public @Nullable Person getPerson() {
        return person;
    }

    public void setPerson(final @Nullable Person person) {
        this.person = person;
    }

    public JLabel getLblPortrait() {
        return lblPortrait;
    }

    public void setLblPortrait(final JLabel lblPortrait) {
        this.lblPortrait = lblPortrait;
    }

    public JLabel getLblPrimaryRole() {
        return lblPrimaryRole;
    }

    public void setLblPrimaryRole(final JLabel lblPrimaryRole) {
        this.lblPrimaryRole = lblPrimaryRole;
    }

    public JLabel getLblGender() {
        return lblGender;
    }

    public void setLblGender(final JLabel lblGender) {
        this.lblGender = lblGender;
    }

    public JLabel getLblBirthday() {
        return lblBirthday;
    }

    public void setLblBirthday(final JLabel lblBirthday) {
        this.lblBirthday = lblBirthday;
    }

    public JLabel getLblOrigin() {
        return lblOrigin;
    }

    public void setLblOrigin(final JLabel lblOrigin) {
        this.lblOrigin = lblOrigin;
    }

    public JLabel getLblCallsign() {
        return lblCallsign;
    }

    public void setLblCallsign(final JLabel lblCallsign) {
        this.lblCallsign = lblCallsign;
    }

    public JPanel getSkillsPanel() {
        return skillsPanel;
    }

    public void setSkillsPanel(final JPanel skillsPanel) {
        this.skillsPanel = skillsPanel;
    }

    public JPanel getAbilitiesPanel() {
        return abilitiesPanel;
    }

    public void setAbilitiesPanel(final JPanel abilitiesPanel) {
        this.abilitiesPanel = abilitiesPanel;
    }

    public JPanel getImplantsPanel() {
        return implantsPanel;
    }

    public void setImplantsPanel(final JPanel implantsPanel) {
        this.implantsPanel = implantsPanel;
    }

    public JLabel getLblEdge() {
        return lblEdge;
    }

    public void setLblEdge(final JLabel lblEdge) {
        this.lblEdge = lblEdge;
    }

    public JLabel getLblToughness() {
        return lblToughness;
    }

    public void setLblToughness(final JLabel lblToughness) {
        this.lblToughness = lblToughness;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize(final boolean showOrigin) {
        // Create Panel Components
        setLblPortrait(new JLabel(((getPerson() == null) ? new Portrait() : getPerson().getPortrait())
                .getImageIcon(150), SwingConstants.CENTER));
        getLblPortrait().setName("lblPortrait");

        setLblPrimaryRole(new JLabel());
        getLblPrimaryRole().setName("lblPrimaryRole");

        setLblGender(new JLabel());
        getLblGender().setName("lblGender");

        setLblBirthday(new JLabel());
        getLblBirthday().setName("lblBirthday");

        setLblOrigin(new JLabel());
        getLblOrigin().setName("lblOrigin");
        getLblOrigin().setVisible(showOrigin);

        setLblCallsign(new JLabel());
        getLblCallsign().setName("lblCallsign");

        setSkillsPanel(new JPanel());
        getSkillsPanel().setBorder(BorderFactory.createTitledBorder(resources.getString("skillsPanel.title")));
        getSkillsPanel().setName("skillsPanel");

        setAbilitiesPanel(new JPanel());
        getAbilitiesPanel().setBorder(BorderFactory.createTitledBorder(resources.getString("abilitiesPanel.title")));
        getAbilitiesPanel().setName("abilitiesPanel");
        getAbilitiesPanel().setVisible(getCampaign().getCampaignOptions().useAbilities());

        setImplantsPanel(new JPanel());
        getImplantsPanel().setBorder(BorderFactory.createTitledBorder(resources.getString("implantsPanel.title")));
        getImplantsPanel().setName("implantsPanel");
        getImplantsPanel().setVisible(getCampaign().getCampaignOptions().useImplants());

        setLblEdge(new JLabel());
        getLblEdge().setName("lblEdge");
        getLblEdge().setEnabled(getCampaign().getCampaignOptions().useEdge());

        setLblToughness(new JLabel());
        getLblToughness().setName("lblToughness");
        getLblToughness().setEnabled(getCampaign().getCampaignOptions().useToughness());

        // Layout the UI
        setBorder(BorderFactory.createTitledBorder(""));
        setName("personnelHiringDetailPanel");
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getLblPortrait())
                        .addComponent(getLblPrimaryRole())
                        .addComponent(getLblGender())
                        .addComponent(getLblBirthday())
                        .addComponent(getLblOrigin())
                        .addComponent(getLblCallsign())
                        .addComponent(getSkillsPanel())
                        .addComponent(getAbilitiesPanel())
                        .addComponent(getImplantsPanel())
                        .addComponent(getLblEdge())
                        .addComponent(getLblToughness())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(getLblPortrait())
                        .addComponent(getLblPrimaryRole())
                        .addComponent(getLblGender())
                        .addComponent(getLblBirthday())
                        .addComponent(getLblOrigin())
                        .addComponent(getLblCallsign())
                        .addComponent(getSkillsPanel())
                        .addComponent(getAbilitiesPanel())
                        .addComponent(getImplantsPanel())
                        .addComponent(getLblEdge())
                        .addComponent(getLblToughness())
        );
    }
    //endregion Initialization

    //region Update Values
    public void updateValues() {
        if (getPerson() == null) {
            return;
        }

        setBorder(BorderFactory.createTitledBorder(getPerson().getFullTitle()));

        if (!getPerson().getPortrait().toString().equals(getLblPortrait().getToolTipText())) {
            getLblPortrait().setIcon(getPerson().getPortrait().getImageIcon(150));
            getLblPortrait().setToolTipText(getPerson().getPortrait().toString());
        }

        getLblPrimaryRole().setText(getPerson().getPrimaryRole().getName(getPerson().isClanner()));

        getLblGender().setText(String.format(resources.getString("PersonnelHiringDetailPanel.lblGender.text"),
                GenderDescriptors.MALE_FEMALE.getDescriptorCapitalized(getPerson().getGender())));

        getLblBirthday().setText(String.format(resources.getString("PersonnelHiringDetailPanel.lblBirthday.text"),
                getPerson().getBirthday(), getPerson().getAge(getCampaign().getLocalDate())));

        if (getLblOrigin().isVisible()) {
            getLblOrigin().setText((getPerson().getOriginPlanet() == null)
                    ? String.format(resources.getString("PersonnelHiringDetailPanel.lblOrigin.text"),
                    getPerson().getOriginFaction().getFullName(getCampaign().getGameYear()))
                    : String.format(resources.getString("PersonnelHiringDetailPanel.lblOrigin.Planet.text"),
                    getPerson().getOriginPlanet().getName(getCampaign().getLocalDate()),
                    getPerson().getOriginFaction().getFullName(getCampaign().getGameYear())));
            if ((getGUI() != null) && (getPerson() != null) && (getPerson().getOriginPlanet() != null)) {
                getLblOrigin().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                getLblOrigin().addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        final PlanetarySystem system = getPerson().getOriginPlanet().getParentSystem();
                        // Stay on the interstellar map if their origin planet is the primary planet...
                        if (system.getPrimaryPlanet().equals(getPerson().getOriginPlanet())) {
                            getGUI().getMapTab().switchSystemsMap(system);
                        } else {
                            // ... otherwise, dive on in to the system view!
                            getGUI().getMapTab().switchPlanetaryMap(getPerson().getOriginPlanet());
                        }
                        getGUI().setSelectedTab(GuiTabType.MAP);
                    }
                });
            }
        }

        if (!"-".equals(getPerson().getCallsign()) && !getPerson().getCallsign().isBlank()) {
            getLblCallsign().setText(String.format(resources.getString("PersonnelHiringDetailPanel.lblCallsign.text"), getPerson().getCallsign()));
            getLblCallsign().setVisible(true);
        } else {
            getLblCallsign().setVisible(false);
        }

        recreateSkillsPanel();

        if (getCampaign().getCampaignOptions().useAbilities()) {
            recreateAbilitiesPanel();
        }

        if (getCampaign().getCampaignOptions().useImplants()) {
            recreateImplantsPanel();
        }

        if (getLblEdge().isEnabled() && (getPerson().getEdge() > 0)) {
            getLblEdge().setText(String.format(resources.getString("PersonnelHiringDetailPanel.lblEdge.text"), getPerson().getEdge()));
            getLblEdge().setVisible(true);
        } else {
            getLblEdge().setVisible(false);
        }

        if (getLblToughness().isEnabled() && (getPerson().getToughness() > 0)) {
            getLblToughness().setText(String.format(resources.getString("PersonnelHiringDetailPanel.lblToughness.text"), getPerson().getToughness()));
            getLblToughness().setVisible(true);
        } else {
            getLblToughness().setVisible(false);
        }
    }

    private void recreateSkillsPanel() {
        getSkillsPanel().removeAll();

        getSkillsPanel().setLayout(new GridLayout(0, 2));

        for (final Skill skill : getPerson().getSkills().getSkills()) {
            final String labelName = "lbl" + skill.getType().getName();
            JLabel lblSkillName = new JLabel(skill.getType().getName() + ":");
            lblSkillName.setName(labelName + "Name");
            getSkillsPanel().add(lblSkillName);

            JLabel lblSkillValue = new JLabel(skill.toString());
            lblSkillValue.setName(labelName + "Value");
            getSkillsPanel().add(lblSkillValue);

            lblSkillName.setLabelFor(lblSkillValue);
        }
    }

    private void recreateAbilitiesPanel() {
        getAbilitiesPanel().removeAll();

        if (getPerson().countOptions(PilotOptions.LVL3_ADVANTAGES) == 0) {
            getAbilitiesPanel().setVisible(false);
            return;
        }

        getAbilitiesPanel().setLayout(new GridLayout(0, 1));
        getAbilitiesPanel().setVisible(true);

        for (Enumeration<IOption> i = getPerson().getOptions(PilotOptions.LVL3_ADVANTAGES); i.hasMoreElements();) {
            final IOption option = i.nextElement();

            if (option.booleanValue()) {
                JLabel lblAbilities = new JLabel(Utilities.getOptionDisplayName(option));
                lblAbilities.setToolTipText(option.getDescription());
                lblAbilities.setName("lblAbilities" + option.getName());
                getAbilitiesPanel().add(lblAbilities);
            }
        }
    }

    private void recreateImplantsPanel() {
        getImplantsPanel().removeAll();

        if (getPerson().countOptions(PilotOptions.MD_ADVANTAGES) == 0) {
            getImplantsPanel().setVisible(false);
            return;
        }

        getImplantsPanel().setLayout(new GridLayout(0, 1));
        getImplantsPanel().setVisible(true);

        for (Enumeration<IOption> i = getPerson().getOptions(PilotOptions.MD_ADVANTAGES); i.hasMoreElements();) {
            IOption option = i.nextElement();

            if (option.booleanValue()) {
                JLabel lblImplant = new JLabel(Utilities.getOptionDisplayName(option));
                lblImplant.setToolTipText(option.getDescription());
                lblImplant.setName("lblImplant" + option.getName());
                getImplantsPanel().add(lblImplant);
            }
        }
    }
    //endregion Update Values
}
