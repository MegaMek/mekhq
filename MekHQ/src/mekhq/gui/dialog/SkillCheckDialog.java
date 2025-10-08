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
package mekhq.gui.dialog;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.determineTargetNumber;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.ButtonLabelTooltipPair;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog that facilitates skill checks for a character.
 *
 * <p>This dialog allows the user to perform skill checks for a specific skill by selecting the skill, applying
 * modifiers, and choosing whether to use Edge. It consists of an initial dialog to gather input, executes the skill
 * check, and then presents the result in a results' dialog.</p>
 *
 * @author Illiani
 * @since 0.50.05
 */
public class SkillCheckDialog {
    final String RESOURCE_BUNDLE = "mekhq.resources.SkillCheckDialog";

    final String DIALOG_IMAGE_FILENAME_DEFAULT = "data/images/misc/skill_check_default.png";
    final String DIALOG_IMAGE_FILENAME_PASS = "data/images/misc/skill_check_pass.png";
    final String DIALOG_IMAGE_FILENAME_FAIL = "data/images/misc/skill_check_fail.png";

    final int DIALOG_CANCEL_INDEX = 0;
    final int DIALOG_USE_EDGE_INDEX = 2;

    private final Campaign campaign;
    private final Person character;
    boolean isSuccess = false;
    private final List<String> skillNames = new ArrayList<>();


    /**
     * Constructs a {@code SkillCheckDialog} for the specified campaign and character.
     *
     * <p>This constructor initializes the dialog, processes the selected skill check, and displays the results. If
     * the user cancels the skill check, no further action is taken.</p>
     *
     * @param campaign  the {@link Campaign} containing the current game state
     * @param character the {@link Person} performing the skill check
     *
     * @author Illiani
     * @since 0.50.05
     */
    public SkillCheckDialog(Campaign campaign, Person character) {
        this.campaign = campaign;
        this.character = character;

        boolean isUseAgingEffects = campaign.getCampaignOptions().isUseAgeEffects();
        boolean isClanCampaign = campaign.isClanCampaign();
        LocalDate today = campaign.getLocalDate();

        // Initial Dialog
        ImmersiveDialogCore dialog = getInitialDialog(isUseAgingEffects, isClanCampaign, today);
        int choiceIndex = dialog.getDialogChoice();

        if (choiceIndex == DIALOG_CANCEL_INDEX) {
            return;
        }

        // Perform Check
        String results = performSkillCheck(dialog.getComboBoxChoiceIndex(), dialog.getSpinnerValue(), choiceIndex,
              isUseAgingEffects, isClanCampaign, today);

        // Results Dialog
        campaign.addReport(results.replaceAll("<p>", "<br><br>").replaceAll("</p>", ""));
        showResultsDialog(results);
    }


    /**
     * Creates and returns the initial dialog for skill check configuration.
     *
     * <p>This dialog gathers user input for the skill, modifier, and whether to use Edge or not.</p>
     *
     * @return an {@link ImmersiveDialogCore} instance for the initial dialog
     *
     * @author Illiani
     * @since 0.50.05
     */
    private ImmersiveDialogCore getInitialDialog(boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        return new ImmersiveDialogCore(campaign,
              character,
              null,
              getInCharacterMessage(),
              getButtons(character.getCurrentEdge() > 0, campaign.getCampaignOptions().isUseEdge()),
              getFormattedTextAt(RESOURCE_BUNDLE, "message.ooc"),
              null,
              false,
              getSupplementalPanel(isUseAgingEffects, isClanCampaign, today),
              new ImageIcon(DIALOG_IMAGE_FILENAME_DEFAULT),
              true);
    }

    /**
     * Performs the skill check and returns the result as a string.
     *
     * @param selectedSkill    the index of the skill selected in the ComboBox
     * @param selectedModifier the modifier applied to the roll
     * @param choiceIndex      the user's choice (e.g., whether to use Edge or not)
     *
     * @return a {@code String} containing the result of the skill check
     *
     * @author Illiani
     * @since 0.50.05
     */
    private String performSkillCheck(int selectedSkill, int selectedModifier, int choiceIndex,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        String skillName = skillNames.get(selectedSkill);
        boolean useEdge = choiceIndex == DIALOG_USE_EDGE_INDEX;
        SkillCheckUtility utility = new SkillCheckUtility(character, skillName, null, selectedModifier, useEdge, true,
              isUseAgingEffects, isClanCampaign, today);
        isSuccess = utility.isSuccess();

        return utility.getResultsText();
    }


    /**
     * Displays the results of the skill check in a results' dialog.
     *
     * @param results the results text to display
     *
     * @author Illiani
     * @since 0.50.05
     */
    private void showResultsDialog(String results) {
        new ImmersiveDialogSimple(campaign,
              character,
              null,
              results,
              null,
              null,
              new ImageIcon(isSuccess ? DIALOG_IMAGE_FILENAME_PASS : DIALOG_IMAGE_FILENAME_FAIL),
              false);
    }

