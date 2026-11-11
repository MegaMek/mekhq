/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jakarta.annotation.Nullable;
import megamek.common.CriticalSlot;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;

/**
 * Works out a refit's class from the Campaign Operations customization rules (pp. 211-212), and keeps the reason for
 * every change that sets a class, so the player can be shown why a refit is as big as it is.
 *
 * <p>The class of a refit is the highest class of any one change in it. For example, refitting a WVR-6R Wolverine
 * to the WVR-6M adds armor (Class A), adds two lasers to the right arm where the autocannon came out (Class B), and
 * adds heat sinks and moves jump jets elsewhere (Class C), so the whole refit is Class C.</p>
 */
public class RefitClassGrader {
    private static final MMLogger LOGGER = MMLogger.create(RefitClassGrader.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    /**
     * One change in a refit and the class it calls for.
     *
     * @param refitClass the class this change calls for, one of the {@link Refit} class constants
     * @param reason     the change, in words the player can read
     */
    public record RefitClassStep(int refitClass, String reason) {}

    /**
     * One reason in a class's group of reasons.
     *
     * @param location the location the reason is about, for pod work, or {@code null} when the text says it all
     * @param text     the reason, in words the player can read
     */
    public record ReasonItem(@Nullable String location, String text) {}

    /**
     * The reasons that set one class, for the briefing to list under that class.
     *
     * @param refitClass the class, one of the {@link Refit} class constants
     * @param className  the class as the player reads it
     * @param items      the reasons, each listed once
     */
    public record ReasonGroup(int refitClass, String className, List<ReasonItem> items) {}

    private final List<RefitClassStep> steps = new ArrayList<>();
    private int refitClass = Refit.NO_CHANGE;
    // Pods going in and coming out, by location, so a pod swap can say what each location becomes
    private final Map<String, Map<String, Integer>> podsIn = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> podsOut = new LinkedHashMap<>();

    /**
     * @return the highest class of any change graded so far
     */
    public int getRefitClass() {
        return refitClass;
    }

    /**
     * @return every graded change, in the order it was graded
     */
    public List<RefitClassStep> getSteps() {
        return List.copyOf(steps);
    }

    /**
     * Lists the reasons for the refit's class, the biggest changes first, each with its class name. A change made
     * several times in the same way is listed once.
     *
     * @return one line for each different reason
     */
    public List<String> describeSteps() {
        List<String> lines = new ArrayList<>();
        for (ReasonGroup group : describeByClass()) {
            for (ReasonItem item : group.items()) {
                String reason = (item.location() == null) ? item.text()
                                      : getFormattedTextAt(RESOURCE_BUNDLE, "RefitClass.pods.location",
                                            item.location(), item.text());
                lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "RefitClass.step", group.className(), reason));
            }
        }
        return lines;
    }

    /**
     * Groups the reasons by the class they set, the biggest class first. Pod work is listed by location, saying what
     * each location's pods become.
     *
     * @return one group for each class that has reasons
     */
    public List<ReasonGroup> describeByClass() {
        Map<Integer, Set<ReasonItem>> itemsByClass = new TreeMap<>(Comparator.reverseOrder());
        for (RefitClassStep step : steps) {
            itemsByClass.computeIfAbsent(step.refitClass(), key -> new LinkedHashSet<>())
                  .add(new ReasonItem(null, step.reason()));
        }
        Set<String> podLocations = new LinkedHashSet<>(podsOut.keySet());
        podLocations.addAll(podsIn.keySet());
        for (String location : podLocations) {
            itemsByClass.computeIfAbsent(Refit.CLASS_OMNI, key -> new LinkedHashSet<>())
                  .add(new ReasonItem(location, describePodChange(location)));
        }
        List<ReasonGroup> groups = new ArrayList<>();
        for (Map.Entry<Integer, Set<ReasonItem>> classItems : itemsByClass.entrySet()) {
            groups.add(new ReasonGroup(classItems.getKey(), Refit.getRefitClassName(classItems.getKey()),
                  List.copyOf(classItems.getValue())));
        }
        return groups;
    }

    /**
     * Describes each location's pod changes, for example {@code In the Left Arm: ER PPC becomes ER Large Laser, ER
     * Micro Laser}.
     *
     * @return one description for each location whose pods change, in the order the locations were graded
     */
    public List<String> describePodChanges() {
        Set<String> locations = new LinkedHashSet<>(podsOut.keySet());
        locations.addAll(podsIn.keySet());
        List<String> descriptions = new ArrayList<>();
        for (String location : locations) {
            descriptions.add(getFormattedTextAt(RESOURCE_BUNDLE, "RefitClass.pods.location", location,
                  describePodChange(location)));
        }
        return descriptions;
    }

    /**
     * @return what one location's pods become, for example {@code ER PPC becomes ER Large Laser, ER Micro Laser}
     */
    private String describePodChange(String location) {
        String goingIn = listPods(podsIn.get(location));
        String comingOut = listPods(podsOut.get(location));
        if (goingIn.isEmpty()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "RefitClass.pods.remove", comingOut);
        }
        if (comingOut.isEmpty()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "RefitClass.pods.add", goingIn);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "RefitClass.pods.swap", comingOut, goingIn);
    }

    private static String listPods(@Nullable Map<String, Integer> podCounts) {
        if (podCounts == null) {
            return "";
        }
        List<String> pods = new ArrayList<>();
        for (Map.Entry<String, Integer> pod : podCounts.entrySet()) {
            boolean isSingle = pod.getValue() == 1;
            pods.add(isSingle ? pod.getKey()
                           : getFormattedTextAt(RESOURCE_BUNDLE, "RefitClass.pods.count", pod.getValue(),
                                 pod.getKey()));
        }
        return String.join(", ", pods);
    }

    private void raise(int stepClass, String reasonKey, Object... arguments) {
        String reason = getFormattedTextAt(RESOURCE_BUNDLE, reasonKey, arguments);
        steps.add(new RefitClassStep(stepClass, reason));
        raiseClass(stepClass, reason);
    }

    private void raiseClass(int stepClass, String reason) {
        if (stepClass > refitClass) {
            LOGGER.debug("[RefitClass] {} raises the refit to {}", reason, Refit.getRefitClassName(stepClass));
            refitClass = stepClass;
        }
    }

    /**
     * Grades a component added to a location. Adding where something was removed in the same refit is Class B,
     * adding where nothing was removed is Class C, and a component that must take slots in more than one location
     * (such as a null-signature system) is Class D.
     *
     * @param partName                the component added
     * @param locationName            where it goes
     * @param isSomethingRemovedThere {@code true} if the refit takes something else out of that location
     * @param spansLocations          {@code true} if the component must take slots in more than one location
     */
    public void gradeAddedComponent(String partName, String locationName, boolean isSomethingRemovedThere,
          boolean spansLocations) {
        if (spansLocations) {
            raise(Refit.CLASS_D, "RefitClass.added.spansLocations", partName);
        } else if (isSomethingRemovedThere) {
            raise(Refit.CLASS_B, "RefitClass.added.whereRemoved", partName, locationName);
        } else {
            raise(Refit.CLASS_C, "RefitClass.added.whereNothingRemoved", partName, locationName);
        }
    }

    /**
     * Grades a component that stays on the unit but moves. Turning it to face another way in the same location is
     * Class B; moving it to another location is Class C.
     *
     * @param partName       the component
     * @param fromLocation   where it was
     * @param toLocation     where it goes
     * @param isSameLocation {@code true} if only its facing changes
     */
    public void gradeMovedComponent(String partName, String fromLocation, String toLocation,
          boolean isSameLocation) {
        if (isSameLocation) {
            raise(Refit.CLASS_B, "RefitClass.facing", partName, toLocation);
        } else {
            raise(Refit.CLASS_C, "RefitClass.moved", partName, fromLocation, toLocation);
        }
    }

    /** Taking components off the unit is Class A. */
    public void gradeRemovedComponents() {
        raise(Refit.CLASS_A, "RefitClass.removed");
    }

    /** Changing armor type, amount or distribution is Class A. */
    public void gradeArmorChange() {
        raise(Refit.CLASS_A, "RefitClass.armor");
    }

    /**
     * A pod fitted to an Omni unit. Swapping pods is the quickest kind of refit.
     *
     * @param partName     the pod-mounted component
     * @param locationName where it goes
     */
    public void gradeOmniPodWork(String partName, String locationName) {
        podsIn.computeIfAbsent(locationName, key -> new LinkedHashMap<>()).merge(partName, 1, Integer::sum);
        raiseClass(Refit.CLASS_OMNI, partName);
    }

    /**
     * A pod taken off an Omni unit.
     *
     * @param partName     the pod-mounted component
     * @param locationName where it comes from
     */
    public void gradeOmniPodRemoved(String partName, String locationName) {
        podsOut.computeIfAbsent(locationName, key -> new LinkedHashMap<>()).merge(partName, 1, Integer::sum);
        raiseClass(Refit.CLASS_OMNI, partName);
    }

    /** Altering an Omni unit's fixed components is Class F. */
    public void gradeFixedOmniChange() {
        raise(Refit.CLASS_F, "RefitClass.omniFixed");
    }

    /**
     * Turning a standard unit into an Omni, or an Omni into a standard unit, changes which of its components are fixed
     * to the chassis, so it is Class F like any other change to an Omni unit's fixed components (CamOps p.212).
     */
    public void gradeOmniConversion() {
        raise(Refit.CLASS_F, "RefitClass.omniConversion");
    }

    /**
     * Moving an Omni unit's weightless fixed slots, such as Endo Steel or Ferro-Fibrous, into a location is part of a
     * Class F refit.
     *
     * @param locationName the location the slots move to
     */
    public void gradeWeightlessSlotsMoved(String locationName) {
        raise(Refit.CLASS_F, "RefitClass.weightlessSlots", locationName);
    }

    /**
     * Finds the locations of an Omni unit that hold more of some weightless fixed slot after the refit than before,
     * such as Endo Steel, Ferro-Fibrous, Reflective armor or TSM slots. Only how many of each a location holds
     * matters: two Endo Steel slots in the left torso before and after is no change, wherever they sit in it, but two
     * becoming one is (CamOps p.212, with errata). Each location gaining slots costs a base 60 minutes.
     *
     * @param oldEntity the unit as it is
     * @param newEntity the design it is refitted to
     *
     * @return the locations gaining weightless fixed slots, in order
     */
    public static List<Integer> getLocationsGainingWeightlessSlots(Entity oldEntity, Entity newEntity) {
        Map<Integer, Map<String, Integer>> oldSlots = countWeightlessFixedSlots(oldEntity);
        Map<Integer, Map<String, Integer>> newSlots = countWeightlessFixedSlots(newEntity);
        List<Integer> locationsGaining = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, Integer>> location : newSlots.entrySet()) {
            Map<String, Integer> slotsBefore = oldSlots.getOrDefault(location.getKey(), Map.of());
            boolean isGainingSlots = location.getValue()
                                           .entrySet()
                                           .stream()
                                           .anyMatch(slot -> slot.getValue() > slotsBefore.getOrDefault(slot.getKey(),
                                                 0));
            if (isGainingSlots) {
                locationsGaining.add(location.getKey());
            }
        }
        return locationsGaining;
    }

    /**
     * @param oldEntity the unit as it is
     * @param newEntity the design it is refitted to
     *
     * @return {@code true} if any location holds a different number of some weightless fixed slot after the refit
     */
    public static boolean haveWeightlessSlotsChanged(Entity oldEntity, Entity newEntity) {
        return !countWeightlessFixedSlots(oldEntity).equals(countWeightlessFixedSlots(newEntity));
    }

    /**
     * Counts each kind of weightless fixed slot in each location. Pods, equipment that can be hit or weighs
     * something, and the Clan CASE MegaMek adds wherever explosive ammunition sits are left out.
     */
    private static Map<Integer, Map<String, Integer>> countWeightlessFixedSlots(Entity entity) {
        Map<Integer, Map<String, Integer>> slotsByLocation = new TreeMap<>();
        for (int location = 0; location < entity.locations(); location++) {
            for (int slot = 0; slot < entity.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot criticalSlot = entity.getCritical(location, slot);
                boolean isEquipmentSlot = (criticalSlot != null)
                      && (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT)
                      && (criticalSlot.getMount() != null);
                if (isEquipmentSlot && isWeightlessFixedSlot(criticalSlot.getMount())) {
                    slotsByLocation.computeIfAbsent(location, key -> new TreeMap<>())
                          .merge(criticalSlot.getMount().getType().getInternalName(), 1, Integer::sum);
                }
            }
        }
        return slotsByLocation;
    }

    private static boolean isWeightlessFixedSlot(Mounted<?> mounted) {
        EquipmentType type = mounted.getType();
        boolean isFixed = !mounted.isOmniPodMounted();
        boolean isSlotOnlyEquipment = (type instanceof MiscType) && !type.isHittable();
        boolean isWeightless = mounted.getTonnage() <= 0;
        boolean isAutomaticClanCase = EquipmentTypeLookup.CLAN_CASE.equals(type.getInternalName());
        return isFixed && isSlotOnlyEquipment && isWeightless && !isAutomaticClanCase;
    }

    /**
     * Changing engine type or rating is Class E, or Class D when a refit kit supplies the engine.
     *
     * @param isRefitKit {@code true} if the work is done with a refit kit
     */
    public void gradeEngineChange(boolean isRefitKit) {
        if (isRefitKit) {
            raise(Refit.CLASS_D, "RefitClass.engine.kit");
        } else {
            raise(Refit.CLASS_E, "RefitClass.engine");
        }
    }

    /** Any change to the heat sinks built into the engine is Class B. */
    public void gradeEngineHeatSinkChange() {
        raise(Refit.CLASS_B, "RefitClass.engineHeatSinks");
    }

    /** Changing gyro type or weight is Class D. */
    public void gradeGyroChange() {
        raise(Refit.CLASS_D, "RefitClass.gyro");
    }

    /** Changing the cockpit is Class E. */
    public void gradeCockpitChange() {
        raise(Refit.CLASS_E, "RefitClass.cockpit");
    }

    /** Changing internal structure type is Class F. */
    public void gradeStructureChange() {
        raise(Refit.CLASS_F, "RefitClass.structure");
    }

    /**
     * Adding a turret where there was none is Class F; changing the weight of an existing turret is Class D.
     *
     * @param locationName   the turret's location
     * @param hadTurretThere {@code true} if the unit already had a turret there
     */
    public void gradeTurret(String locationName, boolean hadTurretThere) {
        if (hadTurretThere) {
            raise(Refit.CLASS_D, "RefitClass.turret.weight", locationName);
        } else {
            raise(Refit.CLASS_F, "RefitClass.turret.new", locationName);
        }
    }

    /**
     * Adding CASE or CASE II to a location that had neither is Class D, or a pod swap when it goes in an Omni
     * unit's pods.
     *
     * @param locationName  the location
     * @param isOmniPodWork {@code true} if the CASE goes in an Omni unit's pods
     */
    public void gradeCaseAdded(String locationName, boolean isOmniPodWork) {
        if (isOmniPodWork) {
            gradeOmniPodWork(getFormattedTextAt(RESOURCE_BUNDLE, "RefitClass.case.name"), locationName);
        } else {
            raise(Refit.CLASS_D, "RefitClass.case", locationName);
        }
    }

    /**
     * A new part of a large spacecraft, such as a drive or system.
     *
     * @param partName the new part
     */
    public void gradeNewShipComponent(String partName) {
        raise(Refit.CLASS_C, "RefitClass.shipComponent", partName);
    }

    /**
     * Work on cargo and transport bays, at the class the bay rules set for it.
     *
     * @param bayClass the class of the bay work, one of the {@link Refit} class constants
     */
    public void gradeBayWork(int bayClass) {
        raise(bayClass, "RefitClass.bays");
    }

    /** A new infantry armor kit or motive type. */
    public void gradeInfantryKit() {
        raise(Refit.CLASS_A, "RefitClass.infantryKit");
    }

    /** A change in the number or size of an infantry platoon's squads. */
    public void gradeInfantrySquadChange() {
        raise(Refit.CLASS_A, "RefitClass.infantrySquads");
    }

    /** Refurbishment is Class E. */
    public void gradeRefurbishment() {
        raise(Refit.CLASS_E, "RefitClass.refurbishment");
    }

    /**
     * Grades the changes that belong to the unit as a whole rather than to one part: internal structure type,
     * myomer, ejection, command consoles and power amplifiers.
     *
     * @param oldEntity the unit as it is
     * @param newEntity the design it is refitted to
     */
    public void gradeWholeUnitChanges(Entity oldEntity, Entity newEntity) {
        if (oldEntity.getStructureType() != newEntity.getStructureType()) {
            gradeStructureChange();
        }
        if (!getMyomerTypes(oldEntity).equals(getMyomerTypes(newEntity))) {
            raise(Refit.CLASS_F, "RefitClass.myomer");
        }
        if (hasFullHeadEjection(oldEntity) != hasFullHeadEjection(newEntity)) {
            raise(Refit.CLASS_E, "RefitClass.ejection");
        }
        if (countCommandConsoles(oldEntity) != countCommandConsoles(newEntity)) {
            raise(Refit.CLASS_E, "RefitClass.console");
        }
        boolean hadPowerAmplifier = oldEntity.getPowerAmplifierWeight() > 0;
        boolean needsPowerAmplifier = newEntity.getPowerAmplifierWeight() > 0;
        if (!hadPowerAmplifier && needsPowerAmplifier) {
            raise(Refit.CLASS_C, "RefitClass.powerAmplifier");
        }
    }

    /**
     * Whether a component must take slots in more than one location, which makes adding it Class D. Armor,
     * internal structure and myomer slots are left out; those changes have their own classes.
     *
     * @param type    the component's equipment type
     * @param mounted the component as mounted on the new design, or {@code null} if it is not known
     *
     * @return {@code true} if the component spans locations
     */
    public static boolean spansLocations(EquipmentType type, @Nullable Mounted<?> mounted) {
        boolean isArmorOrStructure = EquipmentType.isArmorType(type) || EquipmentType.isStructureType(type);
        if (isArmorOrStructure || isMyomer(type)) {
            return false;
        }
        boolean isSplitAcrossLocations = (mounted != null) && mounted.isSplit();
        return type.isSpreadable() || isSplitAcrossLocations;
    }

    /**
     * @param type an equipment type
     *
     * @return {@code true} if the type is a Mek turret, which is graded as a turret rather than a component
     */
    public static boolean isMekTurret(EquipmentType type) {
        // Only equipment carries these flags; asking a weapon or ammunition type for one logs a warning
        if (!(type instanceof MiscType)) {
            return false;
        }
        return type.hasFlag(MiscType.F_HEAD_TURRET)
              || type.hasFlag(MiscType.F_SHOULDER_TURRET)
              || type.hasFlag(MiscType.F_QUAD_TURRET);
    }

    /**
     * @param type an equipment type
     *
     * @return {@code true} if the type is a kind of myomer, whose change is graded for the whole unit
     */
    public static boolean isMyomer(EquipmentType type) {
        if (!(type instanceof MiscType)) {
            return false;
        }
        return type.hasFlag(MiscType.F_TSM) || type.hasFlag(MiscType.F_INDUSTRIAL_TSM) || type.hasFlag(MiscType.F_SCM);
    }

    /**
     * @param type an equipment type
     *
     * @return {@code true} if the type is a command console, whose change is graded for the whole unit
     */
    public static boolean isCommandConsole(EquipmentType type) {
        if (!(type instanceof MiscType)) {
            return false;
        }
        return type.hasFlag(MiscType.F_COMMAND_CONSOLE);
    }

    private static Set<String> getMyomerTypes(Entity entity) {
        Set<String> myomerTypes = new TreeSet<>();
        for (MiscMounted mounted : entity.getMisc()) {
            if (isMyomer(mounted.getType())) {
                myomerTypes.add(mounted.getType().getInternalName());
            }
        }
        return myomerTypes;
    }

    private static int countCommandConsoles(Entity entity) {
        int consoles = 0;
        for (MiscMounted mounted : entity.getMisc()) {
            if (isCommandConsole(mounted.getType())) {
                consoles++;
            }
        }
        return consoles;
    }

    private static boolean hasFullHeadEjection(Entity entity) {
        return (entity instanceof Mek mek) && mek.hasFullHeadEject();
    }
}
