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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.universe.Atmosphere;
import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.LifeForm;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySystemEvent;
import mekhq.campaign.universe.PlanetarySystemYamlIO;
import mekhq.campaign.universe.Satellite;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.SourceableValue;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.enums.PlanetaryType;
import org.junit.jupiter.api.Test;

class PlanetarySystemChangeSummaryTest {
    private static final LocalDate EVENT_DATE = LocalDate.of(3000, 1, 1);
    private static final LocalDate EDIT_DATE = LocalDate.of(3050, 1, 1);

    private static final String SYSTEM = """
          id: Summary Test
          xcood: 1.25
          ycood: -2.5
          spectralType: G2V
          primarySlot: 1
          planet:
            - name: Summary Test Prime
              type: TERRESTRIAL
              orbitalDist: 1.0
              sysPos: 1
              pressure: STANDARD
              atmosphere: BREATHABLE
              gravity: 1.0
              diameter: 12000
              density: 5.5
              dayLength: 24
              yearLength: 1.0
              temperature: 20
              water: 70
              event:
                - date: '3000-01-01'
                  faction:
                    value:
                      - FS
                  socioIndustrial: C-C-C-C-C
          """;

    private static final String TWO_PLANET_SYSTEM = """
          id: Summary Test
          xcood: 1.25
          ycood: -2.5
          spectralType: G2V
          primarySlot: 1
          planet:
            - name: Summary Test Prime
              type: TERRESTRIAL
              orbitalDist: 1.0
              sysPos: 1
              pressure: STANDARD
              atmosphere: BREATHABLE
              gravity: 1.0
              diameter: 12000
              density: 5.5
              dayLength: 24
              yearLength: 1.0
              temperature: 20
              water: 70
              event:
                - date: '3000-01-01'
                  faction:
                    value:
                      - FS
                  socioIndustrial: C-C-C-C-C
            - name: Summary Test Secundus
              type: GAS_GIANT
              orbitalDist: 5.0
              sysPos: 2
          """;

    @Test
    void summarizeIgnoresMissingSystems() throws Exception {
        PlanetarySystem system = readSystem();

        assertTrue(PlanetarySystemChangeSummary.summarize(null, system, EVENT_DATE).isEmpty());
        assertTrue(PlanetarySystemChangeSummary.summarize(system, null, EVENT_DATE).isEmpty());
        assertTrue(PlanetarySystemChangeSummary.summarizeForPlanet(null, system, system.getPrimaryPlanet(),
              EVENT_DATE).isEmpty());
        assertFalse(PlanetarySystemChangeSummary.hasChangesForPlanet(null, system, system.getPrimaryPlanet(),
              EVENT_DATE));
    }

    @Test
    void summarizeAddedAndRemovedPlanets() throws Exception {
        List<String> addedChanges = PlanetarySystemChangeSummary.summarize(readSystem(), readTwoPlanetSystem(),
              EVENT_DATE);
        List<String> removedChanges = PlanetarySystemChangeSummary.summarize(readTwoPlanetSystem(), readSystem(),
              EVENT_DATE);

        assertTrue(addedChanges.contains("Summary Test Secundus: Added planet"));
        assertTrue(removedChanges.contains("Summary Test Secundus: Removed planet"));
    }

    @Test
    void summarizeStaticPlanetFieldChanges() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        Planet editedPlanet = edited.getPrimaryPlanet();

