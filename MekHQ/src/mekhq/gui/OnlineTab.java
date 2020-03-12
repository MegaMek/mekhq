/*
 * Copyright (c) 2020 The MegaMek Team.
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.CampaignController;
import mekhq.campaign.RemoteCampaign;
import mekhq.gui.model.OnlineCampaignsTableModel;
import mekhq.online.events.CampaignListUpdatedEvent;
import mekhq.preferences.PreferencesNode;

public final class OnlineTab extends CampaignGuiTab implements ActionListener {

    private static final long serialVersionUID = 4133071018441878778L;

    private ResourceBundle resourceMap;

    private OnlineCampaignsTableModel hostCampaignTableModel;
    private JTable hostCampaignTable;
    private OnlineCampaignsTableModel campaignsTableModel;
    private JTable campaignsTable;

    OnlineTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
        setUserPreferences();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(OnlineTab.class);

        // TODO: manage preferences
    }

    private CampaignController getCampaignController() {
        return getCampaignGui().getCampaignController();
    }

	@Override
    public void initTab() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$
                new EncodeControl());

        GridBagConstraints gbc;

        setName("panelOnline"); //$NON-NLS-1$
        setLayout(new GridLayout(0, 1));

        hostCampaignTableModel = new OnlineCampaignsTableModel(getHostCampaignAsList());
        hostCampaignTable = new JTable(hostCampaignTableModel);

        JScrollPane hostCampaignScrollPane = new JScrollPane(hostCampaignTable);
        JPanel hostCampaignTablePanel = new JPanel(new GridLayout(0, 1));
        hostCampaignTablePanel.setBorder(BorderFactory.createTitledBorder("Host Campaign"));
        hostCampaignTablePanel.add(hostCampaignScrollPane);
        hostCampaignTablePanel.setMinimumSize(new Dimension(400, 120));

        campaignsTableModel = new OnlineCampaignsTableModel(getCampaignController().getRemoteCampaigns());
        campaignsTable = new JTable(campaignsTableModel);

        JScrollPane campaignsTableScrollPane = new JScrollPane(campaignsTable);
        JPanel campaignsTablePanel = new JPanel(new GridLayout(0, 1));
        campaignsTablePanel.setBorder(BorderFactory.createTitledBorder("Remote Campaigns"));
        campaignsTablePanel.add(campaignsTableScrollPane);
        campaignsTablePanel.setMinimumSize(new Dimension(400, 300));

        JPanel topPanel = new JPanel(new GridBagLayout());
        if (!getCampaignController().isHost()) {
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            topPanel.add(hostCampaignTablePanel, gbc);
        }

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        topPanel.add(campaignsTablePanel, gbc);

        JPanel campaignDetailsPanel = new JPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel,
            campaignDetailsPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);

        add(splitPane);
    }

    @Override
    public void refreshAll() {
        hostCampaignTableModel.setData(getHostCampaignAsList());
        campaignsTableModel.setData(new ArrayList<>(getCampaignController().getRemoteCampaigns()));
    }

    private List<RemoteCampaign> getHostCampaignAsList() {
        List<RemoteCampaign> list = new ArrayList<>();

        list.add(new RemoteCampaign(getCampaignController().getHost(),
            getCampaignController().getHostName(), getCampaignController().getHostDate(),
            getCampaignController().getHostLocation(), getCampaignController().getHostIsGMMode(),
            true/*isActive*/));

        return list;
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.ONLINE;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

    }

    @Subscribe
    public void handle(CampaignListUpdatedEvent e) {
        refreshAll();
    }
}
