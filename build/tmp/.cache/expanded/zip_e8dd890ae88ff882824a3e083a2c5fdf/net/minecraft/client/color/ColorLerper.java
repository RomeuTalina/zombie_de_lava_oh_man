package net.minecraft.client.color;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColorLerper {
    public static final DyeColor[] MUSIC_NOTE_COLORS = new DyeColor[]{
        DyeColor.WHITE,
        DyeColor.LIGHT_GRAY,
        DyeColor.LIGHT_BLUE,
        DyeColor.BLUE,
        DyeColor.CYAN,
        DyeColor.GREEN,
        DyeColor.LIME,
        DyeColor.YELLOW,
        DyeColor.ORANGE,
        DyeColor.PINK,
        DyeColor.RED,
        DyeColor.MAGENTA
    };

    public static int getLerpedColor(ColorLerper.Type pType, float pTime) {
        int i = Mth.floor(pTime);
        int j = i / pType.colorDuration;
        int k = pType.colors.length;
        int l = j % k;
        int i1 = (j + 1) % k;
        float f = (i % pType.colorDuration + Mth.frac(pTime)) / pType.colorDuration;
        int j1 = pType.getColor(pType.colors[l]);
        int k1 = pType.getColor(pType.colors[i1]);
        return ARGB.lerp(f, j1, k1);
    }

    static int getModifiedColor(DyeColor pColor, float pBrightness) {
        if (pColor == DyeColor.WHITE) {
            return -1644826;
        } else {
            int i = pColor.getTextureDiffuseColor();
            return ARGB.color(
                255, Mth.floor(ARGB.red(i) * pBrightness), Mth.floor(ARGB.green(i) * pBrightness), Mth.floor(ARGB.blue(i) * pBrightness)
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        SHEEP(25, DyeColor.values(), 0.75F),
        MUSIC_NOTE(30, ColorLerper.MUSIC_NOTE_COLORS, 1.25F);

        final int colorDuration;
        private final Map<DyeColor, Integer> colorByDye;
        final DyeColor[] colors;

        private Type(final int pColorDuration, final DyeColor[] pColors, final float pBrightness) {
            this.colorDuration = pColorDuration;
            this.colorByDye = Maps.newHashMap(
                Arrays.stream(pColors).collect(Collectors.toMap(p_407631_ -> (DyeColor)p_407631_, p_407270_ -> ColorLerper.getModifiedColor(p_407270_, pBrightness)))
            );
            this.colors = pColors;
        }

        public final int getColor(DyeColor pDye) {
            return this.colorByDye.get(pDye);
        }
    }
}