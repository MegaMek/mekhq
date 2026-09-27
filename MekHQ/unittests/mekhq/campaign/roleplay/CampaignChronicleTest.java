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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.GroundTransitLocation;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.events.TransitCompleteEvent;
import mekhq.campaign.events.missions.MissionCompletedEvent;
import mekhq.campaign.events.missions.MissionNewEvent;
import mekhq.campaign.events.persons.PersonNewEvent;
import mekhq.campaign.events.persons.PersonStatusChangedEvent;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.roleplay.CampaignChronicle.PersonChange;
import mekhq.campaign.universe.PlanetarySystem;
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
        chronicle.contractStarted(UUID.randomUUID(), "Garrison", "ComStar", null);
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
    void aGroupDeletedFromTheLogStartsAgain() {
        JournalEntry first = chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ana", null);
        roleplay.getOracleLog().remove(first);
        JournalEntry again = chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ben", null);

        assertTrue(first != again, "the deleted entry is not reused");
        assertEquals("Killed: Ben.", again.getText());
        assertEquals(1, roleplay.getOracleLog().size());
    }

    @Test
    void campaignEventsOutliveTheOracleLogLimit() {
        maximum[0] = 1;
        chronicle.personChanged(PersonChange.KILLED, UUID.randomUUID(), "Ana", null);
        chronicle.arrived("Helm");
        assertEquals(2, roleplay.getOracleLog().size());
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

    @Test
    void onlyTheMainForceArrivingIsChronicled() {
        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        Roleplay roleplay = new Roleplay();
        CurrentLocation force = mock(CurrentLocation.class);
        PlanetarySystem galax = mock(PlanetarySystem.class);
        when(galax.getName(any())).thenReturn("Galax");
        when(campaign.getRoleplay()).thenReturn(roleplay);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
        when(campaign.getCurrentSystem()).thenReturn(galax);
        when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation()).thenReturn(force);
        when(campaign.getCampaignOptions().get(CampaignOption.USE_ORACLE_CHRONICLE)).thenReturn(true);
        when(campaign.getCampaignOptions().get(CampaignOption.MAXIMUM_ORACLE_LOG_ENTRIES)).thenReturn(100);
        CampaignChronicleListener listener = new CampaignChronicleListener(campaign);

        // A convoy between bases finishing its journey is not the company arriving.
        listener.handle(new TransitCompleteEvent(mock(GroundTransitLocation.class)));
        assertTrue(roleplay.getOracleLog().stream().noneMatch(entry -> entry.getType() == JournalEntryType.CHRONICLE));

        listener.handle(new TransitCompleteEvent(force));
        assertEquals(1, roleplay.getOracleLog().stream()
                              .filter(entry -> entry.getType() == JournalEntryType.CHRONICLE).count());
    }

    // region Listener

    private static final LocalDate TODAY = LocalDate.of(3025, 1, 1);

    private Campaign campaign;
    private Roleplay journal;
    private final Map<UUID, AbstractContract> contracts = new LinkedHashMap<>();
    private final Map<UUID, Person> personnel = new HashMap<>();

    private CampaignChronicleListener listener() {
        campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        journal = new Roleplay();
        when(campaign.getRoleplay()).thenReturn(journal);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getCampaignOptions().get(CampaignOption.USE_ORACLE_CHRONICLE)).thenReturn(true);
        when(campaign.getCampaignOptions().get(CampaignOption.MAXIMUM_ORACLE_LOG_ENTRIES)).thenReturn(100);
        when(campaign.getContractHistoryAsMap()).thenAnswer(invocation -> new LinkedHashMap<>(contracts));
        when(campaign.getPlayerForce().getHumanResources().getPerson(any()))
              .thenAnswer(invocation -> personnel.get(invocation.<UUID>getArgument(0)));
        return new CampaignChronicleListener(campaign);
    }

    private List<String> chronicled() {
        return journal.getOracleLog().stream().filter(entry -> entry.getType() == JournalEntryType.CHRONICLE)
                     .map(JournalEntry::getText).toList();
    }

    private static AbstractContract contract(final String name, final @Nullable PlanetarySystem system) {
        AbstractContract contract = mock(AbstractContract.class);
        UUID id = UUID.randomUUID();
        when(contract.getId()).thenReturn(id);
        when(contract.getName()).thenReturn(name);
        when(contract.getEmployerDisplayName()).thenReturn("ComStar");
        when(contract.getTargetSystem()).thenReturn(system);
        when(contract.getTargetSystemName(any())).thenReturn(system == null ? "-" : "Helm");
        when(contract.getStatus()).thenReturn(MissionStatus.BREACH);
        return contract;
    }

    private static Person person(final String name, final boolean prisoner, final LocalDate born) {
        Person person = mock(Person.class, RETURNS_DEEP_STUBS);
        when(person.getId()).thenReturn(UUID.randomUUID());
        when(person.getFullName()).thenReturn(name);
        when(person.getPrisonerStatus().isCurrentPrisoner()).thenReturn(prisoner);
        when(person.getDateOfBirth()).thenReturn(born);
        when(person.getPrimaryRoleDesc()).thenReturn("MekWarrior");
        when(person.getStatus()).thenReturn(PersonnelStatus.KIA);
        return person;
    }

    @Test
    void contractsInThisCampaignAreChronicled() {
        CampaignChronicleListener listener = listener();
        AbstractContract hammer = contract("Hammer", mock(PlanetarySystem.class));
        AbstractContract garrison = contract("Garrison", null);
        contracts.put(hammer.getId(), hammer);
        contracts.put(garrison.getId(), garrison);

        listener.handle(new MissionNewEvent(hammer));
        listener.handle(new MissionNewEvent(garrison));
        listener.handle(new MissionCompletedEvent(hammer));

        assertEquals(List.of("Contract accepted: Hammer, for ComStar, at Helm.",
              "Contract accepted: Garrison, for ComStar.", "Contract ended: Hammer (" + MissionStatus.BREACH + ")."),
              chronicled());
        assertEquals(TODAY, journal.getTravelLog().getContractEnd(hammer.getId()));
    }

    @Test
    void contractsFromAnotherCampaignAreIgnored() {
        CampaignChronicleListener listener = listener();
        AbstractContract ours = contract("Hammer", null);
        contracts.put(ours.getId(), ours);
        // The same contract loaded into another copy of the campaign shares its id but is another object.
        UUID id = ours.getId();
        AbstractContract copy = contract("Hammer", null);
        when(copy.getId()).thenReturn(id);

        listener.handle(new MissionNewEvent(copy));
        listener.handle(new MissionCompletedEvent(copy));
        listener.handle(new MissionNewEvent(contract("Stranger", null)));

        assertTrue(chronicled().isEmpty());
        assertNull(journal.getTravelLog().getContractEnd(ours.getId()));
    }

    @Test
    void onlyThisCampaignsBattlesAreChronicled() {
        CampaignChronicleListener listener = listener();
        Scenario ours = mock(Scenario.class, RETURNS_DEEP_STUBS);
        when(ours.getId()).thenReturn(7);
        when(ours.getName()).thenReturn("Ridge Assault");
        Scenario copy = mock(Scenario.class, RETURNS_DEEP_STUBS);
        when(copy.getId()).thenReturn(7);
        when(campaign.getScenario(7)).thenReturn(ours);

        listener.handle(new ScenarioResolvedEvent(copy));
        assertTrue(chronicled().isEmpty());
        listener.handle(new ScenarioResolvedEvent(ours));
        assertEquals(1, chronicled().size());
        assertTrue(chronicled().get(0).startsWith("Battle fought: Ridge Assault"), chronicled().get(0));
    }

    @Test
    void newPeopleAreChronicledButPrisonersAreNot() {
        CampaignChronicleListener listener = listener();
        Person recruit = person("Ana", false, TODAY.minusYears(20));
        Person baby = person("Ben", false, TODAY);
        Person prisoner = person("Cole", true, TODAY.minusYears(30));
        for (Person person : List.of(recruit, baby, prisoner)) {
            personnel.put(person.getId(), person);
            listener.handle(new PersonNewEvent(person));
        }

        assertEquals(2, chronicled().size());
        assertTrue(chronicled().stream().anyMatch(text -> text.contains("Ana (MekWarrior)")), chronicled().toString());
        assertTrue(chronicled().stream().anyMatch(text -> text.contains("Ben") && !text.contains("Ana")),
              chronicled().toString());
        assertTrue(chronicled().stream().noneMatch(text -> text.contains("Cole")), chronicled().toString());
    }

    @Test
    void statusChangesForPeopleFromAnotherCampaignAreIgnored() {
        CampaignChronicleListener listener = listener();
        Person ours = person("Ana", false, TODAY.minusYears(20));
        personnel.put(ours.getId(), ours);
        UUID id = ours.getId();
        Person copy = person("Ana", false, TODAY.minusYears(20));
        when(copy.getId()).thenReturn(id);

        listener.handle(new PersonStatusChangedEvent(copy));
        listener.handle(new PersonStatusChangedEvent(person("Stranger", false, TODAY.minusYears(20))));
        assertTrue(chronicled().isEmpty());
        listener.handle(new PersonStatusChangedEvent(ours));
        assertEquals(1, chronicled().size());
    }

    // endregion Listener
}
