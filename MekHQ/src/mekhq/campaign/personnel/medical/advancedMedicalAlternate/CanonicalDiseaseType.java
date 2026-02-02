/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.InjuryType;

public enum CanonicalDiseaseType {
    ALARION_HANTA_VIRUS("ALARION_HANTA_VIRUS",
          List.of("Alarion"),
          LocalDate.of(3069, Month.APRIL, 12),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.ALARION_HANTA_VIRUS),
    ALBIERO_CONSUMPTION("ALBIERO_CONSUMPTION",
          List.of("Albiero", "Luzerne", "Savinsville", "Schuyler", "Hanover", "Coudoux", "Brocchi's Cluster (40)",
                "Almunge", "Turtle Bay", "Rockland", "Schwartz", "Jeronimo", "Bangor", "Jeanette", "Matamoras",
                "Virentofta", "Stapelfeld", "Sawyer", "Lonaconing", "Echo", "Bjarred"),
          LocalDate.of(2903, 1, 1),
          LocalDate.of(2907, 1, 1),
          LocalDate.of(2904, 1, 1),
          AlternateInjuries.ALBIERO_CONSUMPTION),
    ALGEDI_BLOOD_BURN("ALGEDI_BLOOD_BURN",
          List.of("Algedi"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          // We don't have a canonical cure date, but this was the year the Azami joined the Combine. So we can
          // conclude that, by that point, knowledge of the disease was widespread enough to no longer be a threat
          LocalDate.of(2516, 1, 1),
          AlternateInjuries.ALGEDI_BLOOD_BURN),
    ANCHA_VIRUS("ANCHA_VIRUS",
          List.of("Ancha"),
          LocalDate.of(2319, 1, 1),
          LocalDate.of(2319, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.ANCHA_VIRUS),
    BETHOLD_SYNDROME_ONE("BETHOLD_SYNDROME_ONE",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(2865, 1, 1),
          LocalDate.of(2400, 1, 1),
          AlternateInjuries.BETHOLD_SYNDROME),
    BETHOLD_SYNDROME_TWO("BETHOLD_SYNDROME_TWO",
          List.of(),
          LocalDate.of(2866, 1, 1),
          LocalDate.of(9999, 1, 1),
          // This disease was a non-issue during the Star League era, but became incurable until the discovery of the
          // Helm Core.
          LocalDate.of(3028, 1, 1),
          AlternateInjuries.BETHOLD_SYNDROME),
    BLACK_MARSH_FEVER("BLACK_MARSH_FEVER",
          List.of("Gallitzin"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.BLACK_MARSH_FEVER),
    BRISBANE_VIRUS("BRISBANE_VIRUS",
          List.of("Brisbane"),
          // We know the disease was present prior to 3038, but was made famous in 3038
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          // We're choosing to believe the FedSuns claims of having a cure
          LocalDate.of(3064, 1, 1),
          AlternateInjuries.BRISBANE_VIRUS),
    CHELOSIAN_VIRUS("CHELOSIAN_VIRUS",
          List.of("Donenac"),
          LocalDate.of(2964, 12, 1),
          // We only know the outbreak occurred for 3 months in 2964, we're chosen to translate that as the Dec ->
          // Feb flu season
          LocalDate.of(2965, 2, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.CHELOSIAN_VIRUS),
    CHILDUS_FEVER("CHILDUS_FEVER",
          List.of("Solaris"),
          LocalDate.of(3060, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.CHILDUS_FEVER),
    CHUNGALOMENINGITIS_AMARIS("CHUNGALOMENINGITIS_AMARIS",
          List.of("Piedmont"),
          LocalDate.of(2766, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.CHUNGALOMENINGITIS_AMARIS),
    CHUNGALOMENINGITIS_TRADITIONAL("CHUNGALOMENINGITIS_TRADITIONAL",
          List.of("Piedmont"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2400, 1, 1),
          AlternateInjuries.CHUNGALOMENINGITIS_TRADITIONAL),
    CROMARTY_SUPERFLU("CROMARTY_SUPERFLU",
          List.of(),
          // We don't have a start date for the epidemic, so gave it a 10-year lifespan
          LocalDate.of(2689, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2699, 1, 1),
          AlternateInjuries.CROMARTY_SUPERFLU),
    CURSE_OF_EDEN("CURSE_OF_EDEN",
          List.of("Eden"),
          LocalDate.of(2790, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2821, 1, 1),
          AlternateInjuries.CURSE_OF_EDEN),
    CURSE_OF_GALEDON("CURSE_OF_GALEDON",
          List.of("Galedon V", "An Ting"),
          LocalDate.of(3069, Month.MAY, 16),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.CURSE_OF_GALEDON),
    CUSSET_CRUD("CUSSET_CRUD",
          List.of("Cusset"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.CUSSET_CRUD),
    DANGMARS_FEVER_DRUG_RESISTANT("DANGMARS_FEVER_DRUG_RESISTANT",
          List.of(),
          LocalDate.of(3072, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.DANGMARS_FEVER),
    DANGMARS_FEVER_NORMAL("DANGMARS_FEVER_NORMAL",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(3071, 1, 1),
          LocalDate.of(3050, 1, 1),
          AlternateInjuries.DANGMARS_FEVER),
    DARRS_DISEASE("DARRS_DISEASE",
          List.of("Sertar"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.DARRS_DISEASE),
    DELPHI_CURSE("DELPHI_CURSE",
          List.of("Anglia", "Belgae", "Dania", "Halkidik", "Helvetia", "Hibernia", "Karpathos", "Lemnos",
                "New Delphi", "Thasos"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.DELPHI_CURSE),
    DEVILITCH("DEVILITCH",
          List.of("Ashburton"),
          LocalDate.of(3064, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(3072, Month.FEBRUARY, 1),
          AlternateInjuries.DEVILITCH),
    DOWNING_POLTURS_DISEASE("DOWNING_POLTURS_DISEASE",
          List.of("Sabik"),
          LocalDate.of(3078, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.DOWNING_POLTURS_DISEASE),
    EDISON_WHITE_FLU("EDISON_WHITE_FLU",
          List.of("Evciler"),
          LocalDate.of(2756, 1, 1),
          LocalDate.of(2759, 1, 1),
          LocalDate.of(2759, 1, 1),
          AlternateInjuries.EDISON_WHITE_FLU),
    ELTANIN_BRAIN_FEVER("ELTANIN_BRAIN_FEVER",
          List.of("Eltanin"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2400, 1, 1),
          AlternateInjuries.ELTANIN_BRAIN_FEVER),
    FENRIS_PLAGUE("FENRIS_PLAGUE",
          List.of("Rasalhague"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.FENRIS_PLAGUE),
    GALAX_PATHOGEN("GALAX_PATHOGEN",
          List.of("Galax"),
          LocalDate.of(3069, Month.APRIL, 12),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.GALAX_PATHOGEN),
    GARMS_SYNDROME("GARMS_SYNDROME",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2400, 1, 1),
          AlternateInjuries.GARMS_SYNDROME),
    GENOAN_SPINAL_MENINGITIS("GENOAN_SPINAL_MENINGITIS",
          List.of("Genoa"),
          LocalDate.of(2400, 1, 1),
          // We don't have a specific cure year, but we're going to assume one is found by the time the Star League
          // is founded.
          LocalDate.of(2571, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.GENOAN_SPINAL_MENINGITIS),
    HYBORIAN_BLOOD_PLAGUE("HYBORIAN_BLOOD_PLAGUE",
          List.of("Towne"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2400, 1, 1),
          AlternateInjuries.HYBORIAN_BLOOD_PLAGUE),
    KAER_PATHOGEN("KAER_PATHOGEN",
          List.of("Hall"),
          LocalDate.of(2789, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.KAER_PATHOGEN),
    KILEN_WATTS_SYNDROME("KILEN_WATTS_SYNDROME",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.KILEN_WATTS_SYNDROME),
    KNIGHTS_GRASSE_SYNDROME("KNIGHTS_GRASSE_SYNDROME",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.KNIGHTS_GRASSE_SYNDROME),
    LAENS_REGRET("LAENS_REGRET",
          List.of("Tokasha"),
          LocalDate.of(2840, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2840, 1, 1),
          AlternateInjuries.LAENS_REGRET),
    LANDMARK_SUPERVIRUS("LANDMARK_SUPERVIRUS",
          List.of("Landmark"),
          LocalDate.of(2589, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2592, 1, 1),
          AlternateInjuries.LANDMARK_SUPERVIRUS),
    MIAPLACIDUS_PLAGUE("MIAPLACIDUS_PLAGUE",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.MIAPLACIDUS_PLAGUE),
    NEISSERIA_MALTHUSIA("NEISSERIA_MALTHUSIA",
          List.of("Dustball", "Arcturus"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2400, 1, 1),
          AlternateInjuries.NEISSERIA_MALTHUSIA),
    NEO_SMALLPOX("NEO_SMALLPOX",
          List.of("Timbuktu"),
          LocalDate.of(3023, 1, 1),
          LocalDate.of(3067, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.NEO_SMALLPOX),
    NOTILC_SWEATS("NOTILC_SWEATS",
          List.of("Shimosuwa"),
          LocalDate.of(3011, 1, 1),
          LocalDate.of(3012, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.NOTILC_SWEATS),
    NYKVARN_VIRUS("NYKVARN_VIRUS",
          List.of("Nykvarn"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(2400, 1, 1),
          AlternateInjuries.NYKVARN_VIRUS),
    OCKHAMS_BLOOD_DISEASE("OCKHAMS_BLOOD_DISEASE",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.OCKHAMS_BLOOD_DISEASE),
    PINGREE_FEVER("PINGREE_FEVER",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.PINGREE_FEVER),
    REDBURN_VIRUS("REDBURN_VIRUS",
          List.of("Alkaid", "Galatea", "Summer", "Mizar", "Skondia", "Lyons", "Ko", "Cebalrai", "Vega",
                "Eltanin", "Alya", "Kaus Borealis", "Kaus Australis", "Kaus Media", "Ascella", "Moore", "Sabik",
                "Lambrecht", "Dyev", "Kervil", "Pike IV", "Telos IV", "Imbros III", "Athenry", "Nashira", "Al Na'ir",
                "Yorii", "Asta", "Styx", "Deneb Algedi", "Cor Caroli", "Muphrid", "Thorin", "Altair", "Dieron",
                "Nirasaki", "Quentin", "Saffel", "Zaniah", "Gacrux", "Milton", "Denebola", "Lipton", "Chara",
                "New Earth", "Rigil Kentarus", "Fomalhaut", "Helen", "Towne", "Errai", "Shiloh", "Phecda", "Zavijava",
                "Terra", "Caph", "Northwind", "Small World", "Addicks", "Rochelle", "Kalidasa", "New Hope", "Stewart",
                "Wing", "Chertan", "Dubhe", "Callison", "Wyatt", "Zosma", "Marcus", "Castor", "Devil's Rock", "Pollux",
                "Graham IV", "Alula Australis", "Procyon", "Sirius", "Keid", "New Home", "Bryant", "Epsilon Indi",
                "Ingress", "Deneb Kaitos", "Ankaa", "Hean", "Tybalt", "Liberty", "Epsilon Eridani", "Sheratan",
                "Ruchbah", "Mirach", "Schedar", "Terra Firma", "Fletcher", "Tigress", "Rio", "Caselton", "Kawich",
                "Basalt", "Achernar", "Angol", "Tikonov", "Yangtze", "Hamal", "Bharat", "Woodstock", "Nopah", "Acamar",
                "Capolla", "Outreach", "Talitha", "Van Diemen IV", "Acubens", "Irian", "Wasat", "Hall", "Elgin",
                "Nanking", "Arboris", "Azha", "Slocum", "Berenson", "Tall Trees", "Saiph", "Zurich", "Genoa", "Kansu",
                "Ningpo", "Algol", "Buchlau", "Demeter", "Berenson", "Menkalinan", "New Canton", "Zion", "Asuncion",
                "Pleione", "Poznan", "Halloran V", "Algot", "New Aragon", "Menkar", "Wei"),
          LocalDate.of(3079, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.REDBURN_VIRUS),
    ROCKLAND_FEVER("ROCKLAND_FEVER",
          List.of(),
          LocalDate.of(3060, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.ROCKLAND_FEVER),
    SCOURGE_PLAGUE("SCOURGE_PLAGUE",
          List.of("Brinton"),
          LocalDate.of(3018, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.SCOURGE_PLAGUE),
    SKOKIE_SHIVERS("SKOKIE_SHIVERS",
          List.of("Skokie"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.SKOKIE_SHIVERS),
    TOXOPLASMA_GONDII_HARDCOREA("TOXOPLASMA_GONDII_HARDCOREA",
          List.of(),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.TOXOPLASMA_GONDII_HARDCOREA),
    UNOLE_FLU("UNOLE_FLU",
          List.of("Dieron"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.UNOLE_FLU),
    WINSONS_REGRET("WINSONS_REGRET",
          List.of("Albion", "Atreus", "Bearclaw", "Brim", "Delios", "Foster", "Glory", "Grant's Station",
                "Hector", "Hellgate", "Homer", "Ironhold", "Kirin", "Londerholm", "Lum", "Marshall", "Niles",
                "New Kent", "Paxon", "Roche", "Shadow", "Strato Domingo", "Tameron", "Tathis", "Tokasha", "Tranquil",
                "Tiber", "Strana Mechty", "Vinton", "York", "Colleen", "Tanis"),
          LocalDate.of(2400, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(9999, 1, 1),
          AlternateInjuries.WINSONS_REGRET),
    YIMPISEE_FEVER("YIMPISEE_FEVER",
          List.of("Butler", "Leskovik"),
          LocalDate.of(3072, 1, 1),
          LocalDate.of(9999, 1, 1),
          LocalDate.of(3072, Month.APRIL, 1),
          AlternateInjuries.YIMPISEE_FEVER);

    private static final List<CanonicalDiseaseType> allNormalDiseases = new ArrayList<>();
    private static final List<CanonicalDiseaseType> allBioweaponDiseases = new ArrayList<>();

    static {
        for (CanonicalDiseaseType disease : values()) {
            if (disease.getInjuryType().getSubType().isBioweaponDisease()) {
                allBioweaponDiseases.add(disease);
            } else {
                allNormalDiseases.add(disease);
            }
        }
    }

    private final String lookupName;
    private final List<String> affectedSystemCodes;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate cureStartDate;
    private final InjuryType injuryType;

    CanonicalDiseaseType(String lookupName, List<String> affectedSystemCodes, LocalDate startDate, LocalDate endDate,
          LocalDate cureStartDate, InjuryType injuryType) {
        this.lookupName = lookupName;
        this.affectedSystemCodes = affectedSystemCodes;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cureStartDate = cureStartDate;
        this.injuryType = injuryType;
    }

    public String getLookupName() {
        return lookupName;
    }

    public static Set<InjuryType> getAllActiveDiseases(String currentSystemCode, LocalDate today, boolean isStrict) {
        Set<InjuryType> activeDiseases = new HashSet<>();
        for (CanonicalDiseaseType diseaseType : allNormalDiseases) {
            if (diseaseType.isActiveInSystem(currentSystemCode, today, isStrict)) {
                activeDiseases.add(diseaseType.getInjuryType());
            }
        }

        return activeDiseases;
    }

    public static Set<InjuryType> getAllActiveBioweapons(String currentSystemCode, LocalDate today, boolean isStrict) {
        Set<InjuryType> activeDiseases = new HashSet<>();
        for (CanonicalDiseaseType diseaseType : allBioweaponDiseases) {
            if (diseaseType.isActiveInSystem(currentSystemCode, today, isStrict)) {
                activeDiseases.add(diseaseType.getInjuryType());
            }
        }

        return activeDiseases;
    }

    public static @Nullable InjuryType getNewBioweaponAttack(String currentSystemCode, LocalDate today,
          boolean isStrict) {
        for (CanonicalDiseaseType diseaseType : allBioweaponDiseases) {
            if (diseaseType.isActiveInSystem(currentSystemCode, today, isStrict)) {
                if (diseaseType.startDate.equals(today)) {
                    return diseaseType.getInjuryType();
                }
            }
        }

        return null;
    }

    public static @Nullable Set<InjuryType> getNewDiseaseOutbreaks(String currentSystemCode, LocalDate today,
          boolean isStrict) {
        Set<InjuryType> newOutbreaks = new HashSet<>();

        for (CanonicalDiseaseType diseaseType : allNormalDiseases) {
            if (diseaseType.isActiveInSystem(currentSystemCode, today, isStrict)) {
                if (diseaseType.startDate.equals(today)) {
                    newOutbreaks.add(diseaseType.getInjuryType());
                }
            }
        }

        return newOutbreaks;
    }

    private boolean isActiveInSystem(String systemCode, LocalDate today, boolean isStrict) {
        // An empty affectedSystemCodes means that it's not isolated to specific systems
        if ((affectedSystemCodes.isEmpty() && !isStrict) || affectedSystemCodes.contains(systemCode)) {
            return !today.isBefore(startDate) && !today.isAfter(endDate);
        }

        return false;
    }

    public static Set<InjuryType> getAllSystemSpecificDiseasesWithCures(String currentSystemCode, LocalDate today,
          boolean isStrict) {
        Set<InjuryType> availableCures = new HashSet<>();

        for (CanonicalDiseaseType diseaseType : CanonicalDiseaseType.values()) {
            if (diseaseType.isActiveInSystem(currentSystemCode, today, isStrict)) {
                if (diseaseType.isCureAvailable(today)) {
                    availableCures.add(diseaseType.getInjuryType());
                }
            }
        }

        return availableCures;
    }

    public static Set<InjuryType> getAllNewCures(String currentSystemCode, LocalDate today) {
        Set<InjuryType> availableCures = new HashSet<>();

        for (CanonicalDiseaseType diseaseType : CanonicalDiseaseType.values()) {
            if (diseaseType.isActiveInSystem(currentSystemCode, today, false)) {
                if (diseaseType.cureStartDate.equals(today)) {
                    availableCures.add(diseaseType.getInjuryType());
                }
            }
        }

        return availableCures;
    }

    public boolean isCureAvailable(LocalDate today) {
        return !today.isBefore(cureStartDate);
    }

    public InjuryType getInjuryType() {
        return injuryType;
    }
}
