/*
 * Loan.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import megamek.common.Compute;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Loan implements MekHqXmlSerializable {
    // If you add more Canon institutions, please add them at the beginning and change the next line.
    // The first four of these are Canon, the rest are made up.
    private static final List<String> madeUpInstitutions = Arrays.asList("Southern Bank and Trust" /* Canon */, "The Alliance Reserve Bank" /* Canon */,
        "Capellan Commonality Bank" /* Canon */, "Potwin Bank and Trust" /* Canon */, "ComStar Reserve", "Federated Employees Union",
        "Bank of Oriente", "New Avalon Interstellar Bank", "Federated Boeing Credit Union", "First Commonwealth Bank",
        "Donegal Bank and Trust", "Defiance Industries Credit Union", "Superior Bank of Sarna", "St. Ives Bank and Trust",
        "Luthien Bank of the Dragon", "Golden Bank of Sian", "Rasalhague National Bank", "Canopus Federal Reserve",
        "Concordat Bank and Trust", "Outworlds Alliance National Bank", "Hegemony Bank and Trust",
        "Andurien First National Bank");

    private String institution;
    private String refNumber;
    private Money principal;
    private int rate;
    private LocalDate nextPayment;
    private int years;
    private int schedule;
    private int collateral;
    private int nPayments;
    private Money payAmount;
    private Money collateralValue;
    private boolean overdue;

    public Loan() {
        //don't do anything, this is for loading
    }

    public Loan(Money p, int r, int c, int y, int s, LocalDate today) {
        this(p, r, c, y, s, today, Utilities.getRandomItem(madeUpInstitutions), randomRefNumber());
    }

    public Loan(Money p, int r, int c, int y, int s, LocalDate today, String i, String ref) {
        this.principal = p;
        this.rate = r;
        this.collateral = c;
        this.years = y;
        this.schedule = s;
        nextPayment = today;
        setFirstPaymentDate();
        calculateAmortization();
        institution = i;
        refNumber = ref;
        overdue = false;
    }

    public void setFirstPaymentDate() {
        //We are going to assume a standard grace period, so you have to go
        //through the first full time length (not partial) before your first
        //payment

        // First, we need to increase the number of days by one
        nextPayment = nextPayment.plusDays(1);

        // Finally, we use that and the schedule type to determine the length including the grace period
        switch (schedule) {
            case Finances.SCHEDULE_BIWEEKLY:
                nextPayment = nextPayment.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).plusWeeks(2);
                break;
            case Finances.SCHEDULE_MONTHLY:
                if (nextPayment.getDayOfMonth() != 1) {
                    nextPayment = nextPayment.with(TemporalAdjusters.firstDayOfNextMonth());
                }
                nextPayment = nextPayment.plusMonths(1);
                break;
            case Finances.SCHEDULE_QUARTERLY:
                if (nextPayment.getDayOfMonth() != 1) {
                    nextPayment = nextPayment.with(TemporalAdjusters.firstDayOfNextMonth());
                }
                nextPayment = nextPayment.plusMonths(3);
                break;
            case Finances.SCHEDULE_YEARLY:
                if (nextPayment.getDayOfYear() != 1) {
                    nextPayment = nextPayment.with(TemporalAdjusters.firstDayOfNextYear());
                }
                nextPayment = nextPayment.plusYears(1);
                break;
        }
    }

    public void calculateAmortization() {
        //figure out actual rate from APR
        int denom = 1;
        switch (schedule) {
            case Finances.SCHEDULE_BIWEEKLY:
                denom = 26;
                break;
            case Finances.SCHEDULE_MONTHLY:
                denom = 12;
                break;
            case Finances.SCHEDULE_QUARTERLY:
                denom = 4;
                break;
            case Finances.SCHEDULE_YEARLY:
                denom = 1;
                break;
        }
        double r = ((double) rate / 100.0) / denom;
        nPayments = years * denom;
        payAmount = principal.multipliedBy(r * Math.pow(1 + r, nPayments)).dividedBy(Math.pow(1 + r, nPayments) - 1);
        collateralValue = principal.multipliedBy(collateral).dividedBy(100);
    }

    public Money getPrincipal() {
        return principal;
    }

    public void setPrincipal(Money principal) {
        this.principal = principal;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String s) {
        this.institution = s;
    }

    public String getRefNumber() {
        return refNumber;
    }

    public void setRefNumber(String s) {
        this.refNumber = s;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean b) {
        overdue = b;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getInterestRate() {
        return rate;
    }

    public boolean checkLoanPayment(LocalDate today) {
        return (today.equals(nextPayment) || (today.isAfter(nextPayment)) && (nPayments > 0));
    }

    public Money getPaymentAmount() {
        return payAmount;
    }

    public Money getRemainingValue() {
        return payAmount.multipliedBy(nPayments);
    }

    public void paidLoan() {
        switch (schedule) {
            case Finances.SCHEDULE_BIWEEKLY:
                setNextPayment(getNextPayment().plusWeeks(2));
                break;
            case Finances.SCHEDULE_MONTHLY:
                setNextPayment(getNextPayment().plusMonths(1));
                break;
            case Finances.SCHEDULE_QUARTERLY:
                setNextPayment(getNextPayment().plusMonths(3));
                break;
            case Finances.SCHEDULE_YEARLY:
                setNextPayment(getNextPayment().plusYears(1));
                break;
        }
        nPayments--;
        overdue = false;
    }

    public int getRemainingPayments() {
        return nPayments;
    }

    public String getDescription() {
        return institution + " " + refNumber;
    }

    public LocalDate getNextPayment() {
        return nextPayment;
    }

    public void setNextPayment(LocalDate nextPayment) {
        this.nextPayment = nextPayment;
    }

    public int getPaymentSchedule() {
        return schedule;
    }

    public int getSchedule() {
        return schedule;
    }

    public void setSchedule(int schedule) {
        this.schedule = schedule;
    }

    public int getCollateralPercent() {
        return collateral;
    }

    public int getCollateral() {
        return collateral;
    }

    public void setCollateral(int collateral) {
        this.collateral = collateral;
    }

    public int getnPayments() {
        return nPayments;
    }

    public void setnPayments(int nPayments) {
        this.nPayments = nPayments;
    }

    public Money getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Money payAmount) {
        this.payAmount = payAmount;
    }

    public Money getCollateralAmount() {
        return collateralValue;
    }

    public Money getCollateralValue() {
        return collateralValue;
    }

    public void setCollateralValue(Money collateralValue) {
        this.collateralValue = collateralValue;
    }

    public static List<String> getMadeupInstitutions() {
        return madeUpInstitutions;
    }

    public int getYears() {
        return years;
    }

    public void setYears(int years) {
        this.years = years;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "loan");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "institution", institution);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "refNumber", refNumber);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "principal", principal.toXmlString());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "rate", rate);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "years", years);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "schedule", schedule);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "collateral", collateral);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "nPayments", nPayments);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payAmount", payAmount.toXmlString());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "collateralValue", collateralValue.toXmlString());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "overdue", overdue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "nextPayment",
                MekHqXmlUtil.saveFormattedDate(getNextPayment()));
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "loan");
    }

    public static Loan generateInstanceFromXML(Node wn) {
        Loan retVal = new Loan();

        NodeList nl = wn.getChildNodes();
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("institution")) {
                retVal.institution = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("refNumber")) {
                retVal.refNumber = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("principal")) {
                retVal.principal = Money.fromXmlString(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("payAmount")) {
                retVal.payAmount = Money.fromXmlString(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("collateralValue")) {
                retVal.collateralValue = Money.fromXmlString(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("rate")) {
                retVal.rate = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("years")) {
                retVal.years = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("schedule")) {
                retVal.schedule = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("collateral")) {
                retVal.collateral = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("nPayments")) {
                retVal.nPayments = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("nextPayment")) {
                retVal.setNextPayment(MekHqXmlUtil.parseDate(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("overdue")) {
                retVal.overdue = wn2.getTextContent().equalsIgnoreCase("true");
            }
        }
        return retVal;
    }

    public static Loan getBaseLoanFor(int rating, LocalDate date) {
        //we are going to treat the score from StellarOps the same as dragoons score
        //TODO: pirates and government forces
        if (rating <= 0) {
            return new Loan(Money.of(10000000), 35, 80, 1, Finances.SCHEDULE_MONTHLY, date);
        } else if (rating < 5) {
            return new Loan(Money.of(10000000), 20, 60, 1, Finances.SCHEDULE_MONTHLY, date);
        } else if (rating < 10) {
            return new Loan(Money.of(10000000), 15, 40, 2, Finances.SCHEDULE_MONTHLY, date);
        } else if (rating < 14) {
            return new Loan(Money.of(10000000), 10, 25, 3, Finances.SCHEDULE_MONTHLY, date);
        } else {
            return new Loan(Money.of(10000000), 7, 15, 5, Finances.SCHEDULE_MONTHLY, date);
        }
    }

    /* These two bracket methods below return a 3=length integer array
     * of (minimum, starting, maximum). They are based on the StellarOps beta,
     * but since that document doesn't have minimum collateral amounts, we
     * just make up some numbers there (note that the minimum collateral also
     * determines the maximum interest)
     */
    public static int[] getInterestBracket(int rating) {
        if (rating <= 0) {
            return new int[]{15,35,75};
        } else if (rating < 5) {
            return new int[]{10,20,60};
        } else if (rating < 10) {
            return new int[]{5,15,35};
        } else if (rating < 14) {
            return new int[]{5,10,25};
        } else {
            return new int[]{4,7,17};
        }
    }

    public static int[] getCollateralBracket(int rating) {
        if (rating <= 0) {
            return new int[]{60,80,380};
        } else if (rating < 5) {
            return new int[]{40,60,210};
        } else if (rating < 10) {
            return new int[]{20,40,140};
        } else if (rating < 14) {
            return new int[]{10,25,75};
        } else {
            return new int[]{5,15,35};
        }
    }

    public static int getMaxYears(int rating) {
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

    public static int getCollateralIncrement(boolean interestPositive, int rating) {
        if (rating < 5) {
            if (interestPositive) {
                return 2;
            } else {
                return 15;
            }
        } else {
            if (interestPositive) {
                return 1;
            } else {
                return 10;
            }
        }
    }

    public static int recalculateCollateralFromInterest(int interest, int rating) {
        int interestDiff = interest - getInterestBracket(rating)[1];
        if (interestDiff < 0) {
            return getCollateralBracket(rating)[1] + (Math.abs(interestDiff) * getCollateralIncrement(false, rating));
        } else {
            return getCollateralBracket(rating)[1] - (interestDiff/getCollateralIncrement(true, rating));
        }
    }

    public static int recalculateInterestFromCollateral(int collateral, int rating) {
        int collateralDiff = collateral - getCollateralBracket(rating)[1];
        if (collateralDiff < 0) {
            return getInterestBracket(rating)[1] + (getCollateralIncrement(true, rating) * Math.abs(collateralDiff));
        } else {
            return getInterestBracket(rating)[1] - (collateralDiff/getCollateralIncrement(false, rating));
        }

    }

    public static String randomRefNumber() {
        int length = Compute.randomInt(5) + 6;
        StringBuilder buffer = new StringBuilder();
        int nSinceSlash = 2;
        while (length > 0) {
            if (Compute.randomInt(9) < 3) {
                buffer.append((char) (Compute.randomInt(26) + 'A'));
            } else {
                buffer.append(Compute.randomInt(9));
            }
            length--;
            nSinceSlash++;
            //Check for a random slash
            if ((length > 0) && (Compute.randomInt(9) < 3) && (nSinceSlash >= 3)) {
                buffer.append("-");
                nSinceSlash = 0;
            }
        }
        return buffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Loan)) {
            return false;
        }
        Loan loan = (Loan) obj;
        return this.getDescription().equals(loan.getDescription())
                && this.getInterestRate() == loan.getInterestRate()
                && this.getCollateralAmount().equals(loan.getCollateralAmount())
                && this.getCollateralPercent() == loan.getCollateralPercent()
                && this.getNextPayment().equals(loan.getNextPayment())
                && this.getYears() == loan.getYears()
                && this.getPrincipal().equals(loan.getPrincipal())
                && this.getPaymentSchedule() == loan.getPaymentSchedule()
                && this.getRemainingPayments() == loan.getRemainingPayments()
                && this.getRemainingValue().equals(loan.getRemainingValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(),
                getInterestRate(),
                getCollateralAmount(),
                getCollateralPercent(),
                getNextPayment(),
                getYears(),
                getPrincipal(),
                getPaymentSchedule(),
                getRemainingPayments(),
                getRemainingValue());
    }
}
