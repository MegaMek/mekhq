/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

/**
 * This class is responsible to control the logging of Service Log Entries.
 *
 * @author Miguel Azevedo
 */
public class PerformanceLogger {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.LogEntries",
          MekHQ.getMHQOptions().getLocale());

    /**
     * @deprecated use {@link #improvedSkill(boolean, Person, LocalDate, String, int)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static void improvedSkill(final Campaign campaign, final Person person, final LocalDate date,
          final String skill, final String value) {
        if (campaign.getCampaignOptions().isPersonnelLogSkillGain()) {
            person.addPerformanceLogEntry(new PerformanceLogEntry(date,
                  MessageFormat.format(resources.getString("improvedSkill.text"), skill, value)));
        }
    }

    /**
     * Logs a skill improvement event for a specified person if skill gain logging  is enabled. This method records the
     * event details including the skill improved, its value, and the date of the improvement.
     *
     * @param isLogSkillGain a boolean indicating whether skill gain logging is enabled
     * @param person         the {@link Person} object representing the individual gaining the skill
     * @param date           the {@link LocalDate} of the skill improvement
     * @param skill          a {@link String} representing the name of the skill that was improved
     * @param value          an {@code int} representing the value of the improvement to the skill
     */
    public static void improvedSkill(final boolean isLogSkillGain, final Person person, final LocalDate date,
          final String skill, final int value) {
        if (isLogSkillGain) {
            person.addPerformanceLogEntry(new PerformanceLogEntry(date,
                  MessageFormat.format(resources.getString("improvedSkill.text"), skill, value)));
        }
    }

    public static void gainedSPA(final Campaign campaign, final Person person, final LocalDate date, final String spa) {
        if (campaign.getCampaignOptions().isPersonnelLogAbilityGain()) {
            person.addPerformanceLogEntry(new PerformanceLogEntry(date,
                  MessageFormat.format(resources.getString("gained.text"), spa)));
        }
    }

    public static void gainedEdge(final Campaign campaign, final Person person, final LocalDate date) {
        if (campaign.getCampaignOptions().isPersonnelLogEdgeGain()) {
            person.addPerformanceLogEntry(new PerformanceLogEntry(date,
                  MessageFormat.format(resources.getString("gainedEdge.text"), person.getEdge())));
        }
    }

    public static void changedEdge(final Campaign campaign, final Person person, final LocalDate date) {
        if (campaign.getCampaignOptions().isPersonnelLogEdgeGain()) {
            person.addPerformanceLogEntry(new PerformanceLogEntry(date,
                  MessageFormat.format(resources.getString("changedEdge.text"), person.getEdge())));
        }
    }
}
