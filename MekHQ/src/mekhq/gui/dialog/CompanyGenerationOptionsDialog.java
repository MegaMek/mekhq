/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.Container;
import javax.swing.JFrame;

import megamek.client.ui.enums.ValidationState;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.panels.CompanyGenerationOptionsPanel;

/**
 * @author Justin "Windchild" Bowen
 */
public class CompanyGenerationOptionsDialog extends AbstractMHQValidationButtonDialog {
    //region Variable Declarations
    private final Campaign campaign;
    private final CompanyGenerationOptions companyGenerationOptions;
    private CompanyGenerationOptionsPanel companyGenerationOptionsPanel;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationOptionsDialog(final JFrame frame, final Campaign campaign,
          final @Nullable CompanyGenerationOptions companyGenerationOptions) {
        super(frame, "CompanyGenerationOptionsDialog", "CompanyGenerationOptionsDialog.title");
        this.campaign = campaign;
        this.companyGenerationOptions = companyGenerationOptions;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public @Nullable CompanyGenerationOptions getCompanyGenerationOptions() {
        return companyGenerationOptions;
    }

    public CompanyGenerationOptionsPanel getCompanyGenerationOptionsPanel() {
        return companyGenerationOptionsPanel;
    }

    public void setCompanyGenerationOptionsPanel(final CompanyGenerationOptionsPanel companyGenerationOptionsPanel) {
        this.companyGenerationOptionsPanel = companyGenerationOptionsPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setCompanyGenerationOptionsPanel(new CompanyGenerationOptionsPanel(getFrame(), getCampaign(),
              getCompanyGenerationOptions()));
        return getCompanyGenerationOptionsPanel();
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected ValidationState validateAction(final boolean display) {
        return getCompanyGenerationOptionsPanel().validateOptions(display);
    }
    //endregion Button Actions

    public @Nullable CompanyGenerationOptions getSelectedItem() {
        return getResult().isConfirmed() ? getCompanyGenerationOptionsPanel().createOptionsFromPanel()
                     : getCompanyGenerationOptions();
    }
}
