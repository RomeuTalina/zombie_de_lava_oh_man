package net.minecraft.client.renderer.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TranslucencyPointOfView {
    private int x;
    private int y;
    private int z;

    public static TranslucencyPointOfView of(Vec3 pPos, long pChunkPos) {
        return new TranslucencyPointOfView().set(pPos, pChunkPos);
    }

    public TranslucencyPointOfView set(Vec3 pPos, long pChunkPos) {
        this.x = getCoordinate(pPos.x(), SectionPos.x(pChunkPos));
        this.y = getCoordinate(pPos.y(), SectionPos.y(pChunkPos));
        this.z = getCoordinate(pPos.z(), SectionPos.z(pChunkPos));
        return this;
    }

    private static int getCoordinate(double pCoord, int pChunkCoord) {
        int i = SectionPos.blockToSectionCoord(pCoord) - pChunkCoord;
        return Mth.clamp(i, -1, 1);
    }

    public boolean isAxisAligned() {
        return this.x == 0 || this.y == 0 || this.z == 0;
    }

    @Override
    public boolean equals(Object pOther) {
        if (pOther == this) {
            return true;
        } else {
            return !(pOther instanceof TranslucencyPointOfView translucencypointofview)
                ? false
                : this.x == translucencypointofview.x
                    && this.y == translucencypointofview.y
                    && this.z == translucencypointofview.z;
        }
    }
}