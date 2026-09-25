package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.ITransportAssignment;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the helper methods in {@link CamOpsSalvageUtilities} that decide which units can recover salvage, how much
 * they can tow, whose skill a tech uses, and how salvage time is taken from the assigned techs.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CamOpsSalvageUtilitiesTest {
    private static final double DELTA = 0.0001;

    private static Tank tank(boolean isTrailer, double weight) {
        Tank tank = mock(Tank.class);
        when(tank.isTrailer()).thenReturn(isTrailer);
        when(tank.getWeight()).thenReturn(weight);
        return tank;
    }

    private static Mek mekWithHands(boolean hasTwoWorkingHands) {
        Mek mek = mock(Mek.class);
        when(mek.canPerformGroundSalvageOperations()).thenReturn(hasTwoWorkingHands);
        return mek;
    }

    private static Unit unitWithEntity(Entity entity, double currentTowWeight) {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getTotalWeightOfUnitsAssignedToBeTransported(CampaignTransportType.TOW_TRANSPORT,
              TransporterType.TANK_TRAILER_HITCH)).thenReturn(currentTowWeight);
        return unit;
    }

    @Nested
    class IsTowCapable {
        @Test
        void mekWithTwoWorkingHandsCanTow() {
            assertTrue(CamOpsSalvageUtilities.isTowCapable(mekWithHands(true)));
        }

        @Test
        void mekWithoutTwoWorkingHandsCannotTow() {
            assertFalse(CamOpsSalvageUtilities.isTowCapable(mekWithHands(false)));
        }

        @Test
        void vehicleCanTow() {
            assertTrue(CamOpsSalvageUtilities.isTowCapable(tank(false, 50.0)));
        }

        @Test
        void trailerCannotTow() {
            assertFalse(CamOpsSalvageUtilities.isTowCapable(tank(true, 50.0)));
        }

        @Test
        void largeVesselCannotTow() {
            assertFalse(CamOpsSalvageUtilities.isTowCapable(mock(Dropship.class)));
        }

        @Test
        void nullEntityCannotTow() {
            assertFalse(CamOpsSalvageUtilities.isTowCapable(null));
        }
    }

    @Nested
    class GetTowCapacity {
        @Test
        void unhitchedVehicleTowsItsOwnWeight() {
            Unit unit = unitWithEntity(tank(false, 50.0), 0.0);

            assertEquals(50.0, CamOpsSalvageUtilities.getTowCapacity(unit), DELTA);
        }

        @Test
        void trailersAlreadyBeingTowedReduceCapacity() {
            Unit unit = unitWithEntity(tank(false, 50.0), 20.0);

            assertEquals(30.0, CamOpsSalvageUtilities.getTowCapacity(unit), DELTA);
        }

        @Test
        void capacityNeverGoesNegative() {
            Unit unit = unitWithEntity(tank(false, 50.0), 80.0);

            assertEquals(0.0, CamOpsSalvageUtilities.getTowCapacity(unit), DELTA);
        }

        @Test
        void mekTowsItsOwnWeight() {
            Mek mek = mekWithHands(true);
            when(mek.getWeight()).thenReturn(75.0);
            Unit unit = unitWithEntity(mek, 0.0);

            assertEquals(75.0, CamOpsSalvageUtilities.getTowCapacity(unit), DELTA);
        }

        @Test
        void mekWithoutTwoWorkingHandsHasNoTowCapacity() {
            Mek mek = mekWithHands(false);
            when(mek.getWeight()).thenReturn(75.0);
            Unit unit = unitWithEntity(mek, 0.0);

            assertEquals(0.0, CamOpsSalvageUtilities.getTowCapacity(unit), DELTA);
        }

        @Test
        void trailerHasNoTowCapacity() {
            Unit unit = unitWithEntity(tank(true, 50.0), 0.0);

            assertEquals(0.0, CamOpsSalvageUtilities.getTowCapacity(unit), DELTA);
        }

        @Test
        void unitWithoutEntityHasNoTowCapacity() {
            Unit unit = mock(Unit.class);

            assertEquals(0.0, CamOpsSalvageUtilities.getTowCapacity(unit), DELTA);
        }
    }

    @Nested
    class CanSalvage {
        private static final AbstractSalvage STRICT_RULES = new CamOpsStrictSalvage();
        private static final AbstractSalvage REVISED_RULES = new CamOpsRevisedSalvage();

        /** A 'Mek whose unit fails the basic capability check, as it lacks two working hands. */
        private static Unit handlessMek(double cargoCapacity, boolean isFullyCrewed) {
            Mek mek = mekWithHands(false);
            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(mek);
            when(unit.canSalvage(anyBoolean())).thenReturn(false);
            when(unit.getCargoCapacityForSalvage()).thenReturn(cargoCapacity);
            when(unit.isFullyCrewed()).thenReturn(isFullyCrewed);
            return unit;
        }

        @Test
        void unitThatPassesTheBasicCheckCanSalvage() {
            Unit unit = mock(Unit.class);
            when(unit.canSalvage(false)).thenReturn(true);

            assertTrue(STRICT_RULES.canSalvage(unit, false));
        }

        @Test
        void strictRulesDontLetAHandlessMekUseCargo() {
            assertFalse(STRICT_RULES.canSalvage(handlessMek(10.0, true), false));
        }

        @Test
        void otherRulesLetAHandlessMekUseCargo() {
            assertTrue(REVISED_RULES.canSalvage(handlessMek(10.0, true), false));
        }

        @Test
        void handlessMekWithoutCargoCannotSalvage() {
            assertFalse(REVISED_RULES.canSalvage(handlessMek(0.0, true), false));
        }

        @Test
        void handlessMekThatIsNotFullyCrewedCannotSalvage() {
            assertFalse(REVISED_RULES.canSalvage(handlessMek(10.0, false), false));
        }

        @Test
        void handlessMekCannotUseCargoInSpace() {
            assertFalse(REVISED_RULES.canSalvage(handlessMek(10.0, true), true));
        }
    }

    @Nested
    class IsAvailableForSalvage {
        private static final AbstractSalvage STRICT_RULES = new CamOpsStrictSalvage();
        private static final AbstractSalvage REVISED_RULES = new CamOpsRevisedSalvage();

        private static Unit salvageCapableUnit(Entity entity) {
            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.canSalvage(anyBoolean())).thenReturn(true);
            when(unit.isRepairable()).thenReturn(true);
            return unit;
        }

        private static ITransportAssignment transportAssignment(boolean hasTransport) {
            ITransportAssignment transportAssignment = mock(ITransportAssignment.class);
            when(transportAssignment.hasTransport()).thenReturn(hasTransport);
            return transportAssignment;
        }

        private static Mek mek(boolean hasBadLeg, boolean isImmobilized) {
            Mek mek = mock(Mek.class);
            when(mek.atLeastOneBadLeg()).thenReturn(hasBadLeg);
            when(mek.isPermanentlyImmobilized(false)).thenReturn(isImmobilized);
            return mek;
        }

        @Test
        void unitThatCannotSalvageIsUnavailable() {
            Unit unit = salvageCapableUnit(tank(false, 50.0));
            when(unit.canSalvage(anyBoolean())).thenReturn(false);

            assertFalse(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @Test
        void vehicleThatCanSalvageIsAvailable() {
            Unit unit = salvageCapableUnit(tank(false, 50.0));

            assertTrue(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @Test
        void unrepairableUnitIsUnavailable() {
            Unit unit = salvageCapableUnit(tank(false, 50.0));
            when(unit.isRepairable()).thenReturn(false);

            assertFalse(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @Test
        void unitBeingStrippedForPartsIsUnavailable() {
            Unit unit = salvageCapableUnit(tank(false, 50.0));
            when(unit.isSalvage()).thenReturn(true);

            assertFalse(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @Test
        void immobilizedVehicleIsUnavailable() {
            Tank tank = tank(false, 50.0);
            when(tank.isPermanentlyImmobilized(false)).thenReturn(true);
            Unit unit = salvageCapableUnit(tank);

            assertFalse(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @Test
        void dropshipWithoutThrustIsUnavailable() {
            Dropship dropship = mock(Dropship.class);
            when(dropship.isPermanentlyImmobilized(false)).thenReturn(true);
            Unit unit = salvageCapableUnit(dropship);

            assertFalse(STRICT_RULES.isAvailableForSalvage(unit, true));
        }

        @Test
        void unhitchedTrailerIsUnavailable() {
            Unit unit = salvageCapableUnit(tank(true, 50.0));
            when(unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT)).thenReturn(null);

            assertFalse(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @Test
        void trailerWithoutTransportIsUnavailable() {
            Unit unit = salvageCapableUnit(tank(true, 50.0));
            ITransportAssignment assignment = transportAssignment(false);
            when(unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT)).thenReturn(assignment);

            assertFalse(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @Test
        void hitchedTrailerIsAvailable() {
            Unit unit = salvageCapableUnit(tank(true, 50.0));
            ITransportAssignment assignment = transportAssignment(true);
            when(unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT)).thenReturn(assignment);

            assertTrue(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @Test
        void hitchedTrailerIsNotRuledOutByHavingNoEngineOfItsOwn() {
            // Trailers have no MP, so they always read as immobilized; they move with whatever is towing them
            Tank trailer = tank(true, 50.0);
            when(trailer.isPermanentlyImmobilized(false)).thenReturn(true);
            Unit unit = salvageCapableUnit(trailer);
            ITransportAssignment assignment = transportAssignment(true);
            when(unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT)).thenReturn(assignment);

            assertTrue(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @ParameterizedTest
        @CsvSource({
              "false, false",
              "true, false",
              "false, true",
              "true, true"
        })
        void strictRulesIgnoreMekMobility(boolean hasBadLeg, boolean isImmobilized) {
            Unit unit = salvageCapableUnit(mek(hasBadLeg, isImmobilized));

            assertTrue(STRICT_RULES.isAvailableForSalvage(unit, false));
        }

        @ParameterizedTest
        @CsvSource({
              "false, false, true",
              "true, false, false",
              "false, true, false",
              "true, true, false"
        })
        void otherRulesRequireMekMobility(boolean hasBadLeg, boolean isImmobilized, boolean isExpectedAvailable) {
            Unit unit = salvageCapableUnit(mek(hasBadLeg, isImmobilized));

            assertEquals(isExpectedAvailable, REVISED_RULES.isAvailableForSalvage(unit, false));
        }
    }

    @Nested
    class IsUseSecondaryTechSkill {
        @ParameterizedTest
        @CsvSource({
              "MEKWARRIOR, MEK_TECH, true",
              "MEKWARRIOR, MECHANIC, true",
              "MEK_TECH, MECHANIC, false",
              "MEK_TECH, NONE, false",
              "MEKWARRIOR, NONE, false",
              "MEKWARRIOR, DOCTOR, false"
        })
        void secondarySkillOnlyUsedWhenPrimaryRoleIsNotTech(PersonnelRole primaryRole, PersonnelRole secondaryRole,
              boolean expected) {
            Person tech = mock(Person.class);
            when(tech.getPrimaryRole()).thenReturn(primaryRole);
            when(tech.getSecondaryRole()).thenReturn(secondaryRole);

            assertEquals(expected, CamOpsSalvageUtilities.isUseSecondaryTechSkill(tech));
        }
    }

    @Nested
    class DepleteTechMinutes {
        private final Campaign campaign = mockCampaign();

        private Person tech(int minutesLeft) {
            UUID id = UUID.randomUUID();
            Person tech = mock(Person.class);
            when(tech.getId()).thenReturn(id);
            when(tech.getMinutesLeft()).thenReturn(minutesLeft);
            when(campaign.getPlayerForce().getHumanResources().getPerson(id)).thenReturn(tech);
            return tech;
        }

        private static List<UUID> idsOf(Person... techs) {
            List<UUID> ids = new ArrayList<>();
            for (Person tech : techs) {
                ids.add(tech.getId());
            }
            return ids;
        }

        @Test
        void minutesAreSpreadEvenlyWithRemainderGoingFirst() {
            Person techA = tech(480);
            Person techB = tech(480);
            Person techC = tech(480);

            CamOpsSalvageUtilities.depleteTechMinutes(campaign, idsOf(techA, techB, techC), 100);

            // 100 minutes across three techs: 34, 33, 33
            verify(techA).setMinutesLeft(446);
            verify(techB).setMinutesLeft(447);
            verify(techC).setMinutesLeft(447);
        }

        @Test
        void shortfallCarriesOverToTechsWithMoreTime() {
            Person busyTech = tech(10);
            Person freeTech = tech(480);

            // Passed with the busier tech last, to show techs are processed from least time remaining
            CamOpsSalvageUtilities.depleteTechMinutes(campaign, idsOf(freeTech, busyTech), 100);

            verify(busyTech).setMinutesLeft(0);
            verify(freeTech).setMinutesLeft(390);
        }

        @Test
        void usingMoreMinutesThanAvailableEmptiesEveryTech() {
            Person techA = tech(10);
            Person techB = tech(20);

            CamOpsSalvageUtilities.depleteTechMinutes(campaign, idsOf(techA, techB), 100);

            verify(techA).setMinutesLeft(0);
            verify(techB).setMinutesLeft(0);
        }

        @Test
        void noMinutesUsedLeavesTechsUntouched() {
            Person tech = tech(480);

            CamOpsSalvageUtilities.depleteTechMinutes(campaign, idsOf(tech), 0);

            verify(tech, never()).setMinutesLeft(anyInt());
        }

        @Test
        void negativeMinutesUsedLeavesTechsUntouched() {
            Person tech = tech(480);

            CamOpsSalvageUtilities.depleteTechMinutes(campaign, idsOf(tech), -50);

            verify(tech, never()).setMinutesLeft(anyInt());
        }

        @Test
        void missingTechIsSkipped() {
            Person tech = tech(480);
            UUID missingTechId = UUID.randomUUID();
            when(campaign.getPlayerForce().getHumanResources().getPerson(missingTechId)).thenReturn(null);

            List<UUID> techIds = idsOf(tech);
            techIds.add(missingTechId);
            CamOpsSalvageUtilities.depleteTechMinutes(campaign, techIds, 60);

            // The missing tech isn't counted, so the remaining tech covers all 60 minutes
            verify(tech).setMinutesLeft(420);
        }
    }
}
