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
package mekhq.gui.companyGeneration.contents;

import java.awt.BorderLayout;
import javax.swing.JPanel;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

/**
 * Wraps the embedded {@link ForceGeneratorOptionsView} from MegaMek. The faction / echelon / unit
 * type / weight class / rating / experience / augmentation / transport-percent / mission-role pickers
 * the user actually drives at generation time.
 *
 * <p>The view's own Generate / Export MUL / Clear buttons are hidden — the dialog's OK button drives
 * the pipeline. The year field is locked to the campaign year because {@code CompanyGenerator}'s
 * Stage 0 re-anchors year from the campaign regardless of what the panel shows; locking the field
 * keeps the displayed value honest.</p>
 *
 * <p>The dialog reads the user's selections on OK via {@link #getOptionsView()} →
 * {@link ForceGeneratorOptionsView#buildForceDescriptor()}, then feeds the result into
 * {@code CompanyGenerationOptions.getForceDescriptorSnapshot().populateFromForceDescriptor(fd)}.</p>
 */
public class ForceGeneratorTab {

    private final Campaign campaign;
    private CompanyGenerationOptions options;
    private ForceGeneratorOptionsView optionsView;

    public ForceGeneratorTab(Campaign campaign, CompanyGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    public JPanel createTab() {
        // Constructed lazily here so we only pay the RATGenerator / Ruleset / MekSummaryCache
        // initialization cost when the dialog is actually shown. The on-generate Consumer is a
        // no-op because we hide the view's own Generate button and route generation through the
        // dialog's OK action.
        optionsView = new ForceGeneratorOptionsView(fd -> {},
              campaign == null ? null : campaign.getGameOptions());
        optionsView.setGenerateButtonVisible(false);
        optionsView.setExportMULButtonVisible(false);
        optionsView.setClearButtonVisible(false);
        optionsView.setYearFieldEditable(false);
        if (campaign != null) {
            optionsView.setCurrentYear(campaign.getGameYear());
        }

        JPanel host = new JPanel(new BorderLayout());
        host.setName("pnlForceGeneratorTab");
        host.add(optionsView, BorderLayout.CENTER);
        return host;
    }

    /**
     * Returns the embedded {@link ForceGeneratorOptionsView}, or {@code null} if {@link #createTab()}
     * hasn't run yet. The Company Generation dialog calls this on OK to read the user's selections.
     */
    public ForceGeneratorOptionsView getOptionsView() {
        return optionsView;
    }

    /**
     * Convenience: builds the {@link ForceDescriptor} from the current view state. Equivalent to
     * {@code getOptionsView().buildForceDescriptor()} with a null-guard.
     */
    public ForceDescriptor buildForceDescriptor() {
        return optionsView == null ? null : optionsView.buildForceDescriptor();
    }

    /**
     * The view's current state is the source of truth — there's nothing to push from the options here
     * because the snapshot the dialog persists is generated from the view's controls on OK rather than
     * driving them. If a future preset round-trip needs to set the view's controls from saved values,
     * that goes here.
     */
    public void loadValuesFromOptions(CompanyGenerationOptions sourceOptions) {
        this.options = sourceOptions;
    }

    /**
     * Reads the user's force-shape picks back into the options' snapshot. The dialog can call this on
     * OK as an alternative to going through {@link #getOptionsView()} directly.
     */
    public void writeValuesToOptions(CompanyGenerationOptions targetOptions) {
        if (targetOptions == null || optionsView == null) {
            return;
        }
        ForceDescriptor fd = optionsView.buildForceDescriptor();
        if (fd != null) {
            targetOptions.getForceDescriptorSnapshot().populateFromForceDescriptor(fd);
        }
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }
}
