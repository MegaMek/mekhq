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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.medical.BodyLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers {@link AdvancedMedicalAlternate#getInjuriesRemovedBySurgery(ProstheticType, List, List)}, the rule deciding
 * what a successful surgery takes away.
 */
class AdvancedMedicalAlternateTest {
    private Campaign campaign;
    private Person patient;

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();
        patient = new Person(campaign);
    }

    private Injury fitted(InjuryType injuryType, BodyLocation location) {
        return injuryType.newInjury(campaign, patient, location, 0);
    }

    /** Fitting an implant only ever looks at the patient's whole record, not the injuries at the location. */
    private List<Injury> removedByImplant(ProstheticType type, List<Injury> injuries) {
        return AdvancedMedicalAlternate.getInjuriesRemovedBySurgery(type, injuries, List.of());
    }

    // region implants
    @Test
    @DisplayName("Fitting the power supply leaves an explosive suicide implant in place")
    void powerSupplyLeavesSuicideImplant() {
        Injury suicideImplant = fitted(AlternateInjuries.EXPLOSIVE_SUICIDE_IMPLANT, BodyLocation.INTERNAL);

        assertTrue(removedByImplant(ProstheticType.POWER_SUPPLY, List.of(suicideImplant)).isEmpty());
    }

    @Test
    @DisplayName("Fitting an explosive suicide implant leaves the power supply in place")
    void suicideImplantLeavesPowerSupply() {
        Injury powerSupply = fitted(AlternateInjuries.SECONDARY_POWER_SUPPLY, BodyLocation.INTERNAL);

        assertTrue(removedByImplant(ProstheticType.EXPLOSIVE_SUICIDE_IMPLANT, List.of(powerSupply)).isEmpty());
    }

    @Test
    @DisplayName("Fitting the pheromone effuser leaves the toxin effuser and suicide implant in place")
    void pheromoneEffuserLeavesOtherInternalImplants() {
        List<Injury> injuries = List.of(fitted(AlternateInjuries.TOXIN_EFFUSER, BodyLocation.INTERNAL),
              fitted(AlternateInjuries.EXPLOSIVE_SUICIDE_IMPLANT, BodyLocation.INTERNAL));

        assertTrue(removedByImplant(ProstheticType.PHEROMONE_EFFUSER, injuries).isEmpty());
    }

    @Test
    @DisplayName("Fitting the toxin effuser leaves the pheromone effuser in place")
    void toxinEffuserLeavesPheromoneEffuser() {
        Injury pheromoneEffuser = fitted(AlternateInjuries.PHEROMONE_EFFUSER, BodyLocation.INTERNAL);

        assertTrue(removedByImplant(ProstheticType.TOXIN_EFFUSER, List.of(pheromoneEffuser)).isEmpty());
    }

    @Test
    @DisplayName("Fitting an implant the patient already carries replaces the old one")
    void identicalImplantIsReplaced() {
        Injury oldImplant = fitted(AlternateInjuries.EXPLOSIVE_SUICIDE_IMPLANT, BodyLocation.INTERNAL);

        assertEquals(List.of(oldImplant),
              removedByImplant(ProstheticType.EXPLOSIVE_SUICIDE_IMPLANT, List.of(oldImplant)));
    }

    @Test
    @DisplayName("Fitting powered flight wings replaces glider wings")
    void wingsReplaceEachOther() {
        Injury gliderWings = fitted(AlternateInjuries.GLIDER_WINGS, BodyLocation.SPINE);

        assertEquals(List.of(gliderWings), removedByImplant(ProstheticType.POWERED_FLIGHT_WINGS,
              List.of(gliderWings)));
    }

    @Test
    @DisplayName("Fitting wings leaves extraneous prosthetic limbs on the same spine in place")
    void wingsLeaveExtraneousLimbs() {
        Injury extraLimbs = fitted(AlternateInjuries.EXTRANEOUS_PROSTHETIC_LIMBS, BodyLocation.SPINE);

        assertTrue(removedByImplant(ProstheticType.GLIDER_WINGS, List.of(extraLimbs)).isEmpty());
    }

    @Test
    @DisplayName("Fitting a buffered VDNI replaces a standard VDNI")
    void vdniImplantsReplaceEachOther() {
        Injury vdni = fitted(AlternateInjuries.VEHICULAR_DNI, BodyLocation.BRAIN);

        assertEquals(List.of(vdni), removedByImplant(ProstheticType.BUFFERED_VDNI, List.of(vdni)));
    }

    @Test
    @DisplayName("Fitting an implant leaves wounds alone, even those at the location being treated")
    void implantLeavesWounds() {
        Injury burn = AlternateInjuries.BURNED_CHEST.newInjury(campaign, patient, BodyLocation.CHEST, 1);

        List<Injury> removed = AdvancedMedicalAlternate.getInjuriesRemovedBySurgery(ProstheticType.POWER_SUPPLY,
              List.of(burn), List.of(burn));

        assertTrue(removed.isEmpty());
    }
    // endregion implants

    // region replacements
    @Test
    @DisplayName("A replacement clears everything mapped to the location it treats, and nothing elsewhere")
    void replacementClearsTheTreatedLocation() {
        Injury burnAtLocation = AlternateInjuries.BURN_UPPER_ARM.newInjury(campaign, patient,
              BodyLocation.UPPER_LEFT_ARM, 1);
        Injury oldProstheticAtLocation = fitted(AlternateInjuries.ENHANCED_PROSTHETIC_LIMB, BodyLocation.LEFT_ARM);
        Injury suicideImplant = fitted(AlternateInjuries.EXPLOSIVE_SUICIDE_IMPLANT, BodyLocation.INTERNAL);

        List<Injury> removed = AdvancedMedicalAlternate.getInjuriesRemovedBySurgery(
              ProstheticType.IMPROVED_ENHANCED_PROSTHETIC_LIMB,
              List.of(burnAtLocation, oldProstheticAtLocation, suicideImplant),
              List.of(burnAtLocation, oldProstheticAtLocation));

        assertEquals(List.of(burnAtLocation, oldProstheticAtLocation), removed);
    }

    @Test
    @DisplayName("Cosmetic surgery only clears the burns at the location it treats")
    void cosmeticSurgeryOnlyClearsBurns() {
        Injury burn = AlternateInjuries.BURN_FACE.newInjury(campaign, patient, BodyLocation.FACE, 1);
        Injury fracture = AlternateInjuries.FRACTURED_JAW.newInjury(campaign, patient, BodyLocation.JAW, 1);

        List<Injury> removed = AdvancedMedicalAlternate.getInjuriesRemovedBySurgery(ProstheticType.COSMETIC_SURGERY,
              List.of(burn, fracture), List.of(burn, fracture));

        assertEquals(List.of(burn), removed);
    }

    @Test
    @DisplayName("A replacement tolerates gaps in the location's injury list")
    void replacementSkipsNullEntries() {
        Injury burn = AlternateInjuries.BURN_UPPER_ARM.newInjury(campaign, patient, BodyLocation.UPPER_LEFT_ARM, 1);
        List<Injury> atLocation = new ArrayList<>();
        atLocation.add(null);
        atLocation.add(burn);

        List<Injury> removed = AdvancedMedicalAlternate.getInjuriesRemovedBySurgery(
              ProstheticType.ENHANCED_PROSTHETIC_LIMB, List.of(burn), atLocation);

        assertEquals(List.of(burn), removed);
    }
    // endregion replacements
}
