package mekhq.gui.dialog;

import mekhq.campaign.mission.Scenario;

import javax.swing.*;
import java.awt.*;

public class EditMapSettingsDialog extends JDialog {

    private JFrame frame;
    private boolean wasCancelled;

    private int mapSizeX;
    private int mapSizeY;
    private String map;
    private boolean usingFixedMap;
    private int boardType;

    private JCheckBox checkFixed;
    private JComboBox<String> comboBoardType;
    private JSpinner spnMapX;
    private JSpinner spnMapY;

    JPanel panSizeRandom;

    public EditMapSettingsDialog(JFrame parent, boolean modal, int boardType, boolean usingFixedMap, String map,
                                 int mapSizeX, int mapSizeY) {

        super(parent, modal);
        this.boardType = boardType;
        this.usingFixedMap = usingFixedMap;
        this.map = map;
        this.mapSizeX = mapSizeX;
        this.mapSizeY = mapSizeY;
        wasCancelled = true;

        initComponents();
        setLocationRelativeTo(parent);
        pack();
    }

    public int getBoardType() {
        return boardType;
    }

    public boolean getUsingFixedMap() {
        return usingFixedMap;
    }

    public String getMap() {
        return map;
    }

    public int getMapSizeX() {
        return mapSizeX;
    }

    public int getMapSizeY() {
        return mapSizeY;
    }

    private void initComponents() {

        getContentPane().setLayout(new BorderLayout());
        JPanel panMain = new JPanel(new GridBagLayout());
        panSizeRandom = new JPanel(new GridBagLayout());
        JPanel panButtons = new JPanel(new GridLayout(0, 2));

        checkFixed = new JCheckBox("Use fixed map");
        checkFixed.setSelected(usingFixedMap);

        comboBoardType = new JComboBox();
        for (int i = Scenario.T_GROUND; i <= Scenario.T_SPACE; i++) {
            comboBoardType.addItem(Scenario.getBoardTypeName(i));
        }
        comboBoardType.setSelectedIndex(boardType);

        spnMapX = new JSpinner(new SpinnerNumberModel(mapSizeX, 0, null, 1));
        spnMapY = new JSpinner(new SpinnerNumberModel(mapSizeY, 0, null, 1));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        panSizeRandom.add(spnMapX, gbc);
        gbc.gridx++;
        panSizeRandom.add(new JLabel("x"));
        gbc.gridx++;
        gbc.weightx = 1.0;
        panSizeRandom.add(spnMapY);

       gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel("Board Type:"));
        gbc.weightx = 1.0;
        gbc.gridx++;
        panMain.add(comboBoardType, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panMain.add(new JLabel("Map Size:"), gbc);
        gbc.gridx++;
        panMain.add(panSizeRandom, gbc);

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy++;
        panMain.add(checkFixed, gbc);

        JButton btnOK = new JButton("Done");
        btnOK.addActionListener(evt -> done());
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(evt -> cancel());
        panButtons.add(btnOK);
        panButtons.add(btnCancel);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);
    }

    public void done() {
        boardType = comboBoardType.getSelectedIndex();
        usingFixedMap = checkFixed.isSelected();
        if(usingFixedMap) {

        } else {
            mapSizeX = (int) spnMapX.getValue();
            mapSizeY = (int) spnMapY.getValue();
        }
        setVisible(false);
    }

    public void cancel() {
        setVisible(false);
    }

}
