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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;

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
	
	public void debit(long amount, String reason, GregorianCalendar date) {
		transactions.add(new Transaction(-1 * amount, reason, date));
	}
	
	public void credit(long amount, String reason, GregorianCalendar date) {
		transactions.add(new Transaction(amount, reason, date));
	}
}