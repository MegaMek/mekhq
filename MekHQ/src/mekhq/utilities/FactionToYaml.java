/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMekRoot.megamek.main.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */

package mekhq.utilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.universe.FactionTag;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class FactionToYaml {

    /*

    TODO

    + move together MHQ and RAT Faction information
    + separate to factions, commands, technical factions?
    + unify rat fallback ("parent" and "alternate")
    X single fallback for rating system? auto-fallback to IS/CLAN? another time
    X better era mods yaml, like: AOW: 1; later
    + create new MM Faction class, read in yaml
    + use in RATGen
    + use in MHQ
    + use in MML
    + altNames (without year) is not used anywhere; retire
    + save factions from RATGenEd: problem factionrecords are not factions, how to save back to faction?
    + add camos folder -> problem: varying with year
    X add specific camos for commands - later
    + standard naming for changes with year
    + change startingplanet to capital
    X exact dates? seems unnecessary
    + add lance/star sizes formation sizes
    - add aero lance sizes (cant find)
    - tests
    - testing MHQ


RAT factions not found in MHQ factions: PP BH SE TamPact SL3 IS Periphery VesMar MalCon Blessed Order BAN
and all commands

MHQ factions not found in RAT factions: [DoL, SCW, SCon, GDL, NONE, IE, CCon, CCom, AC, OMA, AE, AG, MalC,
VSM, ARC, ARD, MRep, FWLR, ARL, JF, RP, BC, TGU, RU, CRep, SIMA, NDC, ABN, TiC, RTR, FCo, KE, SP, THW, CH,
ChP, THa, NTamP, REB, DIS, Alf, TU, LR, FoO, FoS, DT, FFR, MA, ME, NOC, RON, ACPS, SIS, IND, Mara, RPG, WA,
UND, IoS, SSUP, PD, TamP, SKP, AXP, CTL]

    - clean up Tamar Pact: TamP, TamPact, NTamP









     */















    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
          .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
          .disable(YAMLGenerator.Feature.SPLIT_LINES)
          .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
          .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)

    );

    public static class YamlFaction {
//        public static final String DEFAULT_CODE = "???";


        private String key;
        private String fullName;
        private NavigableMap<Integer, String> nameChanges = new TreeMap<>();
        private String startingPlanet;
        private NavigableMap<Integer, String> planetChanges = new TreeMap<>();
        private ArrayList<FactionRecord.DateRange> yearsActive;
        private String successor;
        private Set<FactionTag> tags;
        private Color color;
        private String logo;
        private String background;
        private String nameGenerator;
        private int[] eraMods;
        //        private String currencyCode = ""; // unused
//        private String layeredForceIconBackgroundCategory;
//        private String layeredForceIconBackgroundFilename;
//        private String layeredForceIconLogoCategory;
//        private String layeredForceIconLogoFilename;
        //private int start; // Start year (inclusive)   may have several year ranges
        //private int end; // End year (inclusive)

                private List<String> ratingLevels;

        // Seem to serve the same purpose: RAT Gen / RAT Manager fallback: but are sometimes different
        private Set<String> fallBackFactions = new HashSet<>();

        public List<String> getRatingLevels() {
            return ratingLevels;
        }

        public String getKey() {
            return key;
        }

        public String getFullName() {
            return fullName;
        }

        public Set<FactionTag> getTags() {
            return tags;
        }

        public List<FactionRecord.DateRange> getYearsActive() {
            return yearsActive;
        }

        public String getBackground() {
            return background;
        }

        public String getLogo() {
            return logo;
        }

        public int[] getEraMods() {
            return eraMods;
        }

        public String getSuccessor() {

            return successor;
        }

        public String getStartingPlanet() {
            return startingPlanet;
        }

        public Color getColor() {
            return color;
        }

        public String getNameGenerator() {
            return nameGenerator;
        }

        public NavigableMap<Integer, String> getNameChanges() {
            return nameChanges;
        }

        public NavigableMap<Integer, String> getPlanetChanges() {

            return planetChanges;
        }

        public Set<String> getFallBackFactions() {
            return fallBackFactions;
        }
    }

    private record FactionContainer(YamlFaction yamlFaction) {
    }

    public static class ColorSerializer extends StdSerializer<Color> {

        public ColorSerializer() {
            this(null);
        }

        public ColorSerializer(Class<Color> t) {
            super(t);
        }

        @Override
        public void serialize(Color value, JsonGenerator jgen, SerializerProvider provider)
              throws IOException, JsonProcessingException {

            jgen.writeStartObject();
            jgen.writeNumberField("red", value.getRed());
            jgen.writeNumberField("green", value.getGreen());
            jgen.writeNumberField("blue", value.getBlue());
            jgen.writeEndObject();
        }
    }




    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        while (!RATGenerator.getInstance().isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        RATGenerator ratGen = RATGenerator.getInstance();
        HashSet<String> factionKeySet = new HashSet<>(ratGen.getFactionKeySet());
//        factionKeySet.removeIf(key -> key.contains("."));

        Factions.setInstance(Factions.loadDefault());

        SimpleModule module = new SimpleModule();
        module.addSerializer(Color.class, new ColorSerializer());
        YAML_MAPPER.registerModule(module);
        YAML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        factionKeySet.addAll(Factions.getInstance().getFactionList());
        for (String factionKey : factionKeySet) {
//
//
//            final FactionRecord fRec = RATGenerator.getInstance().getFaction(factionKey);
//            Faction faction = Factions.getInstance().getFaction(factionKey);
//            if (faction.getShortName().equals("???")) {
//                System.out.println("Faction not found in MHQ: "+factionKey);
//                faction = null;
//            }
//
//            YamlFaction yamlFaction = new YamlFaction();
//            yamlFaction.key = factionKey;
//            if (faction != null) {
//                yamlFaction.fullName = faction.getFullName();
//            } else {
//                yamlFaction.fullName = fRec.getName();
//            }
//
//            // TAGS
//            yamlFaction.tags = new HashSet<>();
//            if (faction != null) {
//                yamlFaction.tags.addAll(faction.getTags());
//            } else {
//                if (fRec.isMinor()) {
//                    yamlFaction.tags.add(FactionTag.MINOR);
//                }
//                if (fRec.isPeriphery()) {
//                    yamlFaction.tags.add(FactionTag.PERIPHERY);
//                }
//                if (fRec.isClan()) {
//                    yamlFaction.tags.add(FactionTag.CLAN);
//                }
//            }
//
//            // PARENT = FALLBACK
//            if (fRec != null) {
//
//                yamlFaction.fallBackFactions.addAll(fRec.getParentFactions());
//                yamlFaction.yearsActive = new ArrayList<>(fRec.getYears());
//                if (yamlFaction.yearsActive.size() == 1 && yamlFaction.yearsActive.get(0).end == null
//                      && yamlFaction.yearsActive.get(0).start == null) {
//                    yamlFaction.yearsActive = null;
//                }
//                yamlFaction.ratingLevels = fRec.getRatingLevels();
//            }
//
//            if (faction != null) {
//
//                yamlFaction.color = faction.getColor();
//                yamlFaction.nameGenerator = faction.getNameGenerator();
//                yamlFaction.nameChanges = faction.getNameChanges();
//                yamlFaction.startingPlanet=faction.getStartingPlanet();
//                for (Map.Entry<LocalDate, String> entry : faction.getPlanetChanges().entrySet()) {
//                    yamlFaction.planetChanges.put(entry.getKey().getYear(), entry.getValue());
//                }
//
//                yamlFaction.successor = faction.getSuccessor();
//                yamlFaction.eraMods = faction.getEraMods();
//                boolean noEraMods = true;
//                if (yamlFaction.eraMods != null) {
//                    for (int mod : yamlFaction.eraMods) {
//                        if (mod != 0) {
//                            noEraMods = false;
//                            break;
//                        }
//                    }
//                }
//
//                if (noEraMods) {
//                    yamlFaction.eraMods = null;
//                }
//                if (faction.getAlternativeFactionCodes()!=null) {
//                    yamlFaction.fallBackFactions.addAll(Arrays.asList(faction.getAlternativeFactionCodes()));
//                }
//                yamlFaction.logo = faction.getLayeredForceIconLogoCategory() + faction.getLayeredForceIconLogoFilename();
//                yamlFaction.background = faction.getLayeredForceIconBackgroundCategory() + faction.getLayeredForceIconBackgroundFilename();
//                if (yamlFaction.logo.equals("null")) {
//                    yamlFaction.logo = null;
//                }
//                if (yamlFaction.background.equals("null")) {
//                    yamlFaction.background = null;
//                }
//                if (yamlFaction.nameGenerator.equals("General")) {
//                    yamlFaction.nameGenerator = null;
//                }
//            }
//
//
//            try {
//                String dir = factionKey.contains(".") ? "commands" : "factions";
//                YAML_MAPPER.writeValue(new File("data/universe/"+dir+"/" + factionKey + ".yml"),
//                      yamlFaction);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        var mhqFactions = Factions.getInstance().getFactionList();
        mhqFactions.removeAll(factionKeySet);
        System.out.println("MHQ factions not found in RAT factions: " + mhqFactions);
    }

}

