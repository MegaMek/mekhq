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
package mekhq.gui.commandGeneration.contents;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.gui.commandGeneration.components.CommandGenerationLabel;
import mekhq.gui.commandGeneration.components.CommandGenerationStandardPanel;

/**
 * Spares tab. Mirrors the AutoLogistics restock percentages from Campaign Options' Acquisition &amp;
 * Delivery page: the same thirteen part categories, in the same order, with the same labels,
 * tooltips, and 0-10000 range, one spinner per category {@code PartsInUseManager.findStockUpAmount}
 * tracks.
 *
 * <p>The values ARE the campaign options: spinners load from and write back to the campaign's
 * {@link CampaignOptions} {@code autoLogistics*} fields directly, so the tab always opens showing
 * whatever is set in Campaign Options and edits here apply to the campaign on OK - no field
 * duplication on {@link CommandGenerationOptions}. The same percentages drive both the starting
 * spare inventory at generation time and the campaign's ongoing auto-logistics resupply during
 * play.</p>
 */
public class SparesTab {

    // Mirror the Campaign Options AutoLogistics spinners (AcquisitionPage.createAutoLogisticsPanel):
    // 0-10000 in steps of 1, so no value settable there is ever clamped or rounded here.
    private static final int MIN_PERCENT = 0;
    private static final int MAX_PERCENT = 10000;
    private static final int STEP_PERCENT = 1;

    private final Campaign campaign;
    private CommandGenerationOptions options;

    /** Ordered map: bundle-key suffix (also the lbl{key}.text key) → spinner. */
    private final Map<String, JSpinner> spinners = new LinkedHashMap<>();

    public SparesTab(Campaign campaign, CommandGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    public JPanel createTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setName("pnlSparesTab");

        panel.add(buildSparesSection());
        panel.add(Box.createVerticalStrut(6));
        panel.add(buildHelpSection());

        return panel;
    }

    private JPanel buildSparesSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "SparesPercentages", true, "SparesPercentages");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        // Same categories and order as the Campaign Options AutoLogistics grid.
        String[] keys = {
              "SparesMekHead",
              "SparesMekLocation",
              "SparesNonRepairableLocation",
              "SparesArmor",
              "SparesAmmunition",
              "SparesHeatSink",
              "SparesWeapons",
              "SparesActuators",
              "SparesJumpJets",
              "SparesHeadComponents",
              "SparesEngines",
              "SparesGyros",
              "SparesOther"
        };

        int row = 0;
        for (String key : keys) {
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(100, MIN_PERCENT, MAX_PERCENT, STEP_PERCENT));
            spinner.setName("spn" + key);
            spinners.put(key, spinner);

            JLabel label = new CommandGenerationLabel(key);
            label.setLabelFor(spinner);

            gbc.gridy = row;
            gbc.gridx = 0;
            section.add(label, gbc);
            gbc.gridx = 1;
            section.add(spinner, gbc);
            row++;
        }

        return section;
    }

    private JPanel buildHelpSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "SparesHelp", true, "SparesHelp");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        section.add(new CommandGenerationLabel("SparesHelpBody", true), gbc);
        return section;
    }

    private static GridBagConstraints sectionConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 6, 3, 6);
        return gbc;
    }

    /**
     * Reads percentages from the campaign's {@link CampaignOptions} into the spinners. Note: this
     * source-of-truth is the {@code Campaign} itself, not the supplied {@code sourceOptions} — that
     * argument exists for parity with the other Tab classes but is unused here. The same
     * autoLogistics percentages drive ongoing campaign resupply, so the dialog operates on them
     * directly rather than copying through a Company-Generation-only mirror field.
     */
    public void loadValuesFromOptions(CommandGenerationOptions sourceOptions) {
        this.options = sourceOptions;
        if (campaign == null) {
            return;
        }
        CampaignOptions co = campaign.getCampaignOptions();
        if (co == null) {
            return;
        }
        spinners.get("SparesMekHead").setValue(clamp(co.getAutoLogisticsMekHead()));
        spinners.get("SparesMekLocation").setValue(clamp(co.getAutoLogisticsMekLocation()));
        spinners.get("SparesNonRepairableLocation").setValue(clamp(co.getAutoLogisticsNonRepairableLocation()));
        spinners.get("SparesArmor").setValue(clamp(co.getAutoLogisticsArmor()));
        spinners.get("SparesAmmunition").setValue(clamp(co.getAutoLogisticsAmmunition()));
        spinners.get("SparesHeatSink").setValue(clamp(co.getAutoLogisticsHeatSink()));
        spinners.get("SparesWeapons").setValue(clamp(co.getAutoLogisticsWeapons()));
        spinners.get("SparesActuators").setValue(clamp(co.getAutoLogisticsActuators()));
        spinners.get("SparesJumpJets").setValue(clamp(co.getAutoLogisticsJumpJets()));
        spinners.get("SparesHeadComponents").setValue(clamp(co.getAutoLogisticsHeadComponents()));
        spinners.get("SparesEngines").setValue(clamp(co.getAutoLogisticsEngines()));
        spinners.get("SparesGyros").setValue(clamp(co.getAutoLogisticsGyros()));
        spinners.get("SparesOther").setValue(clamp(co.getAutoLogisticsOther()));
    }

    /**
     * Writes the spinner values back to the campaign's {@link CampaignOptions}. The
     * {@code targetOptions} argument is unused here — see {@link #loadValuesFromOptions} for why.
     * The Company Generation pipeline reads the same percentages from the campaign at generation time
     * via {@code PartsInUseManager.findStockUpAmount}, so this write makes the dialog's selections
     * effective immediately for both the initial spawn and ongoing resupply.
     */
    public void writeValuesToOptions(CommandGenerationOptions targetOptions) {
        if (campaign == null) {
            return;
        }
        CampaignOptions co = campaign.getCampaignOptions();
        if (co == null) {
            return;
        }
        co.setAutoLogisticsMekHead((Integer) spinners.get("SparesMekHead").getValue());
        co.setAutoLogisticsMekLocation((Integer) spinners.get("SparesMekLocation").getValue());
        co.setAutoLogisticsNonRepairableLocation((Integer) spinners.get("SparesNonRepairableLocation").getValue());
        co.setAutoLogisticsArmor((Integer) spinners.get("SparesArmor").getValue());
        co.setAutoLogisticsAmmunition((Integer) spinners.get("SparesAmmunition").getValue());
        co.setAutoLogisticsHeatSink((Integer) spinners.get("SparesHeatSink").getValue());
        co.setAutoLogisticsWeapons((Integer) spinners.get("SparesWeapons").getValue());
        co.setAutoLogisticsActuators((Integer) spinners.get("SparesActuators").getValue());
        co.setAutoLogisticsJumpJets((Integer) spinners.get("SparesJumpJets").getValue());
        co.setAutoLogisticsHeadComponents((Integer) spinners.get("SparesHeadComponents").getValue());
        co.setAutoLogisticsEngines((Integer) spinners.get("SparesEngines").getValue());
        co.setAutoLogisticsGyros((Integer) spinners.get("SparesGyros").getValue());
        co.setAutoLogisticsOther((Integer) spinners.get("SparesOther").getValue());
    }

    private static int clamp(int value) {
        if (value < MIN_PERCENT) return MIN_PERCENT;
        if (value > MAX_PERCENT) return MAX_PERCENT;
        return value;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CommandGenerationOptions getOptions() {
        return options;
    }
}
