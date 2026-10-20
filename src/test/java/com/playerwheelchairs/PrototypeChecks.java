package com.playerwheelchairs;

import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.runelite.api.Player;
import net.runelite.api.ModelData;
import net.runelite.api.PlayerComposition;

/** Offline checks; no game connection or credentials. */
public final class PrototypeChecks
{
    public static void main(String[] args) throws Exception
    {
        ChairMesh mesh=ChairMesh.create(0,0);
        require(mesh.vertices.size()>200,"Chair has detailed 3D geometry");
        Map<String,Integer> edges=new HashMap<>();
        int[] parent=new int[mesh.vertices.size()];
        for(int i=0;i<parent.length;i++) parent[i]=i;
        for(int[] face:mesh.faces)
        {
            for(int i=0;i<3;i++) require(face[i]>=0 && face[i]<mesh.vertices.size(),"Face index in range");
            parent[root(parent,face[1])]=root(parent,face[0]);
            parent[root(parent,face[2])]=root(parent,face[0]);
            int[] a=mesh.vertices.get(face[0]), b=mesh.vertices.get(face[1]), c=mesh.vertices.get(face[2]);
            long ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
            long nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
            require(nx*nx+ny*ny+nz*nz>0,"No degenerate triangles");
            for(int i=0;i<3;i++)
            {
                int p=face[i],q=face[(i+1)%3];
                String key=Math.min(p,q)+":"+Math.max(p,q);
                edges.merge(key,p<q?1:-1,Integer::sum);
            }
        }
        require(edges.values().stream().allMatch(v->v==0),"Closed surfaces with consistent winding");
        Map<Integer,Double> volumes=new HashMap<>();
        for(int[] f:mesh.faces)
        {
            int[] a=mesh.vertices.get(f[0]),b=mesh.vertices.get(f[1]),c=mesh.vertices.get(f[2]);
            double volume=(double)a[0]*(b[1]*c[2]-b[2]*c[1])+(double)a[1]*(b[2]*c[0]-b[0]*c[2])+(double)a[2]*(b[0]*c[1]-b[1]*c[0]);
            volumes.merge(root(parent,f[0]),volume,Double::sum);
        }
        require(volumes.values().stream().allMatch(v->v>0),"Every mesh component has outward-facing triangles");
        ChairMesh offset=ChairMesh.create(12,-7);
        for(int i=0;i<mesh.vertices.size();i++)
        {
            int[] a=mesh.vertices.get(i),b=offset.vertices.get(i);
            require(a[0]==b[0] && a[1]+12==b[1] && a[2]-7==b[2],"Alignment offsets applied exactly");
        }
        int[] state={808,4};
        Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->{
            switch(m.getName())
            {
                case "getPoseAnimation":return state[0];
                case "getPoseAnimationFrame":return state[1];
                case "setPoseAnimation":state[0]=(int)a[0];return null;
                case "setPoseAnimationFrame":state[1]=(int)a[0];return null;
                default:throw new AssertionError("Unexpected player mutation: "+m.getName());
            }
        });
        PoseOverride pose=new PoseOverride(player,4073);
        require(state[0]==4073 && state[1]==0,"Seated frame applied");
        pose.restore(); require(state[0]==808 && state[1]==4,"Original pose/frame restored");
        pose=new PoseOverride(player,4073); state[0]=819;state[1]=7;
        pose.restore(); require(state[0]==819 && state[1]==7,"Newer game/plugin pose respected");

        // Reproduce the native API behavior seen in the first live test:
        // cloneTextures throws if the model has no backing texture array.
        checkTexturePreparation(false);
        checkTexturePreparation(true);
        checkSeatedAttack();
        SeatedPostureChecks.run();

