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

 import megamek.common.Entity;
 import megamek.common.Jumpship;
 import megamek.common.SpaceStation;
 import mekhq.campaign.personnel.Person;
 import mekhq.campaign.unit.Unit;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.Mock;
 import org.mockito.junit.jupiter.MockitoExtension;

 import java.util.Collections;
 import java.util.List;

 import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;

 /**
  * This class contains test cases for the {@link UnitTableModel} class.
  * It tests the different combinations of crew requirements and verifies the behavior of the
  * {@code getCrewTooltip()} method.
  */
@ExtendWith(value = MockitoExtension.class)
public class UnitTableModelTest {
    @Mock
    private Unit unit;
    @Mock
    private Person crewMember;

    /**
     * Sets the crew of a unit with the given parameters.
     *
     * @param driverCount        The number of drivers in the unit.
     * @param totalDriverNeeds   The total number of drivers needed for the unit.
     * @param gunnerCount        The number of gunners in the unit.
     * @param totalGunnerNeeds   The total number of gunners needed for the unit.
     * @param crewCount          The number of crew members (excluding drivers and gunners) in the unit.
     * @param totalCrewNeeds     The total number of crew members needed for the unit.
     * @param hasNavigator       Indicates whether the unit has a navigator.
     * @param entity             The entity associated with the unit.
     * @param expected           The expected result for the crew tooltip.
     */
    private void setCrew(int driverCount, int totalDriverNeeds,
                         int gunnerCount, int totalGunnerNeeds,
                         int crewCount, int totalCrewNeeds,
                         boolean hasNavigator, Entity entity,
                         String expected) {
        List<Person> drivers = Collections.nCopies(driverCount, crewMember);
        List<Person> gunners = Collections.nCopies(gunnerCount, crewMember);
        List<Person> crew = Collections.nCopies(crewCount + drivers.size() + gunners.size()
                + (hasNavigator ? 1 : 0), crewMember);

        when(unit.getDrivers()).thenReturn(drivers);
        when(unit.getTotalDriverNeeds()).thenReturn(totalDriverNeeds);

        when(unit.getGunners()).thenReturn(gunners);
        when(unit.getTotalGunnerNeeds()).thenReturn(totalGunnerNeeds);

        when(unit.getCrew()).thenReturn(crew);
        when(unit.getTotalCrewNeeds()).thenReturn(totalCrewNeeds);

        when(unit.getEntity()).thenReturn(entity);
        Person navigator = hasNavigator ? mock(Person.class) : null;
        when(unit.getNavigator()).thenReturn(navigator);

        String result = UnitTableModel.getCrewTooltip(unit);
        assertEquals(expected, result);
    }

     // In the following tests the getCrewTooltip() method is called, and its responses are checked
     // against expected behavior

    @Test
    public void noCrewNeeded() {
        setCrew(0, 0,
                0, 0,
                0, 0,
                true, mock(SpaceStation.class),
                "<html></html>");
    }

    @Test
    public void fullyCrewed() {
        setCrew(1, 1,
                2, 2,
                3, 3,
                true, mock(Jumpship.class),
                "<html><b>Drivers: </b>1/1<br><b>Gunners: </b>2/2<br><b>Crew: </b>3/3<br><b>Navigator: </b>1/1</html>");
    }

    @Test
    public void partiallyCrewed() {
        setCrew(0, 1,
                1, 2,
                2, 3,
                false, mock(Jumpship.class),
                "<html><b>Drivers: </b>0/1<br><b>Gunners: </b>1/2<br><b>Crew: </b>2/3<br><b>Navigator: </b>0/1</html>");
    }

    @Test
    public void excessCrew() {
        setCrew(2, 1,
                4, 2,
                6, 5,
                true, mock(SpaceStation.class),
                "<html><b>Drivers: </b>2/1<br><b>Gunners: </b>4/2<br><b>Crew: </b>6/5</html>");
    }

    @Test
    public void noAssignedCrew() {
        setCrew(0, 1,
                0, 1,
                0, 1,
                false, mock(Jumpship.class),
                "<html><b>Drivers: </b>0/1<br><b>Gunners: </b>0/1<br><b>Crew: </b>0/1<br><b>Navigator: </b>0/1</html>");
    }
}