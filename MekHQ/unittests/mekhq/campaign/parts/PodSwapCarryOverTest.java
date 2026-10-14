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

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import megamek.common.equipment.WeaponType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.location.LocationNewDayUtil;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.service.mrms.MRMSConfiguredOptions;
import mekhq.service.mrms.MRMSOption;
import mekhq.service.mrms.MRMSService;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A pod swap that runs out of time carries over to the next day like any other job: it keeps its tech and the time
 * already spent, holds its spare, is saved, and is finished by the new-day pass. Mass Repair's repair-in-place choice
 * stays with the jobs it started (issue #10319).
 */
class PodSwapCarryOverTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int POD_SWAP_MINUTES = 30;
    private static final int MINUTES_LEFT_TODAY = 10;
    private static final int SUCCESSFUL_FACE = 6;

    private PartsScenario scenario;
    private Campaign campaign;
    private Person mechanic;
    private Unit epona;
    private int podLocation;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        mechanic = scenario.withTech(EXP_REGULAR);
        mechanic.setPrimaryRoleDirect(PersonnelRole.MECHANIC);
        epona = scenario.withUnit(UnitFixture.EPONA_PURSUIT_TANK_PRIME);

        EquipmentPart podWeapon = findPodWeapon();
        podLocation = podWeapon.getLocation();
        scenario.withSpare(podWeapon.clone(), 1);
        podWeapon.remove(false);
        epona.refreshPodSpace();
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private EquipmentPart findPodWeapon() {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(epona, EquipmentPart.class)) {
            if ((equipmentPart.getType() instanceof WeaponType) && equipmentPart.isOmniPodded()) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("The Epona Prime carries pod-mounted weapons");
    }

    private PodSpace podSpace() {
        for (PodSpace podSpace : epona.getPodSpace()) {
            if (podSpace.getLocation() == podLocation) {
                return podSpace;
            }
        }
        throw new IllegalStateException("No pod space at location " + podLocation);
    }

    private List<MissingEquipmentPart> missingPodWeapons() {
        return PartsScenario.unitParts(epona, MissingEquipmentPart.class)
                     .stream()
                     .filter(missingPart -> missingPart.getLocation() == podLocation)
                     .toList();
    }

    /** Starts the swap with too little of the day left, so it carries over. */
    private PodSpace podSwapCarriedOver() {
        mechanic.setMinutesLeft(MINUTES_LEFT_TODAY);
        mechanic.setOvertimeLeft(0);
        PodSpace podSpace = podSpace();
        campaign.fixPart(podSpace, mechanic);
        return podSpace;
    }

    @Test
    void aPodSwapThatRunsOutOfTimeKeepsItsTechAndTime() {
        PodSpace podSpace = podSwapCarriedOver();

        assertSame(mechanic, podSpace.getTech());
        assertEquals(MINUTES_LEFT_TODAY, podSpace.getTimeSpent());
        assertEquals(POD_SWAP_MINUTES - MINUTES_LEFT_TODAY, podSpace.getTimeLeft());
    }

    @Test
    void aPodSwapThatRunsOutOfTimeHoldsItsSpare() {
        podSwapCarriedOver();

        MissingEquipmentPart missingWeapon = missingPodWeapons().getFirst();
        assertTrue(missingWeapon.hasReplacementPart(), "The spare weapon is held for the swap");
        assertTrue(missingWeapon.getReplacementPart().isReservedByPerson(mechanic.getId()));
    }

    @Test
    void theNewDayFinishesACarriedOverPodSwap() {
        PodSpace podSpace = podSwapCarriedOver();
        mechanic.resetMinutesLeft(false);
        campaign.getPlayerForce().getHumanResources().resetAsTechMinutes(campaign.getCampaignOptions());

        FixedDieRolls.everyDieShows(SUCCESSFUL_FACE);
        LocationNewDayUtil.processAllLocationUnits(campaign);

        assertTrue(missingPodWeapons().isEmpty(), "The spare weapon was fitted");
        assertNull(podSpace.getTech());
        assertEquals(0, podSpace.getTimeSpent());
    }

    @Test
    void rebuildingTheUnitsPartsKeepsThePodSwapInProgress() {
        podSwapCarriedOver();

        epona.initializeParts(true);

        PodSpace rebuilt = podSpace();
        assertSame(mechanic, rebuilt.getTech());
        assertEquals(MINUTES_LEFT_TODAY, rebuilt.getTimeSpent());
    }

    @Test
    void aSavedPodSwapIsReadBackWithItsTechTimeAndChoice() throws Exception {
        PodSpace podSpace = podSwapCarriedOver();
        podSpace.setRepairInPlace(true);
        StringWriter written = new StringWriter();
        podSpace.writeToXML(new PrintWriter(written), 0);

        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                  .parse(new ByteArrayInputStream(written.toString()
                                                                        .getBytes(StandardCharsets.UTF_8)));
        PodSpace savedJob = PodSpace.generateInstanceFromXML(document.getDocumentElement(), epona);
        assertNotNull(savedJob);
        savedJob.fixReferences(campaign);
        PodSpace loaded = new PodSpace(podLocation, epona);
        loaded.continueWorkFrom(savedJob);

        assertEquals(podLocation, savedJob.getLocation());
        assertSame(mechanic, loaded.getTech());
        assertEquals(MINUTES_LEFT_TODAY, loaded.getTimeSpent());
        assertTrue(loaded.isRepairInPlace());
    }

    @Test
    void aPodSpaceWithNoJobIsNotSaved() {
        assertFalse(podSpace().hasWorkInProgress());
    }

    @Test
    void aTechWhoLeavesIsTakenOffThePodSwapAndTheSpareIsFreed() {
        PodSpace podSpace = podSwapCarriedOver();
        MissingEquipmentPart missingWeapon = missingPodWeapons().getFirst();
        Part heldSpare = missingWeapon.getReplacementPart();

        mechanic.removeAllTechJobs(campaign);

        assertNull(podSpace.getTech());
        assertFalse(missingWeapon.hasReplacementPart());
        assertFalse(heldSpare.isReservedForReplacement());
    }

    @Test
    void removingTheUnitsPartsEndsThePodSwapAndFreesTheSpare() {
        podSwapCarriedOver();
        Part heldSpare = missingPodWeapons().getFirst().getReplacementPart();

        epona.removeParts();
        epona.initializeParts(true);

        assertNull(podSpace().getTech(), "A restored unit starts with no pod swap under way");
        assertFalse(heldSpare.isReservedForReplacement());
    }

    @Test
    void endingAPodJobForgetsItsRepairInPlaceChoice() {
        PodSpace podSpace = podSpace();
        podSpace.setRepairInPlace(true);

        podSpace.cancelAssignment(false);
        assertTrue(podSpace.isRepairInPlace(), "A job put on hold keeps its choice");

        podSpace.cancelAssignment(true);
        assertFalse(podSpace.isRepairInPlace(), "A finished or abandoned job forgets it");
    }

    /**
     * Mass Repair runs with only the Weapons row on and pod swaps off, so it picks repair-in-place for the pod spaces
     * but works only on a second, damaged pod weapon, which it repairs where it is. That repair proves it got as far as
     * the pod setting.
     */
    private EquipmentPart massRepairOfAnotherDamagedPodWeapon() {
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, true);
        campaign.getCampaignOptions().set(CampaignOption.MRMS_REPLACE_POD, false);
        for (MRMSOption option : campaign.getCampaignOptions().get(CampaignOption.MRMS_OPTIONS)) {
            option.setActive(option.getType() == PartRepairType.WEAPON);
        }
        EquipmentPart damagedWeapon = findPodWeapon();
        damagedWeapon.setHits(1);

        FixedDieRolls.everyDieShows(SUCCESSFUL_FACE);
        MRMSService.mrmsUnits(campaign, List.of(epona), new MRMSConfiguredOptions(campaign));
        return damagedWeapon;
    }

    @Test
    void massRepairPutsBackThePodSettingOfAPodItDidNotStart() {
        EquipmentPart damagedWeapon = massRepairOfAnotherDamagedPodWeapon();

        assertEquals(0, damagedWeapon.getHits(), "Mass Repair worked on the unit");
        assertNull(podSpace().getTech());
        assertFalse(podSpace().isRepairInPlace(), "A pod job started by hand later swaps as usual");
    }

    @Test
    void massRepairLeavesTheChoiceOfAPodJobAlreadyUnderWay() {
        PodSpace podSpace = podSwapCarriedOver();
        // A new day for the mechanic, whose pod swap stays assigned while Mass Repair finds them other work
        mechanic.resetMinutesLeft(false);
        campaign.getPlayerForce().getHumanResources().resetAsTechMinutes(campaign.getCampaignOptions());

        EquipmentPart damagedWeapon = massRepairOfAnotherDamagedPodWeapon();

        assertEquals(0, damagedWeapon.getHits(), "Mass Repair worked on the unit");
        assertSame(mechanic, podSpace.getTech());
        assertFalse(podSpace.isRepairInPlace(), "The swap started by hand stays a swap");
    }
}
