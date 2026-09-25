package mekhq.campaign.mission.scenarios.salvage;

import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;

/**
 * Decides how a contract's salvage is divided between the player and their employer, and what money changes hands.
 *
 * <p>There are three kinds of settlement:</p>
 * <ul>
 *   <li><b>Capped</b> (see {@link CappedSalvageSettlement}): the salvage rights cap how much of the salvage the
 *   player may claim, by value. Claimed salvage can be kept or sold.</li>
 *   <li><b>Exchange</b> (see {@link ExchangeSalvageSettlement}): all salvage goes to the employer, and the player is
 *   paid their share of its value in cash.</li>
 *   <li><b>Purchase</b> (see {@link PurchaseSalvageSettlement}): there is no cap. For each wreck, the player is paid
 *   their share of its value in cash, unless they buy the unit by paying the employer their share.</li>
 * </ul>
 *
 * <p>Salvage is always valued at its sell value, not its purchase value (Draconis Reach, 1st printing, p. 32).</p>
 *
 * <p>Settlements are created by the campaign's salvage system (see
 * {@link AbstractSalvage#createSettlement(AbstractContract)}).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract sealed class SalvageSettlement
      permits CappedSalvageSettlement, ExchangeSalvageSettlement, PurchaseSalvageSettlement {
    static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    private final double playerShare;

    /**
     * @param playerShare the player's share of the salvage under the contract's salvage rights; clamped to between 0
     *                    and 1
     */
    SalvageSettlement(double playerShare) {
        this.playerShare = Math.clamp(playerShare, 0.0, 1.0);
    }

    /**
     * Creates the settlement used by Campaign Operations: the salvage rights cap how much salvage the player may
     * claim, unless the contract has salvage exchange rights.
     *
     * @param contract the contract the salvage was recovered under
     *
     * @return a capped or exchange settlement
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static SalvageSettlement forCappedSalvageRights(AbstractContract contract) {
        double playerShare = contract.getSalvageRightsMultiplier();
        return contract.isSalvageExchange() ?
                     new ExchangeSalvageSettlement(playerShare) :
                     new CappedSalvageSettlement(playerShare);
    }

    /**
     * Creates the settlement used under salvage purchases: the salvage rights set the player's share of each wreck,
     * which they can use to buy the unit. Under salvage exchange rights, the player can't buy units, and only
     * receives their cash share.
     *
     * @param contract the contract the salvage was recovered under
     *
     * @return a purchase or exchange settlement
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static SalvageSettlement forSalvagePurchases(AbstractContract contract) {
        double playerShare = contract.getSalvageRightsMultiplier();
        return contract.isSalvageExchange() ?
                     new ExchangeSalvageSettlement(playerShare) :
                     new PurchaseSalvageSettlement(playerShare);
    }

    /**
     * Gets the player's share of the salvage under the contract's salvage rights.
     *
     * @return the player's share, from 0 to 1
     *
     * @author Illiani
     * @since 0.51.01
     */
    public double getPlayerShare() {
        return playerShare;
    }

    /**
     * Gets the player's share of the salvage under the contract's salvage rights, as a percentage.
     *
     * @return the player's share, from 0 to 100
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getPlayerSharePercent() {
        return (int) Math.round(playerShare * 100);
    }

    /**
     * Checks whether the player's share caps how much of the salvage they may claim.
     *
     * @return {@code true} if the player may only claim salvage up to their share
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isSalvageCapped() {
        return false;
    }

    /**
     * Checks whether the player may keep a recovered wreck, either by claiming it or by buying it.
     *
     * @return {@code true} if the player may keep salvage
     *
     * @author Illiani
     * @since 0.51.01
     */
    public abstract boolean canKeepSalvage();

    /**
     * Checks whether the player may claim a recovered wreck to sell.
     *
     * @return {@code true} if the player may sell salvage
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean canSellSalvage() {
        return false;
    }

    /**
     * Checks whether the player keeps salvage by buying it from their employer.
     *
     * @return {@code true} if salvage the player keeps must be bought
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isKeptSalvageBought() {
        return false;
    }

    /**
     * Gets what the player pays their employer to keep salvage.
     *
     * @param keptSalvageValue the total value of the salvage the player keeps
     *
     * @return the purchase cost
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Money getPurchaseCost(Money keptSalvageValue) {
        return Money.zero();
    }

    /**
     * Gets the cash the player is paid as their share of the salvage going to their employer.
     *
     * @param employerSalvageValue the total value of the salvage going to the employer
     *
     * @return the player's cash share
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Money getCashShare(Money employerSalvageValue) {
        return Money.zero();
    }

    /**
     * Checks whether the player can afford the salvage they've chosen to keep.
     *
     * @param keptSalvageValue the total value of the salvage the player keeps
     * @param availableFunds   the player's funds
     *
     * @return {@code true} if the player can afford their salvage
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean canAfford(Money keptSalvageValue, Money availableFunds) {
        return getPurchaseCost(keptSalvageValue).compareTo(availableFunds) <= 0;
    }

    /**
     * Charges the player for the salvage they keep, if it must be bought.
     *
     * @param campaign         the current campaign
     * @param scenario         the scenario the salvage came from
     * @param keptSalvageValue the total value of the salvage the player keeps
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void chargePurchases(Campaign campaign, Scenario scenario, Money keptSalvageValue) {
        Money purchaseCost = getPurchaseCost(keptSalvageValue);
        if (!purchaseCost.isPositive()) {
            return;
        }

        campaign.getPlayerForce().getFinances().debit(TransactionType.UNIT_PURCHASE, campaign.getLocalDate(),
              purchaseCost, getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.purchase", scenario.getName()));
        campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.purchase.report",
              purchaseCost.toAmountString(), scenario.getHyperlinkedName()));
    }

    /**
     * Pays the player their cash share of the salvage going to their employer.
     *
     * @param campaign             the current campaign
     * @param scenario             the scenario the salvage came from
     * @param employerSalvageValue the total value of the salvage going to the employer
     *
     * @return the cash share paid to the player
     *
     * @author Illiani
     * @since 0.51.01
     */
    public Money payCashShare(Campaign campaign, Scenario scenario, Money employerSalvageValue) {
        Money cashShare = getCashShare(employerSalvageValue);
        if (!cashShare.isPositive()) {
            return cashShare;
        }

        String reasonKey = getCashShareReasonKey();
        campaign.getPlayerForce().getFinances().credit(getCashShareTransactionType(), campaign.getLocalDate(),
              cashShare, getFormattedTextAt(RESOURCE_BUNDLE, reasonKey, scenario.getName()));
        campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE, reasonKey + ".report",
              cashShare.toAmountString(), scenario.getHyperlinkedName()));
        return cashShare;
    }

    /**
     * @return the transaction type used when paying the player their cash share
     */
    TransactionType getCashShareTransactionType() {
        return TransactionType.SALVAGE;
    }

    /**
     * @return the resource key of the transaction description used when paying the player their cash share; the
     *       daily report uses the same key with {@code .report} appended
     */
    String getCashShareReasonKey() {
        return "CamOpsSalvageUtilities.share";
    }
}
