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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.client.ratgenerator.FormationNamingConvention.DesignatorStyle;
import megamek.client.ratgenerator.FormationNamingConvention.Tier;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies that {@link ForceDescriptorWalker} honors preview exclusions: excluded leaves are skipped,
 * formations whose units are all excluded are dropped, and a single re-included unit keeps its
 * formation. Created formations are named through {@link FormationNamer}.
 */
class ForceDescriptorWalkerTest {

    private static final int ECHELON_LANCE = 3;
    private static final int ECHELON_COMPANY = 4;
    private static final int ECHELON_BATTALION = 5;

    /**
     * The Inner Sphere naming rules, supplied directly rather than read from the faction rulesets: this
     * test suite does not load MegaMek's data set, so the production resolver would find no rule for
     * any echelon and every formation would fall back to keeping its ruleset name. That the shipped
     * rulesets really do declare these rules is covered by
     * {@code megamek.client.ratgenerator.FormationNamingConventionTest}.
     */
    private static FormationNamer namer() {
        Map<Integer, Tier> innerSphereRules = Map.of(
              ECHELON_BATTALION, new Tier(ECHELON_BATTALION, DesignatorStyle.ALPHABET, false),
              ECHELON_COMPANY, new Tier(ECHELON_COMPANY, DesignatorStyle.ALPHABET, false),
              ECHELON_LANCE, new Tier(ECHELON_LANCE, DesignatorStyle.ALPHABET, false));
        return new FormationNamer(ForceNamingMethod.CCB_1943, List.of(),
              (faction, echelon) -> innerSphereRules.get(echelon));
    }

    /** {@code ForceDescriptor.entity} has no setter (set only during generation), so inject via reflection. */
    private static void injectEntity(ForceDescriptor descriptor) throws Exception {
        Field field = ForceDescriptor.class.getDeclaredField("entity");
        field.setAccessible(true);
        field.set(descriptor, mock(Entity.class));
    }

