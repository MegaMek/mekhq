/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static java.lang.Math.min;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType.FLEXIBLE_XP;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import megamek.common.EnhancedTabbedPane;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathComponentStorage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

public class LifePathBuilderTabFlexibleXP {
    private final static MMLogger LOGGER = MMLogger.create(LifePathBuilderTabFlexibleXP.class);

    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    private final LifePathBuilderDialog parent;
    private final Map<Integer, LifePathComponentStorage> flexibleXPTabStorageMap = new HashMap<>();
    private final Map<Integer, String> flexibleXPTabTextMap = new HashMap<>();
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo;
    JSpinner spnPicks;

    public Map<Integer, LifePathComponentStorage> getFlexibleXPTabStorageMap() {
        return flexibleXPTabStorageMap;
    }

    public void setFlexibleXPTabStorageMap(Map<Integer, LifePathComponentStorage> flexibleXPTabStorageMap) {
        this.flexibleXPTabStorageMap.clear();
        this.flexibleXPTabStorageMap.putAll(flexibleXPTabStorageMap);
    }

    public Map<Integer, String> getFlexibleXPTabTextMap() {
        return flexibleXPTabTextMap;
    }

    public int getPickCount() {
        return min((int) spnPicks.getValue(), flexibleXPTabStorageMap.size());
    }

