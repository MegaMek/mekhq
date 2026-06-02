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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static mekhq.utilities.spaUtilities.SpaUtilities.getSpaCategory;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_CREATION_ONLY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_FLAW;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.COMBAT_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.MANEUVERING_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.UTILITY_ABILITY;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import mekhq.CampaignPreset;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillPrerequisite;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.campaignOptions.components.AbilitySelectorDialog;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.SectionHeaderControlProvider;
import mekhq.gui.campaignOptions.components.SkillPrerequisitesDialog;
import mekhq.utilities.ReportingUtilities;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

/**
 * The {@code AbilitiesTab} class represents a GUI tab for configuring and managing special abilities in a campaign.
 * This class handles the initialization, categorization, and display of abilities, providing functionality for
 * enabling, disabling, and customizing abilities within the combat, maneuvering, and utility categories.
 * <p>
 * This tab is used as part of the MekHQ campaign options UI for managing personnel-related abilities.
 */
public class AbilitiesTab {
    /**
     * Pre-scaling width (in pixels) for each of the three prerequisite/incompatible/removed list columns. Keeps long
     * lists wrapping within the column instead of expanding the ability panel past the page width cap.
     */
    private static final int ABILITY_COLUMN_TEXT_WIDTH = 150;

    private ArrayList<String> level3Abilities;
    private Map<String, CampaignOptionsAbilityInfo> allAbilityInfo;
    private Map<AbilityCategory, JPanel> createdCategoryTabs;

    /**
     * Constructor for the {@code AbilitiesTab} class. Initializes the tab by creating containers for ability categories
     * and populating them with related ability information.
     */
    public AbilitiesTab() {
        initialize();
    }

    /**
     * Initializes the internal data structures and UI components for managing abilities. Prepares the containers for
     * each ability category (combat, maneuvering, utility) and populates the {@code allAbilityInfo} map by processing
     * an initial set of abilities.
     */
    private void initialize() {
        allAbilityInfo = new HashMap<>();
        level3Abilities = new ArrayList<>();
        createdCategoryTabs = new EnumMap<>(AbilityCategory.class);
        buildAllAbilityInfo(SpecialAbility.getSpecialAbilities());
    }

    /**
     * Builds and initializes the {@code allAbilityInfo} map, which holds information about abilities categorized into
     * combat, maneuvering, and utility abilities. Ensures missing abilities are also included and updates the display.
     *
     * @param abilities A map of active special abilities keyed by name.
     */
    public void buildAllAbilityInfo(Map<String, SpecialAbility> abilities) {
        // Remove old data
        allAbilityInfo.clear();
        level3Abilities.clear();

        // Build list of Level 3 abilities
        PersonnelOptions personnelOptions = new PersonnelOptions();
        for (final Enumeration<IOptionGroup> i = personnelOptions.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (final Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                level3Abilities.add(option.getName());
            }
        }

        // Build abilities
        buildAbilityInfo(abilities, true);

        Map<String, SpecialAbility> allSpecialAbilities = SpecialAbility.getDefaultSpecialAbilities();
        Map<String, SpecialAbility> missingAbilities = new HashMap<>();

        for (SpecialAbility ability : allSpecialAbilities.values()) {
            if (!allAbilityInfo.containsKey(ability.getName())) {
                missingAbilities.put(ability.getName(), ability);
            }
        }

        if (!missingAbilities.isEmpty()) {
            buildAbilityInfo(missingAbilities, false);
        }

        refreshCreatedTabs();
    }

    /**
     * Refreshes and updates created tabs related to abilities by clearing their contents and reloading the data.
     */
    private void refreshCreatedTabs() {
        for (AbilityCategory category : new ArrayList<>(createdCategoryTabs.keySet())) {
            refreshTabContents(category);
        }
    }

    /**
     * Updates the contents of a specific ability category tab by rebuilding its layout and content based on the
     * category.
     *
     * @param category The ability category associated with the tab.
     */
    private void refreshTabContents(AbilityCategory category) {
        JPanel tab = createdCategoryTabs.get(category);
        if (tab == null) {
            return;
        }

        tab.removeAll();
        JPanel newContents = createAbilitiesPage(category);

        // Add new content to the same panel
        tab.setLayout(new BorderLayout());
        tab.add(newContents, BorderLayout.CENTER);

        tab.revalidate();
        tab.repaint();
    }

