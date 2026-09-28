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
package testUtilities.parts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.CombatInformationCenter;
import mekhq.campaign.parts.InfantryArmorPart;
import mekhq.campaign.parts.InfantryMotiveType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.SpacecraftCoolingSystem;
import mekhq.campaign.parts.TransportBayPart;
import mekhq.campaign.parts.equipment.BattleArmorAmmoBin;
import mekhq.campaign.parts.equipment.InfantryWeaponPart;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.parts.missing.MissingMekLocation;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.parts.missing.MissingProtoMekLocation;
import mekhq.campaign.unit.Unit;

/**
 * Collects real parts for the part contract tests: every distinct part installed on the {@link UnitFixture} units,
 * plus a few parts no fixture unit carries (MASC, a Combat Information Center, infantry armor kits and motive types),
 * built the way the game builds them.
 *
 * <p>Parts are distinct by class and name, so a unit with twelve identical heat sinks contributes one heat sink. The
 * parts stay attached to their units inside the scenario's campaign, so a test can reload a saved part against the
 * same campaign and have its unit reference resolve.</p>
 */
public final class PartSamples {
    /** The cost given to the Combat Information Center sample, a round figure in the range a WarShip's CIC costs. */
    public static final Money COMBAT_INFORMATION_CENTER_COST = Money.of(1_000_000);

    /**
     * Real parts whose missing placeholder does not accept a copy of the part today. These cases are outside issue
     * #10176 and are skipped with this reason until they get their own fix.
     */
    private static final Map<Class<? extends Part>, String> KNOWN_REPLACEMENT_BREAKS = Map.of(
          BattleArmorAmmoBin.class,
          "Follow-up to #10176: the placeholder is a plain MissingAmmoBin, which only accepts a plain AmmoBin",
          InfantryWeaponPart.class,
          "Follow-up to #10176: the placeholder is a MissingEquipmentPart, which only accepts a plain EquipmentPart");

    private final PartsScenario scenario;
    private final List<Part> realParts;

    private PartSamples(PartsScenario scenario, List<Part> realParts) {
        this.scenario = scenario;
        this.realParts = realParts;
    }

    /**
     * Builds a new scenario, adds one of every {@link UnitFixture} unit, and gathers the samples. Each call builds
     * everything afresh, so tests never share part objects.
     *
     * @return the samples
     */
    public static PartSamples create() {
        PartsScenario scenario = PartsScenario.create();
        List<Part> realParts = new ArrayList<>();
        Set<String> seenParts = new HashSet<>();

        for (UnitFixture fixture : UnitFixture.values()) {
            Unit unit = scenario.withUnit(fixture);
            for (Part part : unit.getParts()) {
                boolean isPlaceholder = part instanceof MissingPart;
                boolean isFirstOfItsKind = seenParts.add(part.getClass().getName() + '|' + part.getName());
                if (!isPlaceholder && isFirstOfItsKind) {
                    realParts.add(part);
                }
            }
        }

        realParts.addAll(constructedParts(scenario.getCampaign()));
        return new PartSamples(scenario, realParts);
    }

    /**
     * Builds the parts that no fixture unit carries, as unattached spares.
     *
     * @param campaign the campaign the parts belong to
     *
     * @return one MASC, one Combat Information Center, one infantry armor kit and one infantry jump pack
     */
    private static List<Part> constructedParts(Campaign campaign) {
        EquipmentType mascType = EquipmentType.get(EquipmentTypeLookup.IS_MASC);
        MASC masc = new MASC(55, mascType, -1, campaign, 275, false);
        CombatInformationCenter combatInformationCenter = new CombatInformationCenter(0,
              COMBAT_INFORMATION_CENTER_COST, campaign);
        InfantryArmorPart infantryArmor = new InfantryArmorPart(0, campaign, 2.0, false, false, true, false, false,
              false);
        InfantryMotiveType jumpPack = new InfantryMotiveType(0, campaign, EntityMovementMode.INF_JUMP);
        return List.of(masc, combatInformationCenter, infantryArmor, jumpPack);
    }

    /**
     * @return the scenario the samples live in; installed parts are attached to units of its campaign
     */
    public PartsScenario getScenario() {
        return scenario;
    }

    /**
     * @return the campaign the samples belong to
     */
    public Campaign getCampaign() {
        return scenario.getCampaign();
    }

    /**
     * @return every sampled real part, that is every part that is not a missing-part placeholder
     */
    public List<Part> getRealParts() {
        return realParts;
    }

    /**
     * Builds the missing-part placeholder of every sampled real part that has one.
     *
     * @return each placeholder paired with the real part it stands for
     */
    public List<MissingSample> getMissingParts() {
        List<MissingSample> missingSamples = new ArrayList<>();
        for (Part realPart : realParts) {
            MissingPart missingPart = realPart.getMissingPart();
            if (missingPart != null) {
                missingSamples.add(new MissingSample(realPart, missingPart));
            }
        }
        return missingSamples;
    }

    /**
     * Tells whether a part can never be a spare: it is built into the unit, cannot be removed, and is never the same
     * part type as any other part. A transport bay and a spacecraft cooling system are like this, so they are never
     * stocked or cloned into a spare.
     *
     * @param part the part to check
     *
     * @return {@code true} when the part is never stocked
     */
    public static boolean isNeverStocked(Part part) {
        return (part instanceof TransportBayPart) || (part instanceof SpacecraftCoolingSystem);
    }

    /**
     * Gives the reason a real part's missing placeholder is known not to accept a copy of the part, for a case
     * outside issue #10176 that is skipped until its own fix.
     *
     * @param realPart the part the placeholder was made from
     *
     * @return the reason, or {@code null} when the placeholder is expected to accept the part
     */
    public static @Nullable String knownReplacementBreak(Part realPart) {
        return KNOWN_REPLACEMENT_BREAKS.get(realPart.getClass());
    }

    /**
     * Gives a sample a readable name for a parameterized test, such as {@code MekLocation: Left Arm}.
     *
     * @param part the sample
     *
     * @return the part's class name and part name
     */
    public static String describe(Part part) {
        return part.getClass().getSimpleName() + ": " + part.getName();
    }

    /**
     * A missing-part placeholder together with the real part it stands for.
     *
     * @param realPart    the part the placeholder was made from
     * @param missingPart the placeholder, as {@link Part#getMissingPart()} returned it
     */
    public record MissingSample(Part realPart, MissingPart missingPart) {
        /**
         * Tells whether the placeholder may only be filled during a refit. By rule a destroyed Mek center torso or
         * ProtoMek torso cannot be replaced in an ordinary repair, so its placeholder refuses every spare outside a
         * refit.
         *
         * @return {@code true} for a Mek center torso or ProtoMek torso placeholder
         */
        public boolean isReplacedOnlyByRefit() {
            boolean isMekCenterTorso = (missingPart instanceof MissingMekLocation missingMekLocation)
                  && (missingMekLocation.getLocation() == Mek.LOC_CENTER_TORSO);
            boolean isProtoMekTorso = (missingPart instanceof MissingProtoMekLocation missingProtoMekLocation)
                  && (missingProtoMekLocation.getLocation() == ProtoMek.LOC_TORSO);
            return isMekCenterTorso || isProtoMekTorso;
        }
    }
}
