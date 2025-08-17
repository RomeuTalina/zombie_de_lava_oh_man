package net.minecraft.client.animation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimations {
    public static Vector3f posVec(float pX, float pY, float pZ) {
        return new Vector3f(pX, -pY, pZ);
    }

    public static Vector3f degreeVec(float pXDegrees, float pYDegrees, float pZDegrees) {
        return new Vector3f(pXDegrees * (float) (Math.PI / 180.0), pYDegrees * (float) (Math.PI / 180.0), pZDegrees * (float) (Math.PI / 180.0));
    }

    public static Vector3f scaleVec(double pXScale, double pYScale, double pZScale) {
        return new Vector3f((float)(pXScale - 1.0), (float)(pYScale - 1.0), (float)(pZScale - 1.0));
    }
}