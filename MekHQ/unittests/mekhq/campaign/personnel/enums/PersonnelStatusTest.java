/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;

class PersonnelStatusTest {
    // region Variable Declarations
    private static final PersonnelStatus[] statuses = PersonnelStatus.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testGetToolTipText() {
        assertEquals(resources.getString("PersonnelStatus.MIA.toolTipText"),
                PersonnelStatus.MIA.getToolTipText());
        assertEquals(resources.getString("PersonnelStatus.MEDICAL_COMPLICATIONS.toolTipText"),
                PersonnelStatus.MEDICAL_COMPLICATIONS.getToolTipText());
    }

    @Test
    void testGetReportText() {
        assertEquals(resources.getString("PersonnelStatus.AWOL.reportText"),
                PersonnelStatus.AWOL.getReportText());
        assertEquals(resources.getString("PersonnelStatus.ACCIDENTAL.reportText"),
                PersonnelStatus.ACCIDENTAL.getReportText());
    }

    @Test
    void testGetLogText() {
        assertEquals(resources.getString("PersonnelStatus.DESERTED.logText"),
                PersonnelStatus.DESERTED.getLogText());
        assertEquals(resources.getString("PersonnelStatus.HOMICIDE.logText"),
                PersonnelStatus.HOMICIDE.getLogText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsActive() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.ACTIVE) {
                assertTrue(personnelStatus.isActive());
            } else {
                assertFalse(personnelStatus.isActive());
            }
        }
    }

    @Test
    void testIsMIA() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.MIA) {
                assertTrue(personnelStatus.isMIA());
            } else {
                assertFalse(personnelStatus.isMIA());
            }
        }
    }

    @Test
    void testIsPoW() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.POW) {
                assertTrue(personnelStatus.isPoW());
            } else {
                assertFalse(personnelStatus.isPoW());
            }
        }
    }

    @Test
    void testIsOnLeave() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.ON_LEAVE) {
                assertTrue(personnelStatus.isOnLeave());
            } else {
                assertFalse(personnelStatus.isOnLeave());
            }
        }
    }

    @Test
    void testIsAwol() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.AWOL) {
                assertTrue(personnelStatus.isAwol());
            } else {
                assertFalse(personnelStatus.isAwol());
            }
        }
    }

    @Test
    void testIsRetired() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.RETIRED) {
                assertTrue(personnelStatus.isRetired());
            } else {
                assertFalse(personnelStatus.isRetired());
            }
        }
    }

    @Test
    void testIsDeserted() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.DESERTED) {
                assertTrue(personnelStatus.isDeserted());
            } else {
                assertFalse(personnelStatus.isDeserted());
            }
        }
    }

    @Test
    void testIsStudent() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.STUDENT) {
                assertTrue(personnelStatus.isStudent());
            } else {
                assertFalse(personnelStatus.isStudent());
            }
        }
    }

    @Test
    void testIsKIA() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.KIA) {
                assertTrue(personnelStatus.isKIA());
            } else {
                assertFalse(personnelStatus.isKIA());
            }
        }
    }

    @Test
    void testIsHomicide() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.HOMICIDE) {
                assertTrue(personnelStatus.isHomicide());
            } else {
                assertFalse(personnelStatus.isHomicide());
            }
        }
    }

    @Test
    void testIsWounds() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.WOUNDS) {
                assertTrue(personnelStatus.isWounds());
            } else {
                assertFalse(personnelStatus.isWounds());
            }
        }
    }

    @Test
    void testIsDisease() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.DISEASE) {
                assertTrue(personnelStatus.isDisease());
            } else {
                assertFalse(personnelStatus.isDisease());
            }
        }
    }

    @Test
    void testIsAccidental() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.ACCIDENTAL) {
                assertTrue(personnelStatus.isAccidental());
            } else {
                assertFalse(personnelStatus.isAccidental());
            }
        }
    }

    @Test
    void testIsNaturalCauses() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.NATURAL_CAUSES) {
                assertTrue(personnelStatus.isNaturalCauses());
            } else {
                assertFalse(personnelStatus.isNaturalCauses());
            }
        }
    }

    @Test
    void testIsOldAge() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.OLD_AGE) {
                assertTrue(personnelStatus.isOldAge());
            } else {
                assertFalse(personnelStatus.isOldAge());
            }
        }
    }

    @Test
    void testIsMedicalComplications() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.MEDICAL_COMPLICATIONS) {
                assertTrue(personnelStatus.isMedicalComplications());
            } else {
                assertFalse(personnelStatus.isMedicalComplications());
            }
        }
    }

    @Test
    void testIsPregnancyComplications() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.PREGNANCY_COMPLICATIONS) {
                assertTrue(personnelStatus.isPregnancyComplications());
            } else {
                assertFalse(personnelStatus.isPregnancyComplications());
            }
        }
    }

    @Test
    void testIsUndetermined() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.UNDETERMINED) {
                assertTrue(personnelStatus.isUndetermined());
            } else {
                assertFalse(personnelStatus.isUndetermined());
            }
        }
    }

    @Test
    void testIsSuicide() {
        for (final PersonnelStatus personnelStatus : statuses) {
            if (personnelStatus == PersonnelStatus.SUICIDE) {
                assertTrue(personnelStatus.isSuicide());
            } else {
                assertFalse(personnelStatus.isSuicide());
            }
        }
    }

    @Test
    void testIsAbsent() {
        for (final PersonnelStatus personnelStatus : statuses) {
            switch (personnelStatus) {
                case MIA:
                case POW:
                case ON_LEAVE:
                case STUDENT:
                case MISSING:
                case AWOL:
                    assertTrue(personnelStatus.isAbsent());
                    break;
                default:
                    assertFalse(personnelStatus.isAbsent());
                    break;
            }
        }
    }

    @Test
    void testIsDead() {
        for (final PersonnelStatus personnelStatus : statuses) {
            switch (personnelStatus) {
                case KIA:
                case HOMICIDE:
                case WOUNDS:
                case DISEASE:
                case ACCIDENTAL:
                case NATURAL_CAUSES:
                case OLD_AGE:
                case MEDICAL_COMPLICATIONS:
                case PREGNANCY_COMPLICATIONS:
                case UNDETERMINED:
                case SUICIDE:
                    assertTrue(personnelStatus.isDead());
                    break;
                default:
                    assertFalse(personnelStatus.isDead());
                    break;
            }
        }
    }

    @Test
    void testIsDeadOrMIA() {
        for (final PersonnelStatus personnelStatus : statuses) {
            switch (personnelStatus) {
                case MIA:
                case KIA:
                case HOMICIDE:
                case WOUNDS:
                case DISEASE:
                case ACCIDENTAL:
                case NATURAL_CAUSES:
                case OLD_AGE:
                case MEDICAL_COMPLICATIONS:
                case PREGNANCY_COMPLICATIONS:
                case UNDETERMINED:
                case SUICIDE:
                    assertTrue(personnelStatus.isDeadOrMIA());
                    break;
                default:
                    assertFalse(personnelStatus.isDeadOrMIA());
                    break;
            }
        }
    }
    // endregion Boolean Comparison Methods

    @Test
    void testGetImplementedStatuses() {
        final List<PersonnelStatus> implementedStatuses = PersonnelStatus.getImplementedStatuses();
        for (final PersonnelStatus personnelStatus : statuses) {
            assertTrue(implementedStatuses.contains(personnelStatus));
        }
    }

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(PersonnelStatus.RETIRED, PersonnelStatus.parseFromString("RETIRED"));
        assertEquals(PersonnelStatus.PREGNANCY_COMPLICATIONS,
                PersonnelStatus.parseFromString("PREGNANCY_COMPLICATIONS"));

        // Error Case
        assertEquals(PersonnelStatus.ACTIVE, PersonnelStatus.parseFromString("23"));
        assertEquals(PersonnelStatus.ACTIVE, PersonnelStatus.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("PersonnelStatus.KIA.text"), PersonnelStatus.KIA.toString());
        assertEquals(resources.getString("PersonnelStatus.PREGNANCY_COMPLICATIONS.text"),
                PersonnelStatus.PREGNANCY_COMPLICATIONS.toString());
    }
}
