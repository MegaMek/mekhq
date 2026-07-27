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

import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The Save Options page of the MekHQ Client Options dialog: an Autosave section (frequency radio buttons, save-before
 * triggers, and the saved-game count) and a Campaign Save section (XML output toggles). Owns its controls and copies
 * them back into the shared {@link MHQOptionsModel} in {@link #writeToModel()}.
 */
class MHQSaveOptionsPage extends MHQOptionsPage {
    // Autosave
    private JRadioButton optionNoSave;
    private JRadioButton optionSaveDaily;
    private JRadioButton optionSaveWeekly;
    private JRadioButton optionSaveMonthly;
    private JRadioButton optionSaveYearly;
    private CampaignOptionsCheckBox chkSaveBeforeScenarios;
    private CampaignOptionsCheckBox chkSaveBeforeMissionEnd;
    private CampaignOptionsSpinner spinnerSavedGamesCount;

    // Campaign Save
    private CampaignOptionsCheckBox chkPreferGzippedOutput;
    private CampaignOptionsCheckBox chkWriteCustomsToXML;
    private CampaignOptionsCheckBox chkWriteAllUnitsToXML;
    private CampaignOptionsCheckBox chkSaveMothballState;

    MHQSaveOptionsPage(MHQOptionsModel model) {
        super(model);
    }

    @Override
    Component createPage() {
        JComponent autosaveContent = createAutosaveSection();
        JComponent campaignSaveContent = createCampaignSaveSection();
        // Register each section body's tips before the page is built; |= always evaluates its right side, so no
        // section's registration is skipped.
        boolean hasTooltips = registerDetailsTips(autosaveContent);
        hasTooltips |= registerDetailsTips(campaignSaveContent);
        Component page = pageBuilder("MHQSaveOptionsPage", hasTooltips)
                     .section("lblMHQAutosaveSection.text", "lblMHQAutosaveSection.summary", autosaveContent)
                     .section("lblMHQCampaignSaveSection.text", "lblMHQCampaignSaveSection.summary",
                           campaignSaveContent)
                     .build();
        created = true;
        return page;
    }

    private JPanel createCampaignSaveSection() {
        chkPreferGzippedOutput = checkBox("optionPreferGzippedOutput", model.preferGzippedOutput);
        chkWriteCustomsToXML = checkBox("optionWriteCustomsToXML", model.writeCustomsToXML);
        chkWriteAllUnitsToXML = checkBox("optionWriteAllUnitsToXML", model.writeAllUnitsToXML,
              getMetadata(null, CampaignOptionFlag.IMPORTANT));
        chkSaveMothballState = checkBox("optionSaveMothballState", model.saveMothballState);

        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQCampaignSaveContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        panel.addCheckBoxGrid(2, chkPreferGzippedOutput, chkWriteCustomsToXML, chkWriteAllUnitsToXML,
              chkSaveMothballState);
        return panel;
    }

    private JPanel createAutosaveSection() {
        ButtonGroup saveFrequencyGroup = new ButtonGroup();
        optionNoSave = createAutosaveFrequencyOption("optionNoSave", saveFrequencyGroup);
        optionNoSave.setSelected(model.noAutosave);
        optionSaveDaily = createAutosaveFrequencyOption("optionSaveDaily", saveFrequencyGroup);
        optionSaveDaily.setSelected(model.autosaveDaily);
        optionSaveWeekly = createAutosaveFrequencyOption("optionSaveWeekly", saveFrequencyGroup);
        optionSaveWeekly.setSelected(model.autosaveWeekly);
        optionSaveMonthly = createAutosaveFrequencyOption("optionSaveMonthly", saveFrequencyGroup);
        optionSaveMonthly.setSelected(model.autosaveMonthly);
        optionSaveYearly = createAutosaveFrequencyOption("optionSaveYearly", saveFrequencyGroup);
        optionSaveYearly.setSelected(model.autosaveYearly);

        chkSaveBeforeScenarios = checkBox("checkSaveBeforeScenarios", model.autosaveBeforeScenarios);
        chkSaveBeforeMissionEnd = checkBox("checkSaveBeforeMissionEnd", model.autosaveBeforeMissionEnd);

        CampaignOptionsLabel labelSavedGamesCount = new CampaignOptionsLabel(RESOURCE_BUNDLE, "labelSavedGamesCount");
        spinnerSavedGamesCount =
              new CampaignOptionsSpinner(RESOURCE_BUNDLE, "labelSavedGamesCount", 1, 1, 10, 1);
        spinnerSavedGamesCount.setValue(model.maximumNumberOfAutoSaves);

        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQAutosaveContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        panel.addComponentGrid(1, optionNoSave, optionSaveDaily, optionSaveWeekly, optionSaveMonthly, optionSaveYearly);
        panel.addCheckBoxGrid(2, chkSaveBeforeScenarios, chkSaveBeforeMissionEnd);
        panel.addRow(labelSavedGamesCount, spinnerSavedGamesCount);
        return panel;
    }

    /**
     * Creates a save-frequency radio button whose label comes from {@code resourceName + ".text"} in the GUI bundle,
     * applies the shared campaign-options font scaling so it matches the check boxes on the page, and adds it to the
     * mutually-exclusive frequency group.
     */
    private JRadioButton createAutosaveFrequencyOption(String resourceName, ButtonGroup group) {
        JRadioButton radioButton = new JRadioButton(getTextAt(RESOURCE_BUNDLE, resourceName + ".text"));
        radioButton.setName(resourceName);
        setFontScaling(radioButton, false, 1);
        group.add(radioButton);
        return radioButton;
    }

    @Override
    void writeToModel() {
        if (!created) {
            return;
        }
        model.noAutosave = optionNoSave.isSelected();
        model.autosaveDaily = optionSaveDaily.isSelected();
        model.autosaveWeekly = optionSaveWeekly.isSelected();
        model.autosaveMonthly = optionSaveMonthly.isSelected();
        model.autosaveYearly = optionSaveYearly.isSelected();
        model.autosaveBeforeScenarios = chkSaveBeforeScenarios.isSelected();
        model.autosaveBeforeMissionEnd = chkSaveBeforeMissionEnd.isSelected();
        model.maximumNumberOfAutoSaves = (Integer) spinnerSavedGamesCount.getValue();
        model.preferGzippedOutput = chkPreferGzippedOutput.isSelected();
        model.writeCustomsToXML = chkWriteCustomsToXML.isSelected();
        model.writeAllUnitsToXML = chkWriteAllUnitsToXML.isSelected();
        model.saveMothballState = chkSaveMothballState.isSelected();
    }
}
