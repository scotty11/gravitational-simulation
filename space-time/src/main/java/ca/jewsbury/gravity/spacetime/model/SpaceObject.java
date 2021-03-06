package ca.jewsbury.gravity.spacetime.model;

import ca.jewsbury.gravity.spacetime.SpaceTimeException;
import ca.jewsbury.gravity.spacetime.properties.SpaceTimeConstants;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StaticObject.class
 *
 *
 *
 * 3-Feb-2015
 *
 * @author Nathan
 */
public abstract class SpaceObject implements Orbital {

    private final Logger logger = LoggerFactory.getLogger(SpaceObject.class);
    private final int SAVE_LAST_POSITIONS = 1000;

    protected final boolean STATIC_OBJECT = true;
    protected final boolean DYNAMIC_OBJECT = false;

    protected final String idName;
    protected boolean isReference;

    protected double radius; //meters [[m]]
    protected double mass; //kilograms [[kg]]

    //protected CircularFifoBuffer lastPositions;
    protected SpaceTimeVector position; //meters
    protected SpaceTimeVector velocity; //meters per second [[m/s]]
    protected SpaceTimeVector acceleration; //meters per second squared [[ m/s^2 ]]
    protected SpaceTimeVector lastAcceleration;
    protected double potentialEnergy;
    protected int pushRequests;

    public SpaceObject(String idName) {
        this.idName = idName;

        position = new SpaceTimeVector();
        //lastPositions = new CircularFifoBuffer(SAVE_LAST_POSITIONS);
        velocity = new SpaceTimeVector();
        acceleration = new SpaceTimeVector();
        radius = 1.0;
        mass = 1.0;

        this.pushRequests = 0;
        this.isReference = false;
    }

    /**
     * velocity + delta
     *
     * @param delta
     */
    @Override
    public void increaseVelocity(SpaceTimeVector delta) {
        if (!isStatic()) {
            this.velocity.translate(delta);
        } else {
            logger.warn("Attempted to increase the velocity of a static object.");
        }
    }

    @Override
    public void moveObject(SpaceTimeVector displacement) {
        SpaceTimeVector lastPosition;
        if (!isStatic()) {
            lastPosition = new SpaceTimeVector(position);
            pushLastPosition(lastPosition);
            position.translate(velocity);

        } // else :: static objects dont move!
    }

    @Override
    public void pushLastPosition(SpaceTimeVector position) {
        if (this.pushRequests % SpaceTimeConstants.PUSH_REQUEST_LIMIT == 0) {
            //this.lastPositions.add(position);
            this.pushRequests = 0;
        }
        this.pushRequests++;
    }

    @Override
    public double getKineticEnergy() {
        double kineticEnergy = 0.0;

        if (!this.isStatic()) {
            kineticEnergy = (0.5) * this.mass * (this.velocity.getVectorSquared());
        }
        return kineticEnergy;
    }

    @Override
    public double getPotentialEnergy() {
        return potentialEnergy;
    }

    /* *** ACCESSORS AND MUTATORS *** */
    @Override
    public String getIdName() {
        return idName;
    }

    @Override
    public String getAsciiRender() {
        return " " + Character.toString(idName.charAt(0)).toUpperCase() + " ";
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public void setMass(double mass) {
        this.mass = mass;
    }

    @Override
    public SpaceTimeVector getVelocity() {
        return velocity;
    }

    @Override
    public void setVelocity(SpaceTimeVector velocity) {
        this.velocity = velocity;
    }

    @Override
    public CircularFifoBuffer getLastPositions() {
        return null;
    }

    @Override
    public SpaceTimeVector getPosition() {
        return position;
    }

    @Override
    public void setPosition(SpaceTimeVector position) {
        this.position = position;
    }

    @Override
    public SpaceTimeVector getAcceleration() {
        return acceleration;
    }

    @Override
    public void setAcceleration(SpaceTimeVector acceleration) {
        this.acceleration = acceleration;
    }

    @Override
    public void setPotentialEnergy(double potential) {
        this.potentialEnergy = potential;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + (this.idName != null ? this.idName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        SpaceObject other;

        if (obj != null && obj instanceof Orbital) {
            other = (SpaceObject) obj;
            isEqual = (other.hashCode() == this.hashCode());
            if (!isEqual && getIdName().equals(other.getIdName())) {
                isEqual = true;
            }
        }

        return isEqual;
    }

    @Override
    public double distanceToOther(Orbital other) {
        double distanceTo = 0;
        if (other != null && other.getPosition() != null) {
            try {
                distanceTo = (this.getPosition().distanceTo(other.getPosition()));
            } catch (SpaceTimeException e) {
                logger.error("Reported space time exception :: " + e.getMessage());
            }
        }
        return distanceTo;
    }

    @Override
    public SpaceTimeVector getUnitVectorFacingOther(Orbital other) {
        SpaceTimeVector unitVector = null;
        SpaceTimeVector rOne, rTwo;
        if (other != null && !this.equals(other)) {
            rOne = new SpaceTimeVector(this.getPosition());
            rTwo = new SpaceTimeVector(other.getPosition());
            rOne.parityOperator(); // -R1
            rTwo.translate(rOne); //R2 - R1

            unitVector = new SpaceTimeVector(rTwo); //R2-R1
            unitVector.normalize(); // (R2-R1)/|R2-R1|
        }
        return unitVector;
    }

    @Override
    public SpaceTimeVector getLastAcceleration() {
        return lastAcceleration;
    }

    @Override
    public void setLastAcceleration(SpaceTimeVector lastAcceleration) {
        this.lastAcceleration = lastAcceleration;
    }

    @Override
    public void setReferenceObject(boolean reference) {
        this.isReference = reference;
    }

    @Override
    public boolean isReferenceObject() {
        return isReference;
    }
}
