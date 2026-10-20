package com.playerwheelchairs;

import java.util.ArrayList;
import java.util.List;

/** Original mesh, in OSRS units: negative Y is up; the chair faces negative Z. */
public final class ChairMesh
{
    public final List<int[]> vertices = new ArrayList<>();
    public final List<int[]> faces = new ArrayList<>();
    public final List<Wheel> wheels = new ArrayList<>();
    public static final class Wheel
    {
        public final int start,end,radius;
        public int y,z;
        Wheel(int start,int end,int y,int z,int radius)
        {this.start=start;this.end=end;this.y=y;this.z=z;this.radius=radius;}
    }
    public static final int WOOD = hsl(6, 4, 30);
    public static final int IRON = hsl(0, 0, 34);
    public static final int CUSHION = hsl(6, 3, 19);
    public static final int REAR_RADIUS=47,REAR_AXLE_Y=-48,REAR_AXLE_Z=18;

    public static ChairMesh create(int verticalOffset, int forwardOffset)
    {
        return create(verticalOffset,forwardOffset,0,0);
    }

    public static ChairMesh create(int verticalOffset,int forwardOffset,int seatLift,int footLift)
    {
        return create(verticalOffset,forwardOffset,seatLift,footLift,0);
    }

    public static ChairMesh create(int verticalOffset,int forwardOffset,int seatLift,int footLift,int cushionDrop)
    {
        ChairMesh m = new ChairMesh();
        m.box(-31, -70-seatLift+cushionDrop, -25, 31, -63-seatLift+cushionDrop, 28, CUSHION);
        m.box(-31, -125-seatLift, 23, 31, -67-seatLift, 30, WOOD);
        for (int side : new int[]{-1, 1})
        {
            int x = side * 35;
            m.box(x - 3, -95-seatLift, -27, x + 3, -88-seatLift, 27, WOOD);
            m.beam(x, -91-seatLift, -20, x, -43, -20, 3, IRON);
            m.beam(x, -91-seatLift, 23, x, -43, 23, 3, IRON);
            m.beam(x, -45, 24, x, -20, -51, 3, IRON);
            m.wheel(side * 43, REAR_AXLE_Y, REAR_AXLE_Z, REAR_RADIUS, REAR_RADIUS-6, 5, 16);
            m.wheel(side * 32, -12, -46, 11, 0, 5, 10);
            m.beam(side * 25, -62-seatLift, -21, side * 25, -14-footLift, -65, 3, IRON);
            m.box(side * 20 - 10, -15-footLift, -79, side * 20 + 10, -11-footLift, -56, IRON);
            m.beam(side * 26, -122-seatLift, 27, side * 26, -122-seatLift, 42, 3, IRON);
        }
        m.beam(-39, REAR_AXLE_Y, REAR_AXLE_Z, 39, REAR_AXLE_Y, REAR_AXLE_Z, 3, IRON);
        for (int[] v : m.vertices)
        {
            v[1] += verticalOffset;
            v[2] += forwardOffset;
        }
        for(Wheel wheel:m.wheels){wheel.y+=verticalOffset;wheel.z+=forwardOffset;}
        return m;
    }

    static int hsl(int h, int s, int l) { return (h << 10) | (s << 7) | l; }

    int vertex(double x, double y, double z)
    {
        vertices.add(new int[]{(int)Math.round(x), (int)Math.round(y), (int)Math.round(z)});
        return vertices.size() - 1;
    }

    void quad(int a, int b, int c, int d, int color)
    {
        faces.add(new int[]{a, b, c, color});
        faces.add(new int[]{a, c, d, color});
    }

    void box(int x0, int y0, int z0, int x1, int y1, int z1, int color)
    {
        int b = vertices.size();
        vertex(x0,y0,z0); vertex(x1,y0,z0); vertex(x1,y1,z0); vertex(x0,y1,z0);
        vertex(x0,y0,z1); vertex(x1,y0,z1); vertex(x1,y1,z1); vertex(x0,y1,z1);
        quad(b,b+3,b+2,b+1,color); quad(b+4,b+5,b+6,b+7,color);
        quad(b,b+1,b+5,b+4,color); quad(b+3,b+7,b+6,b+2,color);
        quad(b,b+4,b+7,b+3,color); quad(b+1,b+2,b+6,b+5,color);
    }

