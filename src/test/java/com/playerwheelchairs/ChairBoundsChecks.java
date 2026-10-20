package com.playerwheelchairs;

final class ChairBoundsChecks
{
    static void run()
    {
        ChairMesh base=ChairMesh.create(0,0),fitted=ChairMesh.create(0,0,16,12);
        for(int i=0;i<8;i++)require(fitted.vertices.get(i)[1]==base.vertices.get(i)[1]-16,"Cushion rises independently");
        for(ChairMesh.Wheel w:base.wheels)for(int i=w.start;i<w.end;i++)
            require(java.util.Arrays.equals(base.vertices.get(i),fitted.vertices.get(i)),"Seat fitting does not move grounded wheels");
        int n=fitted.vertices.size();float[] x=new float[n+8],y=new float[n+8],z=new float[n+8];
        for(int i=0;i<n;i++){int[] p=fitted.vertices.get(i);x[i]=p[0];y[i]=p[1];z[i]=p[2];}
        for(int step=0;step<96;step++)
        {
            ChairMotion.rotate(fitted,x,y,z,step*7.3);
            ChairModelFactory.updateBoundsGuards(fitted,x,y,z);
            for(int i=0;i<n;i++)
                require(x[i]>=x[n]+7.999 && x[i]<=x[n+7]-7.999 && y[i]>=y[n]+7.999 && y[i]<=y[n+7]-7.999 && z[i]>=z[n]+7.999 && z[i]<=z[n+7]-7.999,"Rotated visible vertices stay strictly inside invisible bounds");
            // Mirror the GPU's radius + projected-face-depth bucket check over
            // a range of camera directions, with conservative cylinder bounds.
            double xz=0,top=0,bottom=0;
            for(int i=0;i<x.length;i++){xz=Math.max(xz,x[i]*x[i]+z[i]*z[i]);top=Math.max(top,-y[i]);bottom=Math.max(bottom,y[i]);}
            int radius=(int)Math.ceil(Math.sqrt(xz+top*top)),diameter=radius+(int)Math.ceil(Math.sqrt(xz+bottom*bottom));
            for(int yaw=0;yaw<16;yaw++)for(int pitch=0;pitch<5;pitch++)
            {
                double a=yaw*Math.PI/8,b=pitch*Math.PI/10;
                double dx=Math.sin(a)*Math.cos(b),dy=Math.sin(b),dz=Math.cos(a)*Math.cos(b);
                for(int[] f:fitted.faces)
                {
                    double depth=0;for(int k=0;k<3;k++){int v=f[k];depth+=dx*x[v]+dy*y[v]+dz*z[v];}
                    int bucket=radius+(int)(depth/3);
                    require(bucket>=0 && bucket<diameter,"GPU-style depth bucket remains inside guarded bounds");
                }
            }
        }
        for(int[] face:fitted.faces)for(int k=0;k<3;k++)require(face[k]<n,"Bounds guards have no visible faces");
        System.out.println("PASS: independent cushion fit, grounded wheels, invisible bounds guards and GPU-style sorting buckets across wheel/camera angles.");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
