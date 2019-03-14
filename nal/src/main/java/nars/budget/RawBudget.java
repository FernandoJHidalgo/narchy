package nars.budget;

import org.jetbrains.annotations.NotNull;

/**
 * Contains only the 3 p,d,q as floats.  For general purpose usage, you probably want to use UnitBudget
 * because this includes timestamp.
 */
public class RawBudget implements Budget {

    /**
     * The relative share of time resource to be allocated
     */
    protected float priority;

    /**
     * The percent of priority to be kept in a constant period; All priority
     * values "decay" over time, though at different rates. Each item is given a
     * "durability" factor in (0, 1) to specify the percentage of priority level
     * left after each reevaluation
     */
    protected float durability;

    /**
     * The overall (context-independent) evaluation
     */
    protected float quality;

    public RawBudget() {

    }

    public RawBudget(@NotNull Budgeted b, float scale) {
        this(b.pri()*scale, b.dur(), b.qua());
    }

    public RawBudget(float p, float d, float q) {
        setPriority(p);
        setDurability(d);
        setQuality(q);
    }

    @NotNull
    @Override
    public Budget clone() {
        return new RawBudget(pri(), dur(), qua());
    }

    /**
     * Get priority value
     *
     * @return The current priority
     */
    @Override
    public final float pri() {
        return priority;
    }

    @Override
    public boolean isDeleted() {
        float p = priority;
        return p!=p; //fast NaN check
    }

    /**
     * Get durability value
     *
     * @return The current durability
     */
    @Override
    public final float dur() {
        return durability;
    }



    /**
     * Get quality value
     *
     * @return The current quality
     */
    @Override
    public final float qua() {
        return quality;
    }


    @Override
    public boolean delete() {
        float p = priority;
        if (p==p) {
        //if (!isDeleted()) { //dont call isDeleted it may be overridden in a cyclical way
            this.priority = Float.NaN;
            return true;
        }
//            throw new RuntimeException("Already Deleted");
        return false;
    }




    public boolean equals(Object that) {
        throw new RuntimeException("N/A");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("N/A");
     }

    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    @NotNull
    @Override
    public String toString() {
        return getBudgetString();
    }

    @Override
    public void _setPriority(float p) {
        this.priority = p;
    }


    /**
     * Change durability value
     *
     * @param d The new durability
     */
    @Override
    public void _setDurability(float d) {
        this.durability = d;
    }


    /**
     * Change quality value
     *
     * @param q The new quality
     */
    @Override
    public void _setQuality(float q) {
        this.quality = q;
    }

}