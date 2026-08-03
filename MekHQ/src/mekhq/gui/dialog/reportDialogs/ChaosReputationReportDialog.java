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
package mekhq.gui.dialog.reportDialogs;

import static java.lang.Math.clamp;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JFrame;
import javax.swing.JTextPane;

import megamek.client.ui.util.UIUtil;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.reputation.chaosReputation.ChaosReputation;

/**
 * Displays a report explaining how the force's Chaos Campaign Reputation was determined, including the average
 * experience calculation and the per-character reputation breakdown. This is the Chaos Campaign counterpart to
 * {@link ReputationReportDialog}, which reports on the Campaign Operations reputation system.
 */
public class ChaosReputationReportDialog extends AbstractReportDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosReputation";

    private final Campaign campaign;

    public ChaosReputationReportDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "ChaosReputationReportDialog", "ChaosReputationReportDialog.title");
        this.campaign = campaign;
        initialize();
    }

    public Campaign getCampaign() {
        return campaign;
    }

    @Override
    protected JTextPane createTxtReport() {
        final JTextPane txtReport = new JTextPane();

        txtReport.setContentType("text/html");

        txtReport.setText(getReportText(getCampaign()));

        txtReport.setName("txtReport");
        txtReport.setEditable(false);
        txtReport.setCaretPosition(0);
        return txtReport;
    }
    //endregion Getters

    /**
     * Builds an HTML report explaining how the force's Chaos Campaign Reputation was determined.
     *
     * <p>The report is split into two sections. The first shows how the force's average experience was calculated,
     * tallying how many combat roles sit at each experience level, the running total, and the resulting mean skill
     * level. The second shows how the Chaos Reputation was determined, tallying how many personnel share each adjusted
     * reputation value, then combining the personnel average with the debt penalty to reach the final total.</p>
     *
     * @param campaign the campaign whose player force is reported on
     *
     * @return an HTML string suitable for display in a {@link javax.swing.JTextPane}
     */
    public static String getReportText(Campaign campaign) {
        int titleFontSize = UIUtil.scaleForGUI(7);
        int subtitleFontSize = UIUtil.scaleForGUI(5);

        PlayerForce playerForce = campaign.getPlayerForce();
        Collection<Person> personnel = playerForce.allPersonnel();
        LocalDate currentDate = campaign.getLocalDate();
        boolean isUseAgeEffects = campaign.getCampaignOptions().get(CampaignOption.USE_AGE_EFFECTS);
        boolean isClanForce = playerForce.isClanForce();

        StringBuilder description = new StringBuilder("<html>");

        // HEADER
        description.append(String.format("<div style='text-align: center;'><font size='%d'><b>%s:</b> %d</font></div>",
              titleFontSize,
              playerForce.getName(),
              playerForce.getChaosCampaignReputation()));

        appendChaosReputationSection(description,
              campaign,
              personnel,
              currentDate,
              isUseAgeEffects,
              isClanForce,
              subtitleFontSize);
        
        appendAverageExperienceSection(description, campaign, personnel, subtitleFontSize);

        description.append("</html>");
        return description.toString();
    }

    private static void appendAverageExperienceSection(StringBuilder description, Campaign campaign,
          Collection<Person> personnel, int subtitleFontSize) {
        description.append(String.format("<b><font size='%d'>%s</font></b><br>",
              subtitleFontSize,
              getTextAt(RESOURCE_BUNDLE, "report.averageExperience")));

        // Tally how many combat roles sit at each experience level, rather than listing every person, so the report
        // stays compact for large campaigns.
        Map<Integer, Integer> experienceCounts = new TreeMap<>(Collections.reverseOrder());
        double roleCount = 0;
        int totalExperienceLevel = 0;
        for (Person person : personnel) {
            PersonnelStatus status = person.getStatus();
            if (status.isDepartedUnit() || !status.isActive() || !person.isEmployed()) {
                continue;
            }

            int primaryExperienceLevel = ChaosReputation.getExperienceLevel(campaign, person, true);
            int secondaryExperienceLevel = ChaosReputation.getExperienceLevel(campaign, person, false);

            if (primaryExperienceLevel != EXP_NONE) {
                roleCount++;
                totalExperienceLevel += primaryExperienceLevel;
                experienceCounts.merge(primaryExperienceLevel, 1, Integer::sum);
            }
            if (secondaryExperienceLevel != EXP_NONE) {
                roleCount++;
                totalExperienceLevel += secondaryExperienceLevel;
                experienceCounts.merge(secondaryExperienceLevel, 1, Integer::sum);
            }
        }

        description.append("<table>");
        description.append(String.format("<tr><th align='left'>%s</th><th>%s</th></tr>",
              getTextAt(RESOURCE_BUNDLE, "report.experienceLevel"),
              getTextAt(RESOURCE_BUNDLE, "report.personnel")));
        for (Map.Entry<Integer, Integer> entry : experienceCounts.entrySet()) {
            description.append(String.format("<tr><td>%s</td> <td align='center'>%d</td></tr>",
                  SkillType.getExperienceLevelName(entry.getKey()),
                  entry.getValue()));
        }
        description.append("</table>");

        int meanExperienceLevel = roleCount == 0 ? EXP_NONE : (int) round(totalExperienceLevel / roleCount);
        meanExperienceLevel = clamp(meanExperienceLevel, EXP_NONE, EXP_LEGENDARY);
        SkillLevel averageSkillLevel = SkillType.skillLevelFromExperienceLevel(meanExperienceLevel);

        description.append("<table>");
        description.append(summaryRow(getTextAt(RESOURCE_BUNDLE, "report.totalExperience"),
              Integer.toString(totalExperienceLevel)));
        description.append(summaryRow(getTextAt(RESOURCE_BUNDLE, "report.rolesCounted"),
              Integer.toString((int) roleCount)));
        description.append(summaryRow(getTextAt(RESOURCE_BUNDLE, "report.averageSkillLevel"),
              averageSkillLevel.toString()));
        description.append("</table><br>");
    }

    private static void appendChaosReputationSection(StringBuilder description, Campaign campaign,
          Collection<Person> personnel, LocalDate currentDate, boolean isUseAgeEffects, boolean isClanForce,
          int subtitleFontSize) {
        description.append(String.format("<b><font size='%d'>%s</font></b><br>",
              subtitleFontSize,
              getTextAt(RESOURCE_BUNDLE, "report.chaosReputation")));

        // Tally how many personnel share each adjusted reputation value, rather than listing every person, so the
        // report stays compact for large campaigns.
        Map<Integer, Integer> reputationCounts = new TreeMap<>(Collections.reverseOrder());
        double personCount = 0;
        int totalReputation = 0;
        for (Person person : personnel) {
            PersonnelStatus status = person.getStatus();
            if (status.isDepartedUnit() || !status.isActive() || !person.isEmployed()) {
                continue;
            }

            personCount++;
            int adjustedReputation = person.getAdjustedReputation(isUseAgeEffects, isClanForce, currentDate);
            totalReputation += adjustedReputation;
            reputationCounts.merge(adjustedReputation, 1, Integer::sum);
        }

        description.append("<table>");
        description.append(String.format("<tr><th align='left'>%s</th><th>%s</th></tr>",
              getTextAt(RESOURCE_BUNDLE, "report.reputationValue"),
              getTextAt(RESOURCE_BUNDLE, "report.personnel")));
        for (Map.Entry<Integer, Integer> entry : reputationCounts.entrySet()) {
            description.append(String.format("<tr><td align='center'>%d</td> <td align='center'>%d</td></tr>",
                  entry.getKey(),
                  entry.getValue()));
        }
        description.append("</table>");

        int averageReputation = personCount == 0 ? 0 : (int) round(totalReputation / personCount);
        int debtModifier = ChaosReputation.getDebtModifier(campaign.getPlayerForce().getFinances().getLoans(),
              currentDate);
        int total = averageReputation + debtModifier;

        description.append("<table>");
        description.append(summaryRow(getTextAt(RESOURCE_BUNDLE, "report.averageReputation"),
              Integer.toString(averageReputation)));
        description.append(summaryRow(getTextAt(RESOURCE_BUNDLE, "report.debtPenalty"),
              Integer.toString(debtModifier)));
        description.append(summaryRow(getTextAt(RESOURCE_BUNDLE, "report.reputationTotal"),
              Integer.toString(total)));
        description.append("</table><br>");
    }

    private static String summaryRow(String label, String value) {
        String indent = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
        return String.format("<tr><td>%s<b>%s:</b></td> <td>%s</td></tr>", indent, label, value);
    }
}
