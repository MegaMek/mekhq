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

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Vector;

import mekhq.MekHqXmlUtil;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Transaction implements Serializable {
	
	private static final long serialVersionUID = -8772148858528954672L;
	
	public final static int C_MISC            = 0;
	public final static int C_EQUIP           = 1;
	public final static int C_UNIT            = 2;
	public final static int C_SALARY          = 3;
	public final static int C_OVERHEAD        = 4;
	public final static int C_MAINTAIN        = 5;
	public final static int C_UNIT_SALE       = 6;
	public final static int C_EQUIP_SALE      = 7;
	public final static int C_START           = 8;
	public final static int C_TRANSPORT       = 9;
	public final static int C_CONTRACT       = 10;
	public final static int C_BLC            = 11;
	public final static int C_SALVAGE        = 12;
	public final static int C_LOAN_PRINCIPAL = 13;
	public final static int C_LOAN_PAYMENT   = 14;
    public final static int C_REPAIRS        = 15;
    public final static int C_NUM            = 16;

    public static Vector<String> getCategoryList() {
        Vector<String> out = new Vector<>();

        for (int i = 0; i < C_NUM; i++) {
            out.add(Transaction.getCategoryName(i));
        }
        Collections.sort(out);
        return out;
    }
	
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
		case C_CONTRACT:
			return "Contract payments";
		case C_BLC:
			return "Battle Loss Compensation";
		case C_SALVAGE:
			return "Salvage Exchange";
		case C_START:
			return "Starting Capital";
		case C_TRANSPORT:
			return "Transportation";
		case C_LOAN_PRINCIPAL:
		    return "Loan Principal";
		case C_LOAN_PAYMENT:
		    return "Loan Payment";
		case C_REPAIRS:
            return "Repairs";
		default:
			return "Unknown category";
		}
	}

	private Money amount;
	private String description;
	private Date date;
	private int category;
	
	public Transaction() {
		this(Money.zero(),-1,"",null);
	}
	
	public Transaction(Money a, int c, String d, Date dt) {
		amount = a;
		category = c;
		description = d;
		date = dt;
	}
	
	public Transaction(Transaction t) {
	    //copy constructor
	    this.amount = t.amount;
	    this.category = t.category;
	    this.description = t.description;
	    this.date = t.date;
	}
	
    public static int getCategoryIndex(String name) {
        for (int i = 0; i < getCategoryList().size(); i++) {
            if (getCategoryName(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getAmount(),
                getCategory(),
                getDate(),
                getDescription());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Transaction other = (Transaction) obj;
        if (amount != other.amount)
            return false;
        if (category != other.category)
            return false;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (description == null) {
            return other.description == null;
        } else {
            return description.equals(other.description);
        }
    }

    public Money getAmount() {
		return amount;
	}
	
	public void setAmount(Money a) {
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

    public void setDate(Date date) {
        this.date = date;
    }

	protected void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<transaction>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<amount>"
				+amount.toXmlString()
				+"</amount>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<description>"
				+MekHqXmlUtil.escape(description)
				+"</description>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<category>"
				+category
				+"</category>");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<date>"
				+df.format(date)
				+"</date>");
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</transaction>");
	}

	public static Transaction generateInstanceFromXML(Node wn) {
		Transaction retVal = new Transaction();
		
		NodeList nl = wn.getChildNodes();
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("amount")) {
			    retVal.amount = Money.fromXmlString(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("category")) {
				retVal.category = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("description")) {
				retVal.description = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("date")) {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				try {
					retVal.date = df.parse(wn2.getTextContent().trim());
				} catch (DOMException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
		}
		return retVal;
	}

    public String updateTransaction(Transaction previousTransaction) {
        return  "Edited Transaction: {" +
                "Previous = " + previousTransaction.toText() +
                "} -> {New = " + toText() + "}";
    }

    public String voidTransaction() {
        return "Deleted Transaction: " + toText();
    }

    public String toText() {
        return new SimpleDateFormat("MM/dd/yyyy").format(getDate()) + ", " + getCategoryName() + ", " + getDescription() + ", " + getAmount();
    }
}
