/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static megamek.common.compute.Compute.d6;

import java.util.Map;

import mekhq.campaign.personnel.InjuryType;

/**
 * Service class for disease generation and management.
 *
 * <p>This class provides functionality for randomly generating diseases and their durations using roll-based tables.
 * Diseases are categorized by type and severity, with specific injury types determined through a two-stage random
 * process.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class DiseaseService {
    /**
     * Maps 2d6 roll results to disease type categories.
     */
    private static final Map<Integer, DiseaseType> DISEASE_TYPE_TABLE = Map.ofEntries(
          Map.entry(2, DiseaseType.GROWTHS),
          Map.entry(3, DiseaseType.INFECTION),
          Map.entry(4, DiseaseType.HEARING_LOSS),
          Map.entry(5, DiseaseType.WEAKNESS),
          Map.entry(6, DiseaseType.WEEPING_SORES),
          Map.entry(7, DiseaseType.FLU_LIKE),
          Map.entry(8, DiseaseType.VENEREAL),
          Map.entry(9, DiseaseType.EYESIGHT_LOSS),
          Map.entry(10, DiseaseType.TREMORS),
          Map.entry(11, DiseaseType.BREATHING_DIFFICULTIES),
          Map.entry(12, DiseaseType.HEMOPHILIA)
    );

    /**
     * Maps disease types to specific injury types based on a d6 roll.
     */
    private static final Map<DiseaseType, Map<Integer, InjuryType>> SPECIFIC_DISEASE_TABLE = Map.ofEntries(
          Map.entry(DiseaseType.GROWTHS, Map.of(
                1, AlternateInjuries.GROWTHS_DISCOMFORT,
                2, AlternateInjuries.GROWTHS_DISCOMFORT,
                3, AlternateInjuries.GROWTHS_SLIGHT,
                4, AlternateInjuries.GROWTHS_MODERATE,
                5, AlternateInjuries.GROWTHS_SEVERE,
                6, AlternateInjuries.GROWTHS_DEADLY
          )),
          Map.entry(DiseaseType.INFECTION, Map.of(
                1, AlternateInjuries.INFECTION_DISCOMFORT,
                2, AlternateInjuries.INFECTION_DISCOMFORT,
                3, AlternateInjuries.INFECTION_SLIGHT,
                4, AlternateInjuries.INFECTION_MODERATE,
                5, AlternateInjuries.INFECTION_SEVERE,
                6, AlternateInjuries.INFECTION_DEADLY
          )),
          Map.entry(DiseaseType.HEARING_LOSS, Map.of(
                1, AlternateInjuries.HEARING_DISCOMFORT,
                2, AlternateInjuries.HEARING_DISCOMFORT,
                3, AlternateInjuries.HEARING_SLIGHT,
                4, AlternateInjuries.HEARING_MODERATE,
                5, AlternateInjuries.HEARING_SEVERE,
                6, AlternateInjuries.HEARING_DEADLY
          )),
          Map.entry(DiseaseType.WEAKNESS, Map.of(
                1, AlternateInjuries.WEAKNESS_DISCOMFORT,
                2, AlternateInjuries.WEAKNESS_DISCOMFORT,
                3, AlternateInjuries.WEAKNESS_SLIGHT,
                4, AlternateInjuries.WEAKNESS_MODERATE,
                5, AlternateInjuries.WEAKNESS_SEVERE,
                6, AlternateInjuries.WEAKNESS_DEADLY
          )),
          Map.entry(DiseaseType.WEEPING_SORES, Map.of(
                1, AlternateInjuries.SORES_DISCOMFORT,
                2, AlternateInjuries.SORES_DISCOMFORT,
                3, AlternateInjuries.SORES_SLIGHT,
                4, AlternateInjuries.SORES_MODERATE,
                5, AlternateInjuries.SORES_SEVERE,
                6, AlternateInjuries.SORES_DEADLY
          )),
          Map.entry(DiseaseType.FLU_LIKE, Map.of(
                1, AlternateInjuries.FLU_DISCOMFORT,
                2, AlternateInjuries.FLU_DISCOMFORT,
                3, AlternateInjuries.FLU_SLIGHT,
                4, AlternateInjuries.FLU_MODERATE,
                5, AlternateInjuries.FLU_SEVERE,
                6, AlternateInjuries.FLU_DEADLY
          )),
          Map.entry(DiseaseType.VENEREAL, Map.of(
                1, AlternateInjuries.VENEREAL_DISCOMFORT,
                2, AlternateInjuries.VENEREAL_DISCOMFORT,
                3, AlternateInjuries.VENEREAL_SLIGHT,
                4, AlternateInjuries.VENEREAL_MODERATE,
                5, AlternateInjuries.VENEREAL_SEVERE,
                6, AlternateInjuries.VENEREAL_DEADLY
          )),
          Map.entry(DiseaseType.EYESIGHT_LOSS, Map.of(
                1, AlternateInjuries.SIGHT_DISCOMFORT,
                2, AlternateInjuries.SIGHT_DISCOMFORT,
                3, AlternateInjuries.SIGHT_SLIGHT,
                4, AlternateInjuries.SIGHT_MODERATE,
                5, AlternateInjuries.SIGHT_SEVERE,
                6, AlternateInjuries.SIGHT_DEADLY
          )),
          Map.entry(DiseaseType.TREMORS, Map.of(
                1, AlternateInjuries.TREMORS_DISCOMFORT,
                2, AlternateInjuries.TREMORS_DISCOMFORT,
                3, AlternateInjuries.TREMORS_SLIGHT,
                4, AlternateInjuries.TREMORS_MODERATE,
                5, AlternateInjuries.TREMORS_SEVERE,
                6, AlternateInjuries.TREMORS_DEADLY
          )),
          Map.entry(DiseaseType.BREATHING_DIFFICULTIES, Map.of(
                1, AlternateInjuries.BREATHING_DISCOMFORT,
                2, AlternateInjuries.BREATHING_DISCOMFORT,
                3, AlternateInjuries.BREATHING_SLIGHT,
                4, AlternateInjuries.BREATHING_MODERATE,
                5, AlternateInjuries.BREATHING_SEVERE,
                6, AlternateInjuries.BREATHING_DEADLY
          )),
          Map.entry(DiseaseType.HEMOPHILIA, Map.of(
                1, AlternateInjuries.HEMOPHILIA_DISCOMFORT,
                2, AlternateInjuries.HEMOPHILIA_DISCOMFORT,
                3, AlternateInjuries.HEMOPHILIA_SLIGHT,
                4, AlternateInjuries.HEMOPHILIA_MODERATE,
                5, AlternateInjuries.HEMOPHILIA_SEVERE,
                6, AlternateInjuries.HEMOPHILIA_DEADLY
          ))
    );

    /**
     * Generates a random disease with random severity.
     *
     * <p>This method uses a two-stage random process:</p>
     * <ol>
     *   <li>Roll 2d6 to determine the disease type category</li>
     *   <li>Roll 1d6 to determine severity within that category</li>
     * </ol>
     *
     * @return a randomly determined InjuryType representing a disease
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static InjuryType catchRandomDisease() {
        int diseaseTypeRoll = d6(2);
        int actualDiseaseRoll = d6(1);

        DiseaseType type = DISEASE_TYPE_TABLE.get(diseaseTypeRoll);
        return SPECIFIC_DISEASE_TABLE.get(type).get(actualDiseaseRoll);
    }

    /**
     * Generates a random duration for a disease in days.
     *
     * @return the disease duration in days
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int getDiseaseDuration() {
        int roll = d6(1);
        return switch (roll) {
            case 1 -> 7;
            case 2, 3 -> 14;
            case 4, 5 -> 21;
            default -> 28; // a roll of 6
        };
    }
}
