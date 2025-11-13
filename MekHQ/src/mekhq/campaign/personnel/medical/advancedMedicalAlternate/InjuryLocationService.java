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

public class InjuryLocationService {
    private static final MMLogger LOGGER = MMLogger.create(InjuryLocationService.class);

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

    private static final Map<BodyLocation, Map<Integer, SecondaryLocation>> SECONDARY_LOCATION_TABLE = Map.ofEntries(
          Map.entry(BodyLocation.HEAD, Map.of(
                1, new SecondaryLocation(AlternateInjuries.BURN_FACE, BodyLocation.HEAD),
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
                2, new SecondaryLocation(AlternateInjuries.BRUISED_ORGAN, BodyLocation.CHEST),
                3, new SecondaryLocation(AlternateInjuries.ORGAN_TRAUMA, BodyLocation.CHEST),
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

    public void setBodyLocationMissing(BodyLocation location) {
        limbPresence.put(location, false);
    }

    // Inner records
    private record InjuryDurationRoll(int roll, int multiplier) {}

    private record SecondaryLocation(InjuryType injury, BodyLocation location) {}

    // Everything else
    public InjuryLocationService(Person person) {
        updateLimbPresence(person.getInjuries());
    }

    private void updateLimbPresence(List<Injury> injuries) {
        injuries.stream()
              .filter(injury -> injury.getType().impliesMissingLocation())
              .map(Injury::getLocation)
              .filter(limbPresence::containsKey)
              .forEach(location -> limbPresence.put(location, false));
    }

    public InjuryLocationData getInjuryLocation() {
        BodyLocation primaryLocation = getPrimaryBodyLocation();
        return getSecondaryBodyLocation(primaryLocation);
    }

    public boolean hasBodyLocation(BodyLocation location) {
        return limbPresence.getOrDefault(location, true);
    }

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

    private BodyLocation getAvailableLegLocation() {
        boolean isLeftSide = randomInt(2) == 0;
        BodyLocation preferredLeg = isLeftSide ? BodyLocation.LEFT_LEG : BodyLocation.RIGHT_LEG;

        return findFirstAvailableLocation(List.of(preferredLeg, BodyLocation.ABDOMEN));
    }

    private BodyLocation findFirstAvailableLocation(List<BodyLocation> locations) {
        return locations.stream()
                     .filter(this::hasBodyLocation)
                     .findFirst()
                     .orElse(BodyLocation.CHEST);
    }

    private InjuryLocationData getSecondaryBodyLocation(BodyLocation primaryLocation) {
        InjuryDurationRoll durationRoll = calculateInjuryDuration();

        boolean isSevered = primaryLocation.isLimb()
                                  && (durationRoll.multiplier() == MAXIMUM_INJURY_DURATION_MULTIPLIER);

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
                                      .getOrDefault(durationRoll.roll(),
                                            new SecondaryLocation(AlternateInjuries.BURNED_CHEST,
                                                  BodyLocation.CHEST));
        }

        return new InjuryLocationData(secondaryLocation.injury, secondaryLocation.location, durationRoll.multiplier());
    }

    private InjuryDurationRoll calculateInjuryDuration() {
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