    /**
     * Populates the {@code allAbilityInfo} map with special ability information and categorizes abilities based on
     * their type (combat, maneuvering, utility).
     *
     * @param abilities A map of abilities to be processed.
     * @param isEnabled {@code true} if these abilities should start as enabled; otherwise, {@code false}.
     */
    private void buildAbilityInfo(Map<String, SpecialAbility> abilities, boolean isEnabled) {
        for (Entry<String, SpecialAbility> entry : abilities.entrySet()) {
            SpecialAbility clonedAbility = entry.getValue().clone();
            String abilityName = clonedAbility.getName();
            AbilityCategory category = getSpaCategory(clonedAbility);

            if (!level3Abilities.contains(abilityName)) {
                continue;
            }

            // Mark the ability as active
            allAbilityInfo.put(abilityName,
                  new CampaignOptionsAbilityInfo(abilityName, clonedAbility, isEnabled, category));
        }
    }

    /**
     * Creates a new abilities configuration tab for a specific category. This includes adding controls, headers, and
     * listed abilities.
     *
     * @param abilityCategory The {@code AbilityCategory} to generate a tab for.
     *
     * @return A {@code JPanel} representing the generated abilities tab.
     */
    public JPanel createAbilitiesTab(AbilityCategory abilityCategory) {
        JPanel tab = createdCategoryTabs.computeIfAbsent(abilityCategory, category -> new JPanel(new BorderLayout()));
        if (tab.getComponentCount() == 0) {
            refreshTabContents(abilityCategory);
        }

        return tab;
    }

    private JPanel createAbilitiesPage(AbilityCategory abilityCategory) {
        // Header name and logo image per category.
        String[] headerInfo = switch (abilityCategory) {
            case COMBAT_ABILITY -> new String[] { "CombatAbilitiesTab", "logo_aurigan_coalition.png" };
            case MANEUVERING_ABILITY -> new String[] { "ManeuveringAbilitiesTab", "logo_clan_hells_horses.png" };
            case UTILITY_ABILITY -> new String[] { "UtilityAbilitiesTab", "logo_circinus_federation.png" };
            case CHARACTER_FLAW -> new String[] { "CharacterFlawsTab", "logo_word_of_blake.png" };
            case CHARACTER_CREATION_ONLY -> new String[] { "CharacterCreationOnlyTab", "logo_tortuga_dominions.png" };
        };
        String headerName = headerInfo[0];
        String imageAddress = getImageDirectory() + headerInfo[1];

        CampaignOptionsHeaderPanel headerPanel = new CampaignOptionsHeaderPanel(headerName, imageAddress);

        CampaignOptionsPagePanel.Builder builder = CampaignOptionsPagePanel.builder("AbilitiesTab" +
                                                                                          abilityCategory.name(),
                    headerName,
                    imageAddress)
              .header(headerPanel)
              .showDetailsPanel(false)
              .component(createAbilityButtonBar(abilityCategory));

        // Every special ability is selectable as a prerequisite/incompatible/removed entry, so build the lookup map
        // once and share it with each ability control's selector popups.
        Map<String, SpecialAbility> allSPAs = new HashMap<>();
        for (CampaignOptionsAbilityInfo info : allAbilityInfo.values()) {
            allSPAs.put(info.getAbility().getName(), info.getAbility());
        }

        // One collapsible section per ability, alphabetical, titled by the ability's display name.
        ArrayList<String> sortedAbilityNames = new ArrayList<>(allAbilityInfo.keySet());
        Collections.sort(sortedAbilityNames);

        for (String abilityName : sortedAbilityNames) {
            CampaignOptionsAbilityInfo abilityInfo = allAbilityInfo.get(abilityName);

            if (abilityInfo.getCategory() == abilityCategory) {
                AbilityOptionPanel abilityPanel = new AbilityOptionPanel(abilityInfo, allSPAs);
                builder.literalSection(abilityInfo.getAbility().getDisplayName(),
                      abilityPanel.getSummaryText(),
                      abilityPanel);
            }
        }

        return builder.build();
    }

