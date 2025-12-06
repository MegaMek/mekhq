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
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternate.MAXIMUM_INJURY_DURATION_MULTIPLIER;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.medical.BodyLocation;

/**
 * Service class responsible for determining the location and duration of new injuries when using the Alternate Advanced
 * Medical system.
 *
 * <p>This class encapsulates the logic for:</p>
 * <ul>
 *     <li>Selecting a primary {@link BodyLocation} for an injury based on a 2d6 roll and a set of fallback chains
 *     that respect missing limbs.</li>
 *     <li>Selecting a more granular secondary location and specific {@link InjuryType} based on the primary location
 *     and an additional roll.</li>
 *     <li>Handling special cases such as severed limbs and maximum injury duration multipliers.</li>
 *     <li>Tracking which major body locations are still present based on existing injuries that imply a missing
 *     location.</li>
 * </ul>
 *
 * <p>The resulting injury information is returned as an {@link InjuryLocationData} instance containing the injury
 * type, exact location, and duration multiplier.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class InjuryLocationService {
    private static final MMLogger LOGGER = MMLogger.create(InjuryLocationService.class);

    /**
     * Fallback chains used to determine a primary {@link BodyLocation} from a 2d6 roll. Each entry maps a roll result
     * to an ordered list of candidate locations. The first available (non-missing) location in the list is selected.
     *
     * @since 0.50.10
     */
    private static final Map<Integer, List<BodyLocation>> PRIMARY_LOCATION_FALLBACK_TABLE = Map.ofEntries(
          Map.entry(2, List.of(BodyLocation.HEAD, BodyLocation.CHEST)),
          Map.entry(3, List.of(BodyLocation.LEFT_FOOT, BodyLocation.LEFT_LEG, BodyLocation.ABDOMEN)),
          Map.entry(4, List.of(BodyLocation.LEFT_HAND, BodyLocation.LEFT_ARM, BodyLocation.CHEST)),
          Map.entry(5, List.of(BodyLocation.LEFT_ARM, BodyLocation.CHEST)),
          Map.entry(6, List.of(BodyLocation.ABDOMEN)),
          Map.entry(8, List.of(BodyLocation.CHEST)),
          Map.entry(9, List.of(BodyLocation.RIGHT_HAND, BodyLocation.RIGHT_ARM, BodyLocation.CHEST)),
          Map.entry(10, List.of(BodyLocation.RIGHT_ARM, BodyLocation.CHEST)),
          Map.entry(11, List.of(BodyLocation.RIGHT_FOOT, BodyLocation.RIGHT_LEG, BodyLocation.ABDOMEN)),
          Map.entry(12, List.of(BodyLocation.HEAD, BodyLocation.CHEST))
    );

    /**
     * Secondary location and injury table keyed by the primary {@link BodyLocation}. For each primary location, a d6
     * roll selects a specific {@link SecondaryLocation}, which includes the final injury type and the more precise body
     * location.
     *
     * @since 0.50.10
     */
    private static final Map<BodyLocation, Map<Integer, SecondaryLocation>> SECONDARY_LOCATION_TABLE = Map.ofEntries(
          Map.entry(BodyLocation.HEAD, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_FACE, BodyLocation.FACE),
                2, new SecondaryLocation(AlternateInjuries.HEARING_LOSS, BodyLocation.EARS),
                3, new SecondaryLocation(AlternateInjuries.BLINDNESS, BodyLocation.EYES),
                4, new SecondaryLocation(AlternateInjuries.FRACTURED_JAW, BodyLocation.JAW),
                5, new SecondaryLocation(AlternateInjuries.FRACTURED_SKULL, BodyLocation.SKULL)
          )),
          Map.entry(BodyLocation.CHEST, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURNED_CHEST, BodyLocation.CHEST),
                2, new SecondaryLocation(AlternateInjuries.FRACTURED_RIB, BodyLocation.RIBS),
                3, new SecondaryLocation(AlternateInjuries.SMOKE_INHALATION, BodyLocation.LUNGS),
                4, new SecondaryLocation(AlternateInjuries.PUNCTURED_LUNG, BodyLocation.LUNGS),
                5, new SecondaryLocation(AlternateInjuries.HEART_TRAUMA, BodyLocation.HEART)
          )),
          Map.entry(BodyLocation.ABDOMEN, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_ABDOMINAL, BodyLocation.ABDOMEN),
                2, new SecondaryLocation(AlternateInjuries.BRUISED_ORGAN, BodyLocation.ORGANS),
                3, new SecondaryLocation(AlternateInjuries.ORGAN_TRAUMA, BodyLocation.ORGANS),
                4, new SecondaryLocation(AlternateInjuries.FRACTURED_GROIN, BodyLocation.GROIN),
                5, new SecondaryLocation(AlternateInjuries.DISEMBOWELED, BodyLocation.ABDOMEN)
          )),
          Map.entry(BodyLocation.LEFT_ARM, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_UPPER_ARM, BodyLocation.UPPER_LEFT_ARM),
                2, new SecondaryLocation(AlternateInjuries.FRACTURED_UPPER_ARM, BodyLocation.UPPER_LEFT_ARM),
                3, new SecondaryLocation(AlternateInjuries.FRACTURED_ELBOW, BodyLocation.LEFT_ELBOW),
                4, new SecondaryLocation(AlternateInjuries.FRACTURED_SHOULDER, BodyLocation.LEFT_SHOULDER),
                5, new SecondaryLocation(AlternateInjuries.COMPOUND_FRACTURED_SHOULDER, BodyLocation.LEFT_SHOULDER)
          )),
          Map.entry(BodyLocation.RIGHT_ARM, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_UPPER_ARM, BodyLocation.UPPER_RIGHT_ARM),
                2, new SecondaryLocation(AlternateInjuries.FRACTURED_UPPER_ARM, BodyLocation.UPPER_RIGHT_ARM),
                3, new SecondaryLocation(AlternateInjuries.FRACTURED_ELBOW, BodyLocation.RIGHT_ELBOW),
                4, new SecondaryLocation(AlternateInjuries.FRACTURED_SHOULDER, BodyLocation.RIGHT_SHOULDER),
                5, new SecondaryLocation(AlternateInjuries.COMPOUND_FRACTURED_SHOULDER, BodyLocation.RIGHT_SHOULDER)
          )),
          Map.entry(BodyLocation.LEFT_HAND, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_HAND, BodyLocation.LEFT_HAND),
                2, new SecondaryLocation(AlternateInjuries.FRACTURED_HAND, BodyLocation.LEFT_HAND),
                3, new SecondaryLocation(AlternateInjuries.FRACTURED_WRIST, BodyLocation.LEFT_WRIST),
                4, new SecondaryLocation(AlternateInjuries.FRACTURED_FOREARM, BodyLocation.LEFT_FOREARM),
                5, new SecondaryLocation(AlternateInjuries.COMPOUND_FRACTURED_FOREARM, BodyLocation.LEFT_FOREARM)
          )),
          Map.entry(BodyLocation.RIGHT_HAND, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_HAND, BodyLocation.RIGHT_HAND),
                2, new SecondaryLocation(AlternateInjuries.FRACTURED_HAND, BodyLocation.RIGHT_HAND),
                3, new SecondaryLocation(AlternateInjuries.FRACTURED_WRIST, BodyLocation.RIGHT_WRIST),
                4, new SecondaryLocation(AlternateInjuries.FRACTURED_FOREARM, BodyLocation.RIGHT_FOREARM),
                5, new SecondaryLocation(AlternateInjuries.COMPOUND_FRACTURED_FOREARM, BodyLocation.RIGHT_FOREARM)
          )),
          Map.entry(BodyLocation.LEFT_LEG, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_THIGH, BodyLocation.LEFT_THIGH),
                2, new SecondaryLocation(AlternateInjuries.BRUISED_FEMUR, BodyLocation.LEFT_FEMUR),
                3, new SecondaryLocation(AlternateInjuries.FRACTURED_FEMUR, BodyLocation.LEFT_FEMUR),
                4, new SecondaryLocation(AlternateInjuries.COMPOUND_FRACTURED_FEMUR, BodyLocation.LEFT_FEMUR),
                5, new SecondaryLocation(AlternateInjuries.FRACTURED_HIP, BodyLocation.LEFT_HIP)
          )),
          Map.entry(BodyLocation.RIGHT_LEG, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_THIGH, BodyLocation.RIGHT_THIGH),
                2, new SecondaryLocation(AlternateInjuries.BRUISED_FEMUR, BodyLocation.RIGHT_FEMUR),
                3, new SecondaryLocation(AlternateInjuries.FRACTURED_FEMUR, BodyLocation.RIGHT_FEMUR),
                4, new SecondaryLocation(AlternateInjuries.COMPOUND_FRACTURED_FEMUR, BodyLocation.RIGHT_FEMUR),
                5, new SecondaryLocation(AlternateInjuries.FRACTURED_HIP, BodyLocation.RIGHT_HIP)
          )),
          Map.entry(BodyLocation.LEFT_FOOT, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_CALF, BodyLocation.LEFT_CALF),
                2, new SecondaryLocation(AlternateInjuries.FRACTURED_FOOT, BodyLocation.LEFT_FOOT),
                3, new SecondaryLocation(AlternateInjuries.FRACTURED_ANKLE, BodyLocation.LEFT_ANKLE),
                4, new SecondaryLocation(AlternateInjuries.FRACTURED_KNEE, BodyLocation.LEFT_KNEE),
                5, new SecondaryLocation(AlternateInjuries.COMPOUND_FRACTURED_SHIN, BodyLocation.LEFT_SHIN)
          )),
          Map.entry(BodyLocation.RIGHT_FOOT, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_CALF, BodyLocation.RIGHT_CALF),
                2, new SecondaryLocation(AlternateInjuries.FRACTURED_FOOT, BodyLocation.RIGHT_FOOT),
                3, new SecondaryLocation(AlternateInjuries.FRACTURED_ANKLE, BodyLocation.RIGHT_ANKLE),
                4, new SecondaryLocation(AlternateInjuries.FRACTURED_KNEE, BodyLocation.RIGHT_KNEE),
                5, new SecondaryLocation(AlternateInjuries.COMPOUND_FRACTURED_SHIN, BodyLocation.RIGHT_SHIN)
          ))
    );

    /**
     * Tracks whether the major body locations (head, limbs, hands, and feet) are still present. This is initialized
     * with all locations present and updated based on existing injuries and explicit calls to
     * {@link #setBodyLocationMissing(BodyLocation)}.
     *
     * @since 0.50.10
     */
    private final Map<BodyLocation, Boolean> limbPresence = new EnumMap<>(Map.ofEntries(
          Map.entry(BodyLocation.HEAD, true),
          Map.entry(BodyLocation.LEFT_ARM, true),
          Map.entry(BodyLocation.LEFT_HAND, true),
          Map.entry(BodyLocation.LEFT_LEG, true),
          Map.entry(BodyLocation.LEFT_FOOT, true),
          Map.entry(BodyLocation.RIGHT_ARM, true),
          Map.entry(BodyLocation.RIGHT_HAND, true),
          Map.entry(BodyLocation.RIGHT_LEG, true),
          Map.entry(BodyLocation.RIGHT_FOOT, true)
    ));

    /**
     * Marks a specific body location as missing.
     *
     * <p>Callers can use this to explicitly remove a limb or body part that is no longer present, in addition
     * to the automatic detection performed in the constructor.</p>
     *
     * @param location the {@link BodyLocation} to mark as missing
     *
     * @author Illiani
     * @since 0.50.10
     */
    public void setBodyLocationMissing(BodyLocation location) {
        limbPresence.put(location, false);
    }

    // Inner records

    /**
     * Simple value object storing the result of a secondary location roll: the d6 roll itself and the computed injury
     * duration multiplier.
     *
     * @param roll       the final non-exploding roll value (1–5)
     * @param multiplier the injury duration multiplier (1 to
     *                   {@link AdvancedMedicalAlternate#MAXIMUM_INJURY_DURATION_MULTIPLIER})
     *
     * @author Illiani
     * @since 0.50.10
     */
    private record InjuryDurationRoll(int roll, int multiplier) {}

    /**
     * Value object that associates a specific {@link InjuryType} with a more granular {@link BodyLocation}.
     *
     * @param injury   the resolved injury type for this secondary location
     * @param location the precise body location affected
     *
     * @author Illiani
     * @since 0.50.10
     */
    private record SecondaryLocation(InjuryType injury, BodyLocation location) {}

    // Everything else

    /**
     * Creates a new {@link InjuryLocationService} for the given person.
     *
     * <p>The constructor inspects the person's current injuries and updates {@link #limbPresence} to mark any
     * locations as missing if their injury type implies a missing location (for example, severed limbs).</p>
     *
     * @param person the {@link Person} whose current injuries should be used to initialize limb presence
     *
     * @author Illiani
     * @since 0.50.10
     */
    public InjuryLocationService(Person person) {
        updateLimbPresence(person.getInjuries());
    }

    /**
     * Updates {@link #limbPresence} based on a list of existing injuries.
     *
     * <p>Any injury whose {@link InjuryType#impliesMissingLocation()} returns {@code true} will cause its associated
     * {@link BodyLocation} to be marked as missing, provided that location is tracked in {@link #limbPresence}.</p>
     *
     * @param injuries the list of injuries to inspect
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateLimbPresence(List<Injury> injuries) {
        injuries.stream()
              .filter(injury -> injury.getType().impliesMissingLocation())
              .map(Injury::getLocation)
              .filter(limbPresence::containsKey)
              .forEach(location -> limbPresence.put(location, false));
    }

    /**
     * Generates a new {@link InjuryLocationData} instance representing a randomly determined injury location and
     * duration multiplier.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Selects a primary {@link BodyLocation} using
     *       {@link #getPrimaryBodyLocation()}.</li>
     *   <li>Resolves a secondary {@link BodyLocation} and {@link InjuryType}
     *       using {@link #getSecondaryBodyLocation(BodyLocation)}.</li>
     * </ol>
     *
     * @return a new {@link InjuryLocationData} describing the injury type, exact location, and duration multiplier
     *
     * @author Illiani
     * @since 0.50.10
     */
    public InjuryLocationData getInjuryLocation() {
        BodyLocation primaryLocation = getPrimaryBodyLocation();
        return getSecondaryBodyLocation(primaryLocation);
    }

    /**
     * Returns whether the given body location is considered present.
     *
     * <p>Locations tracked in {@link #limbPresence} will return the stored value. Any other locations default to
     * {@code true}, meaning they are assumed to be present unless explicitly marked or implied missing.</p>
     *
     * @param location the {@link BodyLocation} to check
     *
     * @return {@code true} if the location is present, {@code false} if it is missing
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean hasBodyLocation(BodyLocation location) {
        return limbPresence.getOrDefault(location, true);
    }

    /**
     * Determines the primary {@link BodyLocation} for a new injury.
     *
     * <p>This method rolls 2d6 and uses the result to select a fallback chain from
     * {@link #PRIMARY_LOCATION_FALLBACK_TABLE}. It then returns the first available (non-missing) location in that
     * chain.</p>
     *
     * <p>Special rules:</p>
     * <ul>
     *   <li>A roll of {@code 7} is treated as a leg hit and delegated to
     *       {@link #getAvailableLegLocation()}.</li>
     *   <li>If no fallback chain exists for the roll, an error is logged and
     *       {@link BodyLocation#CHEST} is returned.</li>
     * </ul>
     *
     * @return the selected primary {@link BodyLocation}
     *
     * @author Illiani
     * @since 0.50.10
     */
    private BodyLocation getPrimaryBodyLocation() {
        int roll = d6(2);

        // Special case for roll 7: randomly choose left or right leg
        if (roll == 7) {
            return getAvailableLegLocation();
        }

        // Get the fallback chain for this roll
        List<BodyLocation> fallbackChain = PRIMARY_LOCATION_FALLBACK_TABLE.get(roll);

        if (fallbackChain == null) {
            LOGGER.error("Unexpected body location roll: {}. Treating as CHEST", roll);
            return BodyLocation.CHEST;
        }

        // Find the first available location in the chain
        return findFirstAvailableLocation(fallbackChain);
    }

    /**
     * Selects an available leg location when the primary roll indicates a leg hit.
     *
     * <p>The method randomly prefers either the left or right leg, and then falls back to
     * {@link BodyLocation#ABDOMEN} if the preferred leg is not available.</p>
     *
     * @return the first available leg location, or {@link BodyLocation#ABDOMEN} if no leg is present
     *
     * @author Illiani
     * @since 0.50.10
     */
    private BodyLocation getAvailableLegLocation() {
        boolean isLeftSide = randomInt(2) == 0;
        BodyLocation preferredLeg = isLeftSide ? BodyLocation.LEFT_LEG : BodyLocation.RIGHT_LEG;

        return findFirstAvailableLocation(List.of(preferredLeg, BodyLocation.ABDOMEN));
    }

    /**
     * Returns the first body location in the provided list that is still present.
     *
     * <p>The method checks each location using {@link #hasBodyLocation} and returns the first one that is available.
     * If none of the supplied locations are present, {@link BodyLocation#CHEST} is returned as a safe default.</p>
     *
     * @param locations an ordered list of candidate {@link BodyLocation}s
     *
     * @return the first present location, or {@link BodyLocation#CHEST} if none are present
     *
     * @author Illiani
     * @since 0.50.10
     */
    private BodyLocation findFirstAvailableLocation(List<BodyLocation> locations) {
        return locations.stream()
                     .filter(this::hasBodyLocation)
                     .findFirst()
                     .orElse(BodyLocation.CHEST);
    }

    /**
     * Resolves the secondary injury location and injury type for a given primary location, including the injury
     * duration multiplier.
     *
     * <p>The process is:</p>
     * <ol>
     *     <li>Roll for duration and secondary location using
     *     {@link #rollForSecondaryLocationAndDurationMultiplier()}.</li>
     *     <li>If the primary location is a limb and the multiplier is at the maximum value, attempt to treat the
     *     injury as a severed limb or head by selecting an appropriate {@link InjuryType}.</li>
     *     <li>Otherwise, look up the secondary location entry in {@link #SECONDARY_LOCATION_TABLE} for the primary
     *     location and rolled value.</li>
     *     <li>If no matching entry is found, default to a burned chest injury.</li>
     * </ol>
     *
     * @param primaryLocation the primary {@link BodyLocation} previously determined for the injury
     *
     * @return an {@link InjuryLocationData} containing the resolved injury type, secondary location, and duration
     *       multiplier
     *
     * @author Illiani
     * @since 0.50.10
     */
    private InjuryLocationData getSecondaryBodyLocation(BodyLocation primaryLocation) {
        InjuryDurationRoll durationAndLocationRoll = rollForSecondaryLocationAndDurationMultiplier();

        boolean isSevered = primaryLocation.isLimb()
                                  && (durationAndLocationRoll.multiplier() == MAXIMUM_INJURY_DURATION_MULTIPLIER);

        SecondaryLocation secondaryLocation = null;
        if (isSevered) {
            InjuryType newInjury = switch (primaryLocation) {
                case HEAD -> AlternateInjuries.SEVERED_HEAD;
                case LEFT_ARM, RIGHT_ARM -> AlternateInjuries.SEVERED_ARM;
                case LEFT_HAND, RIGHT_HAND -> AlternateInjuries.SEVERED_HAND;
                case LEFT_LEG, RIGHT_LEG -> AlternateInjuries.SEVERED_LEG;
                case LEFT_FOOT, RIGHT_FOOT -> AlternateInjuries.SEVERED_FOOT;
                default -> {
                    LOGGER.error("Unexpected severed body location: {}. Treating as non-severed", primaryLocation);
                    yield null;
                }
            };

            if (newInjury != null) {
                secondaryLocation = new SecondaryLocation(newInjury, primaryLocation);
            }
        }

        if (secondaryLocation == null) {
            secondaryLocation = SECONDARY_LOCATION_TABLE
                                      .getOrDefault(primaryLocation, Map.of())
                                      .getOrDefault(durationAndLocationRoll.roll(),
                                            new SecondaryLocation(AlternateInjuries.BURNED_CHEST,
                                                  BodyLocation.CHEST));
        }

        return new InjuryLocationData(secondaryLocation.injury,
              secondaryLocation.location,
              durationAndLocationRoll.multiplier());
    }

    /**
     * Rolls for the secondary location index and calculates the injury duration multiplier, with exploding sixes.
     *
     * <p>Rules:</p>
     * <ul>
     *     <li>Start with a multiplier of 1 and a single d6 roll.</li>
     *     <li>While the roll is 6 and the multiplier is below
     *     {@link AdvancedMedicalAlternate#MAXIMUM_INJURY_DURATION_MULTIPLIER}, increment the multiplier and roll
     *     again.</li>
     *     <li>If a roll of 6 occurs when the multiplier is already at the maximum, the final roll value is forced to
     *     a number between 1 and 5 (inclusive) so that it can be used as a secondary-location table index.</li>
     * </ul>
     *
     * @return an {@link InjuryDurationRoll} holding both the final roll (1–5) and the computed duration multiplier
     *
     * @author Illiani
     * @since 0.50.10
     */
    private InjuryDurationRoll rollForSecondaryLocationAndDurationMultiplier() {
        int injuryDurationMultiplier = 1;
        int roll = d6(1);

        while (roll == 6 && injuryDurationMultiplier < MAXIMUM_INJURY_DURATION_MULTIPLIER) {
            injuryDurationMultiplier++;
            roll = d6(1);
        }

        // If we hit max multiplier with a roll of 6, force a result between 1 and 5
        if (roll == 6 && injuryDurationMultiplier == MAXIMUM_INJURY_DURATION_MULTIPLIER) {
            roll = randomInt(5) + 1;
        }

        return new InjuryDurationRoll(roll, injuryDurationMultiplier);
    }
}
