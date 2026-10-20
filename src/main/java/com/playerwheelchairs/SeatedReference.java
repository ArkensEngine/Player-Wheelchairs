package com.playerwheelchairs;

import net.runelite.api.Model;

final class SeatedReference
    {
        final SeatedPosture posture;
        final int vertexCount,faceCount,faceHash;
        final java.util.List<int[]> groups;
        SeatedReference(Model model)
        {
            vertexCount=model.getVerticesCount();faceCount=model.getFaceCount();
            posture=new SeatedPosture(model.getVerticesX(),model.getVerticesY(),model.getVerticesZ(),vertexCount,
                model.getFaceIndices1(),model.getFaceIndices2(),model.getFaceIndices3(),faceCount);
            groups=groups(model,vertexCount);posture.fitNativeGroups(groups);
            faceHash=hashFaces(model,faceCount);
        }
        private static java.util.List<int[]> groups(Model m,int n)
        {
            int[] p=new int[n];for(int i=0;i<n;i++)p[i]=i;
            int[] a=m.getFaceIndices1(),b=m.getFaceIndices2(),c=m.getFaceIndices3();
            for(int i=0;i<m.getFaceCount();i++){p[root(p,b[i])]=root(p,a[i]);p[root(p,c[i])]=root(p,a[i]);}
            java.util.Map<Integer,java.util.List<Integer>> bins=new java.util.LinkedHashMap<>();
            for(int i=0;i<n;i++)bins.computeIfAbsent(root(p,i),k->new java.util.ArrayList<>()).add(i);
            java.util.List<int[]> out=new java.util.ArrayList<>();
            for(java.util.List<Integer> bin:bins.values())out.add(bin.stream().mapToInt(Integer::intValue).toArray());return out;
        }
        private static int root(int[] p,int i){while(p[i]!=i){p[i]=p[p[i]];i=p[i];}return i;}
        boolean matches(Model model)
        {
            // Native spot effects may append vertices; keep their extra geometry
            // unchanged, but require the player's base topology to match.
            return model.getVerticesCount()>=vertexCount && model.getFaceCount()>=faceCount && hashFaces(model,faceCount)==faceHash;
        }
        private static int hashFaces(Model m,int n)
        {
            int h=1;int[] a=m.getFaceIndices1(),b=m.getFaceIndices2(),c=m.getFaceIndices3();
            for(int i=0;i<n;i++){h=31*h+a[i];h=31*h+b[i];h=31*h+c[i];}
            return h;
        }
    }
