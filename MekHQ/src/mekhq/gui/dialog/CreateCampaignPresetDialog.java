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
package mekhq.gui.dialog;

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.enums.ValidationState;
import megamek.client.ui.preferences.JIntNumberSpinnerPreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.GameOptionsDialog;
import megamek.common.annotations.Nullable;
import megamek.common.options.GameOptions;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignPreset;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.baseComponents.SortedComboBoxModel;
import mekhq.gui.displayWrappers.FactionDisplay;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.stream.Collectors;

public class CreateCampaignPresetDialog extends AbstractMHQValidationButtonDialog {
    //region Variable Declarations
    private final Campaign campaign;
    private CampaignPreset preset;

    private JTextField txtPresetName;
    private JTextArea txtPresetDescription;

    //region Startup
    private JCheckBox chkSpecifyDate;
    private LocalDate date;
    private JCheckBox chkSpecifyFaction;
    private MMComboBox<FactionDisplay> comboFaction;
    private JCheckBox chkSpecifyPlanet;
    private JCheckBox chkStartingSystemFactionSpecific;
    private MMComboBox<PlanetarySystem> comboStartingSystem;
    private MMComboBox<Planet> comboStartingPlanet;
    private JCheckBox chkSpecifyRankSystem;
    private MMComboBox<RankSystem> comboRankSystem;
    private JSpinner spnContractCount;
    //endregion Startup

    //region Continuous
    private JCheckBox chkSpecifyGameOptions;
    private final GameOptions gameOptions;
    private JCheckBox chkSpecifyCampaignOptions;
    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;
    private final Hashtable<String, SkillType> skills;
    private final Hashtable<String, SpecialAbility> specialAbilities;
    //endregion Continuous
    //endregion Variable Declarations

