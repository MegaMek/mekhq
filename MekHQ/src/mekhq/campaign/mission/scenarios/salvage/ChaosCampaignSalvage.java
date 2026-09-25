package mekhq.campaign.mission.scenarios.salvage;

/**
 * The {@link SalvageSystem#CHAOS_CAMPAIGN Chaos Campaign} salvage system: a simplified version of Campaign Operations
 * salvage.
 *
 * <p>Salvage teams aren't used; the player recovers every wreck automatically. The salvage rights set the player's
 * share of each wreck's value, which they receive in cash unless they buy the unit from the employer (see
 * {@link AbstractSalvage#isUseSalvagePurchases()}).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ChaosCampaignSalvage extends AbstractSalvage {
    @Override
    public boolean isUseSalvageOperations() {
        return false;
    }

    @Override
    public boolean isUseSalvagePurchases() {
        return true;
    }
}
