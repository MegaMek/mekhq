/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.dialogs.buttonDialogs.GameOptionsDialog;
import megamek.client.ui.enums.ValidationState;
import megamek.client.ui.preferences.JIntNumberSpinnerPreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.options.GameOptions;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.CampaignPreset;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.baseComponents.SortedComboBoxModel;
import mekhq.gui.dialog.CompanyGenerationOptionsDialog;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.displayWrappers.FactionDisplay;

/**
 * @author Justin "Windchild" Bowen
 */
public class CreateCampaignPreset extends AbstractMHQValidationButtonDialog {
    private static final int DESCRIPTION_ROWS = 3;
    private static final int DESCRIPTION_COLUMNS = 40;

    //region Variable Declarations
    private final Campaign campaign;
    private @Nullable CampaignPreset preset;

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
    private JCheckBox chkGM;
    private JCheckBox chkSpecifyCompanyGenerationOptions;
    private @Nullable CompanyGenerationOptions companyGenerationOptions;
    //endregion Startup

    //region Continuous
    private JCheckBox chkSpecifyGameOptions;
    private final GameOptions gameOptions;
    private JCheckBox chkSpecifyCampaignOptions;
    private final CampaignOptions campaignOptions;
    private final RandomSkillPreferences randomSkillPreferences;
    private final Map<String, SkillType> skills;
    private final Map<String, SpecialAbility> specialAbilities;
    //endregion Continuous
    //endregion Variable Declarations

