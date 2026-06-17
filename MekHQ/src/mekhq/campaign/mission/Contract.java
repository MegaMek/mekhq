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

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
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

    public Contract() {
        this(null, null);
    }

    public Contract(String name, String employer) {
        super(name);
        setEmployerName(employer);

        setLengthInMonths(12);
        setPaymentMultiplier(2.0);
        setCommandRights(ContractCommandRights.HOUSE);
        setStraightSupport(50);
        setBattleLossCompensation(50);
        setSalvagePercent(50);
        setTransportCompensation(50);
        setAdvancePercent(25);
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

    @Override
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

        if (isPaidMRBCFee()) {
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

    @Override
    protected int writeToXMLBegin(Campaign campaign, final PrintWriter pw, int indent) {
        indent = super.writeToXMLBegin(campaign, pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nMonths", getLengthInMonths());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startDate", getStartDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "endDate", getEndingDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "employer", getEmployerName());
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
                    setEmployerName(wn2.getTextContent());
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
                    setPaidMRBCFee(wn2.getTextContent().trim().equals("true"));
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
