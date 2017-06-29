package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.work.IAcquisitionWork;

public class PartsInUseTableModel extends DataTableModel {
    private static final long serialVersionUID = -7166100476703184175L;
    
    private static final DecimalFormat FORMATTER = new DecimalFormat();
    static {
        FORMATTER.setMaximumFractionDigits(3);
    }
    private static final String EMPTY_CELL = ""; //$NON-NLS-1$

    public final static int COL_PART = 0;
    public final static int COL_IN_USE = 1;
    public final static int COL_STORED = 2;
    public final static int COL_TONNAGE = 3;
    public final static int COL_IN_TRANSFER  = 4;
    public final static int COL_COST = 5;
    public final static int COL_PURCHASEABLE = 6;
    public final static int COL_MAX_INDEX = 6;

    private ResourceBundle resourceMap;
    private Campaign campaign;
    
    public PartsInUseTableModel (Campaign campaign) {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PartsInUseTableModel", new EncodeControl()); //$NON-NLS-1$
        data = new ArrayList<PartInUse>();
        this.campaign = campaign;
    }
    
    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COL_MAX_INDEX + 1;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
        case COL_PART:
            return resourceMap.getString("part.heading"); //$NON-NLS-1$
        case COL_IN_USE:
            return resourceMap.getString("inUse.heading"); //$NON-NLS-1$
        case COL_STORED:
            return resourceMap.getString("stored.heading"); //$NON-NLS-1$
        case COL_TONNAGE:
            return resourceMap.getString("storedTonnage.heading"); //$NON-NLS-1$
        case COL_IN_TRANSFER:
            return resourceMap.getString("ordered.heading"); //$NON-NLS-1$
        case COL_COST:
            return resourceMap.getString("cost.heading"); //$NON-NLS-1$
        case COL_PURCHASEABLE:
            return "Purchasable"; //$NON-NLS-1$
        default:
            return EMPTY_CELL;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        PartInUse piu = getPartInUse(row);
        switch(column) {
            case COL_PART:
                return piu.getDescription();
            case COL_IN_USE:
                return FORMATTER.format(piu.getUseCount());
            case COL_STORED:
                return (piu.getStoreCount() > 0) ? FORMATTER.format(piu.getStoreCount()) : EMPTY_CELL;
            case COL_TONNAGE:
                return (piu.getStoreTonnage() > 0) ? FORMATTER.format(piu.getStoreTonnage()) : EMPTY_CELL;
            case COL_IN_TRANSFER:
                if( piu.getTransferCount() > 0 && piu.getPlannedCount() <= 0 ) {
                    return FORMATTER.format(piu.getTransferCount());
                } else if( piu.getPlannedCount() > 0 ) {
                    return String.format("%s [+%s]", //$NON-NLS-1$
                        FORMATTER.format(piu.getTransferCount()), FORMATTER.format(piu.getPlannedCount()));
                } else {
                    return EMPTY_CELL;
                }
            case COL_COST:
                return FORMATTER.format(piu.getCost());
            case COL_PURCHASEABLE:
                return campaign.canAcquireEquipment(piu.getPartToBuy(), false) ? "Yes" : "No";
            default:
                return EMPTY_CELL;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
    	return true;
    }
    
    public void setData(Set<PartInUse> data) {
        setData(new ArrayList<PartInUse>(data));
    }
    
    @SuppressWarnings("unchecked")
    public void updateRow(int row, PartInUse piu) {
        ((ArrayList<PartInUse>) data).set(row, piu);
        fireTableRowsUpdated(row, row);
    }

    public PartInUse getPartInUse(int row) {
        if((row < 0) || (row >= data.size())) {
            return null;
        }
        return (PartInUse) data.get(row);
    }
    
    public boolean isBuyable(int row) {
        return (row >= 0) && (row < data.size())
            && (null != ((PartInUse) data.get(row)).getPartToBuy());
    }

    public int getAlignment(int column) {
        switch(column) {
            case COL_PART:
                return SwingConstants.LEFT;
            case COL_IN_USE:
            case COL_STORED:
            case COL_TONNAGE:
            case COL_IN_TRANSFER:
            case COL_COST:
                return SwingConstants.RIGHT;
            default:
                return SwingConstants.CENTER;
        }
    }
    
    public int getPreferredWidth(int column) {
        switch(column) {
            case COL_PART:
                return 300;
            case COL_IN_USE:
            case COL_STORED:
            case COL_TONNAGE:
            case COL_IN_TRANSFER:
            case COL_COST:
                return 20;
            default:
                return 100;
        }
    }
    
    public boolean hasConstantWidth(int col) {
    	return false;
    }
    
    public int getWidth(int col) {
    	return Integer.MAX_VALUE;
    }
    
