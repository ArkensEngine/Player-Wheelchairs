package com.playerwheelchairs;

import java.util.*;

/** Component-based fit for the captured short-sleeve human outfit. Unknown
 * component layouts are rejected rather than guessing which vertices are arms. */
final class PlayerArmRig
{
    private final float[] x,y,z;
    private final List<Arm> arms=new ArrayList<>();
    private static final class Arm
    {
        int side;int[] upper,lower,hand;double[] shoulder,elbow,wrist;
    }
    PlayerArmRig(SeatedReference reference)
    {
        SeatedPosture p=reference.posture;x=p.x;y=p.y;z=p.z;
        for(int side:new int[]{-1,1})
        {
            Arm arm=new Arm();arm.side=side;
            for(int[] group:reference.groups)
            {
                double[] c=center(group,0);double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
                for(int i:group){min=Math.min(min,y[i]);max=Math.max(max,y[i]);}
                if(c[0]*side<6 || c[0]*side>40)continue;
                if(group.length==8 && min<-130 && max>-130 && max<-110)arm.upper=group;
                if(group.length==24 && min<-115 && max>-110 && max<-80)arm.lower=group;
                if(group.length==12 && min>-115 && max<-75 && c[2]<0)arm.hand=group;
            }
            if(arm.upper==null || arm.lower==null || arm.hand==null){arms.clear();return;}
            arm.shoulder=center(arm.upper,-1);arm.elbow=center(arm.upper,1);arm.wrist=center(arm.hand,0);
            arms.add(arm);
        }
        for(int phase=0;phase<24;phase++)if(reachError(phase*WheelPushCycle.TRAVEL_PER_CYCLE/24)>3){arms.clear();return;}
    }
    boolean usable(){return arms.size()==2;}
    double reachError(double travel)
    {
        double error=0;
        for(Arm arm:arms)
        {
            WheelPushCycle.Hand h=WheelPushCycle.hand(arm.side,travel);
            double requested=length(sub(new double[]{h.x,h.y,h.z},arm.shoulder));
            double available=length(sub(arm.elbow,arm.shoulder))+length(sub(arm.wrist,arm.elbow));
            error=Math.max(error,requested-available);
        }
        return error;
    }
    private double[] center(int[] group,int end)
    {
        double average=0;for(int i:group)average+=y[i];average/=group.length;
        double[] out=new double[3];int n=0;
        for(int i:group)if(end==0 || (end<0?y[i]<=average:y[i]>=average))
        {out[0]+=x[i];out[1]+=y[i];out[2]+=z[i];n++;}
        for(int j=0;j<3;j++)out[j]/=n;return out;
    }
    void push(float[] outX,float[] outY,float[] outZ,double travel,double blend)
    {
        for(Arm arm:arms)
        {
            WheelPushCycle.Hand h=WheelPushCycle.hand(arm.side,travel);
            double[] target={h.x,h.y,h.z},u=sub(target,arm.shoulder);
            double upper=length(sub(arm.elbow,arm.shoulder)),lower=length(sub(arm.wrist,arm.elbow));
            double reach=Math.max(Math.abs(upper-lower)+.01,Math.min(length(u),upper+lower-.01));
            u=scale(u,1/Math.max(.001,length(u)));target=add(arm.shoulder,scale(u,reach));
            double[] pole={arm.side,0,0};pole=sub(pole,scale(u,dot(pole,u)));pole=scale(pole,1/Math.max(.001,length(pole)));
            double along=(upper*upper-lower*lower+reach*reach)/(2*reach);
            double[] elbow=add(add(arm.shoulder,scale(u,along)),scale(pole,Math.sqrt(Math.max(0,upper*upper-along*along))));
            transform(arm.upper,arm.shoulder,arm.elbow,arm.shoulder,elbow,outX,outY,outZ,blend);
            transform(arm.lower,arm.elbow,arm.wrist,elbow,target,outX,outY,outZ,blend);
            transform(arm.hand,arm.elbow,arm.wrist,elbow,target,outX,outY,outZ,blend);
        }
    }
    private void transform(int[] group,double[] a,double[] b,double[] c,double[] d,float[] ox,float[] oy,float[] oz,double blend)
    {
        double[] u=sub(b,a),v=sub(d,c);u=scale(u,1/length(u));v=scale(v,1/length(v));
        double[] axis={u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]};
        double cos=Math.max(-1,Math.min(1,dot(u,v))),sin=length(axis);axis=scale(axis,1/Math.max(.00001,sin));
        for(int i:group)
        {
            double[] p={x[i]-a[0],y[i]-a[1],z[i]-a[2]};
            double[] cross={axis[1]*p[2]-axis[2]*p[1],axis[2]*p[0]-axis[0]*p[2],axis[0]*p[1]-axis[1]*p[0]};
            double[] rotated=add(c,add(add(scale(p,cos),scale(cross,sin)),scale(axis,dot(axis,p)*(1-cos))));
            ox[i]=(float)(ox[i]*(1-blend)+rotated[0]*blend);oy[i]=(float)(oy[i]*(1-blend)+rotated[1]*blend);oz[i]=(float)(oz[i]*(1-blend)+rotated[2]*blend);
        }
    }
    private static double[] sub(double[] a,double[] b){return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};}
    private static double[] add(double[] a,double[] b){return new double[]{a[0]+b[0],a[1]+b[1],a[2]+b[2]};}
    private static double[] scale(double[] a,double b){return new double[]{a[0]*b,a[1]*b,a[2]*b};}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double length(double[] a){return Math.sqrt(dot(a,a));}
}
