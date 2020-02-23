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

import java.text.Collator;
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
    private char[] a;
    private int aLength;
    private int indexA = 0;
    private char[] b;
    private int bLength;
    private int indexB = 0;

    private static Collator collator = Collator.getInstance();

    /**
     * Compares its two arguments for order.
     *
     * @param stringA the first string to compare by
     * @param stringB the second string to compare by
     * @return negative integer if the first argument is less than the second
     *         zero if they are the same
     *         positive integer if the first argument is greater than the second
     */
    public int compare(String stringA, String stringB) {
        this.a = stringA.toCharArray();
        this.aLength = stringA.length();
        this.b = stringB.toCharArray();
        this.bLength = stringB.length();

        while ((indexA < aLength) && (indexB < bLength)) {
            int comparison;
            if (isNumber(a[indexA]) && isNumber(b[indexB])) {
                comparison = compareIntSections();
            } else {
                comparison = collator.compare(a[indexA], b[indexB]);
            }

            if (comparison != 0) {
                return comparison;
            }

            indexA++;
            indexB++;
        }

        return aLength - bLength;
    }

    private int compareStringSections() {
        return 0;
    }

    /**
     * this compares integer section of a string to determine sorting order
     * @return negative integer if the first argument is less than the second
     *         zero if they are the same
     *         positive integer if the first argument is greater than the second
     */
    private int compareIntSections() {
        StringBuilder sbA = new StringBuilder();
        sbA.append(a[indexA]);
        StringBuilder sbB = new StringBuilder();
        sbB.append(b[indexB]);

        // Here we are incrementing the index, then comparing it to the length
        // Then, we determine if the char is a number, and continue adding to the StringBuilder
        // until we've got the two numbers to compare
        while ((++indexA < aLength) && (isNumber(a[indexA]))) {
            sbA.append(a[indexA]);
        }
        while ((++indexB < bLength) && (isNumber(b[indexB]))) {
            sbB.append(b[indexB]);
        }

        return Integer.compare(Integer.parseInt(sbA.toString()), Integer.parseInt(sbB.toString()));
    }

    /**
     * This is used to determine if a character is a number or not, based on its ASCII value
     *
     * @param c the character to check
     * @return true if it is a number, otherwise false
     */
    private boolean isNumber(char c) {
        return (((int) c <= 57) && ((int) c >= 48));
    }
}
