/*
 * Copyright (C) 2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.randomEvents.prisoners;

import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;

import static megamek.common.Compute.d6;
import static megamek.common.MiscType.createBeagleActiveProbe;
import static megamek.common.MiscType.createCLImprovedSensors;
import static megamek.common.MiscType.createISImprovedSensors;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_D;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ACTIVE;
import static mekhq.campaign.personnel.enums.PersonnelStatus.LEFT;

/**
 * Handles the recovery of missing personnel (MIA) through abstracted search-and-rescue (SAR)
 * operations.
 *
 * <p>This class defines the process of conducting a SAR operation to attempt the rescue of a
 * missing character. The success of the operation is determined by various factors, including the
 * quality of the SAR team, the presence of specialized technology, and a dice roll.</p>
 *
 * <p>The recovery process is based on rules adapted from the Campaign Operations manual.</p>
 */
public class RecoverMIAPersonnel {
    private final Campaign campaign;

    // SAR Modifiers (based on CamOps pg 223)
    final int SAR_CONTAINS_VTOL_OR_WIGE = 1;
    final int SAR_HAS_IMPROVED_SENSORS = 2; // largest only
    final int SAR_HAS_ACTIVE_PROBE = 1; // largest only
    private TargetRoll sarTargetNumber = new TargetRoll(8, "Base TN"); // Target Number (CamOps pg 223)

    /**
     * Constructs a new instance to handle the SAR search for MIA personnel.
     *
     * <p>This constructor sets up the base target number for the operation and adjusts it using
     * modifiers based on the SAR team's quality, equipment availability, and other relevant factors.
     * The availability of certain technologies and their effects on the SAR operation depends on
     * the current year in the game's campaign and the faction's tech level.</p>
     *
     * @param campaign       The current campaign instance containing the operation context.
     * @param searchingFaction The faction conducting the SAR operation. Can be {@code null}, in
     *                        which case a default technology setting is applied.
     * @param sarQuality     The quality of the SAR team's equipment (null defaults to average
     *                      quality).
     */
    public RecoverMIAPersonnel(Campaign campaign, Faction searchingFaction, Integer sarQuality) {
        this.campaign = campaign;

        int today = campaign.getLocalDate().getYear();
        boolean isClan = searchingFaction != null && searchingFaction.isClan();

        int techFaction = isClan ? ITechnology.getCodeFromMMAbbr("CLAN") : ITechnology.getCodeFromMMAbbr("IS");
        try {
            // searchingFaction being null is fine because we're just ignoring any exceptions
            techFaction = ITechnology.getCodeFromMMAbbr(searchingFaction.getShortName());
        } catch (Exception ignored) {
            // if we can't get the tech faction, we just use the fallbacks already assigned.
        }

        sarTargetNumber.addModifier(SAR_CONTAINS_VTOL_OR_WIGE, "SAR Contains VTOL or WIGE");

        final int isImprovedSensorsAvailability = createISImprovedSensors().calcYearAvailability(
            today, isClan, techFaction);
        final int clanImprovedSensorsAvailability = createCLImprovedSensors().calcYearAvailability(
            today, isClan, techFaction);

        final int improvedSensorsAvailability = isClan ? clanImprovedSensorsAvailability : isImprovedSensorsAvailability;

        final int activeProbeAvailability = createBeagleActiveProbe().calcYearAvailability(
            today, isClan, techFaction);

        if (sarQuality == null) {
            sarQuality = QUALITY_D.ordinal();
        }

        if (sarQuality >= improvedSensorsAvailability) {
            sarTargetNumber.addModifier(SAR_HAS_IMPROVED_SENSORS, "SAR has Improved Sensors");
        } else if (sarQuality >= activeProbeAvailability) {
            sarTargetNumber.addModifier(SAR_HAS_ACTIVE_PROBE, "SAR has Active Probe");
        }
    }

    /**
     * Attempts to rescue a player-character who is listed as missing in action (MIA).
     *
     * <p>The success of the SAR operation is determined by a die roll against a target number (TN).
     * The TN is influenced by factors such as the SAR team's available equipment and the
     * technology level of the faction conducting the operation.</p>
     *
     * <p>If the rescue attempt is successful, the missing person's status is updated to indicate
     * they have been found.</p>
     *
     * @param missingPerson The {@link Person} representing the character who is MIA.
     */
    public void attemptRescueOfPlayerCharacter(Person missingPerson) {
        int targetNumber = sarTargetNumber.getValue();
        int roll = d6(2);

        boolean wasRescued = roll >= targetNumber;

        if (wasRescued) {
            missingPerson.changeStatus(campaign, campaign.getLocalDate(), ACTIVE);
        }
    }

    /**
     * Updates the status of all personnel marked as Missing In Action (MIA) in the given campaign
     * to indicate they have left the campaign.
     *
     * @param campaign The campaign instance containing the personnel to be checked and updated.
     */
    public static void abandonMissingPersonnel(Campaign campaign) {
        for (Person person : campaign.getPersonnel()) {
            if (!person.getStatus().isMIA()) {
                continue;
            }

            person.changeStatus(campaign, campaign.getLocalDate(), LEFT);
        }
    }
}
