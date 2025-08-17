package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class VertexFormat implements net.minecraftforge.client.extensions.IForgeVertexFormat {
    public static final int UNKNOWN_ELEMENT = -1;
    private static final boolean USE_STAGING_BUFFER_WORKAROUND = Util.getPlatform() == Util.OS.WINDOWS && Util.isAarch64();
    @Nullable
    private static GpuBuffer UPLOAD_STAGING_BUFFER;
    private final List<VertexFormatElement> elements;
    private final List<String> names;
    private final int vertexSize;
    private final int elementsMask;
    private final int[] offsetsByElement = new int[32];
    @Nullable
    private GpuBuffer immediateDrawVertexBuffer;
    @Nullable
    private GpuBuffer immediateDrawIndexBuffer;
    private final com.google.common.collect.ImmutableMap<String, VertexFormatElement> elementMapping;

    VertexFormat(List<VertexFormatElement> p_343616_, List<String> p_345241_, IntList p_345522_, int p_344162_) {
        this.elements = p_343616_;
        this.names = p_345241_;
        this.vertexSize = p_344162_;
        this.elementsMask = p_343616_.stream().mapToInt(VertexFormatElement::mask).reduce(0, (p_344142_, p_345074_) -> p_344142_ | p_345074_);

        for (int i = 0; i < this.offsetsByElement.length; i++) {
            VertexFormatElement vertexformatelement = VertexFormatElement.byId(i);
            int j = vertexformatelement != null ? p_343616_.indexOf(vertexformatelement) : -1;
            this.offsetsByElement[i] = j != -1 ? p_345522_.getInt(j) : -1;
        }

        ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        for (int i = 0; i < p_343616_.size(); i++)
            elements.put(p_345241_.get(i), p_343616_.get(i));
        this.elementMapping = elements.buildOrThrow();
    }

    public static VertexFormat.Builder builder() {
        return new VertexFormat.Builder();
    }

    @Override
    public String toString() {
        return "VertexFormat" + this.names;
    }

    public int getVertexSize() {
        return this.vertexSize;
    }

    public List<VertexFormatElement> getElements() {
        return this.elements;
    }

    public List<String> getElementAttributeNames() {
        return this.names;
    }

    public int[] getOffsetsByElement() {
        return this.offsetsByElement;
    }

    public int getOffset(VertexFormatElement p_342517_) {
        return this.offsetsByElement[p_342517_.id()];
    }

    public boolean contains(VertexFormatElement p_345196_) {
        return (this.elementsMask & p_345196_.mask()) != 0;
    }

    public int getElementsMask() {
        return this.elementsMask;
    }

    public String getElementName(VertexFormatElement p_345336_) {
        int i = this.elements.indexOf(p_345336_);
        if (i == -1) {
            throw new IllegalArgumentException(p_345336_ + " is not contained in format");
        } else {
            return this.names.get(i);
        }
    }

    @Override
    public boolean equals(Object p_86026_) {
        return this == p_86026_
            ? true
            : p_86026_ instanceof VertexFormat vertexformat
                && this.elementsMask == vertexformat.elementsMask
                && this.vertexSize == vertexformat.vertexSize
                && this.names.equals(vertexformat.names)
                && Arrays.equals(this.offsetsByElement, vertexformat.offsetsByElement);
    }

    @Override
    public int hashCode() {
        return this.elementsMask * 31 + Arrays.hashCode(this.offsetsByElement);
    }

    private static GpuBuffer uploadToBuffer(@Nullable GpuBuffer p_410777_, ByteBuffer p_410807_, int p_410790_, Supplier<String> p_410792_) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        if (p_410777_ == null) {
            p_410777_ = gpudevice.createBuffer(p_410792_, p_410790_, p_410807_);
        } else {
            CommandEncoder commandencoder = gpudevice.createCommandEncoder();
            if (p_410777_.size() < p_410807_.remaining()) {
                p_410777_.close();
                p_410777_ = gpudevice.createBuffer(p_410792_, p_410790_, p_410807_);
            } else {
                commandencoder.writeToBuffer(p_410777_.slice(), p_410807_);
            }
        }

        return p_410777_;
    }

    private GpuBuffer uploadToBufferWithWorkaround(@Nullable GpuBuffer p_410785_, ByteBuffer p_410811_, int p_410786_, Supplier<String> p_410797_) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        if (USE_STAGING_BUFFER_WORKAROUND) {
            if (p_410785_ == null) {
                p_410785_ = gpudevice.createBuffer(p_410797_, p_410786_, p_410811_);
            } else {
                CommandEncoder commandencoder = gpudevice.createCommandEncoder();
                if (p_410785_.size() < p_410811_.remaining()) {
                    p_410785_.close();
                    p_410785_ = gpudevice.createBuffer(p_410797_, p_410786_, p_410811_);
                } else {
                    UPLOAD_STAGING_BUFFER = uploadToBuffer(UPLOAD_STAGING_BUFFER, p_410811_, p_410786_, p_410797_);
                    commandencoder.copyToBuffer(UPLOAD_STAGING_BUFFER.slice(0, p_410811_.remaining()), p_410785_.slice(0, p_410811_.remaining()));
                }
            }

            return p_410785_;
        } else if (GraphicsWorkarounds.get(gpudevice).alwaysCreateFreshImmediateBuffer()) {
            if (p_410785_ != null) {
                p_410785_.close();
            }

            return gpudevice.createBuffer(p_410797_, p_410786_, p_410811_);
        } else {
            return uploadToBuffer(p_410785_, p_410811_, p_410786_, p_410797_);
        }
    }

    public GpuBuffer uploadImmediateVertexBuffer(ByteBuffer p_394343_) {
        this.immediateDrawVertexBuffer = this.uploadToBufferWithWorkaround(
            this.immediateDrawVertexBuffer, p_394343_, 40, () -> "Immediate vertex buffer for " + this
        );
        return this.immediateDrawVertexBuffer;
    }

    public GpuBuffer uploadImmediateIndexBuffer(ByteBuffer p_391835_) {
        this.immediateDrawIndexBuffer = this.uploadToBufferWithWorkaround(
            this.immediateDrawIndexBuffer, p_391835_, 72, () -> "Immediate index buffer for " + this
        );
        return this.immediateDrawIndexBuffer;
    }

    public ImmutableMap<String, VertexFormatElement> getElementMapping() { return elementMapping; }
    public int getOffset(int index) { return offsetsByElement[index]; }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public static class Builder {
        private final ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        private final IntList offsets = new IntArrayList();
        private int offset;

        Builder() {
        }

        public VertexFormat.Builder add(String p_343401_, VertexFormatElement p_345244_) {
            this.elements.put(p_343401_, p_345244_);
            this.offsets.add(this.offset);
            this.offset = this.offset + p_345244_.byteSize();
            return this;
        }

        public VertexFormat.Builder padding(int p_345477_) {
            this.offset += p_345477_;
            return this;
        }

        public VertexFormat build() {
            ImmutableMap<String, VertexFormatElement> immutablemap = this.elements.buildOrThrow();
            ImmutableList<VertexFormatElement> immutablelist = immutablemap.values().asList();
            ImmutableList<String> immutablelist1 = immutablemap.keySet().asList();
            return new VertexFormat(immutablelist, immutablelist1, this.offsets, this.offset);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum IndexType {
        SHORT(2),
        INT(4);

        public final int bytes;

        private IndexType(final int p_166930_) {
            this.bytes = p_166930_;
        }

        public static VertexFormat.IndexType least(int p_166934_) {
            return (p_166934_ & -65536) != 0 ? INT : SHORT;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Mode {
        LINES(2, 2, false),
        LINE_STRIP(2, 1, true),
        DEBUG_LINES(2, 2, false),
        DEBUG_LINE_STRIP(2, 1, true),
        TRIANGLES(3, 3, false),
        TRIANGLE_STRIP(3, 1, true),
        TRIANGLE_FAN(3, 1, true),
        QUADS(4, 4, false);

        public final int primitiveLength;
        public final int primitiveStride;
        public final boolean connectedPrimitives;

        private Mode(final int p_231238_, final int p_231239_, final boolean p_231241_) {
            this.primitiveLength = p_231238_;
            this.primitiveStride = p_231239_;
            this.connectedPrimitives = p_231241_;
        }

        public int indexCount(int p_166959_) {
            return switch (this) {
                case LINES, QUADS -> p_166959_ / 4 * 6;
                case LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> p_166959_;
                default -> 0;
            };
        }
    }
}
