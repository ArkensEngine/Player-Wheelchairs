package com.playerwheelchairs;

import java.util.Arrays;

/** Position-based visual correction against a captured native seated mesh.
 * Animation definitions and gameplay state are never modified. */
final class SeatedPosture
{
    final float[] x,y,z;
    int[] anchors;
    double hipY;
    private double refX,refY,refZ;
    private boolean[] nativeLegs;
    private final boolean[] rigidAttachment;
    static final double MAX_LEAN=Math.toRadians(12);

    SeatedPosture(float[] x,float[] y,float[] z,int count,int[] a,int[] b,int[] c,int faceCount)
    {
        this.x=Arrays.copyOf(x,count);this.y=Arrays.copyOf(y,count);this.z=Arrays.copyOf(z,count);
        double top=quantile(this.y,.08),bottom=quantile(this.y,.92);
        double height=bottom-top;
        if(count<16 || height<40) throw new IllegalArgumentException("No usable human reference mesh");
        double centerX=quantile(this.x,.5),centerZ=quantile(this.z,.5);
        hipY=top+height*.62;
        int[] chosen=new int[count];int n=0;
        for(int i=0;i<count;i++)
            if(this.y[i]>top+height*.20 && this.y[i]<top+height*.49
                && Math.abs(this.x[i]-centerX)<25 && Math.abs(this.z[i]-centerZ)<38)
                chosen[n++]=i;
        if(n<8) throw new IllegalArgumentException("Not enough central torso reference vertices");
        anchors=Arrays.copyOf(chosen,n);
        double sx=0,sy=0,sz=0;
        for(int i:anchors){sx+=this.x[i];sy+=this.y[i];sz+=this.z[i];}
        refX=sx/n;refY=sy/n;refZ=sz/n;
        rigidAttachment=findRigidAttachments(a,b,c,faceCount,height);
    }

    void fitNativeGroups(java.util.List<int[]> groups)
    {
        int[] body=null;
        for(int[] group:groups)
        {
            double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY;
            for(int i:group){minX=Math.min(minX,x[i]);maxX=Math.max(maxX,x[i]);}
            if(group.length>=150 && maxX-minX<46 && (body==null||group.length>body.length))body=group;
        }
        if(body==null)return;
        double top=Double.POSITIVE_INFINITY,bottom=Double.NEGATIVE_INFINITY;
        for(int i:body){top=Math.min(top,y[i]);bottom=Math.max(bottom,y[i]);}
        java.util.List<Integer> torso=new java.util.ArrayList<>();
        for(int i:body)if(y[i]>top+(bottom-top)*.35 && y[i]<top+(bottom-top)*.75 && Math.abs(x[i])<14)torso.add(i);
        if(torso.size()<8)return;
        anchors=torso.stream().mapToInt(Integer::intValue).toArray();hipY=bottom-4;
        refX=refY=refZ=0;for(int i:anchors){refX+=x[i];refY+=y[i];refZ+=z[i];}
        refX/=anchors.length;refY/=anchors.length;refZ/=anchors.length;
        nativeLegs=new boolean[x.length];
        for(int[] group:groups)
        {
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            for(int i:group){min=Math.min(min,y[i]);max=Math.max(max,y[i]);}
            if((group.length>=50 && min>hipY-8 && max>-30) || (group.length<=3 && min>=0))for(int i:group)nativeLegs[i]=true;
        }
    }

    /** Returns the pitch correction in radians. The source must be a private
     * vertex copy, obtained from Client.applyTransformations. */
    void holdSeated(float[] currentX,float[] currentY,float[] currentZ)
    {
        System.arraycopy(x,0,currentX,0,x.length);
        System.arraycopy(y,0,currentY,0,y.length);
        System.arraycopy(z,0,currentZ,0,z.length);
    }

