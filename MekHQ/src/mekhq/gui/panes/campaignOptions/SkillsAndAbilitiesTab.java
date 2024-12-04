package mekhq.gui.panes.campaignOptions;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.SkillType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static mekhq.campaign.personnel.SkillType.isCombatSkill;
import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

public class SkillsAndAbilitiesTab {
    JFrame frame;
    String name;

    private static List<JLabel> allSkillLabels = new ArrayList<>();
    private static List<JSpinner> allSkillSpinners = new ArrayList<>();
    private static List<JComboBox<SkillLevel>> allSkillComboBoxes = new ArrayList<>();
    private static double storedTargetNumber = 0;
    private static List<Double> storedValuesSpinners = new ArrayList<>();
    private static List<SkillLevel> storedValuesComboBoxes = new ArrayList<>();

    SkillsAndAbilitiesTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    private void initialize() {
        initializeGeneral();
    }

    private void initializeGeneral() {
        allSkillLabels = new ArrayList<>();
        allSkillSpinners = new ArrayList<>();
        allSkillComboBoxes = new ArrayList<>();
        storedTargetNumber = 0;
        storedValuesSpinners = new ArrayList<>();
        storedValuesComboBoxes = new ArrayList<>();
    }

    JPanel createSkillsTab(boolean isCombatTab) {
        // Header
        JPanel headerPanel;
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
            setVisibleForAll(false);

            panel.revalidate();
            panel.repaint();
        });

        // Create a button to toggle the table
        JButton showAllButton = new JButton(resources.getString("btnDisplayAll.text"));
        showAllButton.addActionListener(e -> {
            setVisibleForAll(true);

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

    private void setVisibleForAll(boolean visible) {
        for (int i = 0; i < allSkillLabels.size(); i++) {
            allSkillLabels.get(i).setVisible(visible);
            allSkillSpinners.get(i).setVisible(visible);
            allSkillComboBoxes.get(i).setVisible(visible);
        }
    }

    private static JPanel createSkillPanel(String skillName) {
        String panelName = "SkillPanel" + skillName.replace(" ", "");

        // Create the target number spinner
        JLabel lblTargetNumber = new CampaignOptionsLabel("SkillPanelTargetNumber");
        JSpinner spnTargetNumber = new CampaignOptionsSpinner("SkillPanelTargetNumber",
            0, 0, 10, 1);

        List<JLabel> labels = new ArrayList<>();
        List<JSpinner> spinners = new ArrayList<>();
        List<JComboBox<SkillLevel>> comboBoxes = new ArrayList<>();

        for (int i = 0; i < 11; i++) {
            JLabel label = new CampaignOptionsLabel("SkillLevel" + i, null, true);
            label.setVisible(false);
            labels.add(label);
            allSkillLabels.add(label);

            JSpinner spinner = new CampaignOptionsSpinner("SkillLevel" + i,
                null, 0, 0, 10, 1, true);
            spinner.setVisible(false);
            spinners.add(spinner);
            allSkillSpinners.add(spinner);

            JComboBox<SkillLevel> comboBox = new JComboBox<>(SkillLevel.values());
            comboBox.addActionListener(e -> milestoneActionListener(comboBoxes, comboBox));
            comboBox.setVisible(false);
            comboBoxes.add(comboBox);
            allSkillComboBoxes.add(comboBox);
        }

        JButton copyButton = new JButton(resources.getString("btnCopy.text"));
        copyButton.addActionListener(e -> {
            storedTargetNumber = (Double) spnTargetNumber.getValue();

            storedValuesSpinners = new ArrayList<>();
            storedValuesComboBoxes = new ArrayList<>();

            for (int i = 0; i < labels.size(); i++) {
                storedValuesSpinners.add((Double) spinners.get(i).getValue());
                storedValuesComboBoxes.add((SkillLevel) comboBoxes.get(i).getSelectedItem());
            }
        });

        JButton pasteButton = new JButton(resources.getString("btnPaste.text"));
        pasteButton.addActionListener(e -> {
            spnTargetNumber.setValue(storedTargetNumber);

            for (int i = 0; i < labels.size(); i++) {
                spinners.get(i).setValue(storedValuesSpinners.get(i));
                comboBoxes.get(i).setSelectedItem(storedValuesComboBoxes.get(i));
            }
        });

        final JPanel panel = new CampaignOptionsStandardPanel(panelName, true, panelName);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        // Create a button to toggle the table
        JButton toggleButton = new JButton(resources.getString("btnToggle.text"));
        toggleButton.addActionListener(e -> {
            for (JLabel label : labels) {
                label.setVisible(!label.isVisible());
            }

            for (JComboBox<SkillLevel> comboBox : comboBoxes) {
                comboBox.setVisible(!comboBox.isVisible());
            }

            for (JSpinner spinner : spinners) {
                spinner.setVisible(!spinner.isVisible());
            }
        });

        layout.gridy = 0;
        layout.gridx = 1;
        layout.gridwidth = 2;
        panel.add(toggleButton, layout);
        layout.gridy++;

        layout.gridy++;
        layout.gridx = 1;
        layout.gridwidth = 1;
        panel.add(copyButton, layout);
        layout.gridx++;
        panel.add(pasteButton, layout);

        layout.gridy++;
        layout.gridx = 1;
        panel.add(lblTargetNumber, layout);
        layout.gridx++;
        panel.add(spnTargetNumber, layout);

        for (int i = 0; i < labels.size(); i++) {
            layout.gridy++;
            layout.gridx = 0;
            layout.gridwidth = 1;
            panel.add(labels.get(i), layout);
            layout.gridx++;
            panel.add(spinners.get(i), layout);
            layout.gridx++;
            panel.add(comboBoxes.get(i), layout);
        }

        return panel;
    }

    private static void milestoneActionListener(List<JComboBox<SkillLevel>> comboBoxes, JComboBox<SkillLevel> comboBox) {
        int originIndex = comboBoxes.indexOf(comboBox);

        SkillLevel currentSelection = (SkillLevel) comboBox.getSelectedItem();

        if (currentSelection == null) {
            return;
        }

        if (originIndex != 0) {
            JComboBox<SkillLevel> previousComboBox = comboBoxes.get(originIndex - 1);
            SkillLevel previousSelection = (SkillLevel) previousComboBox.getSelectedItem();

            if (previousSelection != null) {
                if (previousSelection.ordinal() > currentSelection.ordinal()) {
                    comboBox.setSelectedItem(previousSelection);
                } else if (currentSelection.ordinal() > previousSelection.ordinal() + 1) {
                    comboBox.setSelectedItem(SkillLevel.parseFromInteger(previousSelection.ordinal() + 1));
                }
            }
        } else {
            if (comboBox.getSelectedItem() != SkillLevel.NONE) {
                comboBox.setSelectedItem(SkillLevel.ULTRA_GREEN);
            }
        }

        if (originIndex < comboBoxes.size() - 1) {
            originIndex++;
            JComboBox<SkillLevel> nextComboBox = comboBoxes.get(originIndex);
            nextComboBox.setSelectedItem(comboBox.getSelectedItem());
        }
    }



    JPanel abilitiesTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("abilitiesTab",
            getImageDirectory() + "logo_federated_suns.png",
            true);

        // Contents
        chkExtraRandomness = new CampaignOptionsCheckBox("ExtraRandomness");

        pnlPhenotype = createPhenotypePanel();
        pnlRandomAbilities = createAbilityPanel();
        pnlSkillGroups = createSkillGroupPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("abilitiesTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(chkExtraRandomness, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(pnlPhenotype, layout);
        layout.gridx++;
        panel.add(pnlRandomAbilities, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(pnlSkillGroups, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "abilitiesTab");
    }
}
