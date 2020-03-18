/*
 * Copyright (c) 2020 The MegaMek Team. All rights reserved.
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
package mekhq.campaign.personnel;

public class Phenotype {
    // You MUST modify the values in data/names/bloodnames/bloodnames.xml if you modify these values
    public static final int P_NONE = 0; // No phenotype
    public static final int P_MECHWARRIOR = 1;
    public static final int P_ELEMENTAL = 2;
    public static final int P_AEROSPACE = 3;
    public static final int P_VEHICLE = 4;
    public static final int P_PROTOMECH = 5;
    public static final int P_NAVAL = 6;
    public static final int P_GENERAL = 7; // This is used during Bloodname generation, and shouldn't be saved to file
    public static final int P_NUM = 7; // This should be EQUAL to P_GENERAL

    /**
     * @param phenotype the phenotype to get the name for
     * @return the name of the phenotype, or ? if unknown
     */
    public static String getPhenotypeName(int phenotype) {
        switch (phenotype) {
            case P_NONE:
                return "Freeborn";
            case P_MECHWARRIOR:
                return "Trueborn MechWarrior";
            case P_ELEMENTAL:
                return "Trueborn Elemental";
            case P_AEROSPACE:
                return "Trueborn Aerospace Pilot";
            case P_VEHICLE:
                return "Trueborn Vehicle Crew";
            case P_PROTOMECH:
                return "Trueborn ProtoMech Pilot";
            case P_NAVAL:
                return "Trueborn Naval Commander";
            default:
                return "?";
        }
    }

    /**
     * This is used to get the name of the Phenotype grouping a Bloodname is part of
     * @param phenotype the phenotype to get the name for
     * @return the name of the grouping, or ? if unknown
     */
    public static String getBloodnamePhenotypeGroupingName(int phenotype) {
        switch (phenotype) {
            case P_NONE:
                return "None";
            case P_MECHWARRIOR:
                return "MechWarrior";
            case P_ELEMENTAL:
                return "Elemental";
            case P_AEROSPACE:
                return "Aerospace Pilot";
            case P_VEHICLE:
                return "Vehicle Crew";
            case P_PROTOMECH:
                return "ProtoMech Pilot";
            case P_NAVAL:
                return "Naval Commander";
            case P_GENERAL:
                return "General";
            default:
                return "?";
        }
    }

    /**
     * @param phenotype the phenotype int to check
     * @return whether the phenotype is trueborn or freeborn
     */
    public static String getPhenotypeShortName(int phenotype) {
        switch (phenotype) {
            case P_NONE:
                return "Freeborn";
            case P_MECHWARRIOR:
            case P_ELEMENTAL:
            case P_AEROSPACE:
            case P_VEHICLE:
            case P_PROTOMECH:
            case P_NAVAL:
                return "Trueborn";
            default:
                return "?";
        }
    }
}