    //region Constructors
    public CreateCampaignPresetDialog(final JFrame frame, final Campaign campaign,
                                      final @Nullable CampaignPreset preset) {
        super(frame, "CreateCampaignPresetDialog", "CreateCampaignPresetDialog.title");
        this.campaign = campaign;
        setPreset(preset);
        setDate(campaign.getLocalDate());
        this.gameOptions = ((preset == null) || (preset.getGameOptions() == null))
                ? campaign.getGameOptions() : preset.getGameOptions();
        this.campaignOptions = ((preset == null) || (preset.getCampaignOptions() == null))
                ? campaign.getCampaignOptions() : preset.getCampaignOptions();
        this.randomSkillPreferences = ((preset == null) || (preset.getRandomSkillPreferences() == null))
                ? campaign.getRandomSkillPreferences() : preset.getRandomSkillPreferences();
        this.skills = ((preset == null) || preset.getSkills().isEmpty())
                ? SkillType.getSkillHash() : preset.getSkills();
        this.specialAbilities = ((preset == null) || preset.getSpecialAbilities().isEmpty())
                ? SpecialAbility.getAllSpecialAbilities() : preset.getSpecialAbilities();
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public @Nullable CampaignPreset getPreset() {
        return preset;
    }

    public void setPreset(final @Nullable CampaignPreset preset) {
        this.preset = preset;
    }

    public JTextField getTxtPresetName() {
        return txtPresetName;
    }

    public void setTxtPresetName(final JTextField txtPresetName) {
        this.txtPresetName = txtPresetName;
    }

    public JTextArea getTxtPresetDescription() {
        return txtPresetDescription;
    }

    public void setTxtPresetDescription(final JTextArea txtPresetDescription) {
        this.txtPresetDescription = txtPresetDescription;
    }

    //region Startup
    public JCheckBox getChkSpecifyDate() {
        return chkSpecifyDate;
    }

    public void setChkSpecifyDate(final JCheckBox chkSpecifyDate) {
        this.chkSpecifyDate = chkSpecifyDate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(final LocalDate date) {
        this.date = date;
    }

    public JCheckBox getChkSpecifyFaction() {
        return chkSpecifyFaction;
    }

    public void setChkSpecifyFaction(final JCheckBox chkSpecifyFaction) {
        this.chkSpecifyFaction = chkSpecifyFaction;
    }

    public MMComboBox<FactionDisplay> getComboFaction() {
        return comboFaction;
    }

    public void setComboFaction(final MMComboBox<FactionDisplay> comboFaction) {
        this.comboFaction = comboFaction;
    }

    public JCheckBox getChkSpecifyPlanet() {
        return chkSpecifyPlanet;
    }

    public void setChkSpecifyPlanet(final JCheckBox chkSpecifyPlanet) {
        this.chkSpecifyPlanet = chkSpecifyPlanet;
    }

    public JCheckBox getChkStartingSystemFactionSpecific() {
        return chkStartingSystemFactionSpecific;
    }

    public void setChkStartingSystemFactionSpecific(final JCheckBox chkStartingSystemFactionSpecific) {
        this.chkStartingSystemFactionSpecific = chkStartingSystemFactionSpecific;
    }

    public MMComboBox<PlanetarySystem> getComboStartingSystem() {
        return comboStartingSystem;
    }

    public void setComboStartingSystem(final MMComboBox<PlanetarySystem> comboStartingSystem) {
        this.comboStartingSystem = comboStartingSystem;
    }

    private void restoreComboStartingSystem() {
        getComboStartingSystem().removeAllItems();
        getComboStartingSystem().setModel(new DefaultComboBoxModel<>(getPlanetarySystems(
                getChkStartingSystemFactionSpecific().isSelected() ? getComboFaction().getSelectedItem() : null)));
        restoreComboStartingPlanet();
    }

    public MMComboBox<Planet> getComboStartingPlanet() {
        return comboStartingPlanet;
    }

    public void setComboStartingPlanet(final MMComboBox<Planet> comboStartingPlanet) {
        this.comboStartingPlanet = comboStartingPlanet;
    }

    private void restoreComboStartingPlanet()  {
        final PlanetarySystem system = getComboStartingSystem().getSelectedItem();
        if (system != null) {
            getComboStartingPlanet().setModel(new DefaultComboBoxModel<>(
                    system.getPlanets().toArray(new Planet[]{})));
            getComboStartingPlanet().setSelectedItem(system.getPrimaryPlanet());
        } else {
            getComboStartingPlanet().removeAllItems();
        }
    }

    public JCheckBox getChkSpecifyRankSystem() {
        return chkSpecifyRankSystem;
    }

    public void setChkSpecifyRankSystem(final JCheckBox chkSpecifyRankSystem) {
        this.chkSpecifyRankSystem = chkSpecifyRankSystem;
    }

    public MMComboBox<RankSystem> getComboRankSystem() {
        return comboRankSystem;
    }

    public void setComboRankSystem(final MMComboBox<RankSystem> comboRankSystem) {
        this.comboRankSystem = comboRankSystem;
    }

    public JSpinner getSpnContractCount() {
        return spnContractCount;
    }

    public void setSpnContractCount(final JSpinner spnContractCount) {
        this.spnContractCount = spnContractCount;
    }
    //endregion Startup

    //region Continuous
    public JCheckBox getChkSpecifyGameOptions() {
        return chkSpecifyGameOptions;
    }

    public void setChkSpecifyGameOptions(final JCheckBox chkSpecifyGameOptions) {
        this.chkSpecifyGameOptions = chkSpecifyGameOptions;
    }

    public GameOptions getGameOptions() {
        return gameOptions;
    }

    public JCheckBox getChkSpecifyCampaignOptions() {
        return chkSpecifyCampaignOptions;
    }

    public void setChkSpecifyCampaignOptions(final JCheckBox chkSpecifyCampaignOptions) {
        this.chkSpecifyCampaignOptions = chkSpecifyCampaignOptions;
    }

    public CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }

    public RandomSkillPreferences getRandomSkillPreferences() {
        return randomSkillPreferences;
    }

    public Hashtable<String, SkillType> getSkills() {
        return skills;
    }

    public Hashtable<String, SpecialAbility> getSpecialAbilities() {
        return specialAbilities;
    }
    //endregion Continuous
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("createCampaignPresetPanel");

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        setTxtPresetName(new JTextField(resources.getString("txtPresetName.text")));
        getTxtPresetName().setToolTipText(resources.getString("txtPresetName.toolTipText"));
        getTxtPresetName().setName("txtPresetName");
        getTxtPresetName().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }

