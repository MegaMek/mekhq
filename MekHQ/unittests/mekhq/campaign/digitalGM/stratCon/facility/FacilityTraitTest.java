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
package mekhq.campaign.digitalGM.stratCon.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.MonthlySupportPointsEffect;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests facility traits: how many a facility gets and which, what each does, which survive a change of hands, and
 * that they are saved.
 *
 * @author Illiani
 * @since 0.51.01
 */
class FacilityTraitTest {
    private static final StratConCoords FACILITY_COORDS = new StratConCoords(3, 3);

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConFacility facility(ForceAlignment owner, FacilityTier tier) {
        StratConFacility facility = StratConTestData.facility(owner,
              FacilityType.MekBase,
              new LocalModifiersEffect(List.of("MekGarrison.json")),
              new MonthlySupportPointsEffect(1));
        facility.setTier(tier);
        facility.setGarrison(tier.getGarrisonMaximum());
        return facility;
    }

    @Nested
    class Rolling {
        @Test
        void theTraitCountFollowsTheRoll() {
            assertEquals(0, FacilityTrait.getTraitCount(2));
            assertEquals(0, FacilityTrait.getTraitCount(6));
            assertEquals(1, FacilityTrait.getTraitCount(7));
            assertEquals(1, FacilityTrait.getTraitCount(9));
            assertEquals(2, FacilityTrait.getTraitCount(10));
            assertEquals(2, FacilityTrait.getTraitCount(12));
        }

        @Test
        void noTraitIsPickedTwice() {
            // Always picking the first candidate would pick the same trait twice if repeats were allowed.
            List<FacilityTrait> traits = FacilityTrait.pickTraits(FacilityTier.STRONGHOLD, 2, bound -> 0);

            assertEquals(List.of(FacilityTrait.UNDERMANNED, FacilityTrait.VETERAN_GARRISON), traits);
        }

        @Test
        void veteransAndPoorMoraleNeverComeTogether() {
            // Veterans first; poor morale is then no longer a candidate, so index 1 skips to Hardened.
            List<FacilityTrait> traits = FacilityTrait.pickTraits(FacilityTier.OUTPOST, 2, bound -> 0);
            assertEquals(FacilityTrait.VETERAN_GARRISON, traits.get(0));
            assertFalse(traits.contains(FacilityTrait.POOR_MORALE));

            for (int first = 0; first < FacilityTrait.values().length - 1; first++) {
                for (int second = 0; second < FacilityTrait.values().length - 2; second++) {
                    int[] picks = { first, second };
                    int[] call = { 0 };
                    List<FacilityTrait> pair = FacilityTrait.pickTraits(FacilityTier.STRONGHOLD, 2,
                          bound -> Math.min(picks[call[0]++], bound - 1));
                    assertFalse(pair.contains(FacilityTrait.VETERAN_GARRISON)
                                      && pair.contains(FacilityTrait.POOR_MORALE), pair.toString());
                }
            }
        }

        @Test
        void anOutpostIsNeverUndermanned() {
            for (int index = 0; index < FacilityTrait.values().length; index++) {
                int pick = index;
                List<FacilityTrait> traits = FacilityTrait.pickTraits(FacilityTier.OUTPOST, 1,
                      bound -> Math.min(pick, bound - 1));
                assertFalse(traits.contains(FacilityTrait.UNDERMANNED));
            }
        }

        @Test
        void noTraitsAreRolledWhenTheCountIsZero() {
            assertTrue(FacilityTrait.pickTraits(FacilityTier.BASE, 0, bound -> 0).isEmpty());
        }
    }

    @Nested
    class Effects {
        @Test
        void anUndermannedFacilityHoldsOneStepLess() {
            StratConFacility facility = facility(ForceAlignment.Opposing, FacilityTier.STRONGHOLD);

            facility.addTrait(FacilityTrait.UNDERMANNED);

            assertEquals(2, facility.getGarrisonMaximum());
            assertEquals(2, facility.getGarrison(), "the garrison is trimmed to the new maximum");
        }

        @Test
        void anUndermannedFacilityStillHoldsAtLeastOneStep() {
            StratConFacility facility = facility(ForceAlignment.Opposing, FacilityTier.OUTPOST);

            facility.addTrait(FacilityTrait.UNDERMANNED);

            assertEquals(1, facility.getGarrisonMaximum());
        }

        @Test
        void poorMoraleSurrendersWithOneStepLeft() {
            StratConFacility facility = facility(ForceAlignment.Opposing, FacilityTier.BASE);
            facility.setGarrison(1);
            assertFalse(facility.isReadyToSurrender());

            facility.addTrait(FacilityTrait.POOR_MORALE);
            assertTrue(facility.isReadyToSurrender());

            facility.setGarrison(2);
            assertFalse(facility.isReadyToSurrender());
        }

