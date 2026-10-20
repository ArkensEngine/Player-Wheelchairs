package com.playerwheelchairs;

import net.runelite.api.Model;

/** Wheel rotation from rendered displacement, independent for each player.
 * Native geometry is allocated once per chair, never rebuilt each frame. */
final class ChairMotion
{
    private final ChairMesh mesh;
    private boolean positioned;
    private int previousX,previousY,previousPlane;
    private long previousTime;
    private double distance;
    // Integer rear/front radii: this period closes both rotations exactly.
    static final double PERIOD=2*Math.PI*ChairMesh.REAR_RADIUS*11;

    ChairMotion(ChairMesh mesh){this.mesh=mesh;}
    double getDistance(){return distance;}

    void move(int x,int y,int plane,long time)
    {
        if(positioned)
        {
            double step=Math.hypot((double)x-previousX,(double)y-previousY);
            long elapsed=time-previousTime;
            // Scene loads, teleports and long rendering gaps are not wheel travel.
            if(plane==previousPlane && elapsed>0 && elapsed<=1_000_000_000L && step<=512)
                distance=(distance+step)%PERIOD;
        }
        previousX=x;previousY=y;previousPlane=plane;previousTime=time;positioned=true;
    }

    void apply(Model model)
    {
        rotate(mesh,model.getVerticesX(),model.getVerticesY(),model.getVerticesZ(),distance);
        ChairModelFactory.updateBoundsGuards(mesh,model.getVerticesX(),model.getVerticesY(),model.getVerticesZ());
        model.translate(0,0,0);model.calculateBoundsCylinder();
    }

    static void rotate(ChairMesh mesh,float[] x,float[] y,float[] z,double travel)
    {
        for(ChairMesh.Wheel wheel:mesh.wheels)
        {
            double angle=travel/wheel.radius,c=Math.cos(angle),s=Math.sin(angle);
            for(int i=wheel.start;i<wheel.end;i++)
            {
                int[] base=mesh.vertices.get(i);
                double dy=base[1]-wheel.y,dz=base[2]-wheel.z;
                x[i]=base[0];y[i]=(float)(wheel.y+c*dy-s*dz);z[i]=(float)(wheel.z+s*dy+c*dz);
            }
        }
    }
}
