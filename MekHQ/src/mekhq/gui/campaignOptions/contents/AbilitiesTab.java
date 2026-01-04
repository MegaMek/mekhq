/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.spaUtilities.SpaUtilities.getSpaCategory;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_CREATION_ONLY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.CHARACTER_FLAW;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.COMBAT_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.MANEUVERING_ABILITY;
import static mekhq.utilities.spaUtilities.enums.AbilityCategory.UTILITY_ABILITY;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import mekhq.CampaignPreset;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.campaignOptions.components.CampaignOptionsButton;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;
import mekhq.gui.dialog.EditSpecialAbilityDialog;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;

/**
 * The {@code AbilitiesTab} class represents a GUI tab for configuring and managing special abilities in a campaign.
 * This class handles the initialization, categorization, and display of abilities, providing functionality for
 * enabling, disabling, and customizing abilities within the combat, maneuvering, and utility categories.
 * <p>
 * This tab is used as part of the MekHQ campaign options UI for managing personnel-related abilities.
 */
public class AbilitiesTab {
    private ArrayList<String> level3Abilities;
    private Map<String, CampaignOptionsAbilityInfo> allAbilityInfo;
    private JPanel combatTab;
    private JPanel maneuveringTab;
    private JPanel utilityTab;
    private JPanel characterFlawsTab;
    private JPanel characterCreationOnlyTab;

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
        combatTab = new JPanel();
        maneuveringTab = new JPanel();
        utilityTab = new JPanel();
        characterFlawsTab = new JPanel();
        characterCreationOnlyTab = new JPanel();
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

