package mekhq.campaign.mission.scenarios.salvage;

/**
 * The {@link SalvageSystem#MEKHQ MekHQ} salvage system: an expanded version of Campaign Operations salvage.
 *
 * <p>Salvage formations may fight in a scenario and still salvage it. Salvaging 'Meks need working legs, and
 * 'Meks without two working hands may still salvage using cargo space (but can't drag salvage). Carrying units
 * may recover as many wrecks as fit in their cargo space (or, in space, suitable bays).</p>
 *
 * <p>Salvage teams must still recover each wreck, but the salvage rights set the player's share of its value rather
 * than capping what they may claim. For each recovered wreck, the player receives their share in cash unless they buy
 * the unit from the employer (see {@link AbstractSalvage#isUseSalvagePurchases()}).</p>
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

    @Override
    public boolean isMekCargoSalvageWithoutHandsAllowed() {
        return true;
    }

    @Override
    public boolean isMultipleSalvagePerUnitAllowed() {
        return true;
    }

    @Override
    public boolean isUseSalvagePurchases() {
        return true;
    }
}
