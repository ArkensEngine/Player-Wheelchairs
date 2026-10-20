package com.playerwheelchairs;

/** A render-only knee extension for the ordinary unarmed kick (423).
 * The actor's real action and timing remain unchanged. */
final class SeatedKick
{
    static final int ACTION=423;
    static void apply(SeatedReference ref,float[] x,float[] y,float[] z,int frame,int frames)
    {
        if(frames<2)return;
        double t=Math.max(0,Math.min(1,(double)frame/(frames-1)));
        double angle=-Math.toRadians(65)*Math.pow(Math.sin(Math.PI*t),2);
        if(Math.abs(angle)<1e-10)return;
        int[] leg=null;
        for(int[] group:ref.groups)
        {
            double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
            for(int i:group){low=Math.min(low,ref.posture.y[i]);high=Math.max(high,ref.posture.y[i]);}
            if(group.length>=50 && group.length<150 && low>ref.posture.hipY-8 && high>-30){leg=group;break;}
        }
        if(leg==null)return;
        // Locate the front of the seated thigh; do not rotate from the waist.
        double kneeZ=Double.POSITIVE_INFINITY,kneeY=0;int n=0;
        for(int i:leg)if(ref.posture.x[i]>3 && ref.posture.y[i]<ref.posture.hipY+22)
            kneeZ=Math.min(kneeZ,ref.posture.z[i]);
        if(!Double.isFinite(kneeZ))return;
        for(int i:leg)if(ref.posture.x[i]>3 && ref.posture.y[i]<ref.posture.hipY+22 && ref.posture.z[i]<kneeZ+8)
        {kneeY+=ref.posture.y[i];n++;}
        if(n==0)return;kneeY/=n;
        for(int i:leg)
        {
            if(ref.posture.x[i]<=3 || ref.posture.y[i]<=kneeY-3)continue;
            double weight=Math.max(0,Math.min(1,(ref.posture.y[i]-kneeY+3)/14));
            weight=weight*weight*(3-2*weight);
            double a=angle*weight,c=Math.cos(a),s=Math.sin(a),dy=y[i]-kneeY,dz=z[i]-kneeZ;
            y[i]=(float)(kneeY+c*dy-s*dz);z[i]=(float)(kneeZ+s*dy+c*dz);
        }
    }
}
