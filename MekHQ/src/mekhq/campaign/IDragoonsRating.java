/*
 * IMrbcRating.java
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
package mekhq.campaign;

import java.math.BigDecimal;
import java.math.RoundingMode;

import mekhq.campaign.personnel.Person;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %I% %G%
 * @since 3/12/2012
 */
public interface IDragoonsRating {
    
    public static final int PRECISION = 5;
    public static final RoundingMode HALF_EVEN = RoundingMode.HALF_EVEN;
    public static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

    public static final int DRAGOON_F = 0;
    public static final int DRAGOON_D = 1;
    public static final int DRAGOON_C = 2;
    public static final int DRAGOON_B = 3;
    public static final int DRAGOON_A = 4;
    public static final int DRAGOON_ASTAR = 5;

    public void reInitialize();

    //TODO: some of these methods should be static functions
    
    /**
     * Returns the static constant representation of the passed in Dragoon's rating.
     *
     * @param score The total Dragoon's score.
     * @return
     */
    public int getDragoonRating(int score);

    /**
     * Returns the static constant representation of the passed in Dragoon's rating as an integer.
     *
     * @param score The total Dragoon's score.
     * @return
     */
    public int getDragoonsRatingAsInteger();
    
    public int getScore();
    
    public int getModifier();
    
    /**
     * Returns the letter code of the passed in Dragoon's rating.
     *
     * @param rating The numeric rating to be converted.
     * @return
     */
    public String getDragoonRatingName(int rating);

    /**
     * Calculates the force's Dragoon's rating and returns the appropriate letter code.
     *
     * @return
     */
    public String getDragoonRating();

    /**
     * Returns the Dragoon's score for the force's average experience level.
     *
     * @return
     */
    public int getExperienceValue();

    /**
     * Returns the unit's average experience level.
     *
     * @return
     */
    public String getAverageExperience();

    /**
     * Returns the Dragoon's score for the force's commander.
     *
     * @return
     */
    public int getCommanderValue();

    /**
     * Return's the commander of the force.
     *
     * @return
     */
    public Person getCommander();

    /**
     * Returns the Dragoon's score for the force's contract success/failure record.
     *
     * @return
     */
    public int getCombatRecordValue();

    /**
     * Returns the percentage of units that are properly supported.
     *
     * @return
     */
    public BigDecimal getSupportPercent();

    /**
     * Returns the Dragoon's score for the force's ratio of support to combat units.
     *
     * @return
     */
    public int getSupportValue();

    /**
     * Returns the percentage of units that can be transported without outside help.
     *
     * @return
     */
    public BigDecimal getTransportPercent();

    /**
     * Returns the Dragoon's score for the force's ratio of transportation available to transportation needs.
     *
     * @return
     */
    public int getTransportValue();

    /**
     * Returns the Dragoon's score for the percentage of combat units greater than L1 tech.
     *
     * @return
     */
    public int getTechValue();

    /**
     * Returns the Dragoon's score for the force's financial record.  If the unit has never been in debt,
     * a value of 0 is returned.  If the unit has been in debt, a negative number will be returned.
     *
     * @return
     */
    public int getFinancialValue();

    /**
     * Returns a text description of how the MRBC rating was calculated.
     * @return
     */
    public String getDetails();

    /**
     * Returns descriptive text that should be displayed in a Help/About dialog to inform users of the means by which
     * the Dragoon's rating is calculated.
     * @return
     */
    public String getHelpText();

}
