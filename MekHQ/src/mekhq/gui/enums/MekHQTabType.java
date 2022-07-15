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
import megamek.common.util.EncodeControl;
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
public enum MekHQTabType {
    //region Enum Declaration
    COMMAND_CENTER("MekHQTabType.COMMAND_CENTER.text", KeyEvent.VK_O),
    TOE("MekHQTabType.TOE.text", KeyEvent.VK_T),
    BRIEFING_ROOM("MekHQTabType.BRIEFING_ROOM.text", KeyEvent.VK_B),
    INTERSTELLAR_MAP("MekHQTabType.INTERSTELLAR_MAP.text", KeyEvent.VK_S),
    PERSONNEL("MekHQTabType.PERSONNEL.text", KeyEvent.VK_P),
    HANGAR("MekHQTabType.HANGAR.text", KeyEvent.VK_H),
    WAREHOUSE("MekHQTabType.WAREHOUSE.text", KeyEvent.VK_W),
    REPAIR_BAY("MekHQTabType.REPAIR_BAY.text", KeyEvent.VK_R),
    INFIRMARY("MekHQTabType.INFIRMARY.text", KeyEvent.VK_I),
    FINANCES("MekHQTabType.FINANCES.text", KeyEvent.VK_N),
    MEK_LAB("MekHQTabType.MEK_LAB.text", KeyEvent.VK_L),
    STRAT_CON("MekHQTabType.STRAT_CON.text", KeyEvent.VK_C);
    //endregion Enum Declaration

    //region Variable Declarations
    private final String name;
    private final int mnemonic;
    //endregion Variable Declarations

    //region Constructors
    MekHQTabType(final String name, final int mnemonic) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
                MekHQ.getMHQOptions().getLocale(),  new EncodeControl());
        this.name = resources.getString(name);
        this.mnemonic = mnemonic;
    }
    //endregion Constructors

    //region Getters
    public int getMnemonic() {
        return mnemonic;
    }
    //endregion Getters

    public @Nullable CampaignGuiTab createTab(final CampaignGUI gui) {
        switch (this) {
            case COMMAND_CENTER:
                return new CommandCenterTab(gui, name);
            case TOE:
                return new TOETab(gui, name);
            case BRIEFING_ROOM:
                return new BriefingTab(gui, name);
            case INTERSTELLAR_MAP:
                return new MapTab(gui, name);
            case PERSONNEL:
                return new PersonnelTab(gui, name);
            case HANGAR:
                return new HangarTab(gui, name);
            case WAREHOUSE:
                return new WarehouseTab(gui, name);
            case REPAIR_BAY:
                return new RepairTab(gui, name);
            case INFIRMARY:
                return new InfirmaryTab(gui, name);
            case MEK_LAB:
                return new MekLabTab(gui, name);
            case FINANCES:
                return new FinancesTab(gui, name);
            case STRAT_CON:
                return new StratconTab(gui, name);
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
