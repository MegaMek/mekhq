package mekhq.campaign.unit.actions;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.SmallCraft;
import megamek.common.SupportTank;
import megamek.common.Tank;
import megamek.common.TankTrailerHitch;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class HirePersonnelUnitActionTest {
    @Test
    public void fullUnitTakesNoAction() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Mech.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));

        doReturn(false).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(false).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(false).when(unit).canTakeTechOfficer();
        doNothing().when(unit).resetPilotAndEntity();
        doNothing().when(unit).runDiagnostic(false);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(0)).newPerson(eq(Person.T_MECHWARRIOR));
        verify(mockCampaign, times(0)).recruitPerson(any(Person.class), eq(false));
    }

    @Test
    public void actionPassesAlongGMSetting() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Mech.class);
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
        when(mockCampaign.newPerson(eq(Person.T_MECHWARRIOR))).thenReturn(mockDriver);
        when(mockCampaign.recruitPerson(eq(mockDriver), eq(true))).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(true);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_MECHWARRIOR));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(true));
        verify(unit, times(1)).addPilotOrSoldier(eq(mockDriver));
    }

    @Test
    public void mechNeedingDriverAddsDriver() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Mech.class);
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
        when(mockCampaign.newPerson(eq(Person.T_MECHWARRIOR))).thenReturn(mockDriver);
        when(mockCampaign.recruitPerson(eq(mockDriver), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_MECHWARRIOR));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).addPilotOrSoldier(eq(mockDriver));
    }

    @Test
    public void mechDoesntAddDriverIfRecruitFails() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Mech.class);
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
        when(mockCampaign.newPerson(eq(Person.T_MECHWARRIOR))).thenReturn(mockDriver);
        when(mockCampaign.recruitPerson(eq(mockDriver), anyBoolean())).thenReturn(false);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_MECHWARRIOR));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(0)).addPilotOrSoldier(eq(mockDriver));
    }

    @Test
    public void lamNeedingDriverAddsDriver() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(LandAirMech.class);
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
        when(mockCampaign.newPerson(eq(Person.T_MECHWARRIOR), eq(Person.T_AERO_PILOT))).thenReturn(mockDriver);
        when(mockCampaign.recruitPerson(eq(mockDriver), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_MECHWARRIOR), eq(Person.T_AERO_PILOT));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).addPilotOrSoldier(eq(mockDriver));
    }

    @Test
    public void mechNeedingGunnerAddsGunner() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Mech.class);
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
        when(mockCampaign.newPerson(eq(Person.T_MECHWARRIOR))).thenReturn(mockGunner);
        when(mockCampaign.recruitPerson(eq(mockGunner), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_MECHWARRIOR));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).addGunner(eq(mockGunner));
    }

    @Test
    public void tankNeedingGunnerAddsGunner() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Tank.class);
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
        when(mockCampaign.newPerson(eq(Person.T_VEE_GUNNER))).thenReturn(mockGunner);
        when(mockCampaign.recruitPerson(eq(mockGunner), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_VEE_GUNNER));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).addGunner(eq(mockGunner));
    }

    @Test
    public void spaceShipNeedingGunnerAddsGunner() {
        Campaign mockCampaign = mock(Campaign.class);
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
        when(mockCampaign.newPerson(eq(Person.T_SPACE_GUNNER))).thenReturn(mockGunner);
        when(mockCampaign.recruitPerson(eq(mockGunner), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_SPACE_GUNNER));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).addGunner(eq(mockGunner));
    }

    @Test
    public void smallCraftNeedingGunnerAddsGunner() {
        Campaign mockCampaign = mock(Campaign.class);
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
        when(mockCampaign.newPerson(eq(Person.T_SPACE_GUNNER))).thenReturn(mockGunner);
        when(mockCampaign.recruitPerson(eq(mockGunner), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_SPACE_GUNNER));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).addGunner(eq(mockGunner));
    }

    @Test
    public void spaceShipNeedingCrewAddsCrew() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Jumpship.class);
        when(mockEntity.isSupportVehicle()).thenReturn(false);
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
        when(mockCampaign.newPerson(eq(Person.T_SPACE_CREW))).thenReturn(mockCrew);
        when(mockCampaign.recruitPerson(eq(mockCrew), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_SPACE_CREW));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).addVesselCrew(eq(mockCrew));
    }

    @Test
    public void supportVehicleNeedingCrewAddsCrew() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(SupportTank.class);
        when(mockEntity.isSupportVehicle()).thenReturn(true);
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
        when(mockCampaign.newPerson(eq(Person.T_VEHICLE_CREW))).thenReturn(mockCrew);
        when(mockCampaign.recruitPerson(eq(mockCrew), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_VEHICLE_CREW));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).addVesselCrew(eq(mockCrew));
    }

    @Test
    public void spaceShipNeedingNavigatorAddsNavigator() {
        Campaign mockCampaign = mock(Campaign.class);
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
        when(mockCampaign.newPerson(eq(Person.T_NAVIGATOR))).thenReturn(mockNavigator);
        when(mockCampaign.recruitPerson(eq(mockNavigator), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_NAVIGATOR));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).setNavigator(eq(mockNavigator));
    }

    @Test
    public void mechNeedingTechOfficerAddsTechOfficer() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Mech.class);
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
        when(mockCampaign.newPerson(eq(Person.T_MECHWARRIOR))).thenReturn(mockTechOfficer);
        when(mockCampaign.recruitPerson(eq(mockTechOfficer), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_MECHWARRIOR));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).setTechOfficer(eq(mockTechOfficer));
    }

    @Test
    public void tankNeedingTechOfficerAddsTechOfficer() {
        Campaign mockCampaign = mock(Campaign.class);
        Entity mockEntity = mock(Tank.class);
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
        when(mockCampaign.newPerson(eq(Person.T_VEE_GUNNER))).thenReturn(mockTechOfficer);
        when(mockCampaign.recruitPerson(eq(mockTechOfficer), anyBoolean())).thenReturn(true);

        HirePersonnelUnitAction action = new HirePersonnelUnitAction(false);
        action.Execute(mockCampaign, unit);

        verify(mockCampaign, times(1)).newPerson(eq(Person.T_VEE_GUNNER));
        verify(mockCampaign, times(1)).recruitPerson(any(Person.class), eq(false));
        verify(unit, times(1)).setTechOfficer(eq(mockTechOfficer));
    }
}
