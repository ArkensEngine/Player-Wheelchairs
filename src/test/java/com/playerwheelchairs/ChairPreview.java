package com.playerwheelchairs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;

/** Offline orthographic render of the exact plugin chair mesh, not an in-game capture. */
final class ChairPreview
{
    static double[] project(int[] p)
    {
        double a=0.65, b=0.32;
        double x=p[0]*Math.cos(a)-p[2]*Math.sin(a), z=p[0]*Math.sin(a)+p[2]*Math.cos(a);
        return new double[]{x,p[1]*Math.cos(b)-z*Math.sin(b),p[1]*Math.sin(b)+z*Math.cos(b)};
    }
    static void render(ChairMesh mesh,Path path) throws Exception
    {
        BufferedImage image=new BufferedImage(1000,900,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(31,34,38));g.fillRect(0,0,1000,900);
        g.setColor(new Color(22,24,27));g.fillOval(230,670,560,95);
        java.util.List<int[]> faces=new ArrayList<>(mesh.faces);
        faces.sort(Comparator.comparingDouble(f->-(project(mesh.vertices.get(f[0]))[2]+project(mesh.vertices.get(f[1]))[2]+project(mesh.vertices.get(f[2]))[2])));
        for(int[] face:faces)
        {
            Polygon p=new Polygon();
            for(int i=0;i<3;i++)
            {
                double[] v=project(mesh.vertices.get(face[i]));
                p.addPoint((int)(500+v[0]*4.3),(int)(705+v[1]*4.3));
            }
            int[] a=mesh.vertices.get(face[0]),b=mesh.vertices.get(face[1]),c=mesh.vertices.get(face[2]);
            double ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
            double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
            double len=Math.sqrt(nx*nx+ny*ny+nz*nz);
            float shade=(float)(0.65+0.35*Math.abs((nx*0.5-ny*0.7-nz*0.5)/len));
            Color base=face[3]==ChairMesh.WOOD?new Color(122,82,42):face[3]==ChairMesh.CUSHION?new Color(73,49,31):new Color(111,117,123);
            g.setColor(new Color((int)(base.getRed()*shade),(int)(base.getGreen()*shade),(int)(base.getBlue()*shade)));
            g.fillPolygon(p);
        }
        g.setColor(new Color(235,229,212));g.setFont(new Font("SansSerif",Font.BOLD,29));
        g.drawString("PLAYER WHEELCHAIRS / PROTOTYPE MESH",48,62);
        g.setFont(new Font("SansSerif",Font.PLAIN,20));g.setColor(new Color(167,174,181));
        g.drawString("Actual chair geometry from the plugin source",48,102);
        g.drawString("Offline render - player pose and game lighting are not shown",48,839);
        g.dispose();ImageIO.write(image,"png",path.toFile());
    }
}
