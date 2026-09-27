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
package mekhq.campaign.roleplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.roleplay.CampaignChronicle.PersonChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignChronicleTest {
    private Roleplay roleplay;
    private LocalDate[] today;
    private int[] maximum;
    private boolean[] enabled;
    private CampaignChronicle chronicle;

    @BeforeEach
    void setUp() {
        roleplay = new Roleplay();
        today = new LocalDate[] { LocalDate.of(3025, 6, 1) };
        maximum = new int[] { 500 };
        enabled = new boolean[] { true };
        chronicle = new CampaignChronicle(roleplay, () -> today[0], () -> maximum[0], () -> enabled[0]);
    }

    @Test
    void contractsBattlesAndArrivalsAreRecorded() {
        UUID contract = UUID.randomUUID();
        chronicle.contractStarted(contract, "Operation Hammer", "Lyran Commonwealth", "Helm");
        chronicle.battleResolved(7, "Ridge Assault", "Victory");
        chronicle.arrived("Tharkad");
        chronicle.contractEnded(contract, "Operation Hammer", "Success");

        assertEquals(4, roleplay.getOracleLog().size());
        assertEquals("Contract accepted: Operation Hammer, for Lyran Commonwealth, at Helm.",
              roleplay.getOracleLog().get(0).getText());
        assertEquals("Battle fought: Ridge Assault (Victory).", roleplay.getOracleLog().get(1).getText());
        assertEquals("Arrived at Tharkad.", roleplay.getOracleLog().get(2).getText());
        assertEquals("Contract ended: Operation Hammer (Success).", roleplay.getOracleLog().get(3).getText());
        assertTrue(roleplay.getOracleLog().stream().allMatch(entry -> entry.getType() == JournalEntryType.CHRONICLE));
    }

    @Test
    void aContractWithNoSystemLeavesItOut() {
        chronicle.contractStarted(UUID.randomUUID(), "Garrison", "ComStar", " ");
        assertEquals("Contract accepted: Garrison, for ComStar.", roleplay.getOracleLog().get(0).getText());
    }

    @Test
    void anEventAnnouncedTwiceIsRecordedOnceADay() {
        UUID contract = UUID.randomUUID();
        assertNotNull(chronicle.contractStarted(contract, "Hammer", "Lyrans", null));
        assertNull(chronicle.contractStarted(contract, "Hammer", "Lyrans", null));
        assertNotNull(chronicle.arrived("Helm"));
        assertNull(chronicle.arrived("Helm"), "a second arrival the same day");
        today[0] = today[0].plusDays(1);
        assertNotNull(chronicle.arrived("Helm"), "a new day is a new arrival");
    }

    @Test
    void nothingIsRecordedWhenTheChronicleIsOff() {
        enabled[0] = false;
        assertNull(chronicle.arrived("Helm"));
        assertNull(chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ana", "KIA"));
        assertTrue(roleplay.getOracleLog().isEmpty());
    }

    @Test
    void theDaysLossesShareOneEntry() {
        JournalEntry first = chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ana", "Killed in Action");
        JournalEntry second = chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ben", null);

        assertSame(first, second);
        assertEquals(1, roleplay.getOracleLog().size());
        assertEquals("Killed: Ana (Killed in Action), Ben.", first.getText());
        assertEquals("Killed: Ana (Killed in Action), Ben.", first.getPlainText());
    }

    @Test
    void differentChangesAndDaysGetTheirOwnEntries() {
        chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ana", null);
        chronicle.personChanged(PersonChange.JOINED, UUID.randomUUID(), "Cal", "MekWarrior");
        today[0] = today[0].plusDays(1);
        chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Dee", null);

        assertEquals(3, roleplay.getOracleLog().size());
        assertEquals("Joined the company: Cal (MekWarrior).", roleplay.getOracleLog().get(1).getText());
        assertEquals("Killed: Dee.", roleplay.getOracleLog().get(2).getText());
    }

    @Test
    void theSamePersonIsListedOnce() {
        UUID ana = UUID.randomUUID();
        chronicle.personChanged(PersonChange.KILLED, ana, "Ana", null);
        assertNull(chronicle.personChanged(PersonChange.KILLED, ana, "Ana", null));
        assertEquals("Killed: Ana.", roleplay.getOracleLog().get(0).getText());
    }

    @Test
    void castMembersAreTagged() {
        UUID natashaId = UUID.randomUUID();
        OracleCharacter natasha = roleplay.addLinkedCharacter(natashaId, "Natasha");
        JournalEntry entry = chronicle.personChanged(PersonChange.MISSING, natashaId, "Natasha", null);
        chronicle.personChanged(PersonChange.MISSING, UUID.randomUUID(), "Stranger", null);

        assertEquals(Set.of(natasha.getId()), entry.getCharacters());
    }

    @Test
    void aGroupTrimmedFromTheLogStartsAgain() {
        maximum[0] = 1;
        JournalEntry first = chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ana", null);
        chronicle.arrived("Helm");
        JournalEntry again = chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ben", null);

        assertTrue(first != again, "the trimmed entry is not reused");
        assertEquals("Killed: Ben.", again.getText());
        assertEquals(1, roleplay.getOracleLog().size());
    }

    @Test
    void statusesMapToTheRightEntry() {
        assertEquals(PersonChange.KILLED, CampaignChronicleListener.changeFor(PersonnelStatus.KIA));
        assertEquals(PersonChange.MISSING, CampaignChronicleListener.changeFor(PersonnelStatus.MIA));
        assertEquals(PersonChange.CAPTURED, CampaignChronicleListener.changeFor(PersonnelStatus.POW));
        assertEquals(PersonChange.DEPARTED, CampaignChronicleListener.changeFor(PersonnelStatus.RETIRED));
        assertEquals(PersonChange.DEPARTED, CampaignChronicleListener.changeFor(PersonnelStatus.DESERTED));
        assertNull(CampaignChronicleListener.changeFor(PersonnelStatus.ACTIVE));
        assertNull(CampaignChronicleListener.changeFor(PersonnelStatus.ON_LEAVE));
    }
}
