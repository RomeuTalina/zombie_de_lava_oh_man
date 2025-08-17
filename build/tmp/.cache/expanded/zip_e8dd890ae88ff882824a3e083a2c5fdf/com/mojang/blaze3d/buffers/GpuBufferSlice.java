package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public record GpuBufferSlice(GpuBuffer buffer, int offset, int length) {
    public GpuBufferSlice slice(int pOffset, int pLength) {
        if (pOffset >= 0 && pLength >= 0 && pOffset + pLength < this.length) {
            return new GpuBufferSlice(this.buffer, this.offset + pOffset, pLength);
        } else {
            throw new IllegalArgumentException(
                "Offset of "
                    + pOffset
                    + " and length "
                    + pLength
                    + " would put new slice outside existing slice's range (of "
                    + pOffset
                    + ","
                    + pLength
                    + ")"
            );
        }
    }
}