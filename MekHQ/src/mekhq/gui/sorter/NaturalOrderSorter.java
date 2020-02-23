/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
package mekhq.gui.sorter;

import sun.nio.cs.US_ASCII;

import java.util.Comparator;

/**
 * Created by Justin "Windchild" Bowen, February 22nd, 2020.
 * Inspired by the Alphanum Algorithm by Dave Koelle and Pierre-Luc Paour's Natural Order Comparator
 *
 * A comparator that compares the inputs based on natural sort order.
 *
 * Natural sort order is an easier to parse format that counts multi-digit numbers atomically (as a
 * single number)
 *
 * Windows File Explorer uses this format for files as it is more human-friendly, but
 * ASCII sort order is more common in computer programs because of the ease of programming in that
 * order.
 *
 * To showcase how this works, below is an example:
 * The list of Strings { "Atlas 0", "Atlas 15", "Atlas 2", "Atlas 1", "Atlas 5" }
 * would be sorted into { "Atlas 0", "Atlas 1", "Atlas 2", "Atlas 5", "Atlas 15" }
 * instead of ASCII's { "Atlas 0", "Atlas 1", "Atlas 15", "Atlas 2", "Atlas 5" }
 */
public class NaturalOrderSorter implements Comparator<String> {
    // Variable Declarations
    String a;
    int indexA;
    int aLength;
    String b;
    int indexB;
    int bLength;

    public int compare(String stringA, String stringB) {
        this.a = stringA;
        this.indexA = 0;
        this.aLength = stringA.length();
        this.b = stringB;
        this.indexB = 0;
        this.bLength = stringB.length();



        return stringA.length() - stringB.length();
    }

    /**
     *
     * @return 0 if the two are the same, 1 if the A should be sorted first, or 2 if B should be
     *         sorted first
     */
    public byte compareIntSections() {
        int incA;
        int incB;

        if ()
    }

    /** This is used to determine if a character is a number or not, based on its ASCII value
     *
     * @param c the character to check
     * @return
     */
    private boolean isNumber(char c) {
        return (((int) c <= 57) && ((int) c >= 48));
    }
}
