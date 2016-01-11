package network.bitmesh.Units;

import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class TimeUnit extends PurchaseUnit
{
    private static final Logger log = LoggerFactory.getLogger(DataUnit.class);

    @Override
    public void newInstance()
    {
        measurementUnits = new LinkedList<Unit>();
        measurementUnits.add(new Unit("SECONDS",1L));
        measurementUnits.add(new Unit("MINUTES",60L));
        measurementUnits.add(new Unit("HOURS",3600L));
        measurementUnits.add(new Unit("DAYS",6400L));
        sortMeasurements();
    }

    public TimeUnit()
    {
        this.size = minimumUnit().getValue();
        this.price = Coin.valueOf(DEFAULT_PRICE_PER_UNIT);
        newInstance();
    }

    public TimeUnit(Long size, Coin value)
    {
        super(size,value);
        newInstance();
    }

    @Override
    public Unit baseUnit()
    {
        return measurementUnits.getFirst();
    }

    public Unit minimumUnit()
    {
        final Unit min = new Unit("SECONDS", 3L);
        return min;
    }

    @Override
    public PurchaseUnit makeCopy()
    {
        return new TimeUnit(this.size, this.price);
    }
}
