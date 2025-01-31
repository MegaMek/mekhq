/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.randomEvents.prisoners;

import megamek.common.Compute;
import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.universe.Faction;
import mekhq.gui.dialog.DefectionOffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static megamek.common.Board.T_SPACE;
import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static megamek.common.MiscType.createBeagleActiveProbe;
import static megamek.common.MiscType.createCLImprovedSensors;
import static megamek.common.MiscType.createISImprovedSensors;
import static mekhq.campaign.parts.enums.PartQuality.QUALITY_D;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.BONDSMAN;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER_DEFECTOR;

public class CapturePrisoners {
    private final Campaign campaign;
    private final Faction searchingFaction;
    private final boolean isClan;

    private final int ATTEMPT_COUNT = 3; // This will need tweaking till we're happy with the result
    private final int DEFECTION_CHANCE = 100;
    private final int MERCENARY_DIVIDER = 3;
    private final int CLAN_DEZGRA_MULTIPLIER = 5;

    // SAR Modifiers (based on CamOps pg 223)
    final int HAS_BATTLEFIELD_CONTROL = 1;
    final int GOING_TO_GROUND = -4;

    final int SAR_CONTAINS_VTOL_OR_WIGE = 1;
    final int SAR_HAS_IMPROVED_SENSORS = 2; // largest only
    final int SAR_HAS_ACTIVE_PROBE = 1; // largest only

    final int NOT_IN_PLANET_ORBIT = -2;
    final int SAR_INCLUDES_DROPSHIP = 2; // largest only

    private TargetRoll sarTargetNumber = new TargetRoll(8, "Base TN"); // Target Number (CamOps pg 223)

    public CapturePrisoners(Campaign campaign, @Nullable Faction searchingFaction, Scenario scenario,
                            @Nullable Integer sarQuality) {
        this.campaign = campaign;
        this.searchingFaction = searchingFaction;

        sarTargetNumber.addModifier(GOING_TO_GROUND, "Potential Prisoner Going to Ground");
        sarTargetNumber.addModifier(HAS_BATTLEFIELD_CONTROL, "Searcher Has Battlefield Control");

        int today = campaign.getLocalDate().getYear();
        isClan = searchingFaction != null && searchingFaction.isClan();

        int techFaction = isClan ? ITechnology.getCodeFromMMAbbr("CLAN") : ITechnology.getCodeFromMMAbbr("IS");
        try {
            // searchingFaction being null is fine because we're just ignoring any exceptions
            techFaction = ITechnology.getCodeFromMMAbbr(searchingFaction.getShortName());
        } catch (Exception ignored) {
            // if we can't get the tech faction, we just use the fallbacks already assigned.
        }

        if (scenario.getBoardType() == T_SPACE) {
            sarTargetNumber.addModifier(NOT_IN_PLANET_ORBIT, "Not in Planet Orbit");
            sarTargetNumber.addModifier(SAR_INCLUDES_DROPSHIP, "SAR Includes DropShip");
        } else {
            sarTargetNumber.addModifier(SAR_CONTAINS_VTOL_OR_WIGE, "SAR Contains VTOL or WIGE");
            sarTargetNumber.addModifier(SAR_INCLUDES_DROPSHIP, "SAR Includes DropShip");


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
    }

    public boolean attemptCaptureOfNPC(boolean wasPickedUp) {
        // Attempt capture
        boolean captureSuccessful = wasPickedUp;

        if (!captureSuccessful) {
            captureSuccessful = rollForCapture();
        }

        return captureSuccessful;
    }

    public void processCaptureOfNPC(Person prisoner) {
        if (isClan) {
            prisoner.setPrisonerStatus(campaign, BONDSMAN, true);
            handlePostCapture(prisoner);
            return;
        }

        // Attempt defection
        int roll = attemptDefection(prisoner, true);

        if (roll == 0) {
            if (prisoner.isClanPersonnel()) {
                prisoner.setPrisonerStatus(campaign, BONDSMAN, true);
            } else {
                prisoner.setPrisonerStatus(campaign, PRISONER_DEFECTOR, true);
            }

            new DefectionOffer(campaign, prisoner, prisoner.isClanPersonnel());
        } else {
            prisoner.setPrisonerStatus(campaign, PRISONER, true);
        }

        // If defection has failed, convert to normal prisoner
        handlePostCapture(prisoner);
    }

    private int attemptDefection(Person potentialDefector, boolean isNPC) {
        int adjustedDefectionChance = DEFECTION_CHANCE;
        if (potentialDefector.getOriginFaction().isMercenary()) {
            adjustedDefectionChance /= MERCENARY_DIVIDER;
        }

        if (potentialDefector.isClanPersonnel()) {
            if (isNPC) {
                Faction campaignFaction = campaign.getFaction();
                if (campaignFaction.isPirate() || campaignFaction.isMercenary()) {
                    adjustedDefectionChance *= CLAN_DEZGRA_MULTIPLIER;
                }
            } else {
                if (searchingFaction.isPirate() || searchingFaction.isMercenary()) {
                    adjustedDefectionChance *= CLAN_DEZGRA_MULTIPLIER;
                }
            }
        }

        return randomInt(adjustedDefectionChance);
    }

    public void attemptCaptureOfPlayerCharacter(Person potentialPrisoner, boolean wasPickedUp) {
        // Attempt capture
        boolean captureSuccessful = wasPickedUp;

        if (!captureSuccessful) {
            captureSuccessful = rollForCapture();
        }

        // Early exit is capture was unsuccessful
        if (!captureSuccessful) {
            potentialPrisoner.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MIA);
            return;
        }

        if (isClan) {
            potentialPrisoner.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ENEMY_BONDSMAN);
            return;
        }

        // Attempt defection
        int roll = attemptDefection(potentialPrisoner, false);

        if (roll == 0) {
            potentialPrisoner.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.DEFECTED);
        } else {
            potentialPrisoner.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.POW);
        }
    }

    private boolean rollForCapture() {
        int targetNumber = sarTargetNumber.getValue();
        for (int attempt = 0; attempt < ATTEMPT_COUNT; attempt++) {
            int roll = d6(2);

            if (roll >= targetNumber) {
                return true;
            }
        }
        return false;
    }

    private void handlePostCapture(Person prisoner) {
        // non-clan prisoners should generate with lower than average loyalty, so drop the highest roll
        if (!prisoner.isClanPersonnel()) {
            List<Integer> rolls = new ArrayList<>();

            for (int roll = 0; roll < 4; roll++) {
                rolls.add(Compute.d6(1));
            }

            Collections.sort(rolls);

            prisoner.setLoyalty(rolls.get(0) + rolls.get(1) + rolls.get(2));
        }

        // 'Recruit' prisoner
        PrisonerStatus prisonerStatus = prisoner.getPrisonerStatus();
        campaign.recruitPerson(prisoner, prisonerStatus);

        if (prisonerStatus.isPrisonerDefector()) {
            campaign.addReport(String.format("You have convinced %s to defect.",
                prisoner.getHyperlinkedName()));
        }

        if (prisonerStatus.isBondsman()) {
            campaign.addReport(String.format("You have taken %s as a Bondsman.",
                prisoner.getHyperlinkedName()));
        }
    }
}
