package com.playerwheelchairs;

import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;

final class ChairModelFactory
{
    private ChairModelFactory() {}
    static final int BOUNDS_GUARDS=8;
    static final float BOUNDS_MARGIN=8;

    static Model build(Client client, ChairMesh mesh)
    {
        // The public API has no empty-mesh allocator. Merge translated copies of
        // an untextured inventory model for native storage, then replace its geometry.
        // Only fresh merged arrays are modified; the cache model is never mutated.
        int id=client.getItemDefinition(1117).getInventoryModel(); // Bronze platebody
        ModelData seed=client.loadModelData(id);
        if(seed==null) return null; // Asset loading is asynchronous; retry next frame.
        if(seed.getVerticesCount()==0 || seed.getFaceCount()==0)
            throw new IllegalStateException("Empty mesh allocation seed");
        int count=Math.max(2, Math.max(
            (mesh.vertices.size()+BOUNDS_GUARDS+seed.getVerticesCount()-1)/seed.getVerticesCount(),
            (mesh.faces.size()+seed.getFaceCount()-1)/seed.getFaceCount())+1);
        ModelData data=null;
        for(int attempt=0;attempt<4;attempt++,count*=2)
        {
            ModelData[] parts=new ModelData[count];
            for(int i=0;i<count;i++)
                parts[i]=seed.shallowCopy().cloneVertices().translate(i*1024,0,0);
            data=client.mergeModels(parts);
            if(data.getVerticesCount()>=mesh.vertices.size()+BOUNDS_GUARDS && data.getFaceCount()>=mesh.faces.size()) break;
        }
        if(data==null || data.getVerticesCount()<mesh.vertices.size()+BOUNDS_GUARDS || data.getFaceCount()<mesh.faces.size())
            throw new IllegalStateException("Native mesh allocation was too small");
        prepareMutableArrays(data);
        Arrays.fill(data.getVerticesX(),0); Arrays.fill(data.getVerticesY(),0); Arrays.fill(data.getVerticesZ(),0);
        Arrays.fill(data.getFaceIndices1(),0); Arrays.fill(data.getFaceIndices2(),0); Arrays.fill(data.getFaceIndices3(),0);
        Arrays.fill(data.getFaceTransparencies(),(byte)255);
        if(data.getFaceTextures()!=null) Arrays.fill(data.getFaceTextures(),(short)-1);
        for(int i=0;i<mesh.vertices.size();i++)
        {
            int[] v=mesh.vertices.get(i);
            data.getVerticesX()[i]=v[0]; data.getVerticesY()[i]=v[1]; data.getVerticesZ()[i]=v[2];
        }
        for(int i=0;i<mesh.faces.size();i++)
        {
            int[] f=mesh.faces.get(i);
            data.getFaceIndices1()[i]=f[0]; data.getFaceIndices2()[i]=f[1]; data.getFaceIndices3()[i]=f[2];
            data.getFaceColors()[i]=(short)f[3]; data.getFaceTransparencies()[i]=0;
        }
        updateBoundsGuards(mesh,data.getVerticesX(),data.getVerticesY(),data.getVerticesZ());
        return data.light(64,768,-50,-10,-50);
    }

    /** Unreferenced vertices make depth-sort bounds conservative. No faces use
     * these points, so they do not draw. Refresh after rotating the wheels. */
    static void updateBoundsGuards(ChairMesh mesh,float[] x,float[] y,float[] z)
    {
        int n=mesh.vertices.size();
        if(x.length<n+BOUNDS_GUARDS || y.length<n+BOUNDS_GUARDS || z.length<n+BOUNDS_GUARDS)
            throw new IllegalArgumentException("Chair model lacks bounds guard capacity");
        float minX=Float.POSITIVE_INFINITY,minY=minX,minZ=minX;
        float maxX=Float.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        for(int i=0;i<n;i++)
        {
            minX=Math.min(minX,x[i]);maxX=Math.max(maxX,x[i]);
            minY=Math.min(minY,y[i]);maxY=Math.max(maxY,y[i]);
            minZ=Math.min(minZ,z[i]);maxZ=Math.max(maxZ,z[i]);
        }
        for(int i=0;i<BOUNDS_GUARDS;i++)
        {
            x[n+i]=(i&1)==0?minX-BOUNDS_MARGIN:maxX+BOUNDS_MARGIN;
            y[n+i]=(i&2)==0?minY-BOUNDS_MARGIN:maxY+BOUNDS_MARGIN;
            z[n+i]=(i&4)==0?minZ-BOUNDS_MARGIN:maxZ+BOUNDS_MARGIN;
        }
    }

    static void prepareMutableArrays(ModelData data)
    {
        data.cloneVertices().cloneColors().cloneTransparencies(true);
        // Native cloneTextures dereferences its backing array. Untextured cache
        // models (including the allocation seed) have no texture array at all.
        if(data.getFaceTextures()!=null) data.cloneTextures();
    }
}