    /**
     * Retrieves the in-character message to display in the dialog.
     *
     * @return a {@code String} containing the in-character message
     *
     * @author Illiani
     * @since 0.50.05
     */
    private String getInCharacterMessage() {
        int variant = randomInt(50);
        return getFormattedTextAt(RESOURCE_BUNDLE, "message.ic." + variant);
    }

    /**
     * Retrieves the list of buttons for the dialog.
     *
     * <p>The buttons include Cancel, Attempt, and optionally Use Edge (if applicable).</p>
     *
     * @param hasEdge    whether the character has any Edge points available
     * @param allowsEdge whether the campaign allows Edge usage
     *
     * @return a {@code List} of {@link ButtonLabelTooltipPair} instances for dialog buttons
     *
     * @author Illiani
     * @since 0.50.05
     */
    private List<ButtonLabelTooltipPair> getButtons(boolean hasEdge, boolean allowsEdge) {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();
        buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, "button.cancel"), null));
        buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, "button.attempt"), null));

        if (hasEdge && allowsEdge) {
            buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, "button.edge"), null));
        }

        return buttons;
    }


    /**
     * Creates and returns the supplemental panel for the dialog.
     *
     * <p>This panel includes a {@link MMComboBox} for selecting skills and a {@link JSpinner} for adding
     * modifiers.</p>
     *
     * @return a {@link JPanel} with additional input fields
     *
     * @author Illiani
     * @since 0.50.05
     */
    private JPanel getSupplementalPanel(boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = createBaseConstraints();

        // Add label for ComboBox
        JLabel lblSkills = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "component.combo"));
        addComponent(panel, lblSkills, constraints, 0, 0, 1, GridBagConstraints.NONE);

        // Add ComboBox
        MMComboBox<String> cboSkills = new MMComboBox<>("cboSkills",
              getComboListItems(isUseAgingEffects, isClanCampaign, today));
        addComponent(panel, cboSkills, constraints, 1, 0, 2, GridBagConstraints.HORIZONTAL);

        // Add label for spinner
        JLabel lblModifiers = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "component.spinner"));
        addComponent(panel, lblModifiers, constraints, 0, 1, 1, GridBagConstraints.NONE);

        // Add spinner
        JSpinner spnModifiers = new JSpinner(new SpinnerNumberModel(0, -30, 10, 1));
        addComponent(panel, spnModifiers, constraints, 1, 1, 1, GridBagConstraints.NONE);

        return panel;
    }


    /**
     * Creates and returns the base {@link GridBagConstraints} for use in laying out the supplemental panel.
     *
     * @return a {@link GridBagConstraints} object with pre-configured values
     *
     * @author Illiani
     * @since 0.50.05
     */
    private GridBagConstraints createBaseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }


    /**
     * Adds a component to the supplemental panel with specified layout constraints.
     *
     * @param panel       the {@link JPanel} to add the component to
     * @param component   the {@link JComponent} to add
     * @param constraints the {@link GridBagConstraints} to control layout
     * @param gridX       the grid X-coordinate
     * @param gridY       the grid Y-coordinate
     * @param gridWidth   the width of the component in terms of grid cells
     * @param fill        the fill style (e.g., {@link GridBagConstraints#HORIZONTAL})
     *
     * @author Illiani
     * @since 0.50.05
     */
    private void addComponent(JPanel panel, JComponent component, GridBagConstraints constraints, int gridX, int gridY,
          int gridWidth, int fill) {
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.gridwidth = gridWidth;
        constraints.fill = fill;
        panel.add(component, constraints);
    }

    /**
     * Generates a list of skills with formatted labels for display in the ComboBox.
     *
     * <p>Each label includes the skill name (bolded), target number, and any relevant modifiers.</p>
     *
     * @return a {@code String[]} containing the formatted skill labels
     *
     * @author Illiani
     * @since 0.50.05
     */
    private String[] getComboListItems(boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        List<String> skills = new ArrayList<>();

        for (String skillName : SkillType.getSkillList()) {
            SkillType skillType = SkillType.getType(skillName);
            TargetRoll targetRoll = determineTargetNumber(character,
                  skillType,
                  0,
                  isUseAgingEffects,
                  isClanCampaign,
                  today);
            int targetNumber = targetRoll.getValue();
            boolean isCountsUp = SkillType.getType(skillName).isCountUp();

            // Build the label with the target number
            String formattedSkillName = "<html><b>" + skillName.replace(" (RP Only)", "") + "</b>";
            String label = formattedSkillName + " (" + targetNumber + (isCountsUp ? '-' : '+') + ")</html>";

            skills.add(label);
            skillNames.add(skillName);
        }

        // Convert the list to a String array and return it
        return skills.toArray(new String[0]);
    }
}
