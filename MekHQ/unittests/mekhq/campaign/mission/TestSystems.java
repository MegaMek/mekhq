package mekhq.campaign.mission;

import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

public class TestSystems extends Systems {
    
    private static TestSystems systems;

    public static TestSystems getInstance() {
        if (systems == null) {
            systems = new TestSystems();
        }

        return systems;
    }

    public static void setInstance(TestSystems instance) {
        systems = instance;
    }
    
    private TestSystems() {
        super();
    }
    
    public void addPlanetarySystem(PlanetarySystem system) {
        systemList.put(system.getId(), system);
    }

}
