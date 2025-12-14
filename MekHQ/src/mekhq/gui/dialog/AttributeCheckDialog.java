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
import static mekhq.campaign.enums.DailyReportType.SKILL_CHECKS;
import static mekhq.campaign.personnel.skills.AttributeCheckUtility.determineTargetNumber;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.BODY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.CHARISMA;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.DEXTERITY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.INTELLIGENCE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.REFLEXES;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.STRENGTH;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.WILLPOWER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.AttributeCheckUtility;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.ButtonLabelTooltipPair;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog that facilitates Attribute checks for a character.
 *
 * <p>This dialog allows the user to perform Attribute checks for specific Attributes by selecting the Attribute,
 * applying modifiers, and choosing whether to use Edge. It consists of an initial dialog to gather input, executes the
 * Attribute check, and then presents the result in a result dialog.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class AttributeCheckDialog {
    final static String RESOURCE_BUNDLE = "mekhq.resources.SkillCheckDialog";

    final static String DIALOG_IMAGE_FILENAME_DEFAULT = "data/images/misc/skill_check_default.png";
    final static String DIALOG_IMAGE_FILENAME_PASS = "data/images/misc/skill_check_pass.png";
    final static String DIALOG_IMAGE_FILENAME_FAIL = "data/images/misc/skill_check_fail.png";

    final static int DIALOG_CANCEL_INDEX = 0;
    final static int DIALOG_USE_EDGE_INDEX = 2;

    /**
     * A static list of attribute check options used within the dialog.
     *
     * <p>Each entry in the list corresponds to a combination of one or two skill attributes, represented by their
     * localized labels. This allows for the composition of various attribute checks based on predefined attribute
     * combinations, such as single attributes (e.g., "Body") or paired attributes (e.g., "Body-Charisma").</p>
     *
     * <p>The labels for these options are dynamically generated using the {@code getLabel} method of
     * {@link SkillAttribute}, retrieving localized strings defined in resource bundles.</p>
     */
    final static List<String> ATTRIBUTE_CHECK_OPTIONS = List.of(
          BODY.getLabel(),
          CHARISMA.getLabel(),
          DEXTERITY.getLabel(),
          INTELLIGENCE.getLabel(),
          REFLEXES.getLabel(),
          STRENGTH.getLabel(),
          BODY.getLabel() + "-" + CHARISMA.getLabel(),
          BODY.getLabel() + "-" + DEXTERITY.getLabel(),
          BODY.getLabel() + "-" + INTELLIGENCE.getLabel(),
          BODY.getLabel() + "-" + REFLEXES.getLabel(),
          BODY.getLabel() + "-" + STRENGTH.getLabel(),
          BODY.getLabel() + "-" + WILLPOWER.getLabel(),
          CHARISMA.getLabel() + "-" + DEXTERITY.getLabel(),
          CHARISMA.getLabel() + "-" + INTELLIGENCE.getLabel(),
          CHARISMA.getLabel() + "-" + REFLEXES.getLabel(),
          CHARISMA.getLabel() + "-" + STRENGTH.getLabel(),
          CHARISMA.getLabel() + "-" + WILLPOWER.getLabel(),
          DEXTERITY.getLabel() + "-" + INTELLIGENCE.getLabel(),
          DEXTERITY.getLabel() + "-" + REFLEXES.getLabel(),
          DEXTERITY.getLabel() + "-" + STRENGTH.getLabel(),
          DEXTERITY.getLabel() + "-" + WILLPOWER.getLabel(),
          INTELLIGENCE.getLabel() + "-" + REFLEXES.getLabel(),
          INTELLIGENCE.getLabel() + "-" + STRENGTH.getLabel(),
          INTELLIGENCE.getLabel() + "-" + WILLPOWER.getLabel(),
          REFLEXES.getLabel() + "-" + STRENGTH.getLabel(),
          REFLEXES.getLabel() + "-" + WILLPOWER.getLabel(),
          STRENGTH.getLabel() + "-" + WILLPOWER.getLabel());

    private final Campaign campaign;
    private final Person character;
    boolean isSuccess = false;


    /**
     * Constructs a {@code AttributeCheckDialog} for the specified character.
     *
     * <p>This constructor initializes the dialog, processes the selected attribute check, and displays the results. If
     * the user cancels the attribute check, no further action is taken.</p>
     *
     * @param campaign  the {@link Campaign} containing the current game state
     * @param character the {@link Person} performing the attribute check
     *
     * @author Illiani
     * @since 0.50.07
     */
    public AttributeCheckDialog(Campaign campaign, Person character) {
        this.campaign = campaign;
        this.character = character;

        // Initial Dialog
        ImmersiveDialogCore dialog = getInitialDialog();
        int choiceIndex = dialog.getDialogChoice();

        if (choiceIndex == DIALOG_CANCEL_INDEX) {
            return;
        }

        // Perform Check
        String results = performAttributeCheck(dialog.getComboBoxChoiceIndex(), dialog.getSpinnerValue(), choiceIndex);

        // Results Dialog
        campaign.addReport(SKILL_CHECKS, results.replaceAll("<p>", "<br><br>").replaceAll("</p>", ""));
        showResultsDialog(results);
    }


    /**
     * Creates and returns the initial dialog for Attribute check configuration.
     *
     * <p>This dialog gathers user input for the Attribute, modifier, and whether to use Edge or not.</p>
     *
     * @return an {@link ImmersiveDialogCore} instance for the initial dialog
     *
     * @author Illiani
     * @since 0.50.07
     */
    private ImmersiveDialogCore getInitialDialog() {
        return new ImmersiveDialogCore(campaign,
              character,
              null,
              getInCharacterMessage(),
              getButtons(character.getCurrentEdge() > 0, campaign.getCampaignOptions().isUseEdge()),
              getFormattedTextAt(RESOURCE_BUNDLE, "message.ooc.attribute"),
              null,
              false,
              getSupplementalPanel(),
              new ImageIcon(DIALOG_IMAGE_FILENAME_DEFAULT),
              true);
    }

    /**
     * Performs the Attribute check and returns the result as a string.
     *
     * @param selectedOption   the index of the attribute(s) selected in the ComboBox
     * @param selectedModifier the modifier applied to the roll
     * @param choiceIndex      the user's choice (e.g., whether to use Edge or not)
     *
     * @return a {@code String} containing the result of the Attribute check
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String performAttributeCheck(int selectedOption, int selectedModifier, int choiceIndex) {
        List<SkillAttribute> attributes = deriveAttributesFromOption(ATTRIBUTE_CHECK_OPTIONS.get(selectedOption));
        SkillAttribute firstAttribute = attributes.get(0);
        SkillAttribute secondAttribute = attributes.size() > 1 ? attributes.get(1) : null;
        boolean useEdge = choiceIndex == DIALOG_USE_EDGE_INDEX;
        AttributeCheckUtility utility = new AttributeCheckUtility(null,
              character,
              firstAttribute,
              secondAttribute,
              null,
              selectedModifier,
              useEdge,
              true);
        isSuccess = utility.isSuccess();

        return utility.getResultsText();
    }


    /**
     * Displays the results of the Attribute check in a results' dialog.
     *
     * @param results the results text to display
     *
     * @author Illiani
     * @since 0.50.07
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
     * @since 0.50.07
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
     * @since 0.50.07
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
     * <p>This panel includes a {@link MMComboBox} for selecting Attributes and a {@link JSpinner} for adding
     * modifiers.</p>
     *
     * @return a {@link JPanel} with additional input fields
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel getSupplementalPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = createBaseConstraints();

        // Add label for ComboBox
        JLabel lblAttributes = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "component.combo.attribute"));
        addComponent(panel, lblAttributes, constraints, 0, 0, 1, GridBagConstraints.NONE);

        // Add ComboBox
        MMComboBox<String> cboAttributes = new MMComboBox<>("cboAttributes", getComboListItems());
        addComponent(panel, cboAttributes, constraints, 1, 0, 2, GridBagConstraints.HORIZONTAL);

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
     * @since 0.50.07
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
     * @since 0.50.07
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
     * Generates a list of Attributes with formatted labels for display in the ComboBox.
     *
     * <p>Each label includes the Attribute name (bolded), target number, and any relevant modifiers.</p>
     *
     * @return a {@code String[]} containing the formatted Attribute labels
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String[] getComboListItems() {
        List<String> options = new ArrayList<>();

        for (String attributeOption : ATTRIBUTE_CHECK_OPTIONS) {
            List<SkillAttribute> attributes = deriveAttributesFromOption(attributeOption);
            SkillAttribute firstAttribute = attributes.get(0);
            SkillAttribute secondAttribute = attributes.size() > 1 ? attributes.get(1) : null;
            int targetNumber = determineTargetNumber(character, firstAttribute, secondAttribute, 0).getValue();

            // Build the label with the target number
            String formattedAttributeName = "<html><b>" + attributeOption + "</b>";
            String label = formattedAttributeName + " (" + targetNumber + "+)</html>";

            options.add(label);
        }

        // Convert the list to a String array and return it
        return options.toArray(new String[0]);
    }

    /**
     * Derives a list of {@link SkillAttribute} instances from the given option string.
     *
     * <p>The method checks the provided option to identify and collect all {@code SkillAttribute} values whose
     * labels are contained within the given string.</p>
     *
     * @param option the {@link String} representing the option to parse for attribute labels
     *
     * @return a {@link List} of {@link SkillAttribute} instances matching the labels found in the option string
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<SkillAttribute> deriveAttributesFromOption(String option) {
        List<SkillAttribute> attributes = new ArrayList<>();

        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (option.contains(attribute.getLabel())) {
                attributes.add(attribute);
            }
        }

        return attributes;
    }
}
