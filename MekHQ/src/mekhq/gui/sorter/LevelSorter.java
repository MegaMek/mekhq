/*
 * Copyright (c) 2013 - The MegaMek Team
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

import java.util.Comparator;

/**
 * A comparator for skills levels (e.g. Regular, Veteran, etc)
 *   * @author Jay Lawson
 *
 */
public class LevelSorter implements Comparator<String> {

    @Override
    public int compare(String s0, String s1) {
        if(s0.equals("-") && s1.equals("-")) {
            return 0;
        } else if(s0.equals("-")) {
            return -1;
        } else if(s1.equals("-")) {
            return 1;
        } else {
            // TODO : Switch these to instead use RandomSkillGenerator.levelNames
            //probably easiest to turn into numbers and then sort that way
            int l0 = 0;
            int l1 = 0;
            if(s0.contains("Green")) {
                l0 = 2;
            }
            if(s1.contains("Green")) {
                l1 = 2;
            }
            // Ultra-Green has to be below Green when using String.contains() because it contains Green
            if(s0.contains("Ultra-Green")) {
                l0 = 1;
            }
            if(s1.contains("Ultra-Green")) {
                l1 = 1;
            }
            if(s0.contains("Regular")) {
                l0 = 3;
            }
            if(s1.contains("Regular")) {
                l1 = 3;
            }
            if(s0.contains("Veteran")) {
                l0 = 4;
            }
            if(s1.contains("Veteran")) {
                l1 = 4;
            }
            if(s0.contains("Elite")) {
                l0 = 5;
            }
            if(s1.contains("Elite")) {
                l1 = 5;
            }
            return ((Comparable<Integer>)l0).compareTo(l1);
        }
    }
}