    //region Constructors
    public CreateCampaignPreset(final JFrame frame, final @Nonnull Campaign campaign,
          final @Nullable CampaignPreset preset) {
        super(frame, "CreateCampaignPresetDialog", "CreateCampaignPresetDialog.title");
        this.campaign = campaign;
        setPreset(preset);
        setDate(campaign.getLocalDate());
        setCompanyGenerationOptions(null);
        this.gameOptions = ((preset == null) || (preset.getGameOptions() == null))
                                 ? campaign.getGameOptions() : preset.getGameOptions();
        this.campaignOptions = ((preset == null) || (preset.getCampaignOptions() == null))
                                     ? campaign.getCampaignOptions() : preset.getCampaignOptions();
        this.randomSkillPreferences = ((preset == null) || (preset.getRandomSkillPreferences() == null))
                                            ? campaign.getRandomSkillPreferences() : preset.getRandomSkillPreferences();
        this.skills = ((preset == null) || preset.getSkills().isEmpty())
                            ? SkillType.getSkillHash() : preset.getSkills();
        this.specialAbilities = ((preset == null) || preset.getSpecialAbilities().isEmpty())
                                      ? SpecialAbility.getSpecialAbilities() : preset.getSpecialAbilities();
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public @Nonnull Campaign getCampaign() {
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

    private void restoreComboStartingPlanet() {
        final PlanetarySystem system = getComboStartingSystem().getSelectedItem();
        if (system != null) {
            getComboStartingPlanet().setModel(new DefaultComboBoxModel<>(
                  system.getPlanets().toArray(new Planet[] {})));
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

    public JCheckBox getChkGM() {
        return chkGM;
    }

    public void setChkGM(final JCheckBox chkGM) {
        this.chkGM = chkGM;
    }

    public JCheckBox getChkSpecifyCompanyGenerationOptions() {
        return chkSpecifyCompanyGenerationOptions;
    }

    public void setChkSpecifyCompanyGenerationOptions(final JCheckBox chkSpecifyCompanyGenerationOptions) {
        this.chkSpecifyCompanyGenerationOptions = chkSpecifyCompanyGenerationOptions;
    }

    public @Nullable CompanyGenerationOptions getCompanyGenerationOptions() {
        return companyGenerationOptions;
    }

    public void setCompanyGenerationOptions(final @Nullable CompanyGenerationOptions companyGenerationOptions) {
        this.companyGenerationOptions = companyGenerationOptions;
    }
    //endregion Startup

    //region Continuous
    public JCheckBox getChkSpecifyGameOptions() {
        return chkSpecifyGameOptions;
    }

    public void setChkSpecifyGameOptions(final JCheckBox chkSpecifyGameOptions) {
        this.chkSpecifyGameOptions = chkSpecifyGameOptions;
    }

    public @Nonnull GameOptions getGameOptions() {
        return gameOptions;
    }

    public JCheckBox getChkSpecifyCampaignOptions() {
        return chkSpecifyCampaignOptions;
    }

    public void setChkSpecifyCampaignOptions(final JCheckBox chkSpecifyCampaignOptions) {
        this.chkSpecifyCampaignOptions = chkSpecifyCampaignOptions;
    }

    public @Nonnull CampaignOptions getCampaignOptions() {
        return campaignOptions;
    }

    public @Nonnull RandomSkillPreferences getRandomSkillPreferences() {
        return randomSkillPreferences;
    }

    public @Nonnull Map<String, SkillType> getSkills() {
        return skills;
    }

    public @Nonnull Map<String, SpecialAbility> getSpecialAbilities() {
        return specialAbilities;
    }
    //endregion Continuous
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected @Nonnull Container createCenterPane() {
        final int padding = UIUtil.scaleForGUI(5);

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("createCampaignPresetPanel");

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(padding, padding, padding, padding);
        panel.add(createDetailsPanel(), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createStartupPanel(), gbc);

        gbc.gridy++;
        panel.add(createContinuousPanel(), gbc);

        // Soak up any extra vertical space below the sections (for example when a
        // remembered window size is taller than
        // the packed size) so the fixed-height description box does not stretch.
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createDetailsPanel() {
        setTxtPresetName(new JTextField());
        getTxtPresetName().setToolTipText(resources.getString("txtPresetName.toolTipText"));
        getTxtPresetName().setName("txtPresetName");
        getTxtPresetName().putClientProperty("JTextField.placeholderText",
                resources.getString("txtPresetName.text"));
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

        setTxtPresetDescription(new JTextArea(DESCRIPTION_ROWS, DESCRIPTION_COLUMNS));
        getTxtPresetDescription().setToolTipText(resources.getString("txtPresetDescription.toolTipText"));
        getTxtPresetDescription().setName("txtPresetDescription");
        getTxtPresetDescription().setLineWrap(true);
        getTxtPresetDescription().setWrapStyleWord(true);
        getTxtPresetDescription().putClientProperty("JTextField.placeholderText",
                resources.getString("txtPresetDescription.text"));

        final JScrollPane descriptionScrollPane = new JScrollPane(getTxtPresetDescription());
        descriptionScrollPane.setName("txtPresetDescriptionScrollPane");
        // Pin the minimum to the 3-row preferred size so a remembered (shorter) window
        // size can't squash the box below
        // three lines; the trailing glue in the center pane absorbs any extra height
        // instead.
        descriptionScrollPane.setMinimumSize(descriptionScrollPane.getPreferredSize());

        final int padding = UIUtil.scaleForGUI(5);
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(resources.getString("detailsCampaignPresetPanel.title")));
        panel.setToolTipText(resources.getString("detailsCampaignPresetPanel.toolTipText"));
        panel.setName("detailsCampaignPresetPanel");

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(padding, padding, padding, padding);
        panel.add(getTxtPresetName(), gbc);

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        // Give the description a larger bottom inset so it isn't crammed against the
        // Preset Details border.
        gbc.insets = new Insets(padding, padding, UIUtil.scaleForGUI(12), padding);
        panel.add(descriptionScrollPane, gbc);

        return panel;
    }

    private JPanel createStartupPanel() {
        // Initialize Components Used in ActionListeners
        final JButton btnDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(getDate()));

        // Create Panel Components
        setChkSpecifyDate(new JCheckBox(resources.getString("chkSpecifyDate.text")));
        getChkSpecifyDate().setToolTipText(String.format(resources.getString("chkSpecifyDate.toolTipText"),
              MekHQ.getMHQOptions().getDisplayFormattedDate(LocalDate.ofYearDay(3067, 1))));
        getChkSpecifyDate().setName("chkSpecifyDate");
        getChkSpecifyDate().addActionListener(evt -> btnDate.setEnabled(getChkSpecifyDate().isSelected()));

        btnDate.setToolTipText(resources.getString("btnDate.toolTipText"));
        btnDate.setName("btnDate");
        btnDate.addActionListener(evt -> {
            final DateChooser dateChooser = new DateChooser(getFrame(), getDate());
            if (dateChooser.showDateChooser() == DateChooser.OK_OPTION) {
                setDate(dateChooser.getDate());
                btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(getDate()));
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
        getChkStartingSystemFactionSpecific().setToolTipText(resources.getString(
              "chkStartingSystemFactionSpecific.toolTipText"));
        getChkStartingSystemFactionSpecific().setName("chkStartingSystemFactionSpecific");
        getChkStartingSystemFactionSpecific().addActionListener(evt -> {
            final FactionDisplay factionDisplay = getComboFaction().getSelectedItem();
            final PlanetarySystem startingSystem = getComboStartingSystem().getSelectedItem();
            if ((factionDisplay == null) ||
                      (startingSystem == null)
                      ||
                      !startingSystem.getFactionSet(getCampaign().getLocalDate())
                             .contains(factionDisplay.getFaction())) {
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

        setChkGM(new JCheckBox(resources.getString("chkGM.text")));
        getChkGM().setToolTipText(resources.getString("chkGM.toolTipText"));
        getChkGM().setName("chkGM");

        setChkSpecifyCompanyGenerationOptions(new JCheckBox(resources.getString(
              "chkSpecifyCompanyGenerationOptions.text")));
        getChkSpecifyCompanyGenerationOptions().setToolTipText(resources.getString(
              "chkSpecifyCompanyGenerationOptions.toolTipText"));
        getChkSpecifyCompanyGenerationOptions().setName("chkSpecifyCompanyGenerationOptions");

        final JButton btnCompanyGenerationOptions = new JButton(resources.getString(
              "btnCompanyGenerationOptions.text"));
        btnCompanyGenerationOptions.setName("btnCompanyGenerationOptions");
        btnCompanyGenerationOptions.setToolTipText(resources.getString("btnCompanyGenerationOptions.toolTipText"));
        btnCompanyGenerationOptions.addActionListener(evt -> setCompanyGenerationOptions(new CompanyGenerationOptionsDialog(
              getFrame(),
              getCampaign(),
              getCompanyGenerationOptions()).getSelectedItem()));

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
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(resources.getString("startupCampaignPresetPanel.title")));
        panel.setToolTipText(resources.getString("startupCampaignPresetPanel.toolTipText"));
        panel.setName("startupCampaignPresetPanel");

        final int gap = UIUtil.scaleForGUI(4);
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(gap, gap, gap, gap);
        gbc.anchor = GridBagConstraints.WEST;

        // Specify Starting Date
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(getChkSpecifyDate(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(btnDate, gbc);

        // Specify Starting Faction
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(getChkSpecifyFaction(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(getComboFaction(), gbc);

        // Specify Starting Planet
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(getChkSpecifyPlanet(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(getChkStartingSystemFactionSpecific(), gbc);

        // Starting System / Planet
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(getComboStartingSystem(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(getComboStartingPlanet(), gbc);

        // Specify Rank System
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(getChkSpecifyRankSystem(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(getComboRankSystem(), gbc);

        // Starting Contract Count
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(lblContractCount, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(getSpnContractCount(), gbc);

        // Start as GM
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(getChkGM(), gbc);
        gbc.gridwidth = 1;

        // Specify Company Generation Options
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0.0;
        panel.add(getChkSpecifyCompanyGenerationOptions(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(btnCompanyGenerationOptions, gbc);

        return panel;
    }

    private JPanel createContinuousPanel() {
        // Create Panel Components
        setChkSpecifyGameOptions(new JCheckBox(resources.getString("chkSpecifyGameOptions.text")));
        getChkSpecifyGameOptions().setToolTipText(resources.getString("chkSpecifyGameOptions.toolTipText"));
        getChkSpecifyGameOptions().setName("chkSpecifyGameOptions");

        final JButton btnGameOptions = new JButton(resources.getString("btnGameOptions.text"));
        btnGameOptions.setName("btnGameOptions");
        btnGameOptions.setToolTipText(resources.getString("btnGameOptions.toolTipText"));
        btnGameOptions.addActionListener(evt -> {
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
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(resources.getString("continuousCampaignPresetPanel.title")));
        panel.setToolTipText(resources.getString("continuousCampaignPresetPanel.toolTipText"));
        panel.setName("continuousCampaignPresetPanel");

        final int gap = UIUtil.scaleForGUI(4);
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(gap, gap, gap, gap);
        gbc.anchor = GridBagConstraints.WEST;

        // Specify MegaMek Game Options
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(getChkSpecifyGameOptions(), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(btnGameOptions, gbc);

        // Specify Campaign Options
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(getChkSpecifyCampaignOptions(), gbc);
        gbc.gridwidth = 1;

        // Reserve the same first-column width as the Startup panel (whose column 0 is
        // driven by its longest checkbox)
        // so the Game Options button lines up with the Company Generation Options
        // button above it. The Startup panel is
        // built first, so this measures that checkbox's actual (font-scaled) rendered
        // width rather than guessing.
        final int startupFirstColumnWidth = getChkSpecifyCompanyGenerationOptions().getPreferredSize().width;
        final GridBagConstraints strutLayout = new GridBagConstraints();
        strutLayout.gridx = 0;
        strutLayout.gridy = gbc.gridy + 1;
        strutLayout.insets = new Insets(0, gap, 0, gap);
        panel.add(Box.createHorizontalStrut(startupFirstColumnWidth), strutLayout);

        return panel;
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        getOkButton().setEnabled(false);
        getRootPane().setDefaultButton(getOkButton());
        restoreComboStartingSystem();
        final Faction faction = (getPreset() == null) || (getPreset().getFaction() == null)
                                      ? getCampaign().getFaction() : getPreset().getFaction();
        getComboFaction().setSelectedItem(new FactionDisplay(faction, getDate()));
        getComboStartingSystem().setSelectedItem((getPreset() == null) || (getPreset().getPlanet() == null)
                                                       ?
                                                       getCampaign().getCurrentLocation().getCurrentSystem() :
                                                       getPreset().getPlanet().getParentSystem());
        getComboStartingPlanet().setSelectedItem((getPreset() == null) || (getPreset().getPlanet() == null)
                                                       ?
                                                       getCampaign().getCurrentLocation()
                                                             .getCurrentSystem()
                                                             .getPrimaryPlanet() :
                                                       getPreset().getPlanet());
        getComboRankSystem().setSelectedItem((getPreset() == null) || (getPreset().getRankSystem() == null)
                                                   ? getCampaign().getRankSystem() : getPreset().getRankSystem());
        if (getPreset() != null) {
            getSpnContractCount().setValue(getPreset().getContractCount());
        }
        getChkGM().setSelected((getPreset() == null) ? getCampaign().isGM() : getPreset().isGM());

        // Size to the preferred (layout) height so the buttons sit directly under the
        // content, with no band of empty
        // space above them. The height is set explicitly rather than via
        // max(remembered, preferred) because the
        // remembered height is restored inside super.finalizeInitialization() and could
        // be a stale larger value; the
        // content is a fixed height, so there is no reason to keep a taller window. A
        // remembered wider width is kept.
        // The minimum prevents the bottom border from being clipped.
        final Dimension preferredSize = getPreferredSize();
        setMinimumSize(preferredSize);
        setSize(Math.max(getWidth(), preferredSize.width), preferredSize.height);
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        preferences.manage(new JToggleButtonPreference(getChkSpecifyDate()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyFaction()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyPlanet()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyRankSystem()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnContractCount()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyCompanyGenerationOptions()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyGameOptions()));
        preferences.manage(new JToggleButtonPreference(getChkSpecifyCampaignOptions()));
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected @Nonnull JPanel createButtonPanel() {
        final int gap = UIUtil.scaleForGUI(8);
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, gap, gap));

        final JButton validateButton = new JButton(resources.getString("Validate.text"));
        validateButton.setName("validateButton");
        validateButton.setToolTipText(resources.getString("Validate.toolTipText"));
        validateButton.addActionListener(this::validateButtonActionPerformed);
        panel.add(validateButton);

        setOkButton(new JButton(resources.getString("Ok.text")));
        getOkButton().setName("okButton");
        getOkButton().setToolTipText(resources.getString("Ok.toolTipText"));
        getOkButton().addActionListener(this::okButtonActionPerformed);
        panel.add(getOkButton());

        final JButton cancelButton = new JButton(resources.getString("Cancel.text"));
        cancelButton.setName("cancelButton");
        cancelButton.setToolTipText(resources.getString("Cancel.toolTipText"));
        cancelButton.addActionListener(this::cancelActionPerformed);
        panel.add(cancelButton);

        return panel;
    }

    @Override
    protected void okAction() {
        if (!getState().isSuccess()) {
            validateButtonActionPerformed(null);
        }

        if (getState().isSuccess() || getState().isWarning()) {
            setPreset(new CampaignPreset());

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
            getPreset().setGM(getChkGM().isSelected());
            if (getChkSpecifyCompanyGenerationOptions().isSelected()) {
                getPreset().setCompanyGenerationOptions(getCompanyGenerationOptions());
            }

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

    @Override
    protected @Nonnull ValidationState validateAction(final boolean display) {
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
                  resources.getString("ValidationWarning.title"), JOptionPane.OK_CANCEL_OPTION) !=
                                 JOptionPane.OK_OPTION) {
                getOkButton().setEnabled(false);
                return ValidationState.FAILURE;
            }
        }

        if (getChkSpecifyPlanet().isSelected()
                  && (getComboStartingPlanet().getSelectedItem() == null)) {
            final String text = resources.getString("nullPlanetSpecified.text");
            getOkButton().setToolTipText(text);
            if (display && JOptionPane.showConfirmDialog(getFrame(), text,
                  resources.getString("ValidationWarning.title"), JOptionPane.OK_CANCEL_OPTION) !=
                                 JOptionPane.OK_OPTION) {
                getOkButton().setEnabled(false);
                return ValidationState.FAILURE;
            }
        }

        if (getChkSpecifyRankSystem().isSelected()
                  && (getComboRankSystem().getSelectedItem() == null)) {
            final String text = resources.getString("nullRankSystemSpecified.text");
            getOkButton().setToolTipText(text);
            if (display && JOptionPane.showConfirmDialog(getFrame(), text,
                  resources.getString("ValidationWarning.title"), JOptionPane.OK_CANCEL_OPTION) !=
                                 JOptionPane.OK_OPTION) {
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
                     .filter(p -> (faction == null) ||
                                        p.getFactionSet(getCampaign().getLocalDate()).contains(faction.getFaction()))
                     .sorted(Comparator.comparing(p -> p.getName(getCampaign().getLocalDate())))
                     .toList().toArray(new PlanetarySystem[] {});
    }
}
