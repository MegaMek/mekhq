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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySystemEvent;
import mekhq.campaign.universe.Satellite;
import mekhq.campaign.universe.SourceableValue;

public final class PlanetarySystemChangeSummary {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private PlanetarySystemChangeSummary() {

    }

    public static List<String> summarize(PlanetarySystem baseline, PlanetarySystem edited, LocalDate displayDate) {
        if ((baseline == null) || (edited == null)) {
            return List.of();
        }

        LocalDate effectiveDisplayDate = displayDate == null ? LocalDate.now() : displayDate;
        List<String> changes = new ArrayList<>();
        Map<String, Planet> baselinePlanets = mapPlanets(baseline, effectiveDisplayDate);
        Map<String, Planet> editedPlanets = mapPlanets(edited, effectiveDisplayDate);
        TreeSet<String> planetKeys = new TreeSet<>(baselinePlanets.keySet());
        planetKeys.addAll(editedPlanets.keySet());

        for (String planetKey : planetKeys) {
            Planet baselinePlanet = baselinePlanets.get(planetKey);
            Planet editedPlanet = editedPlanets.get(planetKey);
            String planetName = planetDisplayName(Objects.requireNonNullElse(editedPlanet, baselinePlanet),
                  effectiveDisplayDate);
            if (baselinePlanet == null) {
                changes.add(MessageFormat.format("{0}: Added planet", planetName));
            } else if (editedPlanet == null) {
                changes.add(MessageFormat.format("{0}: Removed planet", planetName));
            } else {
                summarizePlanetStaticFields(planetName, baselinePlanet, editedPlanet, effectiveDisplayDate, changes);
                summarizePlanetEvents(planetName, baselinePlanet, editedPlanet, changes);
            }
        }

        summarizeSystemEvents(systemDisplayName(edited, effectiveDisplayDate), baseline, edited, changes);

        return changes;
    }

    public static boolean hasChangesForPlanet(PlanetarySystem baseline, PlanetarySystem edited, Planet planet,
          LocalDate displayDate) {
        if ((baseline == null) || (edited == null) || (planet == null)) {
            return false;
        }

        LocalDate effectiveDisplayDate = displayDate == null ? LocalDate.now() : displayDate;
        String planetKey = planetKey(planet, effectiveDisplayDate);
        Planet baselinePlanet = mapPlanets(baseline, effectiveDisplayDate).get(planetKey);
        Planet editedPlanet = mapPlanets(edited, effectiveDisplayDate).get(planetKey);
        if ((baselinePlanet == null) || (editedPlanet == null)) {
            return baselinePlanet != editedPlanet;
        }

        List<String> changes = new ArrayList<>();
        summarizePlanetStaticFields(planetDisplayName(editedPlanet, effectiveDisplayDate), baselinePlanet, editedPlanet,
              effectiveDisplayDate, changes);
        summarizePlanetEvents(planetDisplayName(editedPlanet, effectiveDisplayDate), baselinePlanet, editedPlanet,
              changes);
        return !changes.isEmpty();
    }

    public static List<String> summarizeForPlanet(PlanetarySystem baseline, PlanetarySystem edited, Planet planet,
          LocalDate displayDate) {
        if ((baseline == null) || (edited == null) || (planet == null)) {
            return List.of();
        }

        LocalDate effectiveDisplayDate = displayDate == null ? LocalDate.now() : displayDate;
        String planetKey = planetKey(planet, effectiveDisplayDate);
        Planet baselinePlanet = mapPlanets(baseline, effectiveDisplayDate).get(planetKey);
        Planet editedPlanet = mapPlanets(edited, effectiveDisplayDate).get(planetKey);
        String planetName = planetDisplayName(Objects.requireNonNullElse(editedPlanet, baselinePlanet),
              effectiveDisplayDate);

        if (baselinePlanet == null) {
            return List.of(MessageFormat.format("{0}: Added planet", planetName));
        }
        if (editedPlanet == null) {
            return List.of(MessageFormat.format("{0}: Removed planet", planetName));
        }

        List<String> changes = new ArrayList<>();
        summarizePlanetStaticFields(planetName, baselinePlanet, editedPlanet, effectiveDisplayDate, changes);
        summarizePlanetEvents(planetName, baselinePlanet, editedPlanet, changes);
        return changes;
    }

