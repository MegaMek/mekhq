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
package testUtilities.parts;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import testUtilities.MHQTestUtilities;

/**
 * A real-object setting for parts, repair and refit tests: a real {@link Campaign} with its real quartermaster and
 * warehouse, into which canon units from {@link UnitFixture} and spare parts can be placed in a line each. Nothing is
 * mocked, so a test can check the state the campaign ends up in rather than which methods were called.
 *
 * <pre>{@code
 * PartsScenario scenario = PartsScenario.create();
 * Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
 * scenario.withSpare(PartsScenario.unitParts(locust, EnginePart.class).getFirst().clone(), 2);
 * assertEquals(2, scenario.countSpareParts(EnginePart.class));
 * }</pre>
 */
public final class PartsScenario {
    private final Campaign campaign;

    private PartsScenario(Campaign campaign) {
        this.campaign = campaign;
    }

    /**
     * Builds a scenario around a fresh test campaign from {@link MHQTestUtilities#getTestCampaign()}. The campaign
     * starts with no units and an empty warehouse.
     *
     * @return the new scenario
     */
    public static PartsScenario create() {
        return new PartsScenario(MHQTestUtilities.getTestCampaign());
    }

    /**
     * @return the real campaign behind this scenario
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * @return the campaign's own warehouse, which holds both the parts installed on units and the spare parts
     */
    public LocalWarehouse getWarehouse() {
        return campaign.getPlayerForce().getWarehouse();
    }

    /**
     * Adds a fresh copy of a fixture unit to the campaign the way the game adds a purchased unit: it arrives at once,
     * without new pilots, at quality D, and its parts are initialised and placed in the warehouse.
     *
     * @param fixture the canon unit to add
     *
     * @return the unit the campaign created, never {@code null}
     */
    public Unit withUnit(UnitFixture fixture) {
        Unit unit = campaign.addNewUnit(fixture.loadEntity(), false, 0, PartQuality.QUALITY_D);
        assertNotNull(unit, "Campaign did not create a unit for fixture " + fixture.name());
        return unit;
    }

    /**
     * Places a spare part in the campaign warehouse through the quartermaster, as a delivered, used part. The
     * warehouse merges it with a matching spare it already holds, exactly as it would in play.
     *
     * @param part     a part not attached to any unit, for example a {@link Part#clone()} of an installed part
     * @param quantity how many of the part to add; for armor and ammunition this is the number of stacks, not points
     *                 or shots
     */
    public void withSpare(Part part, int quantity) {
        part.setCampaign(campaign);
        part.setQuantity(quantity);
        campaign.getQuartermaster().addPart(part, 0, false);
    }

    /**
     * Adds a Mek technician to the campaign's roster who holds every technician skill at the same experience level,
     * so the tech is the right type for any part and the skill used is the one the level names. The tech is added
     * directly, without the hiring process, and starts the day with a full shift of minutes and overtime.
     *
     * @param experienceLevel the experience level of every tech skill, such as {@link SkillType#EXP_REGULAR}
     *
     * @return the new tech
     */
    public Person withTech(int experienceLevel) {
        if (SkillType.lookupHash == null) {
            SkillType.initializeTypes();
        }
        Person tech = new Person("Test", "Tech", campaign);
        tech.setPrimaryRoleDirect(PersonnelRole.MEK_TECH);
        for (String techSkillName : SkillType.getTechSkills()) {
            int skillLevel = SkillType.getType(techSkillName).getLevelFromExperience(experienceLevel);
            tech.addSkill(techSkillName, skillLevel, 0);
        }
        campaign.getPlayerForce().getHumanResources().importPerson(tech);
        tech.resetMinutesLeft(false);
        return tech;
    }

    /**
     * Adds temporary AsTechs to the campaign's AsTech pool, together with a full day of their minutes and overtime.
     * Six AsTechs make one complete team, so no shorthanded modifier applies.
     *
     * @param count how many AsTechs to add
     */
    public void withAsTechs(int count) {
        campaign.getPlayerForce().getHumanResources().increaseAsTechPool(campaign, count);
    }

    /**
     * @return the spare parts currently in the warehouse, that is the parts not installed on any unit
     */
    public List<Part> getSpareParts() {
        return getWarehouse().getSpareParts();
    }

    /**
     * Counts the spare parts of a type in the warehouse, adding up the quantity of each matching spare.
     *
     * @param partType the part class to count; subclasses count too
     *
     * @return the total quantity of matching spares, {@code 0} when there are none
     */
    public int countSpareParts(Class<? extends Part> partType) {
        int count = 0;
        for (Part sparePart : getSpareParts()) {
            if (partType.isInstance(sparePart)) {
                count += sparePart.getQuantity();
            }
        }
        return count;
    }

    /**
     * Finds the parts of a type installed on a unit, including missing-part placeholders when their class matches.
     *
     * @param unit     the unit to look at
     * @param partType the part class to find; subclasses match too
     * @param <T>      the part class
     *
     * @return the matching parts in the unit's own order, empty when there are none
     */
    public static <T extends Part> List<T> unitParts(Unit unit, Class<T> partType) {
        List<T> matchingParts = new ArrayList<>();
        for (Part part : unit.getParts()) {
            if (partType.isInstance(part)) {
                matchingParts.add(partType.cast(part));
            }
        }
        return matchingParts;
    }
}