            @Override
            public void removeUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }

            @Override
            public void changedUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }
        });
        panel.add(getTxtPresetName(), gbc);

        setTxtPresetDescription(new JTextArea(resources.getString("txtPresetDescription.text")));
        getTxtPresetDescription().setToolTipText(resources.getString("txtPresetDescription.toolTipText"));
        getTxtPresetDescription().setName("txtPresetDescription");
        getTxtPresetDescription().setEditable(true);
        getTxtPresetDescription().setLineWrap(true);
        getTxtPresetDescription().setWrapStyleWord(true);
        gbc.gridy++;
        panel.add(getTxtPresetDescription(), gbc);

        gbc.gridy++;
        panel.add(createStartupPanel(), gbc);

        gbc.gridy++;
        panel.add(createContinuousPanel(), gbc);

        return panel;
    }

    private JPanel createStartupPanel() {
        // Initialize Components Used in ActionListeners
        final JButton btnDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(getDate()));

        // Create Panel Components
        setChkSpecifyDate(new JCheckBox(resources.getString("chkSpecifyDate.text")));
        getChkSpecifyDate().setToolTipText(String.format(resources.getString("chkSpecifyDate.toolTipText"),
                MekHQ.getMekHQOptions().getDisplayFormattedDate(LocalDate.ofYearDay(3067, 1))));
        getChkSpecifyDate().setName("chkSpecifyDate");
        getChkSpecifyDate().addActionListener(evt -> btnDate.setEnabled(getChkSpecifyDate().isSelected()));

        btnDate.setToolTipText(resources.getString("btnDate.toolTipText"));
        btnDate.setName("btnDate");
        btnDate.addActionListener(evt -> {
            final DateChooser dateChooser = new DateChooser(getFrame(), getDate());
            if (dateChooser.showDateChooser() == DateChooser.OK_OPTION) {
                setDate(dateChooser.getDate());
                btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(getDate()));
            }
        });

        setChkSpecifyFaction(new JCheckBox(resources.getString("chkSpecifyFaction.text")));
        getChkSpecifyFaction().setToolTipText(resources.getString("chkSpecifyFaction.toolTipText"));
        getChkSpecifyFaction().setName("chkSpecifyFaction");
        getChkSpecifyFaction().addActionListener(evt -> {
            final boolean selected = getChkSpecifyFaction().isSelected();
            getComboFaction().setEnabled(selected);
            if (!selected && getChkStartingSystemFactionSpecific().isSelected()) {
                getChkStartingSystemFactionSpecific().doClick();
            }
            getChkStartingSystemFactionSpecific().setEnabled(selected && getChkSpecifyPlanet().isSelected());
        });

        final DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();
        factionModel.addAll(FactionDisplay.getSortedFactionDisplays(
                Factions.getInstance().getChoosableFactions(), getDate()));
        setComboFaction(new MMComboBox<>("comboFactions", factionModel));

        setChkSpecifyPlanet(new JCheckBox(resources.getString("chkSpecifyPlanet.text")));
        getChkSpecifyPlanet().setToolTipText(resources.getString("chkSpecifyPlanet.toolTipText"));
        getChkSpecifyPlanet().setName("chkSpecifyPlanet");
        getChkSpecifyPlanet().addActionListener(evt -> {
            final boolean selected = getChkSpecifyPlanet().isSelected();
            getChkStartingSystemFactionSpecific().setEnabled(selected && getChkSpecifyFaction().isSelected());
            getComboStartingSystem().setEnabled(selected);
            getComboStartingPlanet().setEnabled(selected);
        });

        setChkStartingSystemFactionSpecific(new JCheckBox(resources.getString("FactionSpecific.text")));
        getChkStartingSystemFactionSpecific().setToolTipText(resources.getString("chkStartingSystemFactionSpecific.toolTipText"));
        getChkStartingSystemFactionSpecific().setName("chkStartingSystemFactionSpecific");
        getChkStartingSystemFactionSpecific().addActionListener(evt -> {
            final FactionDisplay factionDisplay = getComboFaction().getSelectedItem();
            final PlanetarySystem startingSystem = getComboStartingSystem().getSelectedItem();
            if ((factionDisplay == null) || (startingSystem == null)
                    || !startingSystem.getFactionSet(getCampaign().getLocalDate()).contains(factionDisplay.getFaction())) {
                restoreComboStartingSystem();
            }
        });

        setComboStartingSystem(new MMComboBox<>("comboStartingSystem"));
        getComboStartingSystem().setToolTipText(resources.getString("comboStartingSystem.toolTipText"));
        getComboStartingSystem().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlanetarySystem) {
                    setText(((PlanetarySystem) value).getName(getCampaign().getLocalDate()));
                }
                return this;
            }
        });
        getComboStartingSystem().addActionListener(evt -> {
            final PlanetarySystem startingSystem = getComboStartingSystem().getSelectedItem();
            final Planet startingPlanet = getComboStartingPlanet().getSelectedItem();
            if ((startingSystem == null)
                    || (startingPlanet != null) && !startingPlanet.getParentSystem().equals(startingSystem)) {
                restoreComboStartingPlanet();
            }
        });

        setComboStartingPlanet(new MMComboBox<>("comboStartingPlanet"));
        getComboStartingPlanet().setToolTipText(resources.getString("comboStartingPlanet.toolTipText"));
        getComboStartingPlanet().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Planet) {
                    setText(((Planet) value).getName(getCampaign().getLocalDate()));
                }
                return this;
            }
        });

        setChkSpecifyRankSystem(new JCheckBox(resources.getString("chkSpecifyRankSystem.text")));
        getChkSpecifyRankSystem().setToolTipText(resources.getString("chkSpecifyRankSystem.toolTipText"));
        getChkSpecifyRankSystem().setName("chkSpecifyRankSystem");
        getChkSpecifyRankSystem().addActionListener(evt -> getComboRankSystem().setEnabled(getChkSpecifyRankSystem().isSelected()));

        final Comparator<String> comparator = new NaturalOrderComparator();
        final SortedComboBoxModel<RankSystem> rankSystemModel = new SortedComboBoxModel<>(
                (systemA, systemB) -> comparator.compare(systemA.toString(), systemB.toString()));
        rankSystemModel.addAll(Ranks.getRankSystems().values());
        if ((getCampaign() != null) && getCampaign().getRankSystem().getType().isCampaign()) {
            rankSystemModel.addElement(getCampaign().getRankSystem());
        }
        setComboRankSystem(new MMComboBox<>("comboRankSystem", rankSystemModel));
        getComboRankSystem().setToolTipText(resources.getString("comboRankSystem.toolTipText"));
        getComboRankSystem().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RankSystem) {
                    list.setToolTipText(((RankSystem) value).getDescription());
                }
                return this;
            }
        });

        final JLabel lblContractCount = new JLabel(resources.getString("lblContractCount.text"));
        lblContractCount.setToolTipText(resources.getString("lblContractCount.toolTipText"));
        lblContractCount.setName("lblContractCount");

        setSpnContractCount(new JSpinner(new SpinnerNumberModel(2, 0, 100, 1)));
        getSpnContractCount().setToolTipText(resources.getString("lblContractCount.toolTipText"));
        getSpnContractCount().setName("spnContractCount");

        // Disable Panel Portions by Default
        getChkSpecifyDate().setSelected(true);
        getChkSpecifyDate().doClick();
        getChkSpecifyFaction().setSelected(true);
        getChkSpecifyFaction().doClick();
        getChkSpecifyPlanet().setSelected(true);
        getChkSpecifyPlanet().doClick();
        getChkSpecifyRankSystem().setSelected(true);
        getChkSpecifyRankSystem().doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(resources.getString("startupCampaignPresetPanel.title")));
        panel.setToolTipText(resources.getString("startupCampaignPresetPanel.toolTipText"));
        panel.setName("startupCampaignPresetPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkSpecifyDate())
                                .addComponent(btnDate, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkSpecifyFaction())
                                .addComponent(getComboFaction(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkSpecifyPlanet())
                                .addComponent(getChkStartingSystemFactionSpecific(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getComboStartingSystem())
                                .addComponent(getComboStartingPlanet(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkSpecifyRankSystem())
                                .addComponent(getComboRankSystem(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblContractCount)
                                .addComponent(getSpnContractCount(), GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkSpecifyDate())
                                .addComponent(btnDate))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkSpecifyFaction())
                                .addComponent(getComboFaction()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkSpecifyPlanet())
                                .addComponent(getChkStartingSystemFactionSpecific()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getComboStartingSystem())
                                .addComponent(getComboStartingPlanet()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkSpecifyRankSystem())
                                .addComponent(getComboRankSystem()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblContractCount)
                                .addComponent(getSpnContractCount()))
        );

        return panel;
    }

    private JPanel createContinuousPanel() {
        // Create Panel Components
        setChkSpecifyGameOptions(new JCheckBox(resources.getString("chkSpecifyGameOptions.text")));
        getChkSpecifyGameOptions().setToolTipText(resources.getString("chkSpecifyGameOptions.toolTipText"));
        getChkSpecifyGameOptions().setName("chkSpecifyGameOptions");
        getChkSpecifyRankSystem().addActionListener(evt -> getComboRankSystem().setEnabled(getChkSpecifyRankSystem().isSelected()));

        final JButton btnGameOptions = new MMButton("btnGameOptions", resources,
                "btnGameOptions.text", "btnGameOptions.toolTipText", evt -> {
            final GameOptionsDialog gameOptionsDialog = new GameOptionsDialog(getFrame(), getGameOptions(), false);
            gameOptionsDialog.refreshOptions();
            gameOptionsDialog.setEditable(false);
            gameOptionsDialog.setVisible(true);
        });

        setChkSpecifyCampaignOptions(new JCheckBox(resources.getString("chkSpecifyCampaignOptions.text")));
        getChkSpecifyCampaignOptions().setToolTipText(resources.getString("chkSpecifyCampaignOptions.toolTipText"));
        getChkSpecifyCampaignOptions().setName("chkSpecifyCampaignOptions");
        getChkSpecifyCampaignOptions().setSelected(true);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(resources.getString("continuousCampaignPresetPanel.title")));
        panel.setToolTipText(resources.getString("continuousCampaignPresetPanel.toolTipText"));
        panel.setName("continuousCampaignPresetPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getChkSpecifyGameOptions())
                                .addComponent(btnGameOptions, GroupLayout.Alignment.LEADING))
                        .addComponent(getChkSpecifyCampaignOptions())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(getChkSpecifyGameOptions())
                                .addComponent(btnGameOptions))
                        .addComponent(getChkSpecifyCampaignOptions())
        );

        return panel;
    }

    @Override
    protected void finalizeInitialization() {
        super.finalizeInitialization();
        getOkButton().setEnabled(false);
        restoreComboStartingSystem();
        final Faction faction = (getPreset() == null) || (getPreset().getFaction() == null)
                ? getCampaign().getFaction() : getPreset().getFaction();
        getComboFaction().setSelectedItem(new FactionDisplay(faction, getDate()));
        getComboStartingSystem().setSelectedItem((getPreset() == null) || (getPreset().getPlanet() == null)
                ? getCampaign().getLocation().getCurrentSystem() : getPreset().getPlanet().getParentSystem());
        getComboStartingPlanet().setSelectedItem((getPreset() == null) || (getPreset().getPlanet() == null)
                ? getCampaign().getLocation().getCurrentSystem().getPrimaryPlanet() : getPreset().getPlanet());
        getComboRankSystem().setSelectedItem((getPreset() == null) || (getPreset().getRankSystem() == null)
                ? getCampaign().getRankSystem() : getPreset().getRankSystem());
        if (getPreset() != null) {
            getSpnContractCount().setValue(getPreset().getContractCount());
        }
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JToggleButtonPreference(getChkSpecifyDate()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyFaction()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyPlanet()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyRankSystem()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnContractCount()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyGameOptions()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyCampaignOptions()));
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void okAction() {
        updatePreset();
    }

    @Override
    protected ValidationState validateAction(final boolean display) {
        if (getTxtPresetName().getText().isBlank()) {
            final String text = resources.getString("blankPresetName.text");
            if (display) {
                JOptionPane.showMessageDialog(getFrame(), text, resources.getString("ValidationFailure.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            getOkButton().setEnabled(false);
            getOkButton().setToolTipText(text);
            return ValidationState.FAILURE;
        }

        getOkButton().setToolTipText(null);
        getOkButton().setEnabled(true);

        if (getChkSpecifyFaction().isSelected() && (getComboFaction().getSelectedItem() == null)) {
            final String text = resources.getString("nullFactionSpecified.text");
            getOkButton().setToolTipText(text);
            if (display && JOptionPane.showConfirmDialog(getFrame(), text,
                    resources.getString("ValidationWarning.title"), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                getOkButton().setEnabled(false);
                return ValidationState.FAILURE;
            }
        }

        if (getChkSpecifyPlanet().isSelected()
                && (getComboStartingPlanet().getSelectedItem() == null)) {
            final String text = resources.getString("nullPlanetSpecified.text");
            getOkButton().setToolTipText(text);
            if (display && JOptionPane.showConfirmDialog(getFrame(), text,
                    resources.getString("ValidationWarning.title"), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                getOkButton().setEnabled(false);
                return ValidationState.FAILURE;
            }
        }

        if (getChkSpecifyRankSystem().isSelected()
                && (getComboRankSystem().getSelectedItem() == null)) {
            final String text = resources.getString("nullRankSystemSpecified.text");
            getOkButton().setToolTipText(text);
            if (display && JOptionPane.showConfirmDialog(getFrame(), text,
                    resources.getString("ValidationWarning.title"), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                getOkButton().setEnabled(false);
                return ValidationState.FAILURE;
            }
        }

        if (getOkButton().getToolTipText() != null) {
            return display ? ValidationState.WARNING : ValidationState.FAILURE;
        }

        getOkButton().setToolTipText(resources.getString("ValidationSuccess.text"));
        return ValidationState.SUCCESS;
    }
    //endregion Button Actions

    private PlanetarySystem[] getPlanetarySystems(final @Nullable FactionDisplay faction) {
        return getCampaign().getSystems().stream()
                .filter(p -> (faction == null) || p.getFactionSet(getCampaign().getLocalDate()).contains(faction.getFaction()))
                .sorted(Comparator.comparing(p -> p.getName(getCampaign().getLocalDate())))
                .collect(Collectors.toList()).toArray(new PlanetarySystem[]{});
    }

    public void updatePreset() {
        if (!getState().isSuccess()) {
            validateButtonActionPerformed(null);
        }

        if (getState().isSuccess() || getState().isWarning()) {
            if (getPreset() == null) {
                setPreset(new CampaignPreset());
            }

            getPreset().setTitle(getTxtPresetName().getText().trim());
            getPreset().setDescription(getTxtPresetDescription().getText().trim());
            if (getChkSpecifyDate().isSelected()) {
                getPreset().setDate(getDate());
            }

            if (getChkSpecifyFaction().isSelected()) {
                final FactionDisplay factionDisplay = getComboFaction().getSelectedItem();
                getPreset().setFaction((factionDisplay == null) ? null : factionDisplay.getFaction());
            }

            if (getChkSpecifyPlanet().isSelected()) {
                getPreset().setPlanet(getComboStartingPlanet().getSelectedItem());
            }

            if (getChkSpecifyRankSystem().isSelected()) {
                getPreset().setRankSystem(getComboRankSystem().getSelectedItem());
            }
            getPreset().setContractCount((int) getSpnContractCount().getValue());
            if (getChkSpecifyGameOptions().isSelected()) {
                getPreset().setGameOptions(getGameOptions());
            }

            if (getChkSpecifyCampaignOptions().isSelected()) {
                getPreset().setCampaignOptions(getCampaignOptions());
                getPreset().setRandomSkillPreferences(getRandomSkillPreferences());
                getPreset().setSkills(getSkills());
                getPreset().setSpecialAbilities(getSpecialAbilities());
            }
        }
    }
}
