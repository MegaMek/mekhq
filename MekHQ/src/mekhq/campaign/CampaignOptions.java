/*
 * PartInventiry.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign;

import java.io.Serializable;

/**
 *
 * @author natit
 */
public class CampaignOptions implements Serializable {

    public static int REPAIR_SYSTEM_STRATOPS = 0;
    public static int REPAIR_SYSTEM_WARCHEST_CUSTOM = 1;
    public static int REPAIR_SYSTEM_GENERIC_PARTS = 2;
    public static String [] REPAIR_SYSTEM_NAMES = {"Strat Ops", "Warchest Custom", "Generic Spare Parts"};

    public static boolean useFactionModifiers = true;
    protected boolean useFactionModifiers_saver;

    public static float clanPriceModifier = 2;
    protected float clanPriceModifier_saver;

    public static boolean useEasierRefit = true;
    protected boolean useEasierRefit_saver;

    public static int repairSystem = REPAIR_SYSTEM_STRATOPS;
    protected int repairSystem_saver;

    public CampaignOptions () {
    }

    public void save () {
        useFactionModifiers_saver = useFactionModifiers;
        clanPriceModifier_saver = clanPriceModifier;
        useEasierRefit_saver = useEasierRefit;
        repairSystem_saver = repairSystem;
    }

    public void restore () {
        useFactionModifiers = useFactionModifiers_saver;
        clanPriceModifier = clanPriceModifier_saver;
        useEasierRefit = useEasierRefit_saver;
        repairSystem = repairSystem_saver;
    }

    public static String getRepairSystemName (int repairSystem) {
        return REPAIR_SYSTEM_NAMES[repairSystem];
    }
}
