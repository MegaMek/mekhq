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

    public static void gainedXpFromMedWork(Person doctor, LocalDate date, int taskXP) {
        String message = resources.getString("gainedXpFromMedWork.text");
        doctor.addPerformanceLogEntry(new PerformanceLogEntry(date, MessageFormat.format(message, taskXP)));
    }

    public static void successfullyTreatedWithXp(Person doctor, Person patient, LocalDate date, int injuries, int xp) {
        String message = resources.getString("successfullyTreatedWithXp.text");
        doctor.addPerformanceLogEntry(new PerformanceLogEntry(date,
              MessageFormat.format(message, patient, injuries, xp)));
    }

    public static void improvedSkill(final Campaign campaign, final Person person, final LocalDate date,
          final String skill, final String value) {
        if (campaign.getCampaignOptions().isPersonnelLogSkillGain()) {
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
