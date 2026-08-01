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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

/**
 * Covers fitting the Clan enhanced imaging implant to a generated command.
 *
 * <p>The implant is written to the campaign's {@link Person}, not to the entity's crew: a unit's crew
 * is rebuilt from its people whenever the unit is reset, so an implant written to the crew would be
 * lost. Every claim here is therefore made about the person.</p>
 */
class EnhancedImagingAugmentorTest {

    private static final int SAMPLES = 400;

    private static Person warriorInFormation(int formationId, PersonnelRole role,
          List<Person> collected) {
        Mek entity = mock(Mek.class);
        when(entity.isIndustrial()).thenReturn(false);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getFormationId()).thenReturn(formationId);

        Person person = mock(Person.class);
        when(person.getOptions()).thenReturn(new PersonnelOptions());
        when(person.getUnit()).thenReturn(unit);
        // Which machine a warrior crews is read off their role, as it is for the Manei Domini.
        when(person.getPrimaryRole()).thenReturn(role);
        collected.add(person);
        return person;
    }

    /** A star of five Mek warriors under one formation. */
    private static List<Person> star(int formationId) {
        List<Person> warriors = new ArrayList<>();
        for (int seat = 0; seat < 5; seat++) {
            warriorInFormation(formationId, PersonnelRole.MEKWARRIOR, warriors);
        }
        return warriors;
    }

    private static Campaign campaign(boolean isClan, boolean useImplants,
          NeuralInterfaceMode mode) {
        GameOptions gameOptions = new GameOptions();
        gameOptions.getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE)
              .setValue(mode.optionValue());

        Faction faction = mock(Faction.class);
        when(faction.isClan()).thenReturn(isClan);

        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.USE_IMPLANTS)).thenReturn(useImplants);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getGameOptions()).thenReturn(gameOptions);
        when(campaign.getFaction()).thenReturn(faction);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        return campaign;
    }

    private static long implantedCount(List<Person> warriors) {
        return warriors.stream()
                     .filter(person -> person.getOptions()
                                             .booleanOption(OptionsConstants.MD_EI_IMPLANT))
                     .count();
    }

    /**
     * The regression this class exists for. MekHQ generated no EI warriors at all, because nothing in
     * its pipeline ever fitted the implant.
     */
    @Test
    void someClanWarriorsAreImplanted() {
        int implantedRuns = 0;
        for (int sample = 0; sample < SAMPLES; sample++) {
            List<Person> warriors = star(1);
            EnhancedImagingAugmentor.augment(
                  campaign(true, true, NeuralInterfaceMode.PILOT_ABILITIES_ONLY), null, warriors);
            if (implantedCount(warriors) > 0) {
                implantedRuns++;
            }
        }
        assertTrue(implantedRuns > 0,
              "a Clan command must produce EI warriors over " + SAMPLES + " generations");
    }

    /** A formation is an EI unit or it is not; EI warriors do not appear one to a star. */
    @Test
    void aFormationIsImplantedWholesaleOrNotAtAll() {
        for (int sample = 0; sample < SAMPLES; sample++) {
            List<Person> warriors = star(1);
            EnhancedImagingAugmentor.augment(
                  campaign(true, true, NeuralInterfaceMode.PILOT_ABILITIES_ONLY), null, warriors);

            long implanted = implantedCount(warriors);
            assertTrue((implanted == 0) || (implanted == warriors.size()),
                  "a star must be all EI or none, got " + implanted + " of " + warriors.size());
        }
    }

    @Test
    void aNonClanCommandIsNeverImplanted() {
        List<Person> warriors = star(1);
        EnhancedImagingAugmentor.augment(
              campaign(false, true, NeuralInterfaceMode.PILOT_ABILITIES_ONLY), null, warriors);

        assertEquals(0, implantedCount(warriors), "enhanced imaging is the Clans' alone");
    }

    /**
     * The campaign in hand when this was reported had the neural interface option off, which is the
     * shipped default. An implant fitted then would do nothing, so none is fitted.
     */
    @Test
    void nothingIsImplantedWhenTheNeuralInterfaceRulesAreOff() {
        List<Person> warriors = star(1);
        EnhancedImagingAugmentor.augment(
              campaign(true, true, NeuralInterfaceMode.OFF), null, warriors);

        assertEquals(0, implantedCount(warriors));
    }

    @Test
    void nothingIsImplantedWhenTheCampaignHasImplantsOff() {
        List<Person> warriors = star(1);
        EnhancedImagingAugmentor.augment(
              campaign(true, false, NeuralInterfaceMode.PILOT_ABILITIES_ONLY), null, warriors);

        assertEquals(0, implantedCount(warriors));
    }

    /** Someone with no unit is in no formation, and a pool of unplaced people is not one either. */
    @Test
    void warriorsWithNoUnitAreLeftAlone() {
        Person unplaced = mock(Person.class);
        when(unplaced.getOptions()).thenReturn(new PersonnelOptions());
        when(unplaced.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(unplaced.getUnit()).thenReturn(null);
        List<Person> warriors = new ArrayList<>(List.of(unplaced));

        EnhancedImagingAugmentor.augment(
              campaign(true, true, NeuralInterfaceMode.PILOT_ABILITIES_ONLY), null, warriors);

        assertEquals(0, implantedCount(warriors));
    }

    /** EI works with walking motive systems; a fighter pilot cannot use it whatever the Clan. */
    @Test
    void aWarriorInAnIneligibleUnitIsNotImplanted() {
        Entity fighter = mock(Entity.class);
        when(fighter.isAerospaceFighter()).thenReturn(true);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(fighter);
        when(unit.getFormationId()).thenReturn(1);

        Person pilot = mock(Person.class);
        when(pilot.getOptions()).thenReturn(new PersonnelOptions());
        when(pilot.getPrimaryRole()).thenReturn(PersonnelRole.AEROSPACE_PILOT);
        when(pilot.getUnit()).thenReturn(unit);
        List<Person> warriors = new ArrayList<>(List.of(pilot));

        for (int sample = 0; sample < SAMPLES; sample++) {
            EnhancedImagingAugmentor.augment(
                  campaign(true, true, NeuralInterfaceMode.PILOT_ABILITIES_ONLY), null, warriors);
        }
        assertEquals(0, implantedCount(warriors),
              "enhanced imaging works only with a walking motive system");
    }
}
