/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.sendTipToDetailsPanel;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@link SalariesTab} class represents the user interface components for configuring salary-related options in the
 * MekHQ Campaign Options dialog. This class handles the initialization, layout, and logic for various salary settings
 * spanning multiple tabs.
 */
public class SalariesTab {
    private static final int FORM_LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    // Wider than the default control column so the salary-amount spinners have room for large figures.
    private static final int FORM_CONTROL_COLUMN_WIDTH = 300;
    private static final int FORM_LABEL_CONTROL_GAP = 12;
    private static final int SECTION_CONTENT_WIDTH = FORM_LABEL_COLUMN_WIDTH + FORM_LABEL_CONTROL_GAP
          + FORM_CONTROL_COLUMN_WIDTH;
    private static final int GRID_CONTROL_COLUMN_WIDTH = 100;
    private static final int CIVILIAN_TABLE_WIDTH = SECTION_CONTENT_WIDTH;
    private static final int CIVILIAN_SALARY_COLUMN_WIDTH = 120;
    private static final int CIVILIAN_ROLE_COLUMN_WIDTH = CIVILIAN_TABLE_WIDTH - CIVILIAN_SALARY_COLUMN_WIDTH;

    private final CampaignOptions campaignOptions;
    private SalariesOptionsModel model;
    private boolean combatPageCreated;
    private boolean supportPageCreated;
    private boolean civilianPageCreated;

    //start Combat Salaries Tab
    private CampaignOptionsHeaderPanel salariesHeader;
    private JCheckBox chkDisableSecondaryRoleSalary;

    private JLabel lblAntiMekSalary;
    private JSpinner spnAntiMekSalary;
    private JLabel lblSpecialistInfantrySalary;
    private JSpinner spnSpecialistInfantrySalary;

    private Map<SkillLevel, JLabel> lblSalaryExperienceMultipliers;
    private Map<SkillLevel, JSpinner> spnSalaryExperienceMultipliers;

    private List<PersonnelRole> combatRoles;
    private JLabel[] lblBaseSalaryCombat;
    private JSpinner[] spnBaseSalaryCombat;

    private List<PersonnelRole> supportRoles;
    private JLabel[] lblBaseSalarySupport;
    private JSpinner[] spnBaseSalarySupport;

    private List<PersonnelRole> civilianRoles;
    private SalaryTableModel civilianSalaryTableModel;
    private JTable civilianSalaryTable;
    private JScrollPane civilianSalaryTableScrollPane;
    //end Salaries Tab