        // Clear and update tabs in-place
        refreshAll();
    }

    /**
     * Refreshes and updates all tabs related to abilities by clearing their contents and reloading the data.
     */
    private void refreshAll() {
        refreshTabContents(combatTab, COMBAT_ABILITY);
        refreshTabContents(maneuveringTab, MANEUVERING_ABILITY);
        refreshTabContents(utilityTab, UTILITY_ABILITY);
        refreshTabContents(characterFlawsTab, CHARACTER_FLAW);
        refreshTabContents(characterCreationOnlyTab, CHARACTER_CREATION_ONLY);
    }

    /**
     * Updates the contents of a specific ability category tab by rebuilding its layout and content based on the
     * category.
     *
     * @param tab      The {@code JPanel} representing the tab to be refreshed.
     * @param category The ability category associated with the tab.
     */
    private void refreshTabContents(JPanel tab, AbilityCategory category) {
        tab.removeAll();
        JPanel newContents = createAbilitiesTab(category);

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
        // Header
        CampaignOptionsHeaderPanel headerPanel = switch (abilityCategory) {
            case COMBAT_ABILITY -> new CampaignOptionsHeaderPanel("CombatAbilitiesTab",
                  getImageDirectory() + "logo_aurigan_coalition.png");
            case MANEUVERING_ABILITY -> new CampaignOptionsHeaderPanel("ManeuveringAbilitiesTab",
                  getImageDirectory() + "logo_clan_hells_horses.png");
            case UTILITY_ABILITY -> new CampaignOptionsHeaderPanel("UtilityAbilitiesTab",
                  getImageDirectory() + "logo_circinus_federation.png");
            case CHARACTER_FLAW -> new CampaignOptionsHeaderPanel("CharacterFlawsTab",
                  getImageDirectory() + "logo_word_of_blake.png");
            case CHARACTER_CREATION_ONLY -> new CampaignOptionsHeaderPanel("CharacterCreationOnlyTab",
                  getImageDirectory() + "logo_tortuga_dominions.png");
        };

        // Contents
        RoundedJButton btnEnableCurrent = new CampaignOptionsButton("AddAllCurrent");
        btnEnableCurrent.addActionListener(e -> toggleAbilitiesAction(abilityCategory, true));

        RoundedJButton btnRemoveCurrent = new CampaignOptionsButton("RemoveAllCurrent");
        btnRemoveCurrent.addActionListener(e -> toggleAbilitiesAction(abilityCategory, false));

        RoundedJButton btnEnableAll = new CampaignOptionsButton("AddAll");
        btnEnableAll.addActionListener(e -> toggleAbilitiesAction(null, true));

        RoundedJButton btnRemoveAll = new CampaignOptionsButton("RemoveAll");
        btnRemoveAll.addActionListener(e -> toggleAbilitiesAction(null, false));

        // Layout the Panels
        final JPanel panel = new CampaignOptionsStandardPanel("AbilitiesGeneralTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(btnEnableCurrent, layout);
        layout.gridx++;
        panel.add(btnRemoveCurrent, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(btnEnableAll, layout);
        layout.gridx++;
        panel.add(btnRemoveAll, layout);

        int abilityCounter = 0;

        // Retrieve keySet and sort alphabetically
        ArrayList<String> sortedAbilityNames = new ArrayList<>(allAbilityInfo.keySet());
        Collections.sort(sortedAbilityNames);

        for (String abilityName : sortedAbilityNames) {
            CampaignOptionsAbilityInfo abilityInfo = allAbilityInfo.get(abilityName);

            if (abilityInfo.getCategory() == abilityCategory) {
                JPanel abilityPanel = createSPAPanel(abilityInfo);

                layout.gridx = abilityCounter % 3;
                layout.gridy = 3 + (abilityCounter / 3);
                abilityCounter++;

                layout.gridwidth = 1;
                panel.add(abilityPanel, layout);
            }
        }

        // Create Parent Panel and return
        JPanel parentPanel = createParentPanel(panel, "AbilitiesTab" + COMBAT_ABILITY.name());

        return switch (abilityCategory) {
            case COMBAT_ABILITY -> {
                combatTab = parentPanel;
                yield combatTab;
            }
            case MANEUVERING_ABILITY -> {
                maneuveringTab = parentPanel;
                yield maneuveringTab;
            }
            case UTILITY_ABILITY -> {
                utilityTab = parentPanel;
                yield utilityTab;
            }
            case CHARACTER_FLAW -> {
                characterFlawsTab = parentPanel;
                yield characterFlawsTab;
            }
            case CHARACTER_CREATION_ONLY -> {
                characterCreationOnlyTab = parentPanel;
                yield characterCreationOnlyTab;
            }
        };
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

        refreshAll();
    }

    /**
     * Creates a panel for rendering a single ability within the tab, enabling users to customize or enable/disable the
     * ability.
     *
     * @param abilityInfo The {@code CampaignOptionsAbilityInfo} containing details about a specific ability.
     *
     * @return A {@code JPanel} containing the UI elements related to the ability.
     */
    private JPanel createSPAPanel(CampaignOptionsAbilityInfo abilityInfo) {
        SpecialAbility ability = abilityInfo.getAbility();

        // Initialization
        final JPanel panel = new AbilitiesTabStandardPanel(ability);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel,
              GridBagConstraints.NORTHWEST,
              GridBagConstraints.HORIZONTAL);

        // Contents
        JCheckBox chkAbility = new JCheckBox(getTextAt(getCampaignOptionsResourceBundle(), "abilityEnable.text"));
        chkAbility.setSelected(abilityInfo.isEnabled());
        chkAbility.addActionListener(e -> abilityInfo.setEnabled(chkAbility.isSelected()));

        JLabel lblCost = new JLabel(String.format(getTextAt(getCampaignOptionsResourceBundle(), "abilityCost.text"),
              ability.getCost()));

        JLabel lblDescription = new JLabel();
        lblDescription.setText(String.format("<html><div style='width: %s; text-align:justify;'><i>%s</i></div></html>",
              UIUtil.scaleForGUI(400),
              ability.getDescription()));

        JLabel lblPrerequisites = createAbilityLabel("prerequisites.text", ability.getAllPrereqDesc());
        JLabel lblIncompatible = createAbilityLabel("incompatible.text", ability.getInvalidDesc());
        JLabel lblRemoves = createAbilityLabel("removes.text", ability.getRemovedDesc());

        RoundedJButton btnCustomizeAbility = new CampaignOptionsButton("CustomizeAbility", (Integer) null);
        btnCustomizeAbility.addActionListener(e -> {
            if (editSPA(ability)) {
                // This will run on the SWT thread
                SwingUtilities.invokeLater(() -> {
                    // Update components with new values
                    lblCost.setText(String.format(getTextAt(getCampaignOptionsResourceBundle(), "abilityCost.text"),
                          ability.getCost()));

                    String prerequisitesDescription = getTextAt(getCampaignOptionsResourceBundle(),
                          "prerequisites.text") +
                                                            ability.getAllPrereqDesc();
                    lblPrerequisites.setText(buildAbilityDescription(prerequisitesDescription));

                    String incompatibleDescription = getTextAt(getCampaignOptionsResourceBundle(),
                          "incompatible.text") +
                                                           ability.getInvalidDesc();
                    lblIncompatible.setText(buildAbilityDescription(incompatibleDescription));

                    String removesDescription = getTextAt(getCampaignOptionsResourceBundle(), "removes.text") +
                                                      ability.getRemovedDesc();
                    lblRemoves.setText(buildAbilityDescription(removesDescription));
                });
            }
        });

        // Layout
        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkAbility, layout);
        layout.gridx++;
        panel.add(lblCost, layout);

        layout.gridwidth = 3;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblDescription, layout);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPrerequisites, layout);
        layout.gridx++;
        panel.add(lblIncompatible, layout);
        layout.gridx++;
        panel.add(lblRemoves, layout);

        layout.gridwidth = 3;
        layout.gridx = 0;
        layout.gridy++;
        panel.add(btnCustomizeAbility, layout);

        return panel;
    }

    /**
     * Opens a dialog to edit the details of the specified special ability.
     *
     * @param ability The {@code SpecialAbility} instance to be edited.
     *
     * @return {@code true} if the user confirmed the changes; {@code false} if the operation was canceled.
     */
    private boolean editSPA(SpecialAbility ability) {
        Map<String, SpecialAbility> temporaryMap = new HashMap<>();

        for (Entry<String, CampaignOptionsAbilityInfo> info : allAbilityInfo.entrySet()) {
            temporaryMap.put(info.getKey(), info.getValue().getAbility());
        }

        EditSpecialAbilityDialog dialog = new EditSpecialAbilityDialog(null, ability, temporaryMap);
        dialog.setVisible(true);

        return !dialog.wasCancelled();
    }

    /**
     * A custom {@code JPanel} implementation for displaying abilities configured in the tab. Displays the ability's
     * name in a bordered, titled panel and scales the panel size appropriately for the UI.
     */
    static class AbilitiesTabStandardPanel extends JPanel {
        /**
         * Constructs a panel representing an individual ability with a styled header based on the name of the given
         * {@code SpecialAbility}.
         *
         * @param ability The {@code SpecialAbility} used to configure the layout and title of this panel.
         */
        public AbilitiesTabStandardPanel(SpecialAbility ability) {
            String name = ability.getDisplayName();

            new JPanel() {
                @Override
                public Dimension getPreferredSize() {
                    Dimension standardSize = super.getPreferredSize();
                    return UIUtil.scaleForGUI((Math.max(standardSize.width, 500)), standardSize.height);
                }
            };

            setBorder(RoundedLineBorder.createRoundedLineBorder(name));
            setName("pnl" + name);
        }
    }

    /**
     * Creates a label with a description for a specific attribute of the ability. For example, prerequisites,
     * incompatible abilities, or removed abilities.
     *
     * @param resourceKey            The key used to retrieve the label text from resources.
     * @param descriptionFromAbility The description related to the ability attribute.
     *
     * @return A {@code JLabel} with the generated description.
     */
    private JLabel createAbilityLabel(String resourceKey, String descriptionFromAbility) {
        String description = getTextAt(getCampaignOptionsResourceBundle(), resourceKey) + descriptionFromAbility;
        return new JLabel(buildAbilityDescription(description));
    }

    /**
     * Builds the HTML-formatted description for a specific ability or attribute for display in the UI.
     *
     * @param description The plain text description to be formatted.
     *
     * @return A string containing the HTML-formatted description.
     */
    private static String buildAbilityDescription(String description) {
        return ("<html>" + description + "</html>").replaceAll("\\{", "").replaceAll("}", "");
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
