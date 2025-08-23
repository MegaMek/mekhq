package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType.REQUIREMENTS;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.EnhancedTabbedPane;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathComponentStorage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.dialog.advancedCharacterBuilder.TooltipMouseListenerUtil;

public class LifePathTab {
    private final static MMLogger LOGGER = MMLogger.create(LifePathTab.class);

    private final static String RESOURCE_BUNDLE = getLifePathBuilderResourceBundle();
    private final static int PADDING = getLifePathBuilderPadding();

    private final Factions factions = Factions.getInstance();

    private final LifePathBuilderDialog parent;
    private final EnhancedTabbedPane tabMain;
    private final int gameYear;
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();

    private final Map<Integer, List<String>> storedFactions = new HashMap<>();
    private final Map<Integer, List<UUID>> storedLifePaths = new HashMap<>();
    private final Map<Integer, Map<LifePathCategory, Integer>> storedCategories = new HashMap<>();
    private final Map<Integer, Map<SkillAttribute, Integer>> storedAttributes = new HashMap<>();
    private final Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> storedTraits = new HashMap<>();
    private final Map<Integer, Map<String, Integer>> storedSkills = new HashMap<>();
    private final Map<Integer, Map<String, Integer>> storedAbilities = new HashMap<>();

    LifePathTab(LifePathBuilderDialog parent, EnhancedTabbedPane tabMain, int gameYear,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo) {
        this.parent = parent;
        this.tabMain = tabMain;
        this.gameYear = gameYear;
        this.allAbilityInfo.putAll(allAbilityInfo);
    }

    protected void buildTab(boolean enableGroupControls) {
        JPanel tabRequirements = new JPanel(new BorderLayout());
        tabRequirements.setName("requirements");
        String titleRequirements = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title.requirements");
        tabMain.addTab(titleRequirements, tabRequirements);

        // Panel for the two buttons at the top
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        RoundedJButton btnAddRequirementGroup = getAddGroup(buttonPanel);
        btnAddRequirementGroup.setVisible(enableGroupControls);

        RoundedJButton btnRemoveRequirementGroup = getRemoveGroup(buttonPanel);
        btnRemoveRequirementGroup.setVisible(enableGroupControls);

        RoundedJButton btnDuplicateGroup = getDuplicateGroup(buttonPanel);
        btnDuplicateGroup.setVisible(enableGroupControls);

        // The actual tabbed pane and any button action listeners (we add them here to avoid a situation where they
        // can be called before the pane has been initialized)
        EnhancedTabbedPane tabbedPane = new EnhancedTabbedPane();
        tabbedPane.addChangeListener(e -> btnRemoveRequirementGroup.setEnabled(tabbedPane.getSelectedIndex() != 0));
        btnAddRequirementGroup.addActionListener(e -> {
            addTab(tabbedPane);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        });
        btnRemoveRequirementGroup.addActionListener(e -> removeRequirementGroup(tabbedPane));
        btnDuplicateGroup.addActionListener(e -> duplicateGroup(tabbedPane));

        if (!enableGroupControls) {
            addTab(tabbedPane);
        }

        tabRequirements.add(buttonPanel, BorderLayout.NORTH);
        tabRequirements.add(tabbedPane, BorderLayout.CENTER);
    }