        editedPlanet.setSourcedName(SourceableValue.of("Edited Prime"));
        editedPlanet.setSourcedPlanetType(SourceableValue.of(PlanetaryType.GAS_GIANT));
        editedPlanet.setSourcedGravity(SourceableValue.of(1.25));
        editedPlanet.setSourcedDiameter(SourceableValue.of(13000.0));
        editedPlanet.setSourcedDayLength(SourceableValue.of(26.0));
        editedPlanet.setSourcedYearLength(SourceableValue.of(1.5));
        editedPlanet.setSourcedTemperature(SourceableValue.of(32));
        editedPlanet.setSourcedPressure(SourceableValue.of(megamek.common.planetaryConditions.Atmosphere.HIGH));
        editedPlanet.setSourcedAtmosphere(SourceableValue.of(Atmosphere.TOXIC_POISON));
        editedPlanet.setSourcedComposition(SourceableValue.of("Nitrogen-oxygen mix"));
        editedPlanet.setSourcedPercentWater(SourceableValue.of(55));
        editedPlanet.setSourcedLifeForm(SourceableValue.of(LifeForm.MAMMAL));
        editedPlanet.setSourcedSmallMoons(SourceableValue.of(2));
        editedPlanet.setSourcedRing(SourceableValue.of(true));
        editedPlanet.setDescription("Updated description");

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Edited Prime: name changed from Summary Test Prime to Edited Prime"));
        assertTrue(changes.contains("Edited Prime: type changed from Terrestrial to Gas Giant"));
        assertTrue(changes.contains("Edited Prime: gravity changed from 1.0 to 1.25"));
        assertTrue(changes.contains("Edited Prime: diameter changed from 12000.0 to 13000.0"));
        assertTrue(changes.contains("Edited Prime: day length changed from 24.0 to 26.0"));
        assertTrue(changes.contains("Edited Prime: year length changed from 1.0 to 1.5"));
        assertTrue(changes.contains("Edited Prime: temperature changed from 20 to 32"));
        assertTrue(changes.contains("Edited Prime: atmosphere changed from Breathable to Toxic (Poisonous)"));
        assertTrue(changes.contains("Edited Prime: composition changed from none to Nitrogen-oxygen mix"));
        assertTrue(changes.contains("Edited Prime: % water changed from 70 to 55"));
        assertTrue(changes.contains("Edited Prime: life form changed from none to Mammals"));
        assertTrue(changes.contains("Edited Prime: small moons changed from none to 2"));
        assertTrue(changes.contains("Edited Prime: ring changed from none to true"));
        assertTrue(changes.contains("Edited Prime: description changed from none to Updated description"));
    }

    @Test
    void summarizeLandMassAndSatelliteChanges() throws Exception {
        PlanetarySystem baseline = readSystem();
        Planet baselinePlanet = baseline.getPrimaryPlanet();
        baselinePlanet.setLandMasses(List.of(landMass("Northreach", "Avalon")));
        baselinePlanet.setSatellites(List.of(satellite("Luna", "medium"), satellite("Phobos", "small")));

        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        Planet editedPlanet = edited.getPrimaryPlanet();
        editedPlanet.setLandMasses(List.of(landMass("Southreach", "New Avalon"),
              landMass("New Continent", "New Capital")));
        editedPlanet.setSatellites(List.of(satellite("Selene", "large")));

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Summary Test Prime: landmass [1] name changed from Northreach to Southreach"));
        assertTrue(changes.contains("Summary Test Prime: landmass [1] capital changed from Avalon to New Avalon"));
        assertTrue(changes.contains("Summary Test Prime: Added landmass New Continent"));
        assertTrue(changes.contains("Summary Test Prime: satellite [1] name changed from Luna to Selene"));
        assertTrue(changes.contains("Summary Test Prime: satellite [1] size changed from medium to large"));
        assertTrue(changes.contains("Summary Test Prime: Removed satellite Phobos"));
    }

    @Test
    void summarizeAddedEvent() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);

        PlanetaryEvent event = new PlanetaryEvent();
        event.date = EDIT_DATE;
        event.population = SourceableValue.of(1234567L);
        edited.getPrimaryPlanet().putEvent(event);

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Summary Test Prime: Added event on 3050-01-01"));
    }

    @Test
    void summarizeRemovedEvent() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);

        edited.getPrimaryPlanet().removeEvent(EVENT_DATE);

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Summary Test Prime: Removed event on 3000-01-01"));
    }

    @Test
    void summarizeChangedEventFields() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        Planet editedPlanet = edited.getPrimaryPlanet();
        PlanetaryEvent event = editedPlanet.getEvent(EVENT_DATE);
        event.faction = SourceableValue.of("Handbook", "1.2", List.of("LC"));
        event.population = SourceableValue.of(1234567L);
        event.hpg = SourceableValue.of(HPGRating.A);
        event.socioIndustrial = SourceableValue.of(new SocioIndustrialData(PlanetarySophistication.A,
              PlanetaryRating.A, PlanetaryRating.A, PlanetaryRating.A, PlanetaryRating.A));
        event.message = "Major revision";
        event.custom = true;

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 factions changed from FS to LC"));
        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 population changed from none to 1,234,567"));
        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 HPG changed from none to A-rated"));
        assertTrue(changes.contains(
              "Summary Test Prime: 3000-01-01 socio-industrial code changed from C-C-C-C-C to A-A-A-A-A"));
        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 source changed from none to Handbook"));
        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 version changed from none to 1.2"));
        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 message changed from none to Major revision"));
        assertTrue(changes.contains("Summary Test Prime: 3000-01-01 custom flag changed from false to true"));
    }

    @Test
    void summarizeSystemEventChanges() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        baseline.putEvent(systemEvent(EDIT_DATE, true, null));
        edited.putEvent(systemEvent(EDIT_DATE.plusDays(1), null, true));

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains("Summary Test Prime: Removed system event on 3050-01-01"));
        assertTrue(changes.contains("Summary Test Prime: Added system event on 3050-01-02"));
    }

    @Test
    void summarizeChangedSystemEventFields() throws Exception {
        PlanetarySystem baseline = readSystem();
        baseline.putEvent(systemEvent(EDIT_DATE, true, null));
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        edited.putEvent(systemEvent(EDIT_DATE, false, true));

        List<String> changes = PlanetarySystemChangeSummary.summarize(baseline, edited, EVENT_DATE);

        assertTrue(changes.contains(
              "Summary Test Prime: 3050-01-01 system nadir charge changed from true to false"));
        assertTrue(changes.contains(
              "Summary Test Prime: 3050-01-01 system zenith charge changed from none to true"));
    }

    @Test
    void detectsChangedPlanet() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        Planet editedPlanet = edited.getPrimaryPlanet();
        PlanetaryEvent event = editedPlanet.getEvent(EVENT_DATE);
        event.population = SourceableValue.of(1234567L);

        assertTrue(PlanetarySystemChangeSummary.hasChangesForPlanet(baseline, edited, editedPlanet, EVENT_DATE));
    }

    @Test
    void ignoresUnchangedPlanet() throws Exception {
        PlanetarySystem baseline = readSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);

        assertFalse(PlanetarySystemChangeSummary.hasChangesForPlanet(baseline, edited, edited.getPrimaryPlanet(),
              EVENT_DATE));
    }

    @Test
    void summarizeForPlanetReturnsOnlySelectedPlanetChanges() throws Exception {
        PlanetarySystem baseline = readTwoPlanetSystem();
        PlanetarySystem edited = PlanetarySystemYamlIO.copy(baseline);
        edited.getPlanet(1).setSourcedGravity(SourceableValue.of(1.25));
        edited.getPlanet(2).setSourcedGravity(SourceableValue.of(2.5));

        List<String> changes = PlanetarySystemChangeSummary.summarizeForPlanet(baseline, edited, edited.getPlanet(1),
              EVENT_DATE);

        assertEquals(List.of("Summary Test Prime: gravity changed from 1.0 to 1.25"), changes);
    }

    @Test
    void summarizeForPlanetReportsAddedAndRemovedPlanets() throws Exception {
        PlanetarySystem onePlanetSystem = readSystem();
        PlanetarySystem twoPlanetSystem = readTwoPlanetSystem();

        assertEquals(List.of("Summary Test Secundus: Added planet"),
              PlanetarySystemChangeSummary.summarizeForPlanet(onePlanetSystem, twoPlanetSystem,
                    twoPlanetSystem.getPlanet(2), EVENT_DATE));
        assertEquals(List.of("Summary Test Secundus: Removed planet"),
              PlanetarySystemChangeSummary.summarizeForPlanet(twoPlanetSystem, onePlanetSystem,
                    twoPlanetSystem.getPlanet(2), EVENT_DATE));
    }

    private static PlanetarySystem readSystem() throws Exception {
        return PlanetarySystemYamlIO.read(new ByteArrayInputStream(SYSTEM.getBytes(StandardCharsets.UTF_8)));
    }

    private static PlanetarySystem readTwoPlanetSystem() throws Exception {
        return PlanetarySystemYamlIO.read(new ByteArrayInputStream(TWO_PLANET_SYSTEM.getBytes(StandardCharsets.UTF_8)));
    }

    private static LandMass landMass(String name, String capital) {
        LandMass landMass = new LandMass();
        landMass.setSourcedName(SourceableValue.of(name));
        landMass.setSourcedCapital(SourceableValue.of(capital));
        return landMass;
    }

    private static Satellite satellite(String name, String size) {
        Satellite satellite = new Satellite();
        satellite.setSourcedName(SourceableValue.of(name));
        satellite.setSourcedSize(SourceableValue.of(size));
        return satellite;
    }

    private static PlanetarySystemEvent systemEvent(LocalDate date, Boolean nadirCharge, Boolean zenithCharge) {
        PlanetarySystemEvent event = new PlanetarySystemEvent();
        event.date = date;
        event.nadirCharge = nadirCharge == null ? null : SourceableValue.of(nadirCharge);
        event.zenithCharge = zenithCharge == null ? null : SourceableValue.of(zenithCharge);
        return event;
    }
}
