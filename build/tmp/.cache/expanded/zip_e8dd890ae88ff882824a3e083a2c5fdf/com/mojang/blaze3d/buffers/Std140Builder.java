package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;
import org.joml.Vector2fc;
import org.joml.Vector2ic;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.joml.Vector4fc;
import org.joml.Vector4ic;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class Std140Builder {
    private final ByteBuffer buffer;
    private final int start;

    private Std140Builder(ByteBuffer pBuffer) {
        this.buffer = pBuffer;
        this.start = pBuffer.position();
    }

    public static Std140Builder intoBuffer(ByteBuffer pBuffer) {
        return new Std140Builder(pBuffer);
    }

    public static Std140Builder onStack(MemoryStack pStack, int pSize) {
        return new Std140Builder(pStack.malloc(pSize));
    }

    public ByteBuffer get() {
        return this.buffer.flip();
    }

    public Std140Builder align(int pAlignment) {
        int i = this.buffer.position();
        this.buffer.position(this.start + Mth.roundToward(i - this.start, pAlignment));
        return this;
    }

    public Std140Builder putFloat(float pValue) {
        this.align(4);
        this.buffer.putFloat(pValue);
        return this;
    }

    public Std140Builder putInt(int pValue) {
        this.align(4);
        this.buffer.putInt(pValue);
        return this;
    }

    public Std140Builder putVec2(float pX, float pY) {
        this.align(8);
        this.buffer.putFloat(pX);
        this.buffer.putFloat(pY);
        return this;
    }

    public Std140Builder putVec2(Vector2fc pVector) {
        this.align(8);
        pVector.get(this.buffer);
        this.buffer.position(this.buffer.position() + 8);
        return this;
    }

    public Std140Builder putIVec2(int pX, int pY) {
        this.align(8);
        this.buffer.putInt(pX);
        this.buffer.putInt(pY);
        return this;
    }

    public Std140Builder putIVec2(Vector2ic pVector) {
        this.align(8);
        pVector.get(this.buffer);
        this.buffer.position(this.buffer.position() + 8);
        return this;
    }

    public Std140Builder putVec3(float pX, float pY, float pZ) {
        this.align(16);
        this.buffer.putFloat(pX);
        this.buffer.putFloat(pY);
        this.buffer.putFloat(pZ);
        this.buffer.position(this.buffer.position() + 4);
        return this;
    }

    public Std140Builder putVec3(Vector3fc pVector) {
        this.align(16);
        pVector.get(this.buffer);
        this.buffer.position(this.buffer.position() + 16);
        return this;
    }

    public Std140Builder putIVec3(int pX, int pY, int pZ) {
        this.align(16);
        this.buffer.putInt(pX);
        this.buffer.putInt(pY);
        this.buffer.putInt(pZ);
        this.buffer.position(this.buffer.position() + 4);
        return this;
    }

    public Std140Builder putIVec3(Vector3ic pVector) {
        this.align(16);
        pVector.get(this.buffer);
        this.buffer.position(this.buffer.position() + 16);
        return this;
    }

    public Std140Builder putVec4(float pX, float pY, float pZ, float pW) {
        this.align(16);
        this.buffer.putFloat(pX);
        this.buffer.putFloat(pY);
        this.buffer.putFloat(pZ);
        this.buffer.putFloat(pW);
        return this;
    }

    public Std140Builder putVec4(Vector4fc pVector) {
        this.align(16);
        pVector.get(this.buffer);
        this.buffer.position(this.buffer.position() + 16);
        return this;
    }

    public Std140Builder putIVec4(int pX, int pY, int pZ, int pW) {
        this.align(16);
        this.buffer.putInt(pX);
        this.buffer.putInt(pY);
        this.buffer.putInt(pZ);
        this.buffer.putInt(pW);
        return this;
    }

    public Std140Builder putIVec4(Vector4ic pVector) {
        this.align(16);
        pVector.get(this.buffer);
        this.buffer.position(this.buffer.position() + 16);
        return this;
    }

    public Std140Builder putMat4f(Matrix4fc pMatrix) {
        this.align(16);
        pMatrix.get(this.buffer);
        this.buffer.position(this.buffer.position() + 64);
        return this;
    }
}