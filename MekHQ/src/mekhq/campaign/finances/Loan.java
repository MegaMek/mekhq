/*
 * Loan.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.finances;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import megamek.common.Compute;
import mekhq.campaign.IDragoonsRating;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Loan implements MekHqXmlSerializable {

    public static final int SCHEDULE_BIWEEKLY  = 0;
    public static final int SCHEDULE_MONTHLY   = 1;
    public static final int SCHEDULE_QUARTERLY = 2;
    public static final int SCHEDULE_YEARLY    = 3;
    public static final int SCHEDULE_NUM       = 4;

    private static final String[] madeUpInstitutions = {"Comstar Reserve", "Federated Employees Union", "Bank of Oriente"};
    
    private String institution;
    private String refNumber;
    private long principal;
    private int rate;
    private GregorianCalendar nextPayment;
    private int years;
    private int schedule;
    private int collateral;
    private int nPayments;
    private long payAmount;
    private long totalValue;
    private long collateralValue;
    private boolean overdue;
    
    public Loan() {
        //dont do anything, this is for loading
    }
    
    public Loan(long p, int r, int c, int y, int s, GregorianCalendar today) {
        this.principal = p;
        this.rate = r;
        this.collateral = c;
        this.years = y;
        this.schedule = s;
        nextPayment = (GregorianCalendar)today.clone();
        setFirstPaymentDate();
        calculateAmortization();
        institution = madeUpInstitutions[Compute.randomInt(madeUpInstitutions.length)];
        refNumber = randomRefNumber();
        overdue = false;
    }
    
    private void setFirstPaymentDate() {
        boolean keepGoing = true;
        //We are going to assume a standard grace period, so you have to go 
        //through the first full time length (not partial) before your first
        //payment
        int grace = 1;
        switch(schedule) {
        case SCHEDULE_BIWEEKLY:
            grace = 2;
            break;
        case SCHEDULE_QUARTERLY:
            grace = 3;
            break;
        }
        while(keepGoing) {
            nextPayment.add(GregorianCalendar.DAY_OF_YEAR, 1);
            switch(schedule) {
            case SCHEDULE_BIWEEKLY:
                if(nextPayment.get(Calendar.DAY_OF_WEEK) == 1) {
                    if(grace > 0) {
                        grace--;
                    }
                    else {
                        keepGoing = false;
                    }
                }
                break;
            case SCHEDULE_MONTHLY:
                if(nextPayment.get(Calendar.DAY_OF_MONTH) == 1) {
                    if(grace > 0) {
                        grace--;
                    }
                    else {
                        keepGoing = false;
                    }
                }
                break;
            case SCHEDULE_QUARTERLY:
                if(nextPayment.get(Calendar.DAY_OF_MONTH) == 1) {
                    if(grace > 0) {
                        grace--;
                    }
                    else {
                        keepGoing = false;
                    }
                }
                break;
            case SCHEDULE_YEARLY:
                if(nextPayment.get(Calendar.DAY_OF_YEAR) == 1) {
                    if(grace > 0) {
                        grace--;
                    }
                    else {
                        keepGoing = false;
                    }
                }
                break;       
            default:
                //shouldn't get here but kill the loop just in case
                keepGoing = false;
                break;
                
            }
        }
    }
    
    private void calculateAmortization() {
        
        //figure out actual rate from APR
        int denom = 1;
        switch(schedule) {
        case SCHEDULE_BIWEEKLY:
            denom = 26;
            break;
        case SCHEDULE_MONTHLY:
            denom = 12;
            break;
        case SCHEDULE_QUARTERLY:
            denom = 4;
            break;
        case SCHEDULE_YEARLY:
            denom = 1;
            break;
        }
        double r = ((double)rate/100.0)/denom;
        nPayments = years * denom;
        payAmount = (long)((principal * r * Math.pow(1+r,nPayments))/(Math.pow(1+r, nPayments)-1));
        totalValue = payAmount * nPayments;
        collateralValue = (long)(((double)collateral/100.0)*principal);
    }
    
    public long getPrincipal() {
        return principal;
    }
    
    public long getCollateralAmount() {
        return collateralValue;
    }
    
    public long getTotalValue() {
        return totalValue;
    }
    
    public int getInterestRate() {
        return rate;
    }
    
    public int getCollateralPercent() {
        return collateral;
    }
    
    public int getYears() {
        return years;
    }
    
    public int getPaymentSchedule() {
        return schedule;
    }
    
    public boolean checkLoanPayment(GregorianCalendar today) {
        return (today.equals(nextPayment) || today.after(nextPayment)) && nPayments > 0;
    }
    
    public long getPaymentAmount() {
        return payAmount;
    }
    
    public long getRemainingValue() {
        return payAmount * nPayments;
    }
    
    public Date getNextPayDate() {
        return nextPayment.getTime();
    }
    
    public void paidLoan() {      
        switch(schedule) {
        case SCHEDULE_BIWEEKLY:
            nextPayment.add(GregorianCalendar.WEEK_OF_YEAR, 2);
            break;
        case SCHEDULE_MONTHLY:
            nextPayment.add(GregorianCalendar.MONTH, 1);
            break;
        case SCHEDULE_QUARTERLY:
            nextPayment.add(GregorianCalendar.MONTH, 3);
            break;
        case SCHEDULE_YEARLY:
            nextPayment.add(GregorianCalendar.YEAR, 1);
            break;
        }
        nPayments--;
    }
    
    public int getRemainingPayments() {
        return nPayments;
    }
    
    public String getDescription() {
        return institution + " " + refNumber;
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
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<loan>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<institution>"
                +MekHqXmlUtil.escape(institution)
                +"</institution>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<refNumber>"
                +MekHqXmlUtil.escape(refNumber)
                +"</refNumber>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<principal>"
                +principal
                +"</principal>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<rate>"
                +rate
                +"</rate>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<years>"
                +years
                +"</years>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<schedule>"
                +schedule
                +"</schedule>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<collateral>"
                +collateral
                +"</collateral>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<nPayments>"
                +nPayments
                +"</nPayments>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<payAmount>"
                +payAmount
                +"</payAmount>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<collateralValue>"
                +totalValue
                +"</collateralValue>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<overdue>"
                +overdue
                +"</overdue>");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "nextPayment", df.format(nextPayment.getTime()));
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</loan>");
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
                retVal.principal = Long.parseLong(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("payAmount")) {
                retVal.payAmount = Long.parseLong(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("totalValue")) {
                retVal.totalValue = Long.parseLong(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("collateralValue")) {
                retVal.collateralValue = Long.parseLong(wn2.getTextContent().trim());
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
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                retVal.nextPayment = (GregorianCalendar) GregorianCalendar.getInstance();
                try {
                    retVal.nextPayment.setTime(df.parse(wn2.getTextContent().trim()));
                } catch (DOMException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("overdue")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    retVal.overdue = true;
                else
                    retVal.overdue = false;
            } 
        }
        return retVal;
    }
    
    
    public static String getScheduleName(int schedule) {
        switch(schedule) {
        case SCHEDULE_BIWEEKLY:
            return "Bi-Weekly";
        case SCHEDULE_MONTHLY:
            return "Monthly";
        case SCHEDULE_QUARTERLY:
            return "Quarterly";
        case SCHEDULE_YEARLY:
            return "Yearly";
        default:
            return "?";
        }
    }
    
    public static Loan getBaseLoanFor(int rating, GregorianCalendar cal) {
        //we are going to use the numbers from StellarOps beta here - although
        //its a little unclear how we should translate dragoons rating to reputation
        //score. Since there are 5 categories on the table, I am going to stick
        //with our 5 dragoons ratings
        switch(rating) {
        case IDragoonsRating.DRAGOON_F:
            return new Loan(10000000, 35, 60, 1, SCHEDULE_MONTHLY, cal);
        case IDragoonsRating.DRAGOON_D:
            return new Loan(10000000, 20, 60, 1, SCHEDULE_MONTHLY, cal);
        case IDragoonsRating.DRAGOON_C:
            return new Loan(10000000, 15, 40, 2, SCHEDULE_MONTHLY, cal);
        case IDragoonsRating.DRAGOON_B:
            return new Loan(10000000, 10, 25, 3, SCHEDULE_MONTHLY, cal);
        case IDragoonsRating.DRAGOON_A:
            return new Loan(10000000, 7, 15, 5, SCHEDULE_MONTHLY, cal);
        default:
            return new Loan(10000000, 50, 100, 1, SCHEDULE_MONTHLY, cal);
        }
        
    }
    
    /* These two bracket methods below return a 3=length integer array
     * of (minimum, starting, maximum). They are based on the StellarOps beta, 
     * but since that document doesn't have minimum collateral amounts, we
     * just make up some numbers there (note that the minimum collateral also
     * determines the maximum interest)
     */
    public static int[] getInterestBracket(int rating) {
        switch(rating) {
        case IDragoonsRating.DRAGOON_F:
            return new int[]{15,35,75};
        case IDragoonsRating.DRAGOON_D:
            return new int[]{10,20,60};
        case IDragoonsRating.DRAGOON_C:
            return new int[]{5,15,35};
        case IDragoonsRating.DRAGOON_B:
            return new int[]{5,10,25};
        case IDragoonsRating.DRAGOON_A:
            return new int[]{4,7,17};
        default:
            return new int[]{15,35,75};
        }
    }
    
    public static int[] getCollateralBracket(int rating) {
        switch(rating) {
        case IDragoonsRating.DRAGOON_F:
            return new int[]{60,80,380};
        case IDragoonsRating.DRAGOON_D:
            return new int[]{40,60,210};
        case IDragoonsRating.DRAGOON_C:
            return new int[]{20,40,140};
        case IDragoonsRating.DRAGOON_B:
            return new int[]{10,25,75};
        case IDragoonsRating.DRAGOON_A:
            return new int[]{5,15,35};
        default:
            return new int[]{60,80,380};
        }
    }
    
    public static int getMaxYears(int rating) {
        switch(rating) {
        case IDragoonsRating.DRAGOON_F:
        case IDragoonsRating.DRAGOON_D:
            return 1;
        case IDragoonsRating.DRAGOON_C:
            return 2;
        case IDragoonsRating.DRAGOON_B:
            return 3;
        case IDragoonsRating.DRAGOON_A:
            return 5;
        default:
            return 1;
        }
    }
    
    public static int getCollateralIncrement(boolean interestPositive, int rating) {
        switch(rating) {
        case IDragoonsRating.DRAGOON_F:
        case IDragoonsRating.DRAGOON_D:
            if(interestPositive) {
                return 2;
            } else {
                return 15;
            }
        case IDragoonsRating.DRAGOON_C:
        case IDragoonsRating.DRAGOON_B:
        case IDragoonsRating.DRAGOON_A:
            if(interestPositive) {
                return 1;
            } else {
                return 10;
            }
        default:
            if(interestPositive) {
                return 2;
            } else {
                return 20;
            }
        }
    }
    
    public static int recalculateCollateralFromInterest(int interest, int rating) {
        int interestDiff = interest - getInterestBracket(rating)[1];
        if(interestDiff < 0) {
            return getCollateralBracket(rating)[1] + (Math.abs(interestDiff) * getCollateralIncrement(false, rating));
        }
        else {
            return getCollateralBracket(rating)[1] - (interestDiff/getCollateralIncrement(true, rating));
        }
    }
    
    public static int recalculateInterestFromCollateral(int collateral, int rating) {
        int collateralDiff = collateral - getCollateralBracket(rating)[1];
        if(collateralDiff < 0) {
            return getInterestBracket(rating)[1] + (getCollateralIncrement(true, rating) * Math.abs(collateralDiff));
        } else {
            return getInterestBracket(rating)[1] - (collateralDiff/getCollateralIncrement(false, rating));
        }
        
    }
    
    public String randomRefNumber() {
        int length = Compute.randomInt(5)+6;
        StringBuffer buffer = new StringBuffer();
        int nSinceSlash = 2;
        while(length > 0) {
            if(Compute.randomInt(9) < 3) {
                buffer.append((char) (Compute.randomInt(26) + 'A'));
            } else {
                buffer.append(Compute.randomInt(9));
            }
            length--;
            nSinceSlash++;
            //Check for a random slash
            if(length > 0 && Compute.randomInt(9) < 3 && nSinceSlash >= 3) {
                buffer.append("-");
                nSinceSlash = 0;
            }
        }
        return buffer.toString();
    }
}