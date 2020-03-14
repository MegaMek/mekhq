package mekhq.gui.dialog;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.Entity;
import megamek.common.TechConstants;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class MekHQUnitSelectorDialog extends AbstractUnitSelectorDialog {
    //region Variable Declarations
    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitSelectorDialog",
            new EncodeControl());
    private Campaign campaign;
    private boolean addToCampaign;
    //endregion Variable Declarations

    public MekHQUnitSelectorDialog(JFrame frame, UnitLoadingDialog unitLoadingDialog, Campaign campaign, boolean addToCampaign) {
        super(frame, unitLoadingDialog);
        this.campaign = campaign;
        this.addToCampaign = addToCampaign;

        allowedYear = client.getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        canonOnly = client.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_CANON_ONLY);
        gameTechLevel = TechConstants.getSimpleLevel(client.getGame().getOptions()
                .stringOption("techlevel"));

        initialize();
    }

    @Override
    protected void initialize() {
        super.initialize();
        setUserPreferences();
    }

    //region Button Methods
    @Override
    protected JPanel createButtonsPanel() {
        JPanel panelButtons = new JPanel(new GridBagLayout());

        //
        buttonSelect = new JButton(Messages.getString("MechSelectorDialog.m_bPick"));
        buttonSelect.addActionListener(this);
        panelButtons.add(buttonSelect, new GridBagConstraints());

        buttonSelectClose = new JButton(Messages.getString("MechSelectorDialog.m_bPickClose"));
        buttonSelectClose.addActionListener(this);
        panelButtons.add(buttonSelectClose, new GridBagConstraints());

        buttonClose = new JButton(Messages.getString("Close"));
        buttonClose.addActionListener(this);
        panelButtons.add(buttonClose, new GridBagConstraints());

        buttonShowBV = new JButton(Messages.getString("MechSelectorDialog.BV"));
        buttonShowBV.addActionListener(this);
        panelButtons.add(buttonShowBV, new GridBagConstraints());

        return panelButtons;
    }

    @Override
    protected void select(boolean close) {

    }
    //endregion Button Methods

    @Override
    protected Entity refreshUnitView() {
        return super.refreshUnitView();
    }
}
