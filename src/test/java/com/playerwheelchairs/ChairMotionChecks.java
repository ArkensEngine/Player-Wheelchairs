package com.playerwheelchairs;

import java.util.*;

final class ChairMotionChecks
{
    static void run()
    {
        ChairMesh mesh=ChairMesh.create(13,-9);
        require(mesh.wheels.size()==4,"Two rear wheels and two casters are tracked");
        ChairMotion walker=new ChairMotion(mesh),runner=new ChairMotion(mesh),idle=new ChairMotion(mesh);
        for(ChairMotion state:Arrays.asList(walker,runner,idle))state.move(0,0,0,1);
        walker.move(128,0,0,600_000_001);runner.move(256,0,0,600_000_001);idle.move(0,0,0,600_000_001);
        require(close(walker.getDistance(),128)&&close(runner.getDistance(),256),"Running rolls twice as far as walking over the same time");
        require(idle.getDistance()==0,"A stationary player's wheels do not move with another player's wheels");
        double travel=walker.getDistance();walker.move(128,0,0,700_000_001);
        require(close(walker.getDistance(),travel),"Stopping stops wheel rotation");
        walker.move(4000,0,0,800_000_001);require(close(walker.getDistance(),travel),"Teleport is not wheel travel");
        walker.move(4001,0,1,900_000_001);require(close(walker.getDistance(),travel),"Plane change is not wheel travel");
        walker.move(4010,0,1,3_000_000_001L);require(close(walker.getDistance(),travel),"Long render gap does not cause a spin burst");
        int n=mesh.vertices.size();float[] x=new float[n],y=new float[n],z=new float[n];boolean[] wheelVertex=new boolean[n];
        for(int i=0;i<n;i++){int[] v=mesh.vertices.get(i);x[i]=v[0];y[i]=v[1];z[i]=v[2];}
        for(ChairMesh.Wheel w:mesh.wheels)for(int i=w.start;i<w.end;i++)wheelVertex[i]=true;
        ChairMotion.rotate(mesh,x,y,z,39*Math.PI/2);
        boolean changed=false;
        for(int i=0;i<n;i++)
        {
            int[] base=mesh.vertices.get(i);
            if(!wheelVertex[i])require(close(x[i],base[0])&&close(y[i],base[1])&&close(z[i],base[2]),"Frame, seat, footrests and axle stay still");
            else if(!close(y[i],base[1])||!close(z[i],base[2]))changed=true;
        }
        require(changed,"Wheel geometry actually rotates");
        for(ChairMesh.Wheel w:mesh.wheels)
        {
            int i=w.start;int[] base=mesh.vertices.get(i);
            double a=Math.pow(base[1]-w.y,2)+Math.pow(base[2]-w.z,2);
            double b=Math.pow(y[i]-w.y,2)+Math.pow(z[i]-w.z,2);
            require(Math.abs(a-b)<.02,"Offset axle and wheel radius are preserved");
            double angle=(39*Math.PI/2)/w.radius;
            require(close(y[i],w.y+Math.cos(angle)*(base[1]-w.y)-Math.sin(angle)*(base[2]-w.z)),"Each wheel uses its own radius");
        }
        ChairMotion.rotate(mesh,x,y,z,0);
        for(int i=0;i<n;i++)require(close(y[i],mesh.vertices.get(i)[1])&&close(z[i],mesh.vertices.get(i)[2]),"Transforms use the original mesh, without cumulative drift");
        System.out.println("PASS: distance-driven wheel/caster rotation, walking/running speed, stopping, independent players, teleport/gap handling and fixed chair frame.");
    }
    private static boolean close(double a,double b){return Math.abs(a-b)<.0001;}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
