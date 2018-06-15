package mekhq.module;

import static org.junit.Assert.*;

import org.junit.Test;

public class PersonnelMarketServiceManagerTest {

    @Test
    public void findsServices() {
        PersonnelMarketServiceManager manager = PersonnelMarketServiceManager.getInstance();
        
        assertTrue(manager.getAllMethods().size() > 0);
    }

}
