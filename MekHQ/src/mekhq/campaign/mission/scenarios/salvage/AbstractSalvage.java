package mekhq.campaign.mission.scenarios.salvage;

/**
 * Defines the rules of a salvage system (see {@link SalvageSystem}).
 *
 * <p>The default behavior of every method is that of {@link CamOpsStrictSalvage CamOps (Strict)}. Salvage systems
 * override only the rules they change, so a system that has not been fully implemented yet behaves like CamOps
 * (Strict).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract class AbstractSalvage {
    /**
     * Checks whether salvage is recovered through salvage operations.
     *
     * <p>When salvage operations are used, the player assigns salvage formations and techs before a scenario starts,
     * and those teams recover the wrecks afterward. Otherwise, salvage is claimed directly in the resolve scenario
     * wizard.</p>
     *
     * @return {@code true} if salvage operations are used
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isUseSalvageOperations() {
        return true;
    }
}
