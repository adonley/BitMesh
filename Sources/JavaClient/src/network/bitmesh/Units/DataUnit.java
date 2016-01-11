package network.bitmesh.Units;

import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class DataUnit extends PurchaseUnit
{
    private static final Logger log = LoggerFactory.getLogger(DataUnit.class);

    @Override
    protected void newInstance()
    {
        measurementUnits = new LinkedList<Unit>();
        measurementUnits.add(new Unit("BYTES",1L));
        measurementUnits.add(new Unit("KILOBYTES",1000L));
        measurementUnits.add(new Unit("MEGABYTES",1000000L));
        measurementUnits.add(new Unit("GIGABYTES",1000000000L));
        sortMeasurements();
    }

    public DataUnit()
    {
        this.size = minimumUnit().getValue();
        this.price = Coin.valueOf(DEFAULT_PRICE_PER_UNIT);
        newInstance();
    }

    public DataUnit(Long size, Coin value)
    {
        super(size, value);
        newInstance();
    }

    /**
     * Returns the base unit of DataUnit. This is the first element
     * because of the sorting here.
     * @return Unit that is the baseunit of DataUnit
     */
    public Unit baseUnit()
    {
        return measurementUnits.getFirst();
    }

    public Unit minimumUnit()
    {
        final Unit min = new Unit("KILOBYTES", 2000L);
        return min;
    }

    @Override
    public PurchaseUnit makeCopy()
    {
        return new DataUnit(this.size, this.price);
    }
}
