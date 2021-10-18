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
import mekhq.campaign.finances.enums.TransactionType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Transaction implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -8772148858528954672L;

    private TransactionType type;
    private LocalDate date;
    private Money amount;
    private String description;
    //endregion Variable Declarations

    //region Constructors
    public Transaction() {
        this(TransactionType.MISCELLANEOUS, LocalDate.now(), Money.zero(), "");
    }

    public Transaction(final Transaction transaction) {
        this(transaction.getType(), transaction.getDate(), transaction.getAmount(),
                transaction.getDescription());
    }

    public Transaction(final TransactionType type, final LocalDate date, final Money amount,
                       final String description) {
        setType(type);
        setDate(date);
        setAmount(amount);
        setDescription(description);
    }
    //endregion Constructors

    //region Getters/Setters
    public TransactionType getType() {
        return type;
    }

    public void setType(final TransactionType type) {
        this.type = type;
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
        MekHqXmlUtil.writeSimpleXMLOpenTag(pw, indent++, "transaction");
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "type", getType().name());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "date", getDate());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "amount", getAmount().toXmlString());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "description", getDescription());
        MekHqXmlUtil.writeSimpleXMLCloseTag(pw, --indent, "transaction");
    }

    public static Transaction generateInstanceFromXML(final Node wn) {
        final Transaction transaction = new Transaction();
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    transaction.setType(TransactionType.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    transaction.setDate(MekHqXmlUtil.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("amount")) {
                    transaction.setAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("description")) {
                    transaction.setDescription(MekHqXmlUtil.unEscape(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("category")) { // Legacy - 0.49.4 Removal
                    transaction.setType(TransactionType.parseFromString(wn2.getTextContent().trim()));
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
        return getType() + ", " + MekHQ.getMekHQOptions().getDisplayFormattedDate(getDate())
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
            return (getType() == transaction.getType())
                    && getDate().equals(transaction.getDate())
                    && getAmount().equals(transaction.getAmount())
                    && getDescription().equals(transaction.getDescription());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getDate(), getAmount(), getDescription());
    }
}
