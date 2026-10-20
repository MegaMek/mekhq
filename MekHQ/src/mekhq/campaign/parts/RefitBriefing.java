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
package mekhq.campaign.parts;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.work.WorkTimeEstimate;
import mekhq.campaign.work.WorkTimeFormatter;

/**
 * The briefing the tech gives before a refit starts: what class of refit it is and why, followed by the rules the
 * player should know about, such as a loss of quality or parts the campaign cannot buy.
 *
 * <p>For example, before refitting a Wolverine WVR-6R to the WVR-6M the tech says it is a Class C refit and lists
 * the changes that set that: the jump jets and SRM 6 ammunition moving, heat sinks added to the legs, and so on.</p>
 */
public final class RefitBriefing {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";
    private static final String GUI_RESOURCE_BUNDLE = "mekhq.resources.CampaignGUI";
    /** The most reasons for the class the tech lists before summing up the rest */
    static final int MAXIMUM_REASONS = 15;

    private RefitBriefing() {}

    /**
     * What the tech tells the commander about the refit, as HTML.
     *
     * @param campaign the campaign, for how the commander is addressed
     * @param refit    the planned refit
     *
     * @return the tech's briefing
     */
    public static String describeInCharacter(Campaign campaign, Refit refit) {
        String commander = campaign.getCommanderAddress();
        String unitName = refit.getUnit().getName();
        if (refit.isBeingRefurbished()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "RefitBriefing.refurbish", commander, unitName,
                  refit.getRefitClassName());
        }

        StringBuilder briefing = new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE, "RefitBriefing.refit",
              commander, unitName, refit.getNewEntity().getShortName(), refit.getRefitClassName()));
        List<RefitClassGrader.ReasonGroup> reasonGroups = refit.getRefitClassReasonGroups();
        if (!reasonGroups.isEmpty()) {
            briefing.append(getFormattedTextAt(RESOURCE_BUNDLE, "RefitBriefing.reasons"));
            appendReasonGroups(briefing, reasonGroups);
        }
        briefing.append(describeQuestion(campaign, refit));
        return briefing.toString();
    }

    /**
     * Lists the reasons in point form: each class as a heading, and under the pod work each location with what its
     * pods become. A long list stops after {@link #MAXIMUM_REASONS} reasons with a count of the rest.
     */
    private static void appendReasonGroups(StringBuilder briefing, List<RefitClassGrader.ReasonGroup> reasonGroups) {
        int reasonsListed = 0;
        int reasonsLeftOut = 0;
        briefing.append("<ul>");
        for (RefitClassGrader.ReasonGroup group : reasonGroups) {
            if (reasonsListed >= MAXIMUM_REASONS) {
                reasonsLeftOut += group.items().size();
                continue;
            }
            briefing.append("<li><b>").append(group.className()).append("</b><ul>");
            for (RefitClassGrader.ReasonItem item : group.items()) {
                if (reasonsListed >= MAXIMUM_REASONS) {
                    reasonsLeftOut++;
                    continue;
                }
                if (item.location() == null) {
                    briefing.append("<li>").append(item.text()).append("</li>");
                } else {
                    briefing.append("<li>").append(item.location()).append("<ul><li>").append(item.text())
                          .append("</li></ul></li>");
                }
                reasonsListed++;
            }
            briefing.append("</ul></li>");
        }
        if (reasonsLeftOut > 0) {
            briefing.append("<li>").append(getFormattedTextAt(RESOURCE_BUNDLE, "RefitBriefing.more", reasonsLeftOut))
                  .append("</li>");
        }
        briefing.append("</ul>");
    }

    /**
     * @return the question that closes the briefing, with how long the work will take at the tech's pace
     */
    private static String describeQuestion(Campaign campaign, Refit refit) {
        int workMinutes = refit.getTimeLeft();
        if (workMinutes <= 0) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "RefitBriefing.question");
        }
        int minutesPerDay = WorkTimeEstimate.getMinutesPerDay(campaign, refit.getTech(), false);
        String duration = WorkTimeFormatter.formatDuration(workMinutes, minutesPerDay, campaign.getLocalDate(),
              MekHQ.getMHQOptions().getWorkTimeDisplay());
        return getFormattedTextAt(RESOURCE_BUNDLE, "RefitBriefing.questionWithTime", duration);
    }

    /**
     * What the tech tells the commander when the refit check fails, as HTML. A refit must be done over, so the tech
     * says how much longer it will take; a refurbishment is lost.
     *
     * @param campaign the campaign, for how the commander is addressed and the date
     * @param refit    the refit whose check failed
     *
     * @return the tech's report
     */
    public static String describeFailedCheck(Campaign campaign, Refit refit) {
        String commander = campaign.getCommanderAddress();
        String unitName = refit.getUnit().getName();
        if (refit.isBeingRefurbished()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "RefitBriefing.failed.refurbish", commander, unitName);
        }
        int minutesPerDay = WorkTimeEstimate.getMinutesPerDay(campaign, refit.getTech(), false);
        String timeLeft = WorkTimeFormatter.formatDuration(refit.getTimeLeft(), minutesPerDay,
              campaign.getLocalDate(), MekHQ.getMHQOptions().getWorkTimeDisplay());
        return getFormattedTextAt(RESOURCE_BUNDLE, "RefitBriefing.failed.refit", commander, unitName, timeLeft);
    }

    /**
     * @param refit the refit whose check failed
     *
     * @return the rule behind a failed check, for the player
     */
    public static String describeFailedCheckRules(Refit refit) {
        String key = refit.isBeingRefurbished() ? "RefitBriefing.failed.refurbish.rules"
                           : "RefitBriefing.failed.refit.rules";
        return getFormattedTextAt(RESOURCE_BUNDLE, key);
    }

    /**
     * The rules the player should know before the refit starts: a quality loss for work without a refit kit, and parts
     * that are not in stock and that the campaign's purchasing options do not allow buying.
     *
     * @param campaign the campaign
     * @param refit    the planned refit
     *
     * @return the notes, or {@code null} if there are none
     */
    public static @Nullable String describeOutOfCharacter(Campaign campaign, Refit refit) {
        List<String> notes = new ArrayList<>();
        if (refit.isLosingQualityForNoKit()) {
            notes.add(getFormattedTextAt(RESOURCE_BUNDLE, "Refit.qualityWarning.noKit"));
        }
        List<String> partsThatCannotBeBought = RefitPurchaseCheck.findPartsThatCannotBeBought(campaign, refit);
        if (!partsThatCannotBeBought.isEmpty()) {
            notes.add(getFormattedTextAt(GUI_RESOURCE_BUNDLE, "refitConfirm.cannotBuy.text",
                  String.join(", ", partsThatCannotBeBought)));
        }
        return notes.isEmpty() ? null : String.join("<br><br>", notes);
    }
}