    private static ForceDescriptor group(String name) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        return descriptor;
    }

    private static ForceDescriptor unit(String name) throws Exception {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        injectEntity(descriptor);
        return descriptor;
    }

    /** A platoon-echelon node of {@code unitType} whose children are all leaf units (a "loose platoon"). */
    private static ForceDescriptor loosePlatoon(String name, int unitType) throws Exception {
        ForceDescriptor platoon = new ForceDescriptor();
        platoon.setName(name);
        platoon.setEchelon(3);
        platoon.setUnitType(unitType);
        platoon.addSubForce(unit(name + " Unit A"));
        platoon.addSubForce(unit(name + " Unit B"));
        return platoon;
    }

    private static List<String> namesOfCreatedFormations(Campaign campaign) {
        ArgumentCaptor<Formation> captor = ArgumentCaptor.forClass(Formation.class);
        verify(campaign, atLeastOnce()).addFormation(captor.capture(), any());
        return captor.getAllValues().stream().map(Formation::getName).toList();
    }

    @Test
    void excludedSubtreeIsSkippedAndEmptyFormationDropped() throws Exception {
        ForceDescriptor root = group("Task Force");
        ForceDescriptor firstLance = group("First Lance");
        ForceDescriptor secondLance = group("Second Lance");
        firstLance.addSubForce(unit("Unit A"));
        firstLance.addSubForce(unit("Unit B"));
        secondLance.addSubForce(unit("Unit C"));
        secondLance.addSubForce(unit("Unit D"));
        root.addSubForce(firstLance);
        root.addSubForce(secondLance);

        secondLance.setIncludedRecursively(false);

        Campaign campaign = mock(Campaign.class);
        List<String> handledUnits = new ArrayList<>();
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> handledUnits.add(leaf.parseName()));

        assertEquals(List.of("Unit A", "Unit B"), handledUnits, "excluded lance's units must be skipped");

        List<String> createdFormations = namesOfCreatedFormations(campaign);
        assertTrue(createdFormations.contains("First Lance"));
        assertFalse(createdFormations.contains("Second Lance"), "a fully excluded lance must not create a formation");
    }

    @Test
    void reIncludedUnitInsideExcludedFormationIsKept() throws Exception {
        ForceDescriptor root = group("Task Force");
        ForceDescriptor lance = group("Second Lance");
        ForceDescriptor unitC = unit("Unit C");
        ForceDescriptor unitD = unit("Unit D");
        lance.addSubForce(unitC);
        lance.addSubForce(unitD);
        root.addSubForce(lance);

        lance.setIncludedRecursively(false);
        unitC.setIncluded(true);

        Campaign campaign = mock(Campaign.class);
        List<String> handledUnits = new ArrayList<>();
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> handledUnits.add(leaf.parseName()));

        assertEquals(List.of("Unit C"), handledUnits, "the re-included unit is kept, its excluded sibling dropped");
        assertTrue(namesOfCreatedFormations(campaign).contains("Second Lance"),
              "a lance with one re-included unit keeps its formation");
    }

    /**
     * The preview names must be exactly the names the build produces: both run the same traversal with
     * equally configured namers, and this test pins that contract.
     */
    @Test
    void previewNamesMatchTheBuiltFormationNames() throws Exception {
        ForceDescriptor root = group("Battalion");
        root.setEchelon(5);
        ForceDescriptor firstCompany = group("A Company");
        firstCompany.setEchelon(4);
        ForceDescriptor battleLance = group("Battle Lance");
        battleLance.setEchelon(3);
        battleLance.addSubForce(unit("Unit A"));
        ForceDescriptor fireLance = group("Fire Lance");
        fireLance.setEchelon(3);
        fireLance.addSubForce(unit("Unit B"));
        firstCompany.addSubForce(battleLance);
        firstCompany.addSubForce(fireLance);
        ForceDescriptor secondCompany = group("A Company");
        secondCompany.setEchelon(4);
        ForceDescriptor secondBattleLance = group("Battle Lance");
        secondBattleLance.setEchelon(3);
        secondBattleLance.addSubForce(unit("Unit C"));
        secondCompany.addSubForce(secondBattleLance);
        root.addSubForce(firstCompany);
        root.addSubForce(secondCompany);

        var previewNames = ForceDescriptorWalker.previewNames(root, namer());

        Campaign campaign = mock(Campaign.class);
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> { });
        List<String> builtNames = namesOfCreatedFormations(campaign);

        assertEquals(builtNames.size(), previewNames.size(),
              "every built formation (no synthesized companies here) must have a previewed name");
        assertEquals("Able Company", previewNames.get(firstCompany));
        assertEquals("Able Battle Lance", previewNames.get(battleLance));
        assertEquals("Baker Fire Lance", previewNames.get(fireLance));
        assertEquals("Baker Company", previewNames.get(secondCompany));
        assertEquals("Able Battle Lance", previewNames.get(secondBattleLance));
        assertTrue(builtNames.containsAll(previewNames.values()),
              "the preview must show exactly the names the build produces");
    }

    @Test
    void looseAttachedPlatoonsWrapIntoUnitTypeCompany() throws Exception {
        ForceDescriptor root = group("Regiment");
        // Two loose Battle Armor platoons attached directly to the root (no company wrapper).
        root.addAttached(loosePlatoon("Platoon", UnitType.BATTLE_ARMOR));
        root.addAttached(loosePlatoon("Platoon", UnitType.BATTLE_ARMOR));

        Campaign campaign = mock(Campaign.class);
        List<String> handledUnits = new ArrayList<>();
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> handledUnits.add(leaf.parseName()));

        assertEquals(4, handledUnits.size(), "all four platoon units are placed");

        List<String> createdFormations = namesOfCreatedFormations(campaign);
        // The synthesized company is company-tier, so the namer prefixes the first CCB
        // designator; its platoons restart the same alphabet inside it.
        String expectedCompany = "Able " + UnitType.getTypeDisplayableName(UnitType.BATTLE_ARMOR) + " Company";
        assertTrue(createdFormations.contains(expectedCompany),
              "loose BA platoons should nest under a synthesized '" + expectedCompany + "'");
        assertTrue(createdFormations.contains("Able Platoon") && createdFormations.contains("Baker Platoon"),
              "platoons take the selected alphabet within their synthesized company");
        assertEquals(1, createdFormations.stream().filter(expectedCompany::equals).count(),
              "both platoons share one synthesized company (grouped by unit type)");
    }
}
