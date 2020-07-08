/*
 * Copyright (c) 2013 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.NewsItem;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * @author Jay Lawson
 */
public class NewsReportDialog extends JDialog {
    private static final long serialVersionUID = 3624327778807359294L;

    private JTextPane txtNews;

    public NewsReportDialog(JFrame parent, NewsItem news, Campaign campaign) {
        super(parent, false);
        setTitle(news.getHeadline());
        initComponents();
        txtNews.setText(news.getFullDescription(campaign));
        txtNews.setCaretPosition(0);
        setMinimumSize(new Dimension(500, 300));
        setPreferredSize(new Dimension(500, 300));
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        txtNews = new JTextPane();
        txtNews.setContentType("text/html");
        txtNews.setEditable(false);
        JScrollPane scrNews = new JScrollPane(txtNews);
        scrNews.setBorder( new EmptyBorder(2,10,2,2));

        setLayout(new BorderLayout());

        txtNews.setEditable(false);

        getContentPane().add(scrNews, BorderLayout.CENTER);
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(NewsReportDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }
}
