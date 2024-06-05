/*
 * GMToolsDialog.java
 *
 * Copyright (c) 2013-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.EntityReadoutDialog;
import megamek.client.ui.preferences.*;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.MekHQ;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Clan;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Factions;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.AbstractMHQDialog;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.displayWrappers.ClanDisplay;
import mekhq.gui.displayWrappers.FactionDisplay;
import mekhq.gui.panels.LayeredForceIconCreationPanel;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;

public class GMToolsDialog extends AbstractMHQDialog {
    //region Variable Declarations
    private final CampaignGUI gui;
    private final Person person;

    //region GUI Variables
    private JTabbedPane tabbedPane;

    //region General Tab
    // Dice Panel
    private JSpinner spnDiceCount;
    private JSpinner spnDiceNumber;
    private JSpinner spnDiceSides;
    private JLabel lblTotalDiceResult;
    private JTextPane txtIndividualDiceResults;

    // RAT Panel
    private MMComboBox<FactionDisplay> comboRATFaction;
    private JTextField txtYear;
    private MMComboBox<String> comboQuality;
    private MMComboBox<String> comboUnitType;
    private MMComboBox<String> comboUnitWeight;
    private JLabel lblUnitPicked;
    private Entity lastRolledUnit;
    //endregion General Tab

    //region Name Tab
    // Name Panel
    private MMComboBox<String> comboEthnicCode;
    private MMComboBox<Gender> comboGender;
    private MMComboBox<FactionDisplay> comboNameGeneratorFaction;
    private JCheckBox chkClanPersonnel;
    private JSpinner spnNameNumber;
    private JLabel lblCurrentName;
    private JTextArea txtNamesGenerated;
    private String[] lastGeneratedName;

    // Callsign Panel
    private JSpinner spnCallsignNumber;
    private JLabel lblCurrentCallsign;
    private JTextArea txtCallsignsGenerated;
    private String lastGeneratedCallsign;

    // Bloodname Panel
    private MMComboBox<ClanDisplay> comboOriginClan;
    private MMComboBox<Integer> comboBloodnameEra;
    private MMComboBox<Phenotype> comboPhenotype;
    private JLabel lblCurrentBloodname;
    private JLabel lblBloodnameGenerated;
    private JLabel lblOriginClanGenerated;
    private JLabel lblPhenotypeGenerated;
    private JLabel lblBloodnameWarning;
    private Clan originClan;
    private int bloodnameYear;
    private Phenotype selectedPhenotype;
    private String lastGeneratedBloodname;
    //endregion Name Tab

    //region Personnel Module Tab
    // Procreation Panel
    private JCheckBox chkProcreationEligibilityType;
    private JSpinner spnPregnancySize;
    //endregion Personnel Module Tab
    //endregion GUI Variables

    //region Constants
    // FIXME : Inline Magic Constants
    private static final String[] QUALITY_NAMES = { "F", "D", "C", "B", "A", "A*" };
    private static final String[] WEIGHT_NAMES = { "Light", "Medium", "Heavy", "Assault" };
    private static final Integer[] BLOODNAME_ERAS = { 2807, 2825, 2850, 2900, 2950, 3000, 3050, 3060, 3075, 3085, 3100 };
    //endregion Constants
    //endregion Variable Declarations

    //region Constructors
    public GMToolsDialog(final JFrame frame, final CampaignGUI gui, final @Nullable Person person) {
        super(frame, (person != null), "GMToolsDialog", "GMToolsDialog.title");
        this.gui = gui;
        this.person = person;
        initialize();
        setValuesFromPerson();
        validateBloodnameInput();
    }
    //endregion Constructors

    //region Getters and Setters
    public CampaignGUI getGUI() {
        return gui;
    }

    public Person getPerson() {
        return person;
    }

    //region GUI Variables
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void setTabbedPane(final JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    //region General Tab
    public JSpinner getSpnDiceCount() {
        return spnDiceCount;
    }

    public void setSpnDiceCount(final JSpinner spnDiceCount) {
        this.spnDiceCount = spnDiceCount;
    }

    public JSpinner getSpnDiceNumber() {
        return spnDiceNumber;
    }

    public void setSpnDiceNumber(final JSpinner spnDiceNumber) {
        this.spnDiceNumber = spnDiceNumber;
    }

    public JSpinner getSpnDiceSides() {
        return spnDiceSides;
    }

    public void setSpnDiceSides(final JSpinner spnDiceSides) {
        this.spnDiceSides = spnDiceSides;
    }

    public JLabel getLblTotalDiceResult() {
        return lblTotalDiceResult;
    }

    public void setLblTotalDiceResult(final JLabel lblTotalDiceResult) {
        this.lblTotalDiceResult = lblTotalDiceResult;
    }

    public JTextPane getTxtIndividualDiceResults() {
        return txtIndividualDiceResults;
    }

    public void setTxtIndividualDiceResults(final JTextPane txtIndividualDiceResults) {
        this.txtIndividualDiceResults = txtIndividualDiceResults;
    }

    public MMComboBox<FactionDisplay> getComboRATFaction() {
        return comboRATFaction;
    }

    public void setComboRATFaction(final MMComboBox<FactionDisplay> comboRATFaction) {
        this.comboRATFaction = comboRATFaction;
    }

    public JTextField getTxtYear() {
        return txtYear;
    }

    public void setTxtYear(final JTextField txtYear) {
        this.txtYear = txtYear;
    }

    public MMComboBox<String> getComboQuality() {
        return comboQuality;
    }

    public void setComboQuality(final MMComboBox<String> comboQuality) {
        this.comboQuality = comboQuality;
    }

    public MMComboBox<String> getComboUnitType() {
        return comboUnitType;
    }

    public void setComboUnitType(final MMComboBox<String> comboUnitType) {
        this.comboUnitType = comboUnitType;
    }

    public MMComboBox<String> getComboUnitWeight() {
        return comboUnitWeight;
    }

    public void setComboUnitWeight(final MMComboBox<String> comboUnitWeight) {
        this.comboUnitWeight = comboUnitWeight;
    }

    public JLabel getLblUnitPicked() {
        return lblUnitPicked;
    }

    public void setLblUnitPicked(final JLabel lblUnitPicked) {
        this.lblUnitPicked = lblUnitPicked;
    }

    public @Nullable Entity getLastRolledUnit() {
        return lastRolledUnit;
    }

    public void setLastRolledUnit(final @Nullable Entity lastRolledUnit) {
        this.lastRolledUnit = lastRolledUnit;
    }
    //endregion General Tab

    //region Name Tab
    public MMComboBox<String> getComboEthnicCode() {
        return comboEthnicCode;
    }

    public void setComboEthnicCode(final MMComboBox<String> comboEthnicCode) {
        this.comboEthnicCode = comboEthnicCode;
    }

    public MMComboBox<Gender> getComboGender() {
        return comboGender;
    }

    public void setComboGender(final MMComboBox<Gender> comboGender) {
        this.comboGender = comboGender;
    }

    public MMComboBox<FactionDisplay> getComboNameGeneratorFaction() {
        return comboNameGeneratorFaction;
    }

    public void setComboNameGeneratorFaction(final MMComboBox<FactionDisplay> comboNameGeneratorFaction) {
        this.comboNameGeneratorFaction = comboNameGeneratorFaction;
    }

    public JCheckBox getChkClanPersonnel() {
        return chkClanPersonnel;
    }

    public void setChkClanPersonnel(final JCheckBox chkClanPersonnel) {
        this.chkClanPersonnel = chkClanPersonnel;
    }

    public JSpinner getSpnNameNumber() {
        return spnNameNumber;
    }

    public void setSpnNameNumber(final JSpinner spnNameNumber) {
        this.spnNameNumber = spnNameNumber;
    }

    public JLabel getLblCurrentName() {
        return lblCurrentName;
    }

    public void setLblCurrentName(final JLabel lblCurrentName) {
        this.lblCurrentName = lblCurrentName;
    }

    public JTextArea getTxtNamesGenerated() {
        return txtNamesGenerated;
    }

    public void setTxtNamesGenerated(final JTextArea txtNamesGenerated) {
        this.txtNamesGenerated = txtNamesGenerated;
    }

    public @Nullable String[] getLastGeneratedName() {
        return lastGeneratedName;
    }

    public void setLastGeneratedName(final @Nullable String... lastGeneratedName) {
        this.lastGeneratedName = lastGeneratedName;
    }

    public JSpinner getSpnCallsignNumber() {
        return spnCallsignNumber;
    }

    public void setSpnCallsignNumber(final JSpinner spnCallsignNumber) {
        this.spnCallsignNumber = spnCallsignNumber;
    }

    public JLabel getLblCurrentCallsign() {
        return lblCurrentCallsign;
    }

    public void setLblCurrentCallsign(JLabel lblCurrentCallsign) {
        this.lblCurrentCallsign = lblCurrentCallsign;
    }

    public JTextArea getTxtCallsignsGenerated() {
        return txtCallsignsGenerated;
    }

    public void setTxtCallsignsGenerated(final JTextArea txtCallsignsGenerated) {
        this.txtCallsignsGenerated = txtCallsignsGenerated;
    }

    public @Nullable String getLastGeneratedCallsign() {
        return lastGeneratedCallsign;
    }

    public void setLastGeneratedCallsign(final @Nullable String lastGeneratedCallsign) {
        this.lastGeneratedCallsign = lastGeneratedCallsign;
    }

    public MMComboBox<ClanDisplay> getComboOriginClan() {
        return comboOriginClan;
    }

    public void setComboOriginClan(final MMComboBox<ClanDisplay> comboOriginClan) {
        this.comboOriginClan = comboOriginClan;
    }

    public MMComboBox<Integer> getComboBloodnameEra() {
        return comboBloodnameEra;
    }

    public void setComboBloodnameEra(final MMComboBox<Integer> comboBloodnameEra) {
        this.comboBloodnameEra = comboBloodnameEra;
    }

    public MMComboBox<Phenotype> getComboPhenotype() {
        return comboPhenotype;
    }

    public void setComboPhenotype(final MMComboBox<Phenotype> comboPhenotype) {
        this.comboPhenotype = comboPhenotype;
    }

    public JLabel getLblCurrentBloodname() {
        return lblCurrentBloodname;
    }

    public void setLblCurrentBloodname(final JLabel lblCurrentBloodname) {
        this.lblCurrentBloodname = lblCurrentBloodname;
    }

    public JLabel getLblBloodnameGenerated() {
        return lblBloodnameGenerated;
    }

    public void setLblBloodnameGenerated(final JLabel lblBloodnameGenerated) {
        this.lblBloodnameGenerated = lblBloodnameGenerated;
    }

    public JLabel getLblOriginClanGenerated() {
        return lblOriginClanGenerated;
    }

    public void setLblOriginClanGenerated(final JLabel lblOriginClanGenerated) {
        this.lblOriginClanGenerated = lblOriginClanGenerated;
    }

    public JLabel getLblPhenotypeGenerated() {
        return lblPhenotypeGenerated;
    }

    public void setLblPhenotypeGenerated(final JLabel lblPhenotypeGenerated) {
        this.lblPhenotypeGenerated = lblPhenotypeGenerated;
    }

    public JLabel getLblBloodnameWarning() {
        return lblBloodnameWarning;
    }

    public void setLblBloodnameWarning(final JLabel lblBloodnameWarning) {
        this.lblBloodnameWarning = lblBloodnameWarning;
    }

    public @Nullable Clan getOriginClan() {
        return originClan;
    }

    public void setOriginClan(final @Nullable Clan originClan) {
        this.originClan = originClan;
    }

    public int getBloodnameYear() {
        return bloodnameYear;
    }

    public void setBloodnameYear(final int bloodnameYear) {
        this.bloodnameYear = bloodnameYear;
    }

    public @Nullable Phenotype getSelectedPhenotype() {
        return selectedPhenotype;
    }

    public void setSelectedPhenotype(final @Nullable Phenotype selectedPhenotype) {
        this.selectedPhenotype = selectedPhenotype;
    }

    public @Nullable String getLastGeneratedBloodname() {
        return lastGeneratedBloodname;
    }

    public void setLastGeneratedBloodname(final @Nullable String lastGeneratedBloodname) {
        this.lastGeneratedBloodname = lastGeneratedBloodname;
    }
    //endregion Name Tab

    //region Personnel Module Tab
    public JCheckBox getChkProcreationEligibilityType() {
        return chkProcreationEligibilityType;
    }

    public void setChkProcreationEligibilityType(final JCheckBox chkProcreationEligibilityType) {
        this.chkProcreationEligibilityType = chkProcreationEligibilityType;
    }

    public JSpinner getSpnPregnancySize() {
        return spnPregnancySize;
    }

    public void setSpnPregnancySize(final JSpinner spnPregnancySize) {
        this.spnPregnancySize = spnPregnancySize;
    }
    //endregion Personnel Module Tab
    //endregion GUI Variables
    //endregion Getters and Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setTabbedPane(new JTabbedPane());
        getTabbedPane().setName("GMToolsTabbedPane");
        getTabbedPane().addTab(resources.getString("generalTab.title"), createGeneralTab());
        getTabbedPane().addTab(resources.getString("namesTab.title"), createNamesTab());
        //getTabbedPane().addTab(resources.getString("personnelModuleTab.title"), createPersonnelModuleTab());
        getTabbedPane().addTab(resources.getString("layeredForceIconTab.title"), createLayeredForceIconTab());
        return getTabbedPane();
    }

    //region General Tab
    private JScrollPane createGeneralTab() {
        // Create Panel Components
        final JPanel dicePanel = createDicePanel();

        final JPanel ratPanel = createRATPanel();

        // Layout the Panel
        final JPanel panel = new DefaultMHQScrollablePanel(getFrame(), "generalTab");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(dicePanel)
                        .addComponent(ratPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(dicePanel)
                        .addComponent(ratPanel)
        );

        return new JScrollPane(panel);
    }

    private JPanel createDicePanel() {
        // Create the Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("dicePanel.title")));
        panel.setName("dicePanel");

        // Create the Constraints
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 3, 0, 3);

        final int maxGridX;

        // Create the Components and Layout
        setSpnDiceCount(new JSpinner(new SpinnerNumberModel(2, 1, 100, 1)));
        getSpnDiceCount().setName("spnDiceCount");
        panel.add(getSpnDiceCount(), gbc);

        final JLabel lblRolls = new JLabel(resources.getString("lblRolls.text"));
        lblRolls.setName("lblRolls");
        gbc.gridx++;
        panel.add(lblRolls, gbc);

        setSpnDiceNumber(new JSpinner(new SpinnerNumberModel(2, 1, 100, 1)));
        getSpnDiceNumber().setName("spnDiceNumber");
        gbc.gridx++;
        panel.add(getSpnDiceNumber(), gbc);

        final JLabel lblSides = new JLabel(resources.getString("lblSides.text"));
        lblSides.setName("lblSides");
        gbc.gridx++;
        panel.add(lblSides, gbc);

        setSpnDiceSides(new JSpinner(new SpinnerNumberModel(6, 1, 200, 1)));
        getSpnDiceSides().setName("spnDiceSides");
        gbc.gridx++;
        panel.add(getSpnDiceSides(), gbc);

        maxGridX = gbc.gridx;

        final JLabel lblTotalDiceResults = new JLabel(resources.getString("lblTotalDiceResults.text"));
        lblTotalDiceResults.setName("lblTotalDiceResults");
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(lblTotalDiceResults, gbc);

        setLblTotalDiceResult(new JLabel("-"));
        getLblTotalDiceResult().setName("lblTotalDiceResult");
        gbc.gridx++;
        panel.add(getLblTotalDiceResult(), gbc);

        final JButton btnDiceRoll = new MMButton("btnDiceRoll", resources, "btnDiceRoll.text",
                "btnDiceRoll.toolTipText", evt -> performDiceRoll());
        gbc.gridx = maxGridX;
        panel.add(btnDiceRoll, gbc);

        final JLabel lblIndividualDiceResults = new JLabel(resources.getString("lblIndividualDiceResults.text"));
        lblIndividualDiceResults.setName("lblIndividualDiceResults");
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(lblIndividualDiceResults, gbc);

        setTxtIndividualDiceResults(new JTextPane());
        getTxtIndividualDiceResults().setText("-");
        getTxtIndividualDiceResults().setName("txtIndividualDiceResults");
        getTxtIndividualDiceResults().setEditable(false);
        gbc.gridx++;
        gbc.gridwidth = maxGridX - 1;
        panel.add(getTxtIndividualDiceResults(), gbc);

        // Programmatically Assign Accessibility Labels
        lblTotalDiceResults.setLabelFor(getLblTotalDiceResult());
        lblIndividualDiceResults.setLabelFor(getTxtIndividualDiceResults());

        return panel;
    }

    private JPanel createRATPanel() {
        // Create the Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("ratPanel.title")));
        panel.setName("ratPanel");

        // Create the Constraints
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 3, 0, 3);

        final int maxGridX;

        // Create the Components and Layout
        final JLabel lblYear = new JLabel(resources.getString("Year.text"));
        lblYear.setName("lblYear");
        panel.add(lblYear, gbc);

        final JLabel lblFaction = new JLabel(resources.getString("Faction.text"));
        lblFaction.setName("lblFaction");
        gbc.gridx++;
        panel.add(lblFaction, gbc);

        final JLabel lblQuality = new JLabel(resources.getString("lblQuality.text"));
        lblQuality.setName("lblQuality");
        gbc.gridx++;
        panel.add(lblQuality, gbc);

        final JLabel lblUnitType = new JLabel(resources.getString("lblUnitType.text"));
        lblUnitType.setName("lblUnitType");
        gbc.gridx++;
        panel.add(lblUnitType, gbc);

        final JLabel lblWeight = new JLabel(resources.getString("lblWeight.text"));
        lblWeight.setName("lblWeight");
        gbc.gridx++;
        panel.add(lblWeight, gbc);

        maxGridX = gbc.gridx;

        setTxtYear(new JTextField(5));
        getTxtYear().setText(String.valueOf(getGUI().getCampaign().getGameYear()));
        getTxtYear().setName("txtYear");
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(getTxtYear(), gbc);

        final DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();
        factionModel.addAll(FactionDisplay.getSortedValidFactionDisplays(Factions.getInstance().getFactions(),
                (getPerson() == null) ? getGUI().getCampaign().getLocalDate() : getPerson().getBirthday()));
        setComboRATFaction(new MMComboBox<>("comboRATFaction", factionModel));
        getComboRATFaction().setSelectedIndex(0);
        gbc.gridx++;
        panel.add(getComboRATFaction(), gbc);

        setComboQuality(new MMComboBox<>("comboQuality", QUALITY_NAMES));
        gbc.gridx++;
        panel.add(getComboQuality(), gbc);

        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        for (int ut = 0; ut < UnitType.SIZE; ut++) {
            if (getGUI().getCampaign().getUnitGenerator().isSupportedUnitType(ut)) {
                unitTypeModel.addElement(UnitType.getTypeName(ut));
            }
        }
        setComboUnitType(new MMComboBox<>("comboUnitType", unitTypeModel));
        getComboUnitType().addItemListener(ev -> {
            final int unitType = getComboUnitType().getSelectedIndex();
            getComboUnitWeight().setEnabled((unitType == UnitType.MEK) || (unitType == UnitType.TANK)
                    || (unitType == UnitType.AEROSPACEFIGHTER));
        });
        gbc.gridx++;
        panel.add(getComboUnitType(), gbc);

        setComboUnitWeight(new MMComboBox<>("comboUnitWeight", WEIGHT_NAMES));
        gbc.gridx++;
        panel.add(getComboUnitWeight(), gbc);

        setLblUnitPicked(new JLabel("-"));
        getLblUnitPicked().setName("lblUnitPicked");
        getLblUnitPicked().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getLblUnitPicked().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent evt) {
                if (getLastRolledUnit() != null) {
                    new EntityReadoutDialog(getFrame(), isModal(), getLastRolledUnit()).setVisible(true);
                }
            }
        });
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = maxGridX - (getGUI().getCampaign().isGM() ? 2 : 1);
        panel.add(getLblUnitPicked(), gbc);

        final JButton btnRollRAT = new MMButton("btnRollRAT", resources, "btnRollRAT.text",
                "btnRollRAT.toolTipText", evt -> setLastRolledUnit(performRATRoll()));
        gbc.gridx = getGUI().getCampaign().isGM() ? maxGridX - 1 : maxGridX;
        gbc.gridwidth = 1;
        panel.add(btnRollRAT, gbc);

        if (getGUI().getCampaign().isGM()) {
            final JButton btnAddUnit = new MMButton("btnAddUnit", resources, "btnAddUnit.text",
                    "btnAddUnit.toolTipText", evt -> addRATRolledUnit());
            gbc.gridx++;
            panel.add(btnAddUnit, gbc);
        }

        return panel;
    }
    //endregion General Tab

    //region Names Tab
    private JScrollPane createNamesTab() {
        // Create Panel Components
        final JPanel namePanel = createNamePanel();

        final JPanel callsignPanel = createCallsignPanel();

        final JPanel bloodnamePanel = createBloodnamePanel();

        // Layout the Panel
        final AbstractMHQScrollablePanel namesPanel = new DefaultMHQScrollablePanel(getFrame(), "namesPanel");
        final GroupLayout layout = new GroupLayout(namesPanel);
        namesPanel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(namePanel)
                        .addComponent(callsignPanel)
                        .addComponent(bloodnamePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(namePanel)
                        .addComponent(callsignPanel)
                        .addComponent(bloodnamePanel)
        );

        return new JScrollPane(namesPanel);
    }

    private JPanel createNamePanel() {
        // Create the Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("namePanel.title")));
        panel.setName("namePanel");

        // Create the Constraints
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 3, 0, 3);

        final int maxGridX;

        // Create the Components and Layout
        final JLabel lblGender = new JLabel(resources.getString("Gender.text"));
        lblGender.setName("lblGender");
        panel.add(lblGender, gbc);

        final JLabel lblOriginFaction = new JLabel(resources.getString("lblOriginFaction.text"));
        lblOriginFaction.setName("lblOriginFaction");
        gbc.gridx++;
        panel.add(lblOriginFaction, gbc);

        final JLabel lblHistoricalEthnicity = new JLabel(resources.getString("lblHistoricalEthnicity.text"));
        lblHistoricalEthnicity.setName("lblHistoricalEthnicity");
        gbc.gridx++;
        panel.add(lblHistoricalEthnicity, gbc);

        final JLabel lblClanPersonnel = new JLabel(resources.getString("lblClanPersonnel.text"));
        lblClanPersonnel.setName("lblClanPersonnel");
        gbc.gridx++;
        panel.add(lblClanPersonnel, gbc);

        maxGridX = gbc.gridx;

        final DefaultComboBoxModel<Gender> genderModel = new DefaultComboBoxModel<>();
        genderModel.addAll(Gender.getExternalOptions());
        setComboGender(new MMComboBox<>("comboGender", genderModel));
        getComboGender().setSelectedIndex(0);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(getComboGender(), gbc);

        final DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();
        factionModel.addAll(FactionDisplay.getSortedValidFactionDisplays(Factions.getInstance().getFactions(),
                (getPerson() == null) ? getGUI().getCampaign().getLocalDate() : getPerson().getBirthday()));
        setComboNameGeneratorFaction(new MMComboBox<>("comboRATFaction", factionModel));
        getComboNameGeneratorFaction().setSelectedIndex(0);
        gbc.gridx++;
        panel.add(getComboNameGeneratorFaction(), gbc);

        final DefaultComboBoxModel<String> historicalEthnicityModel = new DefaultComboBoxModel<>();
        historicalEthnicityModel.addElement(resources.getString("factionWeighted.text"));
        for (final String historicalEthnicity : RandomNameGenerator.getInstance().getHistoricalEthnicity().values()) {
            historicalEthnicityModel.addElement(historicalEthnicity);
        }
        setComboEthnicCode(new MMComboBox<>("comboEthnicCode", historicalEthnicityModel));
        getComboEthnicCode().setSelectedIndex(0);
        getComboEthnicCode().addActionListener(evt -> getComboNameGeneratorFaction().setEnabled(getComboEthnicCode().getSelectedIndex() == 0));
        gbc.gridx++;
        panel.add(getComboEthnicCode(), gbc);

        setChkClanPersonnel(new JCheckBox());
        getChkClanPersonnel().setName("clanPersonnelPicker");
        getChkClanPersonnel().getAccessibleContext().setAccessibleName(resources.getString("lblClanPersonnel.text"));
        gbc.gridx++;
        panel.add(getChkClanPersonnel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;

        if (getPerson() != null) {
            final JLabel lblCurrentName = new JLabel(resources.getString("lblCurrentName.text"));
            lblCurrentName.setName("lblCurrentName");
            panel.add(lblCurrentName, gbc);

            setLblCurrentName(new JLabel("-"));
            getLblCurrentName().setName("lblCurrentName");
            gbc.gridx++;
            panel.add(getLblCurrentName(), gbc);

            // Some minor adjustments we need to do within the if statement
            lblCurrentName.setLabelFor(getLblCurrentName());
            gbc.gridx++;
        }

        final JLabel lblNameGenerated = new JLabel(resources.getString((getPerson() == null)
                ? "lblNamesGenerated.text" : "lblNameGenerated.text"));
        lblNameGenerated.setName((getPerson() == null) ? "lblNamesGenerated" : "lblNameGenerated");
        panel.add(lblNameGenerated, gbc);

        setTxtNamesGenerated(new JTextArea("-"));
        getTxtNamesGenerated().setName((getPerson() == null) ? "txtNamesGenerated" : "txtNameGenerated");
        gbc.gridx++;
        panel.add(getTxtNamesGenerated(), gbc);

        if (getPerson() == null) {
            setSpnNameNumber(new JSpinner(new SpinnerNumberModel(1, 1, 10, 1)));
            getSpnNameNumber().setName("spnNameNumber");
            gbc.gridx = maxGridX - 1;
            panel.add(getSpnNameNumber(), gbc);

            final JButton btnGenerateNames = new MMButton("btnGenerateNames", resources,
                    "btnGenerateNames.text", "btnGenerateNames.toolTipText", evt -> generateNames());
            gbc.gridx++;
            panel.add(btnGenerateNames, gbc);
        } else {
            final JButton btnAssignName = new MMButton("btnAssignName", resources,
                    "btnAssignName.text", "btnAssignName.toolTipText", evt -> assignName());
            gbc.gridx = maxGridX - 1;
            gbc.gridy++;
            panel.add(btnAssignName, gbc);

            final JButton btnGenerateName = new MMButton("btnGenerateName", resources,
                    "btnGenerateName.text", "btnGenerateName.toolTipText", evt -> generateName());
            gbc.gridx++;
            panel.add(btnGenerateName, gbc);
        }

        // Programmatically Assign Accessibility Labels
        lblGender.setLabelFor(getComboGender());
        lblOriginFaction.setLabelFor(getComboNameGeneratorFaction());
        lblHistoricalEthnicity.setLabelFor(getComboEthnicCode());
        lblClanPersonnel.setLabelFor(getChkClanPersonnel());
        lblNameGenerated.setLabelFor(getTxtNamesGenerated());

        return panel;
    }

    private JPanel createCallsignPanel() {
        // Create the Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("callsignPanel.title")));
        panel.setName("callsignPanel");

        // Create the Constraints
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 3, 0, 3);

        final int maxGridX;

        // Create the Components and Layout
        if (getPerson() != null) {
            final JLabel lblCurrentCallsign = new JLabel(resources.getString("lblCurrentCallsign.text"));
            lblCurrentCallsign.setName("lblCurrentCallsign");
            panel.add(lblCurrentCallsign, gbc);

            setLblCurrentCallsign(new JLabel("-"));
            getLblCurrentCallsign().setName("lblCurrentCallsign");
            gbc.gridx++;
            panel.add(getLblCurrentCallsign(), gbc);

            // Some minor adjustments we need to do within the if statement
            lblCurrentCallsign.setLabelFor(getLblCurrentCallsign());
            gbc.gridx++;
        }

        final JLabel lblCallsignGenerated = new JLabel(resources.getString((getPerson() == null)
                ? "lblCallsignsGenerated.text" : "lblCallsignGenerated.text"));
        lblCallsignGenerated.setName((getPerson() == null) ? "lblCallsignsGenerated" : "lblCallsignGenerated");
        panel.add(lblCallsignGenerated, gbc);

        setTxtCallsignsGenerated(new JTextArea("-"));
        getTxtCallsignsGenerated().setName((getPerson() == null) ? "txtCallsignsGenerated" : "txtCallsignGenerated");
        gbc.gridx++;
        panel.add(getTxtCallsignsGenerated(), gbc);

        maxGridX = gbc.gridx;

        if (getPerson() == null) {
            setSpnCallsignNumber(new JSpinner(new SpinnerNumberModel(1, 1, 10, 1)));
            getSpnCallsignNumber().setName("spnCallsignNumber");
            gbc.gridx++;
            panel.add(getSpnCallsignNumber(), gbc);

            final JButton btnGenerateCallsigns = new MMButton("btnGenerateCallsigns", resources,
                    "btnGenerateCallsigns.text", "btnGenerateCallsigns.toolTipText", evt -> generateCallsigns());
            gbc.gridx++;
            panel.add(btnGenerateCallsigns, gbc);
        } else {
            final JButton btnAssignCallsign = new MMButton("btnAssignCallsign", resources,
                    "btnAssignCallsign.text", "btnAssignCallsign.toolTipText", evt -> assignCallsign());
            gbc.gridx = maxGridX - 1;
            gbc.gridy++;
            panel.add(btnAssignCallsign, gbc);

            final JButton btnGenerateCallsign = new MMButton("btnGenerateCallsign", resources,
                    "btnGenerateCallsign.text", "btnGenerateCallsign.toolTipText", evt -> generateCallsign());
            gbc.gridx++;
            panel.add(btnGenerateCallsign, gbc);
        }

        // Programmatically Assign Accessibility Labels
        lblCallsignGenerated.setLabelFor(getTxtCallsignsGenerated());

        return panel;
    }

    private JPanel createBloodnamePanel() {
        // Create the Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("bloodnamePanel.title")));
        panel.setName("bloodnamePanel");

        // Create the Constraints
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 3, 0, 3);

        final int maxGridX;

        // Create the Components and Layout
        final JLabel lblOriginClan = new JLabel(resources.getString("Clan.text"));
        lblOriginClan.setName("lblOriginClan");
        panel.add(lblOriginClan, gbc);

        final JLabel lblYear = new JLabel(resources.getString("Year.text"));
        lblYear.setName("lblYear");
        gbc.gridx++;
        panel.add(lblYear, gbc);

        final JLabel lblPhenotype = new JLabel(resources.getString("Phenotype.text"));
        lblPhenotype.setName("lblPhenotype");
        gbc.gridx++;
        panel.add(lblPhenotype, gbc);

        final DefaultComboBoxModel<ClanDisplay> originClanModel = new DefaultComboBoxModel<>();
        originClanModel.addAll(ClanDisplay.getSortedClanDisplays(Clan.getClans(),
                getGUI().getCampaign().getLocalDate()));
        setComboOriginClan(new MMComboBox<>("comboOriginClan", originClanModel));
        getComboOriginClan().setSelectedIndex(0);
        getComboOriginClan().addActionListener(evt -> validateBloodnameInput());
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(getComboOriginClan(), gbc);

        setComboBloodnameEra(new MMComboBox<>("comboBloodnameEra", BLOODNAME_ERAS));
        getComboBloodnameEra().setSelectedIndex(0);
        getComboBloodnameEra().addActionListener(evt -> validateBloodnameInput());
        gbc.gridx++;
        panel.add(getComboBloodnameEra(), gbc);

        final DefaultComboBoxModel<Phenotype> phenotypeModel = new DefaultComboBoxModel<>();
        phenotypeModel.addElement(Phenotype.GENERAL);
        for (final Phenotype phenotype : Phenotype.getExternalPhenotypes()) {
            phenotypeModel.addElement(phenotype);
        }
        setComboPhenotype(new MMComboBox<>("comboPhenotype", phenotypeModel));
        getComboPhenotype().setSelectedItem(Phenotype.GENERAL);
        getComboPhenotype().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText((value == null) ? ""
                        : ((value instanceof Phenotype) ? ((Phenotype) value).getGroupingName() : "ERROR"));
                return this;
            }
        });
        getComboPhenotype().addActionListener(evt -> validateBloodnameInput());
        gbc.gridx++;
        panel.add(getComboPhenotype(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;

        if (getPerson() != null) {
            final JLabel lblCurrentBloodname = new JLabel(resources.getString("lblCurrentBloodname.text"));
            lblCurrentBloodname.setName("lblCurrentBloodname");
            panel.add(lblCurrentBloodname, gbc);

            setLblCurrentBloodname(new JLabel("-"));
            getLblCurrentBloodname().setName("lblCurrentBloodname");
            gbc.gridx++;
            panel.add(getLblCurrentBloodname(), gbc);

            // Some minor adjustments we need to do within the if statement
            lblCurrentBloodname.setLabelFor(getLblCurrentBloodname());
            gbc.gridx++;
        }

        final JLabel lblBloodnameGenerated = new JLabel(resources.getString("lblBloodnameGenerated.text"));
        lblBloodnameGenerated.setName("lblBloodnameGenerated");
        panel.add(lblBloodnameGenerated, gbc);

        setLblBloodnameGenerated(new JLabel("-"));
        getLblBloodnameGenerated().setName("lblBloodnameGenerated");
        gbc.gridx++;
        panel.add(getLblBloodnameGenerated(), gbc);

        final JLabel lblOriginClanGenerated = new JLabel(resources.getString("lblOriginClanGenerated.text"));
        lblOriginClanGenerated.setName("lblOriginClanGenerated");
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(lblOriginClanGenerated, gbc);

        setLblOriginClanGenerated(new JLabel("-"));
        getLblOriginClanGenerated().setName("lblOriginClanGenerated");
        gbc.gridx++;
        panel.add(getLblOriginClanGenerated(), gbc);

        final JLabel lblPhenotypeGenerated = new JLabel(resources.getString("lblPhenotypeGenerated.text"));
        lblPhenotypeGenerated.setName("lblPhenotypeGenerated");
        gbc.gridx++;
        panel.add(lblPhenotypeGenerated, gbc);

        setLblPhenotypeGenerated(new JLabel("-"));
        getLblPhenotypeGenerated().setName("lblPhenotypeGenerated");
        gbc.gridx++;
        panel.add(getLblPhenotypeGenerated(), gbc);

        maxGridX = gbc.gridx;

        setLblBloodnameWarning(new JLabel(""));
        getLblBloodnameWarning().setName("lblBloodnameWarning");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = maxGridX - ((getPerson() == null) ? 1 : 2);
        panel.add(getLblBloodnameWarning(), gbc);

        final JButton btnGenerateBloodname = new MMButton("btnGenerateBloodname", resources,
                "btnGenerateBloodname.text", "btnGenerateBloodname.toolTipText", evt -> generateBloodname());
        gbc.gridx = maxGridX - ((getPerson() == null) ? 0 : 1);
        gbc.gridwidth = 1;
        panel.add(btnGenerateBloodname, gbc);

        if (getPerson() != null) {
            final JButton btnAssignBloodname = new MMButton("btnAssignBloodname", resources,
                    "btnAssignBloodname.text", "btnAssignBloodname.toolTipText", evt -> assignBloodname());
            gbc.gridx++;
            panel.add(btnAssignBloodname, gbc);
        }

        // Programmatically Assign Accessibility Labels
        lblOriginClan.setLabelFor(getComboOriginClan());
        lblYear.setLabelFor(getComboBloodnameEra());
        lblPhenotype.setLabelFor(getComboPhenotype());
        lblBloodnameGenerated.setLabelFor(getLblBloodnameGenerated());
        lblOriginClanGenerated.setLabelFor(getLblOriginClanGenerated());
        lblPhenotypeGenerated.setLabelFor(getLblPhenotypeGenerated());

        return panel;
    }
    //endregion Names Tab

    //region Personnel Module Tab
    private JScrollPane createPersonnelModuleTab() {
        // Create Panel Components
        final JPanel procreationPanel = createProcreationPanel();

        // Layout the Panel
        final AbstractMHQScrollablePanel personnelModulePanel = new DefaultMHQScrollablePanel(
                getFrame(), "personnelModulePanel");
        final GroupLayout layout = new GroupLayout(personnelModulePanel);
        personnelModulePanel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(procreationPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(procreationPanel)
        );

        return new JScrollPane(personnelModulePanel);
    }

    private JPanel createProcreationPanel() {
        // Create the Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("procreationPanel.title")));
        panel.setName("procreationPanel");

        // Create the Constraints
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 3, 0, 3);

        final int maxGridX;

        // Create the Components and Layout
        if (getPerson() != null) {
            final JLabel lblEligible = new JLabel(resources.getString("Eligible.text"));
            lblEligible.setName("lblEligible");
            panel.add(lblEligible, gbc);

            final JLabel lblEligibility = new JLabel(resources.getString("True.text"));
            lblEligibility.setName("lblEligibility");
            gbc.gridx++;
            panel.add(lblEligibility, gbc);

            setChkProcreationEligibilityType(new JCheckBox(resources.getString("chkProcreationEligibilityType.text")));
            getChkProcreationEligibilityType().setToolTipText(resources.getString("chkProcreationEligibilityType.toolTipText"));
            getChkProcreationEligibilityType().setName("chkProcreationEligibilityType");
            getChkProcreationEligibilityType().addActionListener(evt -> {
                final String reason = getGUI().getCampaign().getProcreation().canProcreate(
                        getGUI().getCampaign().getLocalDate(), getPerson(),
                        getChkProcreationEligibilityType().isSelected());
                lblEligibility.setText(resources.getString((reason == null) ? "True.text" : "False.text"));
                lblEligibility.setToolTipText(reason);
            });
            gbc.gridx++;
            panel.add(getChkProcreationEligibilityType(), gbc);

            // Male Personnel are invalid after this point
            if (getPerson().getGender().isMale()) {
                return panel;
            }
        }

        final JPanel procreationSimulationPanel = new JPanel();
        procreationSimulationPanel.setBorder(BorderFactory.createTitledBorder(resources.getString("procreationSimulationPanel.title")));
        procreationSimulationPanel.setToolTipText(resources.getString("procreationSimulationPanel.toolTipText"));
        procreationSimulationPanel.setName("procreationSimulationPanel");


        return panel;
    }
    //endregion Personnel Module Tab

    //region Layered Force Icon Tab
    private JPanel createLayeredForceIconTab() {
        return new LayeredForceIconCreationPanel(getFrame(), null, true);
    }
    //endregion Layered Force Icon Tab

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        preferences.manage(new JTabbedPanePreference(getTabbedPane()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnDiceCount()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnDiceNumber()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnDiceSides()));

        preferences.manage(new JComboBoxPreference(getComboRATFaction()));
        preferences.manage(new JTextFieldPreference(getTxtYear()));
        preferences.manage(new JComboBoxPreference(getComboQuality()));
        preferences.manage(new JComboBoxPreference(getComboUnitType()));
        preferences.manage(new JComboBoxPreference(getComboUnitWeight()));

        if (getSpnNameNumber() != null) {
            preferences.manage(new JIntNumberSpinnerPreference(getSpnNameNumber()));
        }

        if (getSpnCallsignNumber() != null) {
            preferences.manage(new JIntNumberSpinnerPreference(getSpnCallsignNumber()));
        }
    }

    private void setValuesFromPerson() {
        if (getPerson() == null) {
            return;
        }

        setTitle(getTitle() + " - " + getPerson().getFullTitle());

        // Current Name is the Person's full name
        getLblCurrentName().setText(getPerson().getFullName());

        // Gender is set based on the person's gender
        getComboGender().setSelectedItem(getPerson().getGender().isExternal() ? getPerson().getGender()
                : getPerson().getGender().getExternalVariant());

        // Current Callsign is set if applicable
        if (!StringUtility.isNullOrBlank(getPerson().getCallsign())) {
            getLblCurrentCallsign().setText(getPerson().getCallsign());
        }

        // We set the clan personnel value based on whether or not the person is clan personell
        getChkClanPersonnel().setSelected(getPerson().isClanPersonnel());

        // Now we figure out the person's origin faction
        final FactionDisplay faction = new FactionDisplay(getPerson().getOriginFaction(), getPerson().getBirthday());
        getComboRATFaction().setSelectedItem(faction);
        getComboNameGeneratorFaction().setSelectedItem(faction);

        // Finally, we determine the default unit type
        for (int i = 0; i < getComboUnitType().getModel().getSize(); i++) {
            if (doesPersonPrimarilyDriveUnitType(UnitType.determineUnitTypeCode(getComboUnitType().getItemAt(i)))) {
                getComboUnitType().setSelectedIndex(i);
                break;
            }
        }

        if (!StringUtility.isNullOrBlank(getPerson().getBloodname())) {
            getLblCurrentBloodname().setText(getPerson().getBloodname());
        }

        int year = getGUI().getCampaign().getGameYear();
        for (int i = BLOODNAME_ERAS.length - 1; i >= 0; i--) {
            if (BLOODNAME_ERAS[i] <= year) {
                getComboBloodnameEra().setSelectedIndex(i);
                break;
            }
        }

        final Clan clan = Clan.getClan((getGUI().getCampaign().getFaction().isClan()
                ? getGUI().getCampaign().getFaction() : getPerson().getOriginFaction()).getShortName());
        if (clan != null) {
            getComboOriginClan().setSelectedItem(new ClanDisplay(clan, getGUI().getCampaign().getLocalDate()));
        }

        getComboPhenotype().setSelectedItem(getPerson().getPhenotype());
    }

    /**
     * Determine if a person's primary role supports operating a given unit type.
     */
    private boolean doesPersonPrimarilyDriveUnitType(final int unitType) {
        switch (unitType) {
            case UnitType.AERO:
            case UnitType.AEROSPACEFIGHTER:
                return getPerson().getPrimaryRole().isAerospacePilot();
            case UnitType.BATTLE_ARMOR:
                return getPerson().getPrimaryRole().isBattleArmour();
            case UnitType.CONV_FIGHTER:
                return getPerson().getPrimaryRole().isConventionalAircraftPilot()
                        || getPerson().getPrimaryRole().isAerospacePilot();
            case UnitType.DROPSHIP:
            case UnitType.JUMPSHIP:
            case UnitType.SMALL_CRAFT:
            case UnitType.WARSHIP:
                return getPerson().getPrimaryRole().isVesselPilot();
            case UnitType.INFANTRY:
                return getPerson().getPrimaryRole().isSoldier();
            case UnitType.MEK:
                return getPerson().getPrimaryRole().isMechWarrior();
            case UnitType.NAVAL:
                return getPerson().getPrimaryRole().isNavalVehicleDriver();
            case UnitType.PROTOMEK:
                return getPerson().getPrimaryRole().isProtoMechPilot();
            case UnitType.TANK:
                return getPerson().getPrimaryRole().isGroundVehicleDriver();
            case UnitType.VTOL:
                return getPerson().getPrimaryRole().isVTOLPilot();
            default:
                return false;
        }
    }
    //endregion Initialization

    //region ActionEvent Handlers
    public void performDiceRoll() {
        final List<Integer> individualDice = Compute.individualRolls((Integer) getSpnDiceCount().getValue(),
                (Integer) getSpnDiceNumber().getValue(), (Integer) getSpnDiceSides().getValue());
        getLblTotalDiceResult().setText(String.format(resources.getString("lblTotalDiceResult.text"), individualDice.get(0)));

        final StringBuilder sb = new StringBuilder();
        for (int i = 1; i < individualDice.size() - 1; i++) {
            sb.append(individualDice.get(i)).append(", ");
        }
        sb.append(individualDice.get(individualDice.size() - 1));

        getTxtIndividualDiceResults().setText((sb.length() > 0) ? sb.toString() : "-");
    }

    private @Nullable Entity performRATRoll() {
        final int targetYear;
        try {
            targetYear = Integer.parseInt(getTxtYear().getText());
        } catch (Exception ignored) {
            getLblUnitPicked().setText(Messages.getString("yearParsingFailure.error"));
            return null;
        }

        final Predicate<MechSummary> predicate = summary ->
                (!getGUI().getCampaign().getCampaignOptions().isLimitByYear() || (targetYear > summary.getYear()))
                        && (!summary.isClan() || getGUI().getCampaign().getCampaignOptions().isAllowClanPurchases())
                        && (summary.isClan() || getGUI().getCampaign().getCampaignOptions().isAllowISPurchases());
        final int unitType = UnitType.determineUnitTypeCode(getComboUnitType().getSelectedItem());
        final int unitWeight = getComboUnitWeight().isEnabled()
                ? getComboUnitWeight().getSelectedIndex() + EntityWeightClass.WEIGHT_LIGHT
                : AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED;
        final MechSummary summary = getGUI().getCampaign().getUnitGenerator()
                .generate(Objects.requireNonNull(getComboRATFaction().getSelectedItem()).getFaction().getShortName(),
                        unitType, unitWeight, targetYear, getComboQuality().getSelectedIndex(), predicate);

        if (summary == null) {
            getLblUnitPicked().setText(Messages.getString("noValidUnit.error"));
            return null;
        }

        try {
            final Entity entity = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
            getLblUnitPicked().setText(String.format("<html><a href='ENTITY'>%s</html>", summary.getName()));
            return entity;
        } catch (Exception ex) {
            final String message = String.format(Messages.getString("entityLoadFailure.error"),
                    summary.getName(), summary.getSourceFile());
            LogManager.getLogger().error(message, ex);
            getLblUnitPicked().setText(message);
            return null;
        }
    }

    private void addRATRolledUnit() {
        if (getLastRolledUnit() == null) {
            setLastRolledUnit(performRATRoll());
        }

        if (getLastRolledUnit() != null) {
            final Unit unit = getGUI().getCampaign().addNewUnit(getLastRolledUnit(), false, 0);
            if ((getPerson() != null) && (getPerson().getUnit() == null)) {
                unit.addPilotOrSoldier(getPerson());
                getPerson().setOriginalUnit(unit);
            }
            setLastRolledUnit(null);
        }
    }

    private void generateName() {
        final String[] name = generateIndividualName();
        getTxtNamesGenerated().setText((name[0] + ' ' + name[1]).trim());
        setLastGeneratedName(name);
    }

    private void generateNames() {
        final StringJoiner sj = new StringJoiner("\n");
        for (int i = 0; i < (Integer) getSpnNameNumber().getValue(); i++) {
            final String[] name = generateIndividualName();
            sj.add((name[0] + ' ' + name[1]).trim());
        }
        getTxtNamesGenerated().setText(sj.toString());
    }

    private String[] generateIndividualName() {
        final int ethnicCode = getComboEthnicCode().getSelectedIndex();
        final String[] name;

        if (ethnicCode == 0) {
            name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                    getComboGender().getSelectedItem(), getChkClanPersonnel().isSelected(),
                    (Objects.requireNonNull(getComboNameGeneratorFaction().getSelectedItem()))
                            .getFaction().getShortName());
        } else {
            name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplitWithEthnicCode(
                    getComboGender().getSelectedItem(), getChkClanPersonnel().isSelected(), ethnicCode);
        }
        return name;
    }

    private void assignName() {
        if (getLastGeneratedName() == null) {
            generateName();
        }

        if (getLastGeneratedName() != null) {
            getLblCurrentName().setText((getLastGeneratedName()[0] + ' ' + getLastGeneratedName()[1]).trim());
            getPerson().setGivenName(getLastGeneratedName()[0]);
            getPerson().setSurname(getLastGeneratedName()[1]);
            MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
        }
    }

    private void generateCallsign() {
        getTxtCallsignsGenerated().setText(RandomCallsignGenerator.getInstance().generate());
        setLastGeneratedCallsign(getTxtCallsignsGenerated().getText());
    }

    private void generateCallsigns() {
        final StringJoiner sj = new StringJoiner("\n");
        for (int i = 0; i < (Integer) getSpnCallsignNumber().getValue(); i++) {
            sj.add(RandomCallsignGenerator.getInstance().generate());
        }
        getTxtCallsignsGenerated().setText(sj.toString());
    }

    private void assignCallsign() {
        if (getLastGeneratedCallsign() == null) {
            generateCallsign();
        }

        if (getLastGeneratedCallsign() != null) {
            getLblCurrentCallsign().setText(getLastGeneratedCallsign());
            getPerson().setCallsign(getLastGeneratedCallsign());
            MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
        }
    }

    private void generateBloodname() {
        final Bloodname bloodname = Bloodname.randomBloodname(getOriginClan(),
                getSelectedPhenotype(), getBloodnameYear());
        if (bloodname != null) {
            getLblBloodnameGenerated().setText(bloodname.getName() + " (" + bloodname.getFounder() + ')');
            getLblOriginClanGenerated().setText(bloodname.getOriginClan().getFullName(getBloodnameYear()));
            getLblPhenotypeGenerated().setText(bloodname.getPhenotype().getGroupingName());
            setLastGeneratedBloodname(bloodname.getName());
        }
    }

    private void assignBloodname() {
        if (getLastGeneratedBloodname() == null) {
            generateBloodname();
        }

        if (getLastGeneratedBloodname() != null) {
            getLblCurrentBloodname().setText(getLastGeneratedBloodname());
            getPerson().setBloodname(getLastGeneratedBloodname());
            MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
        }
    }

    private void validateBloodnameInput() {
        setOriginClan((getComboOriginClan().getSelectedItem() == null) ? null
                : getComboOriginClan().getSelectedItem().getClan());
        setBloodnameYear(BLOODNAME_ERAS[getComboBloodnameEra().getSelectedIndex()]);
        setSelectedPhenotype(getComboPhenotype().getSelectedItem());

        if ((getOriginClan() == null) || (getSelectedPhenotype() == null)
                || getSelectedPhenotype().isNone()) {
            return;
        }

        String txt = "<html>";

        if (getBloodnameYear() < getOriginClan().getStartDate()) {
            for (int era : BLOODNAME_ERAS) {
                if (era >= getOriginClan().getStartDate()) {
                    setBloodnameYear(era);
                    txt += "<div>" + getOriginClan().getFullName(getBloodnameYear()) + " formed in "
                            + getOriginClan().getStartDate() + ". Using " + getBloodnameYear() + ".</div>";
                    break;
                }
            }

            if (getBloodnameYear() < getOriginClan().getStartDate()) {
                setBloodnameYear(getOriginClan().getStartDate());
            }
        } else if (getBloodnameYear() > getOriginClan().getEndDate()) {
            for (int i = BLOODNAME_ERAS.length - 1; i >= 0; i--) {
                if (BLOODNAME_ERAS[i] <= getOriginClan().getEndDate()) {
                    setBloodnameYear(BLOODNAME_ERAS[i]);
                    txt += "<div>" + getOriginClan().getFullName(getBloodnameYear()) + " ceased to existed in "
                            + getOriginClan().getEndDate() + ". Using " + getBloodnameYear() + ".</div>";
                    break;
                }
            }

            if (getBloodnameYear() > getOriginClan().getEndDate()) {
                setBloodnameYear(getOriginClan().getEndDate());
            }
        }

        if (getSelectedPhenotype().isProtoMech() && (getBloodnameYear() < 3060)) {
            txt += "<div>ProtoMechs did not exist in " + getBloodnameYear() + ". Using Aerospace.</div>";
            setSelectedPhenotype(Phenotype.AEROSPACE);
        } else if (getSelectedPhenotype().isNaval() && (!"CSR".equals(getOriginClan().getGenerationCode()))) {
            txt += "<div>The Naval phenotype is unique to Clan Snow Raven. Using General.</div>";
            setSelectedPhenotype(Phenotype.GENERAL);
        } else if (getSelectedPhenotype().isVehicle() && (!"CHH".equals(getOriginClan().getGenerationCode()))) {
            txt += "<div>The vehicle phenotype is unique to Clan Hell's Horses. Using General.</div>";
            setSelectedPhenotype(Phenotype.GENERAL);
        } else if (getSelectedPhenotype().isVehicle() && (getBloodnameYear() < 3100)) {
            txt += "<div>The vehicle phenotype began development in the 32nd century. Using 3100.</div>";
            setBloodnameYear(3100);
        }
        txt += "</html>";

        getLblBloodnameWarning().setText(txt);
    }
    //endregion ActionEvent Handlers
}
