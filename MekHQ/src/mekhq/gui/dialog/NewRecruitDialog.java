/*
 * MegaMekLab - Copyright (C) 2019 - The MegaMekTeam
 *
 * Original author - Jay Lawson (jaylawson39 at yahoo.com)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.swing.dialog.imageChooser.AbstractIconChooserDialog;
import megamek.client.ui.swing.dialog.imageChooser.PortraitChooserDialog;
import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.gui.CampaignGUI;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.view.PersonViewPanel;
import mekhq.preferences.PreferencesNode;

public class NewRecruitDialog extends javax.swing.JDialog {
    /**
     * This dialog is used to both hire new pilots and to edit existing ones
     */
    private static final long serialVersionUID = -6265589976779860566L;
    private Person person;

    private CampaignGUI hqView;

    private JComboBox<String> choiceRanks;

    private JScrollPane scrollView;

    /** Creates new form CustomizePilotDialog */
    public NewRecruitDialog(CampaignGUI hqView, boolean modal, Person person) {
        super(hqView.getFrame(), modal);
        this.hqView = hqView;
        this.person = person;
        initComponents();
        setLocationRelativeTo(hqView.getFrame());
        setUserPreferences();
    }

    private void refreshView() {
        scrollView.setViewportView(new PersonViewPanel(person, hqView.getCampaign(), hqView));
        // This odd code is to make sure that the scrollbar stays at the top
        // I cant just call it here, because it ends up getting reset somewhere
        // later
        javax.swing.SwingUtilities.invokeLater(() -> scrollView.getVerticalScrollBar().setValue(0));
    }

    private void initComponents() {
        scrollView = new JScrollPane();
        choiceRanks = new javax.swing.JComboBox<>();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewRecruitDialog", new EncodeControl());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(resourceMap.getString("Form.title")); // NOI18N

        setName("Form"); // NOI18N
        getContentPane().setLayout(new java.awt.BorderLayout());

        JPanel panSidebar = createSidebar(resourceMap);

        JPanel panBottomButtons = createButtonPanel(resourceMap);

        scrollView.setMinimumSize(new java.awt.Dimension(450, 180));
        scrollView.setPreferredSize(new java.awt.Dimension(450, 180));
        scrollView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollView.setViewportView(null);
        refreshView();

        getContentPane().add(panSidebar, BorderLayout.LINE_START);
        getContentPane().add(scrollView, BorderLayout.CENTER);
        getContentPane().add(panBottomButtons, BorderLayout.PAGE_END);

        pack();
    }

    private JPanel createButtonPanel(ResourceBundle resourceMap) {
        JPanel panButtons = new JPanel();
        panButtons.setName("panButtons"); // NOI18N
        panButtons.setLayout(new GridBagLayout());

        JButton button = new JButton(resourceMap.getString("btnHire.text")); // NOI18N
        button.setName("btnOk"); // NOI18N
        button.addActionListener(e -> hire());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;

        panButtons.add(button, gridBagConstraints);
        gridBagConstraints.gridx++;

        if (hqView.getCampaign().isGM()) {
            button = new JButton(resourceMap.getString("btnAddGM.text")); // NOI18N
            button.setName("btnGM"); // NOI18N
            button.addActionListener(e -> addGM());

            panButtons.add(button, gridBagConstraints);
            gridBagConstraints.gridx++;
        }

        button = new JButton(resourceMap.getString("btnClose.text")); // NOI18N
        button.setName("btnClose"); // NOI18N
        button.addActionListener(e -> setVisible(false));
        panButtons.add(button, gridBagConstraints);

        return panButtons;
    }

    private JPanel createSidebar(ResourceBundle resourceMap) {
        boolean randomizeOrigin = hqView.getCampaign().getCampaignOptions().randomizeOrigin();

        JPanel panSidebar = new JPanel();
        panSidebar.setName("panButtons"); // NOI18N
        panSidebar.setLayout(new java.awt.GridLayout(6 + (randomizeOrigin ? 1 : 0), 1));

        choiceRanks.setName("choiceRanks"); // NOI18N
        refreshRanksCombo();
        choiceRanks.addActionListener(e -> changeRank());
        panSidebar.add(choiceRanks);

        JButton button = new JButton(resourceMap.getString("btnRandomName.text")); // NOI18N
        button.setName("btnRandomName"); // NOI18N
        button.addActionListener(e -> randomName());
        panSidebar.add(button);

        button = new JButton(resourceMap.getString("btnRandomPortrait.text")); // NOI18N
        button.setName("btnRandomPortrait"); // NOI18N
        button.addActionListener(e -> randomPortrait());
        panSidebar.add(button);

        if (randomizeOrigin) {
            button = new JButton(resourceMap.getString("btnRandomOrigin.text")); // NOI18N
            button.setName("btnRandomOrigin"); // NOI18N
            button.addActionListener(e -> randomOrigin());
            panSidebar.add(button);
        }

        button = new JButton(resourceMap.getString("btnChoosePortrait.text")); // NOI18N
        button.setName("btnChoosePortrait"); // NOI18N
        button.addActionListener(e -> choosePortrait());
        panSidebar.add(button);

        button = new JButton(resourceMap.getString("btnEditPerson.text")); // NOI18N
        button.setName("btnEditPerson"); // NOI18N
        button.addActionListener(e -> editPerson());
        button.setEnabled(hqView.getCampaign().isGM());
        panSidebar.add(button);

        button = new JButton(resourceMap.getString("btnRegenerate.text")); // NOI18N
        button.setName("btnRegenerate"); // NOI18N
        button.addActionListener(e -> regenerate());
        button.setEnabled(hqView.getCampaign().isGM());
        panSidebar.add(button);

        return panSidebar;
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(NewRecruitDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void hire() {
        if (hqView.getCampaign().recruitPerson(person, false)) {
            createNewRecruit();
        }
        refreshView();
    }

    private void addGM() {
        if (hqView.getCampaign().recruitPerson(person, true)) {
            createNewRecruit();
        }
        refreshView();
    }

    private void createNewRecruit() {
        person = hqView.getCampaign().newPerson(person.getPrimaryRole());
        refreshRanksCombo();
        hqView.getCampaign().changeRank(person, hqView.getCampaign().getRanks().getRankNumericFromNameAndProfession(
                person.getProfession(), (String) choiceRanks.getSelectedItem()), false);
    }

    private void randomName() {
        String factionCode = hqView.getCampaign().getCampaignOptions().useOriginFactionForNames()
                ? person.getOriginFaction().getShortName()
                : RandomNameGenerator.getInstance().getChosenFaction();

        String[] name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                person.getGender(), person.isClanner(), factionCode);
        person.setGivenName(name[0]);
        person.setSurname(name[1]);
        refreshView();
    }

    private void randomPortrait() {
        hqView.getCampaign().assignRandomPortraitFor(person);
        refreshView();
    }

    private void randomOrigin() {
        hqView.getCampaign().assignRandomOriginFor(person);
        refreshView();
    }

    private void choosePortrait() {
        AbstractIconChooserDialog portraitDialog = new PortraitChooserDialog(hqView.getFrame(), person.getPortrait());
        int result = portraitDialog.showDialog();
        if ((result == JOptionPane.OK_OPTION) && (portraitDialog.getSelectedItem() != null)) {
            person.setPortrait(portraitDialog.getSelectedItem());
            refreshView();
        }
    }

    private void editPerson() {
        Gender gender = person.getGender();
        CustomizePersonDialog npd = new CustomizePersonDialog(hqView.getFrame(), true, person, hqView.getCampaign());
        npd.setVisible(true);
        if (gender != person.getGender()) {
            randomPortrait();
        }
        refreshRanksCombo();
        refreshView();
    }

    private void regenerate() {
        person = hqView.getCampaign().newPerson(person.getPrimaryRole(), person.getSecondaryRole());
        refreshRanksCombo();
        refreshView();
    }

    private void changeRank() {
        hqView.getCampaign().changeRank(person, hqView.getCampaign().getRanks().getRankNumericFromNameAndProfession(
                person.getProfession(), (String) choiceRanks.getSelectedItem()), false);
        refreshView();
    }

    private void refreshRanksCombo() {
        DefaultComboBoxModel<String> ranksModel = new DefaultComboBoxModel<>();

        // Determine correct profession to pass into the loop
        int profession = person.getProfession();
        while (hqView.getCampaign().getRanks().isEmptyProfession(profession) && profession != Ranks.RPROF_MW) {
            profession = hqView.getCampaign().getRanks().getAlternateProfession(profession);
        }
        for (String rankName : hqView.getCampaign().getAllRankNamesFor(profession)) {
            ranksModel.addElement(rankName);
        }
        choiceRanks.setModel(ranksModel);
        choiceRanks.setSelectedIndex(0);
    }
}
