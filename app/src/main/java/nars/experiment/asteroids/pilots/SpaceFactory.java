package nars.experiment.asteroids.pilots;

import java.util.Random;
import java.util.HashSet;
import java.util.Collection;

import nars.experiment.asteroids.Ship;
import nars.experiment.asteroids.Universe;

public class SpaceFactory extends Pilot {
    public static final double SHIP_OFFSET = 25;

    private final Random RNG = new Random();
    private String tooling;
    private PilotFactory pilots;
    private double speed, timeout;
    private int support;
    private Collection<Ship> supported = new HashSet<Ship>();
    private double meanGold;
    private double varGold;
    private double dropRate;

    public SpaceFactory(Ship ship, String tooling, double speed, int support,
                        PilotFactory pilots) {
        super(ship);
        this.tooling = tooling;
        this.pilots = pilots;
        this.speed = this.timeout = speed;
        this.support = support;
    }

    public void addGold(double mean, double var, double rate) {
        meanGold = mean;
        varGold = var;
        dropRate = rate;
    }

    @Override
    public void drive(double dt) {
        updateSupported();
        if (supported.size() >= support) return;

        timeout -= dt;
        if (timeout > 0) return;
        timeout += speed;

        Ship ship = getShip();
        Ship forged = Ship.get(tooling);
        forged.setFaction(ship.getFaction());
        forged.setPilot(pilots.create(forged));
        double modX = SHIP_OFFSET;
        double modY = SHIP_OFFSET;
        if(RNG.nextInt(2) == 0) modX = -modX;
        if(RNG.nextInt(2) == 0) modY = -modY;
        forged.setPosition(ship.getX(0) + modX, ship.getY(0) + modY);
        if (RNG.nextDouble() < dropRate) {
            long gold = (long) (RNG.nextGaussian() * varGold + meanGold);
            if (gold > 0) forged.store(gold);
        }
        supported.add(forged);
        Universe.get().add(forged);
    }

    /**
     * Removes the first dead from collection of supported
     * ships. Since only one ship is manufactured per turn we really
     * only need to remove one at a time.
     */
    private void updateSupported() {
        Ship remove = null;
        for (Ship s : supported) {
            if (!s.isActive()) {
                remove = s;
                break;
            }
        }
        if (remove != null) {
            supported.remove(remove);
        }
    }
}
