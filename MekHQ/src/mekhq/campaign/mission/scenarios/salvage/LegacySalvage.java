package mekhq.campaign.mission.scenarios.salvage;

/**
 * The {@link SalvageSystem#LEGACY Legacy} salvage system: the original MekHQ method, where salvage is claimed directly
 * in the resolve scenario wizard without salvage operations.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class LegacySalvage extends AbstractSalvage {
    @Override
    public boolean isUseSalvageOperations() {
        return false;
    }
}