    /**
     * Builds the top action bar containing the four enable/disable buttons for the given category.
     *
     * @param abilityCategory the category these buttons act on for the "current tab" actions
     *
     * @return a left-aligned panel with the four action buttons
     */
    private JPanel createAbilityButtonBar(AbilityCategory abilityCategory) {
        JButton btnEnableCurrent = createAbilityActionButton("AddAllCurrent");
        btnEnableCurrent.addActionListener(e -> toggleAbilitiesAction(abilityCategory, true));

        JButton btnRemoveCurrent = createAbilityActionButton("RemoveAllCurrent");
        btnRemoveCurrent.addActionListener(e -> toggleAbilitiesAction(abilityCategory, false));

        JButton btnEnableAll = createAbilityActionButton("AddAll");
        btnEnableAll.addActionListener(e -> toggleAbilitiesAction(null, true));

        JButton btnRemoveAll = createAbilityActionButton("RemoveAll");
        btnRemoveAll.addActionListener(e -> toggleAbilitiesAction(null, false));

        JPanel buttonBar = new JPanel(new GridLayout(1, 4, UIUtil.scaleForGUI(8), 0));
        buttonBar.setOpaque(false);
        buttonBar.setName("pnlAbilityButtonBar");
        // Leave a gap between this action bar and the expand/collapse-all controls that follow it.
        buttonBar.setBorder(BorderFactory.createEmptyBorder(0, 0, UIUtil.scaleForGUI(12), 0));
        buttonBar.add(btnEnableCurrent);
        buttonBar.add(btnRemoveCurrent);
        buttonBar.add(btnEnableAll);
        buttonBar.add(btnRemoveAll);

        return buttonBar;
    }

    /**
     * Creates a plain (non-rounded) action button whose text and tooltip are read from the campaign options resource
     * bundle using the {@code "lbl" + name} key prefix.
     *
     * @param name the resource name used to resolve the button text and tooltip
     *
     * @return the configured button
     */
    private static JButton createAbilityActionButton(String name) {
        JButton button = new JButton(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".text"));
        button.setName("btn" + name);

