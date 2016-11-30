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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import megamek.common.Compute;
import mekhq.MekHQ;
import mekhq.MekHQOptions;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Loan implements MekHqXmlSerializable {
    
    // If you add more Canon institutions, please add them at the beginning and change the next line.
    // The first four of these are Canon, the rest are made up.
    private static final List<String> madeUpInstitutions = Arrays.asList("Southern Bank and Trust" /* Canon */, "The Alliance Reserve Bank" /* Canon */,
    	"Capellan Commonality Bank" /* Canon */, "Potwin Bank and Trust" /* Canon */, "Comstar Reserve", "Federated Employees Union",
    	"Bank of Oriente", "New Avalon Interstellar Bank", "Federated Boeing Credit Union", "First Commonwealth Bank",
    	"Donegal Bank and Trust", "Defiance Industries Credit Union", "Superior Bank of Sarna", "St. Ives Bank and Trust",
    	"Luthien Bank of the Dragon", "Golden Bank of Sian", "Rasalhauge National Bank", "Canopus Federal Reserve",
    	"Concordat Bank and Trust", "Outworlds Alliance National Bank", "Hegemony Bank and Trust",
    	"Andurien First National Bank");
    
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
        this(p, r, c, y, s, today, Utilities.getRandomItem(madeUpInstitutions), randomRefNumber());
    }
    
    public Loan(long p, int r, int c, int y, int s, GregorianCalendar today, String i, String ref) {
        this.principal = p;
        this.rate = r;
        this.collateral = c;
        this.years = y;
        this.schedule = s;
        nextPayment = (GregorianCalendar)today.clone();
        setFirstPaymentDate();
        calculateAmortization();
        institution = i;
        refNumber = ref;
        overdue = false;
    }
    
    public void setFirstPaymentDate() {
        boolean keepGoing = true;
        //We are going to assume a standard grace period, so you have to go 
        //through the first full time length (not partial) before your first
        //payment
        int grace = 1;
        switch(schedule) {
        case Finances.SCHEDULE_BIWEEKLY:
            grace = 2;
            break;
        case Finances.SCHEDULE_QUARTERLY:
            grace = 3;
            break;
        }
        while(keepGoing) {
            nextPayment.add(GregorianCalendar.DAY_OF_YEAR, 1);
            switch(schedule) {
            case Finances.SCHEDULE_BIWEEKLY:
                if(nextPayment.get(Calendar.DAY_OF_WEEK) == 1) {
                    if(grace > 0) {
                        grace--;
                    }
                    else {
                        keepGoing = false;
                    }
                }
                break;
            case Finances.SCHEDULE_MONTHLY:
                if(nextPayment.get(Calendar.DAY_OF_MONTH) == 1) {
                    if(grace > 0) {
                        grace--;
                    }
                    else {
                        keepGoing = false;
                    }
                }
                break;
            case Finances.SCHEDULE_QUARTERLY:
                if(nextPayment.get(Calendar.DAY_OF_MONTH) == 1) {
                    if(grace > 0) {
                        grace--;
                    }
                    else {
                        keepGoing = false;
                    }
                }
                break;
            case Finances.SCHEDULE_YEARLY:
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
    
    public void calculateAmortization() {
        
        //figure out actual rate from APR
        int denom = 1;
        switch(schedule) {
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
        double r = ((double)rate/100.0)/denom;
        nPayments = years * denom;
        payAmount = (long)((principal * r * Math.pow(1+r,nPayments))/(Math.pow(1+r, nPayments)-1));
        totalValue = payAmount * nPayments;
        collateralValue = (long)(((double)collateral/100.0)*principal);
    }
    
    public long getPrincipal() {
        return principal;
    }

	public void setPrincipal(long principal) {
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
        case Finances.SCHEDULE_BIWEEKLY:
            nextPayment.add(GregorianCalendar.WEEK_OF_YEAR, 2);
            break;
        case Finances.SCHEDULE_MONTHLY:
            nextPayment.add(GregorianCalendar.MONTH, 1);
            break;
        case Finances.SCHEDULE_QUARTERLY:
            nextPayment.add(GregorianCalendar.MONTH, 3);
            break;
        case Finances.SCHEDULE_YEARLY:
            nextPayment.add(GregorianCalendar.YEAR, 1);
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

	public GregorianCalendar getNextPayment() {
		return nextPayment;
	}

	public void setNextPayment(GregorianCalendar nextPayment) {
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

	public long getPayAmount() {
		return payAmount;
	}

	public void setPayAmount(long payAmount) {
		this.payAmount = payAmount;
	}
	
    public long getCollateralAmount() {
        return collateralValue;
    }

	public long getCollateralValue() {
		return collateralValue;
	}

	public void setCollateralValue(long collateralValue) {
		this.collateralValue = collateralValue;
	}

	public static List<String> getMadeupinstitutions() {
		return madeUpInstitutions;
	}
    
    public int getYears() {
        return years;
    }

	public void setYears(int years) {
		this.years = years;
	}
    
    public long getTotalValue() {
        return totalValue;
    }

	public void setTotalValue(long totalValue) {
		this.totalValue = totalValue;
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
        SimpleDateFormat df = MekHQOptions.getInstance().getDateFormatDataStorage();
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
                try {
                	SimpleDateFormat df = MekHQOptions.getInstance().getDateFormatDataStorage();
                    SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    retVal.nextPayment = (GregorianCalendar) GregorianCalendar.getInstance();
                    try {
                        retVal.nextPayment.setTime(df.parse(wn2.getTextContent().trim()));
                    } catch (ParseException e) {
                    	retVal.nextPayment.setTime(fallbackFormat.parse(wn2.getTextContent().trim()));
                    }
                } catch (DOMException e) {
                	MekHQ.logError(e);
                } catch (ParseException e) {
                	MekHQ.logError(e);
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
    
    public static Loan getBaseLoanFor(int rating, GregorianCalendar cal) {
        //we are going to treat the score from StellarOps the same as dragoons score
        //TODO: pirates and government forces
        if(rating <= 0) {
            return new Loan(10000000, 35, 80, 1, Finances.SCHEDULE_MONTHLY, cal);
        }
        else if(rating < 5) {
            return new Loan(10000000, 20, 60, 1, Finances.SCHEDULE_MONTHLY, cal);
        } else if(rating < 10) {
            return new Loan(10000000, 15, 40, 2, Finances.SCHEDULE_MONTHLY, cal);
        } else if(rating < 14) {
            return new Loan(10000000, 10, 25, 3, Finances.SCHEDULE_MONTHLY, cal);
        } else {
            return new Loan(10000000, 7, 15, 5, Finances.SCHEDULE_MONTHLY, cal);
        }
        
    }
    
    /* These two bracket methods below return a 3=length integer array
     * of (minimum, starting, maximum). They are based on the StellarOps beta, 
     * but since that document doesn't have minimum collateral amounts, we
     * just make up some numbers there (note that the minimum collateral also
     * determines the maximum interest)
     */
    public static int[] getInterestBracket(int rating) {
        if(rating <= 0) {
            return new int[]{15,35,75};
        } else if(rating < 5) {
            return new int[]{10,20,60};
        } else if(rating < 10) {
            return new int[]{5,15,35};
        } else if(rating < 14) {
            return new int[]{5,10,25};
        } else {
            return new int[]{4,7,17};
        }
    }
    
    public static int[] getCollateralBracket(int rating) {
        if(rating <= 0) {
            return new int[]{60,80,380};
        } else if(rating < 5) {
            return new int[]{40,60,210};
        } else if(rating < 10) {
            return new int[]{20,40,140};
        } else if(rating < 14) {
            return new int[]{10,25,75};
        } else {
            return new int[]{5,15,35};
        }
    }
    
    public static int getMaxYears(int rating) {
        if(rating < 5) {
            return 1;
        } else if(rating < 9) {
            return 2;
        } else if(rating < 14) {
            return 3;
        } else {
            return 5;
        }
    }
    
    public static int getCollateralIncrement(boolean interestPositive, int rating) {
        if(rating < 5) {
            if(interestPositive) {
                return 2;
            } else {
                return 15;
            }
        }
        else {
            if(interestPositive) {
                return 1;
            } else {
                return 10;
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
    
    public static String randomRefNumber() {
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
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Loan)) {
            return false;
        }
        Loan loan = (Loan)obj;
        return this.getDescription().equals(loan.getDescription()) 
                && this.getInterestRate() == loan.getInterestRate()
                && this.getCollateralAmount() == loan.getCollateralAmount()
                && this.getCollateralPercent() == loan.getCollateralPercent()
                && this.getNextPayDate().equals(loan.getNextPayDate())
                && this.getYears() == loan.getYears()
                && this.getPrincipal() == loan.getPrincipal()
                && this.getPaymentSchedule() == loan.getPaymentSchedule()
                && this.getRemainingPayments() == loan.getRemainingPayments()
                && this.getRemainingValue() == loan.getRemainingValue();
    }
}