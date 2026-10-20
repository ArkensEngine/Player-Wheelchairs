package com.playerwheelchairs;

/** Authoring-space hand targets, in the same local units as ChairMesh.
 * A rim-contact stroke is followed by an elevated recovery. These targets are
 * not applied to arbitrary player vertices: a fitted arm rig is required. */
final class WheelPushCycle
{
    static final double TRAVEL_PER_CYCLE=128;
    static final double CONTACT_FRACTION=.30;
    static final class Hand
    {
        final double x,y,z;
        final boolean touchingRim;
        Hand(double x,double y,double z,boolean touchingRim){this.x=x;this.y=y;this.z=z;this.touchingRim=touchingRim;}
    }
    static Hand hand(int side,double travelled)
    {
        double phase=travelled/TRAVEL_PER_CYCLE;
        phase-=Math.floor(phase);
        double radius=ChairMesh.REAR_RADIUS,axleY=ChairMesh.REAR_AXLE_Y,axleZ=ChairMesh.REAR_AXLE_Z;
        double start=-Math.PI/2+TRAVEL_PER_CYCLE*CONTACT_FRACTION/(2*radius),end=-Math.PI-start;
        if(phase<CONTACT_FRACTION)
        {
            double theta=start-travelledPhase(phase)/radius;
            return new Hand(side*43,axleY+radius*Math.sin(theta),axleZ+radius*Math.cos(theta),true);
        }
        double t=(phase-CONTACT_FRACTION)/(1-CONTACT_FRACTION);
        double lift=18*Math.pow(Math.sin(Math.PI*t),2);
        double duration=1-CONTACT_FRACTION;
        double y=hermite(axleY+radius*Math.sin(end),axleY+radius*Math.sin(start),
            -TRAVEL_PER_CYCLE*Math.cos(end)*duration,-TRAVEL_PER_CYCLE*Math.cos(start)*duration,t)-lift;
        double z=hermite(axleZ+radius*Math.cos(end),axleZ+radius*Math.cos(start),
            TRAVEL_PER_CYCLE*Math.sin(end)*duration,TRAVEL_PER_CYCLE*Math.sin(start)*duration,t);
        return new Hand(side*43,y,z,false);
    }
    private static double travelledPhase(double phase){return phase*TRAVEL_PER_CYCLE;}
    private static double hermite(double a,double b,double va,double vb,double t)
    {
        double t2=t*t,t3=t2*t;
        return (2*t3-3*t2+1)*a+(t3-2*t2+t)*va+(-2*t3+3*t2)*b+(t3-t2)*vb;
    }
    static boolean mayPush(boolean moving,int action,boolean handsFree)
    {
        return moving && action==-1 && handsFree;
    }
}
