/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

package mekhq.gui.menus;

import static mekhq.gui.CampaignGUI.MAX_QUANTITY_SPINNER;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQInternationalization;

public class TemporaryPersonnelManagementMenu extends JMenu {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.MekHQMenuBar";
    private static final String MENU_ITEM_TEXT_SUFFIX = ".text";
    private static final List<PersonnelRole> BLOB_CREW_ROLES = List.of(
          PersonnelRole.SOLDIER, PersonnelRole.BATTLE_ARMOUR,
          PersonnelRole.VEHICLE_CREW_GROUND, PersonnelRole.VEHICLE_CREW_VTOL,
          PersonnelRole.VEHICLE_CREW_NAVAL, PersonnelRole.VESSEL_PILOT,
          PersonnelRole.VESSEL_GUNNER, PersonnelRole.VESSEL_CREW);

    private final Campaign campaign;
    private final BiFunction<String, Integer, Integer> getUserIntInput;

    // Blob crew menu references for visibility control
    private JMenu menuSoldierPool;
    private JMenu menuBattleArmorPool;
    private JMenu menuVehicleCrewGroundPool;
    private JMenu menuVehicleCrewVTOLPool;
    private JMenu menuVehicleCrewNavalPool;
    private JMenu menuVesselPilotPool;
    private JMenu menuVesselGunnerPool;
    private JMenu menuVesselCrewPool;