    public PartsInUseTableModel.Renderer getRenderer() {
        return new PartsInUseTableModel.Renderer();
    }

    public static class Renderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1403740113670268591L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setHorizontalAlignment(((PartsInUseTableModel)table.getModel()).getAlignment(column));

            setForeground(Color.BLACK);
            if (isSelected) {
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
            } else {
                // tiger stripes
                if (row % 2 == 1) {
                    setBackground(new Color(230,230,230));
                } else {
                    setBackground(Color.WHITE);
                }
            }
            return this;
        }
    }
    
    private void buyOrAddPart(IAcquisitionWork partToBuy, int quantity, boolean isBuy) {
    	if (isBuy && !campaign.canAcquireEquipment(partToBuy, false)) {
    		return;
    	}
    	
    	if (quantity <= 0) {
    		return;
    	}
    	
    	//campaign.addReport(String.format("%s %s for %s", isBuy ? "Buying" : "Adding", quantity, piu.getDescription()));
    	
    	if (isBuy) {
    		campaign.getShoppingList().addShoppingItem(partToBuy, quantity, campaign);
    	} else {
            while (quantity > 0) {
                campaign.addPart((Part) partToBuy.getNewEquipment(), 0);
                --quantity;
            }
    	}
    }

    public void buyOrAddPartByExactAmount(int rowIndex, int quantity, boolean isBuy, boolean isSingleAction) {
    	if (quantity <= 0) {
    		return;
    	}
    	
        PartInUse piu = getPartInUse(rowIndex);
        IAcquisitionWork partToBuy = piu.getPartToBuy();
    	
        if (!isSingleAction) {
        	quantity = convertToPurchaseQuantity(piu, quantity);
        }
        
    	if (isBuy) {
    		campaign.getShoppingList().addShoppingItem(partToBuy, quantity, campaign);
    	} else {
            while (quantity > 0) {
                campaign.addPart((Part) partToBuy.getNewEquipment(), 0);
                --quantity;
            }
    	}
    }

	public void buyOrAddPartByMinimum(int rowIndex, int minimumQuantity, boolean isBuy) {
        PartInUse piu = getPartInUse(rowIndex);

		int inStorageAndTransit = convertToTonnage(piu, getStoredAndTransferAmount(piu));
        
        int amountToBuy = convertToPurchaseQuantity(piu, minimumQuantity - inStorageAndTransit);
		
		if (amountToBuy <= 0) {
			return;
		}

        buyOrAddPart(piu.getPartToBuy(), amountToBuy, isBuy);
	}

	public void buyOrAddPartByPercentage(int rowIndex, int percentage, boolean isBuy) {
        PartInUse piu = getPartInUse(rowIndex);

        int inUse = piu.getUseCount();
		int inStorageAndTransit = convertToTonnage(piu, getStoredAndTransferAmount(piu));
		
		int targetAmount = (int)Math.round((double)inUse * (double)percentage / 100d);
        int amountToBuy = convertToPurchaseQuantity(piu, targetAmount - inStorageAndTransit);
		
		if (amountToBuy <= 0) {
			return;
		}

        buyOrAddPart(piu.getPartToBuy(), amountToBuy, isBuy);
	}

	private int convertToTonnage(PartInUse piu, int quantity) {
		if (quantity <= 0) {
			return 0;
		}

        IAcquisitionWork partToBuy = piu.getPartToBuy();
        Part part = partToBuy.getAcquisitionPart();
        
		if (part instanceof Armor) {
			Armor a = (Armor)part;
			double pointsPerTon = a.getArmorPointsPerTon();
			quantity = (int)Math.round((double)quantity / pointsPerTon);
		} if (part instanceof AmmoStorage) {
			AmmoStorage a = (AmmoStorage)part;
			int shots = a.getShots();
			
			quantity = (int)Math.round((double)quantity / (double)shots);
		}
		
		return quantity;
	}

	private int convertToPurchaseQuantity(PartInUse piu, int quantity) {
        IAcquisitionWork partToBuy = piu.getPartToBuy();
        Part part = partToBuy.getAcquisitionPart();
        
		if (part instanceof Armor) {
			Armor a = (Armor)part;
			quantity = (int)Math.round(((double)quantity / a.getTonnage()));
		}
		
		return quantity;
	}
	
	private int getStoredAndTransferAmount(PartInUse piu) {
		int amt = (piu.getStoreCount() > 0) ? piu.getStoreCount() : 0;
		
		if( piu.getTransferCount() > 0 && piu.getPlannedCount() <= 0 ) {
			amt += piu.getTransferCount();					
		} else if (piu.getPlannedCount() > 0) {
			amt += piu.getPlannedCount();	
		}
		
		return amt;
	}
}
