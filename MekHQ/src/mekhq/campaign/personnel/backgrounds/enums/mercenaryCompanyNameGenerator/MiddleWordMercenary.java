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
package mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator;

import mekhq.MekHQ;

import java.util.Random;
import java.util.ResourceBundle;

public enum MiddleWordMercenary {
    AEGIS("MiddleWordMercenary.AEGIS.text"),
    ANVIL("MiddleWordMercenary.ANVIL.text"),
    APEX("MiddleWordMercenary.APEX.text"),
    ARCANE("MiddleWordMercenary.ARCANE.text"),
    ASHEN("MiddleWordMercenary.ASHEN.text"),
    ATLAS("MiddleWordMercenary.ATLAS.text"),
    AVENGER("MiddleWordMercenary.AVENGER.text"),
    BASTION("MiddleWordMercenary.BASTION.text"),
    BLACKHAWK("MiddleWordMercenary.BLACKHAWK.text"),
    BLACKOUT("MiddleWordMercenary.BLACKOUT.text"),
    BLADE("MiddleWordMercenary.BLADE.text"),
    BLAZE("MiddleWordMercenary.BLAZE.text"),
    BLITZ("MiddleWordMercenary.BLITZ.text"),
    BLOODHOUND("MiddleWordMercenary.BLOODHOUND.text"),
    BRAWLER("MiddleWordMercenary.BRAWLER.text"),
    BRIMSTONE("MiddleWordMercenary.BRIMSTONE.text"),
    CINDER("MiddleWordMercenary.CINDER.text"),
    COBALT("MiddleWordMercenary.COBALT.text"),
    COBRA("MiddleWordMercenary.COBRA.text"),
    COLOSSUS("MiddleWordMercenary.COLOSSUS.text"),
    COMET("MiddleWordMercenary.COMET.text"),
    CRUSADER("MiddleWordMercenary.CRUSADER.text"),
    CYCLONE("MiddleWordMercenary.CYCLONE.text"),
    DARKHAWK("MiddleWordMercenary.DARKHAWK.text"),
    DIREWOLF("MiddleWordMercenary.DIREWOLF.text"),
    DRACONIS("MiddleWordMercenary.DRACONIS.text"),
    DRAKON("MiddleWordMercenary.DRAKON.text"),
    DREAD("MiddleWordMercenary.DREAD.text"),
    DREADNOUGHT("MiddleWordMercenary.DREADNOUGHT.text"),
    DRIFTER("MiddleWordMercenary.DRIFTER.text"),
    ECHELON("MiddleWordMercenary.ECHELON.text"),
    ECLIPSE("MiddleWordMercenary.ECLIPSE.text"),
    EMBER("MiddleWordMercenary.EMBER.text"),
    ENFORCER("MiddleWordMercenary.ENFORCER.text"),
    ENIGMA("MiddleWordMercenary.ENIGMA.text"),
    FALCON("MiddleWordMercenary.FALCON.text"),
    FALCONER("MiddleWordMercenary.FALCONER.text"),
    FERAL("MiddleWordMercenary.FERAL.text"),
    FORGE("MiddleWordMercenary.FORGE.text"),
    FROST("MiddleWordMercenary.FROST.text"),
    FROSTBITE("MiddleWordMercenary.FROSTBITE.text"),
    FURY("MiddleWordMercenary.FURY.text"),
    GHOST("MiddleWordMercenary.GHOST.text"),
    GLADIATOR("MiddleWordMercenary.GLADIATOR.text"),
    GLADIUS("MiddleWordMercenary.GLADIUS.text"),
    GRIM("MiddleWordMercenary.GRIM.text"),
    GUARDIAN("MiddleWordMercenary.GUARDIAN.text"),
    HAMMER("MiddleWordMercenary.HAMMER.text"),
    HARBINGER("MiddleWordMercenary.HARBINGER.text"),
    HARRIER("MiddleWordMercenary.HARRIER.text"),
    HAVOC("MiddleWordMercenary.HAVOC.text"),
    HELIX("MiddleWordMercenary.HELIX.text"),
    HELLFIRE("MiddleWordMercenary.HELLFIRE.text"),
    HELLHOUND("MiddleWordMercenary.HELLHOUND.text"),
    HUNTER("MiddleWordMercenary.HUNTER.text"),
    INFERNO("MiddleWordMercenary.INFERNO.text"),
    IRON("MiddleWordMercenary.IRON.text"),
    IRONCLAD("MiddleWordMercenary.IRONCLAD.text"),
    IRONFIST("MiddleWordMercenary.IRONFIST.text"),
    IRONHEART("MiddleWordMercenary.IRONHEART.text"),
    IRONHIDE("MiddleWordMercenary.IRONHIDE.text"),
    JAVELIN("MiddleWordMercenary.JAVELIN.text"),
    JUGGERNAUT("MiddleWordMercenary.JUGGERNAUT.text"),
    KNIGHT("MiddleWordMercenary.KNIGHT.text"),
    LYNX("MiddleWordMercenary.LYNX.text"),
    MAELSTROM("MiddleWordMercenary.MAELSTROM.text"),
    MARAUDING("MiddleWordMercenary.MARAUDING.text"),
    MERCENARY("MiddleWordMercenary.MERCENARY.text"),
    MIRAGE("MiddleWordMercenary.MIRAGE.text"),
    NIGHTFALL("MiddleWordMercenary.NIGHTFALL.text"),
    NIGHTSHADE("MiddleWordMercenary.NIGHTSHADE.text"),
    NOMAD("MiddleWordMercenary.NOMAD.text"),
    NOVA("MiddleWordMercenary.NOVA.text"),
    OBSIDIAN("MiddleWordMercenary.OBSIDIAN.text"),
    OMEN("MiddleWordMercenary.OMEN.text"),
    ONYX("MiddleWordMercenary.ONYX.text"),
    ORION("MiddleWordMercenary.ORION.text"),
    OUTLAW("MiddleWordMercenary.OUTLAW.text"),
    OUTRIDER("MiddleWordMercenary.OUTRIDER.text"),
    PANTHER("MiddleWordMercenary.PANTHER.text"),
    PHALANX("MiddleWordMercenary.PHALANX.text"),
    PHANTOM("MiddleWordMercenary.PHANTOM.text"),
    PHOENIX("MiddleWordMercenary.PHOENIX.text"),
    PREDATOR("MiddleWordMercenary.PREDATOR.text"),
    PYRE("MiddleWordMercenary.PYRE.text"),
    PYRO("MiddleWordMercenary.PYRO.text"),
    QUASAR("MiddleWordMercenary.QUASAR.text"),
    RAMPAGE("MiddleWordMercenary.RAMPAGE.text"),
    RAMPART("MiddleWordMercenary.RAMPART.text"),
    RANGER("MiddleWordMercenary.RANGER.text"),
    RAPTOR("MiddleWordMercenary.RAPTOR.text"),
    RAVAGER("MiddleWordMercenary.RAVAGER.text"),
    RAVEN("MiddleWordMercenary.RAVEN.text"),
    RAVENOUS("MiddleWordMercenary.RAVENOUS.text"),
    RAZOR("MiddleWordMercenary.RAZOR.text"),
    REAPER("MiddleWordMercenary.REAPER.text"),
    REAVER("MiddleWordMercenary.REAVER.text"),
    REBELLION("MiddleWordMercenary.REBELLION.text"),
    RECKONING("MiddleWordMercenary.RECKONING.text"),
    REIGN("MiddleWordMercenary.REIGN.text"),
    RENEGADE("MiddleWordMercenary.RENEGADE.text"),
    ROGUE("MiddleWordMercenary.ROGUE.text"),
    SABRE("MiddleWordMercenary.SABRE.text"),
    SCORPION("MiddleWordMercenary.SCORPION.text"),
    SCOURGE("MiddleWordMercenary.SCOURGE.text"),
    SENTINEL("MiddleWordMercenary.SENTINEL.text"),
    SHADOW("MiddleWordMercenary.SHADOW.text"),
    SHADOWHAWK("MiddleWordMercenary.SHADOWHAWK.text"),
    SHARD("MiddleWordMercenary.SHARD.text"),
    SHATTER("MiddleWordMercenary.SHATTER.text"),
    SHRAPNEL("MiddleWordMercenary.SHRAPNEL.text"),
    SKYHAWK("MiddleWordMercenary.SKYHAWK.text"),
    SPECTER("MiddleWordMercenary.SPECTER.text"),
    SPECTRAL("MiddleWordMercenary.SPECTRAL.text"),
    SPECTRE("MiddleWordMercenary.SPECTRE.text"),
    STARFIRE("MiddleWordMercenary.STARFIRE.text"),
    STEEL("MiddleWordMercenary.STEEL.text"),
    STORM("MiddleWordMercenary.STORM.text"),
    STORMBREAKER("MiddleWordMercenary.STORMBREAKER.text"),
    STORMBRINGER("MiddleWordMercenary.STORMBRINGER.text"),
    STORMWATCH("MiddleWordMercenary.STORMWATCH.text"),
    STRATOS("MiddleWordMercenary.STRATOS.text"),
    STRIDER("MiddleWordMercenary.STRIDER.text"),
    STRIKE("MiddleWordMercenary.STRIKE.text"),
    STRIKER("MiddleWordMercenary.STRIKER.text"),
    SURGE("MiddleWordMercenary.SURGE.text"),
    TALON("MiddleWordMercenary.TALON.text"),
    TEMPEST("MiddleWordMercenary.TEMPEST.text"),
    THUNDER("MiddleWordMercenary.THUNDER.text"),
    THUNDERBOLT("MiddleWordMercenary.THUNDERBOLT.text"),
    THUNDERSTORM("MiddleWordMercenary.THUNDERSTORM.text"),
    THUNDERSTRIKE("MiddleWordMercenary.THUNDERSTRIKE.text"),
    TITAN("MiddleWordMercenary.TITAN.text"),
    TITANFALL("MiddleWordMercenary.TITANFALL.text"),
    TITANIS("MiddleWordMercenary.TITANIS.text"),
    TRACER("MiddleWordMercenary.TRACER.text"),
    VALKYRIE("MiddleWordMercenary.VALKYRIE.text"),
    VALOR("MiddleWordMercenary.VALOR.text"),
    VANGUARD("MiddleWordMercenary.VANGUARD.text"),
    VENOM("MiddleWordMercenary.VENOM.text"),
    VINDICATOR("MiddleWordMercenary.VINDICATOR.text"),
    VIPER("MiddleWordMercenary.VIPER.text"),
    VORTEX("MiddleWordMercenary.VORTEX.text"),
    VULCAN("MiddleWordMercenary.VULCAN.text"),
    WARHOUND("MiddleWordMercenary.WARHOUND.text"),
    WARLOCK("MiddleWordMercenary.WARLOCK.text"),
    WARLORD("MiddleWordMercenary.WARLORD.text"),
    WARPATH("MiddleWordMercenary.WARPATH.text"),
    WOLF("MiddleWordMercenary.WOLF.text"),
    WOLFPACK("MiddleWordMercenary.WOLFPACK.text"),
    WRAITH("MiddleWordMercenary.WRAITH.text"),
    WRATH("MiddleWordMercenary.WRATH.text"),
    ZENITH("MiddleWordMercenary.ZENITH.text"),
    ZEPHYR("MiddleWordMercenary.ZEPHYR.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String word;
    //endregion Variable Declarations

    //region Constructors
    MiddleWordMercenary(final String word) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RandomMercenaryCompanyNameGenerator",
                MekHQ.getMHQOptions().getLocale());
        this.word = resources.getString(word);
    }

    private static final Random RANDOM = new Random();
    //endregion Constructors

    @Override
    public String toString() {
        return word;
    }

    /**
     * @return a random word from the MiddleWordMercenary enum.
     */
    public static String getRandomWord() {
        MiddleWordMercenary[] words = MiddleWordMercenary.values();
        MiddleWordMercenary randomWord = words[RANDOM.nextInt(words.length)];
        return randomWord.toString();
    }
}
