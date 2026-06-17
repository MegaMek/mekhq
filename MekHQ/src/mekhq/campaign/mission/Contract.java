/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contracts - we need to track static amounts here because changes in the underlying campaign don't change the figures
 * once the ink is dry
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Contract extends Mission {
    private static final MMLogger logger = MMLogger.create(Contract.class);

    // this is a transient variable meant to keep track of a single jump path while
    // the contract
    // runs through initial calculations, as the same jump path is referenced
    // multiple times
    // and calculating it each time is expensive. No need to preserve it in save
    // date.
    private transient JumpPath cachedJumpPath;

    public Contract() {
        this(null, null);
    }

    public Contract(String name, String employer) {
        super(name);
        setEmployer(employer);

        setLengthInMonths(12);
        setPaymentMultiplier(2.0);
        setCommandRights(ContractCommandRights.HOUSE);
        setOverheadCompensation(OH_NONE);
        setStraightSupport(50);
        setBattleLossCompensation(50);
        setSalvagePercent(50);
        setSalvageExchange(false);
        setTransportCompensation(50);
        setMRBCFee(true);
        setAdvancePercent(25);
        setSigningBonus(0);
        setHospitalBedsRented(0);
        setKitchensRented(0);
        setHoldingCellsRented(0);
    }

    @Override
    public void setSystemId(String n) {
        super.setSystemId(n);
        setCachedJumpPath(null);
    }

    @Override
    public boolean isActiveOn(LocalDate date, boolean excludeEndDateCheck) {
        return super.isActiveOn(date, excludeEndDateCheck) && !date.isBefore(getStartDate())
                     && (excludeEndDateCheck || !date.isAfter(getEndingDate()));
    }

    /**
     * Gets the currently calculated jump path for this contract, only recalculating if it's not valid any longer or
     * hasn't been calculated yet.
     */
    public @Nullable JumpPath getJumpPath(Campaign c) {
        // if we don't have a cached jump path, or if the jump path's starting/ending
        // point
        // no longer match the campaign's current location or contract's destination
        if ((getCachedJumpPath() == null) || getCachedJumpPath().isEmpty()
                  || !getCachedJumpPath().getFirstSystem().equals(c.getCurrentSystem())
                  || !getCachedJumpPath().getLastSystem().equals(getSystem())) {
            setCachedJumpPath(c.calculateJumpPath(c.getCurrentSystem(), getSystem()));
        }

        return getCachedJumpPath();
    }

    public @Nullable JumpPath getCachedJumpPath() {
        return cachedJumpPath;
    }

    public void setCachedJumpPath(final @Nullable JumpPath cachedJumpPath) {
        this.cachedJumpPath = cachedJumpPath;
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
     * @param c campaign loaded
     *
     * @return the cumulative sum the estimated monthly incomes - expenses
     */
    public Money getTotalMonthlyPayOut(Campaign c) {
        return getMonthlyPayOut()
                     .multipliedBy(getLengthInMonths())
                     .minus(getTotalEstimatedOverheadExpenses(c))
                     .minus(getTotalEstimatedMaintenanceExpenses(c))
                     .minus(getTotalEstimatedPayrollExpenses(c));
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
            double days = Math.round(jumpPath.getTotalTime(campaign.getLocalDate(),
                  campaign.getCurrentLocation().getTransitTime(), isUseCommandCircuit) * 100.0)
                                / 100.0;
            return (int) ceil(days);
        }
        return 0;
    }

    /**
     * @param c campaign loaded
     *
     * @return the approximate number of months for a 2-way trip + deployment, rounded up
     */
    public int getLengthPlusTravel(Campaign c) {
        int travelMonths = (int) ceil(2 * getTravelDays(c) / 30.0);
        return getLengthInMonths() + travelMonths;
    }

    /**
     * @param c campaign loaded
     *
     * @return the cumulative sum of estimated overhead expenses for the duration of travel + deployment
     */
    public Money getTotalEstimatedOverheadExpenses(Campaign c) {
        return c.getAccountant().getOverheadExpenses().multipliedBy(getLengthPlusTravel(c));
    }

    /**
     * @param c campaign loaded
     *
     * @return the cumulative sum of estimated maintenance expenses for the duration of travel + deployment
     */
    public Money getTotalEstimatedMaintenanceExpenses(Campaign c) {
        return c.getAccountant().getMaintenanceCosts().multipliedBy(getLengthPlusTravel(c));
    }

    /**
     * @param c campaign loaded
     *
     * @return the estimated payroll expenses for one month
     */
    public Money getEstimatedPayrollExpenses(Campaign c) {
        Accountant accountant = c.getAccountant();
        if (c.getCampaignOptions().isUsePeacetimeCost()) {
            return accountant.getPeacetimeCost();
        } else {
            return accountant.getPayRoll();
        }
    }

    /**
     * @param c campaign loaded
     *
     * @return the cumulative sum of estimated payroll expenses for the duration of travel + deployment
     */
    public Money getTotalEstimatedPayrollExpenses(Campaign c) {
        return getEstimatedPayrollExpenses(c).multipliedBy(getLengthPlusTravel(c));
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

    /**
     * Calculations to be performed once the contract has been accepted.
     */
    public void acceptContract(Campaign campaign) {

    }

    /**
     * Only do this at the time the contract is set up, otherwise amounts may change after the ink is signed, which is a
     * no-no.
     *
     * @param campaign current campaign
     */
    public void calculateContract(Campaign campaign) {
        Accountant accountant = campaign.getAccountant();

        // calculate base amount
        setBaseAmount(accountant.getContractBase()
                            .multipliedBy(getLengthInMonths())
                            .multipliedBy(getPaymentMultiplier()));

        // calculate overhead
        switch (getOverheadCompensation()) {
            case OH_HALF:
                setOverheadAmount(accountant.getOverheadExpenses()
                                        .multipliedBy(getLengthInMonths())
                                        .multipliedBy(0.5));
                break;
            case OH_FULL:
                setOverheadAmount(accountant.getOverheadExpenses().multipliedBy(getLengthInMonths()));
                break;
            default:
                setOverheadAmount(Money.zero());
        }

        // calculate support amount
        if (campaign.getCampaignOptions().isUsePeacetimeCost()) {
            setSupportAmount(accountant.getPeacetimeCost()
                                   .multipliedBy(getLengthInMonths())
                                   .multipliedBy(getStraightSupport())
                                   .dividedBy(100));
        } else {
            Money maintCosts = campaign.getAllHangar().getUnitCosts(u -> !u.isConventionalInfantry(),
                  Unit::getWeeklyMaintenanceCost);
            maintCosts = maintCosts.multipliedBy(4);
            setSupportAmount(maintCosts
                                   .multipliedBy(getLengthInMonths())
                                   .multipliedBy(getStraightSupport())
                                   .dividedBy(100));
        }

        // calculate employer's transport reimbursement (this is income - what they pay you toward transport)
        // The full transport cost will be subtracted in getEstimatedTotalProfit()
        if (null != getSystem() && campaign.getCampaignOptions().isPayForTransport()) {
            setTransportAmount(getEmployerTransportReimbursement(campaign));
        } else {
            setTransportAmount(Money.zero());
        }

        // calculate transit amount for CO
        if (campaign.getCampaignOptions().isUsePeacetimeCost()) {
            // contract base * transport period * reputation * employer modifier

            boolean useTwoWayPay = campaign.getCampaignOptions().isUseTwoWayPay();
            setTransitAmount(accountant.getContractBase()
                                   .multipliedBy(((getJumpPath(campaign).getJumps()) * (useTwoWayPay ? 2.0 : 1.0)) /
                                                       4.0)
                                   .multipliedBy(campaign.getAtBUnitRatingMod() * 0.2 + 0.5)
                                   .multipliedBy(1.2));
        } else {
            setTransitAmount(Money.zero());
        }

        setSigningBonusAmount(getBaseAmount()
                                    .plus(getOverheadAmount())
                                    .plus(getTransportAmount())
                                    .plus(getTransitAmount())
                                    .plus(getSupportAmount())
                                    .multipliedBy(getSigningBonus())
                                    .dividedBy(100));

        if (isMRBCFee()) {
            setFeeAmount(getBaseAmount()
                               .plus(getOverheadAmount())
                               .plus(getTransportAmount())
                               .plus(getTransitAmount())
                               .plus(getSupportAmount())
                               .multipliedBy(getMRBCFeePercentage())
                               .dividedBy(100));
        } else {
            setFeeAmount(Money.zero());
        }

        setAdvanceAmount(getTotalAmountPlusFees()
                               .multipliedBy(getAdvancePercent())
                               .dividedBy(100));

        // only adjust the start date for travel if the start date is currently null
        boolean adjustStartDate = false;
        LocalDate startDate = getStartDate();
        if (startDate == null) {
            startDate = campaign.getLocalDate();
            adjustStartDate = true;
        }

        if (adjustStartDate && (campaign.getSystemByName(getSystemId()) != null)) {
            boolean isUseCommandCircuit =
                  FactionStandingUtilities.isUseCommandCircuit(campaign.isOverridingCommandCircuitRequirements(),
                        campaign.isGM(),
                        campaign.getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
                        campaign.getFactionStandings(), campaign.getFutureAtBContracts());

            int days = (int) ceil(getJumpPath(campaign).getTotalTime(campaign.getLocalDate(),
                  campaign.getCurrentLocation().getTransitTime(), isUseCommandCircuit));
            startDate = startDate.plusDays(days);
        }

        setStartAndEndDate(startDate);
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
     * Retrieves the percentage of shares for this contract. This currently returns a default value of 30.
     *
     * @return the percentage of shares
     */
    public int getSharesPercent() {
        // TODO make this campaign option configurable
        return 30;
    }

    @Override
    protected int writeToXMLBegin(Campaign campaign, final PrintWriter pw, int indent) {
        indent = super.writeToXMLBegin(campaign, pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nMonths", getLengthInMonths());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startDate", getStartDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "endDate", getEndingDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "employer", getEmployer());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "paymentMultiplier", getPaymentMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commandRights", getCommandRights().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overheadComp", getOverheadCompensation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvagePct", getSalvagePercent());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvageExchange", isSalvageExchange());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "straightSupport", getStraightSupport());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "battleLossComp", getBattleLossCompensation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportComp", getTransportCompensation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrbcFee", getMRBCFeePercentage());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "advancePct", getAdvancePercent());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "signBonus", getSigningBonus());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hospitalBedsRented", getHospitalBedsRented());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "kitchensRented", getKitchensRented());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "holdingCellsRented", getHoldingCellsRented());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "advanceAmount", getAdvanceAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "signingAmount", getSigningBonusAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportAmount", getTransportAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transitAmount", getTransitAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overheadAmount", getOverheadAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "supportAmount", getSupportAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "baseAmount", getBaseAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "feeAmount", getFeeAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvagedByUnit", getSalvagedByUnit());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvagedByEmployer", getSalvagedByEmployer());
        return indent;
    }

    @Override
    public void loadFieldsFromXmlNode(Campaign campaign, Version version, Node wn) throws ParseException {
        // Okay, now load mission-specific fields!
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("employer")) {
                    setEmployer(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("startDate")) {
                    setStartDate(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("endDate")) {
                    setEndingDate(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("nMonths")) {
                    setLengthInMonths(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("paymentMultiplier")) {
                    setPaymentMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("commandRights")) {
                    setCommandRights(ContractCommandRights.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("overheadComp")) {
                    setOverheadCompensation(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("salvagePct")) {
                    setSalvagePercent(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("salvageExchange")) {
                    setSalvageExchange(wn2.getTextContent().trim().equals("true"));
                } else if (wn2.getNodeName().equalsIgnoreCase("straightSupport")) {
                    setStraightSupport(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("battleLossComp")) {
                    setBattleLossCompensation(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("transportComp")) {
                    setTransportCompensation(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("advancePct")) {
                    setAdvancePercent(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("signBonus")) {
                    setSigningBonus(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("hospitalBedsRented")) {
                    setHospitalBedsRented(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("kitchensRented")) {
                    setKitchensRented(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("holdingCellsRented")) {
                    setHoldingCellsRented(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrbcFee")) {
                    setMRBCFee(wn2.getTextContent().trim().equals("true"));
                } else if (wn2.getNodeName().equalsIgnoreCase("advanceAmount")) {
                    setAdvanceAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("signingAmount")) {
                    setSigningBonusAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("transportAmount")) {
                    setTransportAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("transitAmount")) {
                    setTransitAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("overheadAmount")) {
                    setOverheadAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("supportAmount")) {
                    setSupportAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("baseAmount")) {
                    setBaseAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("feeAmount")) {
                    setFeeAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("salvagedByUnit")) {
                    setSalvagedByUnit(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("salvagedByEmployer")) {
                    setSalvagedByEmployer(Money.fromXmlString(wn2.getTextContent().trim()));
                }
            } catch (Exception ex) {
                logger.error("", ex);
            }
        }

        // Version compatibility fix: Prior to 0.50.12, transportAmount stored the player's out-of-pocket
        // transport cost. Now it stores the employer's transport reimbursement. Recalculate for old saves.
        if (version.isLowerThan(new Version("0.50.12"))) {
            if ((null != getSystem()) && campaign.getCampaignOptions().isPayForTransport()) {
                setTransportAmount(getEmployerTransportReimbursement(campaign));
            } else {
                setTransportAmount(Money.zero());
            }
        }
    }
}
