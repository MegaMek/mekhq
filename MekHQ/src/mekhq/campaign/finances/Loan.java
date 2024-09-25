/*
 * Loan.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.finances;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Objects;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.logging.MMLogger;
import mekhq.campaign.finances.enums.FinancialTerm;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.utilities.MHQXMLUtility;

/**
 * TODO : Update loan baseline based on latest Campaign Operations Rules
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Loan {
    private static final MMLogger logger = MMLogger.create(Loan.class);

    // region Variable Declarations
    private String institution;
    private String referenceNumber;
    private Money principal;
    private int rate;
    private int years;
    private FinancialTerm financialTerm;
    private int collateral;
    private int remainingPayments;
    private Money paymentAmount;
    private LocalDate nextPayment;
    private boolean overdue;
    // endregion Variable Declarations

    // region Constructors
    private Loan() {
        // don't do anything, this is for loading
    }

    public Loan(final int principal, final int rate, final int years,
            final FinancialTerm financialTerm, final int collateral, final LocalDate today) {
        this(Money.of(principal), rate, years, financialTerm, collateral, today);
    }

    public Loan(final Money principal, final int rate, final int years,
            final FinancialTerm financialTerm, final int collateral, final LocalDate today) {
        this(FinancialInstitutions.randomFinancialInstitution(today).toString(), randomReferenceNumber(),
                principal, rate, years, financialTerm, collateral, today);
    }

    public Loan(final String institution, final String referenceNumber, final Money principal,
            final int rate, final int years, final FinancialTerm financialTerm,
            final int collateral, final LocalDate today) {
        setInstitution(institution);
        setReferenceNumber(referenceNumber);
        setPrincipal(principal);
        setRate(rate);
        setYears(years);
        setFinancialTerm(financialTerm);
        setCollateral(collateral);
        setNextPayment(getFinancialTerm().nextValidDate(today));
        setOverdue(false);

        calculateAmortization();
    }
    // endregion Constructors

    // region Getters/Setters
    public String getInstitution() {
        return institution;
    }

    public void setInstitution(final String institution) {
        this.institution = institution;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(final String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Money getPrincipal() {
        return principal;
    }

    public void setPrincipal(final Money principal) {
        this.principal = principal;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(final int rate) {
        this.rate = rate;
    }

    public int getYears() {
        return years;
    }

    public void setYears(final int years) {
        this.years = years;
    }

    public FinancialTerm getFinancialTerm() {
        return financialTerm;
    }

    public void setFinancialTerm(final FinancialTerm financialTerm) {
        this.financialTerm = financialTerm;
    }

    public int getCollateral() {
        return collateral;
    }

    public void setCollateral(final int collateral) {
        this.collateral = collateral;
    }

    public int getRemainingPayments() {
        return remainingPayments;
    }

    public void setRemainingPayments(final int remainingPayments) {
        this.remainingPayments = remainingPayments;
    }

    public Money getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(final Money paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public LocalDate getNextPayment() {
        return nextPayment;
    }

    public void setNextPayment(final LocalDate nextPayment) {
        this.nextPayment = nextPayment;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(final boolean overdue) {
        this.overdue = overdue;
    }
    // endregion Getters/Setters

    // region Determination Methods
    public Money determineCollateralAmount() {
        return getPrincipal().multipliedBy(getCollateral()).dividedBy(100);
    }

    public Money determineRemainingValue() {
        return getPaymentAmount()
                .multipliedBy(getRemainingPayments());
    }
    // endregion Determination Methods

    public void calculateAmortization() {
        // figure out actual rate from APR
        final double paymentsPerYear = getFinancialTerm().determineYearlyDenominator();
        final int numberOfPayments = (int) Math.ceil(getYears() * paymentsPerYear);
        final double periodicRate = (getRate() / 100.0) / paymentsPerYear;

        setRemainingPayments(numberOfPayments);
        setPaymentAmount(getPrincipal()
                .multipliedBy(periodicRate * Math.pow(1 + periodicRate, numberOfPayments))
                .dividedBy(Math.pow(1 + periodicRate, numberOfPayments) - 1));
    }

    public void paidLoan() {
        setNextPayment(getFinancialTerm().nextValidDate(getNextPayment()));
        setRemainingPayments(getRemainingPayments() - 1);
        setOverdue(false);
    }

    public boolean checkLoanPayment(final LocalDate today) {
        return today.equals(getNextPayment())
                || (today.isAfter(getNextPayment())) && (getRemainingPayments() > 0);
    }

    public static Loan getBaseLoan(final int rating, final LocalDate date) {
        // we are going to treat the score from StellarOps the same as dragoons score
        // TODO: pirates and government forces
        if (rating <= 0) {
            return new Loan(10000000, 35, 1, FinancialTerm.MONTHLY, 80, date);
        } else if (rating < 5) {
            return new Loan(10000000, 20, 1, FinancialTerm.MONTHLY, 60, date);
        } else if (rating < 10) {
            return new Loan(10000000, 15, 2, FinancialTerm.MONTHLY, 40, date);
        } else if (rating < 14) {
            return new Loan(10000000, 10, 3, FinancialTerm.MONTHLY, 25, date);
        } else {
            return new Loan(10000000, 7, 5, FinancialTerm.MONTHLY, 15, date);
        }
    }

    /*
     * These two bracket methods below return a 3=length integer array
     * of (minimum, starting, maximum). They are based on the StellarOps beta,
     * but since that document doesn't have minimum collateral amounts, we
     * just make up some numbers there (note that the minimum collateral also
     * determines the maximum interest)
     */
    public static int[] getInterestBracket(final int rating) {
        if (rating <= 0) {
            return new int[] { 15, 35, 75 };
        } else if (rating < 5) {
            return new int[] { 10, 20, 60 };
        } else if (rating < 10) {
            return new int[] { 5, 15, 35 };
        } else if (rating < 14) {
            return new int[] { 5, 10, 25 };
        } else {
            return new int[] { 4, 7, 17 };
        }
    }

    public static int[] getCollateralBracket(final int rating) {
        if (rating <= 0) {
            return new int[] { 60, 80, 380 };
        } else if (rating < 5) {
            return new int[] { 40, 60, 210 };
        } else if (rating < 10) {
            return new int[] { 20, 40, 140 };
        } else if (rating < 14) {
            return new int[] { 10, 25, 75 };
        } else {
            return new int[] { 5, 15, 35 };
        }
    }

    public static int getMaxYears(final int rating) {
        if (rating < 5) {
            return 1;
        } else if (rating < 9) {
            return 2;
        } else if (rating < 14) {
            return 3;
        } else {
            return 5;
        }
    }

    public static int getCollateralIncrement(final int rating, final boolean interestPositive) {
        if (rating < 5) {
            return interestPositive ? 2 : 15;
        } else {
            return interestPositive ? 1 : 10;
        }
    }

    public static int recalculateCollateralFromInterest(final int rating, final int interest) {
        final int interestDiff = interest - getInterestBracket(rating)[1];
        if (interestDiff < 0) {
            return getCollateralBracket(rating)[1] + (Math.abs(interestDiff) * getCollateralIncrement(rating, false));
        } else {
            return getCollateralBracket(rating)[1] - (interestDiff / getCollateralIncrement(rating, true));
        }
    }

    public static int recalculateInterestFromCollateral(final int rating, final int collateral) {
        final int collateralDiff = collateral - getCollateralBracket(rating)[1];
        if (collateralDiff < 0) {
            return getInterestBracket(rating)[1] + (getCollateralIncrement(rating, true) * Math.abs(collateralDiff));
        } else {
            return getInterestBracket(rating)[1] - (collateralDiff / getCollateralIncrement(rating, false));
        }
    }

    private static String randomReferenceNumber() {
        int length = Compute.randomInt(5) + 6;
        final StringBuilder stringBuilder = new StringBuilder();
        int nSinceSlash = 2;
        while (length > 0) {
            if (Compute.randomInt(9) < 3) {
                stringBuilder.append((char) (Compute.randomInt(26) + 'A'));
            } else {
                stringBuilder.append(Compute.randomInt(9));
            }
            length--;
            nSinceSlash++;
            // Check for a random slash
            if ((length > 0) && (Compute.randomInt(9) < 3) && (nSinceSlash >= 3)) {
                stringBuilder.append('-');
                nSinceSlash = 0;
            }
        }
        return stringBuilder.toString();
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "loan");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "institution", getInstitution());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "referenceNumber", getReferenceNumber());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "principal", getPrincipal());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rate", getRate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "years", getYears());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "financialTerm", getFinancialTerm().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "collateral", getCollateral());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "remainingPayments", getRemainingPayments());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "paymentAmount", getPaymentAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nextPayment", getNextPayment());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overdue", isOverdue());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "loan");
    }

    public static Loan generateInstanceFromXML(final Node wn) {
        final Loan loan = new Loan();
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("institution")) {
                    loan.setInstitution(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("referenceNumber")) {
                    loan.setReferenceNumber(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("principal")) {
                    loan.setPrincipal(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("rate")) {
                    loan.setRate(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("years")) {
                    loan.setYears(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("financialTerm")) {
                    loan.setFinancialTerm(FinancialTerm.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("collateral")) {
                    loan.setCollateral(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("remainingPayments")) {
                    loan.setRemainingPayments(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("paymentAmount")) {
                    loan.setPaymentAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("nextPayment")) {
                    loan.setNextPayment(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("overdue")) {
                    loan.setOverdue(Boolean.parseBoolean(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        return loan;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return getInstitution() + ' ' + getReferenceNumber();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof Loan) {
            final Loan loan = (Loan) other;
            return getInstitution().equals(loan.getInstitution())
                    && getReferenceNumber().equals(loan.getReferenceNumber())
                    && getPrincipal().equals(loan.getPrincipal())
                    && (getRate() == loan.getRate())
                    && (getYears() == loan.getYears())
                    && (getFinancialTerm() == loan.getFinancialTerm())
                    && (getCollateral() == loan.getCollateral())
                    && (getRemainingPayments() == loan.getRemainingPayments())
                    && getPaymentAmount().equals(loan.getPaymentAmount())
                    && getNextPayment().isEqual(loan.getNextPayment())
                    && (isOverdue() == loan.isOverdue());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstitution(), getReferenceNumber(), getPrincipal(), getRate(),
                getYears(), getFinancialTerm(), getCollateral(), getRemainingPayments(),
                getPaymentAmount(), getNextPayment(), isOverdue());
    }
}