    /**
     * The Astech Pool menu uses the following Mnemonic keys as of 19-March-2020:
     * B, E, F, H
     * The Medic Pool menu uses the following Mnemonic keys as of 19-March-2020:
     * B, E, H, R
     */
    public TemporaryPersonnelManagementMenu(Campaign campaign, BiFunction<String, Integer, Integer> getUserIntInput) {
        super(getTextAt("menuTempPool.text"));
        this.campaign = campaign;
        this.getUserIntInput = getUserIntInput;


        JMenuItem miTempPoolFullStrength = createMenuItem("miTempPoolFullStrength.text", KeyEvent.VK_UNDEFINED,
              evt -> bringAllTempCrewsToFullStrength());
        add(miTempPoolFullStrength);

        JMenuItem miTempPoolReleaseAll = createMenuItem("miTempPoolReleaseAll.text", KeyEvent.VK_UNDEFINED,
              evt -> releaseAllTempCrews());
        add(miTempPoolReleaseAll);

        JMenuItem miTempPoolReleaseSurplus = createMenuItem("miTempPoolReleaseSurplus.text", KeyEvent.VK_UNDEFINED,
              evt -> releaseSurplusTempCrews());
        add(miTempPoolReleaseSurplus);

        addMenuListener(menuListenerFor(() -> {
            // For Astech/Medic: need + pool = what would be needed if there were no temp pool at all.
            // resetAsTechPool/resetMedicPool is a no-op only when pool == max(0, real need).
            int astechPool = getCampaign().getTemporaryAsTechPool();
            int astechIdealPool = Math.max(0, getCampaign().getAsTechNeed() + astechPool);
            int medicPool = getCampaign().getTemporaryMedicPool();
            int medicIdealPool = Math.max(0, getCampaign().getMedicsNeed() + medicPool);

            boolean anyNeed = (astechPool != astechIdealPool) || (medicPool != medicIdealPool) || anyBlobRoleHasNeed();
            setMenuItemState(miTempPoolFullStrength, anyNeed, getTextAt("miTempPoolFullStrength.disabledTip"));

            boolean anyPool = (astechPool > 0) || (medicPool > 0) || anyBlobRoleHasPool();
            setMenuItemState(miTempPoolReleaseAll, anyPool, getTextAt("miTempPoolReleaseAll.disabledTip"));

            boolean anySurplus =
                  (astechPool > astechIdealPool) || (medicPool > medicIdealPool) || anyBlobRoleHasSurplus();
            setMenuItemState(miTempPoolReleaseSurplus, anySurplus, getTextAt("miTempPoolReleaseSurplus.disabledTip"));
        }));

        addSeparator();

        JMenu menuAstechPool = buildSupportPoolSubMenu("menuAstechPool.text", KeyEvent.VK_A,
              "miHireAstechs.text", "popupHireAstechsNum.text", "miFireAstechs.text", "popupFireAstechsNum.text",
              "miFullStrengthAstechs.text", "miFireAllAstechs.text",
              "miFireAstechs.disabledTip", "miFullStrengthAstechs.disabledTip", "miFireAllAstechs.disabledTip",
              getCampaign()::getTemporaryAsTechPool, getCampaign()::getAsTechNeed,
              getCampaign()::increaseAsTechPool, getCampaign()::decreaseAsTechPool,
              getCampaign()::resetAsTechPool, getCampaign()::emptyAsTechPool
        );
        add(menuAstechPool);

        JMenu menuMedicPool = buildSupportPoolSubMenu("menuMedicPool.text", KeyEvent.VK_M,
              "miHireMedics.text", "popupHireMedicsNum.text", "miFireMedics.text", "popupFireMedicsNum.text",
              "miFullStrengthMedics.text", "miFireAllMedics.text",
              "miFireMedics.disabledTip", "miFullStrengthMedics.disabledTip", "miFireAllMedics.disabledTip",
              getCampaign()::getTemporaryMedicPool, getCampaign()::getMedicsNeed,
              getCampaign()::increaseMedicPool, getCampaign()::decreaseMedicPool,
              getCampaign()::resetMedicPool, getCampaign()::emptyMedicPool
        );
        add(menuMedicPool);

        // region Blob Crew Pools (Soldier, Battle Armor, Vehicle, Vessel)
        // Each pool follows the same 4-item structure; see buildBlobCrewPoolSubMenu.
        CampaignOptions opts = getCampaign().getCampaignOptions();

        menuSoldierPool = buildBlobCrewPoolSubMenu(this, PersonnelRole.SOLDIER,
              opts.isUseBlobInfantry(),
              "menuSoldierPool.text",
              "miHireSoldiers.text", "popupHireSoldiersNum.text",
              "miFireSoldiers.text", "popupFireSoldiersNum.text",
              "miFullStrengthSoldiers.text", "miFireAllSoldiers.text");

        menuBattleArmorPool = buildBlobCrewPoolSubMenu(this, PersonnelRole.BATTLE_ARMOUR,
              opts.isUseBlobBattleArmor(),
              "menuBattleArmorPool.text",
              "miHireBattleArmor.text", "popupHireBattleArmorNum.text",
              "miFireBattleArmor.text", "popupFireBattleArmorNum.text",
              "miFullStrengthBattleArmor.text", "miFireAllBattleArmor.text");

        menuVehicleCrewGroundPool = buildBlobCrewPoolSubMenu(this, PersonnelRole.VEHICLE_CREW_GROUND,
              opts.isUseBlobVehicleCrewGround(),
              "menuVehicleCrewGroundPool.text",
              "miHireVehicleCrewGround.text", "popupHireVehicleCrewGroundNum.text",
              "miFireVehicleCrewGround.text", "popupFireVehicleCrewGroundNum.text",
              "miFullStrengthVehicleCrewGround.text", "miFireAllVehicleCrewGround.text");

        menuVehicleCrewVTOLPool = buildBlobCrewPoolSubMenu(this, PersonnelRole.VEHICLE_CREW_VTOL,
              opts.isUseBlobVehicleCrewVTOL(),
              "menuVehicleCrewVTOLPool.text",
              "miHireVehicleCrewVTOL.text", "popupHireVehicleCrewVTOLNum.text",
              "miFireVehicleCrewVTOL.text", "popupFireVehicleCrewVTOLNum.text",
              "miFullStrengthVehicleCrewVTOL.text", "miFireAllVehicleCrewVTOL.text");

        menuVehicleCrewNavalPool = buildBlobCrewPoolSubMenu(this, PersonnelRole.VEHICLE_CREW_NAVAL,
              opts.isUseBlobVehicleCrewNaval(),
              "menuVehicleCrewNavalPool.text",
              "miHireVehicleCrewNaval.text", "popupHireVehicleCrewNavalNum.text",
              "miFireVehicleCrewNaval.text", "popupFireVehicleCrewNavalNum.text",
              "miFullStrengthVehicleCrewNaval.text", "miFireAllVehicleCrewNaval.text");

        menuVesselPilotPool = buildBlobCrewPoolSubMenu(this, PersonnelRole.VESSEL_PILOT,
              opts.isUseBlobVesselPilot(),
              "menuVesselPilotPool.text",
              "miHireVesselPilot.text", "popupHireVesselPilotNum.text",
              "miFireVesselPilot.text", "popupFireVesselPilotNum.text",
              "miFullStrengthVesselPilot.text", "miFireAllVesselPilot.text");

        menuVesselGunnerPool = buildBlobCrewPoolSubMenu(this, PersonnelRole.VESSEL_GUNNER,
              opts.isUseBlobVesselGunner(),
              "menuVesselGunnerPool.text",
              "miHireVesselGunner.text", "popupHireVesselGunnerNum.text",
              "miFireVesselGunner.text", "popupFireVesselGunnerNum.text",
              "miFullStrengthVesselGunner.text", "miFireAllVesselGunner.text");

        menuVesselCrewPool = buildBlobCrewPoolSubMenu(this, PersonnelRole.VESSEL_CREW,
              opts.isUseBlobVesselCrew(),
              "menuVesselCrewPool.text",
              "miHireVesselCrew.text", "popupHireVesselCrewNum.text",
              "miFireVesselCrew.text", "popupFireVesselCrewNum.text",
              "miFullStrengthVesselCrew.text", "miFireAllVesselCrew.text");
    }

