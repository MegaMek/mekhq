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

import static mekhq.campaign.personnel.medical.BodyLocation.HEAD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.BodyLocation;

public class AdvancedMedicalAlternate {
    private static final MMLogger LOGGER = MMLogger.create(AdvancedMedicalAlternate.class);

    static final int MAXIMUM_INJURY_DURATION_MULTIPLIER = 3;

    private static final Map<BodyLocation, BodyRegion> BODY_LOCATION_BODY_REGION_MAP = Map.ofEntries(
          Map.entry(HEAD, BodyRegion.HEAD),
          Map.entry(BodyLocation.LEFT_HAND, BodyRegion.LEFT_HAND),
          Map.entry(BodyLocation.LEFT_ARM, BodyRegion.LEFT_ARM),
          Map.entry(BodyLocation.RIGHT_HAND, BodyRegion.RIGHT_HAND),
          Map.entry(BodyLocation.RIGHT_ARM, BodyRegion.RIGHT_ARM),
          Map.entry(BodyLocation.LEFT_FOOT, BodyRegion.LEFT_FOOT),
          Map.entry(BodyLocation.LEFT_LEG, BodyRegion.LEFT_LEG),
          Map.entry(BodyLocation.RIGHT_FOOT, BodyRegion.RIGHT_FOOT),
          Map.entry(BodyLocation.RIGHT_LEG, BodyRegion.RIGHT_LEG)
    );

    private enum BodyRegion {
        HEAD, LEFT_ARM, LEFT_HAND, RIGHT_ARM, RIGHT_HAND,
        LEFT_LEG, LEFT_FOOT, RIGHT_LEG, RIGHT_FOOT, OTHER
    }

    public static Collection<Injury> generateInjuriesFromHits(Campaign campaign, Person person, int hits) {
        List<Injury> newInjuries = new ArrayList<>();

        InjuryLocationService injuryLocationService = new InjuryLocationService(person);
        for (int i = 0; i < hits; i++) {
            InjuryLocationData injuryLocation = injuryLocationService.getInjuryLocation();
            Injury injury = pickInjury(campaign, person, injuryLocation);
            if (injury == null) {
                continue;
            }

            newInjuries.add(injury);
            // Update the limb presence map if we just removed a limb. From this point onwards we will no longer
            // generate injuries for that location
            if (injury.getType().impliesMissingLocation()) {
                injuryLocationService.setBodyLocationMissing(injury.getLocation());
            }
        }

        return newInjuries;
    }

    private static @Nullable Injury pickInjury(Campaign campaign, Person person,
          InjuryLocationData injuryLocationData) {
        InjuryType injuryType = injuryLocationData.injuryType();
        BodyLocation bodyLocation = injuryLocationData.bodyLocation();
        int injuryDurationMultiplier = injuryLocationData.injuryDurationMultiplier();

        Injury newInjury = injuryType.newInjury(campaign, person, bodyLocation, injuryDurationMultiplier);
        if (newInjury == null) {
            LOGGER.error("Failed to generate injury of type {} at body location {} with duration multiplier {}",
                  injuryType, bodyLocation, injuryDurationMultiplier);
        }

        return newInjury;
    }

    public static void purgeIllogicalInjuries(Person person) {
        List<Injury> injuries = person.getInjuries();

        // Collect all severed locations
        Set<BodyLocation> severedLocations = EnumSet.noneOf(BodyLocation.class);
        for (Injury injury : injuries) {
            if (injury.getType().impliesMissingLocation()) {
                severedLocations.add(injury.getLocation());
            }
        }

        if (severedLocations.isEmpty()) {
            return;
        }

        // Collect injuries to remove (can't remove while iterating)
        List<Injury> injuriesToRemove = new ArrayList<>();
        for (Injury injury : injuries) {
            if (!injury.getType().impliesMissingLocation() && isInSeveredLocation(injury, severedLocations)) {
                injuriesToRemove.add(injury);
            }
        }

        // Remove each injury
        for (Injury injury : injuriesToRemove) {
            person.removeInjury(injury);
        }
    }

