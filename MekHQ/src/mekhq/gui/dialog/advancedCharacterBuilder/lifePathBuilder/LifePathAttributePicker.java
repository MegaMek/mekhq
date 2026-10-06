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
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathPickerUtilities.clampSpinnerValue;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathTab.getAttributeMaximumValue;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathTab.getAttributeMinimumValue;
import static mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder.LifePathTab.getDefaultAttributeValue;

import java.awt.Component;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.advancedCharacterBuilder.LifePathBuilderTabType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.utilities.TooltipMouseListenerUtil;

/**
 * Lets an author set attribute scores a Life Path requires or excludes, or attribute XP it awards.
 *
 * <p>Three kinds of row appear. One per named attribute; one for Edge, which has its own bounds because a character
 * can legitimately have none; and one "any attribute" row, which lets the player choose which attribute the value
 * applies to.</p>
 *
 * <p>Edge and the "any attribute" row both store {@code null} when they sit at their unset value, rather than storing
 * that value. Storing it left every group carrying a rule the author never wrote, which in exclusion groups read as
 * "Edge 10 is banned".</p>
 *
 * @since 0.50.11
 */
class LifePathAttributePicker extends AbstractLifePathPicker {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.LifePathAttributePicker";

    private static final int MINIMUM_MAIN_WIDTH = scaleForGUI(200);
    private static final int MINIMUM_COMPONENT_HEIGHT = scaleForGUI(400);

    private final LifePathBuilderTabType tabType;

    private final Map<SkillAttribute, Integer> storedAttributeScores;
    private Map<SkillAttribute, Integer> selectedAttributeScores;

    private final Integer storedEdge;
    private Integer selectedEdge;

    private final Integer storedFlexibleAttribute;
    private Integer selectedFlexibleAttribute;

    /**
     * Returns the per-attribute values the author settled on.
     *
     * @return the selected attributes and their values
     *
     * @since 0.50.11
     */
    Map<SkillAttribute, Integer> getSelectedAttributeScores() {
        return selectedAttributeScores;
    }

    /**
     * Returns the Edge value, or {@code null} when the author set none.
     *
     * @return the Edge value or {@code null}
     *
     * @since 0.50.11
     */
    @Nullable
    Integer getEdge() {
        return selectedEdge;
    }

    /**
     * Returns the "any attribute" value, or {@code null} when the author set none.
     *
     * @return the value or {@code null}
     *
     * @since 0.50.11
     */
    @Nullable
    Integer getFlexibleAttribute() {
        return selectedFlexibleAttribute;
    }

    /**
     * Opens the picker.
     *
     * @param owner                    the wizard this picker belongs to
     * @param selectedAttributeScores  the per-attribute values already on this group
     * @param selectedFlexibleAttribute the "any attribute" value already on this group, or {@code null}
     * @param selectedEdge             the Edge value already on this group, or {@code null}
     * @param tabType                  the section being edited
     * @param groupIndex               the group being edited, shown in the title
     *
     * @since 0.50.11
     */
    LifePathAttributePicker(@Nullable Window owner, Map<SkillAttribute, Integer> selectedAttributeScores,
          @Nullable Integer selectedFlexibleAttribute, @Nullable Integer selectedEdge,
          LifePathBuilderTabType tabType, int groupIndex) {
        super(owner, RESOURCE_BUNDLE, "LifePathAttributePicker", tabType, groupIndex, MINIMUM_MAIN_WIDTH,
              MINIMUM_COMPONENT_HEIGHT);

        this.tabType = tabType;

        // Defensive copies to avoid external modification
        this.selectedAttributeScores = new HashMap<>(selectedAttributeScores);
        // Edge lives in its own field, never in the attribute map. A file written while the picker still listed Edge
        // among the attributes can carry one here, and left in place it would be priced with no row to clear it.
        this.selectedAttributeScores.remove(SkillAttribute.EDGE);
        this.storedAttributeScores = new HashMap<>(this.selectedAttributeScores);

        this.selectedEdge = selectedEdge;
        this.storedEdge = selectedEdge;

        this.selectedFlexibleAttribute = selectedFlexibleAttribute;
        this.storedFlexibleAttribute = selectedFlexibleAttribute;

        buildAndShow();
    }

