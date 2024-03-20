/*
 * Copyright (c) 2017-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.gui.*;

import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

/**
 * Identifies the standard tabs and provides a creation method.
 * The mnemonics used in this are included in the list at {@link CampaignGUI#initMenu()}, and they
 * MUST be unique on that list.
 * @author Neoancient
 */
public enum MHQTabType {
    //region Enum Declaration
    COMMAND_CENTER("MHQTabType.COMMAND_CENTER.text", KeyEvent.VK_O),
    TOE("MHQTabType.TOE.text", KeyEvent.VK_T),
    BRIEFING_ROOM("MHQTabType.BRIEFING_ROOM.text", KeyEvent.VK_B),
    INTERSTELLAR_MAP("MHQTabType.INTERSTELLAR_MAP.text", KeyEvent.VK_S),
    PERSONNEL("MHQTabType.PERSONNEL.text", KeyEvent.VK_P),
    HANGAR("MHQTabType.HANGAR.text", KeyEvent.VK_H),
    WAREHOUSE("MHQTabType.WAREHOUSE.text", KeyEvent.VK_W),
    REPAIR_BAY("MHQTabType.REPAIR_BAY.text", KeyEvent.VK_R),
    INFIRMARY("MHQTabType.INFIRMARY.text", KeyEvent.VK_I),
    FINANCES("MHQTabType.FINANCES.text", KeyEvent.VK_N),
    MEK_LAB("MHQTabType.MEK_LAB.text", KeyEvent.VK_L),
    STRAT_CON("MHQTabType.STRAT_CON.text", KeyEvent.VK_C);
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
        switch (this) {
            case COMMAND_CENTER:
                return new CommandCenterTab(gui, toString());
            case TOE:
                return new TOETab(gui, toString());
            case BRIEFING_ROOM:
                return new BriefingTab(gui, toString());
            case INTERSTELLAR_MAP:
                return new MapTab(gui, toString());
            case PERSONNEL:
                return new PersonnelTab(gui, toString());
            case HANGAR:
                return new HangarTab(gui, toString());
            case WAREHOUSE:
                return new WarehouseTab(gui, toString());
            case REPAIR_BAY:
                return new RepairTab(gui, toString());
            case INFIRMARY:
                return new InfirmaryTab(gui, toString());
            case FINANCES:
                return new FinancesTab(gui, toString());
            case MEK_LAB:
                return new MekLabTab(gui, toString());
            case STRAT_CON:
                return new StratconTab(gui, toString());
            default:
                return null;
        }
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
