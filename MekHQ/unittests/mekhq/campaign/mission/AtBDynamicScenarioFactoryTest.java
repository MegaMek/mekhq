package mekhq.campaign.mission;

import megamek.common.Entity;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.DOMException;

import java.io.IOException;

import static mekhq.campaign.mission.AtBDynamicScenarioFactory.createEntityWithCrew;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class AtBDynamicScenarioFactoryTest {
    Campaign campaign;
    TestSystems systems;
    
    @BeforeAll
    public static void setUpBeforeClass() throws IOException, DOMException {
    }
    
    @BeforeEach
    public void setUp() {
        systems = TestSystems.getInstance();
        
        String current = "Terra";
        String start = "Galatea";
        Planet primary = new Planet(current);
        Planet startPrimary = new Planet(start);
        PlanetarySystem currentSystem = mock(PlanetarySystem.class);
        PlanetarySystem startSystem = mock(PlanetarySystem.class);
        when(currentSystem.getId()).thenReturn(current);
        when(currentSystem.getPrimaryPlanet()).thenReturn(primary);

        when(startSystem.getId()).thenReturn(start);
        when(startSystem.getPrimaryPlanet()).thenReturn(startPrimary);
        
        systems.addPlanetarySystem(currentSystem);
        systems.addPlanetarySystem(startSystem);
        
        /**
        when(campaign.getCurrentSystem()).thenReturn(currentSystem);
        doReturn(currentSystem).when(systems).getSystemById(eq(current));
        doReturn(currentSystem).when(campaign).getSystemByName(eq(current));
        doReturn(startSystem).when(systems).getSystemById(eq(start));
        doReturn(startSystem).when(campaign).getSystemByName(eq(start));
         */

        campaign = spy(new Campaign(startSystem));
    }

    @Test
    public void testCreateEntityWithCrewNoCallsigns() {
        String factionCode = "LC";
        String unitName = "Shadow Hawk SHD-2H";
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(unitName);
        SkillLevel skill = SkillLevel.ULTRA_GREEN;
        Entity entity = createEntityWithCrew(factionCode, skill, campaign, mekSummary);
    }
}
