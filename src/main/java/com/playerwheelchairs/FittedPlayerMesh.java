package com.playerwheelchairs;

import java.util.*;
import net.runelite.api.*;

/** A fully owned lit mesh. Never edits a cached/player/shared animation model. */
final class FittedPlayerMesh
{
    final ChairMesh layout;
    final Model model;
    private FittedPlayerMesh(ChairMesh layout,Model model){this.layout=layout;this.model=model;}
    static boolean supported(Model source)
    {
        if(source.getVerticesCount()>3000 || source.getFaceCount()>4000)return false;
        short[] textures=source.getFaceTextures();
        if(textures!=null)for(int i=0;i<source.getFaceCount();i++)if(textures[i]>=0)return false;
        return true;
    }
    static FittedPlayerMesh create(Client client,Model source)
    {
        if(!supported(source))return null;
        ChairMesh layout=new ChairMesh();
        for(int i=0;i<source.getVerticesCount();i++)layout.vertex(source.getVerticesX()[i],source.getVerticesY()[i],source.getVerticesZ()[i]);
        for(int i=0;i<source.getFaceCount();i++)layout.faces.add(new int[]{source.getFaceIndices1()[i],source.getFaceIndices2()[i],source.getFaceIndices3()[i],0});
        Model owned=ChairModelFactory.build(client,layout);
        return owned==null?null:new FittedPlayerMesh(layout,owned);
    }
    void copy(Model source)
    {
        int nv=layout.vertices.size(),nf=layout.faces.size();
        if(source.getVerticesCount()!=nv || source.getFaceCount()!=nf)throw new IllegalArgumentException("Changed fitted topology");
        System.arraycopy(source.getVerticesX(),0,model.getVerticesX(),0,nv);
        System.arraycopy(source.getVerticesY(),0,model.getVerticesY(),0,nv);
        System.arraycopy(source.getVerticesZ(),0,model.getVerticesZ(),0,nv);
        System.arraycopy(source.getFaceColors1(),0,model.getFaceColors1(),0,nf);
        System.arraycopy(source.getFaceColors2(),0,model.getFaceColors2(),0,nf);
        System.arraycopy(source.getFaceColors3(),0,model.getFaceColors3(),0,nf);
        Arrays.fill(model.getFaceColors3(),nf,model.getFaceCount(),-2);
        byte[] alpha=source.getFaceTransparencies();
        if(alpha==null)Arrays.fill(model.getFaceTransparencies(),0,nf,(byte)0);
        else System.arraycopy(alpha,0,model.getFaceTransparencies(),0,nf);
        byte[] targetPriority=model.getFaceRenderPriorities(),sourcePriority=source.getFaceRenderPriorities();
        if(targetPriority!=null)
        {
            if(sourcePriority==null)Arrays.fill(targetPriority,0,nf,(byte)0);
            else System.arraycopy(sourcePriority,0,targetPriority,0,nf);
        }
    }
    void finish()
    {
        ChairModelFactory.updateBoundsGuards(layout,model.getVerticesX(),model.getVerticesY(),model.getVerticesZ());
        model.translate(0,0,0);model.calculateBoundsCylinder();
    }
}
