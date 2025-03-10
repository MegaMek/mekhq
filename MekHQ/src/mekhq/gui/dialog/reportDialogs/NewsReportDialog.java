/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.gui.dialog.reportDialogs;

import mekhq.campaign.universe.NewsItem;

import javax.swing.*;

public class NewsReportDialog extends AbstractReportDialog {
    //region Variable Declarations
    private final NewsItem news;
    //endregion Variable Declarations

    //region Constructors
    public NewsReportDialog(final JFrame frame, final NewsItem news) {
        super(frame, "NewsReportDialog", "NewsReportDialog.title");
        this.news = news;
        setTitle(news.getHeadline());
        initialize();
    }
    //endregion Constructors

    //region Getters
    public NewsItem getNews() {
        return news;
    }

    @Override
    protected JTextPane createTxtReport() {
        final JTextPane txtReport = new JTextPane();
        txtReport.setContentType("text/html");
        txtReport.setText(getNews().getFullDescription());
        txtReport.setName("txtReport");
        txtReport.setEditable(false);
        txtReport.setCaretPosition(0);
        return txtReport;
    }
    //endregion Getters
}