    @Override
    public void addNotify() {
        super.addNotify();
        MekHQ.registerHandler(this);
    }

    @Override
    public void removeNotify() {
        MekHQ.unregisterHandler(this);
        super.removeNotify();
    }

    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * Builds a menu for managing temporary support personnel pools. Handles UI layout, quantity dialogs,
     * and dynamic enable/disable states.
     *
     * @param menuKey                    resource key for the menu title
     * @param mnemonic                   keyEvent integer for the menu mnemonic
     * @param hireKey                    resource key for the "Hire" item text
     * @param popupHireKey               resource key for the "Hire" dialog prompt
     * @param fireKey                    resource key for the "Fire" item text
     * @param popupFireKey               resource key for the "Fire" dialog prompt
     * @param fullStrengthKey            resource key for the "Full Strength" item text
     * @param fireAllKey                 resource key for the "Fire All" item text
     * @param fireDisabledTipKey         tooltip key when firing is disabled (pool is empty)
     * @param fullStrengthDisabledTipKey tooltip key when full strength is disabled (already met)
     * @param fireAllDisabledTipKey      tooltip key when firing all is disabled (pool is empty)
     * @param poolSupplier               returns the current temporary pool size
     * @param needSupplier               returns the current need for this role
     * @param increaseAction             adds the specified quantity to the pool
     * @param decreaseAction             removes the specified quantity from the pool
     * @param fullStrengthAction         resets the pool to match the need
     * @param emptyAction                removes all personnel from the pool
     *
     * @return a {@link JMenu} for temporary personnel management
     */
    private JMenu buildSupportPoolSubMenu(String menuKey, int mnemonic,
          String hireKey, String popupHireKey, String fireKey, String popupFireKey,
          String fullStrengthKey, String fireAllKey,
          String fireDisabledTipKey, String fullStrengthDisabledTipKey, String fireAllDisabledTipKey,
          Supplier<Integer> poolSupplier, Supplier<Integer> needSupplier,
          Consumer<Integer> increaseAction, Consumer<Integer> decreaseAction,
          Runnable fullStrengthAction, Runnable emptyAction) {

        JMenu menu = new JMenu(getTextAt(menuKey));
        menu.setMnemonic(mnemonic);

        JMenuItem miHire = createMenuItem(hireKey, KeyEvent.VK_H, evt -> {
            int value = getUserIntInput.apply(getTextAt(popupHireKey), MAX_QUANTITY_SPINNER);
            if (value >= 0) {
                increaseAction.accept(value);
            }
        });
        menu.add(miHire);

        JMenuItem miFire = createMenuItem(fireKey, KeyEvent.VK_E, evt -> {
            int value = getUserIntInput.apply(getTextAt(popupFireKey), poolSupplier.get());
            if (value >= 0) {
                decreaseAction.accept(value);
            }
        });
        menu.add(miFire);

        JMenuItem miFullStrength = createMenuItem(fullStrengthKey, KeyEvent.VK_B, evt -> fullStrengthAction.run());
        menu.add(miFullStrength);

        JMenuItem miFireAll = createMenuItem(fireAllKey, KeyEvent.VK_R, evt -> emptyAction.run());
        menu.add(miFireAll);

        menu.addMenuListener(menuListenerFor(() -> {
            int pool = poolSupplier.get();
            int need = needSupplier.get();
            // need + pool = what would be needed if there were no temp pool at all;
            // the action is a no-op only when pool == max(0, that real need)
            int idealPool = Math.max(0, need + pool);

            setMenuItemState(miFire, pool > 0, getTextAt(fireDisabledTipKey));
            setMenuItemState(miFullStrength, pool != idealPool, getTextAt(fullStrengthDisabledTipKey));
            setMenuItemState(miFireAll, pool > 0, getTextAt(fireAllDisabledTipKey));
        }));

        return menu;
    }

