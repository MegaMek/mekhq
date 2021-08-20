/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.reportDialogs;

import mekhq.gui.baseComponents.AbstractMHQDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * This displays a standard report for MekHQ.
 *
 * Inheriting classes must call initialize() in their constructors and override getText.
 */
public abstract class AbstractReportDialog extends AbstractMHQDialog {
    //region Constructors
    protected AbstractReportDialog(final JFrame frame, final String name, final String title) {
        super(frame, name, title);
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected Container createCenterPane() {
        final JScrollPane scrollPane = new JScrollPane(createTxtReport());
        scrollPane.setBorder(new EmptyBorder(2, 10, 2, 2));
        scrollPane.setName("reportPane");

        return scrollPane;
    }

    protected abstract JTextPane createTxtReport();
    //endregion Initialization
}