    double correct(float[] currentX,float[] currentY,float[] currentZ)
    {
        if(currentX.length<x.length || currentY.length<y.length || currentZ.length<z.length)
            throw new IllegalArgumentException("Incompatible vertex arrays");
        double cx=0,cy=0,cz=0;
        for(int i:anchors){cx+=currentX[i];cy+=currentY[i];cz+=currentZ[i];}
        cx/=anchors.length;cy/=anchors.length;cz/=anchors.length;
        double dot=0,cross=0;
        for(int i:anchors)
        {
            double ay=currentY[i]-cy,az=currentZ[i]-cz,by=y[i]-refY,bz=z[i]-refZ;
            dot+=ay*by+az*bz;cross+=ay*bz-az*by;
        }
        double angle=Math.atan2(cross,dot);
        // Preserve a small natural lean; remove the near-horizontal torso tilt.
        // A smooth lean limit avoids the abrupt change in correction speed at
        // twelve degrees when an attack transitions into a block or idle.
        double correction=angle-MAX_LEAN*Math.tanh(angle/MAX_LEAN);
        double cos=Math.cos(correction),sin=Math.sin(correction);
        for(int i=0;i<x.length;i++)
        {
            double dy=currentY[i]-cy,dz=currentZ[i]-cz;
            double ty=refY+cos*dy-sin*dz,tz=refZ+sin*dy+cos*dz;
            double weight=nativeLegs!=null?(nativeLegs[i]?0:1):(rigidAttachment[i]?1:Math.max(0,Math.min(1,(hipY+8-y[i])/32)));
            weight=weight*weight*(3-2*weight);
            // The seated lower body stays anchored; the upper body retains its
            // live articulated attack motion after the torso pitch correction.
            currentX[i]=(float)(x[i]*(1-weight)+(refX+currentX[i]-cx)*weight);
            currentY[i]=(float)(y[i]*(1-weight)+ty*weight);
            currentZ[i]=(float)(z[i]*(1-weight)+tz*weight);
        }
        return correction;
    }

    private boolean[] findRigidAttachments(int[] a,int[] b,int[] c,int faceCount,double height)
    {
        int n=x.length;int[] parent=new int[n];
        for(int i=0;i<n;i++)parent[i]=i;
        for(int i=0;i<faceCount;i++)
        {
            if(a[i]<n && b[i]<n && c[i]<n)
            {
                parent[root(parent,b[i])]=root(parent,a[i]);
                parent[root(parent,c[i])]=root(parent,a[i]);
            }
        }
        double[][] bounds=new double[n][6];
        for(double[] v:bounds){Arrays.fill(v,0,3,Double.POSITIVE_INFINITY);Arrays.fill(v,3,6,Double.NEGATIVE_INFINITY);}
        for(int i=0;i<n;i++)
        {
            double[] v=bounds[root(parent,i)];
            v[0]=Math.min(v[0],x[i]);v[1]=Math.min(v[1],y[i]);v[2]=Math.min(v[2],z[i]);
            v[3]=Math.max(v[3],x[i]);v[4]=Math.max(v[4],y[i]);v[5]=Math.max(v[5],z[i]);
        }
        boolean[] result=new boolean[n];
        for(int i=0;i<n;i++)
        {
            double[] v=bounds[root(parent,i)];
            double[] span={v[3]-v[0],v[4]-v[1],v[5]-v[2]};Arrays.sort(span);
            // Long narrow connected attachments (e.g. a spear shaft) must rotate
            // as one piece even where they extend below the character's waist.
            result[i]=v[1]<hipY-25 && span[2]>height*.6 && span[2]>Math.max(1,span[1])*3;
        }
        return result;
    }

    private static int root(int[] p,int i){while(p[i]!=i){p[i]=p[p[i]];i=p[i];}return i;}
    private static double quantile(float[] values,double q)
    {
        float[] copy=values.clone();Arrays.sort(copy);return copy[(int)((copy.length-1)*q)];
    }
}