    private static Map<String, Planet> mapPlanets(PlanetarySystem system, LocalDate displayDate) {
        Collection<Planet> planets = system.getPlanets();
        if (planets == null) {
            return Map.of();
        }

        Map<String, Planet> mappedPlanets = new LinkedHashMap<>();
        planets.stream()
              .filter(Objects::nonNull)
              .sorted(Comparator.comparing(planet -> planetSortKey(planet, displayDate)))
              .forEach(planet -> mappedPlanets.put(planetKey(planet, displayDate), planet));
        return mappedPlanets;
    }

    private static void summarizePlanetStaticFields(String planetName, Planet baselinePlanet, Planet editedPlanet,
          LocalDate effectiveDisplayDate, List<String> changes) {
        addPlanetFieldChange(changes, planetName, "name", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedName(effectiveDisplayDate))));
        addPlanetFieldChange(changes, planetName, "type", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedPlanetType())));
        addPlanetFieldChange(changes, planetName, "gravity", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedGravity())));
        addPlanetFieldChange(changes, planetName, "diameter", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedDiameter())));
        addPlanetFieldChange(changes, planetName, "day length", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedDayLength(effectiveDisplayDate))));
        addPlanetFieldChange(changes, planetName, "year length", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedYearLength())));
        addPlanetFieldChange(changes, planetName, "temperature", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedTemperature(effectiveDisplayDate))));
        addPlanetFieldChange(changes, planetName, "pressure", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedPressure(effectiveDisplayDate))));
        addPlanetFieldChange(changes, planetName, "atmosphere", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedAtmosphere(effectiveDisplayDate))));
        addPlanetFieldChange(changes, planetName, "composition", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedComposition(effectiveDisplayDate))));
        addPlanetFieldChange(changes, planetName, "% water", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedPercentWater(effectiveDisplayDate))));
        addPlanetFieldChange(changes, planetName, "life form", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedLifeForm(effectiveDisplayDate))));
        addPlanetFieldChange(changes, planetName, "small moons", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedSmallMoons())));
        addPlanetFieldChange(changes, planetName, "ring", baselinePlanet, editedPlanet,
              planet -> formatObject(sourceableValue(planet.getSourcedRing())));
        addPlanetFieldChange(changes, planetName, "description", baselinePlanet, editedPlanet,
              planet -> formatObject(planet.getDescription()));
        summarizePlanetLandMasses(planetName, baselinePlanet, editedPlanet, changes);
        summarizePlanetSatellites(planetName, baselinePlanet, editedPlanet, changes);
    }

    private static void summarizePlanetLandMasses(String planetName, Planet baselinePlanet, Planet editedPlanet,
          List<String> changes) {
        List<LandMass> baselineList = nonNullList(baselinePlanet.getLandMasses());
        List<LandMass> editedList = nonNullList(editedPlanet.getLandMasses());
        int common = Math.min(baselineList.size(), editedList.size());
        for (int i = 0; i < common; i++) {
            LandMass baseline = baselineList.get(i);
            LandMass edited = editedList.get(i);
            String baselineName = formatObject(sourceableValue(baseline.getSourcedName()));
            String editedName = formatObject(sourceableValue(edited.getSourcedName()));
            if (!Objects.equals(baselineName, editedName)) {
                changes.add(MessageFormat.format("{0}: landmass [{1}] name changed from {2} to {3}",
                      planetName, i + 1, baselineName, editedName));
            }
            String baselineCapital = formatObject(sourceableValue(baseline.getSourcedCapital()));
            String editedCapital = formatObject(sourceableValue(edited.getSourcedCapital()));
            if (!Objects.equals(baselineCapital, editedCapital)) {
                changes.add(MessageFormat.format("{0}: landmass [{1}] capital changed from {2} to {3}",
                      planetName, i + 1, baselineCapital, editedCapital));
            }
        }
        for (int i = common; i < editedList.size(); i++) {
            String name = formatObject(sourceableValue(editedList.get(i).getSourcedName()));
            changes.add(MessageFormat.format("{0}: Added landmass {1}", planetName, name));
        }
        for (int i = common; i < baselineList.size(); i++) {
            String name = formatObject(sourceableValue(baselineList.get(i).getSourcedName()));
            changes.add(MessageFormat.format("{0}: Removed landmass {1}", planetName, name));
        }
    }

    private static void summarizePlanetSatellites(String planetName, Planet baselinePlanet, Planet editedPlanet,
          List<String> changes) {
        List<Satellite> baselineList = nonNullList(baselinePlanet.getSatellites());
        List<Satellite> editedList = nonNullList(editedPlanet.getSatellites());
        int common = Math.min(baselineList.size(), editedList.size());
        for (int i = 0; i < common; i++) {
            Satellite baseline = baselineList.get(i);
            Satellite edited = editedList.get(i);
            String baselineName = formatObject(sourceableValue(baseline.getSourcedName()));
            String editedName = formatObject(sourceableValue(edited.getSourcedName()));
            if (!Objects.equals(baselineName, editedName)) {
                changes.add(MessageFormat.format("{0}: satellite [{1}] name changed from {2} to {3}",
                      planetName, i + 1, baselineName, editedName));
            }
            String baselineSize = formatObject(sourceableValue(baseline.getSourcedSize()));
            String editedSize = formatObject(sourceableValue(edited.getSourcedSize()));
            if (!Objects.equals(baselineSize, editedSize)) {
                changes.add(MessageFormat.format("{0}: satellite [{1}] size changed from {2} to {3}",
                      planetName, i + 1, baselineSize, editedSize));
            }
        }
        for (int i = common; i < editedList.size(); i++) {
            String name = formatObject(sourceableValue(editedList.get(i).getSourcedName()));
            changes.add(MessageFormat.format("{0}: Added satellite {1}", planetName, name));
        }
        for (int i = common; i < baselineList.size(); i++) {
            String name = formatObject(sourceableValue(baselineList.get(i).getSourcedName()));
            changes.add(MessageFormat.format("{0}: Removed satellite {1}", planetName, name));
        }
    }

    private static <T> List<T> nonNullList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static void addPlanetFieldChange(List<String> changes, String planetName, String fieldName,
          Planet baselinePlanet, Planet editedPlanet, Function<Planet, String> valueFormatter) {
        String baselineValue = valueFormatter.apply(baselinePlanet);
        String editedValue = valueFormatter.apply(editedPlanet);
        if (!Objects.equals(baselineValue, editedValue)) {
            changes.add(MessageFormat.format("{0}: {1} changed from {2} to {3}", planetName, fieldName,
                  baselineValue, editedValue));
        }
    }

    private static void summarizePlanetEvents(String planetName, Planet baselinePlanet, Planet editedPlanet,
          List<String> changes) {
        Map<LocalDate, PlanetaryEvent> baselineEvents = mapEvents(baselinePlanet);
        Map<LocalDate, PlanetaryEvent> editedEvents = mapEvents(editedPlanet);
        TreeSet<LocalDate> eventDates = new TreeSet<>(baselineEvents.keySet());
        eventDates.addAll(editedEvents.keySet());

        for (LocalDate eventDate : eventDates) {
            PlanetaryEvent baselineEvent = baselineEvents.get(eventDate);
            PlanetaryEvent editedEvent = editedEvents.get(eventDate);
            if (baselineEvent == null) {
                changes.add(MessageFormat.format("{0}: Added event on {1}", planetName, formatDate(eventDate)));
            } else if (editedEvent == null) {
                changes.add(MessageFormat.format("{0}: Removed event on {1}", planetName, formatDate(eventDate)));
            } else {
                summarizeEventFields(planetName, eventDate, baselineEvent, editedEvent, changes);
            }
        }
    }

    private static Map<LocalDate, PlanetaryEvent> mapEvents(Planet planet) {
        List<PlanetaryEvent> events = planet.getEvents();
        if (events == null) {
            return Map.of();
        }

        Map<LocalDate, PlanetaryEvent> mappedEvents = new TreeMap<>();
        for (PlanetaryEvent event : events) {
            if ((event != null) && (event.date != null)) {
                mappedEvents.put(event.date, event);
            }
        }
        return mappedEvents;
    }

    private static void summarizeSystemEvents(String systemName, PlanetarySystem baseline, PlanetarySystem edited,
          List<String> changes) {
        Map<LocalDate, PlanetarySystemEvent> baselineEvents = mapSystemEvents(baseline);
        Map<LocalDate, PlanetarySystemEvent> editedEvents = mapSystemEvents(edited);
        TreeSet<LocalDate> eventDates = new TreeSet<>(baselineEvents.keySet());
        eventDates.addAll(editedEvents.keySet());

        for (LocalDate eventDate : eventDates) {
            PlanetarySystemEvent baselineEvent = baselineEvents.get(eventDate);
            PlanetarySystemEvent editedEvent = editedEvents.get(eventDate);
            if (baselineEvent == null) {
                changes.add(MessageFormat.format("{0}: Added system event on {1}", systemName,
                      formatDate(eventDate)));
            } else if (editedEvent == null) {
                changes.add(MessageFormat.format("{0}: Removed system event on {1}", systemName,
                      formatDate(eventDate)));
            } else {
                addSystemEventFieldChange(changes, systemName, eventDate, "nadir charge",
                      formatObject(sourceableValue(baselineEvent.nadirCharge)),
                      formatObject(sourceableValue(editedEvent.nadirCharge)));
                addSystemEventFieldChange(changes, systemName, eventDate, "zenith charge",
                      formatObject(sourceableValue(baselineEvent.zenithCharge)),
                      formatObject(sourceableValue(editedEvent.zenithCharge)));
            }
        }
    }

    private static Map<LocalDate, PlanetarySystemEvent> mapSystemEvents(PlanetarySystem system) {
        if (system == null) {
            return Map.of();
        }
        List<PlanetarySystemEvent> events = system.getEvents();
        if (events == null) {
            return Map.of();
        }
        Map<LocalDate, PlanetarySystemEvent> mapped = new TreeMap<>();
        for (PlanetarySystemEvent event : events) {
            if ((event != null) && (event.date != null)) {
                mapped.put(event.date, event);
            }
        }
        return mapped;
    }

    private static void addSystemEventFieldChange(List<String> changes, String systemName, LocalDate eventDate,
          String fieldName, String baselineValue, String editedValue) {
        if (!Objects.equals(baselineValue, editedValue)) {
            changes.add(MessageFormat.format("{0}: {1} system {2} changed from {3} to {4}", systemName,
                  formatDate(eventDate), fieldName, baselineValue, editedValue));
        }
    }

    private static void summarizeEventFields(String planetName, LocalDate eventDate, PlanetaryEvent baselineEvent,
          PlanetaryEvent editedEvent, List<String> changes) {
        addFieldChange(changes, planetName, eventDate, "factions", baselineEvent, editedEvent,
              event -> formatList(sourceableValue(event.faction)));
        addFieldChange(changes, planetName, eventDate, "population", baselineEvent, editedEvent,
              event -> formatPopulation(sourceableValue(event.population)));
        addFieldChange(changes, planetName, eventDate, "HPG", baselineEvent, editedEvent,
              event -> formatObject(sourceableValue(event.hpg)));
        addFieldChange(changes, planetName, eventDate, "socio-industrial code", baselineEvent, editedEvent,
              event -> formatObject(sourceableValue(event.socioIndustrial)));
        addFieldChange(changes, planetName, eventDate, "source", baselineEvent, editedEvent,
              PlanetarySystemChangeSummary::firstSource);
        addFieldChange(changes, planetName, eventDate, "version", baselineEvent, editedEvent,
              PlanetarySystemChangeSummary::firstVersion);
        addFieldChange(changes, planetName, eventDate, "message", baselineEvent, editedEvent,
              event -> formatObject(event.message));
        addFieldChange(changes, planetName, eventDate, "custom flag", baselineEvent, editedEvent,
              event -> String.valueOf(event.custom));
    }

    private static void addFieldChange(List<String> changes, String planetName, LocalDate eventDate, String fieldName,
          PlanetaryEvent baselineEvent, PlanetaryEvent editedEvent, Function<PlanetaryEvent, String> valueFormatter) {
        String baselineValue = valueFormatter.apply(baselineEvent);
        String editedValue = valueFormatter.apply(editedEvent);
        if (!Objects.equals(baselineValue, editedValue)) {
            changes.add(MessageFormat.format("{0}: {1} {2} changed from {3} to {4}", planetName,
                  formatDate(eventDate), fieldName, baselineValue, editedValue));
        }
    }

    private static Object sourceableValue(SourceableValue<?> sourceableValue) {
        return sourceableValue == null ? null : sourceableValue.getValue();
    }

    private static String firstSource(PlanetaryEvent event) {
        return firstMetadataValue(true, event.faction, event.population, event.hpg, event.socioIndustrial);
    }

    private static String firstVersion(PlanetaryEvent event) {
        return firstMetadataValue(false, event.faction, event.population, event.hpg, event.socioIndustrial);
    }

    private static String firstMetadataValue(boolean source, SourceableValue<?>... values) {
        for (SourceableValue<?> value : values) {
            if (value == null) {
                continue;
            }
            String metadataValue = source ? value.getSource() : value.getVersion();
            if (metadataValue != null) {
                return formatObject(metadataValue);
            }
        }
        return "none";
    }

    private static String formatList(Object value) {
        if (value instanceof Collection<?> values) {
            return values.isEmpty() ? "none" : String.join(", ", values.stream()
                                                                   .map(String::valueOf)
                                                                   .toList());
        }
        return formatObject(value);
    }

    private static String formatPopulation(Object value) {
        if (value instanceof Long population) {
            return String.format(Locale.ROOT, "%,d", population);
        }
        return formatObject(value);
    }

    private static String formatObject(Object value) {
        if (value == null) {
            return "none";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "none" : text;
    }

    private static String planetKey(Planet planet, LocalDate displayDate) {
        Integer systemPosition = planet.getSystemPosition();
        if (systemPosition != null) {
            return String.format(Locale.ROOT, "pos:%05d", systemPosition);
        }
        String planetId = planet.getId();
        if (planetId != null) {
            return "id:" + planetId;
        }
        return "name:" + planetDisplayName(planet, displayDate);
    }

    private static String planetSortKey(Planet planet, LocalDate displayDate) {
        return planetKey(planet, displayDate);
    }

    private static String planetDisplayName(Planet planet, LocalDate displayDate) {
        if (planet == null) {
            return "Unknown planet";
        }
        String displayName = planet.getPrintableName(displayDate);
        return ((displayName == null) || displayName.isBlank()) ? "Unnamed planet" : displayName;
    }

    private static String systemDisplayName(PlanetarySystem system, LocalDate displayDate) {
        if (system == null) {
            return "Unknown system";
        }
        String name = system.getName(displayDate);
        if ((name == null) || name.isBlank()) {
            String id = system.getId();
            return ((id == null) || id.isBlank()) ? "Unnamed system" : id;
        }
        return name;
    }

    private static String formatDate(LocalDate date) {
        return DATE_FORMATTER.format(date);
    }
}