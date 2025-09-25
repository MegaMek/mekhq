package mekhq.campaign.market;

import mekhq.campaign.Campaign;

/**
 * Test-only PartsStore that does not stock by default.
 */
public class TestPartsStore extends PartsStore{
    public TestPartsStore() {
        super();
    }

    @Override
    public void stock(Campaign campaign) {
        // Does nothing
    }

    public void reallyStock(Campaign campaign) {
        // runs the real stock(); high memory and CPU time use.
        super.stock(campaign);
    }
}
