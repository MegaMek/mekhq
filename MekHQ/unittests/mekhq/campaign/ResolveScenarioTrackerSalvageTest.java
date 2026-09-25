package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import megamek.common.icons.Camouflage;
import megamek.common.units.Entity;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.BotForce;
import mekhq.campaign.mission.scenarios.Scenario;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers the salvage-related helpers of {@link ResolveScenarioTracker}: ignoring entities MegaMek reports more than
 * once, and finding the camouflage MekHQ assigned to salvage before the scenario.
 */
class ResolveScenarioTrackerSalvageTest {
    private static Entity entity(int id, String externalId, Camouflage camouflage) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getExternalIdAsString()).thenReturn(externalId);
        when(entity.getCamouflage()).thenReturn(camouflage);
        return entity;
    }

    @Nested
    class DuplicateEntities {
        private final Set<Integer> processedEntityIds = new HashSet<>();

        @Test
        void firstSightingIsProcessed() {
            assertFalse(ResolveScenarioTracker.isDuplicateEntity(entity(3, "-1", null), processedEntityIds));
            assertTrue(processedEntityIds.contains(3));
        }

        @Test
        void secondSightingIsSkipped() {
            ResolveScenarioTracker.isDuplicateEntity(entity(3, "-1", null), processedEntityIds);

            assertTrue(ResolveScenarioTracker.isDuplicateEntity(entity(3, "-1", null), processedEntityIds));
        }

        @Test
        void differentEntitiesAreBothProcessed() {
            assertFalse(ResolveScenarioTracker.isDuplicateEntity(entity(3, "-1", null), processedEntityIds));
            assertFalse(ResolveScenarioTracker.isDuplicateEntity(entity(4, "-1", null), processedEntityIds));
        }

        @Test
        void entitiesWithoutAGameIdAreAlwaysProcessed() {
            assertFalse(ResolveScenarioTracker.isDuplicateEntity(entity(Entity.NONE, "-1", null), processedEntityIds));
            assertFalse(ResolveScenarioTracker.isDuplicateEntity(entity(Entity.NONE, "-1", null), processedEntityIds));
            assertTrue(processedEntityIds.isEmpty());
        }
    }

    @Nested
    class PreScenarioCamouflage {
        private final Campaign campaign = mockCampaign();
        private final Scenario scenario = mock(Scenario.class);
        private final List<BotForce> botForces = new ArrayList<>();
        private final ResolveScenarioTracker tracker;

        private final Camouflage lobbyCamouflage = new Camouflage("Lobby", "Changed.png");
        private final Camouflage unitCamouflage = new Camouflage("Clans", "Wolf.png");
        private final Camouflage forceCamouflage = new Camouflage("Clans", "Jade Falcon.png");

        PreScenarioCamouflage() {
            when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
            when(campaign.getLocalDate()).thenReturn(LocalDate.of(3067, 1, 1));
            Formation noForces = mock(Formation.class);
            when(noForces.getAllUnits(anyBoolean())).thenReturn(new Vector<>());
            when(scenario.getForces(any())).thenReturn(noForces);
            when(scenario.getBotForces()).thenReturn(botForces);
            tracker = new ResolveScenarioTracker(scenario, campaign, true);
        }

        private void addBotForce(Camouflage botForceCamouflage, Entity... entities) {
            BotForce botForce = mock(BotForce.class);
            when(botForce.getCamouflage()).thenReturn(botForceCamouflage);
            when(botForce.getFullEntityList(campaign)).thenReturn(new ArrayList<>(Arrays.asList(entities)));
            botForces.add(botForce);
        }

        @Test
        void unitsOwnCamouflageWinsOverTheLobby() {
            addBotForce(forceCamouflage, entity(1, "abc", unitCamouflage));

            assertSame(unitCamouflage, tracker.getPreScenarioCamouflage(entity(9, "abc", lobbyCamouflage)));
        }

        @Test
        void botForceCamouflageIsUsedWhenTheUnitHasNone() {
            addBotForce(forceCamouflage, entity(1, "abc", new Camouflage()));

            assertSame(forceCamouflage, tracker.getPreScenarioCamouflage(entity(9, "abc", lobbyCamouflage)));
        }

        @Test
        void unitWithoutCamouflageInAForceWithoutCamouflageKeepsItsOwn() {
            Camouflage noCamouflage = new Camouflage();
            addBotForce(null, entity(1, "abc", noCamouflage));

            assertSame(noCamouflage, tracker.getPreScenarioCamouflage(entity(9, "abc", lobbyCamouflage)));
        }

        @Test
        void entityOutsideTheBotForcesKeepsItsInGameCamouflage() {
            addBotForce(forceCamouflage, entity(1, "abc", unitCamouflage));

            assertSame(lobbyCamouflage, tracker.getPreScenarioCamouflage(entity(9, "xyz", lobbyCamouflage)));
        }

        @Test
        void entityWithoutAnExternalIdKeepsItsInGameCamouflage() {
            addBotForce(forceCamouflage, entity(1, "-1", unitCamouflage));

            assertSame(lobbyCamouflage, tracker.getPreScenarioCamouflage(entity(9, "-1", lobbyCamouflage)));
        }

        @Test
        void missingBotEntitiesAreSkipped() {
            addBotForce(forceCamouflage, (Entity) null);
            addBotForce(forceCamouflage, entity(1, "abc", unitCamouflage));

            assertSame(unitCamouflage, tracker.getPreScenarioCamouflage(entity(9, "abc", lobbyCamouflage)));
        }

        @Test
        void scenarioWithoutBotForcesKeepsTheInGameCamouflage() {
            assertSame(lobbyCamouflage, tracker.getPreScenarioCamouflage(entity(9, "abc", lobbyCamouflage)));
        }
    }
}
