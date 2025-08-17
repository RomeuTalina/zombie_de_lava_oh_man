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

    VertexFormat(List<VertexFormatElement> pElements, List<String> pNames, IntList pOffsets, int pVertexSize) {
        this.elements = pElements;
        this.names = pNames;
        this.vertexSize = pVertexSize;
        this.elementsMask = pElements.stream().mapToInt(VertexFormatElement::mask).reduce(0, (p_344142_, p_345074_) -> p_344142_ | p_345074_);

        for (int i = 0; i < this.offsetsByElement.length; i++) {
            VertexFormatElement vertexformatelement = VertexFormatElement.byId(i);
            int j = vertexformatelement != null ? pElements.indexOf(vertexformatelement) : -1;
            this.offsetsByElement[i] = j != -1 ? pOffsets.getInt(j) : -1;
        }

        ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        for (int i = 0; i < pElements.size(); i++)
            elements.put(pNames.get(i), pElements.get(i));
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

    public int getOffset(VertexFormatElement pElement) {
        return this.offsetsByElement[pElement.id()];
    }

    public boolean contains(VertexFormatElement pElement) {
        return (this.elementsMask & pElement.mask()) != 0;
    }

    public int getElementsMask() {
        return this.elementsMask;
    }

    public String getElementName(VertexFormatElement pElement) {
        int i = this.elements.indexOf(pElement);
        if (i == -1) {
            throw new IllegalArgumentException(pElement + " is not contained in format");
        } else {
            return this.names.get(i);
        }
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther
            ? true
            : pOther instanceof VertexFormat vertexformat
                && this.elementsMask == vertexformat.elementsMask
                && this.vertexSize == vertexformat.vertexSize
                && this.names.equals(vertexformat.names)
                && Arrays.equals(this.offsetsByElement, vertexformat.offsetsByElement);
    }

    @Override
    public int hashCode() {
        return this.elementsMask * 31 + Arrays.hashCode(this.offsetsByElement);
    }

    private static GpuBuffer uploadToBuffer(@Nullable GpuBuffer pBuffer, ByteBuffer pData, int pUsage, Supplier<String> pLabel) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        if (pBuffer == null) {
            pBuffer = gpudevice.createBuffer(pLabel, pUsage, pData);
        } else {
            CommandEncoder commandencoder = gpudevice.createCommandEncoder();
            if (pBuffer.size() < pData.remaining()) {
                pBuffer.close();
                pBuffer = gpudevice.createBuffer(pLabel, pUsage, pData);
            } else {
                commandencoder.writeToBuffer(pBuffer.slice(), pData);
            }
        }

        return pBuffer;
    }

    private GpuBuffer uploadToBufferWithWorkaround(@Nullable GpuBuffer pBuffer, ByteBuffer pData, int pUsage, Supplier<String> pLabel) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        if (USE_STAGING_BUFFER_WORKAROUND) {
            if (pBuffer == null) {
                pBuffer = gpudevice.createBuffer(pLabel, pUsage, pData);
            } else {
                CommandEncoder commandencoder = gpudevice.createCommandEncoder();
                if (pBuffer.size() < pData.remaining()) {
                    pBuffer.close();
                    pBuffer = gpudevice.createBuffer(pLabel, pUsage, pData);
                } else {
                    UPLOAD_STAGING_BUFFER = uploadToBuffer(UPLOAD_STAGING_BUFFER, pData, pUsage, pLabel);
                    commandencoder.copyToBuffer(UPLOAD_STAGING_BUFFER.slice(0, pData.remaining()), pBuffer.slice(0, pData.remaining()));
                }
            }

            return pBuffer;
        } else if (GraphicsWorkarounds.get(gpudevice).alwaysCreateFreshImmediateBuffer()) {
            if (pBuffer != null) {
                pBuffer.close();
            }

            return gpudevice.createBuffer(pLabel, pUsage, pData);
        } else {
            return uploadToBuffer(pBuffer, pData, pUsage, pLabel);
        }
    }

    public GpuBuffer uploadImmediateVertexBuffer(ByteBuffer pBuffer) {
        this.immediateDrawVertexBuffer = this.uploadToBufferWithWorkaround(
            this.immediateDrawVertexBuffer, pBuffer, 40, () -> "Immediate vertex buffer for " + this
        );
        return this.immediateDrawVertexBuffer;
    }

    public GpuBuffer uploadImmediateIndexBuffer(ByteBuffer pBuffer) {
        this.immediateDrawIndexBuffer = this.uploadToBufferWithWorkaround(
            this.immediateDrawIndexBuffer, pBuffer, 72, () -> "Immediate index buffer for " + this
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

        public VertexFormat.Builder add(String pName, VertexFormatElement pElement) {
            this.elements.put(pName, pElement);
            this.offsets.add(this.offset);
            this.offset = this.offset + pElement.byteSize();
            return this;
        }

        public VertexFormat.Builder padding(int pPadding) {
            this.offset += pPadding;
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

        private IndexType(final int pBytes) {
            this.bytes = pBytes;
        }

        public static VertexFormat.IndexType least(int pIndexCount) {
            return (pIndexCount & -65536) != 0 ? INT : SHORT;
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

        private Mode(final int pPrimitiveLength, final int pPrimitiveStride, final boolean pConnectedPrimitives) {
            this.primitiveLength = pPrimitiveLength;
            this.primitiveStride = pPrimitiveStride;
            this.connectedPrimitives = pConnectedPrimitives;
        }

        public int indexCount(int pVertices) {
            return switch (this) {
                case LINES, QUADS -> pVertices / 4 * 6;
                case LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> pVertices;
                default -> 0;
            };
        }
    }
}
