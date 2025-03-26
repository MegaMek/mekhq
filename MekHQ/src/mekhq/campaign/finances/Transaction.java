/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.finances;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Objects;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Transaction {
    private static final MMLogger logger = MMLogger.create(Transaction.class);

    // region Variable Declarations
    private TransactionType type;
    private LocalDate       date;
    private Money           amount;
    private String          description;
    // endregion Variable Declarations

    // region Constructors
    public Transaction() {
        this(TransactionType.MISCELLANEOUS, LocalDate.now(), Money.zero(), "");
    }

    public Transaction(final Transaction transaction) {
        this(transaction.getType(), transaction.getDate(), transaction.getAmount(), transaction.getDescription());
    }

    public Transaction(final TransactionType type, final LocalDate date, final Money amount, final String description) {
        setType(type);
        setDate(date);
        setAmount(amount);
        setDescription(description);
    }
    // endregion Constructors

    // region Getters/Setters
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
    // endregion Getters/Setters

    /**
     * I'd be better as part of the GUI class
     *
     * @since 0.50.04
     * @deprecated - Move to GUI Class
     */
    @Deprecated(since = "0.50.04")
    public String updateTransaction(Transaction previousTransaction) {
        return "Edited Transaction: {" +
               "Previous = " +
               previousTransaction.toString() +
               "} -> {New = " +
               toString() +
               "}";
    }

    /**
     * @since 0.50.04
     * @deprecated - Move to GUI Class
     */
    @Deprecated(since = "0.50.04")
    public String voidTransaction() {
        return "Deleted Transaction: " + toString();
    }

    // region File I/O
    protected void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "transaction");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", getType().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "date", getDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "amount", getAmount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "description", getDescription());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "transaction");
    }

    public static Transaction generateInstanceFromXML(final Node wn) {
        final Transaction transaction = new Transaction();
        final NodeList    nl          = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    transaction.setType(TransactionType.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    transaction.setDate(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("amount")) {
                    transaction.setAmount(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("description")) {
                    transaction.setDescription(MHQXMLUtility.unEscape(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        return transaction;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return getType() +
               ", " +
               MekHQ.getMHQOptions().getDisplayFormattedDate(getDate()) +
               ", " +
               getAmount() +
               ", " +
               getDescription();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof Transaction transaction) {
            return (getType() == transaction.getType()) &&
                   getDate().equals(transaction.getDate()) &&
                   getAmount().equals(transaction.getAmount()) &&
                   getDescription().equals(transaction.getDescription());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getDate(), getAmount(), getDescription());
    }
}
