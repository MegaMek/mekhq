/*
 * Transaction.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All Rights Reserved.
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Vector;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Transaction implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -8772148858528954672L;

    private int category;
    private LocalDate date;
    private Money amount;
    private String description;
    //endregion Variable Declarations

    //region TransactionType Enum
    // TODO : Windchild : Enum Me
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
    public static final int C_RANSOM         = 16;
    public final static int C_NUM            = 17;

    @Deprecated // Replace with Enum
    public String getCategoryName() {
        return getCategoryName(getCategory());
    }

    @Deprecated // Replace with Enum
    public static String getCategoryName(int cat) {
        switch (cat) {
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
            case C_RANSOM:
                return "Ransom";
            default:
                return "Unknown category";
        }
    }

    @Deprecated // Replace with Enum
    public static Vector<String> getCategoryList() {
        Vector<String> out = new Vector<>();

        for (int i = 0; i < C_NUM; i++) {
            out.add(Transaction.getCategoryName(i));
        }
        Collections.sort(out);
        return out;
    }

    @Deprecated // Replace with Enum
    public static int getCategoryIndex(String name) {
        for (int i = 0; i < getCategoryList().size(); i++) {
            if (getCategoryName(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }
    //endregion TransactionType Enum

    //region Constructors
    public Transaction() {
        this(C_MISC, LocalDate.now(), Money.zero(), "");
    }

    public Transaction(final Transaction transaction) {
        this(transaction.getCategory(), transaction.getDate(), transaction.getAmount(),
                transaction.getDescription());
    }

    public Transaction(final int category, final LocalDate date, final Money amount,
                       final String description) {
        setCategory(category);
        setDate(date);
        setAmount(amount);
        setDescription(description);
    }
    //endregion Constructors

    //region Getters/Setters
    public int getCategory() {
        return category;
    }

    public void setCategory(final int category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(final LocalDate date) {
        this.date = date;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(final Money amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
    //endregion Getters/Setters

    @Deprecated // I'd be better as part of the GUI class
    public String updateTransaction(Transaction previousTransaction) {
        return "Edited Transaction: {" +
                "Previous = " + previousTransaction.toString() +
                "} -> {New = " + toString() + "}";
    }

    @Deprecated // I'd be better as part of the GUI class
    public String voidTransaction() {
        return "Deleted Transaction: " + toString();
    }

    //region File I/O
    protected void writeToXML(final PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "transaction");
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "category", getCategory());
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "date", getDate());
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "amount", getAmount().toXmlString());
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "description", getDescription());
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "transaction");
    }

    public static Transaction generateInstanceFromXML(final Node wn) {
        final Transaction transaction = new Transaction();
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("category")) {
                    transaction.setCategory(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    transaction.setDate(MekHqXmlUtil.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("amount")) {
                    transaction.setAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("description")) {
                    transaction.setDescription(MekHqXmlUtil.unEscape(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
        return transaction;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return getCategoryName() + ", " + MekHQ.getMekHQOptions().getDisplayFormattedDate(getDate())
                + ", " + getAmount() + ", " + getDescription();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof Transaction) {
            final Transaction transaction = (Transaction) other;
            return (getCategory() == transaction.getCategory())
                    && getDate().equals(transaction.getDate())
                    && getAmount().equals(transaction.getAmount())
                    && getDescription().equals(transaction.getDescription());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCategory(), getDate(), getAmount(), getDescription());
    }
}
