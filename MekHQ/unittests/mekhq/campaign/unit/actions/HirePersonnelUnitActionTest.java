/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.actions;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.time.Duration;

import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Jumpship;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.SmallCraft;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class HirePersonnelUnitActionTest {
    @Test
    public void fullUnitTakesNoAction() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Mek.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(0)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(0)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
    }

    @Test
    public void actionPassesAlongGMSetting() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Mek.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(true).doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(true).when(unit).usesSoloPilot();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockDriver = mock(Person.class);
        doNothing().when(unit).addPilotOrSoldier(eq(mockDriver));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR)).thenReturn(
              mockDriver);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockDriver),
                         eq(true),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(true);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(true),
              eq(true));
        verify(unit, times(1)).addPilotOrSoldier(eq(mockDriver));
    }

    @Test
    public void mekNeedingDriverAddsDriver() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Mek.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(true).doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(true).when(unit).usesSoloPilot();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockDriver = mock(Person.class);
        doNothing().when(unit).addPilotOrSoldier(eq(mockDriver));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR)).thenReturn(
              mockDriver);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockDriver),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).addPilotOrSoldier(eq(mockDriver));
    }

    @Test
    public void mekDoesntAddDriverIfRecruitFails() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        Entity mockEntity = mock(Mek.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(true).doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(true).when(unit).usesSoloPilot();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockDriver = mock(Person.class);
        doNothing().when(unit).addPilotOrSoldier(eq(mockDriver));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR)).thenReturn(
              mockDriver);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockDriver),
                         anyBoolean(),
                         eq(true))).thenReturn(false);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(0)).addPilotOrSoldier(eq(mockDriver));
    }

    @Test
    public void lamNeedingDriverAddsDriver() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(LandAirMek.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(true).doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(true).when(unit).usesSoloPilot();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockDriver = mock(Person.class);
        doNothing().when(unit).addPilotOrSoldier(eq(mockDriver));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.LAM_PILOT)).thenReturn(
              mockDriver);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockDriver),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.LAM_PILOT);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).addPilotOrSoldier(eq(mockDriver));
    }

    @Test
    public void mekNeedingGunnerAddsGunner() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Mek.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(true).doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockGunner = mock(Person.class);
        doNothing().when(unit).addGunner(eq(mockGunner));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR)).thenReturn(
              mockGunner);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockGunner),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).addGunner(eq(mockGunner));
    }

    @Test
    public void tankNeedingGunnerAddsGunner() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Tank.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(true).doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockGunner = mock(Person.class);
        doNothing().when(unit).addGunner(eq(mockGunner));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign,
                         mekhq.campaign.personnel.enums.PersonnelRole.VEHICLE_CREW_GROUND)).thenReturn(mockGunner);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockGunner),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.VEHICLE_CREW_GROUND);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).addGunner(eq(mockGunner));
    }

    @Test
    public void spaceShipNeedingGunnerAddsGunner() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Jumpship.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(true).doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockGunner = mock(Person.class);
        doNothing().when(unit).addGunner(eq(mockGunner));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_GUNNER)).thenReturn(
              mockGunner);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockGunner),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_GUNNER);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).addGunner(eq(mockGunner));
    }

    @Test
    public void smallCraftNeedingGunnerAddsGunner() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(SmallCraft.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(true).doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockGunner = mock(Person.class);
        doNothing().when(unit).addGunner(eq(mockGunner));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_GUNNER)).thenReturn(
              mockGunner);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockGunner),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_GUNNER);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).addGunner(eq(mockGunner));
    }

    @Test
    public void spaceShipNeedingCrewAddsCrew() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Jumpship.class);
        when(mockEntity.isSupportVehicle()).thenReturn(false);
        when(mockEntity.isLargeCraft()).thenReturn(true);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(true).doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockCrew = mock(Person.class);
        doNothing().when(unit).addVesselCrew(eq(mockCrew));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_CREW)).thenReturn(
              mockCrew);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockCrew),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_CREW);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).addVesselCrew(eq(mockCrew));
    }

    @Test
    public void supportVehicleNeedingCrewAddsCrew() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(SupportTank.class);
        when(mockEntity.isSupportVehicle()).thenReturn(true);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(true).doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockCrew = mock(Person.class);
        doNothing().when(unit).addVesselCrew(eq(mockCrew));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(eq(mockCampaign),
                         any(mekhq.campaign.personnel.enums.PersonnelRole.class))).thenReturn(
              mockCrew);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockCrew),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.VEHICLE_CREW_GROUND);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).addVesselCrew(eq(mockCrew));
    }

    @Test
    public void spaceShipNeedingNavigatorAddsNavigator() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Jumpship.class);
        when(mockEntity.isSupportVehicle()).thenReturn(false);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(true).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockNavigator = mock(Person.class);
        doNothing().when(unit).setNavigator(eq(mockNavigator));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign,
                         mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_NAVIGATOR)).thenReturn(mockNavigator);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockNavigator),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_NAVIGATOR);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).setNavigator(eq(mockNavigator));
    }

    @Test
    public void mekNeedingTechOfficerAddsTechOfficer() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Mek.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(true).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockTechOfficer = mock(Person.class);
        doNothing().when(unit).setTechOfficer(eq(mockTechOfficer));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign, mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR)).thenReturn(
              mockTechOfficer);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockTechOfficer),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).setTechOfficer(eq(mockTechOfficer));
    }

    @Test
    public void tankNeedingTechOfficerAddsTechOfficer() {
        Campaign mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
        doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);
        Entity mockEntity = mock(Tank.class);
        when(mockEntity.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(true).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        Person mockTechOfficer = mock(Person.class);
        doNothing().when(unit).setTechOfficer(eq(mockTechOfficer));
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .newPerson(mockCampaign,
                         mekhq.campaign.personnel.enums.PersonnelRole.VEHICLE_CREW_GROUND)).thenReturn(
              mockTechOfficer);
        when(mockCampaign.getPlayerForce()
                   .getHumanResources()
                   .recruitPerson(eq(mockCampaign),
                         eq(mockTechOfficer),
                         anyBoolean(),
                         eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.execute(mockCampaign, unit);

        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).newPerson(mockCampaign,
              mekhq.campaign.personnel.enums.PersonnelRole.VEHICLE_CREW_GROUND);
        verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
              any(Person.class),
              eq(false),
              eq(true));
        verify(unit, times(1)).setTechOfficer(eq(mockTechOfficer));
    }

    /**
     * Covers hiring crew for a unit that is not with the main force. New recruits always join the main force, so the
     * action must move them to the unit's location before assigning them, and must never loop forever when it can't.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Nested
    class UnitAwayFromMainForce {
        private Campaign mockCampaign;
        private Unit unit;

        @BeforeEach
        void setUp() {
            mockCampaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
            CampaignOptions mockOptions = mock(CampaignOptions.class);
            doReturn(mockOptions).when(mockCampaign).getCampaignOptions();
            doReturn(false).when(mockOptions).get(CampaignOption.USE_ARTILLERY);

            Entity mockEntity = mock(Jumpship.class);
            when(mockEntity.isSupportVehicle()).thenReturn(false);
            when(mockEntity.isLargeCraft()).thenReturn(true);
            unit = spy(new Unit(mockEntity, mockCampaign));

            doReturn(false).when(unit).canTakeMoreDrivers();
            doReturn(false).when(unit).canTakeMoreGunners();
            doReturn(false).when(unit).canTakeNavigator();
            doReturn(false).when(unit).canTakeTechOfficer();
            doNothing().when(unit).resetPilotAndEntity();
            doNothing().when(unit).runDiagnostic(false);
        }

        private Person createRealPerson() {
            return new Person("First", "Last", null, "MERC");
        }

        private void stubHiring(Person newHire) {
            when(mockCampaign.getPlayerForce()
                       .getHumanResources()
                       .newPerson(eq(mockCampaign), any(PersonnelRole.class))).thenReturn(newHire);
            when(mockCampaign.getPlayerForce()
                       .getHumanResources()
                       .recruitPerson(eq(mockCampaign), eq(newHire), anyBoolean(), eq(true))).thenReturn(true);
        }

        @Test
        void unitAtBase_newHireIsMovedToBasePersonnelAndAssigned() {
            PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
            LocationNode.LocationManager.setLocation(unit, base.getBaseHangar());

            Person newHire = createRealPerson();
            stubHiring(newHire);
            doReturn(true).doReturn(false).when(unit).canTakeMoreVesselCrew();
            doNothing().when(unit).addVesselCrew(eq(newHire));

            new HirePersonnelUnitAction(true).execute(mockCampaign, unit);

            assertSame(base.getBasePersonnel(), newHire.getParentLocation());
            assertTrue(base.getBasePersonnel().containsKey(newHire.getId()));
            assertTrue(LocationUtils.areSameEffectiveLocation(unit, newHire));
            verify(unit, times(1)).addVesselCrew(eq(newHire));
        }

        @Test
        void unitInTransit_newHireIsMovedOntoTheSameTravelNodeAndAssigned() {
            PlanetarySystem mockSystem = mock(PlanetarySystem.class);
            when(mockSystem.getTimeToJumpPoint(1.0)).thenReturn(1.0);
            CurrentLocation travelNode = new CurrentLocation(mockSystem, 0.0);
            JumpPath mockJumpPath = mock(JumpPath.class);
            when(mockJumpPath.isEmpty()).thenReturn(false);
            travelNode.setJumpPath(mockJumpPath);
            LocationNode.LocationManager.setLocation(unit, travelNode);

            Person newHire = createRealPerson();
            stubHiring(newHire);
            doReturn(true).doReturn(false).when(unit).canTakeMoreVesselCrew();
            doNothing().when(unit).addVesselCrew(eq(newHire));

            new HirePersonnelUnitAction(true).execute(mockCampaign, unit);

            assertSame(travelNode, newHire.getParentLocation());
            assertTrue(LocationUtils.areSameEffectiveLocation(unit, newHire));
            verify(unit, times(1)).addVesselCrew(eq(newHire));
        }

        @Test
        void unitWithMainForce_newHireIsNotMoved() {
            Person newHire = createRealPerson();
            stubHiring(newHire);
            doReturn(true).doReturn(false).when(unit).canTakeMoreVesselCrew();
            doNothing().when(unit).addVesselCrew(eq(newHire));

            new HirePersonnelUnitAction(true).execute(mockCampaign, unit);

            assertNull(newHire.getParentLocation());
            verify(unit, times(1)).addVesselCrew(eq(newHire));
        }

        @Test
        void newHireCannotBeMovedToUnit_stopsHiringInsteadOfLoopingForever() {
            PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
            LocationNode.LocationManager.setLocation(unit, base.getBaseHangar());

            // A mocked person ignores setParent, so they can never reach the unit's location
            Person immovableHire = mock(Person.class);
            when(immovableHire.getLocationNode()).thenReturn(new LocationNode(immovableHire));
            stubHiring(immovableHire);
            // The unit always reports a free slot, exactly as it did when the bug looped forever
            doReturn(true).when(unit).canTakeMoreVesselCrew();

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                  () -> new HirePersonnelUnitAction(true).execute(mockCampaign, unit));

            verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
                  eq(immovableHire),
                  eq(true),
                  eq(true));
            verify(unit, never()).addVesselCrew(any(Person.class));
        }

        @Test
        void unitRefusesAssignment_stopsHiringInsteadOfLoopingForever() {
            Person newHire = createRealPerson();
            stubHiring(newHire);
            // The unit always reports a free slot but never accepts anyone into it
            doReturn(true).when(unit).canTakeMoreVesselCrew();
            doNothing().when(unit).addVesselCrew(any(Person.class));

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                  () -> new HirePersonnelUnitAction(true).execute(mockCampaign, unit));

            verify(mockCampaign.getPlayerForce().getHumanResources(), times(1)).recruitPerson(eq(mockCampaign),
                  eq(newHire),
                  eq(true),
                  eq(true));
            verify(unit, times(1)).addVesselCrew(eq(newHire));
        }
    }
}
