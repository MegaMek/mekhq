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

public enum EndWordMercenary {
    ADVERSARIES("EndWordMercenary.ADVERSARIES.text"),
    AEGIS("EndWordMercenary.AEGIS.text"),
    ASSASSINS("EndWordMercenary.ASSASSINS.text"),
    ASSAULT("EndWordMercenary.ASSAULT.text"),
    AVATARS("EndWordMercenary.AVATARS.text"),
    AVENGERS("EndWordMercenary.AVENGERS.text"),
    BANSHEES("EndWordMercenary.BANSHEES.text"),
    BATTALIONS("EndWordMercenary.BATTALIONS.text"),
    BATTLEMAGES("EndWordMercenary.BATTLEMAGES.text"),
    BERSERKERS("EndWordMercenary.BERSERKERS.text"),
    BLACKGUARDS("EndWordMercenary.BLACKGUARDS.text"),
    BLACKHAWKS("EndWordMercenary.BLACKHAWKS.text"),
    BLACKSMITHS("EndWordMercenary.BLACKSMITHS.text"),
    BLADES("EndWordMercenary.BLADES.text"),
    BLADESMEN("EndWordMercenary.BLADESMEN.text"),
    BLITZERS("EndWordMercenary.BLITZERS.text"),
    BOUNTY_HUNTERS("EndWordMercenary.BOUNTY_HUNTERS.text"),
    BRIGADE("EndWordMercenary.BRIGADE.text"),
    BRIGANDS("EndWordMercenary.BRIGANDS.text"),
    BULWARKS("EndWordMercenary.BULWARKS.text"),
    CATAPHRACTS("EndWordMercenary.CATAPHRACTS.text"),
    CAVALIERS("EndWordMercenary.CAVALIERS.text"),
    CAVALRY("EndWordMercenary.CAVALRY.text"),
    CENTURIONS("EndWordMercenary.CENTURIONS.text"),
    COMMANDERS("EndWordMercenary.COMMANDERS.text"),
    COMMANDOS("EndWordMercenary.COMMANDOS.text"),
    CONQUERORS("EndWordMercenary.CONQUERORS.text"),
    CORSAIRS("EndWordMercenary.CORSAIRS.text"),
    CRUSADERS("EndWordMercenary.CRUSADERS.text"),
    DEFENDERS("EndWordMercenary.DEFENDERS.text"),
    DEFIANCE("EndWordMercenary.DEFIANCE.text"),
    DOGS("EndWordMercenary.DOGS.text"),
    DOMINATORS("EndWordMercenary.DOMINATORS.text"),
    DOMINION("EndWordMercenary.DOMINION.text"),
    DRAGONS("EndWordMercenary.DRAGONS.text"),
    DRAGONSLAYERS("EndWordMercenary.DRAGONSLAYERS.text"),
    DRAGOONS("EndWordMercenary.DRAGOONS.text"),
    DREADLORDS("EndWordMercenary.DREADLORDS.text"),
    DREADNAUGHTS("EndWordMercenary.DREADNAUGHTS.text"),
    DRUIDS("EndWordMercenary.DRUIDS.text"),
    ELITES("EndWordMercenary.ELITES.text"),
    ENFORCERS("EndWordMercenary.ENFORCERS.text"),
    ENIGMA("EndWordMercenary.ENIGMA.text"),
    EXILES("EndWordMercenary.EXILES.text"),
    FALCONS("EndWordMercenary.FALCONS.text"),
    FIREBRANDS("EndWordMercenary.FIREBRANDS.text"),
    FRONTIERS("EndWordMercenary.FRONTIERS.text"),
    FRONTLINER("EndWordMercenary.FRONTLINER.text"),
    FROSTBORN("EndWordMercenary.FROSTBORN.text"),
    FURIES("EndWordMercenary.FURIES.text"),
    FURY("EndWordMercenary.FURY.text"),
    GHOSTS("EndWordMercenary.GHOSTS.text"),
    GOLEMS("EndWordMercenary.GOLEMS.text"),
    GOLIATHS("EndWordMercenary.GOLIATHS.text"),
    GRIMHEARTS("EndWordMercenary.GRIMHEARTS.text"),
    GRYPHONS("EndWordMercenary.GRYPHONS.text"),
    GUARDIANS("EndWordMercenary.GUARDIANS.text"),
    GUNNERS("EndWordMercenary.GUNNERS.text"),
    GUNSLINGERS("EndWordMercenary.GUNSLINGERS.text"),
    HARBINGERS("EndWordMercenary.HARBINGERS.text"),
    HAWKMEN("EndWordMercenary.HAWKMEN.text"),
    HAWKS("EndWordMercenary.HAWKS.text"),
    HELLHOUNDS("EndWordMercenary.HELLHOUNDS.text"),
    HELLIONS("EndWordMercenary.HELLIONS.text"),
    HOUNDS("EndWordMercenary.HOUNDS.text"),
    HUNTERS("EndWordMercenary.HUNTERS.text"),
    HUNTSMEN("EndWordMercenary.HUNTSMEN.text"),
    HUSKIES("EndWordMercenary.HUSKIES.text"),
    INFILTRATORS("EndWordMercenary.INFILTRATORS.text"),
    INVADERS("EndWordMercenary.INVADERS.text"),
    INVICTUS("EndWordMercenary.INVICTUS.text"),
    IRONCLADS("EndWordMercenary.IRONCLADS.text"),
    IRONMEN("EndWordMercenary.IRONMEN.text"),
    JUGGERNAUTS("EndWordMercenary.JUGGERNAUTS.text"),
    KNIGHTS("EndWordMercenary.KNIGHTS.text"),
    LANCERS("EndWordMercenary.LANCERS.text"),
    LEGENDS("EndWordMercenary.LEGENDS.text"),
    LEGION("EndWordMercenary.LEGION.text"),
    LEGIONNAIRES("EndWordMercenary.LEGIONNAIRES.text"),
    LYNX("EndWordMercenary.LYNX.text"),
    MAELSTROM("EndWordMercenary.MAELSTROM.text"),
    MARAUDERS("EndWordMercenary.MARAUDERS.text"),
    MAULERS("EndWordMercenary.MAULERS.text"),
    MAVERICKS("EndWordMercenary.MAVERICKS.text"),
    MERCENARIES("EndWordMercenary.MERCENARIES.text"),
    MERCS("EndWordMercenary.MERCS.text"),
    NOMADS("EndWordMercenary.NOMADS.text"),
    OUTCASTS("EndWordMercenary.OUTCASTS.text"),
    OUTLAWS("EndWordMercenary.OUTLAWS.text"),
    OUTRIDERS("EndWordMercenary.OUTRIDERS.text"),
    PATHFINDERS("EndWordMercenary.PATHFINDERS.text"),
    PHALANX("EndWordMercenary.PHALANX.text"),
    PHANTOM("EndWordMercenary.PHANTOM.text"),
    PHANTOMS("EndWordMercenary.PHANTOMS.text"),
    PILLARS("EndWordMercenary.PILLARS.text"),
    PIONEERS("EndWordMercenary.PIONEERS.text"),
    PREDATORS("EndWordMercenary.PREDATORS.text"),
    PROWLERS("EndWordMercenary.PROWLERS.text"),
    RAIDERS("EndWordMercenary.RAIDERS.text"),
    RANGERS("EndWordMercenary.RANGERS.text"),
    RAPTORS("EndWordMercenary.RAPTORS.text"),
    RAVAGERS("EndWordMercenary.RAVAGERS.text"),
    REAPERS("EndWordMercenary.REAPERS.text"),
    REAVERS("EndWordMercenary.REAVERS.text"),
    REBELS("EndWordMercenary.REBELS.text"),
    RECON("EndWordMercenary.RECON.text"),
    RECRUITS("EndWordMercenary.RECRUITS.text"),
    RENEGADES("EndWordMercenary.RENEGADES.text"),
    RESOLUTES("EndWordMercenary.RESOLUTES.text"),
    RIDERS("EndWordMercenary.RIDERS.text"),
    RIPTIDES("EndWordMercenary.RIPTIDES.text"),
    RIVALS("EndWordMercenary.RIVALS.text"),
    ROGUES("EndWordMercenary.ROGUES.text"),
    RUNNERS("EndWordMercenary.RUNNERS.text"),
    SABERS("EndWordMercenary.SABERS.text"),
    SABOTEURS("EndWordMercenary.SABOTEURS.text"),
    SAVAGES("EndWordMercenary.SAVAGES.text"),
    SCORPIONS("EndWordMercenary.SCORPIONS.text"),
    SCOUTS("EndWordMercenary.SCOUTS.text"),
    SEEKERS("EndWordMercenary.SEEKERS.text"),
    SEERS("EndWordMercenary.SEERS.text"),
    SENTINELS("EndWordMercenary.SENTINELS.text"),
    SENTRIES("EndWordMercenary.SENTRIES.text"),
    SHADOWHUNTERS("EndWordMercenary.SHADOWHUNTERS.text"),
    SHADOWLORDS("EndWordMercenary.SHADOWLORDS.text"),
    SHADOWMEN("EndWordMercenary.SHADOWMEN.text"),
    SHADOWS("EndWordMercenary.SHADOWS.text"),
    SHARPSHOOTERS("EndWordMercenary.SHARPSHOOTERS.text"),
    SHOCKERS("EndWordMercenary.SHOCKERS.text"),
    SHOCKTROOPERS("EndWordMercenary.SHOCKTROOPERS.text"),
    SHOCKTROOPS("EndWordMercenary.SHOCKTROOPS.text"),
    SHOCKWAVE("EndWordMercenary.SHOCKWAVE.text"),
    SKIRMISHERS("EndWordMercenary.SKIRMISHERS.text"),
    SKYFIGHTERS("EndWordMercenary.SKYFIGHTERS.text"),
    SKYHAWKS("EndWordMercenary.SKYHAWKS.text"),
    SKYWALKERS("EndWordMercenary.SKYWALKERS.text"),
    SLAYERS("EndWordMercenary.SLAYERS.text"),
    SOVEREIGNS("EndWordMercenary.SOVEREIGNS.text"),
    SPARTANS("EndWordMercenary.SPARTANS.text"),
    SPECTERS("EndWordMercenary.SPECTERS.text"),
    STALKERS("EndWordMercenary.STALKERS.text"),
    STORMBORN("EndWordMercenary.STORMBORN.text"),
    STORMBREAKERS("EndWordMercenary.STORMBREAKERS.text"),
    STORMBRINGERS("EndWordMercenary.STORMBRINGERS.text"),
    STORMGUARD("EndWordMercenary.STORMGUARD.text"),
    STORMLORDS("EndWordMercenary.STORMLORDS.text"),
    STORMRIDERS("EndWordMercenary.STORMRIDERS.text"),
    STORMRUNNERS("EndWordMercenary.STORMRUNNERS.text"),
    STRIDERS("EndWordMercenary.STRIDERS.text"),
    STRIKERS("EndWordMercenary.STRIKERS.text"),
    SYNDICATE("EndWordMercenary.SYNDICATE.text"),
    TACTICAL("EndWordMercenary.TACTICAL.text"),
    TACTICIANS("EndWordMercenary.TACTICIANS.text"),
    TEMPESTS("EndWordMercenary.TEMPESTS.text"),
    TERRORS("EndWordMercenary.TERRORS.text"),
    THUNDERBOLTS("EndWordMercenary.THUNDERBOLTS.text"),
    THUNDERERS("EndWordMercenary.THUNDERERS.text"),
    TIGERS("EndWordMercenary.TIGERS.text"),
    TITANS("EndWordMercenary.TITANS.text"),
    TRACERS("EndWordMercenary.TRACERS.text"),
    TRIDENTS("EndWordMercenary.TRIDENTS.text"),
    TROUBLESHOOTERS("EndWordMercenary.TROUBLESHOOTERS.text"),
    VANGUARD("EndWordMercenary.VANGUARD.text"),
    VANQUISHERS("EndWordMercenary.VANQUISHERS.text"),
    VIGILANTES("EndWordMercenary.VIGILANTES.text"),
    VIKINGS("EndWordMercenary.VIKINGS.text"),
    VORTEX("EndWordMercenary.VORTEX.text"),
    WARBAND("EndWordMercenary.WARBAND.text"),
    WARBRINGERS("EndWordMercenary.WARBRINGERS.text"),
    WARDENS("EndWordMercenary.WARDENS.text"),
    WARHOUNDS("EndWordMercenary.WARHOUNDS.text"),
    WARLOCKS("EndWordMercenary.WARLOCKS.text"),
    WARLORDS("EndWordMercenary.WARLORDS.text"),
    WARRIORS("EndWordMercenary.WARRIORS.text"),
    WATCHDOGS("EndWordMercenary.WATCHDOGS.text"),
    WATCHERS("EndWordMercenary.WATCHERS.text"),
    WOLFPACK("EndWordMercenary.WOLFPACK.text"),
    WOLVES("EndWordMercenary.WOLVES.text"),
    WRAITHS("EndWordMercenary.WRAITHS.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String word;
    //endregion Variable Declarations

    //region Constructors
    EndWordMercenary(final String word) {
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
     * @return a random word from the EndWordMercenary enum.
     */
    public static String getRandomWord() {
        EndWordMercenary[] words = EndWordMercenary.values();
        EndWordMercenary randomWord = words[RANDOM.nextInt(words.length)];
        return randomWord.toString();
    }
}
