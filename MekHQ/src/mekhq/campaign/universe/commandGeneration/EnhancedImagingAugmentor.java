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
package mekhq.campaign.universe.commandGeneration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.client.ratgenerator.ClanEnhancedImagingAugmentor;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AugmentedUnitType;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Fits the Clans' enhanced imaging implant to a freshly generated command's warriors.
 *
 * <p>The rules are MegaMek's - which Clans take the implant more readily, how many warriors take it,
 * and which units it works with all come from {@link ClanEnhancedImagingAugmentor}, so a campaign and a
 * skirmish do not disagree about how common EI warriors are. What is MekHQ's own is where the implant
 * is written: a campaign's warriors are {@link Person}s, and the crew carried on the entity is rebuilt
 * from them whenever the unit is reset. Writing to the entity's crew, as MegaMek does, would therefore
 * be undone the moment the unit was touched.</p>
 *
 * <p>EI warriors serve together rather than appearing one to a star, so the roll is made once per
 * formation and the whole formation takes it or none of it does.</p>
 */
public final class EnhancedImagingAugmentor {

    private static final MMLogger LOGGER = MMLogger.create(EnhancedImagingAugmentor.class);

    private EnhancedImagingAugmentor() {
    }

    /**
     * Fits the implant to the warriors of whichever generated formations take it.
     *
     * @param campaign          the campaign the command was generated into
     * @param generationFaction the faction the command was generated for, which decides both whether
     *                          the implant applies at all and how readily it is taken
     * @param generatedPersons  every person this generation created
     */
    public static void augment(Campaign campaign, @Nullable String generationFaction,
          List<Person> generatedPersons) {
        boolean isClan = isClanFaction(campaign, generationFaction);
        boolean campaignAllowsImplants = campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS);
        boolean rulesAllowNeuralInterfaces = ClanEnhancedImagingAugmentor
                                                   .neuralInterfaceRulesAllowImplants(
                                                         campaign.getGameOptions());
        // One line answering "did this even get a chance to run", before any gate, so a playtest never
        // has to infer it from the absence of later lines.
        LOGGER.info("[EnhancedImaging] ENTER: generationFaction='{}', isClan={}, campaign Use"
                    + " Implants={}, MegaMek neural interface rules on={}, generatedPersons={}",
              generationFaction, isClan, campaignAllowsImplants, rulesAllowNeuralInterfaces,
              generatedPersons.size());

        if (!isClan) {
            LOGGER.info("[EnhancedImaging] SKIPPED - the command was generated for '{}', which is not"
                        + " a Clan. Enhanced imaging is the Clans' alone.", generationFaction);
            return;
        }
        if (!campaignAllowsImplants) {
            LOGGER.info("[EnhancedImaging] SKIPPED - the campaign has implants switched off. Turn on"
                        + " Campaign Options > Personnel > General > Use Implants and generate again;"
                        + " this cannot be applied retrospectively.");
            return;
        }
        if (!rulesAllowNeuralInterfaces) {
            LOGGER.info("[EnhancedImaging] SKIPPED - MegaMek's neural interface rules are off, so an"
                        + " implant fitted here would do nothing. Set Game Options > Advanced Rules >"
                        + " Neural Interface to 'Pilot Abilities Only' or 'Full Tracking' and generate"
                        + " again; this cannot be applied retrospectively.");
            return;
        }

        double chance = ClanEnhancedImagingAugmentor.formationChanceFor(generationFaction);
        Map<Integer, List<Person>> formations = byFormation(generatedPersons);
        int implanted = implantWholeFormations(formations, chance);
        LOGGER.info("[EnhancedImaging] DONE: {} warrior(s) implanted across {} formation(s), at a {}%"
                    + " chance per formation", implanted, formations.size(),
              Math.round(chance * 100));
    }

    /**
     * @return {@code true} if the command was generated for a Clan, sub-factions of a Clan being Clans
     */
    private static boolean isClanFaction(Campaign campaign, @Nullable String generationFaction) {
        if (generationFaction == null) {
            return campaign.getPlayerForce().getFaction().isClan();
        }
        // The faction record is the authority MegaMek's own generator reads. Where the generation
        // faction is unknown to it, the campaign's own faction settles it.
        FactionRecord factionRecord = RATGenerator.getInstance().getFaction(generationFaction);
        return (factionRecord != null) ? factionRecord.isClan() : campaign.getPlayerForce().getFaction().isClan();
    }

    /**
     * Groups the generated warriors by the formation they were assigned to, which is what makes the
     * roll a formation's rather than a warrior's.
     *
     * <p>Warriors with no unit or no formation - support staff, and anyone the tree did not place - are
     * left out rather than lumped together, a pool of unrelated people being no formation at all.</p>
     */
    private static Map<Integer, List<Person>> byFormation(List<Person> generatedPersons) {
        Map<Integer, List<Person>> formations = new LinkedHashMap<>();
        for (Person person : generatedPersons) {
            Unit unit = person.getUnit();
            if (unit == null) {
                continue;
            }
            int formationId = unit.getFormationId();
            if (formationId < 0) {
                continue;
            }
            formations.computeIfAbsent(formationId, key -> new ArrayList<>()).add(person);
        }
        return formations;
    }

    /**
     * Rolls for each formation and implants every warrior of those that take it.
     *
     * @return how many warriors were implanted
     */
    private static int implantWholeFormations(Map<Integer, List<Person>> formations, double chance) {
        int implanted = 0;
        for (Map.Entry<Integer, List<Person>> formation : formations.entrySet()) {
            if (Math.random() >= chance) {
                continue;
            }
            int implantedHere = 0;
            for (Person warrior : formation.getValue()) {
                if (implant(warrior)) {
                    implantedHere++;
                }
            }
            if (implantedHere > 0) {
                LOGGER.debug("[EnhancedImaging] formation {} is an EI unit: {} of {} warrior(s)"
                            + " implanted", formation.getKey(), implantedHere,
                      formation.getValue().size());
            }
            implanted += implantedHere;
        }
        return implanted;
    }

    /**
     * @return {@code true} if this warrior took the implant, which needs a unit the implant works with
     */
    private static boolean implant(Person warrior) {
        AugmentedUnitType unitType = ManeiDominiAugmentor.unitTypeFor(warrior);
        if (!ClanEnhancedImagingAugmentor.canUseEnhancedImaging(unitType)) {
            return false;
        }
        warrior.getOptions().getOption(OptionsConstants.MD_EI_IMPLANT).setValue(true);
        return true;
    }
}
