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
import java.util.ArrayList;
import java.util.Date;

import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	
	public Finances() {
		transactions = new ArrayList<Transaction>();
	}
	
	public long getBalance() {
		long balance = 0;
		for(Transaction transaction : transactions) {
			balance += transaction.getAmount();
		}
		return balance;
	}
	
	public boolean isInDebt() {
		return getBalance() < 0;
	}
	
	public void debit(long amount, int category, String reason, Date date) {
		transactions.add(new Transaction(-1 * amount, category, reason, date));
	}
	
	public void credit(long amount, int category, String reason, Date date) {
		transactions.add(new Transaction(amount, category, reason, date));
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
		if(carryover < 0) {
			debit(carryover, Transaction.C_START, "Carryover from previous year", date);
		} else if(carryover > 0) {
			credit(carryover, Transaction.C_START, "Carryover from previous year", date);
		}
	}
	
	public ArrayList<Transaction> getAllTransactions() {
		return transactions;
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<finances>");
		for(Transaction trans : transactions) {
			trans.writeToXml(pw1, indent+1);
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
		}
		
		return retVal;
	}
}