    private static boolean isInSeveredLocation(Injury injury, Set<BodyLocation> severedLocations) {
        BodyLocation injuryLocation = injury.getLocation();

        for (BodyLocation severedLocation : severedLocations) {
            if (injuryLocation.isChildOf(severedLocation) || injuryLocation == severedLocation) {
                return true;
            }
        }

        return false;
    }

    public static List<InjuryEffect> getAllActiveInjuryEffects(boolean isAmbidextrous, List<Injury> currentInjuries) {
        if (currentInjuries.isEmpty()) {
            return List.of(InjuryEffect.NONE);
        }

        // Group effects by body region
        Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion = new EnumMap<>(BodyRegion.class);
        for (BodyRegion region : BodyRegion.values()) {
            effectsByRegion.put(region, EnumSet.noneOf(InjuryEffect.class));
        }

        collectEffectsByRegion(currentInjuries, effectsByRegion);
        processSeverance(effectsByRegion, isAmbidextrous);

        // If head is severed, nothing else matters
        if (effectsByRegion.get(BodyRegion.HEAD).contains(InjuryEffect.SEVERED)) {
            return List.of(InjuryEffect.SEVERED);
        }

        // Merge limb subsections and resolve fracture precedence
        mergeLimbEffects(effectsByRegion);

        // Combine all effects and sort
        List<InjuryEffect> allEffects = effectsByRegion.values().stream()
                                              .flatMap(Collection::stream)
                                              .sorted()
                                              .toList();

        return allEffects.isEmpty() ? List.of(InjuryEffect.NONE) : new ArrayList<>(allEffects);
    }

    private static void collectEffectsByRegion(List<Injury> injuries,
          Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion) {
        for (Injury injury : injuries) {
            InjuryEffect effect = injury.getInjuryEffect();
            if (effect == InjuryEffect.NONE) {
                continue;
            }

            BodyRegion region = determineBodyRegion(injury.getLocation());
            effectsByRegion.get(region).add(effect);
        }
    }

    private static BodyRegion determineBodyRegion(BodyLocation location) {
        for (Map.Entry<BodyLocation, BodyRegion> entry : BODY_LOCATION_BODY_REGION_MAP.entrySet()) {
            if (location.isChildOf(entry.getKey())) {
                return entry.getValue();
            }
        }
        return BodyRegion.OTHER;
    }

    private static void processSeverance(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion,
          boolean isAmbidextrous) {
        // Handle ambidextrous arm compensation
        processArmSeverance(effectsByRegion, isAmbidextrous);

        // Severed leg includes foot
        processSeveredLimb(effectsByRegion, BodyRegion.LEFT_LEG, BodyRegion.LEFT_FOOT);
        processSeveredLimb(effectsByRegion, BodyRegion.RIGHT_LEG, BodyRegion.RIGHT_FOOT);

        // Process individual severed hands/feet/head
        for (BodyRegion region : List.of(BodyRegion.HEAD, BodyRegion.LEFT_HAND, BodyRegion.RIGHT_HAND,
              BodyRegion.LEFT_FOOT, BodyRegion.RIGHT_FOOT)) {
            EnumSet<InjuryEffect> effects = effectsByRegion.get(region);
            if (effects.contains(InjuryEffect.SEVERED)) {
                replaceWithSevered(effects);
            }
        }
    }

    private static void processArmSeverance(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion,
          boolean isAmbidextrous) {
        boolean leftArmSevered = effectsByRegion.get(BodyRegion.LEFT_ARM).contains(InjuryEffect.SEVERED);
        boolean rightArmSevered = effectsByRegion.get(BodyRegion.RIGHT_ARM).contains(InjuryEffect.SEVERED);

        if (isAmbidextrous && leftArmSevered != rightArmSevered) {
            // Only one arm severed - ambidextrous can compensate, remove the effect
            if (leftArmSevered) {
                effectsByRegion.get(BodyRegion.LEFT_ARM).remove(InjuryEffect.SEVERED);
            } else {
                effectsByRegion.get(BodyRegion.RIGHT_ARM).remove(InjuryEffect.SEVERED);
            }
        } else {
            // Both arms severed, no arms severed, or not ambidextrous - process normally
            processSeveredLimb(effectsByRegion, BodyRegion.LEFT_ARM, BodyRegion.LEFT_HAND);
            processSeveredLimb(effectsByRegion, BodyRegion.RIGHT_ARM, BodyRegion.RIGHT_HAND);
        }
    }

