/*
 * ReflectionShader.java                  STATUS: Vorl�ufig abgeschlossen
 * ----------------------------------------------------------------------
 * 
 */

package raytracer.shader;

import raytracer.basic.ColorEx;
import raytracer.basic.Intersection;
import raytracer.basic.Ray;
import raytracer.util.FloatingPoint;

import javax.vecmath.Vector3d;

/**
 * Dieser Shader stellt eine vom Betrachtungswinkel abh�ngige Reflektion des
 * Lichtes auf der Objektoberfl�che dar.<br>
 * Dabei wird die Fresnel-Formel verwendet.
 * 
 * @author Mathias Kosch
 *
 */
public class ReflectionShader implements Shader
{
    /** Statisches <code>MirrorShader</code>-Objekt zum Berechnen des
     * reflektiven Anteils. */
    protected final static MirrorShader mirrorShader = new MirrorShader();
    
    
    /** Brechungsindex dieses Objekts. */
    protected final float refractiveIndex;
    /** Shader f�r den nicht reflektiven Anteil. */
    protected Shader subShader = null;
    
    
    /**
     * Erzeugt einen neuen <code>ReflectionShader</code> mit dem
     * Standard-Reflexionsexponenten.
     * 
     * @param subShader Shader, der auf das Objekt angewendet werden soll,
     *        um die Materialfarbe zu bestimmen.
     */
    public ReflectionShader(Shader subShader)
    {
        refractiveIndex = 1.2f;
        this.subShader = subShader;
    }
    
    /**
     * Erzeugt einen neuen <code>ReflectionShader</code>.
     * 
     * @param refractiveIndex Brechungsindex dieses Objekts.
     * @param subShader Shader, der auf das Objekt angewendet werden soll,
     *        um die Materialfarbe zu bestimmen.
     */
    public ReflectionShader(float refractiveIndex, Shader subShader)
    {
        this.refractiveIndex = refractiveIndex;
        this.subShader = subShader;
    }
    
    
    @Override
    public ColorEx shade(Intersection intersection)
    {
        
        Ray ray = Ray.continueRay(intersection.ray);
        if (ray == null)
            return intersection.scene.getBackgroundColor();

        Vector3d normal = intersection.getNormal();     
        Vector3d x = new Vector3d(ray.dir);
        
        
        
        
        byte sign = (byte)Math.signum(-normal.dot(x));
        
        
        float n1 = refractiveIndex;
        float n2 = (ray.refractionStack.empty()) ?
                RefractionShader.INDEX_VACUUM : ray.refractionStack.peek();

        
        float mirrorRatio;
        if (FloatingPoint.compareTolerated(n1, n2) == 0)
            mirrorRatio = 0.0f;
        else
        {
            x.scale(1.0/(x.length()* (double) n1 / (double) n2));
            
            double nDotN = normal.dot(normal);
            double nDotX = -normal.dot(x);
            Vector3d k = new Vector3d();
            k.scaleAdd(nDotX/nDotN, normal, x);
            
            
            
            double height2 = 1.0-k.dot(k);
            if (height2 < 0.0)
                height2 = 0.0;
            
            
            ray.dir.scaleAdd((double) -(int) sign *Math.sqrt(height2/nDotN), normal, k);
            
            
            float cosalpha = (float)Math.abs(normal.dot(intersection.ray.dir)/intersection.ray.dir.length());
            float cosbeta = (float)Math.abs(normal.dot(ray.dir)/ray.dir.length());
            float Rs = (n1*cosalpha-n2*cosbeta)/(n1*cosalpha+n2*cosbeta);
            float Rp = (n2*cosalpha-n1*cosbeta)/(n2*cosalpha+n1*cosbeta);
            mirrorRatio = (Rs*Rs+Rp*Rp)/2.0f;
        }
            
        
        
        float savedWeight = intersection.ray.weight;
        ColorEx color = new ColorEx();
        
        if ((double) mirrorRatio < 1.0)
        {
            
            intersection.ray.weight = savedWeight*(1.0f-mirrorRatio);
            color.scale(1.0f-mirrorRatio, subShader.shade(intersection));
        }
        if ((double) mirrorRatio > 0.0)
        {
            
            intersection.ray.weight = savedWeight*mirrorRatio;
            color.scaleAdd(mirrorRatio, mirrorShader.shade(intersection), color);
        }
        intersection.ray.weight = savedWeight;
        
        return color;

        /*float savedWeight = intersection.ray.weight;
        
        
        
        Vector3d n = intersection.getNormal();
        Vector3d v = new Vector3d();
        v.cross(n, intersection.ray.dir);
        float mirrorRatio = (float)Math.pow(v.length()/(n.length()*intersection.ray.dir.length()), reflectionExponent);
        
        
        intersection.ray.weight = savedWeight*mirrorRatio;
        ColorEx mirrorColor = mirrorShader.shade(intersection);
        
        
        intersection.ray.weight = savedWeight*(1.0f-mirrorRatio);
        ColorEx materialColor = subShader.shade(intersection);
        
        intersection.ray.weight = savedWeight;
        
        
        materialColor.scale(1.0f-mirrorRatio);
        mirrorColor.scaleAdd(mirrorRatio, materialColor);
        return mirrorColor;*/
    }
}