    private boolean anyBlobRoleHasNeed() {
        return BLOB_CREW_ROLES.stream().anyMatch(this::blobRoleHasUnitsNeedingCrew);
    }

    private boolean anyBlobRoleHasPool() {
        Campaign c = getCampaign();
        return BLOB_CREW_ROLES.stream().anyMatch(role -> c.isBlobCrewEnabled(role) && c.getTempCrewPool(role) > 0);
    }

    private boolean anyBlobRoleHasSurplus() {
        Campaign c = getCampaign();
        for (PersonnelRole role : BLOB_CREW_ROLES) {
            if (!c.isBlobCrewEnabled(role)) {
                continue;
            }
            if (c.getAvailableTempCrewPool(role) > 0) {
                return true;
            }
            for (Unit unit : c.getUnits()) {
                int tempCrew = unit.getTempCrewByPersonnelRole(role);
                if (tempCrew > 0) {
                    int excess = (unit.getActiveCrew().size() + tempCrew) - unit.getFullCrewSize();
                    if (excess > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean blobRoleHasUnitsNeedingCrew(PersonnelRole role) {
        Campaign c = getCampaign();
        if (!c.isBlobCrewEnabled(role)) {
            return false;
        }
        for (Unit unit : c.getUnits()) {
            boolean unitMatchesRole = (role == PersonnelRole.VESSEL_CREW)
                                            ? unit.canTakeMoreVesselCrew()
                                            : (role == unit.getDriverRole() || role == unit.getGunnerRole());
            if (!unitMatchesRole || unit.getActiveCrew().isEmpty()) {
                continue;
            }
            // Use total temp crew across all roles (mirrors distributeTempCrewPoolToUnits logic)
            // so a vessel already full of another role's temp crew is not counted as needing crew
            int current = unit.getActiveCrew().size() + unit.getTotalTempCrew();
            if (current < unit.getFullCrewSize()) {
                return true;
            }
        }
        return false;
    }

    private void updateBlobCrewPoolMenuItems(PersonnelRole role,
          JMenuItem fireItem, JMenuItem fullStrengthItem, JMenuItem fireAllItem) {
        int pool = getCampaign().getTempCrewPool(role);
        boolean unitsNeedCrew = blobRoleHasUnitsNeedingCrew(role);

        setMenuItemState(fireItem, pool > 0, getTextAt("miFireBlobCrew.disabledTip"));
        setMenuItemState(fullStrengthItem, unitsNeedCrew, getTextAt("miFullStrengthBlobCrew.disabledTip"));
        setMenuItemState(fireAllItem, pool > 0, getTextAt("miFireAllBlobCrew.disabledTip"));
    }

    private JMenu buildBlobCrewPoolSubMenu(JMenu parentMenu, PersonnelRole role, boolean isVisible,
          String menuKey, String hireKey, String hirePopupKey,
          String fireSomeKey, String fireSomePopupKey,
          String fullStrengthKey, String fireAllKey) {
        JMenu menu = new JMenu(getTextAt(menuKey));
        menu.setVisible(isVisible);

        JMenuItem miHire = createMenuItem(hireKey, KeyEvent.VK_UNDEFINED, evt -> {
            int value = getUserIntInput.apply(getTextAt(hirePopupKey), MAX_QUANTITY_SPINNER);
            if (value >= 0) {
                getCampaign().increaseTempCrewPool(role, value);
            }
        });
        menu.add(miHire);

        JMenuItem miFireSome = createMenuItem(fireSomeKey, KeyEvent.VK_UNDEFINED, evt -> {
            int value = getUserIntInput.apply(getTextAt(fireSomePopupKey), getCampaign().getTempCrewPool(role));
            if (value >= 0) {
                getCampaign().decreaseTempCrewPool(role, value);
            }
        });
        menu.add(miFireSome);

        JMenuItem miFullStrength = createMenuItem(fullStrengthKey, KeyEvent.VK_UNDEFINED, evt -> {
            getCampaign().resetTempCrewPoolForRole(role);
            getCampaign().distributeTempCrewPoolToUnits(role);
        });
        menu.add(miFullStrength);

        JMenuItem miFireAll = createMenuItem(fireAllKey, KeyEvent.VK_UNDEFINED,
              evt -> getCampaign().setTempCrewPool(role, 0));
        menu.add(miFireAll);

        menu.addMenuListener(menuListenerFor(
              () -> updateBlobCrewPoolMenuItems(role, miFireSome, miFullStrength, miFireAll)));

        parentMenu.add(menu);
        return menu;
    }

    private void bringAllTempCrewsToFullStrength() {
        getCampaign().resetAsTechPool();
        getCampaign().resetMedicPool();
        if (getCampaign().getCampaignOptions().isUseBlobInfantry()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.SOLDIER);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.SOLDIER);
        }
        if (getCampaign().getCampaignOptions().isUseBlobBattleArmor()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.BATTLE_ARMOUR);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.BATTLE_ARMOUR);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewGround()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VEHICLE_CREW_GROUND);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewVTOL()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VEHICLE_CREW_VTOL);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVehicleCrewNaval()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VEHICLE_CREW_NAVAL);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselPilot()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VESSEL_PILOT);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VESSEL_PILOT);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselGunner()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VESSEL_GUNNER);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VESSEL_GUNNER);
        }
        if (getCampaign().getCampaignOptions().isUseBlobVesselCrew()) {
            getCampaign().resetTempCrewPoolForRole(PersonnelRole.VESSEL_CREW);
            getCampaign().distributeTempCrewPoolToUnits(PersonnelRole.VESSEL_CREW);
        }
    }

    private void releaseAllTempCrews() {
        getCampaign().emptyAsTechPool();
        getCampaign().emptyMedicPool();
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        if (campaignOptions.isUseBlobInfantry()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.SOLDIER);
        }
        if (campaignOptions.isUseBlobBattleArmor()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.BATTLE_ARMOUR);
        }
        if (campaignOptions.isUseBlobVehicleCrewGround()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (campaignOptions.isUseBlobVehicleCrewVTOL()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (campaignOptions.isUseBlobVehicleCrewNaval()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (campaignOptions.isUseBlobVesselPilot()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_PILOT);
        }
        if (campaignOptions.isUseBlobVesselGunner()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_GUNNER);
        }
        if (campaignOptions.isUseBlobVesselCrew()) {
            getCampaign().clearBlobCrewForRole(PersonnelRole.VESSEL_CREW);
        }
    }

    private void releaseSurplusTempCrews() {
        getCampaign().releaseSurplusAsTechPool();
        getCampaign().releaseSurplusMedicPool();
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        if (campaignOptions.isUseBlobInfantry()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.SOLDIER);
        }
        if (campaignOptions.isUseBlobBattleArmor()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.BATTLE_ARMOUR);
        }
        if (campaignOptions.isUseBlobVehicleCrewGround()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_GROUND);
        }
        if (campaignOptions.isUseBlobVehicleCrewVTOL()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_VTOL);
        }
        if (campaignOptions.isUseBlobVehicleCrewNaval()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VEHICLE_CREW_NAVAL);
        }
        if (campaignOptions.isUseBlobVesselPilot()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VESSEL_PILOT);
        }
        if (campaignOptions.isUseBlobVesselGunner()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VESSEL_GUNNER);
        }
        if (campaignOptions.isUseBlobVesselCrew()) {
            getCampaign().releaseSurplusBlobCrewForRole(PersonnelRole.VESSEL_CREW);
        }
    }

    private static MenuListener menuListenerFor(Runnable onSelected) {
        return new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                onSelected.run();
            }

            @Override
            public void menuDeselected(MenuEvent e) {}

            @Override
            public void menuCanceled(MenuEvent e) {}
        };
    }

    private static void setMenuItemState(JMenuItem item, boolean enabled, String disabledTip) {
        item.setEnabled(enabled);
        item.setToolTipText(enabled ? null : disabledTip);
    }

    /**
     * Creates and configures a {@link JMenuItem} with localized text, an optional keyboard mnemonic, and an action
     * listener. Component's name is generated by removing {@code ".text"} suffix from {@code textKey} parameter.
     *
     * @param textKey  the resource key with the menu item's localized display text, must end with {@code ".text"}
     * @param mnemonic the keyboard shortcut or {@code KeyEvent.VK_UNDEFINED} if no mnemonic is required
     * @param action   the {@link ActionListener} to trigger when the menu item is selected
     *
     * @return the initialized {@code JMenuItem}
     */
    private static JMenuItem createMenuItem(String textKey, int mnemonic, ActionListener action) {
        if (!textKey.endsWith(MENU_ITEM_TEXT_SUFFIX)) {
            throw new IllegalArgumentException(String.format("Text key must end with '%s', actual: '%s'",
                  MENU_ITEM_TEXT_SUFFIX, textKey));
        }
        JMenuItem item = new JMenuItem(getTextAt(textKey));
        item.setName(textKey.substring(0, textKey.length() - MENU_ITEM_TEXT_SUFFIX.length()));
        if (mnemonic != KeyEvent.VK_UNDEFINED) {
            item.setMnemonic(mnemonic);
        }
        item.addActionListener(action);
        return item;
    }

    /**
     * Retrieves localized text from the panel's resource bundle.
     */
    private static String getTextAt(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }

    // ======================================
    // Event handlers for UI synchronization
    // ======================================

    /**
     * Processes changes in campaign options.
     *
     * <p>Updates the visibility and availability of UI tabs and menu items based on the new campaign settings.
     * Also triggers a refresh of all tabs and schedules updates for funds and parts availability.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param event the event containing the updated options
     */
    @Subscribe
    public void handle(final OptionsChangedEvent event) {
        // Update blob crew menu visibility based on campaign options
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        menuSoldierPool.setVisible(campaignOptions.isUseBlobInfantry());
        menuBattleArmorPool.setVisible(campaignOptions.isUseBlobBattleArmor());
        menuVehicleCrewGroundPool.setVisible(campaignOptions.isUseBlobVehicleCrewGround());
        menuVehicleCrewVTOLPool.setVisible(campaignOptions.isUseBlobVehicleCrewVTOL());
        menuVehicleCrewNavalPool.setVisible(campaignOptions.isUseBlobVehicleCrewNaval());
        menuVesselPilotPool.setVisible(campaignOptions.isUseBlobVesselPilot());
        menuVesselGunnerPool.setVisible(campaignOptions.isUseBlobVesselGunner());
        menuVesselCrewPool.setVisible(campaignOptions.isUseBlobVesselCrew());
    }

}