    void beam(double ax, double ay, double az, double bx, double by, double bz, double r, int color)
    {
        double dx=bx-ax, dy=by-ay, dz=bz-az, len=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (len == 0) throw new IllegalArgumentException("Zero-length beam");
        dx/=len; dy/=len; dz/=len;
        double ux=0, uy=dz, uz=-dy;
        if (Math.abs(dx) > 0.9) { ux=-dz; uy=0; uz=dx; }
        double ul=Math.sqrt(ux*ux+uy*uy+uz*uz);
        ux/=ul; uy/=ul; uz/=ul;
        double vx=dy*uz-dz*uy, vy=dz*ux-dx*uz, vz=dx*uy-dy*ux;
        int base=vertices.size();
        for (int end=0; end<2; end++)
            for (int i=0; i<4; i++)
            {
                double a=2*Math.PI*i/4, u=Math.cos(a)*r, v=Math.sin(a)*r;
                vertex((end==0?ax:bx)+u*ux+v*vx, (end==0?ay:by)+u*uy+v*vy,
                    (end==0?az:bz)+u*uz+v*vz);
            }
        quad(base+3,base+2,base+1,base,color);
        quad(base+4,base+5,base+6,base+7,color);
        for(int i=0;i<4;i++) quad(base+i,base+(i+1)%4,base+4+(i+1)%4,base+4+i,color);
    }

    void wheel(int x, int y, int z, int outer, int inner, int thickness, int segments)
    {
        int start=vertices.size();
        wheelGeometry(x,y,z,outer,inner,thickness,segments);
        wheels.add(new Wheel(start,vertices.size(),y,z,outer));
    }

    private void wheelGeometry(int x, int y, int z, int outer, int inner, int thickness, int segments)
    {
        int faceStart=faces.size();
        if (inner == 0)
        {
            // A narrow solid cylinder for the small front caster.
            int a=vertex(x-thickness/2.0,y,z), b=vertex(x+thickness/2.0,y,z);
            int base=vertices.size();
            for(int i=0;i<segments;i++)
            {
                double t=2*Math.PI*i/segments;
                vertex(x-thickness/2.0,y+outer*Math.sin(t),z+outer*Math.cos(t));
                vertex(x+thickness/2.0,y+outer*Math.sin(t),z+outer*Math.cos(t));
            }
            for(int i=0;i<segments;i++)
            {
                int p=base+2*i, q=base+2*((i+1)%segments);
                faces.add(new int[]{a,q,p,IRON}); faces.add(new int[]{b,p+1,q+1,IRON});
                quad(p,q,q+1,p+1,IRON);
            }
            reverseFaces(faceStart);
            return;
        }
        int base=vertices.size();
        for(int i=0;i<segments;i++)
        {
            double t=2*Math.PI*i/segments;
            for (int side : new int[]{-1,1})
                for(int radius : new int[]{outer,inner})
                    vertex(x+side*thickness/2.0,y+radius*Math.sin(t),z+radius*Math.cos(t));
        }
        for(int i=0;i<segments;i++)
        {
            int p=base+4*i, q=base+4*((i+1)%segments);
            quad(p,p+1,q+1,q,IRON); quad(p+2,q+2,q+3,p+3,IRON);
            quad(p,q,q+2,p+2,IRON); quad(p+1,p+3,q+3,q+1,IRON);
        }
        reverseFaces(faceStart);
        for(int i=0;i<8;i++)
        {
            double t=2*Math.PI*i/8;
            beam(x,y,z,x,y+(inner-1)*Math.sin(t),z+(inner-1)*Math.cos(t),1.6,WOOD);
        }
    }

    private void reverseFaces(int start)
    {
        for(int i=start;i<faces.size();i++)
        {
            int[] face=faces.get(i);
            int swap=face[1];face[1]=face[2];face[2]=swap;
        }
    }
}