    private RoundedJButton getDuplicateGroup(JPanel buttonPanel) {
        String titleDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.duplicateGroup.label");
        String tooltipDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.duplicateGroup.tooltip");
        RoundedJButton btnDuplicateGroup = new RoundedJButton(titleDuplicateGroup);
        btnDuplicateGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipDuplicateGroup)
        );
        buttonPanel.add(btnDuplicateGroup);
        return btnDuplicateGroup;
    }

    private RoundedJButton getRemoveGroup(JPanel buttonPanel) {
        String titleRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.removeGroup.label");
        String tooltipRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.removeGroup.tooltip");
        RoundedJButton btnRemoveRequirementGroup = new RoundedJButton(titleRemoveGroup);
        btnRemoveRequirementGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipRemoveGroup)
        );
        buttonPanel.add(btnRemoveRequirementGroup);
        return btnRemoveRequirementGroup;
    }

    private RoundedJButton getAddGroup(JPanel buttonPanel) {
        String titleAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addGroup.label");
        String tooltipAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addGroup.tooltip");
        RoundedJButton btnAddRequirementGroup = new RoundedJButton(titleAddGroup);
        btnAddRequirementGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddGroup)
        );
        buttonPanel.add(btnAddRequirementGroup);

        return btnAddRequirementGroup;
    }

    private void removeRequirementGroup(EnhancedTabbedPane tabbedPane) {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // Remove the current tab, unless it's Group 0
        if (selectedIndex > 0) {
            // We need to remove the tab's storage data from the storge map and then re-add the tabs in the
            // correct order (since we're removing a tab, the indexes will shift)
            storedFactions.remove(selectedIndex);
            Map<Integer, List<String>> tempFactions = new HashMap<>();
            for (Map.Entry<Integer, List<String>> entry : storedFactions.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempFactions.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempFactions.put(entry.getKey() - 1, entry.getValue());
                }
            }
            storedFactions.clear();
            storedFactions.putAll(tempFactions);

            storedLifePaths.remove(selectedIndex);
            Map<Integer, List<UUID>> tempLifePaths = new HashMap<>();
            for (Map.Entry<Integer, List<UUID>> entry : storedLifePaths.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempLifePaths.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempLifePaths.put(entry.getKey() - 1, entry.getValue());
                }
            }
            storedLifePaths.clear();
            storedLifePaths.putAll(tempLifePaths);

            storedCategories.remove(selectedIndex);
            Map<Integer, Map<LifePathCategory, Integer>> tempCategories = new HashMap<>();
            for (Map.Entry<Integer, Map<LifePathCategory, Integer>> entry : storedCategories.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempCategories.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempCategories.put(entry.getKey() - 1, entry.getValue());
                }
            }
            storedCategories.clear();
            storedCategories.putAll(tempCategories);

            storedAttributes.remove(selectedIndex);
            Map<Integer, Map<SkillAttribute, Integer>> tempAttributes = new HashMap<>();
            for (Map.Entry<Integer, Map<SkillAttribute, Integer>> entry : storedAttributes.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempAttributes.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempAttributes.put(entry.getKey() - 1, entry.getValue());
                }
            }
            storedAttributes.clear();
            storedAttributes.putAll(tempAttributes);

            storedTraits.remove(selectedIndex);
            Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> tempTraits = new HashMap<>();
            for (Map.Entry<Integer, Map<LifePathEntryDataTraitLookup, Integer>> entry : storedTraits.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempTraits.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempTraits.put(entry.getKey() - 1, entry.getValue());
                }
            }
            storedTraits.clear();
            storedTraits.putAll(tempTraits);

            storedSkills.remove(selectedIndex);
            Map<Integer, Map<String, Integer>> tempSkills = new HashMap<>();
            for (Map.Entry<Integer, Map<String, Integer>> entry : storedSkills.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempSkills.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempSkills.put(entry.getKey() - 1, entry.getValue());
                }
            }
            storedSkills.clear();
            storedSkills.putAll(tempSkills);

            storedAbilities.remove(selectedIndex);
            Map<Integer, Map<String, Integer>> tempAbilities = new HashMap<>();
            for (Map.Entry<Integer, Map<String, Integer>> entry : storedAbilities.entrySet()) {
                if (entry.getKey() < selectedIndex) {
                    tempAbilities.put(entry.getKey(), entry.getValue());
                } else if (entry.getKey() > selectedIndex) {
                    tempAbilities.put(entry.getKey() - 1, entry.getValue());
                }
            }
            storedAbilities.clear();
            storedAbilities.putAll(tempAbilities);

            // Remove the desired tab
            tabbedPane.remove(selectedIndex);

            // Update the progress panel
            parent.updateTxtProgress(gameYear);
        }
    }

    private void duplicateGroup(EnhancedTabbedPane tabbedPane) {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return; // nothing selected, do nothing
        }

        addTab(tabbedPane);

        int newIndex = tabbedPane.getTabCount() - 1;

        List<String> currentFactions = new ArrayList<>(storedFactions.get(selectedIndex));
        storedFactions.put(newIndex, currentFactions);

        List<UUID> currentLifePaths = new ArrayList<>(storedLifePaths.get(selectedIndex));
        storedLifePaths.put(newIndex, currentLifePaths);

        Map<LifePathCategory, Integer> currentCategories = new HashMap<>(storedCategories.get(selectedIndex));
        storedCategories.put(newIndex, currentCategories);

        Map<SkillAttribute, Integer> currentAttributes = new HashMap<>(storedAttributes.get(selectedIndex));
        storedAttributes.put(newIndex, currentAttributes);

        Map<LifePathEntryDataTraitLookup, Integer> currentTraits = new HashMap<>(storedTraits.get(selectedIndex));
        storedTraits.put(newIndex, currentTraits);

        Map<String, Integer> currentSkills = new HashMap<>(storedSkills.get(selectedIndex));
        storedSkills.put(newIndex, currentSkills);

        Map<String, Integer> currentAbilities = new HashMap<>(storedAbilities.get(selectedIndex));
        storedAbilities.put(newIndex, currentAbilities);

        JPanel newTabPanel = (JPanel) tabbedPane.getComponentAt(newIndex);
        JPanel pnlRequirements = (JPanel) newTabPanel.getComponent(1);
        JEditorPane txtRequirements = findEditorPaneByName(pnlRequirements, "txtRequirements");
        if (txtRequirements != null) {
            txtRequirements.setText(buildIndividualProgressText(selectedIndex).toString());
        } else {
            LOGGER.warn("Could not find txtRequirements in duplicateGroup");
        }

        parent.updateTxtProgress(gameYear);

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

    private void addTab(EnhancedTabbedPane tabbedPane) {
        int index = tabbedPane.getTabCount();

        // Create the panel to be used in the tab
        JPanel requirementGroupPanel = new JPanel();
        requirementGroupPanel.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        storedAttributes.put(index, new HashMap<>());

        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits
        storedTraits.put(index, new HashMap<>());

        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        // Skills
        storedSkills.put(index, new HashMap<>());

        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        // SPAs
        storedAbilities.put(index, new HashMap<>());

        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Factions
        storedFactions.put(index, new ArrayList<>());

        String titleAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addFaction.label");
        String tooltipAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addFaction.tooltip");
        RoundedJButton btnAddFaction = new RoundedJButton(titleAddFaction);
        btnAddFaction.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddFaction)
        );
        buttonsPanel.add(btnAddFaction);

        // Life Paths
        storedLifePaths.put(index, new ArrayList<>());

        String titleAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addLifePath.label");
        String tooltipAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addLifePath.tooltip");
        RoundedJButton btnAddLifePath = new RoundedJButton(titleAddLifePath);
        btnAddLifePath.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddLifePath)
        );
        btnAddLifePath.setEnabled(false); // TODO Implement
        buttonsPanel.add(btnAddLifePath);

        // Categories
        storedCategories.put(index, new HashMap<>());

        String titleAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addCategory.label");
        String tooltipAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.requirements.button.addCategory.tooltip");
        RoundedJButton btnAddCategory = new RoundedJButton(titleAddCategory);
        btnAddCategory.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setLblTooltipDisplay, tooltipAddCategory)
        );
        buttonsPanel.add(btnAddCategory);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.requirements.tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane txtRequirements = new JEditorPane();
        txtRequirements.setName("txtRequirements");
        txtRequirements.setContentType("text/html");
        txtRequirements.setEditable(false);
        String progressText = buildIndividualProgressText(gameYear).toString();
        txtRequirements.setText(progressText);

        FastJScrollPane scrollRequirements = new FastJScrollPane(txtRequirements);
        scrollRequirements.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollRequirements.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollRequirements.setBorder(null);

        pnlDisplay.add(scrollRequirements, BorderLayout.CENTER);

        // Add panels and then add Tab
        requirementGroupPanel.add(buttonsPanel, BorderLayout.NORTH);
        requirementGroupPanel.add(pnlDisplay, BorderLayout.CENTER);

        int count = tabbedPane.getComponentCount();
        String titleTab = getFormattedTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.tab." + (count == 0 ? "compulsory" : "optional") + ".formattedLabel");
        tabbedPane.addTab(titleTab, requirementGroupPanel);

        // Action Listeners
        btnAddAttribute.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabbedPane.getSelectedIndex();

            LifePathAttributePicker picker = new LifePathAttributePicker(storedAttributes.get(currentIndex),
                  REQUIREMENTS);
            storedAttributes.put(currentIndex, picker.getSelectedAttributeScores());
            standardizedActions(currentIndex, txtRequirements);

            parent.setVisible(true);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabbedPane.getSelectedIndex();

            LifePathTraitPicker picker = new LifePathTraitPicker(storedTraits.get(currentIndex), REQUIREMENTS);
            storedTraits.put(currentIndex, picker.getSelectedTraitScores());
            standardizedActions(currentIndex, txtRequirements);

            parent.setVisible(true);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabbedPane.getSelectedIndex();

            LifePathSkillPicker picker = new LifePathSkillPicker(storedSkills.get(currentIndex), REQUIREMENTS);
            storedSkills.put(currentIndex, picker.getSelectedSkillLevels());
            standardizedActions(currentIndex, txtRequirements);

            parent.setVisible(true);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabbedPane.getSelectedIndex();

            LifePathSPAPicker picker = new LifePathSPAPicker(storedAbilities.get(currentIndex), allAbilityInfo,
                  REQUIREMENTS);
            storedAbilities.put(currentIndex, picker.getSelectedAbilities());
            standardizedActions(currentIndex, txtRequirements);

            parent.setVisible(true);
        });
        btnAddFaction.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabbedPane.getSelectedIndex();

            LifePathFactionPicker picker = new LifePathFactionPicker(storedFactions.get(currentIndex),
                  gameYear,
                  REQUIREMENTS);
            storedFactions.put(currentIndex, picker.getSelectedFactions());
            standardizedActions(currentIndex, txtRequirements);

            parent.setVisible(true);
        });
        btnAddLifePath.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabbedPane.getSelectedIndex();

            // TODO launch a dialog that lists the current requirements and allows the user to remove one
            standardizedActions(currentIndex, txtRequirements);

            parent.setVisible(true);
        });
        btnAddCategory.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabbedPane.getSelectedIndex();

            LifePathCategoryCountPicker picker = new LifePathCategoryCountPicker(storedCategories.get(currentIndex),
                  REQUIREMENTS);
            storedCategories.put(currentIndex, picker.getSelectedCategoryCounts());
            standardizedActions(currentIndex, txtRequirements);

            parent.setVisible(true);
        });
    }

    private void standardizedActions(int index, JEditorPane txtRequirements) {
        List<String> requirementsText = buildRequirementText();
        txtRequirements.setText(requirementsText.get(index));
        parent.updateTxtProgress(gameYear);
    }

    private List<String> buildRequirementText() {
        List<String> progressText = new ArrayList<>();

        for (int index : storedAbilities.keySet()) {
            StringBuilder individualProgressText = buildIndividualProgressText(index);
            progressText.add(individualProgressText.toString());
        }

        return progressText;
    }

    private StringBuilder buildIndividualProgressText(int index) {
        StringBuilder individualProgressText = new StringBuilder();

        // Factions
        List<String> workingFactions = storedFactions.get(index);
        if (workingFactions != null && !workingFactions.isEmpty()) {
            for (int i = 0; i < workingFactions.size(); i++) {
                String factionCode = workingFactions.get(i);
                Faction faction = factions.getFaction(factionCode);
                if (faction == null) {
                    LOGGER.error("Faction not found: {}", factionCode);
                    continue;
                }
                individualProgressText.append(faction.getFullName(gameYear));
                if (i != workingFactions.size() - 1) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Life Paths
        List<UUID> workingLifePaths = storedLifePaths.get(index);
        if (workingLifePaths != null && !workingLifePaths.isEmpty()) {
            appendComma(individualProgressText);

            for (int i = 0; i < workingLifePaths.size(); i++) {
                // TODO fetch life path from dictionary using ID
                individualProgressText.append("Life Path Name");
                if (i != workingLifePaths.size() - 1) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Categories
        Map<LifePathCategory, Integer> workingCategories = storedCategories.get(index);
        if (workingCategories != null && !workingCategories.isEmpty()) {
            appendComma(individualProgressText);

            int counter = 0;
            int length = workingCategories.size();
            for (Map.Entry<LifePathCategory, Integer> entry : workingCategories.entrySet()) {
                individualProgressText.append(entry.getKey().getDisplayName());
                individualProgressText.append(" ");
                individualProgressText.append(entry.getValue());
                individualProgressText.append("+");
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Attributes
        Map<SkillAttribute, Integer> workingAttributes = storedAttributes.get(index);
        if (workingAttributes != null && !workingAttributes.isEmpty()) {
            appendComma(individualProgressText);

            int counter = 0;
            int length = workingAttributes.size();
            for (Map.Entry<SkillAttribute, Integer> entry : workingAttributes.entrySet()) {
                individualProgressText.append(entry.getKey().getLabel());
                individualProgressText.append(" ");
                individualProgressText.append(entry.getValue());
                individualProgressText.append("+");
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> workingTraits = storedTraits.get(index);
        if (workingTraits != null && !workingTraits.isEmpty()) {
            appendComma(individualProgressText);

            int counter = 0;
            int length = workingTraits.size();
            for (Map.Entry<LifePathEntryDataTraitLookup, Integer> entry : workingTraits.entrySet()) {
                individualProgressText.append(entry.getKey().getDisplayName());
                individualProgressText.append(" ");
                individualProgressText.append(entry.getValue());
                individualProgressText.append("+");
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Skills
        Map<String, Integer> workingSkills = storedSkills.get(index);
        if (workingSkills != null && !workingSkills.isEmpty()) {
            appendComma(individualProgressText);

            int counter = 0;
            int length = workingSkills.size();
            for (Map.Entry<String, Integer> entry : workingSkills.entrySet()) {
                String label = entry.getKey().replace(SkillType.RP_ONLY_TAG, "");

                individualProgressText.append(label);
                individualProgressText.append(" ");
                individualProgressText.append(entry.getValue());
                individualProgressText.append("+");
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // SPAs
        Map<String, Integer> workingAbilities = storedAbilities.get(index);
        if (workingAbilities != null && !workingAbilities.isEmpty()) {
            appendComma(individualProgressText);

            List<String> spas = workingAbilities.keySet().stream().toList();

            for (int i = 0; i < spas.size(); i++) {
                String abilityName = spas.get(i);
                CampaignOptionsAbilityInfo abilityInfo = allAbilityInfo.get(abilityName);
                if (abilityInfo == null) {
                    LOGGER.warn("Could not find AbilityInfo for abilityName: {}", abilityName);
                    continue;
                }

                SpecialAbility ability = abilityInfo.getAbility();
                String label = ability.getDisplayName().replaceAll("\\s*\\(.*$", "");
                individualProgressText.append(label);
                if (i != spas.size() - 1) {
                    individualProgressText.append(", ");
                }
            }
        }
        return individualProgressText;
    }

    private static void appendComma(StringBuilder progressText) {
        if (!progressText.isEmpty()) {
            progressText.append(", ");
        }
    }

    public void updateTabStorage(Map<Integer, LifePathComponentStorage> newData) {
        // Remove old Data
        storedFactions.clear();
        storedLifePaths.clear();
        storedCategories.clear();
        storedAttributes.clear();
        storedTraits.clear();
        storedSkills.clear();
        storedAbilities.clear();

        // Add new Data
        for (Map.Entry<Integer, LifePathComponentStorage> entry : newData.entrySet()) {
            LifePathComponentStorage value = entry.getValue();

            storedFactions.put(entry.getKey(), value.factions());
            storedLifePaths.put(entry.getKey(), value.lifePaths());
            storedCategories.put(entry.getKey(), value.categories());
            storedAttributes.put(entry.getKey(), value.attributes());
            storedTraits.put(entry.getKey(), value.traits());
            storedSkills.put(entry.getKey(), value.skills());
            storedAbilities.put(entry.getKey(), value.abilities());
        }
    }

    public List<String> getTabProgress() {
        return buildRequirementText();
    }
}