    LifePathBuilderTabFlexibleXP(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo) {
        this.parent = parent;
        this.allAbilityInfo = allAbilityInfo;

        JPanel tabFlexibleXP = new JPanel(new BorderLayout());
        tabFlexibleXP.setName("flexibleXP");
        String titleFlexibleXP = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.flexibleXP");
        tabMain.addTab(titleFlexibleXP, tabFlexibleXP);

        // Panel for the buttons and spinner at the top
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JPanel panelButtonsRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        String titleAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addGroup.label");
        String tooltipAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addGroup.tooltip");
        RoundedJButton btnAddFlexibleXPGroup = new RoundedJButton(titleAddGroup);
        btnAddFlexibleXPGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddGroup)
        );
        panelButtonsRow.add(btnAddFlexibleXPGroup);

        String titleRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.removeGroup.label");
        String tooltipRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.removeGroup.tooltip");
        RoundedJButton btnRemoveFlexibleXPGroup = new RoundedJButton(titleRemoveGroup);
        btnRemoveFlexibleXPGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipRemoveGroup)
        );
        panelButtonsRow.add(btnRemoveFlexibleXPGroup);

        String titleDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.duplicateGroup.label");
        String tooltipDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.duplicateGroup.tooltip");
        RoundedJButton btnDuplicateGroup = new RoundedJButton(titleDuplicateGroup);
        btnDuplicateGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipDuplicateGroup)
        );
        panelButtonsRow.add(btnDuplicateGroup);

        JPanel panelPicksRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        String titlePicks = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.pickCount.label");
        String tooltipPicks = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.pickCount.tooltip");
        JLabel lblPicks = new JLabel(titlePicks);
        spnPicks = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        lblPicks.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipPicks)
        );
        spnPicks.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipPicks)
        );
        panelPicksRow.add(lblPicks);
        panelPicksRow.add(spnPicks);

        buttonPanel.add(panelButtonsRow);
        buttonPanel.add(panelPicksRow);

        // The actual tabbed pane and any button action listeners (we add them here to avoid a situation where they
        // can be called before the pane has been initialized)
        EnhancedTabbedPane tabbedPane = new EnhancedTabbedPane();
        tabbedPane.addChangeListener(e -> btnRemoveFlexibleXPGroup.setEnabled(tabbedPane.getSelectedIndex() != 0));
        btnAddFlexibleXPGroup.addActionListener(e -> {
            addFlexibleXPTab(tabbedPane, null);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        });
        btnRemoveFlexibleXPGroup.addActionListener(e -> removeFlexibleXPGroup(tabbedPane));
        btnDuplicateGroup.addActionListener(e -> duplicateGroup(tabbedPane));

        // Add 'Group 0' - this group is required
        addFlexibleXPTab(tabbedPane, null);

        tabFlexibleXP.add(buttonPanel, BorderLayout.NORTH);
        tabFlexibleXP.add(tabbedPane, BorderLayout.CENTER);
    }

    private void removeFlexibleXPGroup(EnhancedTabbedPane tabbedPane) {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // Remove the current tab, unless it's Group 0
        if (selectedIndex > 0) {
            // We need to remove the tab's storage data from the storge map and then re-add the tabs in the
            // correct order (since we're removing a tab, the indexes will shift)
            Map<Integer, LifePathComponentStorage> tempStorageMap = new HashMap<>();
            for (Map.Entry<Integer, LifePathComponentStorage> entry : flexibleXPTabStorageMap.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempStorageMap.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempStorageMap.put(entry.getKey() - 1, entry.getValue());
                }
            }

            flexibleXPTabStorageMap.clear();
            flexibleXPTabStorageMap.putAll(tempStorageMap);

            Map<Integer, String> tempTextMap = new HashMap<>();
            for (Map.Entry<Integer, String> entry : flexibleXPTabTextMap.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempTextMap.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempTextMap.put(entry.getKey() - 1, entry.getValue());
                }
            }

            flexibleXPTabTextMap.clear();
            flexibleXPTabTextMap.putAll(tempTextMap);

            // Remove the desired tab
            tabbedPane.remove(selectedIndex);

            // Update the progress panel
            parent.updateTxtProgress();
        }
    }

    private void duplicateGroup(EnhancedTabbedPane tabbedPane) {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return; // nothing selected, do nothing
        }

        LifePathComponentStorage currentValues = flexibleXPTabStorageMap.get(selectedIndex);
        String currentText = flexibleXPTabTextMap.get(selectedIndex);

        addFlexibleXPTab(tabbedPane, currentValues);
        int newIndex = tabbedPane.getTabCount() - 1;

        flexibleXPTabStorageMap.put(newIndex, currentValues);
        flexibleXPTabTextMap.put(newIndex, currentText);

        JPanel newTabPanel = (JPanel) tabbedPane.getComponentAt(newIndex);
        JPanel pnlFlexibleXP = (JPanel) newTabPanel.getComponent(1);
        JEditorPane txtFlexibleXP = findEditorPaneByName(pnlFlexibleXP, "txtFlexibleXP");
        if (txtFlexibleXP != null) {
            txtFlexibleXP.setText(currentText);
        } else {
            LOGGER.warn("Could not find txtFlexibleXP in duplicateGroup");
        }

        parent.updateTxtProgress();

        tabbedPane.setSelectedIndex(newIndex);
    }

    private JEditorPane findEditorPaneByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (component instanceof JEditorPane && name.equals(component.getName())) {
                return (JEditorPane) component;
            } else if (component instanceof Container) {
                JEditorPane result = findEditorPaneByName((Container) component, name);
                if (result != null) {return result;}
            }
        }
        return null;
    }

    private void addFlexibleXPTab(EnhancedTabbedPane tabbedPane, @Nullable LifePathComponentStorage storage) {
        boolean hasStorage = storage != null;

        int index = tabbedPane.getTabCount();

        // Create the panel to be used in the tab
        JPanel flexibleXPGroupPanel = new JPanel();
        flexibleXPGroupPanel.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        Map<SkillAttribute, Integer> attributes = new HashMap<>();
        if (hasStorage) {
            attributes.putAll(storage.attributes());
        }

        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> traits = new HashMap<>();
        if (hasStorage) {
            traits.putAll(storage.traits());
        }

        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        // Skills
        Map<String, Integer> skills = new HashMap<>();
        if (hasStorage) {
            skills.putAll(storage.skills());
        }

        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        // SPAs
        Map<String, Integer> abilities = new HashMap<>();
        if (hasStorage) {
            abilities.putAll(storage.abilities());
        }

        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexibleXP.button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.flexibleXP.tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane txtFlexibleXP = new JEditorPane();
        txtFlexibleXP.setName("txtFlexibleXP");
        txtFlexibleXP.setContentType("text/html");
        txtFlexibleXP.setEditable(false);
        LifePathComponentStorage initialStorage = getFlexibleXPStorage(
              attributes,
              traits,
              skills,
              abilities);
        String initialFlexibleXPText = buildFlexibleXPText(initialStorage);
        flexibleXPTabStorageMap.put(index, initialStorage);
        flexibleXPTabTextMap.put(index, initialFlexibleXPText);

        txtFlexibleXP.setText(initialFlexibleXPText);

        FastJScrollPane scrollFlexibleXP = new FastJScrollPane(txtFlexibleXP);
        scrollFlexibleXP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollFlexibleXP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollFlexibleXP.setBorder(null);

        pnlDisplay.add(scrollFlexibleXP, BorderLayout.CENTER);

        // Action Listeners
        btnAddAttribute.addActionListener(e -> {
            parent.setVisible(false);

            LifePathAttributePicker picker = new LifePathAttributePicker(attributes, FLEXIBLE_XP);
            attributes.clear();
            attributes.putAll(picker.getSelectedAttributeScores());

            standardizedActions(index, attributes, traits, skills, abilities, txtFlexibleXP, initialStorage);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);
            LifePathTraitPicker picker = new LifePathTraitPicker(traits, FLEXIBLE_XP);
            traits.clear();
            traits.putAll(picker.getSelectedTraitScores());

            standardizedActions(index, attributes, traits, skills, abilities, txtFlexibleXP, initialStorage);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSkillPicker picker = new LifePathSkillPicker(skills, FLEXIBLE_XP);
            skills.clear();
            skills.putAll(picker.getSelectedSkillLevels());

            standardizedActions(index, attributes, traits, skills, abilities, txtFlexibleXP, initialStorage);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);
            LifePathSPAPicker picker = new LifePathSPAPicker(abilities, allAbilityInfo, FLEXIBLE_XP);
            abilities.clear();
            abilities.putAll(picker.getSelectedAbilities());

            standardizedActions(index, attributes, traits, skills, abilities, txtFlexibleXP, initialStorage);
        });

        // Add panels and then add Tab
        flexibleXPGroupPanel.add(buttonsPanel, BorderLayout.NORTH);
        flexibleXPGroupPanel.add(pnlDisplay, BorderLayout.CENTER);

        String titleTab = getFormattedTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.tab.optional.formattedLabel");
        tabbedPane.addTab(titleTab, flexibleXPGroupPanel);
    }

    private void standardizedActions(int index, Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<String, Integer> skills,
          Map<String, Integer> abilities, JEditorPane txtFlexibleXP, LifePathComponentStorage initialStorage) {
        LifePathComponentStorage storage = getFlexibleXPStorage(
              attributes,
              traits,
              skills,
              abilities);
        String flexibleXPText = buildFlexibleXPText(storage);
        txtFlexibleXP.setText(flexibleXPText);

        flexibleXPTabStorageMap.put(index, initialStorage);
        flexibleXPTabTextMap.put(index, flexibleXPText);
        parent.updateTxtProgress();

        parent.setVisible(true);
    }

    private static LifePathComponentStorage getFlexibleXPStorage(Map<SkillAttribute, Integer> attributes,
          Map<LifePathEntryDataTraitLookup, Integer> traits, Map<String, Integer> skills,
          Map<String, Integer> abilities) {
        Map<String, Integer> skillNames = new HashMap<>();
        for (String skill : skills.keySet()) {
            skillNames.put(skill, skills.get(skill));
        }

        return new LifePathComponentStorage(0,
              new ArrayList<>(),
              new ArrayList<>(),
              new HashMap<>(),
              attributes,
              traits,
              skillNames,
              abilities);
    }

    private String buildFlexibleXPText(LifePathComponentStorage storage) {
        StringBuilder progressText = new StringBuilder();

        // Attributes
        Map<SkillAttribute, Integer> attributes = storage.attributes();
        if (!attributes.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = attributes.size();
            for (Map.Entry<SkillAttribute, Integer> entry : attributes.entrySet()) {
                int value = entry.getValue();

                progressText.append(entry.getKey().getLabel());
                progressText.append(value >= 0 ? " +" : " ");
                progressText.append(entry.getValue());
                progressText.append(" XP");
                counter++;
                if (counter != length) {
                    progressText.append(", ");
                }
            }
        }

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> traits = storage.traits();
        if (!traits.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = traits.size();
            for (Map.Entry<LifePathEntryDataTraitLookup, Integer> entry : traits.entrySet()) {
                int value = entry.getValue();

                progressText.append(entry.getKey().getDisplayName());
                progressText.append(value >= 0 ? " +" : " ");
                progressText.append(entry.getValue());
                progressText.append(" XP");
                counter++;
                if (counter != length) {
                    progressText.append(", ");
                }
            }
        }

        // Skills
        Map<String, Integer> skills = storage.skills();
        if (!skills.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = skills.size();
            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                int value = entry.getValue();

                String label = entry.getKey().replace(SkillType.RP_ONLY_TAG, "");

                progressText.append(label);
                progressText.append(value >= 0 ? " +" : " ");
                progressText.append(value);
                progressText.append(" XP");
                counter++;
                if (counter != length) {
                    progressText.append(", ");
                }
            }
        }

        // SPAs
        Map<String, Integer> selectedSPAs = storage.abilities();
        if (!selectedSPAs.isEmpty()) {
            appendComma(progressText);

            int counter = 0;
            int length = selectedSPAs.size();
            for (Map.Entry<String, Integer> entry : selectedSPAs.entrySet()) {
                String abilityName = entry.getKey();
                CampaignOptionsAbilityInfo abilityInfo = allAbilityInfo.get(abilityName);
                if (abilityInfo == null) {
                    LOGGER.warn("Could not find AbilityInfo for abilityName: {}", abilityName);
                    continue;
                }

                SpecialAbility ability = abilityInfo.getAbility();
                String label = ability.getDisplayName().replaceAll("\\s*\\(.*$", "");

                int value = entry.getValue();

                progressText.append(label);
                progressText.append(value >= 0 ? " +" : " ");
                progressText.append(entry.getValue());
                progressText.append(" XP");
                counter++;
                if (counter != length) {
                    progressText.append(", ");
                }
            }
        }

        return progressText.toString();
    }

    private static void appendComma(StringBuilder progressText) {
        if (!progressText.isEmpty()) {
            progressText.append(", ");
        }
    }
}
