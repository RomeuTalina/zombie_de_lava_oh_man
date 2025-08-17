package net.minecraft.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ARGB {
    public static int alpha(int pColor) {
        return pColor >>> 24;
    }

    public static int red(int pColor) {
        return pColor >> 16 & 0xFF;
    }

    public static int green(int pColor) {
        return pColor >> 8 & 0xFF;
    }

    public static int blue(int pColor) {
        return pColor & 0xFF;
    }

    public static int color(int pAlpha, int pRed, int pGreen, int pBlue) {
        return pAlpha << 24 | pRed << 16 | pGreen << 8 | pBlue;
    }

    public static int color(int pRed, int pGreen, int pBlue) {
        return color(255, pRed, pGreen, pBlue);
    }

    public static int color(Vec3 pColor) {
        return color(as8BitChannel((float)pColor.x()), as8BitChannel((float)pColor.y()), as8BitChannel((float)pColor.z()));
    }

    public static int multiply(int pColor1, int pColor2) {
        if (pColor1 == -1) {
            return pColor2;
        } else {
            return pColor2 == -1
                ? pColor1
                : color(
                    alpha(pColor1) * alpha(pColor2) / 255,
                    red(pColor1) * red(pColor2) / 255,
                    green(pColor1) * green(pColor2) / 255,
                    blue(pColor1) * blue(pColor2) / 255
                );
        }
    }

    public static int scaleRGB(int pColor, float pScale) {
        return scaleRGB(pColor, pScale, pScale, pScale);
    }

    public static int scaleRGB(int pColor, float pRedScale, float pGreenScale, float pBlueScale) {
        return color(
            alpha(pColor),
            Math.clamp((long)((int)(red(pColor) * pRedScale)), 0, 255),
            Math.clamp((long)((int)(green(pColor) * pGreenScale)), 0, 255),
            Math.clamp((long)((int)(blue(pColor) * pBlueScale)), 0, 255)
        );
    }

    public static int scaleRGB(int pColor, int pScale) {
        return color(
            alpha(pColor),
            Math.clamp((long)red(pColor) * pScale / 255L, 0, 255),
            Math.clamp((long)green(pColor) * pScale / 255L, 0, 255),
            Math.clamp((long)blue(pColor) * pScale / 255L, 0, 255)
        );
    }

    public static int greyscale(int pColor) {
        int i = (int)(red(pColor) * 0.3F + green(pColor) * 0.59F + blue(pColor) * 0.11F);
        return color(i, i, i);
    }

    public static int lerp(float pDelta, int pColor1, int pColor2) {
        int i = Mth.lerpInt(pDelta, alpha(pColor1), alpha(pColor2));
        int j = Mth.lerpInt(pDelta, red(pColor1), red(pColor2));
        int k = Mth.lerpInt(pDelta, green(pColor1), green(pColor2));
        int l = Mth.lerpInt(pDelta, blue(pColor1), blue(pColor2));
        return color(i, j, k, l);
    }

    public static int opaque(int pColor) {
        return pColor | 0xFF000000;
    }

    public static int transparent(int pColor) {
        return pColor & 16777215;
    }

    public static int color(int pAlpha, int pColor) {
        return pAlpha << 24 | pColor & 16777215;
    }

    public static int color(float pAlpha, int pColor) {
        return as8BitChannel(pAlpha) << 24 | pColor & 16777215;
    }

    public static int white(float pAlpha) {
        return as8BitChannel(pAlpha) << 24 | 16777215;
    }

    public static int colorFromFloat(float pAlpha, float pRed, float pGreen, float pBlue) {
        return color(as8BitChannel(pAlpha), as8BitChannel(pRed), as8BitChannel(pGreen), as8BitChannel(pBlue));
    }

    public static Vector3f vector3fFromRGB24(int pColor) {
        float f = red(pColor) / 255.0F;
        float f1 = green(pColor) / 255.0F;
        float f2 = blue(pColor) / 255.0F;
        return new Vector3f(f, f1, f2);
    }

    public static int average(int pColor1, int pColor2) {
        return color(
            (alpha(pColor1) + alpha(pColor2)) / 2,
            (red(pColor1) + red(pColor2)) / 2,
            (green(pColor1) + green(pColor2)) / 2,
            (blue(pColor1) + blue(pColor2)) / 2
        );
    }

    public static int as8BitChannel(float pValue) {
        return Mth.floor(pValue * 255.0F);
    }

    public static float alphaFloat(int pColor) {
        return from8BitChannel(alpha(pColor));
    }

    public static float redFloat(int pColor) {
        return from8BitChannel(red(pColor));
    }

    public static float greenFloat(int pColor) {
        return from8BitChannel(green(pColor));
    }

    public static float blueFloat(int pColor) {
        return from8BitChannel(blue(pColor));
    }

    private static float from8BitChannel(int pValue) {
        return pValue / 255.0F;
    }

    public static int toABGR(int pColor) {
        return pColor & -16711936 | (pColor & 0xFF0000) >> 16 | (pColor & 0xFF) << 16;
    }

    public static int fromABGR(int pColor) {
        return toABGR(pColor);
    }

    public static int setBrightness(int pColor, float pBrightness) {
        int i = red(pColor);
        int j = green(pColor);
        int k = blue(pColor);
        int l = alpha(pColor);
        int i1 = Math.max(Math.max(i, j), k);
        int j1 = Math.min(Math.min(i, j), k);
        float f = i1 - j1;
        float f1;
        if (i1 != 0) {
            f1 = f / i1;
        } else {
            f1 = 0.0F;
        }

        float f2;
        if (f1 == 0.0F) {
            f2 = 0.0F;
        } else {
            float f3 = (i1 - i) / f;
            float f4 = (i1 - j) / f;
            float f5 = (i1 - k) / f;
            if (i == i1) {
                f2 = f5 - f4;
            } else if (j == i1) {
                f2 = 2.0F + f3 - f5;
            } else {
                f2 = 4.0F + f4 - f3;
            }

            f2 /= 6.0F;
            if (f2 < 0.0F) {
                f2++;
            }
        }

        if (f1 == 0.0F) {
            i = j = k = Math.round(pBrightness * 255.0F);
            return color(l, i, j, k);
        } else {
            float f8 = (f2 - (float)Math.floor(f2)) * 6.0F;
            float f9 = f8 - (float)Math.floor(f8);
            float f10 = pBrightness * (1.0F - f1);
            float f6 = pBrightness * (1.0F - f1 * f9);
            float f7 = pBrightness * (1.0F - f1 * (1.0F - f9));
            switch ((int)f8) {
                case 0:
                    i = Math.round(pBrightness * 255.0F);
                    j = Math.round(f7 * 255.0F);
                    k = Math.round(f10 * 255.0F);
                    break;
                case 1:
                    i = Math.round(f6 * 255.0F);
                    j = Math.round(pBrightness * 255.0F);
                    k = Math.round(f10 * 255.0F);
                    break;
                case 2:
                    i = Math.round(f10 * 255.0F);
                    j = Math.round(pBrightness * 255.0F);
                    k = Math.round(f7 * 255.0F);
                    break;
                case 3:
                    i = Math.round(f10 * 255.0F);
                    j = Math.round(f6 * 255.0F);
                    k = Math.round(pBrightness * 255.0F);
                    break;
                case 4:
                    i = Math.round(f7 * 255.0F);
                    j = Math.round(f10 * 255.0F);
                    k = Math.round(pBrightness * 255.0F);
                    break;
                case 5:
                    i = Math.round(pBrightness * 255.0F);
                    j = Math.round(f10 * 255.0F);
                    k = Math.round(f6 * 255.0F);
            }

            return color(l, i, j, k);
        }
    }
}