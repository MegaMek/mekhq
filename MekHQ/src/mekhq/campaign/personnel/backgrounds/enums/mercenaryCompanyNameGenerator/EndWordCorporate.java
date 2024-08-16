/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator;

import mekhq.MekHQ;

import java.util.Random;
import java.util.ResourceBundle;

public enum EndWordCorporate {
    ADVISORY("EndWordCorporate.ADVISORY.text"),
    ALLIANCE("EndWordCorporate.ALLIANCE.text"),
    ASSOCIATES("EndWordCorporate.ASSOCIATES.text"),
    CAPITAL("EndWordCorporate.CAPITAL.text"),
    CONSORTIUM("EndWordCorporate.CONSORTIUM.text"),
    CONSULTING("EndWordCorporate.CONSULTING.text"),
    CORPORATION("EndWordCorporate.CORPORATION.text"),
    DEVELOPMENT("EndWordCorporate.DEVELOPMENT.text"),
    DYNAMICS("EndWordCorporate.DYNAMICS.text"),
    ENGINEERING("EndWordCorporate.ENGINEERING.text"),
    ENTERPRISES("EndWordCorporate.ENTERPRISES.text"),
    GLOBAL("EndWordCorporate.GLOBAL.text"),
    GROUP("EndWordCorporate.GROUP.text"),
    HOLDINGS("EndWordCorporate.HOLDINGS.text"),
    INFRASTRUCTURE("EndWordCorporate.INFRASTRUCTURE.text"),
    INNOVATIONS("EndWordCorporate.INNOVATIONS.text"),
    INTEGRATION("EndWordCorporate.INTEGRATION.text"),
    INVESTMENTS("EndWordCorporate.INVESTMENTS.text"),
    LOGISTICS("EndWordCorporate.LOGISTICS.text"),
    MANAGEMENT("EndWordCorporate.MANAGEMENT.text"),
    NETWORKS("EndWordCorporate.NETWORKS.text"),
    OPERATIONS("EndWordCorporate.OPERATIONS.text"),
    PARTNERS("EndWordCorporate.PARTNERS.text"),
    RESOURCES("EndWordCorporate.RESOURCES.text"),
    SERVICES("EndWordCorporate.SERVICES.text"),
    SOLUTIONS("EndWordCorporate.SOLUTIONS.text"),
    STRATEGIES("EndWordCorporate.STRATEGIES.text"),
    SYSTEMS("EndWordCorporate.SYSTEMS.text"),
    TECHNOLOGIES("EndWordCorporate.TECHNOLOGIES.text"),
    VENTURES("EndWordCorporate.VENTURES.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String word;
    //endregion Variable Declarations

    //region Constructors
    EndWordCorporate(final String word) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RandomMercenaryCompanyNameGenerator",
        MekHQ.getMHQOptions().getLocale());
        this.word = resources.getString(word);
    }

    private static final Random RANDOM = new Random();
    //endregion Constructors

    @Override
    public String toString() {
        return word;
    }

    /**
     * @return a random word from the CloserMercenary enum.
     */
    public static String getRandomWord() {
        EndWordCorporate[] words = EndWordCorporate.values();
        EndWordCorporate randomWord = words[RANDOM.nextInt(words.length)];
        return randomWord.toString();
    }
}
