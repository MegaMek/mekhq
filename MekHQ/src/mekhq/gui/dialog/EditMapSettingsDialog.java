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
        JPanel panMain = new JPanel(new GridLayout(2, 0));
        JPanel panButtons = new JPanel(new GridLayout(0, 2));

        checkFixed = new JCheckBox("Use fixed map");
        checkFixed.setSelected(usingFixedMap);

        comboBoardType = new JComboBox();
        for (int i = Scenario.T_GROUND; i <= Scenario.T_SPACE; i++) {
            comboBoardType.addItem(Scenario.getBoardTypeName(i));
        }
        comboBoardType.setSelectedIndex(boardType);

        panMain.add(checkFixed);
        panMain.add(comboBoardType);

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
        setVisible(false);
    }

    public void cancel() {
        setVisible(false);
    }

}
