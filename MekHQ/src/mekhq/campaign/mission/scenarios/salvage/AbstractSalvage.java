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

    /**
     * Checks whether a formation set to 'Salvage' may fight in a scenario and still recover that scenario's salvage.
     *
     * <p>Under CamOps (Strict) rules, a salvage team cannot take part in the fighting; it sits on the sidelines until
     * the OpFor has been routed. Other systems may allow Salvage formations to fight and then salvage. Units in any
     * other kind of formation can never salvage a scenario they fought in.</p>
     *
     * @return {@code true} if Salvage formations may fight and then salvage the same scenario
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isSalvageFormationCombatAllowed() {
        return false;
    }

    /**
     * Checks whether a 'Mek needs working legs to take part in salvage operations.
     *
     * <p>Campaign Operations only requires a salvaging 'Mek to have two working hands, so under CamOps (Strict) a 'Mek
     * with a destroyed leg can still salvage. Other systems require the 'Mek to have no destroyed legs and to not be
     * permanently immobilized.</p>
     *
     * @return {@code true} if a 'Mek with a destroyed leg, or that is immobilized, cannot salvage
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isMekMobilityRequiredForSalvage() {
        return false;
    }

    /**
     * Checks whether a 'Mek without two working hands may still salvage using its cargo space, such as lift hoists.
     *
     * <p>A 'Mek always needs two working hands to drag salvage. Under CamOps (Strict), a 'Mek without them can't
     * salvage at all. Other systems let it carry salvage in its cargo space instead.</p>
     *
     * @return {@code true} if a 'Mek without two working hands may salvage using its cargo space
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isMekCargoSalvageWithoutHandsAllowed() {
        return false;
    }

    /**
     * Checks whether a unit carrying salvage may recover more than one wreck.
     *
     * <p>Under CamOps (Strict), each recovery unit recovers a single wreck. Other systems let a unit carry as many
     * wrecks as fit in its cargo space. In space, fighters and small craft are instead limited by the number of
     * suitable bays with working doors: fighters fit in fighter or small craft bays, small craft only in small craft
     * bays.</p>
     *
     * <p>Either way, a unit either drags (or tugs) salvage or carries it; never both. A unit that drags salvage, or
     * that teams up with another unit to recover a wreck, is committed to that one wreck.</p>
     *
     * @return {@code true} if a carrying unit may recover several wrecks
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isMultipleSalvagePerUnitAllowed() {
        return false;
    }

    /**
     * Checks whether the player is paid for salvage, with the option to buy units from the employer.
     *
     * <p>Under CamOps rules, the contract's salvage rights cap how much of the salvage the player may claim. When
     * salvage purchases are used instead, there is no cap: the salvage rights set the player's share of each wreck's
     * value. For each recovered wreck, the player either receives their share in cash, or buys the unit by paying the
     * employer's share. Under salvage exchange rights, the player only receives the cash share and can't buy units.</p>
     *
     * @return {@code true} if salvage purchases are used
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isUseSalvagePurchases() {
        return false;
    }
}
