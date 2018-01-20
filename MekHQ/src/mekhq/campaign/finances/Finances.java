 /* Finances.java
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
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.LoanDefaultedEvent;
import mekhq.campaign.event.TransactionCreditEvent;
import mekhq.campaign.event.TransactionDebitEvent;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Finances implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 8533117455496219692L;

	private ArrayList<Transaction> transactions;
	private ArrayList<Loan> loans;
	private ArrayList<Asset> assets;
	private int loanDefaults;
	private int failCollateral;
	private Date wentIntoDebt;

    public static final int SCHEDULE_BIWEEKLY  = 0;
    public static final int SCHEDULE_MONTHLY   = 1;
    public static final int SCHEDULE_QUARTERLY = 2;
    public static final int SCHEDULE_YEARLY    = 3;
    public static final int SCHEDULE_NUM       = 4;

    public static String getScheduleName(int schedule) {
        switch(schedule) {
        case Finances.SCHEDULE_BIWEEKLY:
            return "Bi-Weekly";
        case Finances.SCHEDULE_MONTHLY:
            return "Monthly";
        case Finances.SCHEDULE_QUARTERLY:
            return "Quarterly";
        case Finances.SCHEDULE_YEARLY:
            return "Yearly";
        default:
            return "?";
        }
    }

	public Finances() {
		transactions = new ArrayList<Transaction>();
	    loans = new ArrayList<Loan>();
	    assets = new ArrayList<Asset>();
	    loanDefaults = 0;
	    failCollateral = 0;
	    wentIntoDebt = null;
	}

	public long getBalance() {
		long balance = 0;
		for(Transaction transaction : transactions) {
			balance += transaction.getAmount();
		}
		return balance;
	}

	public long getLoanBalance() {
	    long balance = 0;
	    for(Loan loan : loans) {
	        balance += loan.getRemainingValue();
	    }
	    return balance;
	}

	public boolean isInDebt() {
		return getLoanBalance() > 0;
	}

	public int getFullYearsInDebt(GregorianCalendar cal) {
	    if(null == wentIntoDebt) {
	        return 0;
	    }
	    return Utilities.getDiffFullYears(wentIntoDebt, cal);
	}

	public int getPartialYearsInDebt(GregorianCalendar cal) {
		if (wentIntoDebt == null) {
			return 0;
		}
		return Utilities.getDiffPartialYears(wentIntoDebt, cal);
	}

	public boolean debit(long amount, int category, String reason, Date date) {
	    if(getBalance() < amount) {
	        return false;
	    }
	    Transaction t = new Transaction(-1 * amount, category, reason, date);
		transactions.add(t);
		if(null != wentIntoDebt && !isInDebt()) {
            wentIntoDebt = null;
        }
        MekHQ.triggerEvent(new TransactionDebitEvent(t));
		return true;
	}

	public void credit(long amount, int category, String reason, Date date) {
	    Transaction t = new Transaction(amount, category, reason, date);
		transactions.add(t);
		if(null == wentIntoDebt && isInDebt()) {
		    wentIntoDebt = date;
		}
		MekHQ.triggerEvent(new TransactionCreditEvent(t));
	}

	/**
	 * This function will update the starting amount to the current balance
	 * and clear transactions
	 * By default, this will be called up on Jan 1 of every year in order to keep
	 * the transaction record from becoming too large
	 */
	public void newFiscalYear(Date date) {
		long carryover = getBalance();
		transactions = new ArrayList<Transaction>();
		credit(carryover, Transaction.C_START, "Carryover from previous year", date);
	}

	public ArrayList<Transaction> getAllTransactions() {
		return transactions;
	}

	public ArrayList<Loan> getAllLoans() {
	    return loans;
	}

	public ArrayList<Asset> getAllAssets() {
	    return assets;
	}

	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<finances>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<loanDefaults>"
                +loanDefaults
                +"</loanDefaults>");
		for(Transaction trans : transactions) {
			trans.writeToXml(pw1, indent+1);
		}
		for(Loan loan : loans) {
            loan.writeToXml(pw1, indent+1);
        }
		for(Asset asset : assets) {
            asset.writeToXml(pw1, indent+1);
        }
		if(null != wentIntoDebt) {
    		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "wentIntoDebt", df.format(wentIntoDebt));
		}
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</finances>");
	}

	public static Finances generateInstanceFromXML(Node wn) {
		Finances retVal = new Finances();
		NodeList nl = wn.getChildNodes();
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			 if (wn2.getNodeName().equalsIgnoreCase("transaction")) {
				 retVal.transactions.add(Transaction.generateInstanceFromXML(wn2));
			 }
			 else if (wn2.getNodeName().equalsIgnoreCase("loan")) {
                 retVal.loans.add(Loan.generateInstanceFromXML(wn2));
             }
			 else if (wn2.getNodeName().equalsIgnoreCase("asset")) {
                 retVal.assets.add(Asset.generateInstanceFromXML(wn2));
             }
			 else if (wn2.getNodeName().equalsIgnoreCase("loanDefaults")) {
                 retVal.loanDefaults = Integer.parseInt(wn2.getTextContent().trim());
             }
			 else if (wn2.getNodeName().equalsIgnoreCase("wentIntoDebt")) {
			     SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			     try {
			         retVal.wentIntoDebt = df.parse(wn2.getTextContent().trim());
			     } catch (DOMException e) {
			         // TODO Auto-generated catch block
			         e.printStackTrace();
			     } catch (ParseException e) {
			         // TODO Auto-generated catch block
			         e.printStackTrace();
			     }
			 }
		}

		return retVal;
	}

	public void addLoan(Loan loan) {
	    loans.add(loan);
	}

	public void newDay(Campaign campaign) {
	    ArrayList<Loan> newLoans = new ArrayList<Loan>();
	    for(Loan loan : loans) {
	        if(loan.checkLoanPayment(campaign.getCalendar())) {
	           if(debit(loan.getPaymentAmount(), Transaction.C_LOAN_PAYMENT, "loan payment to " + loan.getDescription(), campaign.getCalendar().getTime())) {
	               campaign.addReport("Your account has been debited for " + DecimalFormat.getInstance().format(loan.getPaymentAmount()) + " C-bills in loan payment to " + loan.getDescription());
	               loan.paidLoan();
	           } else {
                   campaign.addReport("<font color='red'><b>You have insufficient funds to service the debt on loan " + loan.getDescription() + "!</b></font> Funds required: " + DecimalFormat.getInstance().format(loan.getPaymentAmount()));
                   loan.setOverdue(true);
	           }
	        }
	        if(loan.getRemainingPayments() > 0) {
	            newLoans.add(loan);
	        } else {
	            campaign.addReport("You have fully paid off loan " + loan.getDescription());
	        }
	    }
	    if(null != wentIntoDebt && !isInDebt()) {
            wentIntoDebt = null;
        }
	    loans = newLoans;
	    for(Asset asset : assets) {
	        if(asset.getSchedule() == SCHEDULE_YEARLY && campaign.calendar.get(Calendar.DAY_OF_YEAR) == 1) {
	            credit(asset.getIncome(), Transaction.C_MISC, "income from " + asset.getName(), campaign.getCalendar().getTime());
                campaign.addReport("Your account has been credited for " + DecimalFormat.getInstance().format(asset.getIncome()) + " C-bills from " + asset.getName());
	        } else if(asset.getSchedule() == SCHEDULE_MONTHLY && campaign.calendar.get(Calendar.DAY_OF_MONTH) == 1) {
	            credit(asset.getIncome(), Transaction.C_MISC, "income from " + asset.getName(), campaign.getCalendar().getTime());
                campaign.addReport("Your account has been credited for " + DecimalFormat.getInstance().format(asset.getIncome()) + " C-bills from " + asset.getName());
	        }
	    }
	}

	public long checkOverdueLoanPayments(Campaign campaign) {
	    ArrayList<Loan> newLoans = new ArrayList<Loan>();
	    long overdueAmount = 0;
	    for(Loan loan : loans) {
            if(loan.isOverdue()) {
               if(debit(loan.getPaymentAmount(), Transaction.C_LOAN_PAYMENT, "loan payment " + loan.getDescription(), campaign.getCalendar().getTime())) {
                   campaign.addReport("Your account has been debited for " + DecimalFormat.getInstance().format(loan.getPaymentAmount()) + " C-bills in loan payment to " + loan.getDescription());
                   loan.paidLoan();
               } else {
                   overdueAmount += loan.getPaymentAmount();
               }
            }
            if(loan.getRemainingPayments() > 0) {
                newLoans.add(loan);
            } else {
                campaign.addReport("You have fully paid off loan " + loan.getDescription());
            }
	    }
	    loans = newLoans;
	    if(null != wentIntoDebt && !isInDebt()) {
            wentIntoDebt = null;
        }
	    return overdueAmount;
	}

	public void removeLoan(Loan loan) {
	    loans.remove(loan);
	    if(null != wentIntoDebt && !isInDebt()) {
            wentIntoDebt = null;
        }
	}

	public void defaultOnLoan(Loan loan, boolean paidCollateral) {
        loanDefaults++;
        if(!paidCollateral) {
            failCollateral++;
        }
        removeLoan(loan);
        MekHQ.triggerEvent(new LoanDefaultedEvent(loan));
    }

	public int getLoanDefaults() {
	    return loanDefaults;
	}

	public int getFailedCollateral() {
	    return failCollateral;
	}

	public long getTotalLoanCollateral() {
	    long amount = 0;
	    for(Loan loan : loans) {
	        amount += loan.getCollateralAmount();
	    }
	    return amount;
	}

	public long getTotalAssetValue() {
	    long amount = 0;
	    for(Asset asset : assets) {
	        amount += asset.getValue();
	    }
	    return amount;
	}

	public void setAssets(ArrayList<Asset> newAssets) {
	    assets = newAssets;
	}

	public long getMaxCollateral(Campaign c) {
        return c.getTotalEquipmentValue() + getTotalAssetValue() - getTotalLoanCollateral();
    }
}
