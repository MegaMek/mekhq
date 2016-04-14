package mekhq.campaign.work;

import mekhq.campaign.inventory.Inventory;
import mekhq.campaign.unit.Unit;

/**
 * Conceptually, a work unit is a simple piece of work: Given a {@link Unit} and an {@link Inventory},
 * modify them if possible, keeping the amount of items/parts in both the same. No other state 
 * is allowed to be modified, aside of outputting log messages.
 * <p>
 * Subclasses implement the canWork() and doWork() methods. Classes using it call the work() method only.
 */
public abstract class WorkUnit {
    /**
     * @return <code>true</code> if the work can be done given the unit and inventory,
     *         <code>false</code> otherwise
     */
    protected abstract boolean canWork(Unit unit, Inventory inv);
    /**
     * Do the actual work, transforming the unit and entity
     * 
     * @return the base amount of time spend working, in hours
     * @throws WorkException
     */
    protected abstract int doWork(Unit unit, Inventory inv) throws WorkException;
    
    /**
     * See if this work unit is applicable to the supplied arguments, and do the work if it is.
     * <p>
     * Both <code>unit</code> and <code>inv</code> can be <code>null</code>, for work units which
     * don't need them.
     * 
     * @return the base amount of time spend working, in hours
     * @throws WorkException
     */
    public final int work(Unit unit, Inventory inv) throws WorkException {
        if(!canWork(unit, inv)) {
            throw new WorkException(unit, inv, "Work unit not applicable to the specified data"); //$NON-NLS-1$
        }
        return doWork(unit, inv);
    }
    
    /**
     * An exception indicating some form of incompatibility of the work unit with the supplied
     * unit or inventory, like missing parts.
     */
    public static class WorkException extends Exception {
        private static final long serialVersionUID = -3801134506458553823L;
        
        private final Unit unit;
        private final Inventory inv;
        
        public WorkException(Unit unit, Inventory inv, String message) {
            super(message);
            this.unit = unit;
            this.inv = inv;
        }
        
        @Override
        public String getMessage() {
            StringBuffer sb = new StringBuffer();
            sb.append("[UNIT:").append(unit).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append("[INVENTORY:").append(inv).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(super.getMessage());
            return sb.toString();
        }
    }
}
