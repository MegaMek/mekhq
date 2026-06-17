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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
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
    private boolean salvageExchange;
    private int transportCompensation;

    // need to keep track of total value salvaged for salvage rights
    private Money salvagedByUnit = Money.zero();
    private Money salvagedByEmployer = Money.zero();

    // actual amounts
    private Money advanceAmount = Money.zero();
    private Money signingAmount = Money.zero();

    private Money transportAmount = Money.zero();
    private Money transitAmount = Money.zero();
    private Money overheadAmount = Money.zero();
    private Money supportAmount = Money.zero();
    private Money baseAmount = Money.zero();
    private Money feeAmount = Money.zero();

    private boolean mrbcFee;
    private int advancePercent;
    private int signingBonus;

    private int hospitalBedsRented;
    private int kitchensRented;
    private int holdingCellsRented;

    private final List<Scenario> scenarios = new ArrayList<>();

    public final static int MRBC_FEE_PERCENTAGE = 5;

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

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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
        return signingAmount;
    }

    public void setSigningBonusAmount(Money signingAmount) {
        this.signingAmount = signingAmount;
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

    public boolean isMrbcFee() {
        return mrbcFee;
    }

    public void setMrbcFee(boolean mrbcFee) {
        this.mrbcFee = mrbcFee;
    }

    public boolean isMRBCFee() {
        return mrbcFee;
    }

    public void setMRBCFee(boolean mrbcFee) {
        this.mrbcFee = mrbcFee;
    }

    public int getMRBCFeePercentage() {
        return MRBC_FEE_PERCENTAGE;
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

    @Override
    public String toString() {
        return !getStatus().isCompleted() ?
                     getName() :
                     getFormattedTextAt(RESOURCE_BUNDLE, "AbstractMission.name.completed", getName());
    }
}
