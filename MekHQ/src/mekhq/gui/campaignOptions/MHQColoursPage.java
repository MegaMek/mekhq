/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.buttons.ColourSelectorButton;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;

/**
 * The Colours page of the MekHQ Client Options dialog: grids of foreground/background colour swatches for unit status,
 * personnel status, other lists (contracts, finance, map), and skill feedback. Owns its colour buttons and copies them
 * back into the shared {@link MHQOptionsModel} in {@link #writeToModel()}.
 */
class MHQColoursPage extends MHQOptionsPage {
    // Colours (keyed by resource name, matching MHQOptionsModel.statusColours)
    private final Map<String, ColourSelectorButton> colourButtons = new HashMap<>();

    MHQColoursPage(MHQOptionsModel model) {
        super(model);
    }

    @Override
    Component createPage() {
        List<JComponent> unitStatusButtons = createColoursUnitStatusButtons();
        List<JComponent> personnelStatusButtons = createColoursPersonnelStatusButtons();
        List<JComponent> otherButtons = createColoursOtherButtons();
        List<JComponent> skillFeedbackButtons = createColoursSkillFeedbackButtons();
        // Size every colour button on the page to one shared width (across all sections) so all the 2-column grids
        // line their columns up at the same x - the way the checkbox grids do through a shared label-column width.
        // Letting each section size its own buttons would let their differing longest labels push the columns to
        // different positions.
        List<JComponent> allButtons = new ArrayList<>(unitStatusButtons);
        allButtons.addAll(personnelStatusButtons);
        allButtons.addAll(otherButtons);
        allButtons.addAll(skillFeedbackButtons);
        setUniformWidth(allButtons);

        JComponent unitStatusContent = colourButtonGrid("MHQColoursUnitStatusContent", unitStatusButtons);
        JComponent personnelStatusContent = colourButtonGrid("MHQColoursPersonnelStatusContent", personnelStatusButtons);
        JComponent otherContent = colourButtonGrid("MHQColoursOtherContent", otherButtons);
        JComponent skillFeedbackContent = colourButtonGrid("MHQColoursSkillFeedbackContent", skillFeedbackButtons);
        // Register each section body's tips before the page is built; |= always evaluates its right side, so no
        // section's registration is skipped.
        boolean hasTooltips = registerDetailsTips(unitStatusContent);
        hasTooltips |= registerDetailsTips(personnelStatusContent);
        hasTooltips |= registerDetailsTips(otherContent);
        hasTooltips |= registerDetailsTips(skillFeedbackContent);
        // The disclaimer that some colours live in MegaMek's Client Options is shown as the page intro, above the
        // sections.
        Component page = pageBuilder("MHQColoursPage", hasTooltips)
                     .intro("coloursTab.disclaimer")
                     .section("lblMHQColoursUnitStatusSection.text", "lblMHQColoursUnitStatusSection.summary",
                           unitStatusContent)
                     .section("lblMHQColoursPersonnelStatusSection.text",
                           "lblMHQColoursPersonnelStatusSection.summary", personnelStatusContent)
                     .section("lblMHQColoursOtherSection.text", "lblMHQColoursOtherSection.summary", otherContent)
                     .section("lblMHQColoursSkillFeedbackSection.text", "lblMHQColoursSkillFeedbackSection.summary",
                           skillFeedbackContent)
                     .build();
        created = true;
        return page;
    }

    private List<JComponent> createColoursUnitStatusButtons() {
        // Order follows Unit#determineForegroundColor so the swatches read in the same priority the app applies them.
        // Deployed is shared with personnel (Unit and the personnel list both use it) and lives here as a unit state.
        return buildColourButtons(
              "optionDeployedForeground", "optionDeployedBackground",
              "optionInTransitForeground", "optionInTransitBackground",
              "optionQueuedForTravelForeground", "optionQueuedForTravelBackground",
              "optionRefittingForeground", "optionRefittingBackground",
              "optionMothballingForeground", "optionMothballingBackground",
              "optionMothballedForeground", "optionMothballedBackground",
              "optionUnmaintainedForeground", "optionUnmaintainedBackground",
              "optionNotRepairableForeground", "optionNotRepairableBackground",
              "optionNonFunctionalForeground", "optionNonFunctionalBackground",
              "optionNeedsPartsFixedForeground", "optionNeedsPartsFixedBackground",
              "optionUncrewedForeground", "optionUncrewedBackground");
    }

    private List<JComponent> createColoursPersonnelStatusButtons() {
        // Order follows PersonnelTableModel's status priority.
        return buildColourButtons(
              "optionAbsentForeground", "optionAbsentBackground",
              "optionGoneForeground", "optionGoneBackground",
              "optionAwayFromMainForceForeground", "optionAwayFromMainForceBackground",
              "optionInjuredForeground", "optionInjuredBackground",
              "optionPregnantForeground", "optionPregnantBackground",
              "optionFatiguedForeground", "optionFatiguedBackground",
              "optionHealedInjuriesForeground", "optionHealedInjuriesBackground");
    }

    private List<JComponent> createColoursOtherButtons() {
        // Colours used outside the unit and personnel lists: contract deployment coverage, the finances loan table,
        // and StratCon map hex coordinates.
        return buildColourButtons(
              "optionBelowContractMinimumForeground", "optionBelowContractMinimumBackground",
              "optionLoanOverdueForeground", "optionLoanOverdueBackground",
              "optionStratConHexCoordForeground");
    }

    private List<JComponent> createColoursSkillFeedbackButtons() {
        return buildColourButtons(
              "optionFontColorNegative", "optionFontColorWarning",
              "optionFontColorPositive", "optionFontColorAmazing",
              "optionFontColorSkillUltraGreen", "optionFontColorSkillGreen",
              "optionFontColorSkillRegular", "optionFontColorSkillVeteran",
              "optionFontColorSkillElite");
    }

    /**
     * Creates a colour button for each of {@code keys} (loading its initial colour from
     * {@link MHQOptionsModel#statusColours}), registers it in {@link #colourButtons} under its key so
     * {@link #writeToModel()} can read it back, left-aligns its swatch and text, and returns the buttons in
     * order for placement in a grid. Button widths are equalised later, once every section's buttons exist (see
     * {@link #setUniformWidth(List)}), so the sections' grid columns line up with each other.
     */
    private List<JComponent> buildColourButtons(String... keys) {
        List<JComponent> buttons = new ArrayList<>();
        for (String key : keys) {
            ColourSelectorButton button = colourButton(key, model.statusColours.get(key));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            colourButtons.put(key, button);
            buttons.add(button);
        }
        return buttons;
    }

    /** Lays {@code buttons} out in a 2-column colour grid panel named {@code name}. */
    private JPanel colourButtonGrid(String name, List<JComponent> buttons) {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel(name);
        panel.addComponentGrid(2, buttons.toArray(new JComponent[0]));
        return panel;
    }

    @Override
    void writeToModel() {
        if (!created) {
            return;
        }
        colourButtons.forEach((key, button) -> model.statusColours.put(key, button.getColour()));
    }
}