        @Test
        void aHardenedFacilityKeepsItsConditionWhenAttackedButNotTaken() {
            StratConFacility facility = facility(ForceAlignment.Opposing, FacilityTier.BASE);
            facility.addTrait(FacilityTrait.HARDENED);

            facility.applyAttackerVictory();

            assertEquals(FacilityCondition.INTACT, facility.getCondition());
            assertEquals(1, facility.getGarrison(), "it still loses garrison");
        }

        @Test
        void anEnemyVeteranOrExperimentalFacilityAddsItsModifiers() {
            StratConFacility facility = facility(ForceAlignment.Opposing, FacilityTier.OUTPOST);
            facility.addTrait(FacilityTrait.VETERAN_GARRISON);
            facility.addTrait(FacilityTrait.EXPERIMENTAL_WEAPONS);

            List<String> modifiers = facility.getLocalModifiers();
            assertTrue(modifiers.contains(StratConFacility.VETERAN_GARRISON_MODIFIER));
            assertTrue(modifiers.contains(StratConFacility.EXPERIMENTAL_WEAPONS_MODIFIER));
        }

        @Test
        void theSameTraitsAddNoModifiersOnYourSide() {
            // Those modifiers only work on the opposing side, so they would help the enemy attacking you.
            StratConFacility facility = facility(ForceAlignment.Allied, FacilityTier.OUTPOST);
            facility.addTrait(FacilityTrait.VETERAN_GARRISON);
            facility.addTrait(FacilityTrait.EXPERIMENTAL_WEAPONS);

            List<String> modifiers = facility.getLocalModifiers();
            assertFalse(modifiers.contains(StratConFacility.VETERAN_GARRISON_MODIFIER));
            assertFalse(modifiers.contains(StratConFacility.EXPERIMENTAL_WEAPONS_MODIFIER));
        }

        @Test
        void aWellStockedFacilityOnYourSidePaysExtraEachMonth() {
            StratConFacility allied = facility(ForceAlignment.Allied, FacilityTier.BASE);
            allied.addTrait(FacilityTrait.WELL_STOCKED);
            assertEquals(1 + FacilityTrait.WELL_STOCKED_MONTHLY_SUPPORT, allied.getMonthlySupportPoints());

            StratConFacility hostile = facility(ForceAlignment.Opposing, FacilityTier.BASE);
            hostile.addTrait(FacilityTrait.WELL_STOCKED);
            assertEquals(1, hostile.getMonthlySupportPoints());
        }

        @Test
        void aCrippledWellStockedFacilityPaysNothing() {
            StratConFacility allied = facility(ForceAlignment.Allied, FacilityTier.BASE);
            allied.addTrait(FacilityTrait.WELL_STOCKED);
            allied.setCondition(FacilityCondition.CRIPPLED);

            assertEquals(0, allied.getMonthlySupportPoints());
        }

        @Test
        void theGarrisonChangesTheOffScreenDefence() {
            StratConFacility facility = facility(ForceAlignment.Allied, FacilityTier.BASE);
            assertEquals(0, StratConEnemyFacilityActivity.getTraitModifier(facility));

            facility.addTrait(FacilityTrait.VETERAN_GARRISON);
            assertEquals(FacilityTrait.OFF_SCREEN_DEFENSE_MODIFIER,
                  StratConEnemyFacilityActivity.getTraitModifier(facility));

            facility.addTrait(FacilityTrait.POOR_MORALE);
            assertEquals(-FacilityTrait.OFF_SCREEN_DEFENSE_MODIFIER,
                  StratConEnemyFacilityActivity.getTraitModifier(facility));
        }

        @Test
        void aConflictingTraitReplacesTheOldOne() {
            StratConFacility facility = facility(ForceAlignment.Opposing, FacilityTier.BASE);
            facility.addTrait(FacilityTrait.VETERAN_GARRISON);

            facility.addTrait(FacilityTrait.POOR_MORALE);

            assertEquals(List.of(FacilityTrait.POOR_MORALE), facility.getTraits());
        }
    }

    @Nested
    class Orders {
        private final Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        private final StratConCampaignState campaignState = new StratConCampaignState();
        private final StratConTrackState track = new StratConTrackState();
        private final AbstractContract contract = mock(AbstractContract.class);

        private StratConFacility place(ForceAlignment owner, FacilityTrait trait) {
            track.setWidth(8);
            track.setHeight(8);
            campaignState.addTrack(track);
            campaignState.setSupportPoints(0);
            when(contract.getStratConCampaignState()).thenReturn(campaignState);
            when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
            StratConFacility facility = facility(owner, FacilityTier.BASE);
            facility.addTrait(trait);
            track.addFacility(FACILITY_COORDS, facility);
            return facility;
        }

