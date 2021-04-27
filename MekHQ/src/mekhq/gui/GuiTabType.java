/*
 * Copyright (c) 2017, 2020 - The MegaMek Team. All rights reserved.
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
package mekhq.gui;

import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import megamek.common.util.EncodeControl;

/**
 * Identifies the standard tabs and provides a factory method.
 *
 * @author Neoancient
 *
 * The mnemonics used in this are included in the list at {@link CampaignGUI#initMenu()},
 * and they MUST be unique on that list
 */
public enum GuiTabType {
    //region Enum Declaration
    COMMAND(0, "panCommand.TabConstraints.tabTitle", KeyEvent.VK_O),
    TOE(1, "panOrganization.TabConstraints.tabTitle", KeyEvent.VK_T),
    BRIEFING(2, "panBriefing.TabConstraints.tabTitle", KeyEvent.VK_B),
    MAP(3, "panMap.TabConstraints.tabTitle", KeyEvent.VK_S),
    PERSONNEL(4, "panPersonnel.TabConstraints.tabTitle", KeyEvent.VK_P),
    HANGAR(5, "panHangar.TabConstraints.tabTitle", KeyEvent.VK_H),
    WAREHOUSE(6, "panSupplies.TabConstraints.tabTitle", KeyEvent.VK_W),
    REPAIR(7, "panRepairBay.TabConstraints.tabTitle", KeyEvent.VK_R),
    INFIRMARY(8, "panInfirmary.TabConstraints.tabTitle", KeyEvent.VK_I),
    MEKLAB(9, "panMekLab.TabConstraints.tabTitle", KeyEvent.VK_L),
    FINANCES(10, "panFinances.TabConstraints.tabTitle", KeyEvent.VK_N),
    STRATCON(11, "panStratcon.TabConstraints.tabTitle", KeyEvent.VK_C),
    CUSTOM(12, "panCustom.TabConstraints.tabTitle", KeyEvent.VK_UNDEFINED);
    //endregion Enum Declaration

    //region Variable Declarations
    private final int defaultPos;
    private final String name;
    private final int mnemonic;
    //endregion Variable Declarations

    public int getDefaultPos() {
        return defaultPos;
    }

    public String getTabName() {
        return name;
    }

    public int getMnemonic() {
        return mnemonic;
    }

    GuiTabType(int defaultPos, String resKey, int mnemonic) {
        this.defaultPos = defaultPos;

        ResourceBundle resources = ResourceBundle.getBundle(
                "mekhq.resources.CampaignGUI", new EncodeControl());

        name = (resKey == null) ? "Custom" : resources.getString(resKey);

        this.mnemonic = mnemonic;
    }

    public CampaignGuiTab createTab(CampaignGUI gui) {
        switch (this) {
            case COMMAND:
                return new CommandCenterTab(gui, name);
            case TOE:
                return new TOETab(gui, name);
            case BRIEFING:
                return new BriefingTab(gui, name);
            case MAP:
                return new MapTab(gui, name);
            case PERSONNEL:
                return new PersonnelTab(gui, name);
            case HANGAR:
                return new HangarTab(gui, name);
            case WAREHOUSE:
                return new WarehouseTab(gui, name);
            case REPAIR:
                return new RepairTab(gui, name);
            case INFIRMARY:
                return new InfirmaryTab(gui, name);
            case MEKLAB:
                return new MekLabTab(gui, name);
            case FINANCES:
                return new FinancesTab(gui, name);
            case STRATCON:
                return new StratconTab(gui, name);
            default:
                return null;
        }
    }
}
