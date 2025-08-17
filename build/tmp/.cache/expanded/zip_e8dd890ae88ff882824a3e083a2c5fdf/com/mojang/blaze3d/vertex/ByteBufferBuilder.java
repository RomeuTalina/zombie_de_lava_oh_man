package com.mojang.blaze3d.vertex;

import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.MemoryAllocator;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ByteBufferBuilder implements AutoCloseable {
    private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("ByteBufferBuilder");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
    private static final long DEFAULT_MAX_CAPACITY = 4294967295L;
    private static final int MAX_GROWTH_SIZE = 2097152;
    private static final int BUFFER_FREED_GENERATION = -1;
    long pointer;
    private long capacity;
    private final long maxCapacity;
    private long writeOffset;
    private long nextResultOffset;
    private int resultCount;
    private int generation;

    public ByteBufferBuilder(int pCapacity, long pMaxCapacity) {
        this.capacity = pCapacity;
        this.maxCapacity = pMaxCapacity;
        this.pointer = ALLOCATOR.malloc(pCapacity);
        MEMORY_POOL.malloc(this.pointer, pCapacity);
        if (this.pointer == 0L) {
            throw new OutOfMemoryError("Failed to allocate " + pCapacity + " bytes");
        }
    }

    public ByteBufferBuilder(int pCapacity) {
        this(pCapacity, 4294967295L);
    }

    public static ByteBufferBuilder exactlySized(int pSize) {
        return new ByteBufferBuilder(pSize, pSize);
    }

    public long reserve(int pBytes) {
        long i = this.writeOffset;
        long j = Math.addExact(i, (long)pBytes);
        this.ensureCapacity(j);
        this.writeOffset = j;
        return Math.addExact(this.pointer, i);
    }

    private void ensureCapacity(long pCapacity) {
        if (pCapacity > this.capacity) {
            if (pCapacity > this.maxCapacity) {
                throw new IllegalArgumentException("Maximum capacity of ByteBufferBuilder (" + this.maxCapacity + ") exceeded, required " + pCapacity);
            }

            long i = Math.min(this.capacity, 2097152L);
            long j = Mth.clamp(this.capacity + i, pCapacity, this.maxCapacity);
            this.resize(j);
        }
    }

    private void resize(long pNewSize) {
        MEMORY_POOL.free(this.pointer);
        this.pointer = ALLOCATOR.realloc(this.pointer, pNewSize);
        MEMORY_POOL.malloc(this.pointer, (int)Math.min(pNewSize, 2147483647L));
        LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.capacity, pNewSize);
        if (this.pointer == 0L) {
            throw new OutOfMemoryError("Failed to resize buffer from " + this.capacity + " bytes to " + pNewSize + " bytes");
        } else {
            this.capacity = pNewSize;
        }
    }

    @Nullable
    public ByteBufferBuilder.Result build() {
        this.checkOpen();
        long i = this.nextResultOffset;
        long j = this.writeOffset - i;
        if (j == 0L) {
            return null;
        } else if (j > 2147483647L) {
            throw new IllegalStateException("Cannot build buffer larger than 2147483647 bytes (was " + j + ")");
        } else {
            this.nextResultOffset = this.writeOffset;
            this.resultCount++;
            return new ByteBufferBuilder.Result(i, (int)j, this.generation);
        }
    }

    public void clear() {
        if (this.resultCount > 0) {
            LOGGER.warn("Clearing BufferBuilder with unused batches");
        }

        this.discard();
    }

    public void discard() {
        this.checkOpen();
        if (this.resultCount > 0) {
            this.discardResults();
            this.resultCount = 0;
        }
    }

    boolean isValid(int pGeneration) {
        return pGeneration == this.generation;
    }

    void freeResult() {
        if (--this.resultCount <= 0) {
            this.discardResults();
        }
    }

    private void discardResults() {
        long i = this.writeOffset - this.nextResultOffset;
        if (i > 0L) {
            MemoryUtil.memCopy(this.pointer + this.nextResultOffset, this.pointer, i);
        }

        this.writeOffset = i;
        this.nextResultOffset = 0L;
        this.generation++;
    }

    @Override
    public void close() {
        if (this.pointer != 0L) {
            MEMORY_POOL.free(this.pointer);
            ALLOCATOR.free(this.pointer);
            this.pointer = 0L;
            this.generation = -1;
        }
    }

    private void checkOpen() {
        if (this.pointer == 0L) {
            throw new IllegalStateException("Buffer has been freed");
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class Result implements AutoCloseable {
        private final long offset;
        private final int capacity;
        private final int generation;
        private boolean closed;

        Result(final long pOffset, final int pCapacity, final int pGeneration) {
            this.offset = pOffset;
            this.capacity = pCapacity;
            this.generation = pGeneration;
        }

        public ByteBuffer byteBuffer() {
            if (!ByteBufferBuilder.this.isValid(this.generation)) {
                throw new IllegalStateException("Buffer is no longer valid");
            } else {
                return MemoryUtil.memByteBuffer(ByteBufferBuilder.this.pointer + this.offset, this.capacity);
            }
        }

        @Override
        public void close() {
            if (!this.closed) {
                this.closed = true;
                if (ByteBufferBuilder.this.isValid(this.generation)) {
                    ByteBufferBuilder.this.freeResult();
                }
            }
        }
    }
}