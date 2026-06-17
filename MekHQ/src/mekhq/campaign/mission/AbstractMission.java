/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.mission;

import static java.lang.Math.ceil;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import org.apache.commons.lang3.NotImplementedException;
import org.w3c.dom.Node;

public class AbstractMission {
    private static final MMLogger LOGGER = MMLogger.create(AbstractMission.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AbstractMission";

    private String name;
    private int id = -1;
    private MissionStatus status = MissionStatus.ACTIVE;
    private String type;
    private String description;

    private String systemId;
    private String legacyPlanetName;
    /*
     * This is a transient variable meant to keep track of a single jump path while the contract runs through initial
     * calculations, as the same jump path is referenced multiple times and calculating it each time is expensive. No
     * need to preserve it in save data.
     */
    private transient JumpPath cachedJumpPath;

    private LocalDate startDate;
    private LocalDate endDate;
    private int lengthInMonths;

    private String employer;

    private double paymentMultiplier;
    private ContractCommandRights commandRights;
    private int overheadCompensation;
    private int straightSupport;
    private int battleLossCompensation;
    private int salvagePercent;
    private int transportCompensation;

    // need to keep track of total value salvaged for salvage rights
    private boolean salvageExchange;
    private Money salvagedByUnit = Money.zero();
    private Money salvagedByEmployer = Money.zero();

    // actual amounts
    private Money advanceAmount = Money.zero();
    private Money signingBonusAmount = Money.zero();
    private Money transportAmount = Money.zero();
    private Money transitAmount = Money.zero();
    private Money overheadAmount = Money.zero();
    private Money supportAmount = Money.zero();
    private Money baseAmount = Money.zero();
    private Money feeAmount = Money.zero();

    private boolean paidMRBCFee;
    private int mrbcFeePercent = MRBC_FEE_PERCENTAGE;
    private int sharesPercent = DEFAULT_SHARES_PERCENT;
    private int advancePercent;
    private int signingBonus;

    private int hospitalBedsRented;
    private int kitchensRented;
    private int holdingCellsRented;

    private final List<Scenario> scenarios = new ArrayList<>();

    public final static int MRBC_FEE_PERCENTAGE = 5;
    public final static int DEFAULT_SHARES_PERCENT = 30;

    public final static int OH_NONE = 0;
    public final static int OH_HALF = 1;
    public final static int OH_FULL = 2;
    public final static int OH_NUM = 3;

    public AbstractMission() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this object as an HTML hyperlink.
     *
     * <p>The hyperlink is formatted with a "MISSION:" protocol prefix followed by the object's ID. This allows UI
     * components that support HTML to render the name as a clickable link, which can be used to navigate to or focus on
     * this specific object when clicked.</p>
     *
     * @return An HTML formatted string containing the object's name as a hyperlink with its ID
     *
     * @author Illiani
     * @since 0.50.05
     */
    public String getHyperlinkedName() {
        return String.format("<a href='MISSION:%s'>%s</a>", getId(), getName());
    }


    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public PlanetarySystem getSystem() {
        return Systems.getInstance().getSystemById(getSystemId());
    }

    /**
     * Convenience property to return the name of the current planet. Sometimes, the "current planet" doesn't match up
     * with an existing planet in our planet database, in which case we return whatever was stored.
     */
    public String getSystemName(LocalDate when) {
        if (getSystem() == null) {
            return legacyPlanetName;
        }

        return getSystem().getName(when);
    }

    public @Nullable JumpPath getCachedJumpPath() {
        return cachedJumpPath;
    }

    public void setCachedJumpPath(JumpPath cachedJumpPath) {
        this.cachedJumpPath = cachedJumpPath;
    }

    /**
     * Gets the currently calculated jump path for this contract, only recalculating if it's not valid any longer or
     * hasn't been calculated yet.
     */
    public @Nullable JumpPath getJumpPath(Campaign campaign) {
        // if we don't have a cached jump path, or if the jump path's starting/ending point no longer match the
        // campaign's current location or contract's destination
        if ((getCachedJumpPath() == null) || getCachedJumpPath().isEmpty()
                  || !getCachedJumpPath().getFirstSystem().equals(campaign.getCurrentSystem())
                  || !getCachedJumpPath().getLastSystem().equals(getSystem())) {
            setCachedJumpPath(campaign.calculateJumpPath(campaign.getCurrentSystem(), getSystem()));
        }

        return getCachedJumpPath();
    }

    public MissionStatus getStatus() {
        return status;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
    }

    public boolean isActiveOn(LocalDate date) {
        return isActiveOn(date, false);
    }

    public boolean isActiveOn(LocalDate date, boolean excludeEndDateCheck) {
        return getStatus().isActive();
    }

    /**
     * Returns the contract length in months.
     *
     * @return the number and corresponding length of the contract in months as an integer
     */
    public int getLengthInMonths() {
        return lengthInMonths;
    }

    public void setLengthInMonths(int lengthInMonths) {
        this.lengthInMonths = lengthInMonths;
    }

    /**
     * Calculates the number of days required for travel based on the current campaign state.
     *
     * <p>This method determines if a valid destination system is set, computes if the command circuit should be used,
     * retrieves the jump path, and totals the travel time (including recharge, start, and end times). The result is
     * rounded to two decimal places and then to the nearest whole day.</p>
     *
     * @param campaign the {@link Campaign} instance containing context such as date, location, and command circuit
     *                 options
     *
     * @return the total number of travel days required; returns 0 if there is no valid system to travel to
     */
    public int getTravelDays(Campaign campaign) {
        if (null != this.getSystem()) {
            boolean isUseCommandCircuit =
                  FactionStandingUtilities.isUseCommandCircuit(campaign.isOverridingCommandCircuitRequirements(),
                        campaign.isGM(),
                        campaign.getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
                        campaign.getFactionStandings(), campaign.getFutureAtBContracts());

            JumpPath jumpPath = getJumpPath(campaign);
            if (jumpPath == null) {
                return 0;
            }

            double days = Math.round(jumpPath.getTotalTime(campaign.getLocalDate(),
                  campaign.getCurrentLocation().getTransitTime(), isUseCommandCircuit) * 100.0)
                                / 100.0;
            return (int) ceil(days);
        }
        return 0;
    }

    /**
     * @param campaign campaign loaded
     *
     * @return the approximate number of months for a 2-way trip + deployment, rounded up
     */
    public int getLengthPlusTravel(Campaign campaign) {
        int travelMonths = (int) ceil(2 * getTravelDays(campaign) / 30.0);
        return getLengthInMonths() + travelMonths;
    }

    /**
     * Get the number of months left in this contract after the given date. Partial months are counted as full months.
     *
     * @param date the date to use in the calculation
     *
     * @return the number of months left
     */
    public int getMonthsLeft(LocalDate date) {
        int monthsLeft = Math.toIntExact(ChronoUnit.MONTHS.between(date, getEndingDate()));
        // Ensure partial months are counted based on the current day of the month, as
        // the above only
        // counts full months
        if (date.getDayOfMonth() != getEndingDate().getDayOfMonth()) {
            monthsLeft++;
        }
        return monthsLeft;
    }

    public LocalDate getEndingDate() {
        return endDate;
    }

    public void setEndingDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * This sets the Start Date and End Date of the Contract based on the length of the contract and the starting date
     * provided
     *
     * @param startDate the date the contract starts at
     */
    public void setStartAndEndDate(LocalDate startDate) {
        this.startDate = startDate;
        this.endDate = startDate.plusMonths(getLengthInMonths());
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    /**
     * Don't use this method directly as it will not add an id to the added scenario. Use Campaign#AddScenario instead
     *
     * @param scenario the scenario to add this mission
     */
    public void addScenario(final Scenario scenario) {
        scenario.setMissionId(getId());
        getScenarios().add(scenario);
    }

    public List<Scenario> getVisibleScenarios() {
        return getScenarios().stream().filter(scenario -> !scenario.isCloaked()).collect(Collectors.toList());
    }

    public List<Scenario> getCurrentScenarios() {
        return getScenarios().stream()
                     .filter(scenario -> scenario.getStatus().isCurrent())
                     .collect(Collectors.toList());
    }

    public List<AtBScenario> getCurrentAtBScenarios() {
        return getScenarios().stream()
                     .filter(scenario -> scenario.getStatus().isCurrent() && (scenario instanceof AtBScenario))
                     .map(scenario -> (AtBScenario) scenario)
                     .collect(Collectors.toList());
    }

    public List<Scenario> getCompletedScenarios() {
        return getScenarios().stream()
                     .filter(scenario -> !scenario.getStatus().isCurrent())
                     .collect(Collectors.toList());
    }

    public void clearScenarios() {
        scenarios.clear();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLegacyPlanetName() {
        return legacyPlanetName;
    }

    public void setLegacyPlanetName(String legacyPlanetName) {
        this.legacyPlanetName = legacyPlanetName;
    }

    public int getSalvagePercent() {
        return salvagePercent;
    }

    public void setSalvagePercent(int salvagePercent) {
        this.salvagePercent = salvagePercent;
    }

    public String getSalvagePercentString() {
        return getSalvagePercent() + "%";
    }

    public boolean canSalvage() {
        return getSalvagePercent() > 0;
    }

    public double getPaymentMultiplier() {
        return paymentMultiplier;
    }

    public void setPaymentMultiplier(double paymentMultiplier) {
        this.paymentMultiplier = paymentMultiplier;
    }

    public ContractCommandRights getCommandRights() {
        return commandRights;
    }

    public void setCommandRights(ContractCommandRights commandRights) {
        this.commandRights = commandRights;
    }

    public int getOverheadCompensation() {
        return overheadCompensation;
    }

    public void setOverheadCompensation(int overheadCompensation) {
        this.overheadCompensation = overheadCompensation;
    }

    public int getStraightSupport() {
        return straightSupport;
    }

    public void setStraightSupport(int straightSupport) {
        this.straightSupport = Math.clamp(straightSupport, 0, 100);
    }

    public String getStraightSupportString() {
        return getStraightSupport() + "%";
    }

    public int getBattleLossCompensation() {
        return battleLossCompensation;
    }

    public void setBattleLossCompensation(int battleLossCompensation) {
        this.battleLossCompensation = Math.clamp(battleLossCompensation, 0, 100);
    }

    public String getBattleLossCompString() {
        return getBattleLossCompensation() + "%";
    }

    public boolean isSalvageExchange() {
        return salvageExchange;
    }

    public void setSalvageExchange(boolean salvageExchange) {
        this.salvageExchange = salvageExchange;
    }

    public int getTransportCompensation() {
        return transportCompensation;
    }

    public void setTransportCompensation(int transportCompensation) {
        this.transportCompensation = transportCompensation;
    }

    public String getTransportCompString() {
        return getTransportCompensation() + "%";
    }

    public Money getOverheadAmount() {
        return overheadAmount;
    }

    public void setOverheadAmount(Money overheadAmount) {
        this.overheadAmount = overheadAmount;
    }

    public static String getOverheadCompensationName(int i) {
        return switch (i) {
            case OH_NONE -> "None";
            case OH_HALF -> "Half";
            case OH_FULL -> "Full";
            default -> "?";
        };
    }

    public Money getSalvagedByUnit() {
        return salvagedByUnit;
    }

    public void setSalvagedByUnit(Money salvagedByUnit) {
        this.salvagedByUnit = salvagedByUnit;
    }

    public void addSalvageByUnit(Money money) {
        salvagedByUnit = salvagedByUnit.plus(money);
    }

    public void subtractSalvageByUnit(Money money) {
        salvagedByUnit = salvagedByUnit.minus(money);
    }

    public Money getSalvagedByEmployer() {
        return salvagedByEmployer;
    }

    public void setSalvagedByEmployer(Money salvagedByEmployer) {
        this.salvagedByEmployer = salvagedByEmployer;
    }

    public void addSalvageByEmployer(Money money) {
        salvagedByEmployer = salvagedByEmployer.plus(money);
    }

    /**
     * Computes the player's share of the total salvage value as an integer percentage, using
     * {@link RoundingMode#CEILING} (i.e. any fractional percentage rounds up to the next whole percent).
     *
     * <p>Rounding up is intentional from a gameplay standpoint: the percentage is compared against the contract's
     * salvage cap, and a true value of e.g. 50.001% against a 50% cap is a breach and must be surfaced as such. It also
     * fixes the truncation artifacts that previously could cause the displayed value to shift by a full percentage
     * point after a small change to the salvage assignment (see issue #5683).</p>
     *
     * @param playerShare   the salvage value assigned to the player (mercs)
     * @param employerShare the salvage value assigned to the employer
     *
     * @return integer percentage in the range {@code [0, 100]}, or {@code 0} if there is no salvage to split
     */
    public static int calculateSalvagePercentage(Money playerShare, Money employerShare) {
        Money total = playerShare.plus(employerShare);
        if (!total.isPositive()) {
            return 0;
        }
        return playerShare.multipliedBy(100)
                     .getAmount()
                     .divide(total.getAmount(), 0, RoundingMode.CEILING)
                     .intValue();
    }


    /**
     * Convenience overload that computes the current salvage percentage from the values stored on this contract.
     *
     * @return integer percentage in the range {@code [0, 100]}, or {@code 0} if there is no salvage to split
     */
    public int getCurrentSalvagePct() {
        return calculateSalvagePercentage(getSalvagedByUnit(), getSalvagedByEmployer());
    }

    public Money getAdvanceAmount() {
        return advanceAmount;
    }

    public void setAdvanceAmount(Money advanceAmount) {
        this.advanceAmount = advanceAmount;
    }

    /**
     * @return total amount that will be paid on contract acceptance.
     */
    public Money getTotalAdvanceAmount() {
        return advanceAmount.plus(getSigningBonusAmount());
    }

    public Money getSigningBonusAmount() {
        return signingBonusAmount;
    }

    public void setSigningBonusAmount(Money signingBonusAmount) {
        this.signingBonusAmount = signingBonusAmount;
    }

    public Money getTransportAmount() {
        return transportAmount;
    }

    public void setTransportAmount(Money transportAmount) {
        this.transportAmount = transportAmount;
    }

    public Money getTransitAmount() {
        return transitAmount;
    }

    public void setTransitAmount(Money transitAmount) {
        this.transitAmount = transitAmount;
    }

    public Money getSupportAmount() {
        return supportAmount;
    }

    public void setSupportAmount(Money supportAmount) {
        this.supportAmount = supportAmount;
    }

    public Money getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(Money baseAmount) {
        this.baseAmount = baseAmount;
    }

    public Money getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(Money feeAmount) {
        this.feeAmount = feeAmount;
    }

    public boolean isPaidMRBCFee() {
        return paidMRBCFee;
    }

    public void setPaidMRBCFee(boolean paidMRBCFee) {
        this.paidMRBCFee = paidMRBCFee;
    }

    public int getMRBCFeePercentage() {
        return mrbcFeePercent;
    }

    public void setMRBCFeePercentage(int mrbcFeePercent) {
        this.mrbcFeePercent = mrbcFeePercent;
    }

    /**
     * Retrieves the percentage of shares for this contract. This currently returns a default value of 30.
     *
     * @return the percentage of shares
     */
    public int getSharesPercent() {
        return sharesPercent;
    }

    public void setSharesPercent(int sharesPercent) {
        this.sharesPercent = sharesPercent;
    }

    public int getAdvancePercent() {
        return advancePercent;
    }

    public void setAdvancePercent(int advancePercent) {
        this.advancePercent = advancePercent;
    }

    public int getSigningBonus() {
        return signingBonus;
    }

    public void setSigningBonus(int signingBonus) {
        this.signingBonus = signingBonus;
    }

    public Money getTotalAmountPlusFeesAndBonuses() {
        return getTotalAmountPlusFees().plus(getSigningBonusAmount());
    }

    public Money getTotalAmountPlusFees() {
        return getTotalAmount().minus(getFeeAmount());
    }

    public Money getTotalAmount() {
        return getBaseAmount()
                     .plus(getSupportAmount())
                     .plus(getOverheadAmount())
                     .plus(getTransportAmount())
                     .plus(getTransitAmount());
    }

    public Money getMonthlyPayOut() {
        if (getLengthInMonths() <= 0) {
            return Money.zero();
        }

        return getTotalAmountPlusFeesAndBonuses()
                     .minus(getTotalAdvanceAmount())
                     .dividedBy(getLengthInMonths());
    }

    /**
     * @param campaign campaign loaded
     *
     * @return the cumulative sum the estimated monthly incomes - expenses
     */
    public Money getTotalMonthlyPayOut(Campaign campaign) {
        return getMonthlyPayOut()
                     .multipliedBy(getLengthInMonths())
                     .minus(getTotalEstimatedOverheadExpenses(campaign))
                     .minus(getTotalEstimatedMaintenanceExpenses(campaign))
                     .minus(getTotalEstimatedPayrollExpenses(campaign));
    }

    /**
     * @param campaign campaign loaded
     *
     * @return the cumulative sum of estimated overhead expenses for the duration of travel + deployment
     */
    public Money getTotalEstimatedOverheadExpenses(Campaign campaign) {
        return campaign.getAccountant().getOverheadExpenses().multipliedBy(getLengthPlusTravel(campaign));
    }

    /**
     * @param campaign campaign loaded
     *
     * @return the cumulative sum of estimated maintenance expenses for the duration of travel + deployment
     */
    public Money getTotalEstimatedMaintenanceExpenses(Campaign campaign) {
        return campaign.getAccountant().getMaintenanceCosts().multipliedBy(getLengthPlusTravel(campaign));
    }

    /**
     * @param campaign campaign loaded
     *
     * @return the estimated payroll expenses for one month
     */
    public Money getEstimatedPayrollExpenses(Campaign campaign) {
        Accountant accountant = campaign.getAccountant();
        if (campaign.getCampaignOptions().isUsePeacetimeCost()) {
            return accountant.getPeacetimeCost();
        } else {
            return accountant.getPayRoll();
        }
    }

    /**
     * @param campaign campaign loaded
     *
     * @return the cumulative sum of estimated payroll expenses for the duration of travel + deployment
     */
    public Money getTotalEstimatedPayrollExpenses(Campaign campaign) {
        return getEstimatedPayrollExpenses(campaign).multipliedBy(getLengthPlusTravel(campaign));
    }

    /**
     * @param campaign campaign loaded
     *
     * @return the total (2-way) estimated transportation fee from the player's current location to this contract's
     *       planet
     */
    public Money getTotalTransportationFees(Campaign campaign) {
        if ((null != getSystem()) && campaign.getCampaignOptions().isPayForTransport()) {
            return getTransportCost(campaign, false);
        }

        return Money.zero();
    }

    /**
     * Calculates the employer's transport reimbursement based on the contract's transport compensation percentage.
     *
     * <p>This represents the amount the employer pays toward your transport costs. For example, if transport
     * compensation is 50% and the full transport cost is 1,000,000 C-bills, the employer reimburses you 500,000.</p>
     *
     * @param campaign the current {@link Campaign} used for transport cost calculation
     *
     * @return the {@link Money} amount the employer reimburses for transport
     */
    public Money getEmployerTransportReimbursement(Campaign campaign) {
        if ((null == getSystem()) || !campaign.getCampaignOptions().isPayForTransport()) {
            return Money.zero();
        }

        Money fullTransportCost = getTransportCost(campaign, false);
        return fullTransportCost.multipliedBy(getTransportCompensation() / 100.0);
    }

    /**
     * Calculates the player's out-of-pocket transport cost after the employer's reimbursement.
     *
     * <p>This is the full transport cost minus the employer's reimbursement. For example, if transport compensation
     * is 50% and the full transport cost is 1,000,000 C-bills, the player pays 500,000.</p>
     *
     * @param campaign the current {@link Campaign} used for transport cost calculation
     *
     * @return the {@link Money} amount the player pays for transport after employer reimbursement
     */
    public Money getPlayerTransportCost(Campaign campaign) {
        if ((null == getSystem()) || !campaign.getCampaignOptions().isPayForTransport()) {
            return Money.zero();
        }

        Money fullTransportCost = getTransportCost(campaign, false);
        Money employerReimbursement = getEmployerTransportReimbursement(campaign);
        return fullTransportCost.minus(employerReimbursement);
    }

    /**
     * Calculates the total transport cost for this contract based on the campaign's transport settings and the
     * contract's jump path.
     *
     * <p>The calculation considers the following factors:</p>
     * <ul>
     *   <li>The jump path duration, including any command circuit adjustments</li>
     *   <li>The campaign's transport cost tables (using the Regular experience level)</li>
     *   <li>Whether the employer pays for a round trip (two-way pay)</li>
     *   <li>Whether transport compensation should be applied to reduce the final cost</li>
     * </ul>
     * <p>When {@code includeTransportCompensation} is true, the method calculates the employer's compensation
     * percentage and subtracts it from the final transport cost.</p>
     *
     * @param campaign                     the current {@link Campaign} used for jump path, transport options, and cost
     *                                     calculation
     * @param includeTransportCompensation whether to apply the contract's transport compensation percentage to reduce
     *                                     the cost
     *
     * @return the total {@link Money} required for transport, after applying all applicable modifiers
     */
    private Money getTransportCost(Campaign campaign, boolean includeTransportCompensation) {
        JumpPath jumpPath = getJumpPath(campaign);
        if (jumpPath == null) {
            return Money.zero();
        }

        TransportCostCalculations transportCostCalculations = campaign.getTransportCostCalculation(EXP_REGULAR);
        boolean useTwoWayPay = campaign.getCampaignOptions().isUseTwoWayPay();
        boolean isUseCommandCircuits = campaign.isUseCommandCircuitForContract(this);
        int duration = (int) ceil(jumpPath.getTotalTime(campaign.getLocalDate(),
              campaign.getCurrentLocation().getTransitTime(), isUseCommandCircuits));
        Money transportCost = transportCostCalculations.calculateJumpCostForEntireJourney(duration,
              jumpPath.getJumps());

        // Is the employer paying for both ways?
        transportCost = transportCost.multipliedBy(useTwoWayPay ? 2 : 1);

        if (includeTransportCompensation) {
            Money transportCompensation = transportCost.multipliedBy(getTransportCompensation() / 100.0);
            transportCost = transportCost.minus(transportCompensation);
        }

        return transportCost;
    }

    /**
     * Get the estimated total profit for this contract. The total profit is the total contract payment including fees
     * and bonuses, minus overhead, maintenance, payroll, spare parts, and other monthly expenses. The duration used for
     * monthly expenses is the contract duration plus the travel time from the unit's current world to the contract
     * world and back.
     *
     * <p>Transport costs are handled as follows: the employer's transport reimbursement is included in the contract
     * income (via {@link #getTotalAmount()}), and the full transport cost is subtracted here. The net effect is that
     * profit is reduced by the player's out-of-pocket transport cost (full cost minus employer reimbursement).</p>
     *
     * @param campaign The campaign with which this contract is associated.
     *
     * @return The estimated profit in the current default currency.
     */
    public Money getEstimatedTotalProfit(Campaign campaign) {
        return getTotalAdvanceAmount()
                     .plus(getTotalMonthlyPayOut(campaign))
                     .minus(getTotalTransportationFees(campaign));
    }

    /**
     * Only do this at the time the contract is set up, otherwise amounts may change after the ink is signed, which is a
     * no-no.
     *
     * @param campaign current campaign
     */
    public void calculateContract(Campaign campaign) {
    }

    public int getHospitalBedsRented() {
        return hospitalBedsRented;
    }

    public void setHospitalBedsRented(int hospitalBedsRented) {
        this.hospitalBedsRented = hospitalBedsRented;
    }

    public int getKitchensRented() {
        return kitchensRented;
    }

    public void setKitchensRented(int kitchensRented) {
        this.kitchensRented = kitchensRented;
    }

    public int getHoldingCellsRented() {
        return holdingCellsRented;
    }

    public void setHoldingCellsRented(int holdingCellsRented) {
        this.holdingCellsRented = holdingCellsRented;
    }

    /**
     * Returns the default repair location constant for the unit.
     *
     * @return the repair location constant {@code Unit.SITE_FACILITY_BASIC}
     */
    public int getRepairLocation() {
        return Unit.SITE_FACILITY_BASIC;
    }

    public void writeToXML(Campaign campaign, final PrintWriter pw, int indent) {
        NotImplementedException error = new NotImplementedException();
        LOGGER.error(error);
    }

    protected int writeToXMLBegin(Campaign campaign, final PrintWriter pw, int indent) {
        NotImplementedException error = new NotImplementedException();
        LOGGER.error(error);
        return indent;
    }

    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        NotImplementedException error = new NotImplementedException();
        LOGGER.error(error);
    }

    public void loadFieldsFromXmlNode(Campaign campaign, Version version, Node wn) throws ParseException {
        NotImplementedException error = new NotImplementedException();
        LOGGER.error(error);
    }

    public static AbstractMission generateInstanceFromXML(Node node, Campaign campaign, Version version) {
        NotImplementedException error = new NotImplementedException();
        LOGGER.error(error);

        return new AbstractMission();
    }

    /**
     * Calculations to be performed once the contract has been accepted.
     */
    public void acceptContract(Campaign campaign) {

    }

    @Override
    public String toString() {
        return !getStatus().isCompleted() ?
                     getName() :
                     getFormattedTextAt(RESOURCE_BUNDLE, "AbstractMission.name.completed", getName());
    }
}
