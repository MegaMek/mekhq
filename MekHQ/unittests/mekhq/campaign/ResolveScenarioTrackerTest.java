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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.getEntityForUnitTesting;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import megamek.client.IClient;
import megamek.common.Player;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.event.PostGameResolution;
import megamek.common.icons.Camouflage;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Crew;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for {@link ResolveScenarioTracker#processGame()}, verifying that enemy entities destroyed by ammo detonation
 * (devastated) are properly tracked for prisoner capture.
 *
 * @see <a href="https://github.com/MegaMek/mekhq/issues/6497">GitHub issue #6497</a>
 */
class ResolveScenarioTrackerTest {

    private Campaign campaign;
    private Scenario scenario;
    private IClient client;
    private PostGameResolution victoryEvent;

    // Players
    private Player localPlayer;
    private Player enemyPlayer;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        // Create two players on different teams so isEnemyOf works correctly
        localPlayer = new Player(0, "LocalPlayer");
        localPlayer.setTeam(1);

        enemyPlayer = new Player(1, "EnemyPlayer");
        enemyPlayer.setTeam(2);

        // Mock IClient
        client = mock(IClient.class);
        when(client.getLocalPlayer()).thenReturn(localPlayer);

        // Mock Scenario - return empty forces and loot
        scenario = mock(Scenario.class);
        Formation emptyFormation = mock(Formation.class);
        when(emptyFormation.getAllUnits(anyBoolean())).thenReturn(new Vector<>());
        when(scenario.getForces(any())).thenReturn(emptyFormation);
        when(scenario.getTraitorUnits(any())).thenReturn(Collections.emptyList());
        when(scenario.getLoot()).thenReturn(List.of());
        when(scenario.isTraitor(any(Entity.class), any(Campaign.class))).thenReturn(false);

        // Mock Campaign
        campaign = mockCampaign();
        when(campaign.getPlayerForce().getFaction()).thenReturn(null);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3067, 1, 1));
        CampaignOptions campaignOptions = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getGameOptions()).thenReturn(new megamek.common.options.GameOptions());

        // Mock PostGameResolution - default empty enumerations
        victoryEvent = mock(PostGameResolution.class);
        when(victoryEvent.getEntities()).thenReturn(Collections.emptyEnumeration());
        when(victoryEvent.getGraveyardEntities()).thenReturn(Collections.emptyEnumeration());
        when(victoryEvent.getRetreatedEntities()).thenReturn(Collections.emptyEnumeration());
        when(victoryEvent.getDevastatedEntities()).thenReturn(Collections.emptyEnumeration());
        when(victoryEvent.getWreckedEntities()).thenReturn(Collections.emptyEnumeration());
    }

    /**
     * Creates a ResolveScenarioTracker with the test fixtures and sets the victory event and client.
     */
    private ResolveScenarioTracker createTracker() {
        ResolveScenarioTracker tracker = new ResolveScenarioTracker(scenario, campaign, true);
        tracker.setClient(client);
        tracker.setEvent(victoryEvent);
        return tracker;
    }

    /**
     * Creates an enemy entity with a unique external ID, assigned to the enemy player.
     */
    private Entity createEnemyEntity(String unitName) {
        Entity entity = getEntityForUnitTesting(unitName, false);
        if (entity == null) {
            throw new IllegalStateException("Failed to load test entity: " + unitName);
        }
        entity.setOwner(enemyPlayer);
        entity.setExternalIdAsString(UUID.randomUUID().toString());
        entity.setCamouflage(new Camouflage());
        return entity;
    }

    /**
     * Verifies that an enemy entity in the devastated list is added to devastatedEnemyUnits and tracked in
     * salvageStatus. This is the fix for GitHub issue #6497: pilots of opfor units destroyed by ammo detonation were
     * missing from the capturable personnel list.
     */
    @Test
    void processGameAddsDevastatedEnemyUnitsToDevastatedList() {
        Entity devastatedEnemy = createEnemyEntity("Locust LCT-1V");

        // Every call gets a fresh enumeration, as the real results do; the tracker reads this list more than once
        when(victoryEvent.getDevastatedEntities())
              .thenAnswer(invocation -> Collections.enumeration(List.of(devastatedEnemy)));

        ResolveScenarioTracker tracker = createTracker();
        tracker.processGame();

        assertFalse(tracker.devastatedEnemyUnits.isEmpty(),
              "Devastated enemy units should contain the destroyed enemy entity");
        assertEquals(1, tracker.devastatedEnemyUnits.size(),
              "Should have exactly one devastated enemy unit");

        TestUnit capturedUnit = tracker.devastatedEnemyUnits.getFirst();
        assertTrue(tracker.salvageStatus.containsKey(capturedUnit.getId()),
              "Devastated enemy unit should be tracked in salvageStatus for prisoner processing");
    }

    /**
     * Verifies that an enemy EjectedCrew in the devastated list is routed to enemyEjections and NOT added to
     * devastatedEnemyUnits, matching the graveyard handling pattern.
     */
    @Test
    void processGameAddsDevastatedEnemyEjectedCrewToEnemyEjections() {
        Entity originalRide = createEnemyEntity("Locust LCT-1V");
        EjectedCrew ejectedCrew = new EjectedCrew(originalRide);
        ejectedCrew.setOwner(enemyPlayer);
        UUID ejectedId = UUID.randomUUID();
        ejectedCrew.setExternalIdAsString(ejectedId.toString());
        ejectedCrew.getCrew().setExternalIdAsString(ejectedId.toString(), 0);
        ejectedCrew.setCamouflage(new Camouflage());

        // Every call gets a fresh enumeration, as the real results do; the tracker reads this list more than once
        when(victoryEvent.getDevastatedEntities())
              .thenAnswer(invocation -> Collections.enumeration(List.of(ejectedCrew)));

        ResolveScenarioTracker tracker = createTracker();
        tracker.processGame();

        assertTrue(tracker.enemyEjections.containsKey(ejectedId),
              "Ejected enemy crew from devastated unit should be in enemyEjections");
        assertTrue(tracker.devastatedEnemyUnits.isEmpty(),
              "EjectedCrew should not appear in devastatedEnemyUnits");
    }

    /**
     * Creates a player entity with a unique external ID and crew, assigned to the local player.
     */
    private Entity createPlayerEntity(String unitName) {
        Entity entity = getEntityForUnitTesting(unitName, false);
        if (entity == null) {
            throw new IllegalStateException("Failed to load test entity: " + unitName);
        }
        entity.setOwner(localPlayer);
        UUID entityId = UUID.randomUUID();
        entity.setExternalIdAsString(entityId.toString());
        entity.setCamouflage(new Camouflage());

        // Ensure the crew has a valid external ID so crew recovery can be tested
        if (entity.getCrew() != null) {
            entity.getCrew().setExternalIdAsString(UUID.randomUUID().toString(), 0);
        }
        return entity;
    }

    /**
     * Creates a mock {@link Unit} wrapping the given entity with a specific ID.
     */
    private Unit createMockUnit(Entity entity, UUID unitId) {
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(unitId);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getName()).thenReturn(entity.getDisplayName());
        when(unit.getActiveCrew()).thenReturn(new ArrayList<>());
        when(unit.getCrew()).thenReturn(new ArrayList<>());
        return unit;
    }

    /**
     * Verifies that a player unit deployed by MekHQ but never present in game results (e.g., rejected by the server as
     * an illegal design) is recovered rather than treated as a total loss. This is the fix for GitHub issue #6606.
     *
     * @see <a href="https://github.com/MegaMek/mekhq/issues/6606">GitHub issue #6606</a>
     */
    @Test
    void processGameRecoversDeployedUnitNotInResults() {
        Entity playerEntity = createPlayerEntity("Locust LCT-1V");
        UUID unitId = UUID.fromString(playerEntity.getExternalIdAsString());
        Unit unit = createMockUnit(playerEntity, unitId);

        ResolveScenarioTracker tracker = createTracker();

        // Simulate the state the constructor creates: unit is deployed but assumed lost
        tracker.units.add(unit);
        ResolveScenarioTracker.UnitStatus status = new ResolveScenarioTracker.UnitStatus(unit);
        tracker.unitsStatus.put(unitId, status);

        // Precondition: UnitStatus defaults to total loss
        assertTrue(status.isTotalLoss(), "UnitStatus should default to total loss");

        // The unit does NOT appear in any victory event enumeration (all are empty),
        // so entities map will remain empty after processGame
        tracker.processGame();

        // The safety net should have recovered the unit
        assertFalse(status.isTotalLoss(),
              "Unit should not be a total loss when it never appeared in game results");

        // Crew should be tracked as alive
        UUID crewId = UUID.fromString(playerEntity.getCrew().getExternalIdAsString());
        assertTrue(tracker.pilots.containsKey(crewId),
              "Crew of recovered unit should be tracked in pilots map");
    }

    /**
     * Verifies that a player unit which DID appear in results is NOT double-processed by the safety-net recovery
     * logic.
     */
    @Test
    void processGameDoesNotRecoverUnitFoundInResults() {
        Entity playerEntity = createPlayerEntity("Locust LCT-1V");
        UUID unitId = UUID.fromString(playerEntity.getExternalIdAsString());
        Unit unit = createMockUnit(playerEntity, unitId);

        // Make the entity appear in the devastated list — it IS in results, genuinely destroyed
        playerEntity.setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);

        // Every call gets a fresh enumeration, as the real results do; the tracker reads this list more than once
        when(victoryEvent.getDevastatedEntities())
              .thenAnswer(invocation -> Collections.enumeration(List.of(playerEntity)));

        ResolveScenarioTracker tracker = createTracker();
        tracker.units.add(unit);
        ResolveScenarioTracker.UnitStatus status = new ResolveScenarioTracker.UnitStatus(unit);
        tracker.unitsStatus.put(unitId, status);

        tracker.processGame();

        // The entity was found in devastated results, so it should be marked as a total loss
        assertTrue(status.isTotalLoss(),
              "Unit that appeared in devastated results should remain a total loss");
    }

    /**
     * Creates a mock crew member with a unique ID.
     */
    private Person mockCrewMember(String fullName) {
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        when(person.getFullName()).thenReturn(fullName);
        return person;
    }

    /**
     * Sets up a unit credited with a single kill, and returns it.
     */
    private Unit unitCreditedWithAKill(ResolveScenarioTracker tracker, List<Person> activeCrew) {
        Entity playerEntity = createPlayerEntity("Locust LCT-1V");
        UUID unitId = UUID.fromString(playerEntity.getExternalIdAsString());
        Unit unit = createMockUnit(playerEntity, unitId);
        when(unit.getFormationId()).thenReturn(1);
        when(unit.getActiveCrew()).thenReturn(activeCrew);

        tracker.units.add(unit);
        tracker.killCredits.put("Atlas AS7-D", unitId.toString());
        return unit;
    }

    /**
     * A unit scores a kill once, not once per crew member. Multi-crew units used to record the same destroyed unit in
     * their history for every named crew member aboard.
     */
    @Test
    void assignKillsRecordsOneUnitHistoryEntryForAMultiCrewUnit() {
        ResolveScenarioTracker tracker = createTracker();
        Person commander = mockCrewMember("Natasha Kerensky");
        Unit unit = unitCreditedWithAKill(tracker,
              List.of(commander, mockCrewMember("Joanna Fetladral"), mockCrewMember("Nikolai Malthus")));
        when(unit.getCommander()).thenReturn(commander);

        tracker.assignKills();

        verify(unit, times(1)).addKillLogEntry(any());
    }

    /**
     * A unit run entirely by temporary (blob) crew still scored the kill, so it still records one. Its history used to
     * be empty, because the entry was written from inside the loop over named crew.
     */
    @Test
    void assignKillsRecordsAUnitHistoryEntryForAUnitWithoutNamedCrew() {
        ResolveScenarioTracker tracker = createTracker();
        Unit unit = unitCreditedWithAKill(tracker, List.of());

        tracker.assignKills();

        verify(unit, times(1)).addKillLogEntry(any());
    }

    // region Blob (temporary) crew casualties

    /**
     * Loads a test entity from a BLK file (infantry, vehicles) and prepares it as a player unit with a valid external
     * ID and crew ID.
     */
    private Entity createPlayerBlkEntity(String unitName) {
        Entity entity = getEntityForUnitTesting(unitName, true);
        if (entity == null) {
            throw new IllegalStateException("Failed to load test entity: " + unitName);
        }
        entity.setOwner(localPlayer);
        entity.setExternalIdAsString(UUID.randomUUID().toString());
        entity.setCamouflage(new Camouflage());
        if (entity.getCrew() != null) {
            entity.getCrew().setExternalIdAsString(UUID.randomUUID().toString(), 0);
        }
        return entity;
    }

    /**
     * Creates a mock {@link Unit} wrapping the given entity with no named crew but the supplied number of temp (blob)
     * crew of a single role.
     */
    private Unit createBlobCrewUnit(Entity entity, PersonnelRole tempRole, int tempCount, int totalCrewSize) {
        UUID unitId = UUID.fromString(entity.getExternalIdAsString());
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(unitId);
        when(unit.getEntity()).thenReturn(entity);
        // Use a literal rather than entity.getDisplayName(): calling a method on the (possibly mocked) entity while
        // this stubbing is open trips Mockito's unfinished-stubbing detection.
        when(unit.getName()).thenReturn("Blob Crew Test Unit");
        when(unit.getActiveCrew()).thenReturn(new ArrayList<>());
        when(unit.getCrew()).thenReturn(new ArrayList<>());
        when(unit.getTotalCrewSize()).thenReturn(totalCrewSize);
        when(unit.getTotalTempCrew()).thenReturn(tempCount);
        when(unit.getTempCrewByPersonnelRole(tempRole)).thenReturn(tempCount);
        return unit;
    }

    /**
     * Registers a unit with the tracker, forcing its post-battle entity and total-loss state.
     */
    private void registerUnit(ResolveScenarioTracker tracker, Unit unit, Entity entity, boolean totalLoss) {
        tracker.units.add(unit);
        ResolveScenarioTracker.UnitStatus status = new ResolveScenarioTracker.UnitStatus(unit);
        status.assignFoundEntity(entity, totalLoss);
        tracker.unitsStatus.put(unit.getId(), status);
    }

    /**
     * A wiped-out infantry platoon (for example a field gun) crewed almost entirely by temp crew used to process only
     * one casualty per named crew member, leaving the rest of the platoon untouched. Every casualty must now fall on
     * the temp crew, and each loss must be removed from the campaign pool so it is permanent.
     */
    @Test
    void checkStatusOfPersonnelDrainsAllTempCrewWhenInfantryIsWipedOut() {
        Entity infantry = createPlayerBlkEntity("Foot Platoon (DCMS) (MG 2620+)");
        int tempCrew = 10;
        Unit unit = createBlobCrewUnit(infantry, PersonnelRole.SOLDIER, tempCrew, tempCrew);

        ResolveScenarioTracker tracker = createTracker();
        registerUnit(tracker, unit, infantry, true);

        tracker.checkStatusOfPersonnel();

        verify(campaign).decreaseTempCrewPool(PersonnelRole.SOLDIER, tempCrew);
        verify(unit).setTempCrew(PersonnelRole.SOLDIER, 0);
    }

    /**
     * A destroyed vehicle crewed by temp crew now kills them: each temp crew member rolls to survive on the same odds
     * as named vehicle crew (2d6 >= 7 survives). Here the roll is lethal, so all of them die and are removed from the
     * campaign pool.
     */
    @Test
    void checkStatusOfPersonnelKillsVehicleTempCrewWhenCrewIsLostAndRollIsLethal() {
        Entity tank = createPlayerBlkEntity("Prime Mover");
        int tempCrew = 5;
        Unit unit = createBlobCrewUnit(tank, PersonnelRole.VEHICLE_CREW_GROUND, tempCrew, tempCrew);

        ResolveScenarioTracker tracker = createTracker();
        registerUnit(tracker, unit, tank, true);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(2); // < 7: killed
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);

            tracker.checkStatusOfPersonnel();
        }

        verify(campaign).decreaseTempCrewPool(PersonnelRole.VEHICLE_CREW_GROUND, tempCrew);
    }

    /**
     * Same destroyed vehicle, but the survival rolls are non-lethal: the temp crew survive, so nothing is removed from
     * the campaign pool.
     */
    @Test
    void checkStatusOfPersonnelSparesVehicleTempCrewWhenCrewIsLostButRollSurvives() {
        Entity tank = createPlayerBlkEntity("Prime Mover");
        int tempCrew = 5;
        Unit unit = createBlobCrewUnit(tank, PersonnelRole.VEHICLE_CREW_GROUND, tempCrew, tempCrew);

        ResolveScenarioTracker tracker = createTracker();
        registerUnit(tracker, unit, tank, true);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(12); // >= 7: survives
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);

            tracker.checkStatusOfPersonnel();
        }

        verify(campaign, never()).decreaseTempCrewPool(any(PersonnelRole.class), anyInt());
    }

    /**
     * A vehicle that survives the battle with its named crew intact does not endanger its temp crew.
     */
    @Test
    void checkStatusOfPersonnelSparesVehicleTempCrewWhenVehicleSurvivesWithItsCrew() {
        Entity tank = createPlayerBlkEntity("Prime Mover");
        int tempCrew = 5;
        Unit unit = createBlobCrewUnit(tank, PersonnelRole.VEHICLE_CREW_GROUND, tempCrew, tempCrew + 1);
        Person commander = mockCrewMember("Driver");
        when(unit.getCommander()).thenReturn(commander);

        ResolveScenarioTracker tracker = createTracker();
        // A recovered crew means the vehicle was not lost.
        tracker.pilots.put(commander.getId(), tank.getCrew());
        registerUnit(tracker, unit, tank, false);

        tracker.checkStatusOfPersonnel();

        verify(campaign, never()).decreaseTempCrewPool(any(PersonnelRole.class), anyInt());
    }

    /**
     * A vehicle that survives as a total unit but whose hull is breached (a non-turret, non-body location reduced to
     * zero internal structure) has lost its crew compartment, so its temp crew are at risk even though it was not a
     * total loss and its named crew were recovered.
     */
    @Test
    void checkStatusOfPersonnelKillsVehicleTempCrewWhenHullIsBreached() {
        Entity tank = createPlayerBlkEntity("Prime Mover");
        breachFirstCrewLocation(tank);
        int tempCrew = 4;
        Unit unit = createBlobCrewUnit(tank, PersonnelRole.VEHICLE_CREW_GROUND, tempCrew, tempCrew + 1);
        Person commander = mockCrewMember("Driver");
        when(unit.getCommander()).thenReturn(commander);

        ResolveScenarioTracker tracker = createTracker();
        tracker.pilots.put(commander.getId(), tank.getCrew());
        registerUnit(tracker, unit, tank, false);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(2); // < 7: killed
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);

            tracker.checkStatusOfPersonnel();
        }

        verify(campaign).decreaseTempCrewPool(PersonnelRole.VEHICLE_CREW_GROUND, tempCrew);
    }

    /**
     * Reduces the first non-turret, non-body location of a vehicle to zero internal structure, matching the "hull
     * breach" criteria used by the resolution logic.
     */
    private void breachFirstCrewLocation(Entity tank) {
        for (int loc = 0; loc < tank.locations(); loc++) {
            if ((loc == Tank.LOC_TURRET) || (loc == Tank.LOC_TURRET_2) || (loc == Tank.LOC_BODY)) {
                continue;
            }
            tank.setInternal(0, loc);
            return;
        }
    }

    /**
     * A destroyed large craft (DropShip, JumpShip, WarShip, Small Craft) computes casualties from crew hits, which can
     * far exceed the number of named crew. The overflow must fall on the temp (blob) vessel crew rather than being
     * dropped, and each loss must leave the campaign pool.
     */
    @Test
    void checkStatusOfPersonnelDrainsLargeCraftTempCrewBeyondNamedCrew() {
        SmallCraft smallCraft = mock(SmallCraft.class);
        Crew crew = mock(Crew.class); // isEjected() and getHits() default to false/0
        when(smallCraft.getCrew()).thenReturn(crew);
        when(smallCraft.isDestroyed()).thenReturn(true);
        when(smallCraft.getFullChassis()).thenReturn("Test");
        when(smallCraft.getModel()).thenReturn("Craft");
        when(smallCraft.getExternalIdAsString()).thenReturn(UUID.randomUUID().toString());

        int fullCrewSize = 8;
        int tempCrew = 20;
        Unit unit = createBlobCrewUnit(smallCraft, PersonnelRole.VESSEL_CREW, tempCrew, tempCrew);

        ResolveScenarioTracker tracker = createTracker();
        registerUnit(tracker, unit, smallCraft, true);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.getFullCrewSize(smallCraft)).thenReturn(fullCrewSize);
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);

            tracker.checkStatusOfPersonnel();
        }

        verify(campaign).decreaseTempCrewPool(PersonnelRole.VESSEL_CREW, fullCrewSize);
    }

    // endregion Blob (temporary) crew casualties
}