    @Override
    protected JPanel buildOptionsPanel() {
        JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));
        pnlOptions.setBorder(RoundedLineBorder.createRoundedLineBorder(getPickerText("options.label")));

        int attributeMinimumValue = getAttributeMinimumValue(tabType, false);
        int attributeMaximumValue = getAttributeMaximumValue(tabType, false);
        int attributeKeyValue = getDefaultAttributeValue(tabType, false);

        for (SkillAttribute attribute : SkillAttribute.values()) {
            // Edge is an attribute in the enum but gets its own row below, with its own bounds and its own storage.
            // Listing it here as well showed it twice and let the two rows disagree.
            boolean isPlaceholder = attribute == SkillAttribute.NO_ATTRIBUTE;
            boolean hasOwnRow = attribute == SkillAttribute.EDGE;
            if (isPlaceholder || hasOwnRow) {
                continue;
            }

            int storedValue = selectedAttributeScores.getOrDefault(attribute, attributeKeyValue);

            pnlOptions.add(buildAttributeRow(attribute.getLabel(),
                  attribute.getDescription(),
                  attributeMinimumValue,
                  attributeMaximumValue,
                  storedValue,
                  value -> {
                      if (value == attributeKeyValue) {
                          selectedAttributeScores.remove(attribute);
                      } else {
                          selectedAttributeScores.put(attribute, value);
                      }
                  }));
        }

        // Edge has its own bounds: unlike the other attributes it can legitimately be zero.
        int edgeMinimumValue = getAttributeMinimumValue(tabType, true);
        int edgeMaximumValue = getAttributeMaximumValue(tabType, true);
        int edgeKeyValue = getDefaultAttributeValue(tabType, true);

        pnlOptions.add(buildAttributeRow(getPickerText("edge.label"),
              getPickerText("edge.tooltip"),
              edgeMinimumValue,
              edgeMaximumValue,
              selectedEdge == null ? edgeKeyValue : selectedEdge,
              // Stored as null at the key value, the same way the "any attribute" row below behaves. Storing the key
              // value itself left every group carrying an Edge rule the author never set.
              value -> selectedEdge = (value == edgeKeyValue) ? null : value));

        pnlOptions.add(buildAttributeRow(getPickerText("flexible.label"),
              getPickerText("flexible.tooltip"),
              attributeMinimumValue,
              attributeMaximumValue,
              selectedFlexibleAttribute == null ? attributeKeyValue : selectedFlexibleAttribute,
              value -> selectedFlexibleAttribute = (value == attributeKeyValue) ? null : value));

        return pnlOptions;
    }

    /**
     * Builds one labelled spinner row.
     *
     * @param label        the row's label
     * @param tooltip      the help text shown on hover
     * @param minimumValue the lowest selectable value
     * @param maximumValue the highest selectable value
     * @param storedValue  the value to start at, as read from the Life Path
     * @param onChanged    what to do when the author moves the spinner
     *
     * @return the row
     *
     * @since 0.50.11
     */
    private JPanel buildAttributeRow(String label, String tooltip, int minimumValue, int maximumValue,
          int storedValue, Consumer<Integer> onChanged) {
        JLabel lblAttribute = new JLabel(label);
        lblAttribute.setToolTipText(tooltip);

        // A stored value can sit outside these bounds when a file was hand-edited or predates a change to the range.
        // Clamping keeps the row on screen; this used to return an empty panel instead, which hid the attribute
        // entirely so the author could neither see nor correct the bad value.
        int startingValue = clampSpinnerValue(storedValue, minimumValue, maximumValue, label);
        JSpinner spnAttributeScore = new JSpinner(new SpinnerNumberModel(startingValue, minimumValue, maximumValue,
              1));

        spnAttributeScore.addChangeListener(changeEvent -> onChanged.accept((Integer) spnAttributeScore.getValue()));
        lblAttribute.addMouseListener(TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltip));
        spnAttributeScore.addMouseListener(TooltipMouseListenerUtil.forTooltip(this::setLblTooltipDisplay, tooltip));

        JPanel pnlRows = new JPanel();
        pnlRows.setLayout(new BoxLayout(pnlRows, BoxLayout.X_AXIS));
        pnlRows.add(lblAttribute);
        pnlRows.add(Box.createHorizontalStrut(PADDING));
        pnlRows.add(spnAttributeScore);
        pnlRows.setAlignmentX(Component.LEFT_ALIGNMENT);

        return pnlRows;
    }

    @Override
    protected void restoreStoredSelection() {
        selectedAttributeScores = new HashMap<>(storedAttributeScores);
        selectedEdge = storedEdge;
        selectedFlexibleAttribute = storedFlexibleAttribute;
    }

    @Override
    protected void clearSelection() {
        selectedAttributeScores.clear();
        selectedEdge = null;
        selectedFlexibleAttribute = null;
        rebuildOptions();
    }
}
