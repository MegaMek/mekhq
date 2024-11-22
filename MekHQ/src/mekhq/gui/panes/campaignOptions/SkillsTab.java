package mekhq.gui.panes.campaignOptions;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.SkillType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import static mekhq.campaign.personnel.SkillType.isCombatSkill;
import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

public class SkillsTab {
    JFrame frame;
    String name;

    private static List<JScrollPane> allTableScrollPanes = new ArrayList<>();
    private static List<List<Integer>> storedValues = new ArrayList<>();

    //start Target Numbers
    private Hashtable<String, JSpinner> hashSkillTargets;
    private Hashtable<String, JSpinner> hashGreenSkill;
    private Hashtable<String, JSpinner> hashRegSkill;
    private Hashtable<String, JSpinner> hashVetSkill;
    private Hashtable<String, JSpinner> hashEliteSkill;
    //end Target Numbers

    //start Combat Skills Tab
    //end Combat Skills Tab

    //start Support Skills Tab
    //end Support Skills Tab

    private static final MMLogger logger = MMLogger.create(SkillsTab.class);

    SkillsTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializeTargetNumbers();
        initializeCombatSkillsTab();
    }

    private void initializeTargetNumbers() {
        hashSkillTargets = new Hashtable<>();
        hashGreenSkill = new Hashtable<>();
        hashRegSkill = new Hashtable<>();
        hashVetSkill = new Hashtable<>();
        hashEliteSkill = new Hashtable<>();
    }

    private void initializeCombatSkillsTab() {
    }

    JPanel createSkillsTab(boolean isCombatTab) {
        // Header
        JPanel headerPanel = null;
        if (isCombatTab) {
            headerPanel = new CampaignOptionsHeaderPanel("CombatSkillsTab",
                getImageDirectory() + "logo_clan_ghost_bear.png",
                true);
        } else {
            headerPanel = new CampaignOptionsHeaderPanel("SupportSkillsTab",
                getImageDirectory() + "logo_clan_ghost_bear.png",
                true);
        }

        // Contents
        List<String> skills = new ArrayList<>();

        for (String skillName : SkillType.getSkillList()) {
            SkillType skill = SkillType.getType(skillName);
            boolean isCombatSkill = isCombatSkill(skill);

            if (isCombatSkill == isCombatTab) {
                skills.add(skill.getName());
            }
        }

        java.util.List<JPanel> skillPanels = new ArrayList<>();

        for (String skill : skills) {
            JPanel skillPanel = createSkillPanel(skill);
            skillPanels.add(skillPanel);
        }

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel(isCombatTab ?
            "CombatSkillsTab" : "SupportSkillsTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Create a button to toggle the table
        JButton hideAllButton = new JButton(resources.getString("btnHideAll.text"));
        hideAllButton.addActionListener(e -> {
            for (JScrollPane scrollPane : allTableScrollPanes) {
                scrollPane.setVisible(false);
            }
            panel.revalidate();
            panel.repaint();
        });

        // Create a button to toggle the table
        JButton showAllButton = new JButton(resources.getString("btnDisplayAll.text"));
        showAllButton.addActionListener(e -> {
            for (JScrollPane scrollPane : allTableScrollPanes) {
                scrollPane.setVisible(true);
            }
            panel.revalidate();
            panel.repaint();
        });

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(showAllButton, layout);
        layout.gridx++;
        panel.add(hideAllButton, layout);

        layout.gridx = 0;
        layout.gridy++;
        int tableCounter = 0;
        for (int i = 0; i < 4; i++) {
            layout.gridy++;
            layout.gridx = 0;
            for (int j = 0; j < 5; j++) {
                if (tableCounter < skillPanels.size()) {
                    panel.add(skillPanels.get(tableCounter), layout);
                    layout.gridx++;
                }
                tableCounter++;
            }
        }

        // Create Parent Panel
        return createParentPanel(panel, "CombatSkillsTab");
    }

    private static JPanel createSkillPanel(String skillName) {
        String panelName = "SkillPanel" + skillName.replace(" ", "");

        // Create the target number spinner
        JLabel label = new CampaignOptionsLabel("SkillPanelTargetNumber");
        JSpinner spinner = new CampaignOptionsSpinner("SkillPanelTargetNumber",
            0, 0, 10, 1);

        // Create the table
        JTable skillTable = CustomTableComponent.createCustomTable();

        // Wrap it in a scrollPane, ideally the scrollbars will never be needed.
        // We could disable them, but it's better that we have them present in case we muck up the scaling.
        JScrollPane tableScrollPane = new JScrollPane(skillTable);
        tableScrollPane.setPreferredSize(new Dimension(UIUtil.scaleForGUI(250, 370)));
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        allTableScrollPanes.add(tableScrollPane);
        tableScrollPane.setVisible(false);

        JButton copyButton = new JButton(resources.getString("btnCopy.text"));
        copyButton.addActionListener(e -> {
            storedValues.clear();

            int spinnerValue = ((Double) spinner.getValue()).intValue();
            List<Integer> intermediateList = List.of(spinnerValue);
            storedValues.add(new ArrayList<>(intermediateList));

            JTable table = (JTable) tableScrollPane.getViewport().getView();
            for (int row = 0; row < table.getRowCount(); row++) {
                List<Integer> rowValues = new ArrayList<>();
                rowValues.add((Integer) table.getValueAt(row, 1));

                SkillLevel milestone = (SkillLevel) table.getValueAt(row, 2);
                rowValues.add(milestone.ordinal());

                storedValues.add(rowValues);
            }

            logger.info(storedValues);
        });

        JButton pasteButton = new JButton(resources.getString("btnPaste.text"));
        pasteButton.addActionListener(e -> {
            spinner.setValue((double) storedValues.get(0).get(0));

            JTable table = (JTable) tableScrollPane.getViewport().getView();
            DefaultTableModel model = (DefaultTableModel) table.getModel();

            int tableRowCount = model.getRowCount();

            for (int row = 1; row < tableRowCount; row++) {
                List<Integer> rowValues = storedValues.get(row);

                model.setValueAt(rowValues.get(0), row, 1);
                model.setValueAt(SkillLevel.parseFromInteger(rowValues.get(1)), row, 2);
            }
        });

        final JPanel panel = new CampaignOptionsStandardPanel(panelName, true, panelName);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Create a button to toggle the table
        JButton toggleButton = new JButton(resources.getString("btnToggle.text"));
        toggleButton.addActionListener(e -> {
            boolean visible = tableScrollPane.isVisible();
            tableScrollPane.setVisible(!visible);
            panel.revalidate();
            panel.repaint();
        });

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 2;
        panel.add(toggleButton, layout);
        layout.gridy++;

        layout.gridy++;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(copyButton, layout);
        layout.gridx++;
        panel.add(pasteButton, layout);

        layout.gridy++;
        layout.gridx = 0;
        panel.add(label, layout);
        layout.gridx++;
        panel.add(spinner, layout);

        layout.gridy++;
        layout.gridx = 0;
        layout.gridwidth = 4;
        panel.add(tableScrollPane, layout);

        return panel;
    }
}

