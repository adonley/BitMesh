package network.bitmesh.Units;

import org.bitcoinj.core.Coin;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public abstract class PurchaseUnit
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PurchaseUnit.class);

    // TODO: Maybe longs aren't always the best number to measure with
    protected Long size;
    protected Coin price;
    protected LinkedList<Unit> measurementUnits;
    protected static long DEFAULT_PRICE_PER_UNIT = 100;

    public Long getSize() { return size; }
    public Coin getPrice() { return price; }
    public String getType() { return this.getClass().getName(); }

    public void setSize(Long size) { this.size = size; }
    public void setPrice(Coin price) { this.price = price; }
    public void setPrice(Long size, Coin price) { this.size = size; this.price = price; }

    public PurchaseUnit() { }

    public PurchaseUnit(Long size)
    {
        this.size = size;
    }

    public PurchaseUnit(Long size, Coin price)
    {
        this(size);
        this.price = price;
    }

    protected void sortMeasurements()
    {
        Collections.sort(measurementUnits, new Comparator<Unit>()
        {
            @Override
            public int compare(Unit o1, Unit o2)
            {
                return o1.compareTo(o2);
            }
        });
    }

    /**
     * Gets the name closest associated with the current unit
     * @return name of closest unit
     */
    public String getCanonicalUnitName()
    {
        String retName = null;

        // Expecting the linked list to be sorted
        for(Unit unit : measurementUnits)
            if(unit.getValue() <= size)
                retName = unit.getName();
            else
                break;

        return retName;
    }

    /**
     * Gets the price of the closest unit less than the price. Probably
     * should do something different for the UI, but this will help for
     * debugging purposes.
     * @return name of the unit.size <= price
     */
    public String getCanonicalUnitPrice()
    {
        String retName = null;

        // Expecting the linked list to be sorted
        for(Unit unit : measurementUnits)
            if(unit.getValue() <= size)
                retName = size + " " + unit.getName() + " per " + price.getValue() + " satoshi";
            else
                break;

        return retName;
    }

    /**
     * Base class for a unit division in a PurchaseUnit
     */
    protected class Unit implements Comparable<Unit>
    {
        protected String name;
        protected Long value;

        public Unit(String name, Long value)
        {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public Long getValue() { return value; }

        @Override
        public int compareTo(Unit o)
        {
            if(o.value > this.value)
                return -1;
            else if(o.value < value)
                return 1;
            else
                return 0;
        }
    }

    // TODO: Should base unit be the smallest size resolution?
    abstract public Unit baseUnit();
    abstract public Unit minimumUnit();
    abstract public PurchaseUnit makeCopy();
    abstract protected void newInstance();
}