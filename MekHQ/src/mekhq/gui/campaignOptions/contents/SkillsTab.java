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

import static mekhq.campaign.personnel.skills.enums.SkillSubType.*;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.CAMPAIGN_OPTIONS_PAGE_CONTENT_WIDTH;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * SkillsTab is a component of the campaign options user interface that allows players to configure the rules and costs
 * associated with skills in their campaign.
 *
 * <p>Each skill sub-type (Gunnery, Piloting, Support, Utility, Roleplay) is presented as a single table listing every
 * skill in that category alongside its base target number and a summary of its experience milestones. Per-level XP
 * costs and the milestone thresholds are edited through a dedicated "Advanced" pop-up so the main view stays compact.
 * Ctrl+C copies the selected row's full configuration and Ctrl+V applies it onto one or more selected rows.</p>
 */
public class SkillsTab {
    private final CampaignOptions campaignOptions;

    private SkillsOptionsModel model;
    private Map<SkillSubType, CampaignOptionsPagePanel> createdCategoryTabs;
    private List<SkillsTableModel> tableModels;
    private SkillConfiguration storedConfiguration;

    // Only the spinners are read back (in load/apply); their labels and the containing panel are write-only locals
    // built in createEdgeCostPanel().
    private JSpinner spnEdgeCost;
    private JSpinner spnAttributeCost;

