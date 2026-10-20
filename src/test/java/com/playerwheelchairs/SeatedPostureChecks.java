package com.playerwheelchairs;

import java.lang.reflect.Proxy;
import java.util.*;
import net.runelite.api.*;

final class SeatedPostureChecks
{
    static void run()
    {
        SeatedSmoothingChecks.run();
        ChairMotionChecks.run();
        ChairBoundsChecks.run();
        WheelPushChecks.run();
        SceneResetChecks.run();
        Geometry seated=geometry();
        SeatedPosture posture=seated.posture();
        float original=posture.y[0];
        seated.y[0]+=1;
        require(posture.y[0]==original,"Reference snapshot owns its vertices");
        seated.y[0]-=1;

        Geometry idle=seated.copy();
        require(Math.abs(posture.correct(idle.x,idle.y,idle.z))<1e-6,"Idle pose needs no pitch correction");
        require(equal(idle.y,seated.y)&&equal(idle.z,seated.z),"Idle mesh stays unchanged");
        Geometry inverted=seated.copy();tilt(inverted,posture.hipY,150);
        double deathCorrection=posture.correct(inverted.x,inverted.y,inverted.z);
        require(Math.abs(Math.abs(Math.toDegrees(deathCorrection))-138)<.02,"Extreme death/action pitch is corrected beyond ninety degrees");
        Geometry shifted=seated.copy();
        for(int i=0;i<shifted.x.length;i++){shifted.x[i]+=32;shifted.y[i]-=60;shifted.z[i]+=45;}
        posture.correct(shifted.x,shifted.y,shifted.z);
        require(equal(shifted.x,seated.x)&&equal(shifted.y,seated.y)&&equal(shifted.z,seated.z),"Full-body action translation stays anchored on all axes");

        Geometry attack=seated.copy();tilt(attack,posture.hipY,70);
        Geometry raisedArm=attack.copy();raisedArm.z[seated.armIndex]+=12;
        double angle=posture.correct(attack.x,attack.y,attack.z);
        posture.correct(raisedArm.x,raisedArm.y,raisedArm.z);
        require(Math.abs(Math.toDegrees(angle)+58)<.02,"Seventy-degree torso tilt is reduced to twelve degrees");
        for(int i=0;i<seated.bodyCount;i++)
            if(seated.y[i]>posture.hipY+8)
                require(close(attack.x[i],seated.x[i])&&close(attack.y[i],seated.y[i])&&close(attack.z[i],seated.z[i]),"Lower body remains anchored to the seated reference");
        double armMotion=distance(attack,raisedArm,seated.armIndex);
        require(Math.abs(armMotion-12)<.02,"Articulated arm motion is retained instead of freezing the attack");
        int spearTop=seated.spearStart,spearBottom=seated.spearStart+4;
        require(Math.abs(distance(attack,spearTop,spearBottom)-distance(seated,spearTop,spearBottom))<.02,"Spear shaft rotates rigidly rather than breaking at the waist");

        Model baseModel=model(seated);
        SeatedReference reference=new SeatedReference(baseModel);
        Geometry nativeAttack=seated.copy();tilt(nativeAttack,posture.hipY,70);
        Model nativeModel=model(nativeAttack);
        require(reference.matches(nativeModel),"Reference matches compatible mesh topology");
        Geometry different=geometry();different.a[0]=3;
        require(!reference.matches(model(different)),"Changed equipment topology is rejected");
        System.out.println("PASS: torso correction, seated lower body, arm motion, rigid weapon and topology fallback.");
    }

    private static Geometry geometry()
    {
        List<float[]> v=new ArrayList<>();
        for(int y=-180;y<=0;y+=20)for(int x=-20;x<=20;x+=10)for(int z:new int[]{-10,10})v.add(new float[]{x,y,z});
        int bodyCount=v.size(),arm=v.size();v.add(new float[]{55,-125,-10});
        int spear=v.size();
        for(int y:new int[]{-200,-10})for(int[] p:new int[][]{{46,-2},{50,-2},{50,2},{46,2}})v.add(new float[]{p[0],y,p[1]});
        List<int[]> f=new ArrayList<>();
        for(int i=0;i<4;i++)
        {
            int n=(i+1)%4;
            f.add(new int[]{spear+i,spear+n,spear+4+n});f.add(new int[]{spear+i,spear+4+n,spear+4+i});
        }
        Geometry g=new Geometry(v.size(),f.size());g.bodyCount=bodyCount;g.armIndex=arm;g.spearStart=spear;
        for(int i=0;i<v.size();i++){g.x[i]=v.get(i)[0];g.y[i]=v.get(i)[1];g.z[i]=v.get(i)[2];}
        for(int i=0;i<f.size();i++){g.a[i]=f.get(i)[0];g.b[i]=f.get(i)[1];g.c[i]=f.get(i)[2];}
        return g;
    }
    private static void tilt(Geometry g,double hip,double degrees)
    {
        double angle=Math.toRadians(degrees),c=Math.cos(angle),s=Math.sin(angle);
        for(int i=0;i<g.x.length;i++)if(g.y[i]<hip || i>=g.bodyCount)
        {
            double y=g.y[i]-hip,z=g.z[i];g.y[i]=(float)(hip+c*y-s*z);g.z[i]=(float)(s*y+c*z);
        }
    }
    private static Model model(Geometry g)
    {
        return (Model)Proxy.newProxyInstance(Model.class.getClassLoader(),new Class<?>[]{Model.class},(p,m,a)->{
            switch(m.getName())
            {
                case "getVerticesCount":return g.x.length;
                case "getVerticesX":return g.x;case "getVerticesY":return g.y;case "getVerticesZ":return g.z;
                case "getFaceCount":return g.a.length;
                case "getFaceIndices1":return g.a;case "getFaceIndices2":return g.b;case "getFaceIndices3":return g.c;
                case "translate":return p;
                case "calculateBoundsCylinder":return null;
                default:throw new AssertionError("Unexpected model mutation: "+m.getName());
            }
        });
    }
    private static double distance(Geometry a,Geometry b,int i){return Math.sqrt(Math.pow(a.x[i]-b.x[i],2)+Math.pow(a.y[i]-b.y[i],2)+Math.pow(a.z[i]-b.z[i],2));}
    private static double distance(Geometry a,int i,int j){return Math.sqrt(Math.pow(a.x[i]-a.x[j],2)+Math.pow(a.y[i]-a.y[j],2)+Math.pow(a.z[i]-a.z[j],2));}
    private static boolean close(float a,float b){return Math.abs(a-b)<.001;}
    private static boolean equal(float[] a,float[] b){if(a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(!close(a[i],b[i]))return false;return true;}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static final class Geometry
    {
        float[] x,y,z;int[] a,b,c;int bodyCount,armIndex,spearStart;
        Geometry(int n,int f){x=new float[n];y=new float[n];z=new float[n];a=new int[f];b=new int[f];c=new int[f];}
        Geometry copy(){Geometry g=new Geometry(x.length,a.length);g.x=x.clone();g.y=y.clone();g.z=z.clone();g.a=a.clone();g.b=b.clone();g.c=c.clone();g.bodyCount=bodyCount;g.armIndex=armIndex;g.spearStart=spearStart;return g;}
        SeatedPosture posture(){return new SeatedPosture(x,y,z,x.length,a,b,c,a.length);}
    }
}