    private static void processSeveredLimb(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion,
          BodyRegion mainLimb, BodyRegion subLimb) {
        if (effectsByRegion.get(mainLimb).contains(InjuryEffect.SEVERED)) {
            effectsByRegion.get(subLimb).clear();
            replaceWithSevered(effectsByRegion.get(mainLimb));
        }
    }

    private static void replaceWithSevered(EnumSet<InjuryEffect> effects) {
        effects.clear();
        effects.add(InjuryEffect.SEVERED);
    }

    private static void mergeLimbEffects(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion) {
        mergeLimbAndResolve(effectsByRegion, BodyRegion.LEFT_ARM, BodyRegion.LEFT_HAND);
        mergeLimbAndResolve(effectsByRegion, BodyRegion.RIGHT_ARM, BodyRegion.RIGHT_HAND);
        mergeLimbAndResolve(effectsByRegion, BodyRegion.LEFT_LEG, BodyRegion.LEFT_FOOT);
        mergeLimbAndResolve(effectsByRegion, BodyRegion.RIGHT_LEG, BodyRegion.RIGHT_FOOT);
    }

    private static void mergeLimbAndResolve(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion,
          BodyRegion mainLimb, BodyRegion subLimb) {
        EnumSet<InjuryEffect> mainEffects = effectsByRegion.get(mainLimb);
        mainEffects.addAll(effectsByRegion.get(subLimb));

        if (mainEffects.contains(InjuryEffect.COMPOUND_FRACTURE)) {
            mainEffects.remove(InjuryEffect.FRACTURE_LIMB);
        }
    }

    public static boolean hasInjuryOfType(List<Injury> injuries, InjuryType injuryType) {
        for (Injury injury : injuries) {
            if (injury.getType() == injuryType) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes the specified injury from this person and updates any associated benefits granted by permanent
     * modifications (prosthetics/implants).
     *
     * <p>Behavior details:</p>
     * <ul>
     *   <li>Deletes the given {@link Injury} from the internal injury list.</li>
     *   <li>Invokes {@link AdvancedMedicalAlternate#removeAssociatedInjuryOptions(Injury, List, PersonnelOptions)}
     *   to disable personnel/pilot options that were provided exclusively by the removed prosthetic or implant.</li>
     *   <li>Fires a {@link PersonChangedEvent} to notify the rest of the system.</li>
     * </ul>
     *
     * <p>Note: Only options that are not still provided by other existing prosthetics/implants will be disabled.</p>
     *
     * @param injury the injury instance to remove; if not present, the method is a no-op
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static void removeAssociatedInjuryOptions(Injury injury, List<Injury> injuries, PersonnelOptions options) {
        InjuryType injuryType = injury.getType();
        ProstheticType prostheticType = ProstheticType.getProstheticFromInjury(injuryType);

        if (prostheticType != null) {
            Set<String> benefitsToRemove = new HashSet<>(prostheticType.getAssociatedPersonnelOptions());
            benefitsToRemove.addAll(prostheticType.getAssociatedPilotOptions());

            if (!benefitsToRemove.isEmpty()) {
                Set<String> providedByOthers = new HashSet<>();
                for (Injury otherInjury : injuries) {
                    ProstheticType otherProstheticType = ProstheticType.getProstheticFromInjury(otherInjury.getType());
                    if (otherProstheticType == null) {
                        continue;
                    }
                    providedByOthers.addAll(otherProstheticType.getAssociatedPersonnelOptions());
                    providedByOthers.addAll(otherProstheticType.getAssociatedPilotOptions());
                }

                // Remove any benefits still granted by other prosthetics
                benefitsToRemove.removeAll(providedByOthers);

                // Apply removals only for benefits no longer granted
                if (!benefitsToRemove.isEmpty()) {
                    for (String ability : benefitsToRemove) {
                        IOption option = options.getOption(ability);
                        if (option != null) {
                            option.setValue(false);
                        }
                    }
                }
            }
        }
    }
}
