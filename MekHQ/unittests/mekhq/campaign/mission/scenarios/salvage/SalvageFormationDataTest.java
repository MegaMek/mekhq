package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.enums.SkillLevel;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.units.Dropship;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SalvageFormationDataTest {
    private final Campaign campaign = mockCampaign();
    private final CampaignOptions options = new CampaignOptions();
    private final List<Unit> units = new ArrayList<>();

    SalvageFormationDataTest() {
        options.set(CampaignOption.SALVAGE_SYSTEM, SalvageSystem.CAM_OPS_STRICT);
        options.set(CampaignOption.USE_ADVANCED_MEDICAL, false);
        options.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, false);
        when(campaign.getCampaignOptions()).thenReturn(options);
    }

    private Formation formation(FormationType formationType, UUID techId) {
        Formation formation = mock(Formation.class);
        when(formation.getFormationType()).thenReturn(formationType);
        when(formation.getTechID()).thenReturn(techId);
        when(formation.getAllUnitsAsUnits(any(), eq(false))).thenReturn(units);
        return formation;
    }

    /** A vehicle able to take part in salvage operations. */
    private Unit salvageVehicle(String name, double weight, double cargoCapacity) {
        Tank tank = mock(Tank.class);
        when(tank.getWeight()).thenReturn(weight);
        Unit unit = mock(Unit.class);
        when(unit.getName()).thenReturn(name);
        when(unit.getEntity()).thenReturn(tank);
        when(unit.canSalvage(anyBoolean())).thenReturn(true);
        when(unit.isRepairable()).thenReturn(true);
        when(unit.getCargoCapacityForSalvage()).thenReturn(cargoCapacity);
        units.add(unit);
        return unit;
    }

    private Unit tugShip(String name, double weight) {
        Dropship dropship = mock(Dropship.class);
        when(dropship.getWeight()).thenReturn(weight);
        MiscType type = mock(MiscType.class);
        when(type.hasFlag(MiscType.F_NAVAL_TUG_ADAPTOR)).thenReturn(true);
        MiscMounted tug = mock(MiscMounted.class);
        when(tug.getType()).thenReturn(type);
        when(tug.getEntity()).thenReturn(dropship);
        when(tug.isOperable()).thenReturn(true);
        List<MiscMounted> misc = List.of(tug);
        when(dropship.getMisc()).thenReturn(misc);
        Unit unit = mock(Unit.class);
        when(unit.getName()).thenReturn(name);
        when(unit.getEntity()).thenReturn(dropship);
        when(unit.canSalvage(anyBoolean())).thenReturn(true);
        when(unit.isRepairable()).thenReturn(true);
        units.add(unit);
        return unit;
    }

    private Person tech(boolean isEngineer) {
        Person tech = mock(Person.class);
        UUID id = UUID.randomUUID();
        when(tech.getId()).thenReturn(id);
        when(tech.isEngineer()).thenReturn(isEngineer);
        when(tech.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(tech.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(tech.getFullTitle()).thenReturn("Tech Sergeant Smith");
        when(tech.getSkillLevel(campaign, false, true)).thenReturn(SkillLevel.VETERAN);
        when(campaign.getPlayerForce().getHumanResources().getPerson(id)).thenReturn(tech);
        return tech;
    }

    @Nested
    class Tech {
        @Test
        void salvageFormationsBringTheirTech() {
            Person tech = tech(false);

            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, tech.getId()), false);

            assertSame(tech, data.tech());
        }

        @Test
        void otherFormationsDontBringATech() {
            Person tech = tech(false);

            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.STANDARD, tech.getId()), false);

            assertNull(data.tech());
        }

        @Test
        void engineersCantSalvage() {
            Person engineer = tech(true);

            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, engineer.getId()), false);

            assertNull(data.tech());
        }

        @Test
        void formationWithoutTechHasNone() {
            assertNull(SalvageFormationData.buildData(campaign, formation(FormationType.SALVAGE, null), false).tech());
        }

        @Test
        void missingTechIsIgnored() {
            UUID missingTechId = UUID.randomUUID();
            when(campaign.getPlayerForce().getHumanResources().getPerson(missingTechId)).thenReturn(null);

            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, missingTechId), false);

            assertNull(data.tech());
        }

        @Test
        void noTechTooltipIsLocalized() {
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), false);

            String tooltip = data.getTechTooltip(campaign, null);
            assertFalse(tooltip.isBlank());
            assertFalse(tooltip.startsWith("!"), tooltip);
        }

        @Test
        void techTooltipShowsNameSkillAndHits() {
            Person tech = tech(false);
            when(tech.getHits()).thenReturn(2);
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, tech.getId()), false);

            String tooltip = data.getTechTooltip(campaign, tech);

            assertTrue(tooltip.startsWith("Tech Sergeant Smith<br>"), tooltip);
            assertTrue(tooltip.contains(SkillLevel.VETERAN.toString()), tooltip);
            assertTrue(tooltip.contains("2"), tooltip);
        }

        @Test
        void techTooltipShowsInjurySeverityUnderAdvancedMedical() {
            options.set(CampaignOption.USE_ADVANCED_MEDICAL, true);
            Person tech = tech(false);
            when(tech.getTotalInjurySeverity()).thenReturn(5);
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, tech.getId()), false);

            assertTrue(data.getTechTooltip(campaign, tech).contains("5"));
        }

        @Test
        void crewTechTooltipListsNonEngineerTechCrew() {
            Unit unit = salvageVehicle("Truck", 40, 10);
            Person crewTech = tech(false);
            when(crewTech.isTechExpanded()).thenReturn(true);
            Person engineer = tech(true);
            when(engineer.isTechExpanded()).thenReturn(true);
            Person driver = tech(false);
            when(unit.getCrew()).thenReturn(List.of(crewTech, engineer, driver));
            Formation formation = formation(FormationType.SALVAGE, null);
            SalvageFormationData data = SalvageFormationData.buildData(campaign, formation, false);

            String tooltip = data.getAllCrewTechTooltip(campaign, formation);

            // Only the crew tech is listed
            assertEquals(1, tooltip.split("Tech Sergeant Smith", -1).length - 1, tooltip);
        }

        @Test
        void crewTechsAreSeparated() {
            Unit unit = salvageVehicle("Truck", 40, 10);
            Person first = tech(false);
            when(first.isTechExpanded()).thenReturn(true);
            Person second = tech(false);
            when(second.isTechExpanded()).thenReturn(true);
            when(unit.getCrew()).thenReturn(List.of(first, second));
            Formation formation = formation(FormationType.SALVAGE, null);
            SalvageFormationData data = SalvageFormationData.buildData(campaign, formation, false);

            assertEquals(2, data.getAllCrewTechTooltip(campaign, formation).split("<br><br>").length);
        }
    }

    @Nested
    class Capacity {
        @Test
        void groundCapacitiesAreTheBestOfTheAvailableUnits() {
            salvageVehicle("Truck", 20, 40);
            salvageVehicle("Tank", 60, 5);
            Unit unavailable = salvageVehicle("Wreck", 100, 100);
            when(unavailable.isRepairable()).thenReturn(false);

            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), false);

            assertEquals(2, data.salvageCapableUnits());
            assertEquals(40.0, data.maximumCargoCapacity());
            assertEquals(60.0, data.maximumTowCapacity());
            assertFalse(data.hasTug());
            assertFalse(data.isSpaceScenario());
            assertEquals(FormationType.SALVAGE, data.formationType());
        }

        @Test
        void spaceTowCapacityIsVesselWeightAndTugsAreNoted() {
            tugShip("Union", 3600);

            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), true);

            assertEquals(3600.0, data.maximumTowCapacity());
            assertTrue(data.hasTug());
        }

        @Test
        void emptyFormationHasNoCapacity() {
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), false);

            assertEquals(0, data.salvageCapableUnits());
            assertEquals(0.0, data.maximumCargoCapacity());
            assertEquals(0.0, data.maximumTowCapacity());
        }

        @Test
        void cargoTooltipListsUnitsWithCapacityAlphabetically() {
            salvageVehicle("zebra", 10, 5);
            salvageVehicle("Alpha", 10, 7);
            salvageVehicle("Mule", 10, 0);
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), false);

            String tooltip = data.getCargoCapacityTooltip(campaign);

            assertTrue(tooltip.indexOf("Alpha") < tooltip.indexOf("zebra"), tooltip);
            assertFalse(tooltip.contains("Mule"), tooltip);
        }

        @Test
        void towTooltipUsesGroundTowCapacity() {
            salvageVehicle("Tank", 60, 0);
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), false);

            assertTrue(data.getTowCapacityTooltip(campaign).contains("60"));
        }

        @Test
        void towTooltipUsesVesselWeightInSpace() {
            tugShip("Union", 3600);
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), true);

            assertTrue(data.getTowCapacityTooltip(campaign).contains("Union"));
        }

        @Test
        void tugTooltipListsTugs() {
            tugShip("Union", 3600);
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), true);

            assertTrue(data.getTugTooltip(campaign).startsWith("Union"));
        }

        @Test
        void tugTooltipExplainsTheLackOfTugs() {
            salvageVehicle("Truck", 20, 40);
            SalvageFormationData data = SalvageFormationData.buildData(campaign,
                  formation(FormationType.SALVAGE, null), true);

            String tooltip = data.getTugTooltip(campaign);
            assertFalse(tooltip.isBlank());
            assertFalse(tooltip.startsWith("!"), tooltip);
        }
    }
}
