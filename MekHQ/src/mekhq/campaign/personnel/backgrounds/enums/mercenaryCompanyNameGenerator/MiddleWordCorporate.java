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

public enum MiddleWordCorporate {
    ADVISORY("MiddleWordCorporate.ADVISORY.text"),
    AEGIS("MiddleWordCorporate.AEGIS.text"),
    ALLEGIANCE("MiddleWordCorporate.ALLEGIANCE.text"),
    APEX("MiddleWordCorporate.APEX.text"),
    ASCENDANT("MiddleWordCorporate.ASCENDANT.text"),
    ATLAS("MiddleWordCorporate.ATLAS.text"),
    AXIS("MiddleWordCorporate.AXIS.text"),
    BEACON("MiddleWordCorporate.BEACON.text"),
    CAPITAL("MiddleWordCorporate.CAPITAL.text"),
    CATALYST("MiddleWordCorporate.CATALYST.text"),
    CIPHER("MiddleWordCorporate.CIPHER.text"),
    CITADEL("MiddleWordCorporate.CITADEL.text"),
    COMMAND("MiddleWordCorporate.COMMAND.text"),
    CONCORD("MiddleWordCorporate.CONCORD.text"),
    CONQUEST("MiddleWordCorporate.CONQUEST.text"),
    CONSULTING("MiddleWordCorporate.CONSULTING.text"),
    CONTINUUM("MiddleWordCorporate.CONTINUUM.text"),
    CORE("MiddleWordCorporate.CORE.text"),
    CREST("MiddleWordCorporate.CREST.text"),
    CRUCIBLE("MiddleWordCorporate.CRUCIBLE.text"),
    DEVELOPMENT("MiddleWordCorporate.DEVELOPMENT.text"),
    DOMINION("MiddleWordCorporate.DOMINION.text"),
    DYNAMICS("MiddleWordCorporate.DYNAMICS.text"),
    DYNAMO("MiddleWordCorporate.DYNAMO.text"),
    ELEMENT("MiddleWordCorporate.ELEMENT.text"),
    ELITE("MiddleWordCorporate.ELITE.text"),
    EMPIRE("MiddleWordCorporate.EMPIRE.text"),
    ENDEAVOR("MiddleWordCorporate.ENDEAVOR.text"),
    ENGINEERING("MiddleWordCorporate.ENGINEERING.text"),
    ENIGMA("MiddleWordCorporate.ENIGMA.text"),
    ENTERPRISE("MiddleWordCorporate.ENTERPRISE.text"),
    ENVISION("MiddleWordCorporate.ENVISION.text"),
    EPOCH("MiddleWordCorporate.EPOCH.text"),
    EXCELSIOR("MiddleWordCorporate.EXCELSIOR.text"),
    EXEMPLAR("MiddleWordCorporate.EXEMPLAR.text"),
    FORTRESS("MiddleWordCorporate.FORTRESS.text"),
    FRONTIER("MiddleWordCorporate.FRONTIER.text"),
    FULCRUM("MiddleWordCorporate.FULCRUM.text"),
    FUSION("MiddleWordCorporate.FUSION.text"),
    GENESIS("MiddleWordCorporate.GENESIS.text"),
    GRAVITAS("MiddleWordCorporate.GRAVITAS.text"),
    GRIT("MiddleWordCorporate.GRIT.text"),
    HELIX("MiddleWordCorporate.HELIX.text"),
    HOLDINGS("MiddleWordCorporate.HOLDINGS.text"),
    HORIZON("MiddleWordCorporate.HORIZON.text"),
    IGNITE("MiddleWordCorporate.IGNITE.text"),
    IMPACT("MiddleWordCorporate.IMPACT.text"),
    IMPERIUM("MiddleWordCorporate.IMPERIUM.text"),
    IMPETUS("MiddleWordCorporate.IMPETUS.text"),
    INCEPTION("MiddleWordCorporate.INCEPTION.text"),
    INNOVATIONS("MiddleWordCorporate.INNOVATIONS.text"),
    INSIGHT("MiddleWordCorporate.INSIGHT.text"),
    INTEGRATION("MiddleWordCorporate.INTEGRATION.text"),
    INTEGRITY("MiddleWordCorporate.INTEGRITY.text"),
    INTREPID("MiddleWordCorporate.INTREPID.text"),
    INVESTMENTS("MiddleWordCorporate.INVESTMENTS.text"),
    INVICTUS("MiddleWordCorporate.INVICTUS.text"),
    KINETICS("MiddleWordCorporate.KINETICS.text"),
    LEGACY("MiddleWordCorporate.LEGACY.text"),
    LIBERTY("MiddleWordCorporate.LIBERTY.text"),
    LOGISTICS("MiddleWordCorporate.LOGISTICS.text"),
    LUMINA("MiddleWordCorporate.LUMINA.text"),
    LUMINARY("MiddleWordCorporate.LUMINARY.text"),
    MANAGEMENT("MiddleWordCorporate.MANAGEMENT.text"),
    MATRIX("MiddleWordCorporate.MATRIX.text"),
    MERIDIAN("MiddleWordCorporate.MERIDIAN.text"),
    MODUS("MiddleWordCorporate.MODUS.text"),
    MOMENTUM("MiddleWordCorporate.MOMENTUM.text"),
    MONUMENT("MiddleWordCorporate.MONUMENT.text"),
    NEBULA("MiddleWordCorporate.NEBULA.text"),
    NETWORKS("MiddleWordCorporate.NETWORKS.text"),
    NEXUS("MiddleWordCorporate.NEXUS.text"),
    NOVA("MiddleWordCorporate.NOVA.text"),
    OBJECTIVE("MiddleWordCorporate.OBJECTIVE.text"),
    OBSIDIAN("MiddleWordCorporate.OBSIDIAN.text"),
    ONYX("MiddleWordCorporate.ONYX.text"),
    OPERATIONS("MiddleWordCorporate.OPERATIONS.text"),
    OPTIMA("MiddleWordCorporate.OPTIMA.text"),
    OPTIMUM("MiddleWordCorporate.OPTIMUM.text"),
    ORIGIN("MiddleWordCorporate.ORIGIN.text"),
    PARADIGM("MiddleWordCorporate.PARADIGM.text"),
    PARAGON("MiddleWordCorporate.PARAGON.text"),
    PARTNERS("MiddleWordCorporate.PARTNERS.text"),
    PINNACLE("MiddleWordCorporate.PINNACLE.text"),
    PIONEER("MiddleWordCorporate.PIONEER.text"),
    PRAXIS("MiddleWordCorporate.PRAXIS.text"),
    PRECISION("MiddleWordCorporate.PRECISION.text"),
    PRESTIGE("MiddleWordCorporate.PRESTIGE.text"),
    PRIME("MiddleWordCorporate.PRIME.text"),
    PRISM("MiddleWordCorporate.PRISM.text"),
    PRODIGY("MiddleWordCorporate.PRODIGY.text"),
    PROSPECT("MiddleWordCorporate.PROSPECT.text"),
    PULSE("MiddleWordCorporate.PULSE.text"),
    QUANTUM("MiddleWordCorporate.QUANTUM.text"),
    RADIANCE("MiddleWordCorporate.RADIANCE.text"),
    RAPTOR("MiddleWordCorporate.RAPTOR.text"),
    REALM("MiddleWordCorporate.REALM.text"),
    REIGN("MiddleWordCorporate.REIGN.text"),
    REPUTE("MiddleWordCorporate.REPUTE.text"),
    RESOLUTE("MiddleWordCorporate.RESOLUTE.text"),
    RESOLVE("MiddleWordCorporate.RESOLVE.text"),
    RESOURCES("MiddleWordCorporate.RESOURCES.text"),
    REVIVE("MiddleWordCorporate.REVIVE.text"),
    RISING("MiddleWordCorporate.RISING.text"),
    SAVANT("MiddleWordCorporate.SAVANT.text"),
    SENTINEL("MiddleWordCorporate.SENTINEL.text"),
    SERVICES("MiddleWordCorporate.SERVICES.text"),
    SOLSTICE("MiddleWordCorporate.SOLSTICE.text"),
    SOLUTIONS("MiddleWordCorporate.SOLUTIONS.text"),
    SOVEREIGN("MiddleWordCorporate.SOVEREIGN.text"),
    SPECTRUM("MiddleWordCorporate.SPECTRUM.text"),
    STRATEGIES("MiddleWordCorporate.STRATEGIES.text"),
    STRATEGY("MiddleWordCorporate.STRATEGY.text"),
    STRATOS("MiddleWordCorporate.STRATOS.text"),
    SUMMIT("MiddleWordCorporate.SUMMIT.text"),
    SURGE("MiddleWordCorporate.SURGE.text"),
    SYNERGY("MiddleWordCorporate.SYNERGY.text"),
    SYSTEMS("MiddleWordCorporate.SYSTEMS.text"),
    TECHNOLOGIES("MiddleWordCorporate.TECHNOLOGIES.text"),
    TENACITY("MiddleWordCorporate.TENACITY.text"),
    TITAN("MiddleWordCorporate.TITAN.text"),
    TORQUE("MiddleWordCorporate.TORQUE.text"),
    TRAVERSE("MiddleWordCorporate.TRAVERSE.text"),
    TRIDENT("MiddleWordCorporate.TRIDENT.text"),
    TRIUMPH("MiddleWordCorporate.TRIUMPH.text"),
    UNITY("MiddleWordCorporate.UNITY.text"),
    VALOR("MiddleWordCorporate.VALOR.text"),
    VANGUARD("MiddleWordCorporate.VANGUARD.text"),
    VECTOR("MiddleWordCorporate.VECTOR.text"),
    VELOCITY("MiddleWordCorporate.VELOCITY.text"),
    VENTURE("MiddleWordCorporate.VENTURE.text"),
    VERITAS("MiddleWordCorporate.VERITAS.text"),
    VERTEX("MiddleWordCorporate.VERTEX.text"),
    VISION("MiddleWordCorporate.VISION.text"),
    VORTEX("MiddleWordCorporate.VORTEX.text"),
    ZENITH("MiddleWordCorporate.ZENITH.text"),
    ZEPHYR("MiddleWordCorporate.ZEPHYR.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String word;
    //endregion Variable Declarations

    //region Constructors
    MiddleWordCorporate(final String word) {
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
     * @return a random word from the CloserMercenary enum.
     */
    public static String getRandomWord() {
        MiddleWordCorporate[] words = MiddleWordCorporate.values();
        MiddleWordCorporate randomWord = words[RANDOM.nextInt(words.length)];
        return randomWord.toString();
    }
}
