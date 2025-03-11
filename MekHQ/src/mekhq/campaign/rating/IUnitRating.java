/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.rating;

import java.math.BigDecimal;
import java.math.RoundingMode;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @since 3/12/2012
 */
public interface IUnitRating {

    public static final int PRECISION = 5;
    public static final RoundingMode HALF_EVEN = RoundingMode.HALF_EVEN;
    public static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

    // TODO : Add an array for each level of this, then use it across MekHQ instead of a bunch of random lists
    public static final int DRAGOON_F = ForceDescriptor.RATING_0;
    public static final int DRAGOON_D = ForceDescriptor.RATING_1;
    public static final int DRAGOON_C = ForceDescriptor.RATING_2;
    public static final int DRAGOON_B = ForceDescriptor.RATING_3;
    public static final int DRAGOON_A = ForceDescriptor.RATING_4;
    public static final int DRAGOON_ASTAR = ForceDescriptor.RATING_5;

    void reInitialize();

    //TODO: some of these methods should be static functions

    /**
     * Returns the static constant representation of the passed in Unit rating.
     *
     * @param score The total Dragoon's score.
     * @return
     */
    int getUnitRating(int score);

    /**
     * Returns the static constant representation of the computed Unit/Dragoon's rating as an integer.
     *
     * @return
     */
    int getUnitRatingAsInteger();

    int getScore();

    int getModifier();

    /**
     * Returns the letter code of the passed in Unit rating.
     *
     * @param rating The numeric rating to be converted.
     * @return
     */
    String getUnitRatingName(int rating);

    /**
     * Calculates the force's Unit rating and returns the appropriate letter code.
     *
     * @return
     */
    String getUnitRating();

    /**
     * Returns the Unit Rating score for the force's average experience level.
     *
     * @return
     */
    int getExperienceValue();

    /**
     * Returns the unit's average experience level.
     *
     * @return
     */
    SkillLevel getAverageExperience();

    /**
     * Returns the Unit Rating score for the force's commander.
     *
     * @return
     */
    int getCommanderValue();

    /**
     * Return's the commander of the force.
     *
     * @return
     */
    Person getCommander();

    /**
     * Returns the Unit Rating score for the force's contract success/failure record.
     *
     * @return
     */
    int getCombatRecordValue();

    /**
     * Returns the percentage of units that are properly supported.
     *
     * @return
     */
    BigDecimal getSupportPercent();

    /**
     * Returns the Unit Rating score for the force's ratio of support to combat units.
     *
     * @return
     */
    int getSupportValue();

    /**
     * Returns the percentage of units that can be transported without outside help.
     *
     * @return
     */
    BigDecimal getTransportPercent();

    /**
     * Returns the Unit Rating score for the force's ratio of transportation available to transportation needs.
     *
     * @return
     */
    int getTransportValue();

    /**
     * Returns the Unit Rating score for the percentage of combat units greater than L1 tech.
     *
     * @return
     */
    int getTechValue();

    /**
     * Returns the Unit Rating score for the force's financial record.  If the unit has never been in debt, a value of 0
     * is returned.  If the unit has been in debt, a negative number will be returned.
     *
     * @return
     */
    int getFinancialValue();

    /**
     * Returns a text description of how the Unit rating was calculated.
     *
     * @return
     */
    String getDetails();

    /**
     * Returns descriptive text that should be displayed in a Help/About dialog to inform users of the means by which
     * the Unit rating is calculated.
     *
     * @return
     */
    String getHelpText();

    /**
     * Returns the typs of unit rating method used.
     * @return
     */
    UnitRatingMethod getUnitRatingMethod();
}