        String tooltip = getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".tooltip");
        if (isResourceKeyValid(tooltip)) {
            button.setToolTipText(tooltip);
        }

        return button;
    }

    /**
     * Enables or disables abilities in the specified category, or all abilities if the category is {@code null}.
     *
     * <p>Iterates through all abilities and applies the {@code enable} status as indicated, then refreshes all ability
     * tabs to reflect the changes.</p>
     *
     * @param abilityCategory the {@link AbilityCategory} to filter abilities, or {@code null} to affect all categories
     * @param enable          {@code true} to enable the abilities; {@code false} to disable them
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void toggleAbilitiesAction(@Nullable AbilityCategory abilityCategory, boolean enable) {
        boolean skipComparison = abilityCategory == null;
        for (CampaignOptionsAbilityInfo abilityInfo : allAbilityInfo.values()) {
            if (skipComparison || abilityInfo.getCategory() == abilityCategory) {
                abilityInfo.setEnabled(enable);
            }
        }

        if (abilityCategory == null) {
            refreshCreatedTabs();
        } else {
            refreshTabContents(abilityCategory);
        }
    }

    /**
     * A reusable control representing a single special ability. Used as the content of one collapsible section per
     * ability. Displays an enable checkbox, an inline XP cost spinner, the description, and editable
     * prerequisite/incompatible/removed ability lists.
     */
    static class AbilityOptionPanel extends JPanel implements SectionHeaderControlProvider {
        /**
         * Fixed, unscaled content width for every ability panel. The description label is laid out at this width, and
         * {@link #getPreferredSize()} clamps the whole panel to it. Reporting a constant width for every ability on
         * every category tab is what keeps the layout data-independent: a populated Incompatible/Removes list (common
         * on flaws and origins) can never push a section wider than the page can show and collapse it.
         */
        private static final int PANEL_CONTENT_WIDTH = 860;

        /** Fixed wrapping width for the description label; kept narrower than the panel for comfortable reading. */
        private static final int DESCRIPTION_WIDTH = 800;

        private final SpecialAbility ability;
        private final CampaignOptionsAbilityInfo abilityInfo;
        private final Map<String, SpecialAbility> allSPAs;
        private final JPanel pnlPrerequisites = newColumnPanel();
        private final JPanel pnlSkills = newColumnPanel();
        private final JPanel pnlIncompatible = newColumnPanel();
        private final JPanel pnlRemoves = newColumnPanel();

        /** Header-mounted toggle that enables or disables the ability; the sole enable control for the section. */
        private final JCheckBox headerToggle = new JCheckBox();
        /** Colored "Enabled"/"Disabled" chip shown in the section header so state is readable while collapsed. */
        private final JLabel headerStatusLabel = new JLabel();
        /** Trailing control mounted in the collapsible section header. */
        private final JPanel headerControl = new JPanel();
        /** Notified whenever the enabled state changes, so the section can restyle its (collapsed) title. */
        private Runnable sectionStateListener;


        /**
         * Builds the control for a single ability.
         *
         * @param abilityInfo the ability data backing this control
         * @param allSPAs     every selectable special ability, keyed by lookup name, used by the selector popups
         */
        AbilityOptionPanel(CampaignOptionsAbilityInfo abilityInfo, Map<String, SpecialAbility> allSPAs) {
            super(new GridBagLayout());
            this.ability = abilityInfo.getAbility();
            this.abilityInfo = abilityInfo;
            this.allSPAs = allSPAs;
            setOpaque(false);
            setName("pnl" + ability.getName() + "Ability");

            buildHeaderControl();

            // The cost resource is a formatted "XP Cost: %s" string; format with an empty value to reuse it as the
            // spinner's prefix label without introducing a new resource key.
            String costPrefix = String.format(getTextAt(getCampaignOptionsResourceBundle(), "abilityCost.text"), "")
                                      .trim();
            JLabel lblCost = new JLabel(costPrefix);
            JSpinner spnCost = new JSpinner(new SpinnerNumberModel(ability.getCost(), -100000, 100000, 1));
            spnCost.setName("spnCost" + ability.getName());
            spnCost.addChangeListener(e -> ability.setCost((Integer) spnCost.getValue()));
            spnCost.setMaximumSize(spnCost.getPreferredSize());

            // BoxLayout (not FlowLayout) so the XP cost label lines up flush-left with the description and columns
            // below it; FlowLayout would inset the first component by its horizontal gap.
            JPanel header = new JPanel();
            header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
            header.setOpaque(false);
            header.add(lblCost);
            header.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(8)));
            header.add(spnCost);
            header.add(Box.createHorizontalGlue());

            JLabel lblDescription = new JLabel(String.format(
                  "<html><div style='width: %s; text-align:justify;'><i>%s</i></div></html>",
                  UIUtil.scaleForGUI(DESCRIPTION_WIDTH),
                  ability.getDescription()));

            populatePrerequisites();
            populateSkills();
            populateIncompatible();
            populateRemoves();

            // Four equal-width columns. The panel reports a constant width (see getPreferredSize), so a populated
            // Incompatible/Removes list can never push the section past the page width and collapse it; the columns
            // simply share the fixed panel width. Skill requirements sit beside the ability prerequisites they
            // complement.
            JPanel columns = new JPanel(new GridLayout(1, 4, UIUtil.scaleForGUI(16), 0));
            columns.setOpaque(false);
            columns.add(pnlSkills);
            columns.add(pnlPrerequisites);
            columns.add(pnlIncompatible);
            columns.add(pnlRemoves);

            int gap = UIUtil.scaleForGUI(2);
            GridBagConstraints layout = new GridBagConstraints();

            // Description spanning the full width, shown first so the ability is explained before its cost.
            layout.gridx = 0;
            layout.gridy = 0;
            layout.gridwidth = 1;
            layout.weightx = 1.0;
            layout.fill = GridBagConstraints.HORIZONTAL;
            layout.anchor = GridBagConstraints.NORTHWEST;
            layout.insets = new Insets(UIUtil.scaleForGUI(6), gap, UIUtil.scaleForGUI(6), gap);
            add(lblDescription, layout);

            // Inline XP cost spinner.
            layout.gridy = 1;
            layout.insets = new Insets(gap, gap, gap, gap);
            add(header, layout);

            // Balanced editable columns.
            layout.gridy = 2;
            add(columns, layout);
        }

        /**
         * Builds the control mounted in the collapsible section header: a colored enabled/disabled status chip and a
         * bare checkbox, so the ability can be toggled and its state read without expanding the section.
         */
        private void buildHeaderControl() {
            headerToggle.setName("chkHeader" + ability.getName());
            headerToggle.setOpaque(false);
            headerToggle.setSelected(abilityInfo.isEnabled());
            headerToggle.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "abilityEnable.text"));
            headerToggle.addActionListener(e -> setAbilityEnabled(headerToggle.isSelected()));

            headerControl.setLayout(new BoxLayout(headerControl, BoxLayout.X_AXIS));
            headerControl.setOpaque(false);
            headerControl.add(headerStatusLabel);
            headerControl.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(8)));
            headerControl.add(headerToggle);

            updateHeaderStatus();
        }

        /**
         * Toggles this ability on or off from a single place: updates the backing data, keeps the header toggle in
         * sync, refreshes the header status chip, and notifies the section so it can restyle its title.
         *
         * @param enabled the new enabled state
         */
        private void setAbilityEnabled(boolean enabled) {
            abilityInfo.setEnabled(enabled);
            if (headerToggle.isSelected() != enabled) {
                headerToggle.setSelected(enabled);
            }
            updateHeaderStatus();
            if (sectionStateListener != null) {
                sectionStateListener.run();
            }
        }

        /** Repaints the header status chip to match the current enabled state. */
        private void updateHeaderStatus() {
            boolean enabled = abilityInfo.isEnabled();
            String color = enabled ? ReportingUtilities.getPositiveColor() : ReportingUtilities.getNegativeColor();
            String text = getTextAt(getCampaignOptionsResourceBundle(),
                  enabled ? "abilityStatusEnabled.text" : "abilityStatusDisabled.text");
            headerStatusLabel.setText("<html><b><font color='" + color + "'>" + text + "</font></b></html>");
        }

        @Override
        public @Nullable JComponent getSectionHeaderControl() {
            return headerControl;
        }

        @Override
        public boolean isSectionEnabled() {
            return abilityInfo.isEnabled();
        }

        @Override
        public void setSectionStateListener(@Nullable Runnable listener) {
            this.sectionStateListener = listener;
        }

        /**
         * Reports a constant width (the fixed content width) regardless of how populated the ability's lists are. The
         * section stretches this panel to the available width via a horizontal fill, and the columns/skill rows wrap
         * within it, so clamping the reported preferred width keeps every section the same width and prevents the
         * page-collapse that long prerequisite/incompatible/removed lists would otherwise trigger.
         */
        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = super.getPreferredSize();
            return new Dimension(UIUtil.scaleForGUI(PANEL_CONTENT_WIDTH), preferredSize.height);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension maximumSize = super.getMaximumSize();
            return new Dimension(UIUtil.scaleForGUI(PANEL_CONTENT_WIDTH), maximumSize.height);
        }

        private static JPanel newColumnPanel() {
            JPanel column = new JPanel();
            column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
            column.setOpaque(false);
            return column;
        }

        private void populatePrerequisites() {
            buildColumn(pnlPrerequisites,
                  "prerequisites.text",
                  ability.getPrereqAbilities(),
                  ability::setPrereqAbilities,
                  this::populatePrerequisites);
        }

        private void populateIncompatible() {
            buildColumn(pnlIncompatible,
                  "incompatible.text",
                  ability.getInvalidAbilities(),
                  ability::setInvalidAbilities,
                  this::populateIncompatible);
        }

        private void populateRemoves() {
            buildColumn(pnlRemoves,
                  "removes.text",
                  ability.getRemovedAbilities(),
                  ability::setRemovedAbilities,
                  this::populateRemoves);
        }

        /**
         * Rebuilds the skill-requirements column: a title, the ability's current prerequisite skill sets, and an edit
         * link that opens the skill-set manager.
         */
        private void populateSkills() {
            pnlSkills.removeAll();

            JLabel title = new JLabel("<html>" +
                                            getTextAt(getCampaignOptionsResourceBundle(), "skillRequirements.text") +
                                            "</html>");

            JButton btnEditSkills = new JButton("Edit\u2026");
            btnEditSkills.setToolTipText("Edit skill requirements");
            btnEditSkills.addActionListener(e -> {
                SkillPrerequisitesDialog dialog = new SkillPrerequisitesDialog(SwingUtilities.getWindowAncestor(this),
                      ability);
                dialog.setVisible(true);
                if (dialog.wasChanged()) {
                    populateSkills();
                }
            });
            pnlSkills.add(makeColumnHeader(title, btnEditSkills));

            // Each prerequisite is an OR-group: the character qualifies if they hold ANY one of the listed skills at
            // the required level. Separate prerequisites are AND-ed together (all must be satisfied).
            for (SkillPrerequisite skillPrerequisite : ability.getPrereqSkills()) {
                JLabel lblSkill = new JLabel(formatSkillPrerequisite(skillPrerequisite));
                lblSkill.setAlignmentX(LEFT_ALIGNMENT);
                pnlSkills.add(lblSkill);
            }

            pnlSkills.revalidate();
            pnlSkills.repaint();
        }

        /**
         * Formats a single skill prerequisite (an OR-group) for display: drops the raw {@code {}} delimiters from
         * {@link SkillPrerequisite#toString()} and renders the {@code OR} separators as a muted "or" so the entry reads
         * as a plain "any one of these skills" list.
         *
         * @param skillPrerequisite the prerequisite to format
         *
         * @return an HTML snippet sized to the column width
         */
        private static String formatSkillPrerequisite(SkillPrerequisite skillPrerequisite) {
            String body = skillPrerequisite.toString();
            if (body.startsWith("{") && body.endsWith("}")) {
                body = body.substring(1, body.length() - 1);
            }
            body = body.replace("<br>OR ", "<br><span style='color:gray;'>or </span>");
            return "<html><div style='width: " +
                         UIUtil.scaleForGUI(ABILITY_COLUMN_TEXT_WIDTH) +
                         "px;'>" +
                         body +
                         "</div></html>";
        }

        /**
         * Rebuilds one editable column: a title, a removable chip per selected ability, an add button that opens the
         * searchable selector, and (for the prerequisites column) the prerequisite skill sets with an edit link.
         *
         * @param column      the column container to rebuild in place
         * @param titleKey    the resource key for the column's heading
         * @param current     the ability's current selection for this column
         * @param setter      writes a new selection back to the ability
         * @param rebuild     re-runs this column's population after an edit
         */
        private void buildColumn(JPanel column, String titleKey, Vector<String> current,
              Consumer<Vector<String>> setter, Runnable rebuild) {
            column.removeAll();

            JLabel title = new JLabel("<html>" + getTextAt(getCampaignOptionsResourceBundle(), titleKey) + "</html>");

            JButton btnAdd = new JButton("Add\u2026");
            btnAdd.addActionListener(e -> {
                AbilitySelectorDialog dialog = new AbilitySelectorDialog(SwingUtilities.getWindowAncestor(this),
                      getTextAt(getCampaignOptionsResourceBundle(), titleKey).replaceAll("<[^>]*>", "").trim(),
                      current,
                      allSPAs);
                dialog.setVisible(true);
                if (!dialog.wasCancelled()) {
                    setter.accept(dialog.getSelected());
                    rebuild.run();
                }
            });
            column.add(makeColumnHeader(title, btnAdd));

            List<String> sorted = new ArrayList<>(current);
            sorted.sort(Comparator.comparing(SpecialAbility::getDisplayName, String.CASE_INSENSITIVE_ORDER));
            for (String abilityName : sorted) {
                column.add(makeChipRow(abilityName, () -> {
                    Vector<String> updated = new Vector<>(current);
                    updated.remove(abilityName);
                    setter.accept(updated);
                    rebuild.run();
                }));
            }

            column.revalidate();
            column.repaint();
        }

        /**
         * Builds a column header that keeps the title and its action button on one fixed row, so the button stays put
         * as chips are added or removed below it.
         *
         * @param title  the column heading label
         * @param action the add/edit button for this column
         *
         * @return a left-aligned, height-capped header row
         */
        private static JPanel makeColumnHeader(JLabel title, JButton action) {
            JPanel headerRow = new JPanel();
            headerRow.setLayout(new BoxLayout(headerRow, BoxLayout.X_AXIS));
            headerRow.setOpaque(false);
            headerRow.setAlignmentX(LEFT_ALIGNMENT);
            headerRow.add(title);
            headerRow.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(8)));
            headerRow.add(action);
            headerRow.add(Box.createHorizontalGlue());
            headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerRow.getPreferredSize().height));
            return headerRow;
        }

        private JPanel makeChipRow(String abilityName, Runnable onRemove) {
            JPanel row = new JPanel(new BorderLayout(UIUtil.scaleForGUI(4), 0));
            row.setOpaque(false);
            row.setAlignmentX(LEFT_ALIGNMENT);

            // Width-constrain the chip text so long ability names wrap instead of widening the column. Without this,
            // populated Incompatible/Removes lists (common on flaws and origins) push the section past the page-width
            // cap, which collapses the whole page and mangles the section title.
            JLabel lblName = new JLabel("<html><div style='width: " +
                                              UIUtil.scaleForGUI(ABILITY_COLUMN_TEXT_WIDTH) +
                                              "px;'>" +
                                              SpecialAbility.getDisplayName(abilityName) +
                                              "</div></html>");
            row.add(lblName, BorderLayout.CENTER);

            JButton btnRemove = new JButton("\u2715");
            btnRemove.putClientProperty("JButton.buttonType", "borderless");
            btnRemove.setMargin(new Insets(0, UIUtil.scaleForGUI(4), 0, UIUtil.scaleForGUI(4)));
            btnRemove.setToolTipText("Remove");
            btnRemove.addActionListener(e -> onRemove.run());
            row.add(btnRemove, BorderLayout.EAST);

            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            return row;
        }

        /**
         * Returns a short, plain-text one-line summary derived from the ability description, suitable for the
         * collapsed section header.
         *
         * @return the summary text, or an empty string if there is no description
         */
        String getSummaryText() {
            String description = ability.getDescription();
            if (description == null) {
                return "";
            }

            description = description.replaceAll("<[^>]*>", "").replace("{", "").replace("}", "").trim();

            int sentenceEnd = description.indexOf(". ");
            if (sentenceEnd > 0) {
                description = description.substring(0, sentenceEnd + 1);
            }

            if (description.length() > 140) {
                description = description.substring(0, 137).trim() + "...";
            }

            return description;
        }
    }

    /**
     * Applies the current campaign options to a specified {@code CampaignPreset}. Enabled abilities are added to the
     * preset or globally updated within the campaign.
     *
     * @param preset The {@code CampaignPreset} to apply abilities to, or {@code null} for directly setting them in the
     *               campaign.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignPreset preset) {
        Map<String, SpecialAbility> enabledAbilities = new HashMap<>();

        for (CampaignOptionsAbilityInfo abilityInfo : allAbilityInfo.values()) {
            if (abilityInfo.isEnabled()) {
                enabledAbilities.put(abilityInfo.getAbility().getName(), abilityInfo.getAbility());
            }
        }

        if (preset != null) {
            preset.getSpecialAbilities().clear();
            preset.getSpecialAbilities().putAll(enabledAbilities);
        } else {
            SpecialAbility.replaceSpecialAbilities(enabledAbilities);
        }
    }
}
