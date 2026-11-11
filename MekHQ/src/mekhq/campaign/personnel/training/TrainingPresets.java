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
package mekhq.campaign.personnel.training;

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A campaign's saved auto-train presets, and running the ones marked to run at the start of each month.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class TrainingPresets {
    private static final MMLogger LOGGER = MMLogger.create(TrainingPresets.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.TrainingConsole";

    private final List<AutoTrainSettings> presets = new ArrayList<>();

    /** @return the presets, in the order the player saved them */
    public List<AutoTrainSettings> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    /**
     * @param name a preset's name
     *
     * @return the preset with that name, ignoring case, or {@code null}
     */
    public @Nullable AutoTrainSettings find(String name) {
        for (AutoTrainSettings preset : presets) {
            if (preset.name().equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return null;
    }

    /**
     * Saves a preset, replacing any with the same name.
     *
     * @param preset the preset
     */
    public void save(AutoTrainSettings preset) {
        for (int index = 0; index < presets.size(); index++) {
            if (presets.get(index).name().equalsIgnoreCase(preset.name())) {
                presets.set(index, preset);
                return;
            }
        }
        presets.add(preset);
    }

    /**
     * @param name a preset's name
     *
     * @return {@code true} if a preset with that name was removed
     */
    public boolean remove(String name) {
        return presets.removeIf(preset -> preset.name().equalsIgnoreCase(name));
    }

    /**
     * Runs every preset marked to run monthly, in order, on the campaign's active personnel. A preset whose training
     * fees the campaign can't pay is skipped, with a note in the daily report.
     *
     * @param campaign the campaign
     */
    public void runMonthly(Campaign campaign) {
        for (AutoTrainSettings preset : presets) {
            if (!preset.isRunMonthly()) {
                continue;
            }
            List<Person> people = campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false);
            TrainingPlan plan = new TrainingPlan(new TrainingContext(campaign),
                  campaign.getPlayerForce().getFinances().getBalance());
            AutoTrainPlanner.Plan auto = AutoTrainPlanner.plan(campaign, plan, people, preset);
            if (auto.staged().isEmpty()) {
                continue;
            }
            // Planned against this very plan, so its prices stand and needn't be worked out again before the commit
            String label = getFormattedTextAt(RESOURCE_BUNDLE, "report.monthlyPreset", preset.name());
            if (!plan.stagePriced(label, auto.staged())) {
                campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE, "report.monthlyPresetUnaffordable",
                      spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG, preset.name(),
                      auto.fees().minus(plan.fundsLeft()).toAmountAndSymbolString()));
                continue;
            }
            TrainingService.commit(campaign, plan);
        }
    }

    // region XML

    /**
     * Writes the presets as a {@code <trainingPresets>} element, or nothing if there are none.
     *
     * @param writer the writer
     * @param indent the indent
     */
    public void writeToXML(PrintWriter writer, int indent) {
        if (presets.isEmpty()) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "trainingPresets");
        for (AutoTrainSettings preset : presets) {
            preset.writeToXML(writer, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "trainingPresets");
    }

    /**
     * Reads presets from a {@code <trainingPresets>} element; an unreadable preset is logged and skipped.
     *
     * @param node            the element
     * @param campaignOptions the campaign's options, for the defaults of missing values
     *
     * @return the presets
     */
    public static TrainingPresets generateInstanceFromXML(Node node, CampaignOptions campaignOptions) {
        TrainingPresets trainingPresets = new TrainingPresets();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!child.getNodeName().equalsIgnoreCase("preset")) {
                continue;
            }
            try {
                AutoTrainSettings preset = AutoTrainSettings.generateInstanceFromXML(child, campaignOptions);
                if (!preset.name().isBlank()) {
                    trainingPresets.save(preset);
                }
            } catch (Exception exception) {
                LOGGER.error(exception, "Skipped an unreadable auto-train preset");
            }
        }
        return trainingPresets;
    }

    // endregion XML
}