    /**
     * Constructs a new {@code SkillsTab} instance and initializes the necessary data structures for managing skill
     * configurations.
     *
     * @param campaignOptions the {@code CampaignOptions} instance that holds the settings to be modified or displayed
     *                        in this tab.
     */
    public SkillsTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;
        initialize();
        loadValuesFromCampaignOptions();
    }

    /**
     * Sets up the data structures needed for skill configuration in the SkillsTab.
     */
    private void initialize() {
        createdCategoryTabs = new EnumMap<>(SkillSubType.class);
        tableModels = new ArrayList<>();
        storedConfiguration = null;
    }

    /**
     * Creates the main panel for the SkillsTab UI based on the provided {@link SkillSubType} category.
     *
     * @param category the {@link SkillSubType} representing the skill category to display.
     *
     * @return a {@link JPanel} containing the skill table and supporting controls for the selected category.
     */
    public JPanel createSkillsTab(SkillSubType category) {
        // The page never rebuilds in place (only its table models refresh), so the CampaignOptionsPagePanel can be
        // cached and returned directly rather than wrapping it in a container.
        return createdCategoryTabs.computeIfAbsent(category, this::createSkillsPage);
    }

    private CampaignOptionsPagePanel createSkillsPage(SkillSubType category) {
        // Header
        CampaignOptionsHeaderPanel headerPanel;
        String panelName;
        switch (category) {
            case COMBAT_GUNNERY -> {
                headerPanel = new CampaignOptionsHeaderPanel("GunnerySkillsTab",
                      getImageDirectory() + "logo_clan_diamond_sharks.png");
                panelName = "GunnerySkillsTab";
            }
            case COMBAT_PILOTING -> {
                headerPanel = new CampaignOptionsHeaderPanel("PilotingSkillsTab",
                      getImageDirectory() + "logo_capellan_confederation.png");
                panelName = "PilotingSkillsTab";
            }
            case SUPPORT -> {
                headerPanel = new CampaignOptionsHeaderPanel("SupportSkillsTab",
                      getImageDirectory() + "logo_clan_goliath_scorpion.png");
                panelName = "SupportSkillsTab";
            }
            case UTILITY -> {
                headerPanel = new CampaignOptionsHeaderPanel("UtilitySkillsTab",
                      getImageDirectory() + "logo_axumite_providence.png");
                panelName = "UtilitySkillsTab";
            }
            default -> { // ROLEPLAY
                headerPanel = new CampaignOptionsHeaderPanel("RoleplaySkillsTab",
                      getImageDirectory() + "logo_clan_jade_falcon.png");
                panelName = "RoleplaySkillsTab";
            }
        }

        // Contents
        List<String> relevantSkills = new ArrayList<>();
        for (String skillName : SkillType.getSkillList()) {
            SkillType skill = SkillType.getType(skillName);
            SkillSubType subType = skill.getSubType();

            boolean isCorrectType = switch (category) {
                case NONE, COMBAT_GUNNERY -> subType == COMBAT_GUNNERY;
                case COMBAT_PILOTING -> subType == COMBAT_PILOTING;
                case SUPPORT -> subType == SUPPORT || subType == SUPPORT_TECHNICIAN;
                case UTILITY -> subType == UTILITY || subType == UTILITY_COMMAND;
                case ROLEPLAY_GENERAL -> subType == ROLEPLAY_GENERAL ||
                                               subType == ROLEPLAY_ART ||
                                               subType == ROLEPLAY_INTEREST ||
                                               subType == ROLEPLAY_SCIENCE ||
                                               subType == ROLEPLAY_SECURITY;
                // These next few cases shouldn't get hit, but we include them just in case
                case SUPPORT_TECHNICIAN -> subType == SUPPORT_TECHNICIAN;
                case UTILITY_COMMAND -> subType == UTILITY_COMMAND;
                case ROLEPLAY_ART -> subType == ROLEPLAY_ART;
                case ROLEPLAY_INTEREST -> subType == ROLEPLAY_INTEREST;
                case ROLEPLAY_SCIENCE -> subType == ROLEPLAY_SCIENCE;
                case ROLEPLAY_SECURITY -> subType == ROLEPLAY_SECURITY;
            };

            // If the type is {@code null} for some reason, dump it into the combat category
            if (isCorrectType || (subType == null && category == COMBAT_GUNNERY)) {
                relevantSkills.add(skillName);
            }
        }

        SkillsTableModel tableModel = new SkillsTableModel(relevantSkills, model);
        tableModels.add(tableModel);
        JTable table = createSkillsTable(tableModel);

        // Build the single section that holds the misc costs (Gunnery only), the copy/paste controls, and the table.
        JComponent sectionContent = createSkillsSectionContent(category, table, tableModel);

        return CampaignOptionsPagePanel.builder(panelName, panelName, getImageDirectory())
                     .header(headerPanel)
                     .showDetailsPanel(false)
                     .sectionsExpandedByDefault(true)
                     .section("lblSkillsSection.text", "lblSkillsSection.summary", sectionContent)
                     .build();
    }

    /**
     * Builds the body for the skills section: the optional Misc Costs row (Gunnery only), the keyboard-shortcut hint
     * row, and the skills table.
     *
     * @param category   the skill sub-type being displayed
     * @param table      the skills table
     * @param tableModel the model backing the table
     *
     * @return a {@link JComponent} containing the section's contents
     */
    private JComponent createSkillsSectionContent(SkillSubType category, JTable table, SkillsTableModel tableModel) {
        final JPanel content = new CampaignOptionsStandardPanel("SkillsSectionContent", false);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(content);

        installCopyPasteBindings(table, tableModel);

        JLabel hintLabel = new JLabel(getTextAt(getCampaignOptionsResourceBundle(), "lblSkillTableHint.text"));

        layout.gridy = 0;

        if (category == COMBAT_GUNNERY) {
            layout.gridwidth = 5;
            layout.gridx = 0;
            content.add(createEdgeCostPanel(), layout);
            layout.gridy++;
        }

        layout.gridwidth = 5;
        layout.gridx = 0;
        content.add(hintLabel, layout);

        layout.gridx = 0;
        layout.gridy++;
        content.add(createTableScrollPane(table), layout);

        return content;
    }

    private JTable createSkillsTable(SkillsTableModel tableModel) {
        JTable table = new JTable(tableModel);
        table.setName("tblSkills");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setShowVerticalLines(false);
        table.setRowHeight(Math.max(table.getRowHeight(), UIUtil.scaleForGUI(24)));
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        // Allow Ctrl/Shift selection of several rows so a copied configuration can be pasted onto all of them.
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.getColumnModel().getColumn(SkillsTableModel.SKILL_COLUMN)
              .setPreferredWidth(UIUtil.scaleForGUI(180));
        table.getColumnModel().getColumn(SkillsTableModel.TARGET_COLUMN)
              .setPreferredWidth(UIUtil.scaleForGUI(50));
        table.getColumnModel().getColumn(SkillsTableModel.PROGRESSION_COLUMN)
              .setPreferredWidth(UIUtil.scaleForGUI(230));
        table.getColumnModel().getColumn(SkillsTableModel.COST_COLUMN)
              .setPreferredWidth(UIUtil.scaleForGUI(330));
        table.getColumnModel().getColumn(SkillsTableModel.EDIT_COLUMN)
              .setPreferredWidth(UIUtil.scaleForGUI(70));

        // Left-align the target number so it reads as a value rather than a right-aligned figure.
        DefaultTableCellRenderer leftAlignedRenderer = new DefaultTableCellRenderer();
        leftAlignedRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        table.getColumnModel().getColumn(SkillsTableModel.TARGET_COLUMN).setCellRenderer(leftAlignedRenderer);

        // Auto-select the existing value when editing a TN cell, so clicking a cell then typing a number replaces it.
        JTextField targetEditorField = new JTextField();
        targetEditorField.setHorizontalAlignment(SwingConstants.LEFT);
        ((AbstractDocument) targetEditorField.getDocument()).setDocumentFilter(new DigitOnlyDocumentFilter());
        DefaultCellEditor targetEditor = new DefaultCellEditor(targetEditorField) {
            @Override
            public boolean isCellEditable(EventObject anEvent) {
                // Let Ctrl/Shift clicks extend the row selection instead of starting an edit, so multiple rows can be
                // selected (and pasted onto) even when clicking within the TN column.
                if (anEvent instanceof MouseEvent mouseEvent
                          && (mouseEvent.isControlDown() || mouseEvent.isShiftDown())) {
                    return false;
                }
                return super.isCellEditable(anEvent);
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                  int column) {
                Component editor = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                SwingUtilities.invokeLater(targetEditorField::selectAll);
                return editor;
            }
        };
        targetEditor.setClickCountToStart(1);
        table.getColumnModel().getColumn(SkillsTableModel.TARGET_COLUMN).setCellEditor(targetEditor);
        // Mirror the row copy/paste shortcuts onto the TN editor field so Ctrl+C/Ctrl+V still copy the whole row even
        // when a TN cell is in edit mode (the field would otherwise consume them as a plain text copy/paste).
        registerRowCopyPasteShortcuts(targetEditorField, JComponent.WHEN_FOCUSED, table, tableModel);

        String editText = getTextAt(getCampaignOptionsResourceBundle(), "btnSkillAdvanced.text");
        table.getColumnModel().getColumn(SkillsTableModel.EDIT_COLUMN)
              .setCellRenderer(new ButtonRenderer(editText));
        table.getColumnModel().getColumn(SkillsTableModel.EDIT_COLUMN)
              .setCellEditor(new AdvancedButtonEditor(table, tableModel, editText));

        return table;
    }

    private JScrollPane createTableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        int rowCount = Math.max(1, table.getRowCount());
        int bodyHeight = table.getRowHeight() * rowCount;
        int headerHeight = table.getTableHeader().getPreferredSize().height;
        int width = CAMPAIGN_OPTIONS_PAGE_CONTENT_WIDTH;
        Dimension viewportSize = new Dimension(width, bodyHeight);
        Dimension scrollPaneSize = new Dimension(width, bodyHeight + headerHeight + UIUtil.scaleForGUI(4));

        table.setPreferredScrollableViewportSize(viewportSize);
        scrollPane.setPreferredSize(scrollPaneSize);
        scrollPane.setMinimumSize(scrollPaneSize);

        return scrollPane;
    }

    /**
     * Creates the borderless "Misc Costs" row containing the per-rank Edge cost and per-improvement Attribute cost
     * controls laid out side by side. Only shown on the Gunnery tab.
     *
     * @return a {@link JPanel} containing the Edge and Attribute cost controls.
     */
    private JPanel createEdgeCostPanel() {
        JLabel lblEdgeCost = new CampaignOptionsLabel("EdgeCost");
        spnEdgeCost = new CampaignOptionsSpinner("EdgeCost", 100, 0, 500, 1);

        JLabel lblAttributeCost = new CampaignOptionsLabel("AttributeCost");
        spnAttributeCost = new CampaignOptionsSpinner("AttributeCost", 100, 0, 500, 1);

        // Borderless content panel with both cost controls side by side
        final JPanel content = new CampaignOptionsStandardPanel("EdgeCostPanel", false);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(content);

        layout.gridwidth = 1;
        layout.gridy = 0;
        layout.gridx = 0;
        content.add(lblEdgeCost, layout);
        layout.gridx++;
        content.add(spnEdgeCost, layout);
        layout.gridx++;
        content.add(lblAttributeCost, layout);
        layout.gridx++;
        content.add(spnAttributeCost, layout);

        return content;
    }

    /**
     * Registers Ctrl+C / Ctrl+V key bindings on the skills table so a row's full configuration can be copied from the
     * selected row and pasted onto one or more selected rows without dedicated buttons.
     *
     * @param table      the skills table to bind the shortcuts to
     * @param tableModel the model backing the table
     */
    private void installCopyPasteBindings(JTable table, SkillsTableModel tableModel) {
        registerRowCopyPasteShortcuts(table, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, table, tableModel);
    }

    /**
     * Registers the Ctrl+C / Ctrl+V row copy/paste actions on the given component. This is also applied to the TN cell
     * editor so the shortcuts work while a TN cell is being edited (otherwise the editor would consume the keystrokes
     * as a plain text copy/paste and the row configuration would never be captured).
     *
     * @param component  the component whose input/action maps receive the shortcuts
     * @param condition  the input map condition (e.g. {@link JComponent#WHEN_FOCUSED})
     * @param table      the skills table the actions operate on
     * @param tableModel the model backing the table
     */
    private void registerRowCopyPasteShortcuts(JComponent component, int condition, JTable table,
          SkillsTableModel tableModel) {
        InputMap inputMap = component.getInputMap(condition);
        ActionMap actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copySkill");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "pasteSkill");

        actionMap.put("copySkill", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopEditing(table);
                copySelectedSkill(table, tableModel);
            }
        });
        actionMap.put("pasteSkill", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopEditing(table);
                pasteToSelectedSkills(table, tableModel);
            }
        });
    }

    private void stopEditing(JTable table) {
        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
    }

    private void copySelectedSkill(JTable table, SkillsTableModel tableModel) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }

        SkillConfiguration configuration = tableModel.getConfiguration(table.convertRowIndexToModel(viewRow));
        if (configuration != null) {
            storedConfiguration = new SkillConfiguration(configuration);
        }
    }

    private void pasteToSelectedSkills(JTable table, SkillsTableModel tableModel) {
        if (storedConfiguration == null) {
            return;
        }

        for (int viewRow : table.getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            SkillConfiguration configuration = tableModel.getConfiguration(modelRow);
            if (configuration != null) {
                configuration.copyFrom(storedConfiguration);
                tableModel.fireTableRowsUpdated(modelRow, modelRow);
            }
        }
    }

    private void openAdvancedEditor(JTable table, SkillsTableModel tableModel, int modelRow) {
        SkillConfiguration configuration = tableModel.getConfiguration(modelRow);
        if (configuration == null) {
            return;
        }

        Window parent = SwingUtilities.getWindowAncestor(table);
        SkillAdvancedEditorDialog dialog = new SkillAdvancedEditorDialog(parent,
              tableModel.getSkillName(modelRow), configuration);
        dialog.setVisible(true);

        if (dialog.wasChanged()) {
            tableModel.fireTableRowsUpdated(modelRow, modelRow);
        }
    }

    /**
     * Loads skill values from the current campaign options using default skill values.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, Map.of());
    }

    /**
     * Loads skill values into the model and refreshes any tables that have already been created.
     *
     * @param presetCampaignOptions an optional {@link CampaignOptions} source. If {@code null}, the current campaign
     *                              options are used.
     * @param presetSkillValues     an optional map of preset skill values. If null or empty, default skill values are
     *                              used instead.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
          Map<String, SkillType> presetSkillValues) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        model = new SkillsOptionsModel(options, presetSkillValues);

        for (SkillsTableModel tableModel : tableModels) {
            tableModel.setOptionsModel(model);
        }

        if (spnEdgeCost != null) {
            spnEdgeCost.setValue(model.edgeCost);
        }
        if (spnAttributeCost != null) {
            spnAttributeCost.setValue(model.attributeCost);
        }
    }

    /**
     * Transfers the configured skill values from the SkillsTab into the campaign's underlying data model.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} instance to save settings to, or {@code null} to update
     *                              the current campaign options.
     * @param presetSkills          an optional map of preset skill values. Overrides default values if provided.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions,
          @Nullable Map<String, SkillType> presetSkills) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        if (spnEdgeCost != null) {
            model.edgeCost = (int) spnEdgeCost.getValue();
        }
        if (spnAttributeCost != null) {
            model.attributeCost = (int) spnAttributeCost.getValue();
        }

        model.applyTo(options, presetSkills);
    }

    /**
     * Builds a short, human-readable summary of a skill's experience milestones (the level at which a character is
     * considered Green, Regular, Veteran, Elite, Heroic, and Legendary).
     *
     * @param configuration the configuration to summarize
     *
     * @return a compact milestone summary string
     */
    private static String buildProgressionSummary(SkillConfiguration configuration) {
        return String.format("G %d  ·  R %d  ·  V %d  ·  E %d  ·  H %d  ·  L %d",
              configuration.greenLevel,
              configuration.regularLevel,
              configuration.veteranLevel,
              configuration.eliteLevel,
              configuration.heroicLevel,
              configuration.legendaryLevel);
    }

    /**
     * Builds a compact summary of a skill's per-level XP costs (levels 0 through 10). A cost of {@code -1} marks an
     * unreachable level and is shown as an em dash.
     *
     * @param configuration the configuration to summarize
     *
     * @return a compact XP cost summary string
     */
    private static String buildCostSummary(SkillConfiguration configuration) {
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < configuration.costs.length; i++) {
            if (i > 0) {
                summary.append(" · ");
            }
            Integer cost = configuration.costs[i];
            summary.append((cost == null || cost < 0) ? "—" : cost);
        }
        return summary.toString();
    }

    /**
     * Table model backing a single skill sub-type's table. It reads and writes the live {@link SkillConfiguration}
     * instances held by the {@link SkillsOptionsModel}, so edits made through the table (and the advanced editor) are
     * reflected immediately in the model.
     */
    private static final class SkillsTableModel extends AbstractTableModel {
        private static final int SKILL_COLUMN = 0;
        private static final int TARGET_COLUMN = 1;
        private static final int PROGRESSION_COLUMN = 2;
        private static final int COST_COLUMN = 3;
        private static final int EDIT_COLUMN = 4;
        private static final int COLUMN_COUNT = 5;

        private final List<String> skillNames;
        private SkillsOptionsModel optionsModel;

        private SkillsTableModel(List<String> skillNames, SkillsOptionsModel optionsModel) {
            this.skillNames = List.copyOf(skillNames);
            this.optionsModel = optionsModel;
        }

        private void setOptionsModel(SkillsOptionsModel optionsModel) {
            this.optionsModel = optionsModel;
            fireTableDataChanged();
        }

        private String getSkillName(int rowIndex) {
            return skillNames.get(rowIndex);
        }

        private SkillConfiguration getConfiguration(int rowIndex) {
            if (optionsModel == null) {
                return null;
            }
            return optionsModel.getSkillConfiguration(skillNames.get(rowIndex));
        }

        @Override
        public int getRowCount() {
            return skillNames.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case SKILL_COLUMN -> getTextAt(getCampaignOptionsResourceBundle(), "lblSkillTableSkillColumn.text");
                case TARGET_COLUMN -> getTextAt(getCampaignOptionsResourceBundle(), "lblSkillTableTargetColumn.text");
                case PROGRESSION_COLUMN ->
                      getTextAt(getCampaignOptionsResourceBundle(), "lblSkillTableProgressionColumn.text");
                case COST_COLUMN -> getTextAt(getCampaignOptionsResourceBundle(), "lblSkillTableCostColumn.text");
                case EDIT_COLUMN ->
                      getTextAt(getCampaignOptionsResourceBundle(), "lblSkillTableAdvancedColumn.text");
                default -> throw new IllegalArgumentException("Unknown skill table column: " + column);
            };
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case TARGET_COLUMN -> Integer.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == TARGET_COLUMN || columnIndex == EDIT_COLUMN;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SkillConfiguration configuration = getConfiguration(rowIndex);
            return switch (columnIndex) {
                case SKILL_COLUMN -> skillNames.get(rowIndex);
                case TARGET_COLUMN -> configuration == null ? 0 : configuration.targetNumber;
                case PROGRESSION_COLUMN -> configuration == null ? "" : buildProgressionSummary(configuration);
                case COST_COLUMN -> configuration == null ? "" : buildCostSummary(configuration);
                case EDIT_COLUMN -> "";
                default -> throw new IllegalArgumentException("Unknown skill table column: " + columnIndex);
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != TARGET_COLUMN) {
                return;
            }

            SkillConfiguration configuration = getConfiguration(rowIndex);
            if (configuration == null) {
                return;
            }

            configuration.targetNumber = clampTargetNumber(value);
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        private int clampTargetNumber(Object value) {
            int parsed;
            if (value instanceof Number number) {
                parsed = number.intValue();
            } else if (value == null) {
                parsed = 0;
            } else {
                try {
                    parsed = Integer.parseInt(value.toString().trim());
                } catch (NumberFormatException exception) {
                    parsed = 0;
                }
            }
            return Math.max(0, Math.min(12, parsed));
        }
    }

    /**
     * A {@link DocumentFilter} that only permits digit characters, used to keep the TN cell editor numeric. The table
     * model still clamps the final value to its valid range when editing stops.
     */
    private static final class DigitOnlyDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
              throws BadLocationException {
            if (isDigitsOnly(string)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
              throws BadLocationException {
            if (isDigitsOnly(text)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        private static boolean isDigitsOnly(String text) {
            if (text == null) {
                return true;
            }
            for (int i = 0; i < text.length(); i++) {
                if (!Character.isDigit(text.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A table cell renderer that paints a button. Used to give every skill row an "Advanced" affordance.
     */
    private static final class ButtonRenderer extends JButton implements TableCellRenderer {        private ButtonRenderer(String text) {
            super(text);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            return this;
        }
    }

    /**
     * A table cell editor that turns the "Advanced" column into a clickable button. Clicking it opens the advanced
     * editor dialog for that row.
     */
    private final class AdvancedButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button;
        private final JTable table;
        private final SkillsTableModel tableModel;
        private int editingModelRow = -1;

        private AdvancedButtonEditor(JTable table, SkillsTableModel tableModel, String text) {
            this.table = table;
            this.tableModel = tableModel;
            this.button = new JButton(text);
            this.button.addActionListener(e -> {
                int row = editingModelRow;
                fireEditingStopped();
                if (row >= 0) {
                    openAdvancedEditor(this.table, this.tableModel, row);
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
              int column) {
            editingModelRow = table.convertRowIndexToModel(row);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }
}
