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
package mekhq.campaign.digitalGM.stratCon.facility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The facility effects this version knows, one class per {@code effect} name in the definition files. Each carries
 * only the parameters it needs and answers only its own hook on {@link IStratConFacilityEffect}.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilityEffects {

    private StratConFacilityEffects() {
    }

    /**
     * Base for the effects that carry a list of scenario modifier IDs.
     *
     * @author Illiani
     * @since 0.51.01
     */
    abstract static class ModifierListEffect implements IStratConFacilityEffect {
        private List<String> modifiers = new ArrayList<>();

        ModifierListEffect() {
        }

        ModifierListEffect(List<String> modifiers) {
            this.modifiers = new ArrayList<>(modifiers);
        }

        /**
         * @return the scenario modifier IDs, never {@code null}
         *
         * @author Illiani
         * @since 0.51.01
         */
        public List<String> getModifiers() {
            return (modifiers == null) ? Collections.emptyList() : Collections.unmodifiableList(modifiers);
        }
    }

    /**
     * Scenario modifiers applied to scenarios fought on the facility itself.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class LocalModifiersEffect extends ModifierListEffect {
        public static final String NAME = "localModifiers";

        LocalModifiersEffect() {
        }

        public LocalModifiersEffect(List<String> modifiers) {
            super(modifiers);
        }

        @Override
        public List<String> getLocalModifierIds() {
            return getModifiers();
        }
    }

    /**
     * Scenario modifiers the facility may lend to scenarios elsewhere in its sector.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class SharedModifiersEffect extends ModifierListEffect {
        public static final String NAME = "sharedModifiers";

        SharedModifiersEffect() {
        }

        public SharedModifiersEffect(List<String> modifiers) {
            super(modifiers);
        }

        @Override
        public List<String> getSharedModifierIds() {
            return getModifiers();
        }
    }

    /**
     * Base for the effects that carry a single whole number.
     *
     * @author Illiani
     * @since 0.51.01
     */
    abstract static class ValueEffect implements IStratConFacilityEffect {
        private int value;

        ValueEffect() {
        }

        ValueEffect(int value) {
            this.value = value;
        }

        /**
         * @return the effect's value
         *
         * @author Illiani
         * @since 0.51.01
         */
        public int getValue() {
            return value;
        }
    }

    /**
     * Hexes added to the scan range of every force scouting the sector.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class ScanRangeEffect extends ValueEffect {
        public static final String NAME = "scanRange";

        ScanRangeEffect() {
        }

        public ScanRangeEffect(int value) {
            super(value);
        }

        @Override
        public int getScanRangeIncrease() {
            return getValue();
        }
    }

    /**
     * A change to the sector's scenario odds.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class ScenarioOddsEffect extends ValueEffect {
        public static final String NAME = "scenarioOdds";

        ScenarioOddsEffect() {
        }

        public ScenarioOddsEffect(int value) {
            super(value);
        }

        @Override
        public int getScenarioOddsModifier() {
            return getValue();
        }
    }

    /**
     * Support points gained, or lost if negative, at the start of each month.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class MonthlySupportPointsEffect extends ValueEffect {
        public static final String NAME = "monthlySupportPoints";

        MonthlySupportPointsEffect() {
        }

        public MonthlySupportPointsEffect(int value) {
            super(value);
        }

        @Override
        public int getMonthlySupportPoints() {
            return getValue();
        }
    }

    /**
     * Reveals the facility's whole sector.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class RevealTrackEffect implements IStratConFacilityEffect {
        public static final String NAME = "revealTrack";

        @Override
        public boolean isRevealingTrack() {
            return true;
        }
    }

    /**
     * Keeps air and space scenarios out of the facility's sector, whichever side holds it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class PreventAerospaceEffect implements IStratConFacilityEffect {
        public static final String NAME = "preventAerospace";

        @Override
        public boolean isPreventingAerospace() {
            return true;
        }
    }

    /**
     * Stands in for an effect name this version does not know. It does nothing, so a definition written for a newer
     * version still loads.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final class UnknownEffect implements IStratConFacilityEffect {
    }
}
