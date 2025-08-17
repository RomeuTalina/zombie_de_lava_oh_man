package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuBuffer implements AutoCloseable {
    public static final int USAGE_MAP_READ = 1;
    public static final int USAGE_MAP_WRITE = 2;
    public static final int USAGE_HINT_CLIENT_STORAGE = 4;
    public static final int USAGE_COPY_DST = 8;
    public static final int USAGE_COPY_SRC = 16;
    public static final int USAGE_VERTEX = 32;
    public static final int USAGE_INDEX = 64;
    public static final int USAGE_UNIFORM = 128;
    public static final int USAGE_UNIFORM_TEXEL_BUFFER = 256;
    private final int usage;
    public int size;

    public GpuBuffer(int pUsage, int pSize) {
        this.size = pSize;
        this.usage = pUsage;
    }

    public int size() {
        return this.size;
    }

    public int usage() {
        return this.usage;
    }

    public abstract boolean isClosed();

    @Override
    public abstract void close();

    public GpuBufferSlice slice(int pOffset, int pLength) {
        if (pOffset >= 0 && pLength >= 0 && pOffset + pLength <= this.size) {
            return new GpuBufferSlice(this, pOffset, pLength);
        } else {
            throw new IllegalArgumentException(
                "Offset of " + pOffset + " and length " + pLength + " would put new slice outside buffer's range (of 0," + pLength + ")"
            );
        }
    }

    public GpuBufferSlice slice() {
        return new GpuBufferSlice(this, 0, this.size);
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public interface MappedView extends AutoCloseable {
        ByteBuffer data();

        @Override
        void close();
    }
}