        @Test
        void sabotageOnlyDropsAHardenedFacilityOneStep() {
            StratConFacility facility = place(ForceAlignment.Opposing, FacilityTrait.HARDENED);

            StratConFacilityOperations.resolveSabotage(campaign, contract, track, FACILITY_COORDS, 7,
                  StratConFacilityOperations.SABOTAGE_TARGET_NUMBER);

            assertEquals(FacilityCondition.DAMAGED, facility.getCondition());
        }

        @Test
        void aWonRaidOnAWellStockedFacilityBringsInSupport() {
            StratConFacility facility = place(ForceAlignment.Opposing, FacilityTrait.WELL_STOCKED);

            StratConFacilityOperations.resolveRaidLoot(campaign, contract, facility);

            assertEquals(FacilityTrait.WELL_STOCKED_RAID_SUPPORT, campaignState.getSupportPoints());
        }

        @Test
        void raidingAnOrdinaryFacilityBringsInNothingExtra() {
            StratConFacility facility = place(ForceAlignment.Opposing, FacilityTrait.HARDENED);

            StratConFacilityOperations.resolveRaidLoot(campaign, contract, facility);

            assertEquals(0, campaignState.getSupportPoints());
        }

        @Test
        void capturedPrototypeGearPaysOnceHoweverTheFacilityIsDealtWith() {
            StratConFacility facility = place(ForceAlignment.Allied, FacilityTrait.EXPERIMENTAL_WEAPONS);

            StratConFacilityOperations.resolveCapture(campaign, contract, track, FACILITY_COORDS,
                  FacilityCaptureChoice.HOLD);

            assertEquals(FacilityTrait.EXPERIMENTAL_WEAPONS_CAPTURE_SUPPORT, campaignState.getSupportPoints());
            assertFalse(facility.hasTrait(FacilityTrait.EXPERIMENTAL_WEAPONS));

            // Taken back by the enemy and captured again, it pays nothing more.
            facility.setOwner(ForceAlignment.Opposing);
            facility.setOwner(ForceAlignment.Allied);
            StratConFacilityOperations.resolveCapture(campaign, contract, track, FACILITY_COORDS,
                  FacilityCaptureChoice.HOLD);
            assertEquals(FacilityTrait.EXPERIMENTAL_WEAPONS_CAPTURE_SUPPORT, campaignState.getSupportPoints());
        }
    }

    @Nested
    class ChangingHands {
        @Test
        void garrisonTraitsLeaveWithTheGarrisonButSiteTraitsStay() {
            StratConFacility facility = facility(ForceAlignment.Opposing, FacilityTier.BASE);
            facility.addTrait(FacilityTrait.VETERAN_GARRISON);
            facility.addTrait(FacilityTrait.HARDENED);

            facility.setOwner(ForceAlignment.Player);

            assertEquals(List.of(FacilityTrait.HARDENED), facility.getTraits());
        }

        @Test
        void passingBetweenYourEmployerAndYouKeepsEveryTrait() {
            StratConFacility facility = facility(ForceAlignment.Allied, FacilityTier.BASE);
            facility.addTrait(FacilityTrait.UNDERMANNED);

            facility.setOwner(ForceAlignment.Player);

            assertTrue(facility.hasTrait(FacilityTrait.UNDERMANNED));
        }
    }

    @Nested
    class Saving {
        @Test
        void traitsSurviveASaveAndLoad() throws JAXBException {
            StratConFacility facility = facility(ForceAlignment.Opposing, FacilityTier.BASE);
            facility.addTrait(FacilityTrait.WELL_STOCKED);
            facility.addTrait(FacilityTrait.POOR_MORALE);

            Marshaller marshaller = JAXBContext.newInstance(StratConFacility.class).createMarshaller();
            StringWriter writer = new StringWriter();
            marshaller.marshal(new JAXBElement<>(new QName("facility"), StratConFacility.class, facility), writer);
            StratConFacility loaded = JAXBContext.newInstance(StratConFacility.class)
                                            .createUnmarshaller()
                                            .unmarshal(new StreamSource(new StringReader(writer.toString())),
                                                  StratConFacility.class)
                                            .getValue();

            assertEquals(List.of(FacilityTrait.WELL_STOCKED, FacilityTrait.POOR_MORALE), loaded.getTraits());
        }

        @Test
        void aFacilitySavedWithoutTraitsHasNone() throws JAXBException {
            String xml = "<facility><definitionId>MekBase</definitionId></facility>";
            StratConFacility loaded = JAXBContext.newInstance(StratConFacility.class)
                                            .createUnmarshaller()
                                            .unmarshal(new StreamSource(new StringReader(xml)), StratConFacility.class)
                                            .getValue();

            assertNotNull(loaded.getTraits());
            assertTrue(loaded.getTraits().isEmpty());
        }
    }
}
