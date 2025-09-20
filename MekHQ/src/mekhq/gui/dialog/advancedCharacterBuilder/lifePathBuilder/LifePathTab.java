package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import static java.lang.Math.min;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderPadding;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathBuilderDialog.getLifePathBuilderResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;

import megamek.common.EnhancedTabbedPane;
import megamek.logging.MMLogger;
import megamek.utilities.FastJScrollPane;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePath;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathEntryDataTraitLookup;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
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
    private final EnhancedTabbedPane tabGlobal;
    private final EnhancedTabbedPane tabLocal = new EnhancedTabbedPane();
    private final int gameYear;
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo = new HashMap<>();
    private final LifePathBuilderTabType tabType;
    private final String tabName;
    private final Map<UUID, LifePath> lifePathLibrary;

    private Map<Integer, Set<String>> storedFactions = new HashMap<>();
    private Map<Integer, Set<UUID>> storedLifePaths = new HashMap<>();
    private Map<Integer, Map<LifePathCategory, Integer>> storedCategories = new HashMap<>();
    private Map<Integer, Map<SkillAttribute, Integer>> storedAttributes = new HashMap<>();
    private Map<Integer, Integer> storedFlexibleAttributes = new HashMap<>();
    private Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> storedTraits = new HashMap<>();
    private Map<Integer, Map<String, Integer>> storedSkills = new HashMap<>();
    private Map<Integer, Map<SkillSubType, Integer>> storedMetaSkills = new HashMap<>();
    private Map<Integer, Map<String, Integer>> storedAbilities = new HashMap<>();

    private final JLabel lblFlexibleXPPicks = new JLabel();
    private final JSpinner spnFlexibleXPPicks = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));

    public Map<Integer, Set<String>> getFactions() {
        return storedFactions;
    }

    public void setFactions(Map<Integer, Set<String>> storedFactions) {
        this.storedFactions = storedFactions;
    }

    public Map<Integer, Set<UUID>> getLifePaths() {
        return storedLifePaths;
    }

    public void setLifePaths(Map<Integer, Set<UUID>> storedLifePaths) {
        this.storedLifePaths = storedLifePaths;
    }

    public Map<Integer, Map<LifePathCategory, Integer>> getCategories() {
        return storedCategories;
    }

    public void setCategories(Map<Integer, Map<LifePathCategory, Integer>> storedCategories) {
        this.storedCategories = storedCategories;
    }

    public Map<Integer, Map<SkillAttribute, Integer>> getAttributes() {
        return storedAttributes;
    }

    public void setAttributes(Map<Integer, Map<SkillAttribute, Integer>> storedAttributes) {
        this.storedAttributes = storedAttributes;
    }

    /**
     * Retrieves the mapping of stored flexible attributes.
     *
     * <p><b>Warning:</b> the returned {@link Integer} can be {@code null}.</p>
     *
     * @return a map where the key represents an identifier and the value represents its associated attribute.
     */
    public Map<Integer, Integer> getFlexibleAttribute() {
        return storedFlexibleAttributes;
    }

    public void setFlexibleAttribute(Map<Integer, Integer> storedFlexibleAttribute) {
        this.storedFlexibleAttributes = storedFlexibleAttribute;
    }

    public Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> getTraits() {
        return storedTraits;
    }

    public void setTraits(Map<Integer, Map<LifePathEntryDataTraitLookup, Integer>> storedTraits) {
        this.storedTraits = storedTraits;
    }

    public Map<Integer, Map<String, Integer>> getSkills() {
        return storedSkills;
    }

    public void setSkills(Map<Integer, Map<String, Integer>> storedSkills) {
        this.storedSkills = storedSkills;
    }

    public Map<Integer, Map<SkillSubType, Integer>> getMetaSkills() {
        return storedMetaSkills;
    }

    public void setMetaSkills(Map<Integer, Map<SkillSubType, Integer>> storedMetaSkills) {
        this.storedMetaSkills = storedMetaSkills;
    }

    public Map<Integer, Map<String, Integer>> getAbilities() {
        return storedAbilities;
    }

    public void setAbilities(Map<Integer, Map<String, Integer>> storedAbilities) {
        this.storedAbilities = storedAbilities;
    }

    public int getPickCount() {
        return min((int) spnFlexibleXPPicks.getValue(), getTabCount());
    }

    public void setPickCount(int pickCount) {
        spnFlexibleXPPicks.setValue(pickCount);
    }

    public int getTabCount() {
        return tabLocal.getTabCount();
    }

    EnhancedTabbedPane getLocalTab() {
        return tabLocal;
    }

    LifePathTab(LifePathBuilderDialog parent, EnhancedTabbedPane tabGlobal, int gameYear,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo, LifePathBuilderTabType tabType,
          Map<UUID, LifePath> lifePathLibrary) {
        this.parent = parent;
        this.tabGlobal = tabGlobal;
        this.gameYear = gameYear;
        this.allAbilityInfo.putAll(allAbilityInfo);
        this.tabType = tabType;
        this.tabName = tabType.getLookupName();
        this.lifePathLibrary = lifePathLibrary;
    }

    protected void buildTab() {
        final boolean enableGroupControls = tabType == LifePathBuilderTabType.REQUIREMENTS ||
                                                  tabType == LifePathBuilderTabType.FLEXIBLE_XP;

        JPanel pnlLocal = new JPanel(new BorderLayout());
        pnlLocal.setName(tabName);
        String title = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.tab.title." + tabName);
        tabGlobal.addTab(title, pnlLocal);

        // Panel for the two buttons at the top
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JPanel panelButtonsRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        RoundedJButton btnAddGroup = getAddGroup(panelButtonsRow);
        btnAddGroup.setVisible(enableGroupControls);
        btnAddGroup.addActionListener(e -> {
            addTab();
            tabLocal.setSelectedIndex(getTabCount() - 1);
        });

        RoundedJButton btnRemoveGroup = getRemoveGroup(panelButtonsRow);
        btnRemoveGroup.setVisible(enableGroupControls);
        btnRemoveGroup.addActionListener(e -> removeGroup());

        RoundedJButton btnDuplicateGroup = getDuplicateGroup(panelButtonsRow);
        btnDuplicateGroup.setVisible(enableGroupControls);
        btnDuplicateGroup.addActionListener(e -> duplicateGroup());

        JPanel panelPicksRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buildFlexiblePicksPanel(panelPicksRow);
        spnFlexibleXPPicks.setVisible(tabType == LifePathBuilderTabType.FLEXIBLE_XP);
        lblFlexibleXPPicks.setVisible(tabType == LifePathBuilderTabType.FLEXIBLE_XP);

        buttonPanel.add(panelButtonsRow);
        buttonPanel.add(panelPicksRow);

        if (!enableGroupControls) {
            addTab();
        }

        pnlLocal.add(buttonPanel, BorderLayout.NORTH);
        pnlLocal.add(tabLocal, BorderLayout.CENTER);
    }

    private void buildFlexiblePicksPanel(JPanel panelPicksRow) {
        String titlePicks = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexible_xp.button.pickCount.label");
        String tooltipPicks = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.flexible_xp.button.pickCount.tooltip");
        lblFlexibleXPPicks.setText(titlePicks);
        lblFlexibleXPPicks.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                parent.setTxtTooltipArea(tooltipPicks);
            }
        });
        spnFlexibleXPPicks.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipPicks)
        );
        spnFlexibleXPPicks.addChangeListener(e -> parent.updateTxtProgress());
        panelPicksRow.add(lblFlexibleXPPicks);
        panelPicksRow.add(spnFlexibleXPPicks);
    }

    private RoundedJButton getDuplicateGroup(JPanel buttonPanel) {
        String titleDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.duplicateGroup.label");
        String tooltipDuplicateGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.duplicateGroup.tooltip");
        RoundedJButton btnDuplicateGroup = new RoundedJButton(titleDuplicateGroup);
        btnDuplicateGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipDuplicateGroup)
        );
        buttonPanel.add(btnDuplicateGroup);
        return btnDuplicateGroup;
    }

    private RoundedJButton getRemoveGroup(JPanel buttonPanel) {
        String titleRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.removeGroup.label");
        String tooltipRemoveGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.removeGroup.tooltip");
        RoundedJButton btnRemoveGroup = new RoundedJButton(titleRemoveGroup);
        btnRemoveGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipRemoveGroup)
        );
        buttonPanel.add(btnRemoveGroup);
        return btnRemoveGroup;
    }

    private RoundedJButton getAddGroup(JPanel buttonPanel) {
        String titleAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addGroup.label");
        String tooltipAddGroup = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addGroup.tooltip");
        RoundedJButton btnAddGroup = new RoundedJButton(titleAddGroup);
        btnAddGroup.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddGroup)
        );
        buttonPanel.add(btnAddGroup);

        return btnAddGroup;
    }

    private void removeGroup() {
        if (tabLocal.getTabCount() == 0) {
            return;
        }

        int selectedIndex = tabLocal.getSelectedIndex();

        // We need to remove the tab's storage data from the storge map and then re-add the tabs in the
        // correct order (since we're removing a tab, the indexes will shift)
        storedFactions.remove(selectedIndex);
        Map<Integer, Set<String>> tempFactions = new HashMap<>();
        for (Map.Entry<Integer, Set<String>> entry : storedFactions.entrySet()) {
            if (entry.getKey() < selectedIndex) {
                tempFactions.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey() > selectedIndex) {
                tempFactions.put(entry.getKey() - 1, entry.getValue());
            }
        }
        storedFactions.clear();
        storedFactions.putAll(tempFactions);

        storedLifePaths.remove(selectedIndex);
        Map<Integer, Set<UUID>> tempLifePaths = new HashMap<>();
        for (Map.Entry<Integer, Set<UUID>> entry : storedLifePaths.entrySet()) {
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

        storedFlexibleAttributes.remove(selectedIndex);
        Map<Integer, Integer> tempFlexibleAttributes = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : storedFlexibleAttributes.entrySet()) {
            if (entry.getKey() < selectedIndex) {
                tempFlexibleAttributes.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey() > selectedIndex) {
                tempFlexibleAttributes.put(entry.getKey() - 1, entry.getValue());
            }
        }
        storedFlexibleAttributes.clear();
        storedFlexibleAttributes.putAll(tempFlexibleAttributes);

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

        storedMetaSkills.remove(selectedIndex);
        Map<Integer, Map<SkillSubType, Integer>> tempMetaSkills = new HashMap<>();
        for (Map.Entry<Integer, Map<SkillSubType, Integer>> entry : storedMetaSkills.entrySet()) {
            if (entry.getKey() < selectedIndex) {
                tempMetaSkills.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey() > selectedIndex) {
                tempMetaSkills.put(entry.getKey() - 1, entry.getValue());
            }
        }
        storedMetaSkills.clear();
        storedMetaSkills.putAll(tempMetaSkills);

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
        tabLocal.remove(selectedIndex);

        // Update the progress panel
        parent.updateTxtProgress();
    }

    private void duplicateGroup() {
        int selectedIndex = tabLocal.getSelectedIndex();
        if (selectedIndex < 0) {
            return; // nothing selected, do nothing
        }

        addTab();

        int newIndex = getTabCount() - 1;

        Set<String> currentFactions = new HashSet<>(storedFactions.get(selectedIndex));
        storedFactions.put(newIndex, currentFactions);

        Set<UUID> currentLifePaths = new HashSet<>(storedLifePaths.get(selectedIndex));
        storedLifePaths.put(newIndex, currentLifePaths);

        Map<LifePathCategory, Integer> currentCategories = new HashMap<>(storedCategories.get(selectedIndex));
        storedCategories.put(newIndex, currentCategories);

        Map<SkillAttribute, Integer> currentAttributes = new HashMap<>(storedAttributes.get(selectedIndex));
        storedAttributes.put(newIndex, currentAttributes);

        Integer currentFlexibleAttributes = storedFlexibleAttributes.get(selectedIndex);
        storedFlexibleAttributes.put(newIndex, currentFlexibleAttributes);

        Map<LifePathEntryDataTraitLookup, Integer> currentTraits = new HashMap<>(storedTraits.get(selectedIndex));
        storedTraits.put(newIndex, currentTraits);

        Map<String, Integer> currentSkills = new HashMap<>(storedSkills.get(selectedIndex));
        storedSkills.put(newIndex, currentSkills);

        Map<SkillSubType, Integer> currentMetaSkills = new HashMap<>(storedMetaSkills.get(selectedIndex));
        storedMetaSkills.put(newIndex, currentMetaSkills);

        Map<String, Integer> currentAbilities = new HashMap<>(storedAbilities.get(selectedIndex));
        storedAbilities.put(newIndex, currentAbilities);

        JPanel pnlNewTab = (JPanel) tabLocal.getComponentAt(newIndex);
        JPanel pnlMain = (JPanel) pnlNewTab.getComponent(1);
        JEditorPane editorProgress = findEditorPaneByName(pnlMain, "editorProgress");
        if (editorProgress != null) {
            editorProgress.setText(buildIndividualProgressText(selectedIndex).toString());
        } else {
            LOGGER.warn("Could not find editorProgress in duplicateGroup");
        }

        parent.updateTxtProgress();

        tabLocal.setSelectedIndex(newIndex);
    }

    JEditorPane findEditorPaneByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (component instanceof JEditorPane && name.equals(component.getName())) {
                return (JEditorPane) component;
            } else if (component instanceof Container) {
                JEditorPane result = findEditorPaneByName((Container) component, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    void addTab() {
        final boolean includeSupplementaryButtons = tabType == LifePathBuilderTabType.REQUIREMENTS ||
                                                          tabType == LifePathBuilderTabType.EXCLUSIONS;

        int index = getTabCount();

        // Create the panel to be used in the tab
        JPanel groupPanel = new JPanel();
        groupPanel.setLayout(new BorderLayout());

        // Panel for the 8 buttons (using GridLayout: 2 rows, 4 columns)
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 4, PADDING, PADDING));

        // Attributes
        storedAttributes.put(index, new HashMap<>());
        storedFlexibleAttributes.put(index, null);

        String titleAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addAttribute.label");
        String tooltipAddAttribute = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addAttribute.tooltip");
        RoundedJButton btnAddAttribute = new RoundedJButton(titleAddAttribute);
        btnAddAttribute.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddAttribute)
        );
        buttonsPanel.add(btnAddAttribute);

        // Traits
        storedTraits.put(index, new HashMap<>());

        String titleAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addTrait.label");
        String tooltipAddTrait = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addTrait.tooltip");
        RoundedJButton btnAddTrait = new RoundedJButton(titleAddTrait);
        btnAddTrait.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddTrait)
        );
        buttonsPanel.add(btnAddTrait);

        // Skills
        storedSkills.put(index, new HashMap<>());
        storedMetaSkills.put(index, new HashMap<>());

        String titleAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addSkill.label");
        String tooltipAddSkill = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addSkill.tooltip");
        RoundedJButton btnAddSkill = new RoundedJButton(titleAddSkill);
        btnAddSkill.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddSkill)
        );
        buttonsPanel.add(btnAddSkill);

        // SPAs
        storedAbilities.put(index, new HashMap<>());

        String titleAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addSPA.label");
        String tooltipAddSPA = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addSPA.tooltip");
        RoundedJButton btnAddSPA = new RoundedJButton(titleAddSPA);
        btnAddSPA.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddSPA)
        );
        buttonsPanel.add(btnAddSPA);

        // Factions
        storedFactions.put(index, new HashSet<>());

        String titleAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addFaction.label");
        String tooltipAddFaction = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addFaction.tooltip");
        RoundedJButton btnAddFaction = new RoundedJButton(titleAddFaction);
        btnAddFaction.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddFaction)
        );
        btnAddFaction.setVisible(includeSupplementaryButtons);
        buttonsPanel.add(btnAddFaction);

        // Life Paths
        storedLifePaths.put(index, new HashSet<>());

        String titleAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addLifePath.label");
        String tooltipAddLifePath = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addLifePath.tooltip");
        RoundedJButton btnAddLifePath = new RoundedJButton(titleAddLifePath);
        btnAddLifePath.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddLifePath)
        );
        btnAddLifePath.setVisible(includeSupplementaryButtons);
        buttonsPanel.add(btnAddLifePath);

        // Categories
        storedCategories.put(index, new HashMap<>());

        String titleAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog.button.addCategory.label");
        String tooltipAddCategory = getTextAt(RESOURCE_BUNDLE,
              "LifePathBuilderDialog." + tabName + ".button.addCategory.tooltip");
        RoundedJButton btnAddCategory = new RoundedJButton(titleAddCategory);
        btnAddCategory.addMouseListener(
              TooltipMouseListenerUtil.forTooltip(parent::setTxtTooltipArea, tooltipAddCategory)
        );
        btnAddCategory.setVisible(includeSupplementaryButtons);
        buttonsPanel.add(btnAddCategory);

        // Panel below the buttons
        JPanel pnlDisplay = new JPanel();
        pnlDisplay.setLayout(new BorderLayout());

        String titleBorder = getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog." + tabName + ".tab.title");
        pnlDisplay.setBorder(RoundedLineBorder.createRoundedLineBorder(titleBorder));

        JEditorPane editorProgress = new JEditorPane();
        editorProgress.setName("editorProgress");
        editorProgress.setContentType("text/html");
        editorProgress.setEditable(false);
        String progressText = buildIndividualProgressText(gameYear).toString();
        editorProgress.setText(progressText);

        FastJScrollPane scrollMain = new FastJScrollPane(editorProgress);
        scrollMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollMain.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollMain.setBorder(null);

        pnlDisplay.add(scrollMain, BorderLayout.CENTER);

        // Add panels and then add Tab
        groupPanel.add(buttonsPanel, BorderLayout.NORTH);
        groupPanel.add(pnlDisplay, BorderLayout.CENTER);

        String title = switch (tabType) {
            case FIXED_XP, EXCLUSIONS -> "";
            case FLEXIBLE_XP, REQUIREMENTS -> "<html><b>" + index + "</b></html>";
        };
        tabLocal.addTab(title, groupPanel);

        // Action Listeners
        btnAddAttribute.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabLocal.getSelectedIndex();

            LifePathAttributePicker picker = new LifePathAttributePicker(storedAttributes.get(currentIndex),
                  storedFlexibleAttributes.get(currentIndex), tabType);
            storedAttributes.put(currentIndex, picker.getSelectedAttributeScores());
            storedFlexibleAttributes.put(currentIndex, picker.getFlexibleAttribute());
            standardizedActions(currentIndex, editorProgress);

            parent.setVisible(true);
        });
        btnAddTrait.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabLocal.getSelectedIndex();

            LifePathTraitPicker picker = new LifePathTraitPicker(storedTraits.get(currentIndex), tabType);
            storedTraits.put(currentIndex, picker.getSelectedTraitScores());
            standardizedActions(currentIndex, editorProgress);

            parent.setVisible(true);
        });
        btnAddSkill.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabLocal.getSelectedIndex();

            LifePathSkillPicker picker = new LifePathSkillPicker(storedSkills.get(currentIndex),
                  storedMetaSkills.get(currentIndex), tabType);
            storedSkills.put(currentIndex, picker.getSelectedSkillLevels());
            storedMetaSkills.put(currentIndex, picker.getSelectedMetaSkillLevels());
            standardizedActions(currentIndex, editorProgress);

            parent.setVisible(true);
        });
        btnAddSPA.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabLocal.getSelectedIndex();

            LifePathSPAPicker picker = new LifePathSPAPicker(storedAbilities.get(currentIndex), allAbilityInfo,
                  tabType);
            storedAbilities.put(currentIndex, picker.getSelectedAbilities());
            standardizedActions(currentIndex, editorProgress);

            parent.setVisible(true);
        });
        btnAddFaction.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabLocal.getSelectedIndex();

            LifePathFactionPicker picker = new LifePathFactionPicker(storedFactions.get(currentIndex),
                  gameYear,
                  tabType);
            storedFactions.put(currentIndex, picker.getSelectedFactions());
            standardizedActions(currentIndex, editorProgress);

            parent.setVisible(true);
        });
        btnAddLifePath.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabLocal.getSelectedIndex();

            LifePathLifePathPicker picker = new LifePathLifePathPicker(storedLifePaths.get(currentIndex),
                  lifePathLibrary, tabType);
            storedLifePaths.put(currentIndex, picker.getSelectedLifePaths());
            standardizedActions(currentIndex, editorProgress);

            parent.setVisible(true);
        });
        btnAddCategory.addActionListener(e -> {
            parent.setVisible(false);

            int currentIndex = tabLocal.getSelectedIndex();

            LifePathCategoryCountPicker picker = new LifePathCategoryCountPicker(storedCategories.get(currentIndex),
                  tabType);
            storedCategories.put(currentIndex, picker.getSelectedCategoryCounts());
            standardizedActions(currentIndex, editorProgress);

            parent.setVisible(true);
        });
    }

    private void standardizedActions(int index, JEditorPane newText) {
        List<String> textArray = buildProgressText();
        newText.setText(textArray.get(index));
        parent.updateTxtProgress();
    }

    public List<String> buildProgressText() {
        List<String> progressText = new ArrayList<>();

        for (int index : storedAbilities.keySet()) {
            StringBuilder individualProgressText = buildIndividualProgressText(index);
            progressText.add(individualProgressText.toString());
        }

        return progressText;
    }

    StringBuilder buildIndividualProgressText(int index) {
        StringBuilder individualProgressText = new StringBuilder();

        boolean isXP = tabType == LifePathBuilderTabType.FIXED_XP || tabType == LifePathBuilderTabType.FLEXIBLE_XP;
        final String TEXT_LEAD_POSITIVE = isXP ? " +" : " ";
        final String TEXT_LEAD_NEGATIVE = " ";
        final String TEXT_TRAIL = isXP ? " XP" : "+";

        // Factions
        Set<String> workingFactions = storedFactions.get(index);
        if (workingFactions != null && !workingFactions.isEmpty()) {
            int workingIndex = 0;
            int total = workingFactions.size();
            for (String factionCode : workingFactions) {
                Faction faction = factions.getFaction(factionCode);
                if (faction == null) {
                    LOGGER.error("Faction not found: {}", factionCode);
                    continue;
                }
                individualProgressText.append(faction.getFullName(gameYear));
                if (workingIndex != total - 1) {
                    individualProgressText.append(", ");
                }
                workingIndex++;
            }
        }

        // Life Paths
        Set<UUID> workingLifePaths = storedLifePaths.get(index);
        if (workingLifePaths != null && !workingLifePaths.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingLifePaths.size();
            for (UUID id : workingLifePaths) {
                LifePath lifePath = lifePathLibrary.get(id);
                if (lifePath == null) {
                    LOGGER.error("Life Path not found: {}", id);
                    continue;
                }
                individualProgressText.append(lifePath.name())
                      .append(" (")
                      .append(lifePath.lifeStages().stream()
                                    .map(ATOWLifeStage::getDisplayName)
                                    .collect(Collectors.joining(",")))
                      .append(")");
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Categories
        Map<LifePathCategory, Integer> workingCategories = storedCategories.get(index);
        if (workingCategories != null && !workingCategories.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingCategories.size();
            for (Map.Entry<LifePathCategory, Integer> entry : workingCategories.entrySet()) {
                int value = entry.getValue();

                individualProgressText.append(entry.getKey().getDisplayName());
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Attributes
        Map<SkillAttribute, Integer> workingAttributes = storedAttributes.get(index);
        if (workingAttributes != null && !workingAttributes.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingAttributes.size();
            for (Map.Entry<SkillAttribute, Integer> entry : workingAttributes.entrySet()) {
                int value = entry.getValue();

                individualProgressText.append(entry.getKey().getLabel());
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        Integer workingFlexibleAttributes = storedFlexibleAttributes.get(index);
        if (workingFlexibleAttributes != null && !storedFlexibleAttributes.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;

            individualProgressText.append(getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.flexibleAttribute.label"));
            individualProgressText.append(workingFlexibleAttributes >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
            individualProgressText.append(workingFlexibleAttributes);
            individualProgressText.append(TEXT_TRAIL);
            counter++;
        }

        // Traits
        Map<LifePathEntryDataTraitLookup, Integer> workingTraits = storedTraits.get(index);
        if (workingTraits != null && !workingTraits.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingTraits.size();
            for (Map.Entry<LifePathEntryDataTraitLookup, Integer> entry : workingTraits.entrySet()) {
                int value = entry.getValue();

                individualProgressText.append(entry.getKey().getDisplayName());
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // Skills
        Map<String, Integer> workingSkills = storedSkills.get(index);
        if (workingSkills != null && !workingSkills.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingSkills.size();
            for (Map.Entry<String, Integer> entry : workingSkills.entrySet()) {
                String label = entry.getKey().replace(SkillType.RP_ONLY_TAG, "");
                int value = entry.getValue();

                individualProgressText.append(label);
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        Map<SkillSubType, Integer> workingMetaSkills = storedMetaSkills.get(index);
        if (workingMetaSkills != null && !workingMetaSkills.isEmpty()) {
            appendBreaker(individualProgressText);

            int counter = 0;
            int length = workingMetaSkills.size();
            for (Map.Entry<SkillSubType, Integer> entry : workingMetaSkills.entrySet()) {
                String label = entry.getKey().getDisplayName();
                int value = entry.getValue();

                individualProgressText.append(label);
                individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                individualProgressText.append(entry.getValue());
                individualProgressText.append(TEXT_TRAIL);
                counter++;
                if (counter != length) {
                    individualProgressText.append(", ");
                }
            }
        }

        // SPAs
        Map<String, Integer> workingAbilities = storedAbilities.get(index);
        if (workingAbilities != null && !workingAbilities.isEmpty()) {
            appendBreaker(individualProgressText);

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
                if (isXP) {
                    Integer value = workingAbilities.get(abilityName);
                    if (value != null) {
                        individualProgressText.append(label);
                        individualProgressText.append(value >= 0 ? TEXT_LEAD_POSITIVE : TEXT_LEAD_NEGATIVE);
                        individualProgressText.append(value);
                        individualProgressText.append(TEXT_TRAIL);
                    } else {
                        LOGGER.warn("Could not find value for abilityName in working abilities: {}", abilityName);
                    }
                } else {
                    individualProgressText.append(label);
                }

                if (i != spas.size() - 1) {
                    individualProgressText.append(", ");
                }
            }
        }
        return individualProgressText;
    }

    private void appendBreaker(StringBuilder progressText) {
        final boolean isFixedXP = tabType == LifePathBuilderTabType.FIXED_XP;
        final String BREAKER = isFixedXP ? "&#9654; " : ", ";

        if (isFixedXP) {
            if (!progressText.isEmpty()) {
                progressText.append("<br>");
            }

            progressText.append(BREAKER);
            return;
        }

        if (!progressText.isEmpty()) {
            progressText.append(BREAKER);
        }
    }

    public void resetTab() {
        tabLocal.removeAll();

        storedFactions.clear();
        storedLifePaths.clear();
        storedCategories.clear();
        storedAttributes.clear();
        storedFlexibleAttributes.clear();
        storedTraits.clear();
        storedSkills.clear();
        storedMetaSkills.clear();
        storedAbilities.clear();
        spnFlexibleXPPicks.setValue(0);

        parent.updateTxtProgress();
    }
}
