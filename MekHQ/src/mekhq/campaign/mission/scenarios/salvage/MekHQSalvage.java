package mekhq.campaign.mission.scenarios.salvage;

/**
 * The {@link SalvageSystem#MEKHQ MekHQ} salvage system: an expanded version of Campaign Operations salvage.
 *
 * <p>Salvage formations may fight in a scenario and still salvage it, and salvaging 'Meks need working legs.
 * Otherwise, not yet implemented; currently behaves like {@link CamOpsStrictSalvage CamOps (Strict)}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class MekHQSalvage extends AbstractSalvage {
    @Override
    public boolean isSalvageFormationCombatAllowed() {
        return true;
    }

    @Override
    public boolean isMekMobilityRequiredForSalvage() {
        return true;
    }
}
