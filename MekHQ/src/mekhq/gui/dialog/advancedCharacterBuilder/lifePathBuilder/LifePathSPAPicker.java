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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder.createRoundedLineBorder;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathPickerUtilities.clampSpinnerValue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import megamek.common.annotations.Nullable;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.gui.campaignOptions.CampaignOptionsAbilityInfo;
import mekhq.gui.utilities.TooltipMouseListenerUtil;

/**
 * Lets an author set the special pilot abilities a Life Path requires, excludes or awards.
 *
 * <p>Requirements and exclusions are a yes or no: the character either has the ability or does not, so those tabs show
 * checkboxes. The two XP tabs show spinners instead, because there the ability is being bought with XP.</p>
 *
 * <p>The abilities are split across tabs by category: combat, maneuvering, utility, flaws and character creation.</p>
 *
 * @since 0.50.11
 */
class LifePathSPAPicker extends AbstractLifePathPicker {
    private static final MMLogger LOGGER = MMLogger.create(LifePathSPAPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathSPAPicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(575);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(575);

    /**
     * The value stored for an ability that is simply required or excluded.
     *
     * <p>Requirements and exclusions are a yes or no, so the map's value carries no information; the ability's presence
     * as a key is the whole rule.</p>
     */
    private static final int PRESENCE_ONLY_VALUE = 0;

    /** Largest XP amount one ability row can award or charge on the Fixed XP and Flexible XP tabs. */
    private static final int MAXIMUM_ABILITY_XP = 1000;

    private final LifePathBuilderTabType tabType;
    private final Map<String, CampaignOptionsAbilityInfo> allAbilityInfo;
    private final Map<String, Integer> storedAbilities;
    private Map<String, Integer> selectedAbilities;

    Map<String, Integer> getSelectedAbilities() {
        return selectedAbilities;
    }

    /**
     * Opens the picker.
     *
     * @param owner             the wizard this picker belongs to
     * @param selectedAbilities the abilities already on this group
     * @param allAbilityInfo    every ability the campaign knows about, keyed by name
     * @param tabType           the section being edited
     * @param groupIndex        the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathSPAPicker(@Nullable Window owner, Map<String, Integer> selectedAbilities,
          Map<String, CampaignOptionsAbilityInfo> allAbilityInfo, LifePathBuilderTabType tabType, int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathSPAPicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        this.tabType = tabType;
        this.allAbilityInfo = allAbilityInfo;

        // Defensive copies to avoid external modification
        this.selectedAbilities = new HashMap<>(selectedAbilities);
        storedAbilities = new HashMap<>(selectedAbilities);

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));

        String titleOptions = getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.label");
        pnlOptions.setBorder(createRoundedLineBorder(titleOptions));

        List<CampaignOptionsAbilityInfo> combatAbilities = new ArrayList<>();
        List<CampaignOptionsAbilityInfo> maneuveringAbilities = new ArrayList<>();
        List<CampaignOptionsAbilityInfo> utilityAbilities = new ArrayList<>();
        List<CampaignOptionsAbilityInfo> flawsAbilities = new ArrayList<>();
        List<CampaignOptionsAbilityInfo> originsAbilities = new ArrayList<>();

        for (CampaignOptionsAbilityInfo abilityInfo : allAbilityInfo.values()) {
            switch (abilityInfo.getCategory()) {
                case COMBAT_ABILITY -> combatAbilities.add(abilityInfo);
                case MANEUVERING_ABILITY -> maneuveringAbilities.add(abilityInfo);
                case UTILITY_ABILITY -> utilityAbilities.add(abilityInfo);
                case CHARACTER_FLAW -> flawsAbilities.add(abilityInfo);
                case CHARACTER_CREATION_ONLY -> originsAbilities.add(abilityInfo);
            }
        }

        Comparator<CampaignOptionsAbilityInfo> byDisplayName = Comparator.comparing(
              a -> a.getAbility().getDisplayName(), String.CASE_INSENSITIVE_ORDER);

        combatAbilities.sort(byDisplayName);
        maneuveringAbilities.sort(byDisplayName);
        utilityAbilities.sort(byDisplayName);
        flawsAbilities.sort(byDisplayName);
        originsAbilities.sort(byDisplayName);

        EnhancedTabbedPane optionPane = new EnhancedTabbedPane();

        boolean useBinaryOptions = tabType == LifePathBuilderTabType.REQUIREMENTS ||
                                         tabType == LifePathBuilderTabType.EXCLUSIONS;

        FastJScrollPane pnlCombatSkills = useBinaryOptions ? getAbilityOptionsBinary(combatAbilities) :
                                                getAbilityOptionsVariable(combatAbilities);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.combat.label"),
              pnlCombatSkills);

        FastJScrollPane pnlManeuveringAbilities = useBinaryOptions ? getAbilityOptionsBinary(maneuveringAbilities) :
                                                        getAbilityOptionsVariable(maneuveringAbilities);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.maneuvering.label"),
              pnlManeuveringAbilities);

        FastJScrollPane pnlUtilityAbilities = useBinaryOptions ? getAbilityOptionsBinary(utilityAbilities) :
                                                    getAbilityOptionsVariable(utilityAbilities);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.utility.label"),
              pnlUtilityAbilities);

        FastJScrollPane pnlFlawsAbilities = useBinaryOptions ? getAbilityOptionsBinary(flawsAbilities) :
                                                  getAbilityOptionsVariable(flawsAbilities);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.flaws.label"),
              pnlFlawsAbilities);

        FastJScrollPane pnlOriginsAbilities = useBinaryOptions ? getAbilityOptionsBinary(originsAbilities) :
                                                    getAbilityOptionsVariable(originsAbilities);
        optionPane.addTab(getTextAt(RESOURCE_BUNDLE, "LifePathSPAPicker.options.origins.label"),
              pnlOriginsAbilities);

        pnlOptions.add(optionPane, BorderLayout.NORTH);
        return pnlOptions;
    }

    private FastJScrollPane getAbilityOptionsBinary(List<CampaignOptionsAbilityInfo> abilityInfo) {
        JPanel pnlSkills = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < abilityInfo.size(); i++) {
            CampaignOptionsAbilityInfo info = abilityInfo.get(i);

            SpecialAbility ability = info.getAbility();
            String label = ability.getDisplayName();
            String description = getFormattedTextAt(RESOURCE_BUNDLE,
                  "LifePathSPAPicker.options.ability.description", ability.getDescription(),
                  ability.getCost());
            String abilityName = ability.getName();

            JCheckBox chkAbilityOption = new JCheckBox(label);
            chkAbilityOption.setSelected(selectedAbilities.containsKey(abilityName));
            chkAbilityOption.addActionListener(actionEvent -> {
                if (chkAbilityOption.isSelected()) {
                    selectedAbilities.put(abilityName, 0);
                } else {
                    selectedAbilities.remove(abilityName);
                }
            });
            chkAbilityOption.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(chkAbilityOption);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlSkills.add(pnlRows, gbc);
        }

        FastJScrollPane scrollSkills = new FastJScrollPane(pnlSkills);
        scrollSkills.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSkills.setBorder(null);

        return scrollSkills;
    }

    /**
     * Builds spinner rows for the two XP tabs, where an ability is bought with XP rather than simply held.
     *
     * <p>Only ever called for Fixed XP and Flexible XP. Requirements and Exclusions use
     * {@link #getAbilityOptionsBinary(List)} instead, because there an ability is a yes or no.</p>
     *
     * @param abilities the abilities to build rows for
     *
     * @return the scrollable panel of rows
     *
     * @since 0.50.11
     */
    private FastJScrollPane getAbilityOptionsVariable(List<CampaignOptionsAbilityInfo> abilities) {
        JPanel pnlAbilityOptions = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int columns = 3;
        for (int i = 0; i < abilities.size(); i++) {
            CampaignOptionsAbilityInfo abilityInfo = abilities.get(i);
            SpecialAbility ability = abilityInfo.getAbility();
            String label = ability.getDisplayName();
            String description = getFormattedTextAt(RESOURCE_BUNDLE,
                  "LifePathSPAPicker.options.ability.description", ability.getDescription(),
                  ability.getCost());

            // This panel only serves the XP tabs, so the range is the flat plus or minus an XP award can take.
            // The per-ability bounds that Requirements and Exclusions need are applied in getAbilityOptionsBinary,
            // and the branch that set them here could never be reached.
            int minimumValue = -MAXIMUM_ABILITY_XP;
            int maximumValue = MAXIMUM_ABILITY_XP;
            int keyValue = 0;

            // Clamped first: a stored value from a hand-edited file can sit outside these bounds, and
            // SpinnerNumberModel throws when its initial value is out of range.
            int storedValue = selectedAbilities.getOrDefault(ability.getName(), keyValue);
            int defaultValue = clampSpinnerValue(storedValue, minimumValue, maximumValue, label);

            JLabel lblAbility = new JLabel(label);
            JSpinner spnAbilityValue = new JSpinner(new SpinnerNumberModel(defaultValue, minimumValue,
                  maximumValue, 1));
            lblAbility.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );
            spnAbilityValue.addMouseListener(
                  TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, description)
            );

            spnAbilityValue.addChangeListener(changeEvent -> {
                int value = (int) spnAbilityValue.getValue();
                // Deliberately not compared against the value the spinner started at. Changing a spinner and
                // then changing it back must write the original number, otherwise the map keeps the stale one.
                if (value == keyValue) {
                    selectedAbilities.remove(ability.getName());
                } else {
                    selectedAbilities.put(ability.getName(), value);
                }
            });

            gbc.gridx = i % columns;
            gbc.gridy = i / columns;

            JPanel pnlRows = new JPanel();
            pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
            pnlRows.add(lblAbility);
            pnlRows.add(Box.createHorizontalStrut(PADDING));
            pnlRows.add(spnAbilityValue);
            pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlAbilityOptions.add(pnlRows, gbc);
        }

        FastJScrollPane scrollAbilityOptions = new FastJScrollPane(pnlAbilityOptions);
        scrollAbilityOptions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollAbilityOptions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollAbilityOptions.setBorder(null);

        return scrollAbilityOptions;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedAbilities = new HashMap<>(storedAbilities);
    }

    @Override
    protected void clearSelection() {
        selectedAbilities.clear();
        rebuildOptions();
    }
}