        Path root=Paths.get(args[0]).resolve("build/check-output");
        Files.createDirectories(root);
        FittedDrawChecks.run();
        StringBuilder obj=new StringBuilder("# Original wheelchair geometry; OSRS coordinates, negative Y is up\n");
        for(int[] v:mesh.vertices) obj.append("v ").append(v[0]).append(' ').append(v[1]).append(' ').append(v[2]).append('\n');
        for(int[] f:mesh.faces) obj.append("f ").append(f[0]+1).append(' ').append(f[1]+1).append(' ').append(f[2]+1).append('\n');
        Files.write(root.resolve("wheelchair.obj"),obj.toString().getBytes(StandardCharsets.UTF_8));
        ChairPreview.render(mesh,root.resolve("wheelchair-preview.png"));
        System.out.println("PASS: mesh topology, alignment offsets, pose apply/restore, newer-state preservation, textured/untextured model preparation, seated attack eligibility and action-state preservation.");
        System.out.println(mesh.vertices.size()+" vertices, "+mesh.faces.size()+" triangles. Preview and OBJ exported.");
    }
    static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    static int root(int[] parent,int i) { while(parent[i]!=i) i=parent[i];return i; }

    static void checkTexturePreparation(boolean textured)
    {
        short[] shared=textured?new short[]{3,4}:null;
        short[][] textures={shared};
        boolean[] forceAlpha={false};
        ModelData data=(ModelData)Proxy.newProxyInstance(ModelData.class.getClassLoader(),new Class<?>[]{ModelData.class},(p,m,a)->{
            switch(m.getName())
            {
                case "getFaceTextures":return textures[0];
                case "cloneVertices":case "cloneColors":return p;
                case "cloneTransparencies":forceAlpha[0]=(boolean)a[0];return p;
                case "cloneTextures":textures[0]=textures[0].clone();return p;
                default:throw new AssertionError("Unexpected mesh operation: "+m.getName());
            }
        });
        ChairModelFactory.prepareMutableArrays(data);
        require(forceAlpha[0],"Missing transparency array is allocated");
        if(textured)
        {
            textures[0][0]=-1;
            require(shared[0]==3,"Texture edits cannot mutate shared source data");
        }
        else require(textures[0]==null,"Untextured mesh remains valid without a texture array");
    }

    static void checkSeatedAttack()
    {
        int[] poseState={808,2};
        int[] attackState={422,3};
        boolean[] dead={false};
        int[] transformation={-1};
        PlayerComposition appearance=(PlayerComposition)Proxy.newProxyInstance(PlayerComposition.class.getClassLoader(),new Class<?>[]{PlayerComposition.class},(p,m,a)->{
            if("getTransformedNpcId".equals(m.getName())) return transformation[0];
            throw new AssertionError("Unexpected appearance edit: "+m.getName());
        });
        Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class<?>[]{Player.class},(p,m,a)->{
            switch(m.getName())
            {
                case "getPlayerComposition":return appearance;
                case "isDead":return dead[0];
                case "getAnimation":return attackState[0];
                case "getAnimationFrame":return attackState[1];
                case "getPoseAnimation":return poseState[0];
                case "getPoseAnimationFrame":return poseState[1];
                case "setPoseAnimation":poseState[0]=(int)a[0];return null;
                case "setPoseAnimationFrame":poseState[1]=(int)a[0];return null;
                default:throw new AssertionError("Attack state or effects must not be mutated: "+m.getName());
            }
        });
        require(WheelchairPlugin.canUseChair(player),"An attacking player keeps their chair");
        PoseOverride pose=new PoseOverride(player,4073);
        require(poseState[0]==4073 && poseState[1]==0,"Seated secondary pose applied during attack");
        require(player.getAnimation()==422 && player.getAnimationFrame()==3,"Primary attack animation and frame stay intact");
        pose.restore();
        require(poseState[0]==808 && poseState[1]==2,"Combat secondary pose restored");
        require(player.getAnimation()==422 && player.getAnimationFrame()==3,"Restoration leaves attack state intact");
        dead[0]=true;require(WheelchairPlugin.canUseChair(player),"Death and a stale dead marker after respawn do not remove the chair");
        attackState[0]=836;
        pose=new PoseOverride(player,4073);pose.restore();
        attackState[0]=-1;
        require(WheelchairPlugin.canUseChair(player),"Respawn remains eligible without resetting the actor dead flag");
        pose=new PoseOverride(player,4073);
        require(poseState[0]==4073,"Re-enabling after death applies a fresh seated pose");
        pose.restore();require(poseState[0]==808,"Disabling after respawn restores native pose");
        dead[0]=false;transformation[0]=123;require(!WheelchairPlugin.canUseChair(player),"NPC transformation remains excluded");
    }
}
