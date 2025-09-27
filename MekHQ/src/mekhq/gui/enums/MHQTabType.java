/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.gui.*;

/**
 * Identifies the standard tabs and provides a creation method. The mnemonics used in this are included in the list at
 * {@link CampaignGUI #initMenu()}, and they MUST be unique on that list. The order in which the tabs appear on the
 * CampaignGUI is determined by the order in which they are declared here.
 *
 * @author Neoancient
 */
public enum MHQTabType {
    //region Enum Declaration
    COMMAND_CENTER("MHQTabType.COMMAND_CENTER.text", KeyEvent.VK_O),
    INTERSTELLAR_MAP("MHQTabType.INTERSTELLAR_MAP.text", KeyEvent.VK_S),
    TOE("MHQTabType.TOE.text", KeyEvent.VK_T),
    BRIEFING_ROOM("MHQTabType.BRIEFING_ROOM.text", KeyEvent.VK_B),
    STRAT_CON("MHQTabType.STRAT_CON.text", KeyEvent.VK_C),
    PERSONNEL("MHQTabType.PERSONNEL.text", KeyEvent.VK_P),
    HANGAR("MHQTabType.HANGAR.text", KeyEvent.VK_H),
    REPAIR_BAY("MHQTabType.REPAIR_BAY.text", KeyEvent.VK_R),
    WAREHOUSE("MHQTabType.WAREHOUSE.text", KeyEvent.VK_W),
    INFIRMARY("MHQTabType.INFIRMARY.text", KeyEvent.VK_I),
    FINANCES("MHQTabType.FINANCES.text", KeyEvent.VK_N),
    MEK_LAB("MHQTabType.MEK_LAB.text", KeyEvent.VK_L);
    //endregion Enum Declaration

    //region Variable Declarations
    private final String name;
    private final int mnemonic;
    //endregion Variable Declarations

    //region Constructors
    MHQTabType(final String name, final int mnemonic) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.mnemonic = mnemonic;
    }
    //endregion Constructors

    //region Getters
    public int getMnemonic() {
        return mnemonic;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isCommandCenter() {
        return this == COMMAND_CENTER;
    }

    public boolean isTOE() {
        return this == TOE;
    }

    public boolean isBriefingRoom() {
        return this == BRIEFING_ROOM;
    }

    public boolean isInterstellarMap() {
        return this == INTERSTELLAR_MAP;
    }

    public boolean isPersonnel() {
        return this == PERSONNEL;
    }

    public boolean isHangar() {
        return this == HANGAR;
    }

    public boolean isWarehouse() {
        return this == WAREHOUSE;
    }

    public boolean isRepairBay() {
        return this == REPAIR_BAY;
    }

    public boolean isInfirmary() {
        return this == INFIRMARY;
    }

    public boolean isFinances() {
        return this == FINANCES;
    }

    public boolean isMekLab() {
        return this == MEK_LAB;
    }

    public boolean isStratCon() {
        return this == STRAT_CON;
    }
    //endregion Boolean Comparison Methods

    public @Nullable CampaignGuiTab createTab(final CampaignGUI gui) {
        return switch (this) {
            case COMMAND_CENTER -> new CommandCenterTab(gui, toString());
            case TOE -> new TOETab(gui, toString());
            case BRIEFING_ROOM -> new BriefingTab(gui, toString());
            case INTERSTELLAR_MAP -> new MapTab(gui, toString());
            case PERSONNEL -> new PersonnelTab(gui, toString());
            case HANGAR -> new HangarTab(gui, toString());
            case WAREHOUSE -> new WarehouseTab(gui, toString());
            case REPAIR_BAY -> new RepairTab(gui, toString());
            case INFIRMARY -> new InfirmaryTab(gui, toString());
            case FINANCES -> new FinancesTab(gui, toString());
            case MEK_LAB -> new MekLabTab(gui, toString());
            case STRAT_CON -> new StratConTab(gui, toString());
        };
    }

    @Override
    public String toString() {
        return name;
    }

    public static MHQTabType parseFromString(String source) {

        // attempt enum parse
        try {
            return valueOf(source);
        } catch (Exception ignored) {}

        // failing all else, return command center
        return COMMAND_CENTER;
    }
}