    /**
     * Constructs the {@code PersonnelTab} object with the given campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} instance to be used for initializing and managing personnel
     *                        options.
     */
    public SalariesTab(@Nonnull CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Initializes all tabs and their components within the PersonnelTab.
     */
    private void initialize() {
        chkDisableSecondaryRoleSalary = new JCheckBox();

        lblAntiMekSalary = new JLabel();
        spnAntiMekSalary = new JSpinner();

        lblSpecialistInfantrySalary = new JLabel();
        spnSpecialistInfantrySalary = new JSpinner();

        lblSalaryExperienceMultipliers = new HashMap<>();
        spnSalaryExperienceMultipliers = new HashMap<>();

        combatRoles = PersonnelRole.getCombatRoles();
        combatRoles.sort(Comparator.comparing(role -> role.getLabel(false)));
        lblBaseSalaryCombat = new JLabel[combatRoles.size()];
        spnBaseSalaryCombat = new JSpinner[combatRoles.size()];

        supportRoles = PersonnelRole.getSupportRoles();
        supportRoles.sort(Comparator.comparing(role -> role.getLabel(false)));
        lblBaseSalarySupport = new JLabel[supportRoles.size()];
        spnBaseSalarySupport = new JSpinner[supportRoles.size()];

        civilianRoles = PersonnelRole.getCivilianRoles();
        civilianRoles.sort(Comparator.comparing(role -> role.getLabel(false)));
        civilianRoles.remove(PersonnelRole.NONE);
        civilianRoles.addFirst(PersonnelRole.NONE);
        civilianRoles.remove(PersonnelRole.DEPENDENT);
        civilianRoles.addFirst(PersonnelRole.DEPENDENT);
    }

    /**
     * Creates the layout for the Salaries Tab, including components for salary multipliers and base salary settings.
     *
     * @return a {@link JPanel} representing the Salaries Tab.
     */
    public @Nonnull JPanel createSalariesTab(PersonnelRoleSubType type) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_coyote.png";
        String headerName = getHeaderName(type);
        salariesHeader = new CampaignOptionsHeaderPanel(headerName, imageAddress);
        CampaignOptionsPagePanel.Builder builder = CampaignOptionsPagePanel.builder(headerName, headerName, imageAddress)
            .header(salariesHeader)
            .quote(getQuoteResourceName(type));

        if (type == PersonnelRoleSubType.COMBAT) {
            chkDisableSecondaryRoleSalary = new CampaignOptionsCheckBox("DisableSecondaryRoleSalary",
                getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM));
            chkDisableSecondaryRoleSalary.addMouseListener(createTipPanelUpdater(salariesHeader,
                "DisableSecondaryRoleSalary"));

            builder.section("lblSalaryRulesPanel.text", "lblSalaryRulesPanel.summary", createSalaryRulesPanel())
                    .section("lblSalaryMultipliersPanel.text",
                            "lblSalaryMultipliersPanel.summary",
                            createSalaryMultipliersPanel())
                    .section("lblExperienceMultipliersPanel.text",
                            "lblExperienceMultipliersPanel.summary",
                            createExperienceMultipliersPanel());
        }

        JPanel panel = builder.section("lblBaseSalariesPanel.text",
                getBaseSalariesSummaryKey(type),
                createBaseSalariesPanel(type))
                .build();

        markPageCreated(type);
        updateSalariesControlsFromModel(type);

