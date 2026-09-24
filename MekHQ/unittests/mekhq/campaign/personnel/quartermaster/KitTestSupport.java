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

package mekhq.campaign.personnel.quartermaster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;

/**
 * Shared fixtures for the quartermaster kit tests: mocked people whose armor kit and two equipment-kit slots are
 * backed by live state, and a mocked warehouse whose stock actually goes down as kits are drawn and up as they are
 * returned.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class KitTestSupport {
    private KitTestSupport() {
    }

    /**
     * An active person with the given roles, in coveralls, with both equipment-kit slots empty.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Person person(PersonnelRole primary, PersonnelRole secondary) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(primary);
        when(person.getSecondaryRole()).thenReturn(secondary);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        wireEquipmentKits(person, null, null);
        wireArmorKit(person, ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME);
        return person;
    }

    /**
     * Backs both equipment-kit slots and their awaited kits with mutable state, through the slot accessors and the
     * per-slot shorthands.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void wireEquipmentKits(Person person, String primaryKit, String secondaryKit) {
        String[] held = { primaryKit, secondaryKit };
        String[] intended = { null, null };
        when(person.getKitName(any(KitSlot.class)))
              .thenAnswer(invocation -> held[((KitSlot) invocation.getArgument(0)).ordinal()]);
        doAnswer(invocation -> {
            held[((KitSlot) invocation.getArgument(0)).ordinal()] = invocation.getArgument(1);
            return null;
        }).when(person).setKitName(any(KitSlot.class), any());
        when(person.getIntendedKitName(any(KitSlot.class)))
              .thenAnswer(invocation -> intended[((KitSlot) invocation.getArgument(0)).ordinal()]);
        doAnswer(invocation -> {
            intended[((KitSlot) invocation.getArgument(0)).ordinal()] = invocation.getArgument(1);
            return null;
        }).when(person).setIntendedKitName(any(KitSlot.class), any());
        when(person.getRepairKitName()).thenAnswer(invocation -> held[0]);
        when(person.getSecondaryKitName()).thenAnswer(invocation -> held[1]);
        when(person.getIntendedRepairKitName()).thenAnswer(invocation -> intended[0]);
        when(person.getIntendedSecondaryKitName()).thenAnswer(invocation -> intended[1]);
        when(person.hasRepairKit(anyString())).thenAnswer(invocation -> {
            Object name = invocation.getArgument(0);
            return name.equals(held[0]) || name.equals(held[1]);
        });
    }

    /**
     * Backs the armor kit and awaited armor kit with mutable state ({@code null} set means coveralls, as in Person).
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void wireArmorKit(Person person, String armorKit) {
        String[] worn = { armorKit };
        String[] intended = { null };
        when(person.getArmorKitName()).thenAnswer(invocation -> worn[0]);
        doAnswer(invocation -> {
            String kit = invocation.getArgument(0);
            worn[0] = (kit == null) ? ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME : kit;
            return null;
        }).when(person).setArmorKitName(any());
        when(person.getIntendedArmorKitName()).thenAnswer(invocation -> intended[0]);
        doAnswer(invocation -> {
            intended[0] = invocation.getArgument(0);
            return null;
        }).when(person).setIntendedArmorKitName(any());
    }

    /**
     * A present, spare warehouse part of the given kit type.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static EquipmentPart partOf(EquipmentType type) {
        EquipmentPart part = mock(EquipmentPart.class);
        when(part.isPresent()).thenReturn(true);
        when(part.isSpare()).thenReturn(true);
        when(part.getQuantity()).thenReturn(1);
        when(part.getType()).thenReturn(type);
        return part;
    }

    /**
     * A warehouse holding one of each given kit, whose stock is drawn down by {@code removePart} and topped up by
     * {@code addPart}. Returned kits are recorded as mock parts of the same type, so they can be counted with
     * {@link #count(LocalWarehouse, EquipmentType)}.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static LocalWarehouse warehouseWith(EquipmentType... kits) {
        List<Part> spares = new ArrayList<>();
        for (EquipmentType kit : kits) {
            spares.add(partOf(kit));
        }
        LocalWarehouse warehouse = mock(LocalWarehouse.class);
        when(warehouse.getSpareParts()).thenAnswer(invocation -> new ArrayList<>(spares));
        when(warehouse.findSparePart(any())).thenAnswer(invocation -> {
            Predicate<Part> predicate = invocation.getArgument(0);
            return spares.stream().filter(predicate).findFirst().orElse(null);
        });
        when(warehouse.removePart(any(), anyInt())).thenAnswer(invocation -> spares.remove(invocation.<Part>getArgument(0)));
        when(warehouse.addPart(any(), anyBoolean())).thenAnswer(invocation -> {
            Part added = invocation.getArgument(0);
            EquipmentPart restocked = partOf(((EquipmentPart) added).getType());
            spares.add(restocked);
            return restocked;
        });
        return warehouse;
    }

    /**
     * How many of a kit a {@link #warehouseWith} warehouse currently holds.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int count(LocalWarehouse warehouse, EquipmentType kit) {
        int count = 0;
        for (Part part : warehouse.getSpareParts()) {
            if ((part instanceof EquipmentPart equipmentPart) && kit.equals(equipmentPart.getType())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Makes these people the campaign's player personnel, for the sweeps that walk the whole roster.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void setRoster(Campaign campaign, Collection<Person> people) {
        when(campaign.getPlayerForce().getPersonnel().values()).thenReturn(new ArrayList<>(people));
    }

    /**
     * @return the kit with this internal name, which must exist
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static EquipmentType kit(String internalName) {
        EquipmentType kit = EquipmentType.get(internalName);
        if (kit == null) {
            throw new IllegalStateException("Unknown kit " + internalName);
        }
        return kit;
    }
}
