/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

/**
 * Injury levels are simple categorization values used in the display and prioritization of injuries.
 * The order is important: For display purposes, "later" injury levels are considered more important
 * and have preference over "earlier" ones.
 */
public enum InjuryLevel {
    /** Not actually a real injury */
    NONE,
    /**
     * Low-level chronic injuries and diseases, not threatening under normal circumstances.
     * Examples: Scars, tinnitus, diabetes, lost limbs
     */
    CHRONIC,
    /**
     * Simple injuries, expected to heal without complications by themselves in almost all cases.
     */
    MINOR,
    /**
     * Important injuries, professional medical attention required about weekly to ensure
     * proper healing.
     */
    MAJOR,
    /**
     * Life-threatening injuries, professional medical attention required on a daily base.
     */
    DEADLY;
}