        return panel;
    }

    private String getHeaderName(PersonnelRoleSubType type) {
        return switch (type) {
            case COMBAT -> "CombatSalariesTab";
            case SUPPORT -> "SupportSalariesTab";
            case CIVILIAN -> "CivilianSalariesTab";
        };
    }

    private String getQuoteResourceName(PersonnelRoleSubType type) {
        return switch (type) {
            case COMBAT -> "0combatSalariesTab";
            case SUPPORT -> "1supportSalariesTab";
            case CIVILIAN -> "2civilianSalariesTab";
        };
    }

    private String getBaseSalariesSummaryKey(PersonnelRoleSubType type) {
        return switch (type) {
            case COMBAT -> "lblBaseSalariesPanel.summary.combat";
            case SUPPORT -> "lblBaseSalariesPanel.summary.support";
            case CIVILIAN -> "lblBaseSalariesPanel.summary.civilian";
        };
    }

    private @Nonnull JPanel createSalaryRulesPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("SalaryRulesPanel",
              FORM_LABEL_COLUMN_WIDTH,
              FORM_CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkDisableSecondaryRoleSalary);

        return panel;
    }

    /**
     * Creates the panel for configuring salary multipliers for specific roles in the Salaries Tab.
     *
     * @return a {@link JPanel} containing salary multiplier options.
     */
    private @Nonnull JPanel createSalaryMultipliersPanel() {
        // Contents
        lblAntiMekSalary = new CampaignOptionsLabel("AntiMekSalary");
        lblAntiMekSalary.addMouseListener(createTipPanelUpdater(salariesHeader, "AntiMekSalary"));
        spnAntiMekSalary = new CampaignOptionsSpinner("AntiMekSalary", 0, 0, 100, 0.01);
        spnAntiMekSalary.addMouseListener(createTipPanelUpdater(salariesHeader, "AntiMekSalary"));

        lblSpecialistInfantrySalary = new CampaignOptionsLabel("SpecialistInfantrySalary");
        lblSpecialistInfantrySalary.addMouseListener(createTipPanelUpdater(salariesHeader,
              "SpecialistInfantrySalary"));
        spnSpecialistInfantrySalary = new CampaignOptionsSpinner("SpecialistInfantrySalary", 0, 0, 100, 0.01);
        spnSpecialistInfantrySalary.addMouseListener(createTipPanelUpdater(salariesHeader,
              "SpecialistInfantrySalary"));

        // Layout the Panel
        JComponent[] labels = { lblAntiMekSalary, lblSpecialistInfantrySalary };
        JComponent[] controls = { spnAntiMekSalary, spnSpecialistInfantrySalary };

        return createPairedFieldGridPanel("SalaryMultipliersPanel",
              labels,
              controls,
              2,
              GRID_CONTROL_COLUMN_WIDTH);
    }

    /**
     * Creates the panel for configuring experience multipliers based on skill levels in the Salaries Tab.
     *
     * @return a {@link JPanel} containing settings for skill-based experience multipliers.
     */
    private @Nonnull JPanel createExperienceMultipliersPanel() {
        // Contents
        SkillLevel[] skillLevels = Skills.SKILL_LEVELS;

        for (final SkillLevel skillLevel : skillLevels) {
            final JLabel label = new CampaignOptionsLabel("SkillLevel" + skillLevel.toString(), null, true);
            label.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lblSkillLevelMultiplier.tooltip"));
            label.addMouseListener(createTipPanelUpdater(salariesHeader, "SkillLevelMultiplier"));
            lblSalaryExperienceMultipliers.put(skillLevel, label);

            final JSpinner spinner = new CampaignOptionsSpinner("SkillLevel" + skillLevel, null, 0, 0, 100, 0.1, true);
            spinner.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lblSkillLevelMultiplier.tooltip"));
            spinner.addMouseListener(createTipPanelUpdater(salariesHeader, "SkillLevelMultiplier"));
            spnSalaryExperienceMultipliers.put(skillLevel, spinner);
        }

        // Layout the Panel
        JComponent[] labels = new JComponent[skillLevels.length];
        JComponent[] controls = new JComponent[skillLevels.length];
        for (int index = 0; index < skillLevels.length; index++) {
            labels[index] = lblSalaryExperienceMultipliers.get(skillLevels[index]);
            controls[index] = spnSalaryExperienceMultipliers.get(skillLevels[index]);
        }

        final JPanel panel = createPairedFieldGridPanel("ExperienceMultipliersPanel",
              labels,
              controls,
              2,
              GRID_CONTROL_COLUMN_WIDTH);

        return panel;
    }

    /**
     * Creates the panel for configuring base salaries for various personnel roles in the Salaries Tab.
     *
     * @return a {@link JPanel} containing settings for base salaries.
     */
    private @Nonnull JPanel createBaseSalariesPanel(PersonnelRoleSubType type) {
        if (type == PersonnelRoleSubType.CIVILIAN) {
            return createCivilianBaseSalariesPanel();
        }

        List<PersonnelRole> roles = switch (type) {
            case COMBAT -> combatRoles;
            case SUPPORT -> supportRoles;
            case CIVILIAN -> throw new IllegalArgumentException("Civilian salaries use a table model.");
        };
        JLabel[] trackingLabel = switch (type) {
            case COMBAT -> lblBaseSalaryCombat;
            case SUPPORT -> lblBaseSalarySupport;
            case CIVILIAN -> throw new IllegalArgumentException("Civilian salaries use a table model.");
        };
        JSpinner[] trackingSpinner = switch (type) {
            case COMBAT -> spnBaseSalaryCombat;
            case SUPPORT -> spnBaseSalarySupport;
            case CIVILIAN -> throw new IllegalArgumentException("Civilian salaries use a table model.");
        };

        // Contents
        for (int index = 0; index < roles.size(); index++) {
            PersonnelRole personnelRole = roles.get(index);
            trackingLabel[index] = createRoleSalaryLabel(personnelRole);
            trackingSpinner[index] = createRoleSalarySpinner(personnelRole);
        }

        // Layout the Panel
        return createPairedFieldGridPanel("BaseSalariesPanel",
              trackingLabel,
              trackingSpinner,
              getBaseSalaryColumnCount(type),
              GRID_CONTROL_COLUMN_WIDTH);
    }

    private int getBaseSalaryColumnCount(PersonnelRoleSubType type) {
        return switch (type) {
            case COMBAT -> 2;
            case SUPPORT -> 2;
            case CIVILIAN -> throw new IllegalArgumentException("Civilian salaries use a table model.");
        };
    }

    private JLabel createRoleSalaryLabel(PersonnelRole personnelRole) {
        String componentName = personnelRole.toString().replace(" ", "");
        JLabel label = new JLabel(personnelRole.toString());
        label.setToolTipText(personnelRole.getDescription(false));
        label.addMouseListener(createTipPanelUpdater(salariesHeader, null, personnelRole.getDescription(false)));
        label.setName("lbl" + componentName);

        return label;
    }

    private JSpinner createRoleSalarySpinner(PersonnelRole personnelRole) {
        String componentName = personnelRole.toString().replace(" ", "");
        JSpinner spinner = new JSpinner();
        spinner.setToolTipText(personnelRole.getDescription(false));
        spinner.addMouseListener(createTipPanelUpdater(salariesHeader, null, personnelRole.getDescription(false)));
        spinner.setModel(new SpinnerNumberModel(250.0, 0.0, 1000000, 10.0));
        spinner.setName("spn" + componentName);

        DefaultEditor editor = (DefaultEditor) spinner.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        CampaignOptionsSpinner.installSelectAllOnFocus(spinner);

        return spinner;
    }

    private @Nonnull JPanel createPairedFieldGridPanel(String name, JComponent[] labels, JComponent[] controls,
            int columnCount, int controlWidth) {
        CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
              FORM_LABEL_COLUMN_WIDTH + FORM_LABEL_CONTROL_GAP,
              FORM_CONTROL_COLUMN_WIDTH,
              controlWidth,
              columnCount);
        panel.addPairs(labels, controls);
          return panel;
    }

    private @Nonnull JPanel createCivilianBaseSalariesPanel() {
        civilianSalaryTableModel = new SalaryTableModel(civilianRoles);
        if (model != null) {
            civilianSalaryTableModel.setValues(model.roleBaseSalaries);
        }

        TableRowSorter<SalaryTableModel> sorter = new TableRowSorter<>(civilianSalaryTableModel);
        sorter.setComparator(SalaryTableModel.SALARY_COLUMN,
              Comparator.comparingDouble(value -> ((Number) value).doubleValue()));

        civilianSalaryTable = createCivilianSalaryTable(civilianSalaryTableModel, sorter);
        civilianSalaryTableScrollPane = createCivilianSalaryTableScrollPane(civilianSalaryTable);
        updateCivilianSalaryTableHeight();

        JPanel panel = new CampaignOptionsStandardPanel("BaseSalariesPanel");
        panel.setLayout(new BorderLayout(0, UIUtil.scaleForGUI(8)));
        panel.add(createCivilianSalaryFilterPanel(sorter), BorderLayout.NORTH);
        panel.add(civilianSalaryTableScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JTable createCivilianSalaryTable(SalaryTableModel tableModel, TableRowSorter<SalaryTableModel> sorter) {
        JTable table = new JTable(tableModel);
        table.setName("tblCivilianBaseSalaries");
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setShowVerticalLines(false);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        // Surface the hovered role's profession description in the shared "Option Details" panel rather than as a hover
        // tooltip, so the full description is comfortably readable. Tracking the last row keeps it from re-pushing the
        // same text on every mouse move within a row.
        table.addMouseMotionListener(new MouseMotionAdapter() {
            private int lastModelRow = -1;

            @Override
            public void mouseMoved(MouseEvent event) {
                int viewRow = table.rowAtPoint(event.getPoint());
                if (viewRow < 0) {
                    lastModelRow = -1;
                    return;
                }

                int modelRow = table.convertRowIndexToModel(viewRow);
                if (modelRow == lastModelRow) {
                    return;
                }

                lastModelRow = modelRow;
                sendTipToDetailsPanel(tableModel.getRole(modelRow).getDescription(false));
            }
        });

        table.getColumnModel().getColumn(SalaryTableModel.ROLE_COLUMN)
              .setPreferredWidth(UIUtil.scaleForGUI(CIVILIAN_ROLE_COLUMN_WIDTH));
        table.getColumnModel().getColumn(SalaryTableModel.SALARY_COLUMN)
              .setPreferredWidth(UIUtil.scaleForGUI(CIVILIAN_SALARY_COLUMN_WIDTH));
        table.getColumnModel().getColumn(SalaryTableModel.SALARY_COLUMN).setCellRenderer(createSalaryCellRenderer());

        return table;
    }

    private DefaultTableCellRenderer createSalaryCellRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        return renderer;
    }

    private JScrollPane createCivilianSalaryTableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setName("scrCivilianBaseSalaries");
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        // The table is sized to show every row, so this inner scroll pane never scrolls. Left alone it still receives
        // (and silently drops) mouse-wheel events while the cursor is over the table, preventing the surrounding
        // Campaign Options page from scrolling. Forward wheel events to the nearest ancestor scroll pane so the page
        // scrolls normally even when hovering the table.
        scrollPane.addMouseWheelListener(event -> {
            JScrollPane parentScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class,
                  scrollPane);
            if (parentScrollPane != null) {
                parentScrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(scrollPane, event, parentScrollPane));
            }
        });

        return scrollPane;
    }

    private void updateCivilianSalaryTableHeight() {
        if (civilianSalaryTable == null || civilianSalaryTableScrollPane == null) {
            return;
        }

        int rowCount = Math.max(1, civilianSalaryTable.getRowCount());
        int tableBodyHeight = civilianSalaryTable.getRowHeight() * rowCount;
        int scrollPaneHeight = tableBodyHeight
              + civilianSalaryTable.getTableHeader().getPreferredSize().height
              + UIUtil.scaleForGUI(4);
        Dimension viewportSize = UIUtil.scaleForGUI(CIVILIAN_TABLE_WIDTH, tableBodyHeight);
        Dimension scrollPaneSize = UIUtil.scaleForGUI(CIVILIAN_TABLE_WIDTH, scrollPaneHeight);

        civilianSalaryTableScrollPane.setPreferredSize(scrollPaneSize);
        civilianSalaryTableScrollPane.setMinimumSize(scrollPaneSize);
        civilianSalaryTable.setPreferredScrollableViewportSize(viewportSize);
        civilianSalaryTableScrollPane.revalidate();
        civilianSalaryTableScrollPane.repaint();

        if (civilianSalaryTableScrollPane.getParent() != null) {
            civilianSalaryTableScrollPane.getParent().revalidate();
            civilianSalaryTableScrollPane.getParent().repaint();
        }
    }

    private JPanel createCivilianSalaryFilterPanel(TableRowSorter<SalaryTableModel> sorter) {
        JLabel filterLabel = new CampaignOptionsLabel("CivilianSalaryFilter");
        filterLabel.addMouseListener(createTipPanelUpdater(salariesHeader, "CivilianSalaryFilter"));

        JTextField filterField = new JTextField(24);
        filterField.setName("txtCivilianSalaryFilter");
        filterField.addMouseListener(createTipPanelUpdater(salariesHeader, "CivilianSalaryFilter"));
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateFilter();
            }

            private void updateFilter() {
                applyCivilianSalaryFilter(sorter, filterField.getText());
            }
        });

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        panel.add(filterLabel);
        panel.add(filterField);

        return panel;
    }

    private void applyCivilianSalaryFilter(TableRowSorter<SalaryTableModel> sorter, String filterText) {
        String normalizedFilter = filterText.trim().toLowerCase(Locale.ROOT);
        if (normalizedFilter.isBlank()) {
            sorter.setRowFilter(null);
            updateCivilianSalaryTableHeight();
            return;
        }

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(RowFilter.Entry<? extends SalaryTableModel, ? extends Integer> entry) {
                return entry.getStringValue(SalaryTableModel.ROLE_COLUMN)
                             .toLowerCase(Locale.ROOT)
                             .contains(normalizedFilter);
            }
        });
        updateCivilianSalaryTableHeight();
    }

    /**
     * Shortcut method to load default {@link CampaignOptions} values into the tab components.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads and applies configuration values from the provided {@link CampaignOptions} object, or uses the default
     * campaign options if none are provided. The configuration includes general settings, personnel logs, personnel
     * information, awards, medical settings, prisoner and dependent settings, and salary-related options. It also
     * adjusts certain values based on the version of the application.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} object to load settings from. If null, default campaign
     *                              options will be used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new SalariesOptionsModel(options);
        updateCreatedControlsFromModel();
    }

    /**
     * Applies the modified salary tab settings to the repository's campaign options. If no preset
     * {@link CampaignOptions} is provided, the changes are applied to the current options.
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to apply changes to.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        updateModelFromCreatedControls();
        model.applyTo(options);
    }

    private void markPageCreated(PersonnelRoleSubType type) {
        switch (type) {
            case COMBAT -> combatPageCreated = true;
            case SUPPORT -> supportPageCreated = true;
            case CIVILIAN -> civilianPageCreated = true;
        }
    }

    private boolean isPageCreated(PersonnelRoleSubType type) {
        return switch (type) {
            case COMBAT -> combatPageCreated;
            case SUPPORT -> supportPageCreated;
            case CIVILIAN -> civilianPageCreated;
        };
    }

    private List<PersonnelRole> getRoles(PersonnelRoleSubType type) {
        return switch (type) {
            case COMBAT -> combatRoles;
            case SUPPORT -> supportRoles;
            case CIVILIAN -> civilianRoles;
        };
    }

    private JSpinner[] getBaseSalarySpinners(PersonnelRoleSubType type) {
        return switch (type) {
            case COMBAT -> spnBaseSalaryCombat;
            case SUPPORT -> spnBaseSalarySupport;
            case CIVILIAN -> throw new IllegalArgumentException("Civilian salaries use a table model.");
        };
    }

    private void updateCreatedControlsFromModel() {
        updateSalariesControlsFromModel(PersonnelRoleSubType.COMBAT);
        updateSalariesControlsFromModel(PersonnelRoleSubType.SUPPORT);
        updateSalariesControlsFromModel(PersonnelRoleSubType.CIVILIAN);
    }

    private void updateSalariesControlsFromModel(PersonnelRoleSubType type) {
        if (!isPageCreated(type) || model == null) {
            return;
        }

        if (type == PersonnelRoleSubType.COMBAT) {
            chkDisableSecondaryRoleSalary.setSelected(model.disableSecondaryRoleSalary);
            spnAntiMekSalary.setValue(model.salaryAntiMekMultiplier);
            spnSpecialistInfantrySalary.setValue(model.salarySpecialistInfantryMultiplier);
            for (final Entry<SkillLevel, JSpinner> entry : spnSalaryExperienceMultipliers.entrySet()) {
                entry.getValue().setValue(model.salaryXpMultipliers.get(entry.getKey()));
            }
        }

        if (type == PersonnelRoleSubType.CIVILIAN) {
            civilianSalaryTableModel.setValues(model.roleBaseSalaries);
            return;
        }

        List<PersonnelRole> roles = getRoles(type);
        JSpinner[] salarySpinners = getBaseSalarySpinners(type);
        for (int i = 0; i < salarySpinners.length; i++) {
            PersonnelRole personnelRole = roles.get(i);
            salarySpinners[i].setValue(model.roleBaseSalaries.get(personnelRole));
        }
    }

    private void updateModelFromCreatedControls() {
        updateModelFromSalariesControls(PersonnelRoleSubType.COMBAT);
        updateModelFromSalariesControls(PersonnelRoleSubType.SUPPORT);
        updateModelFromSalariesControls(PersonnelRoleSubType.CIVILIAN);
    }

    private void updateModelFromSalariesControls(PersonnelRoleSubType type) {
        if (!isPageCreated(type) || model == null) {
            return;
        }

        if (type == PersonnelRoleSubType.COMBAT) {
            model.disableSecondaryRoleSalary = chkDisableSecondaryRoleSalary.isSelected();
            model.salaryAntiMekMultiplier = (double) spnAntiMekSalary.getValue();
            model.salarySpecialistInfantryMultiplier = (double) spnSpecialistInfantrySalary.getValue();
            for (final Entry<SkillLevel, JSpinner> entry : spnSalaryExperienceMultipliers.entrySet()) {
                model.salaryXpMultipliers.put(entry.getKey(), (double) entry.getValue().getValue());
            }
        }

        if (type == PersonnelRoleSubType.CIVILIAN) {
            stopCivilianSalaryTableEditing();
            model.roleBaseSalaries.putAll(civilianSalaryTableModel.getValues());
            return;
        }

        List<PersonnelRole> roles = getRoles(type);
        JSpinner[] salarySpinners = getBaseSalarySpinners(type);
        for (int i = 0; i < salarySpinners.length; i++) {
            model.roleBaseSalaries.put(roles.get(i), (double) salarySpinners[i].getValue());
        }
    }

    private void stopCivilianSalaryTableEditing() {
        if (civilianSalaryTable != null && civilianSalaryTable.isEditing()) {
            civilianSalaryTable.getCellEditor().stopCellEditing();
        }
    }

    private static final class SalaryTableModel extends AbstractTableModel {
        private static final int ROLE_COLUMN = 0;
        private static final int SALARY_COLUMN = 1;
        private static final int COLUMN_COUNT = 2;

        private final List<PersonnelRole> roles;
        private final Map<PersonnelRole, Double> salaries = new HashMap<>();

        private SalaryTableModel(List<PersonnelRole> roles) {
            this.roles = List.copyOf(roles);
            for (PersonnelRole role : roles) {
                salaries.put(role, 0.0);
            }
        }

        @Override
        public int getRowCount() {
            return roles.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case ROLE_COLUMN -> getTextAt(getCampaignOptionsResourceBundle(), "lblSalaryRoleColumn.text");
                case SALARY_COLUMN -> getTextAt(getCampaignOptionsResourceBundle(), "lblSalaryAmountColumn.text");
                default -> throw new IllegalArgumentException("Unknown salary table column: " + column);
            };
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case ROLE_COLUMN -> String.class;
                case SALARY_COLUMN -> Double.class;
                default -> Object.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == SALARY_COLUMN;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PersonnelRole role = roles.get(rowIndex);
            return switch (columnIndex) {
                case ROLE_COLUMN -> role.toString();
                case SALARY_COLUMN -> salaries.get(role);
                default -> throw new IllegalArgumentException("Unknown salary table column: " + columnIndex);
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != SALARY_COLUMN) {
                return;
            }

            double salary = parseSalary(value);
            salaries.put(roles.get(rowIndex), salary);
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        private double parseSalary(Object value) {
            if (value == null) {
                return 0.0;
            }

            if (value instanceof Number number) {
                return clampSalary(number.doubleValue());
            }

            try {
                return clampSalary(Double.parseDouble(value.toString()));
            } catch (NumberFormatException exception) {
                return 0.0;
            }
        }

        private double clampSalary(double value) {
            return Math.min(1000000.0, Math.max(0.0, value));
        }

        private PersonnelRole getRole(int rowIndex) {
            return roles.get(rowIndex);
        }

        private void setValues(Map<PersonnelRole, Double> values) {
            for (PersonnelRole role : roles) {
                salaries.put(role, values.getOrDefault(role, 0.0));
            }
            fireTableDataChanged();
        }

        private Map<PersonnelRole, Double> getValues() {
            return Map.copyOf(salaries);
        }
    }

}
