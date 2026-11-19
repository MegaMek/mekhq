/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.gui.campaignOptions.CampaignOptionsPane.triggerUpgradeFreebies;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptionsFreebieTracker;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.campaignOptions.CampaignOptionsDialog;
import mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode;
import mekhq.gui.dialog.CampaignUpgradeConsole.Choice;

/**
 * Upgrades a {@link Campaign} that was saved in an older release.
 *
 * <p>The player makes their choice in the {@link CampaignUpgradeConsole}: apply a preset, keep their current settings,
 * or review everything in Campaign Options. This class acts on that choice. Canceling Campaign Options returns the
 * player to the console; only an explicit, confirmed quit closes MekHQ, which leaves the save untouched.</p>
 *
 * <p><b>Thread Safety</b></p>
 * <p>Called from the campaign loader's worker thread, which this blocks until the upgrade is finished so the campaign
 * is never used mid-upgrade. Every window is built and shown on the Event Dispatch Thread.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class CampaignUpgradeDialog {
    private static final MMLogger LOGGER = MMLogger.create(CampaignUpgradeDialog.class);
    private static final String RESOURCE_BUNDLE = CampaignUpgradeConsole.RESOURCE_BUNDLE;

    private CampaignUpgradeDialog() {}

    /**
     * Shows the upgrade console for the given campaign and applies the player's choice.
     *
     * @param app      the application context
     * @param campaign the campaign being upgraded; must not be {@code null}
     * @param saveFile the save the campaign was loaded from, offered for backup; {@code null} if unknown
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void campaignUpgradeDialog(MekHQ app, Campaign campaign, @Nullable File saveFile) {
        List<CampaignPreset> presets = CampaignPreset.getCampaignPresets();

        // This should never happen under normal circumstances, as MekHQ always ships with a battery of presets.
        if (presets.isEmpty()) {
            LOGGER.errorDialog("Error", "No campaign presets found");
            exitApp(app);
            return;
        }

        CampaignUpgradeConsole console = onEventDispatchThread(
              () -> new CampaignUpgradeConsole(campaign, presets, saveFile));
        try {
            while (true) {
                Choice choice = onEventDispatchThread(console::showAndWait);
                switch (choice.decision()) {
                    case QUIT -> {
                        LOGGER.info("Player quit instead of upgrading {}", campaign.getPlayerForce().getName());
                        exitApp(app);
                        return;
                    }
                    case APPLY_PRESET -> {
                        if (choice.preset() == null) {
                            LOGGER.error("No preset chosen; returning to the upgrade console");
                            continue;
                        }
                        applyPreset(campaign, choice.preset());
                    }
                    case KEEP_SETTINGS -> LOGGER.info("Keeping the current settings during upgrade process");
                    case REVIEW_BY_HAND -> {
                        boolean isCanceled = onEventDispatchThread(() -> {
                            CampaignOptionsDialog optionsDialog = CampaignOptionsDialog.create(null, campaign, null,
                                  CampaignOptionsDialogMode.NORMAL);
                            optionsDialog.setVisible(true);
                            return optionsDialog.wasCanceled();
                        });
                        if (isCanceled) {
                            continue;
                        }
                    }
                }
                break;
            }
        } finally {
            SwingUtilities.invokeLater(console::dispose);
        }

        SwingUtilities.invokeLater(() -> campaign.addReport(DailyReportType.GENERAL,
              getFormattedTextAt(RESOURCE_BUNDLE, "CampaignUpgradeDialog.upgrading",
                    MHQConstants.VERSION.toString())));
    }

    /**
     * Replaces the campaign's options, skills, and special abilities with a preset's, then (on the Event Dispatch
     * Thread) runs the follow-up dialogs for any system the preset switched on.
     *
     * @param campaign     the campaign being upgraded
     * @param chosenPreset the preset to apply
     */
    private static void applyPreset(Campaign campaign, CampaignPreset chosenPreset) {
        // This needs to be before we start changing options
        CampaignOptionsFreebieTracker oldOptions = new CampaignOptionsFreebieTracker(campaign.getCampaignOptions());

        // Preset getters are @Nullable; skip any section the preset does not provide so we
        // do not overwrite the campaign's existing options with null (issue #8945).
        if (chosenPreset.getCampaignOptions() != null) {
            campaign.setCampaignOptions(chosenPreset.getCampaignOptions());
        }
        if (chosenPreset.getGameOptions() != null) {
            campaign.setGameOptions(chosenPreset.getGameOptions());
        }
        if (chosenPreset.getRandomSkillPreferences() != null) {
            campaign.setRandomSkillPreferences(chosenPreset.getRandomSkillPreferences());
        }

        // Keep the MegaMek options that mirror campaign options (armor kits, edge, tech level, ...) in step
        campaign.getCampaignOptions().updateGameOptionsFromCampaignOptions(campaign.getGameOptions());

        Map<String, SkillType> presetSkills = chosenPreset.getSkills();
        for (final String skillName : SkillType.getSkillList()) {
            SkillType storedType = SkillType.getType(skillName);
            SkillType presetType = presetSkills.get(skillName);

            if (storedType == null || presetType == null) {
                LOGGER.info("Skipping outdated or missing skill: {}", skillName);
                continue;
            }

            // Update Target Number
            storedType.setTarget(presetType.getTarget());

            // Update Skill Costs
            int size = storedType.getCosts().length;
            Integer[] presetCosts = presetType.getCosts();
            for (int level = 0; level < size; level++) {
                try {
                    int cost = presetCosts[level];
                    SkillType.setCost(skillName, cost, level);
                } catch (IllegalArgumentException e) {
                    LOGGER.info("Skipping outdated or missing skill level: {}", skillName);
                }
            }

            storedType.setNaturalAptitudeCost(presetType.getNaturalAptitudeCost());

            // Update Skill Milestones
            storedType.setGreenLevel(presetType.getGreenLevel());
            storedType.setRegularLevel(presetType.getRegularLevel());
            storedType.setVeteranLevel(presetType.getVeteranLevel());
            storedType.setEliteLevel(presetType.getEliteLevel());
            storedType.setHeroicLevel(presetType.getHeroicLevel());
            storedType.setLegendaryLevel(presetType.getLegendaryLevel());
        }

        SpecialAbility.replaceSpecialAbilities(chosenPreset.getSpecialAbilities());

        CampaignOptionsFreebieTracker newOptions = new CampaignOptionsFreebieTracker(campaign.getCampaignOptions());
        SwingUtilities.invokeLater(() -> {
            triggerUpgradeFreebies(campaign, oldOptions, newOptions, false);

            LOGGER.info("Applying '{}' during upgrade process", chosenPreset.getTitle());
            MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
        });
    }

    /**
     * Runs a task on the Event Dispatch Thread and waits for its result. Runs it directly if already on that thread.
     *
     * @param task the task
     * @param <T>  the result type
     *
     * @return the task's result
     */
    private static <T> T onEventDispatchThread(Supplier<T> task) {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(task.get()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the upgrade console", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("The upgrade console failed", exception.getCause());
        }
        return result.get();
    }

    /**
     * Exits the application gracefully.
     *
     * <p><b>Notes:</b> This is used to ensure that any campaign passing through the upgrader is upgraded. If the
     * player chooses to quit instead, we want the application to exit without the campaign being opened.</p>
     *
     * @param mekHQ The instance of MekHQ to close.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void exitApp(MekHQ mekHQ) {
        mekHQ.exit(false);
    }
}
