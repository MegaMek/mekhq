package mekhq.module;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.PersonnelMarketDylan;
import mekhq.campaign.market.PersonnelMarketFMMr;
import mekhq.campaign.market.PersonnelMarketRandom;
import mekhq.campaign.market.PersonnelMarketStratOps;
import mekhq.module.atb.PersonnelMarketAtB;

public class PersonnelMarketServiceManagerTest {

    @Test
    public void providesRandomMarket() {
        assertTrue(PersonnelMarketServiceManager.getInstance()
                .getService(PersonnelMarket.getTypeName(PersonnelMarket.TYPE_RANDOM))
                instanceof PersonnelMarketRandom);
    }

    @Test
    public void providesFMMrMarket() {
        assertTrue(PersonnelMarketServiceManager.getInstance()
                .getService(PersonnelMarket.getTypeName(PersonnelMarket.TYPE_FMMR))
                instanceof PersonnelMarketFMMr);
    }

    @Test
    public void providesStratOpsMarket() {
        assertTrue(PersonnelMarketServiceManager.getInstance()
                .getService(PersonnelMarket.getTypeName(PersonnelMarket.TYPE_STRAT_OPS))
                instanceof PersonnelMarketStratOps);
    }

    @Test
    public void providesDylansMarket() {
        assertTrue(PersonnelMarketServiceManager.getInstance()
                .getService(PersonnelMarket.getTypeName(PersonnelMarket.TYPE_DYLANS))
                instanceof PersonnelMarketDylan);
    }

    @Test
    public void providesAtBMarket() {
        assertTrue(PersonnelMarketServiceManager.getInstance()
                .getService(PersonnelMarket.getTypeName(PersonnelMarket.TYPE_ATB))
                instanceof PersonnelMarketAtB);
    }

}
