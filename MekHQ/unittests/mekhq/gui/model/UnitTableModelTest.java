 /*
  * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
  *
  * This file is part of MekHQ.
  *
  * MekHQ is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * MekHQ is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
  */
package mekhq.gui.model;

 import megamek.common.Jumpship;
 import megamek.common.SpaceStation;
 import megamek.common.annotations.Nullable;
 import mekhq.campaign.personnel.Person;
 import mekhq.campaign.unit.Unit;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.Mock;
 import org.mockito.junit.jupiter.MockitoExtension;

 import java.util.Collections;
 import java.util.List;

 import static mekhq.gui.model.UnitTableModel.appendReport;
 import static mekhq.gui.model.UnitTableModel.getCrewTooltip;
 import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.mockito.Mockito.lenient;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;

 /**
  * This class contains test cases for the {@link UnitTableModel} class.
  * It tests the different combinations of crew requirements and verifies the behavior of the
  * {@code getCrewTooltip()} and {@code appendReport()} methods.
  */
 @ExtendWith(value = MockitoExtension.class)
 public class UnitTableModelTest {
     // Mock objects for the tests
     @Mock
     private Person person;
     @Mock
     private Unit unit;

     @BeforeEach
     public void setUp() {
         // we use lenient here, because there is only a single test where these are unneeded
         lenient().when(unit.getTotalDriverNeeds()).thenReturn(1);
         lenient().when(unit.getDrivers()).thenReturn(List.of(person));
     }

     /**
      * Returns a list of people with the specified person count.
      *
      * @param personCount the number of persons to include in the list
      * @return a list of people with the specified person count
      */
     private List<Person> getPeople(int personCount) {
         return Collections.nCopies(personCount, person);
     }

     /**
      * Initializes the gunners of a mock unit.
      *
      * @param gunnerNeeds The total number of gunner needs for the unit.
      * @param gunnersAssigned The number of gunners currently assigned to the unit.
      */
     private void initializeGunners(int gunnerNeeds, int gunnersAssigned) {
         when(unit.getTotalGunnerNeeds()).thenReturn(gunnerNeeds);
         when(unit.getGunners()).thenReturn(getPeople(gunnersAssigned));
     }

     /**
      * Initializes the crew of a mock unit.
      *
      * @param crewNeeds The total number of crew members needed for the unit.
      * @param crewAssigned The number of crew members already assigned to the unit.
      */
     private void initializeCrew(int crewNeeds, int crewAssigned) {
         when(unit.getTotalCrewNeeds()).thenReturn(crewNeeds);
         when(unit.getCrew()).thenReturn(getPeople(crewAssigned));
     }

     /**
      * Initializes the entity class based on the given parameters.
      *
      * @param isSpaceStation {@code true} if the entity is a space station, {@code false} if it is
      *                       a JumpShip
      * @param navigator      optional parameter indicating the navigator of the entity, can be
      *                       {@code null}
      */
     private void initializeEntityClass(boolean isSpaceStation, @Nullable Person navigator) {
         if (isSpaceStation) {
             when(unit.getEntity()).thenReturn(mock(SpaceStation.class));
         } else {
             when(unit.getEntity()).thenReturn(mock(Jumpship.class));
         }

         when(unit.getNavigator()).thenReturn(navigator);
     }

     // In the following test the appendReport() method is called, and its response is checked
     // against expected behavior

     @Test
     public void appendReportTest() {
         StringBuilder report = new StringBuilder("<html>");
         String title = "roleName";
         int assigned = 5;
         int needed = 10;

         appendReport(report, title, assigned, needed);

         String expected = "<html><br><b>roleName: </b>5/10";
         assertEquals(expected, report.toString());
     }

     // In the following tests the getCrewTooltip() method is called, and its response is checked
     // against expected behavior

     @Test
     public void noGunner() {
         initializeGunners(0,0);
         initializeEntityClass(true, null);
         initializeCrew(0,0);

         String expected = "<html><br><b>Drivers: </b>1/1</html>";
         assertEquals(expected, getCrewTooltip(unit));
     }

     @Test
     public void noNavigator() {
         initializeGunners(1, 1);
         initializeEntityClass(true, null);
         initializeCrew(0,0);

         String expected = "<html><br><b>Drivers: </b>1/1<br><b>Gunners: </b>1/1</html>";
         assertEquals(expected, getCrewTooltip(unit));
     }

     @Test
     public void yesNavigator() {
         initializeGunners(1, 1);
         initializeEntityClass(false, person);
         initializeCrew(0,0);

         String expected = "<html><br><b>Drivers: </b>1/1<br><b>Gunners: </b>1/1<br><b>Navigator: </b>1/1</html>";
         assertEquals(expected, getCrewTooltip(unit));
     }

     @Test
     public void fullyCrewed() {
         initializeGunners(1, 1);
         initializeEntityClass(false, person);
         initializeCrew(1,1);

         String expected = "<html><br><b>Drivers: </b>1/1<br><b>Gunners: </b>1/1<br><b>Crew: </b>-2/1<br><b>Navigator: </b>1/1</html>";
         assertEquals(expected, getCrewTooltip(unit));
     }

     @Test
     public void partiallyCrewed() {
         initializeGunners(4, 1);
         initializeEntityClass(false, null);
         initializeCrew(3,1);

         String expected = "<html><br><b>Drivers: </b>1/1<br><b>Gunners: </b>1/4<br><b>Crew: </b>-1/3<br><b>Navigator: </b>0/1</html>";
         assertEquals(expected, getCrewTooltip(unit));
     }
 }