class CustomTableComponent {
    private static final MMLogger logger = MMLogger.create(CustomTableComponent.class);

    static class SpinnerRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JSpinner spinner = new JSpinner();
            spinner.setValue(Integer.parseInt(value.toString()));
            return spinner;
        }
    }

    static class SkillLevelEditor extends AbstractCellEditor implements TableCellEditor {
        private JComboBox<SkillLevel> comboBox = new JComboBox<>();
        private int currentRow = -1;

        SkillLevelEditor(DefaultTableModel model, SkillLevel... skillLevels) {
            comboBox.setModel(new DefaultComboBoxModel<>(skillLevels));
            comboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED && currentRow > 0) {
                    SkillLevel selected = (SkillLevel) comboBox.getSelectedItem();
                    SkillLevel above = (SkillLevel) model.getValueAt(currentRow-1, 2);

                    if (selected == null) {
                        logger.error("Null selection in SkillLevelEditor");
                        return;
                    }

                    if (selected.ordinal() < above.ordinal()) {
                        comboBox.setSelectedItem(above);
                    }

                    for (int i = currentRow + 1; i < model.getRowCount(); i++) {
                        model.setValueAt(comboBox.getSelectedItem(), i, 2);
                    }
                }
            });
        }

        @Override
        public boolean stopCellEditing() {
            comboBox.setSelectedItem(comboBox.getSelectedItem());
            return super.stopCellEditing();
        }

        @Override
        public Component getTableCellEditorComponent (JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            comboBox.setSelectedItem(value);
            return comboBox;
        }

        @Override
        public Object getCellEditorValue() {
            return comboBox.getSelectedItem();
        }
    }

    static class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner();

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            spinner.setValue(value);
            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }
    }

    public static JTable createCustomTable() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int column) {
                return switch (column) {
                    case 0, 1 -> Integer.class;
                    case 2 -> SkillLevel.class;
                    default -> String.class;
                };
            }
        };

        model.addColumn("Level");
        model.addColumn("Cost");
        model.addColumn("Milestone");

        SkillLevel[] skillLevels = Arrays.stream(SkillLevel.values())
            .filter(e -> e != SkillLevel.HEROIC && e != SkillLevel.LEGENDARY)
            .toArray(SkillLevel[]::new);

        for (int i = 0; i <= 10; i++) {
            model.addRow(new Object[] {i, 0, skillLevels[0]});
        }

        JTable table = new JTable(model);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        table.getColumnModel().getColumn(1).setCellEditor(new SpinnerEditor());
        table.getColumnModel().getColumn(1).setCellRenderer(new SpinnerRenderer());

        table.getColumnModel().getColumn(2).setCellEditor(new SkillLevelEditor(model, skillLevels));

        table.setRowHeight(UIUtil.scaleForGUI(30));
        table.setPreferredScrollableViewportSize(new Dimension(UIUtil.scaleForGUI(500, 200)));
        table.setFillsViewportHeight(true);

        return table;
    }

    public static void main(String... args) {
        JFrame frame = new JFrame();
        frame.setSize(UIUtil.scaleForGUI(600, 300));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.add(new JScrollPane(createCustomTable()));

        frame.add(panel);
        frame.setVisible(true);
    }
}
