/*/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.dialogs.PortraitChooserDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.randomEvents.PersonalityController;
import mekhq.gui.CampaignGUI;
import mekhq.gui.displayWrappers.RankDisplay;
import mekhq.gui.view.PersonViewPanel;

/**
 * This dialog is used to both hire new pilots and to edit existing ones
 */
public class NewRecruitDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(NewRecruitDialog.class);

    private Person person;

    private CampaignGUI hqView;

    private JComboBox<RankDisplay> choiceRanks;

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
        // I can't just call it here, because it ends up getting reset somewhere later
        SwingUtilities.invokeLater(() -> scrollView.getVerticalScrollBar().setValue(0));
    }

    private void initComponents() {
        scrollView = new JScrollPane();
        choiceRanks = new JComboBox<>();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewRecruitDialog",
                MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setTitle(resourceMap.getString("Form.title"));

        setName("Form");
        getContentPane().setLayout(new BorderLayout());

        JPanel panSidebar = createSidebar(resourceMap);

        JPanel panBottomButtons = createButtonPanel(resourceMap);

        scrollView.setMinimumSize(new Dimension(450, 180));
        scrollView.setPreferredSize(new Dimension(450, 180));
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
        panButtons.setName("panButtons");
        panButtons.setLayout(new GridBagLayout());

        JButton button = new JButton(resourceMap.getString("btnHire.text"));
        button.setName("btnOk");
        button.addActionListener(e -> hire());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;

        panButtons.add(button, gridBagConstraints);
        gridBagConstraints.gridx++;

        if (hqView.getCampaign().isGM()) {
            button = new JButton(resourceMap.getString("btnAddGM.text"));
            button.setName("btnGM");
            button.addActionListener(e -> addGM());

            panButtons.add(button, gridBagConstraints);
            gridBagConstraints.gridx++;
        }

        button = new JButton(resourceMap.getString("btnClose.text"));
        button.setName("btnClose");
        button.addActionListener(e -> setVisible(false));
        panButtons.add(button, gridBagConstraints);

        return panButtons;
    }

    private JPanel createSidebar(ResourceBundle resourceMap) {
        boolean randomizeOrigin = hqView.getCampaign().getCampaignOptions().getRandomOriginOptions()
                .isRandomizeOrigin();

        JPanel panSidebar = new JPanel();
        panSidebar.setName("panButtons");
        panSidebar.setLayout(new GridLayout(6 + (randomizeOrigin ? 1 : 0), 1));

        choiceRanks.setName("choiceRanks");
        refreshRanksCombo();
        choiceRanks.addActionListener(e -> changeRank());
        panSidebar.add(choiceRanks);

        JButton button = new JButton(resourceMap.getString("btnRandomName.text"));
        button.setName("btnRandomName");
        button.addActionListener(e -> randomName());
        panSidebar.add(button);

        button = new JButton(resourceMap.getString("btnRandomPortrait.text"));
        button.setName("btnRandomPortrait");
        button.addActionListener(e -> randomPortrait());
        panSidebar.add(button);

        if (randomizeOrigin) {
            button = new JButton(resourceMap.getString("btnRandomOrigin.text"));
            button.setName("btnRandomOrigin");
            button.addActionListener(e -> randomOrigin());
            panSidebar.add(button);
        }

        button = new JButton(resourceMap.getString("btnChoosePortrait.text"));
        button.setName("btnChoosePortrait");
        button.addActionListener(e -> choosePortrait());
        panSidebar.add(button);

        button = new JButton(resourceMap.getString("btnEditPerson.text"));
        button.setName("btnEditPerson");
        button.addActionListener(e -> editPerson());
        button.setEnabled(hqView.getCampaign().isGM());
        panSidebar.add(button);

        button = new JButton(resourceMap.getString("btnRegenerate.text"));
        button.setName("btnRegenerate");
        button.addActionListener(e -> regenerate());
        button.setEnabled(hqView.getCampaign().isGM());
        panSidebar.add(button);

        return panSidebar;
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(NewRecruitDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
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
        person.setRank(((RankDisplay) Objects.requireNonNull(choiceRanks.getSelectedItem())).getRankNumeric());
    }

    private void randomName() {
        String factionCode = hqView.getCampaign().getCampaignOptions().isUseOriginFactionForNames()
                ? person.getOriginFaction().getShortName()
                : RandomNameGenerator.getInstance().getChosenFaction();

        String[] name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                person.getGender(), person.isClanPersonnel(), factionCode);
        person.setGivenName(name[0]);
        person.setSurname(name[1]);
        PersonalityController.writeDescription(person);
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
        final PortraitChooserDialog portraitDialog = new PortraitChooserDialog(hqView.getFrame(), person.getPortrait());
        if (portraitDialog.showDialog().isConfirmed()) {
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
        person.setRank(((RankDisplay) Objects.requireNonNull(choiceRanks.getSelectedItem())).getRankNumeric());
        refreshView();
    }

    private void refreshRanksCombo() {
        DefaultComboBoxModel<RankDisplay> ranksModel = new DefaultComboBoxModel<>();
        ranksModel.addAll(RankDisplay.getRankDisplaysForSystem(person.getRankSystem(),
                Profession.getProfessionFromPersonnelRole(person.getPrimaryRole())));
        choiceRanks.setModel(ranksModel);
        choiceRanks.setSelectedIndex(0);
    }
}
