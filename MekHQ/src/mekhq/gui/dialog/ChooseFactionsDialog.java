package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;

import org.joda.time.DateTime;

import megamek.common.util.EncodeControl;
import mekhq.campaign.universe.Era;
import mekhq.campaign.universe.Faction;

public class ChooseFactionsDialog extends JDialog {
    private static final long serialVersionUID = 805616085217507489L;
    
    private DateTime date;
    
    ResourceBundle resourceMap;
    private JList<Faction> factionList;
    private List<String> result;
    private boolean changed;
    
    public ChooseFactionsDialog(Frame parent, DateTime date, List<String> defaults) {
        this(parent, date, defaults, true);
    }
    
    public ChooseFactionsDialog(Frame parent, DateTime date, List<String> defaults, boolean modal) {
        super(parent, modal);
        this.date = Objects.requireNonNull(date);
        this.result = defaults;
        this.changed = false;
        initComponents();
        setLocationRelativeTo(parent);
    }

    protected void initComponents() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.ChooseFactionsDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("form"); //$NON-NLS-1$
        setTitle(resourceMap.getString("Form.title")); //$NON-NLS-1$
        setMinimumSize(new Dimension(400, 500));
        
        final Container content = getContentPane();
        content.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane scrollPane = new JScrollPane();
        factionList = new JList<>(new FactionListModel(date));
        factionList.setCellRenderer(new DefaultListCellRenderer(){
            private static final long serialVersionUID = -2504011562223561964L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                DefaultListCellRenderer result = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if(value instanceof Faction) {
                    result.setText(((Faction)value).getFullName(Era.getEra(date.getYear())));
                }
                return result;
            }
            
        });
        scrollPane.setViewportView(factionList);
        content.add(scrollPane, gbc);
        
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        content.add(new JButton(new AbstractAction(resourceMap.getString("ok.label")) { //$NON-NLS-1$
            private static final long serialVersionUID = -8920630119126015954L;

            @Override
            public void actionPerformed(ActionEvent e) {
                result = new ArrayList<>();
                for(Faction faction : factionList.getSelectedValuesList()) {
                    result.add(faction.getShortName());
                }
                changed = true;
                setVisible(false);
            }
        }), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        content.add(new JButton(new AbstractAction(resourceMap.getString("cancel.label")){ //$NON-NLS-1$
            private static final long serialVersionUID = -8920630119126015955L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }), gbc);
        pack();
    }
    
    public List<String> getResult() {
        return result;
    }
    
    public boolean isChanged() {
        return changed;
    }

    private static class FactionListModel extends AbstractListModel<Faction> {
        private static final long serialVersionUID = 2779479232585980171L;
        
        private TreeMap<String, Faction> factionMap = new TreeMap<>();
        private List<String> names;
        
        public FactionListModel(DateTime date) {
            int era = Era.getEra(date.getYear());
            for(Faction faction : Faction.factions.values()) {
                factionMap.put(faction.getFullName(era), faction);
            }
            names = new ArrayList<>(factionMap.navigableKeySet());
        }
        
        @Override
        public int getSize() {
            return names.size();
        }

        @Override
        public Faction getElementAt(int index) {
            return factionMap.get(names.get(index));
        }
    }
}
