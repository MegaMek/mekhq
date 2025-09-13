/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.prisoners;

import static megamek.common.compute.Compute.d6;
import static megamek.common.equipment.MiscType.createBeagleActiveProbe;
import static megamek.common.equipment.MiscType.createCLImprovedSensors;
import static megamek.common.equipment.MiscType.createISImprovedSensors;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_D;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ACTIVE;
import static mekhq.campaign.personnel.enums.PersonnelStatus.LEFT;

import megamek.common.enums.AvailabilityValue;
import megamek.common.interfaces.ITechnology;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;

/**
 * Handles the recovery of missing personnel (MIA) through abstracted search-and-rescue (SAR) operations.
 *
 * <p>This class defines the process of conducting a SAR operation to attempt the rescue of a
 * missing character. The success of the operation is determined by various factors, including the quality of the SAR
 * team, the presence of specialized technology, and a dice roll.</p>
 *
 * <p>The recovery process is based on rules adapted from the Campaign Operations manual.</p>
 */
public class RecoverMIAPersonnel {
    private final Campaign campaign;

    // SAR Modifiers (based on CamOps pg 223)
    final int SAR_CONTAINS_VTOL_OR_WIGE = 1;
    final int SAR_HAS_IMPROVED_SENSORS = 2; // largest only
    final int SAR_HAS_ACTIVE_PROBE = 1; // largest only
    private final TargetRoll sarTargetNumber = new TargetRoll(8, "Base TN"); // Target Number (CamOps pg 223)

    /**
     * Constructs a new instance to handle the SAR search for MIA personnel.
     *
     * <p>This constructor sets up the base target number for the operation and adjusts it using
     * modifiers based on the SAR team's quality, equipment availability, and other relevant factors. The availability
     * of certain technologies and their effects on the SAR operation depends on the current year in the game's campaign
     * and the faction's tech level.</p>
     *
     * @param campaign         The current campaign instance containing the operation context.
     * @param searchingFaction The faction conducting the SAR operation. Can be {@code null}, in which case a default
     *                         technology setting is applied.
     * @param sarQuality       The quality of the SAR team's equipment (null defaults to average quality).
     */
    public RecoverMIAPersonnel(Campaign campaign, Faction searchingFaction, Integer sarQuality) {
        this.campaign = campaign;

        int today = campaign.getLocalDate().getYear();
        boolean isClan = searchingFaction != null && searchingFaction.isClan();

        megamek.common.enums.Faction techFaction = isClan ?
                                                         ITechnology.getFactionFromMMAbbr("CLAN") :
                                                         ITechnology.getFactionFromMMAbbr("IS");
        try {
            // searchingFaction being null is fine because we're just ignoring any exceptions
            if (searchingFaction != null) {
                techFaction = ITechnology.getFactionFromMMAbbr(searchingFaction.getShortName());
            }
        } catch (Exception ignored) {
            // if we can't get the tech faction, we just use the fallbacks already assigned.
        }

        sarTargetNumber.addModifier(SAR_CONTAINS_VTOL_OR_WIGE, "SAR Contains VTOL or WIGE");

        final AvailabilityValue isImprovedSensorsAvailability = createISImprovedSensors().calcYearAvailability(
              today, isClan, techFaction);
        final AvailabilityValue clanImprovedSensorsAvailability = createCLImprovedSensors().calcYearAvailability(
              today, isClan, techFaction);

        final AvailabilityValue improvedSensorsAvailability = isClan ?
                                                                    clanImprovedSensorsAvailability :
                                                                    isImprovedSensorsAvailability;

        final AvailabilityValue activeProbeAvailability = createBeagleActiveProbe().calcYearAvailability(
              today, isClan, techFaction);

        if (sarQuality == null) {
            sarQuality = QUALITY_D.ordinal();
        }

        // TODO: sarQuality is evaluated against the index of a AvailabilityValue. doesn't seems very nice. Refactor the whole constructor.
        if (sarQuality >= improvedSensorsAvailability.getIndex()) {
            sarTargetNumber.addModifier(SAR_HAS_IMPROVED_SENSORS, "SAR has Improved Sensors");
        } else if (sarQuality >= activeProbeAvailability.getIndex()) {
            sarTargetNumber.addModifier(SAR_HAS_ACTIVE_PROBE, "SAR has Active Probe");
        }
    }

    /**
     * Attempts to rescue a player-character who is listed as missing in action (MIA).
     *
     * <p>The success of the SAR operation is determined by a die roll against a target number (TN).
     * The TN is influenced by factors such as the SAR team's available equipment and the technology level of the
     * faction conducting the operation.</p>
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
     * Updates the status of all personnel marked as Missing In Action (MIA) in the given campaign to indicate they have
     * left the campaign.
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
