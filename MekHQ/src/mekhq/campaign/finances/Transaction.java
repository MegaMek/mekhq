/*
 * Transaction.java
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
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Transaction implements Serializable {
	
	private static final long serialVersionUID = -8772148858528954672L;
	
	public final static int C_MISC       = 0;
	public final static int C_EQUIP      = 1;
	public final static int C_UNIT       = 2;
	public final static int C_SALARY     = 3;
	public final static int C_OVERHEAD   = 4;
	public final static int C_MAINTAIN   = 5;
	public final static int C_UNIT_SALE  = 6;
	public final static int C_EQUIP_SALE = 7;
	public final static int C_START      = 8;
	public final static int C_NUM        = 9;
	
	public static String getCategoryName(int cat) {
		switch(cat) {
		case C_MISC:
			return "Miscellaneous";
		case C_EQUIP:
			return "Equipment Purchases";
		case C_UNIT:
			return "Unit Purchases";
		case C_SALARY:
			return "Salary Payments";
		case C_OVERHEAD:
			return "Overhead Expenses";
		case C_MAINTAIN:
			return "Maintenance Expenses";
		case C_UNIT_SALE:
			return "Unit Sales";
		case C_EQUIP_SALE:
			return "Equipment Sales";
		case C_START:
			return "Starting Capital";
		default:
			return "Unknown category";
		}
	}
	
	private long amount;
	private String description;
	private Date date;
	private int category;
	
	public Transaction(long a, int c, String d, Date dt) {
		amount = a;
		category = c;
		description = d;
		date = dt;
	}
	
	public long getAmount() {
		return amount;
	}
	
	public void setAmount(long a) {
		this.amount = a;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String s) {
		this.description = s;
	}
	
	public int getCategory() {
		return category;
	}
	
	public void setCategory(int c) {
		this.category = c;
	}
	
	public String getCategoryName() {
		return getCategoryName(getCategory());
	}
	
	public Date getDate() {
		return date;
	}
}