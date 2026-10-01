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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import mekhq.campaign.digitalGM.stratCon.facility.FacilityOperation;
import mekhq.campaign.digitalGM.stratCon.facility.FacilityTrait;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityDefinition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.MonthlySupportPointsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.PreventAerospaceEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.RevealTrackEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScanRangeEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScenarioOddsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.SharedModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityFactory;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests a facility's tier, condition, garrison and intel: how each changes what the facility does, how a fight on the
 * facility wears it down, how placement picks the tier, and how the new state is saved.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityStateTest {

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    /** @return the shipped Mek Base test definition, failing loudly if it is missing */
    private static StratConFacilityDefinition shippedMekBase() {
        StratConFacilityDefinition definition = StratConFacilityFactory.getDefinition("MekBase");
        assertNotNull(definition, "The MekBase test definition is not loaded");
        return definition;
    }

    private static StratConFacility mekBase(ForceAlignment owner) {
        return StratConTestData.facility(owner,
              FacilityType.MekBase,
              new LocalModifiersEffect(List.of("MekGarrison.json")),
              new SharedModifiersEffect(List.of("MekReinforcements.json")));
    }

    @Nested
    class Garrison {
        @Test
        void aNewFacilityHasAFullBaseGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);

            assertEquals(FacilityTier.BASE, facility.getTier());
            assertEquals(2, facility.getGarrisonMaximum());
            assertEquals(2, facility.getGarrison());
        }

        @Test
        void theGarrisonStaysWithinItsTiersLimits() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.setGarrison(9);
            assertEquals(2, facility.getGarrison());

            facility.setGarrison(-3);
            assertEquals(0, facility.getGarrison());
        }

        @Test
        void shrinkingTheTierTrimsTheGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.setTier(FacilityTier.STRONGHOLD);
            facility.setGarrison(3);

            facility.setTier(FacilityTier.OUTPOST);

            assertEquals(1, facility.getGarrison());
        }

        @Test
        void anEmptyGarrisonHasNoDefenders() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.setGarrison(0);

            assertTrue(facility.getLocalModifiers().isEmpty());
        }

        @Test
        void eachHostileGarrisonStepClimbsTheLadder() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.setTier(FacilityTier.STRONGHOLD);

            facility.setGarrison(1);
            assertEquals(List.of("MekGarrison.json"), facility.getLocalModifiers());

            facility.setGarrison(2);
            assertEquals(List.of("MekGarrison.json", "EnemyTurrets.json"), facility.getLocalModifiers());

            facility.setGarrison(3);
            assertEquals(List.of("MekGarrison.json", "EnemyTurrets.json", "HostileBVBudgetIncrease.json"),
                  facility.getLocalModifiers());
        }

        @Test
        void anAlliedGarrisonUsesTheAlliedLadder() {
            StratConFacility facility = mekBase(ForceAlignment.Allied);
            facility.setTier(FacilityTier.STRONGHOLD);
            facility.setGarrison(3);

            assertEquals(List.of("MekGarrison.json", "AlliedTurrets.json", "AlliedGroundSupport.json"),
                  facility.getLocalModifiers());
        }

        @Test
        void aLadderModifierTheProfileAlreadyHasIsNotAddedTwice() {
            StratConFacility facility = StratConTestData.facility(ForceAlignment.Opposing,
                  FacilityType.BaseOfOperations,
                  new LocalModifiersEffect(List.of("Veterans.json", "EnemyTurrets.json")));
            facility.setTier(FacilityTier.STRONGHOLD);
            facility.setGarrison(3);

            assertEquals(List.of("Veterans.json", "EnemyTurrets.json", "HostileBVBudgetIncrease.json"),
                  facility.getLocalModifiers());
        }

        @Test
        void modifiersAddedAtPlacementApplyWhateverTheGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.addAdditionalLocalModifiers(List.of("FacilityHostileDestroy.json"));
            facility.setGarrison(0);

            assertEquals(List.of("FacilityHostileDestroy.json"), facility.getLocalModifiers());
        }
    }

    @Nested
    class Condition {
        private StratConFacility dataCenter() {
            return StratConTestData.facility(ForceAlignment.Allied,
                  FacilityType.DataCenter,
                  new ScanRangeEffect(3),
                  new ScenarioOddsEffect(5),
                  new MonthlySupportPointsEffect(2),
                  new SharedModifiersEffect(List.of("Patrol.json")),
                  new RevealTrackEffect(),
                  new PreventAerospaceEffect());
        }

        @Test
        void anIntactFacilityHasItsFullEffects() {
            StratConFacility facility = dataCenter();

            assertEquals(3, facility.getScanRangeIncrease());
            assertEquals(5, facility.getScenarioOddsModifier());
            assertEquals(2, facility.getMonthlySupportPoints());
            assertEquals(List.of("Patrol.json"), facility.getSharedModifiers());
        }

        @Test
        void aDamagedFacilityHalvesItsNumbersAndLendsNothing() {
            StratConFacility facility = dataCenter();
            facility.setCondition(FacilityCondition.DAMAGED);

            assertEquals(1, facility.getScanRangeIncrease(), "halved, rounding down");
            assertEquals(2, facility.getScenarioOddsModifier());
            assertEquals(1, facility.getMonthlySupportPoints());
            assertTrue(facility.getSharedModifiers().isEmpty());
            assertTrue(facility.isRevealingTrack());
            assertTrue(facility.isPreventingAerospace());
        }

        @Test
        void aCrippledFacilityHasNoEffects() {
            StratConFacility facility = dataCenter();
            facility.setCondition(FacilityCondition.CRIPPLED);

            assertEquals(0, facility.getScanRangeIncrease());
            assertEquals(0, facility.getScenarioOddsModifier());
            assertEquals(0, facility.getMonthlySupportPoints());
            assertTrue(facility.getSharedModifiers().isEmpty());
            assertFalse(facility.isRevealingTrack());
            assertFalse(facility.isPreventingAerospace());
        }

        @Test
        void aCrippledFacilityStillHasItsGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.setCondition(FacilityCondition.CRIPPLED);

            assertEquals(List.of("MekGarrison.json", "EnemyTurrets.json"), facility.getLocalModifiers());
        }

        @Test
        void conditionNeverWorsensPastCrippled() {
            assertEquals(FacilityCondition.DAMAGED, FacilityCondition.INTACT.worsened());
            assertEquals(FacilityCondition.CRIPPLED, FacilityCondition.DAMAGED.worsened());
            assertEquals(FacilityCondition.CRIPPLED, FacilityCondition.CRIPPLED.worsened());
        }
    }

    @Nested
    class Intel {
        @Test
        void theEnemysFacilitiesStartUnknown() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);

            assertEquals(FacilityIntel.UNKNOWN, facility.getIntel());
            assertFalse(facility.isVisible());
        }

        @Test
        void thePlayerKnowsEverythingAboutTheirOwnSidesFacilities() {
            assertEquals(FacilityIntel.DETAILED, mekBase(ForceAlignment.Allied).getIntel());
            assertEquals(FacilityIntel.DETAILED, mekBase(ForceAlignment.Player).getIntel());
        }

        @Test
        void raisingIntelNeverLowersIt() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.raiseIntel(FacilityIntel.DETAILED);
            facility.raiseIntel(FacilityIntel.LOCATED);

            assertEquals(FacilityIntel.DETAILED, facility.getIntel());
        }

        @Test
        void seeingAFacilityScoutsItAndHidingItForgetsIt() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.setVisible(true);
            assertEquals(FacilityIntel.SCOUTED, facility.getIntel());
            assertTrue(facility.isVisible());

            facility.setVisible(false);
            assertEquals(FacilityIntel.UNKNOWN, facility.getIntel());
        }

        @Test
        void aLocatedFacilityIsVisible() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.setIntel(FacilityIntel.LOCATED);

            assertTrue(facility.isVisible());
        }
    }

    @Nested
    class Aftermath {
        @Test
        void winningAtAHostileFacilityDamagesItAndCostsItsGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);

            StratConRulesManager.processFacilityAftermath(facility, true, false);

            assertEquals(FacilityCondition.DAMAGED, facility.getCondition());
            assertEquals(1, facility.getGarrison());
            assertEquals(ForceAlignment.Opposing, facility.getOwner());
        }

        @Test
        void losingAtAHostileFacilityStillCostsItsGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);

            StratConRulesManager.processFacilityAftermath(facility, false, false);

            assertEquals(FacilityCondition.INTACT, facility.getCondition());
            assertEquals(1, facility.getGarrison());
        }

        @Test
        void losingAtAnAlliedFacilityDamagesIt() {
            StratConFacility facility = mekBase(ForceAlignment.Allied);

            StratConRulesManager.processFacilityAftermath(facility, false, false);

            assertEquals(FacilityCondition.DAMAGED, facility.getCondition());
            assertEquals(1, facility.getGarrison());
        }

        @Test
        void aDrawAtAnAlliedFacilityOnlyCostsItsGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Allied);

            StratConRulesManager.processFacilityAftermath(facility, false, true);

            assertEquals(FacilityCondition.INTACT, facility.getCondition());
            assertEquals(1, facility.getGarrison());
        }

        @Test
        void holdingAnAlliedFacilityOnlyCostsItsGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Allied);

            StratConRulesManager.processFacilityAftermath(facility, true, false);

            assertEquals(FacilityCondition.INTACT, facility.getCondition());
            assertEquals(1, facility.getGarrison());
        }

        @Test
        void aFacilityCanBeWornDownOverTwoFights() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);

            StratConRulesManager.processFacilityAftermath(facility, true, false);
            StratConRulesManager.processFacilityAftermath(facility, true, false);

            assertEquals(FacilityCondition.CRIPPLED, facility.getCondition());
            assertEquals(0, facility.getGarrison());
            assertTrue(facility.getLocalModifiers().isEmpty(), "nothing left to defend it");
            assertTrue(facility.getSharedModifiers().isEmpty());
        }

        @Test
        void anEarnedCaptureChangesHandsInsteadOfDamaging() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.incrementOwnershipChangeScore();

            StratConRulesManager.processFacilityAftermath(facility, true, false);

            assertEquals(ForceAlignment.Allied, facility.getOwner());
            assertEquals(FacilityCondition.INTACT, facility.getCondition());
            assertEquals(2, facility.getGarrison());
        }

        @Test
        void aCaptureDoesNotCarryOverToTheNextFight() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);
            facility.incrementOwnershipChangeScore();
            StratConRulesManager.processFacilityAftermath(facility, true, false);

            StratConRulesManager.processFacilityAftermath(facility, true, false);

            assertEquals(0, facility.getOwnershipChangeScore());
            assertEquals(ForceAlignment.Allied, facility.getOwner(), "the second fight must not flip it back");
        }

        @Test
        void fightingOverAFacilityRevealsItsGarrison() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);

            StratConRulesManager.processFacilityAftermath(facility, false, false);

            assertEquals(FacilityIntel.DETAILED, facility.getIntel());
        }

        @Test
        void escapingAFailedReconOrSabotageLeavesTheFacilityAlone() {
            for (FacilityOperation operation : List.of(FacilityOperation.RECON, FacilityOperation.SABOTAGE)) {
                StratConFacility facility = mekBase(ForceAlignment.Opposing);

                StratConRulesManager.processFacilityAftermath(facility, true, false, operation);

                assertEquals(FacilityCondition.INTACT, facility.getCondition(), operation.name());
                assertEquals(2, facility.getGarrison(), operation.name());
                assertEquals(FacilityIntel.DETAILED, facility.getIntel(), operation.name());
            }
        }

        @Test
        void aWonRaidDamagesTheFacility() {
            StratConFacility facility = mekBase(ForceAlignment.Opposing);

            StratConRulesManager.processFacilityAftermath(facility, true, false, FacilityOperation.RAID);

            assertEquals(FacilityCondition.DAMAGED, facility.getCondition());
            assertEquals(1, facility.getGarrison());
        }
    }

    @Nested
    class Tier {
        @Test
        void scaleSetsTheTierWhenSupportPointsAreInScale() {
            assertEquals(FacilityTier.OUTPOST, StratConContractInitializer.getFacilityTier(2, true, false));
            assertEquals(FacilityTier.BASE, StratConContractInitializer.getFacilityTier(3, true, false));
            assertEquals(FacilityTier.BASE, StratConContractInitializer.getFacilityTier(5, true, false));
            assertEquals(FacilityTier.STRONGHOLD, StratConContractInitializer.getFacilityTier(6, true, false));
        }

        @Test
        void scaleIsDividedByThreeWhenSupportPointsAreNotInScale() {
            assertEquals(FacilityTier.OUTPOST, StratConContractInitializer.getFacilityTier(8, false, false));
            assertEquals(FacilityTier.BASE, StratConContractInitializer.getFacilityTier(9, false, false));
            assertEquals(FacilityTier.STRONGHOLD, StratConContractInitializer.getFacilityTier(18, false, false));
        }

        @Test
        void anObjectiveFacilityIsOneTierLargerUpToAStronghold() {
            assertEquals(FacilityTier.BASE, StratConContractInitializer.getFacilityTier(1, true, true));
            assertEquals(FacilityTier.STRONGHOLD, StratConContractInitializer.getFacilityTier(4, true, true));
            assertEquals(FacilityTier.STRONGHOLD, StratConContractInitializer.getFacilityTier(9, true, true));
        }

        @Test
        void placedFacilitiesTakeTheTierWithAFullGarrison() {
            StratConTrackState track = new StratConTrackState();
            track.setWidth(5);
            track.setHeight(5);
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    track.setTerrainTile(new StratConCoords(x, y), "Grasslands");
                }
            }

            StratConContractInitializer.initializeTrackFacilities(track,
                  2,
                  ForceAlignment.Opposing,
                  null,
                  Collections.emptyList(),
                  FacilityTier.STRONGHOLD);

            assertEquals(2, track.getFacilities().size());
            for (StratConFacility facility : track.getFacilities().values()) {
                assertEquals(FacilityTier.STRONGHOLD, facility.getTier());
                // A placed facility may roll Undermanned, which holds one step fewer.
                int expectedMaximum = facility.hasTrait(FacilityTrait.UNDERMANNED) ? 2 : 3;
                assertEquals(expectedMaximum, facility.getGarrisonMaximum());
                assertEquals(expectedMaximum, facility.getGarrison());
            }
        }
    }

    @Nested
    class Saving {
        private StratConFacility unmarshal(String xml) throws JAXBException {
            return JAXBContext.newInstance(StratConFacility.class)
                         .createUnmarshaller()
                         .unmarshal(new StreamSource(new StringReader(xml)), StratConFacility.class)
                         .getValue();
        }

        private String marshal(StratConFacility facility) throws JAXBException {
            Marshaller marshaller = JAXBContext.newInstance(StratConFacility.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter writer = new StringWriter();
            marshaller.marshal(new JAXBElement<>(new QName("facility"), StratConFacility.class, facility), writer);
            return writer.toString();
        }

        @Test
        void theNewStateRoundTripsThroughTheSave() throws JAXBException {
            StratConFacility original = new StratConFacility(shippedMekBase(), ForceAlignment.Opposing);
            original.setTier(FacilityTier.STRONGHOLD);
            original.setGarrison(1);
            original.setCondition(FacilityCondition.DAMAGED);
            original.setIntel(FacilityIntel.LOCATED);

            StratConFacility reloaded = unmarshal(marshal(original));
            reloaded.resolveLegacyData();

            assertEquals(FacilityTier.STRONGHOLD, reloaded.getTier());
            assertEquals(1, reloaded.getGarrison());
            assertEquals(FacilityCondition.DAMAGED, reloaded.getCondition());
            assertEquals(FacilityIntel.LOCATED, reloaded.getIntel());
        }

        @Test
        void anOlderSaveGetsAFullIntactBaseAndItsVisibilityAsIntel() throws JAXBException {
            StratConFacility seen = unmarshal("""
                  <facility>
                      <definitionId>MekBase</definitionId>
                      <owner>Opposing</owner>
                      <visible>true</visible>
                  </facility>
                  """);
            seen.resolveLegacyData();

            assertEquals(FacilityTier.BASE, seen.getTier());
            assertEquals(FacilityCondition.INTACT, seen.getCondition());
            assertEquals(2, seen.getGarrison());
            assertEquals(FacilityIntel.SCOUTED, seen.getIntel());
            assertFalse(marshal(seen).contains("<visible>"), "the old flag should not be written back");

            StratConFacility unseen = unmarshal("""
                  <facility>
                      <definitionId>MekBase</definitionId>
                      <owner>Opposing</owner>
                      <visible>false</visible>
                  </facility>
                  """);
            unseen.resolveLegacyData();

            assertEquals(FacilityIntel.UNKNOWN, unseen.getIntel());
        }
    }
}
