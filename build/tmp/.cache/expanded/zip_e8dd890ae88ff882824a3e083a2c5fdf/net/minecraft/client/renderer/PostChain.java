package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PostChain implements AutoCloseable {
    public static final ResourceLocation MAIN_TARGET_ID = ResourceLocation.withDefaultNamespace("main");
    private final List<PostPass> passes;
    private final Map<ResourceLocation, PostChainConfig.InternalTarget> internalTargets;
    private final Set<ResourceLocation> externalTargets;
    private final Map<ResourceLocation, RenderTarget> persistentTargets = new HashMap<>();
    private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer;

    private PostChain(
        List<PostPass> pPasses,
        Map<ResourceLocation, PostChainConfig.InternalTarget> pInternalTargets,
        Set<ResourceLocation> pExternalTargets,
        CachedOrthoProjectionMatrixBuffer pProjectionMatrixBuffer
    ) {
        this.passes = pPasses;
        this.internalTargets = pInternalTargets;
        this.externalTargets = pExternalTargets;
        this.projectionMatrixBuffer = pProjectionMatrixBuffer;
    }

    public static PostChain load(
        PostChainConfig pConfig,
        TextureManager pTextureManager,
        Set<ResourceLocation> pExternalTargets,
        ResourceLocation pName,
        CachedOrthoProjectionMatrixBuffer pProjectionMatrixBuffer
    ) throws ShaderManager.CompilationException {
        Stream<ResourceLocation> stream = pConfig.passes().stream().flatMap(PostChainConfig.Pass::referencedTargets);
        Set<ResourceLocation> set = stream.filter(p_357871_ -> !pConfig.internalTargets().containsKey(p_357871_)).collect(Collectors.toSet());
        Set<ResourceLocation> set1 = Sets.difference(set, pExternalTargets);
        if (!set1.isEmpty()) {
            throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + set1);
        } else {
            Builder<PostPass> builder = ImmutableList.builder();

            for (int i = 0; i < pConfig.passes().size(); i++) {
                PostChainConfig.Pass postchainconfig$pass = pConfig.passes().get(i);
                builder.add(createPass(pTextureManager, postchainconfig$pass, pName.withSuffix("/" + i)));
            }

            return new PostChain(builder.build(), pConfig.internalTargets(), set, pProjectionMatrixBuffer);
        }
    }

    private static PostPass createPass(TextureManager pTextureManager, PostChainConfig.Pass pPass, ResourceLocation pLocation) throws ShaderManager.CompilationException {
        RenderPipeline.Builder renderpipeline$builder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withFragmentShader(pPass.fragmentShaderId())
            .withVertexShader(pPass.vertexShaderId())
            .withLocation(pLocation);

        for (PostChainConfig.Input postchainconfig$input : pPass.inputs()) {
            renderpipeline$builder.withSampler(postchainconfig$input.samplerName() + "Sampler");
        }

        renderpipeline$builder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);

        for (String s1 : pPass.uniforms().keySet()) {
            renderpipeline$builder.withUniform(s1, UniformType.UNIFORM_BUFFER);
        }

        RenderPipeline renderpipeline = renderpipeline$builder.build();
        List<PostPass.Input> list = new ArrayList<>();

        for (PostChainConfig.Input postchainconfig$input1 : pPass.inputs()) {
            switch (postchainconfig$input1) {
                case PostChainConfig.TextureInput(String s2, ResourceLocation resourcelocation, int i, int j, boolean flag):
                    AbstractTexture abstracttexture = pTextureManager.getTexture(resourcelocation.withPath(p_357869_ -> "textures/effect/" + p_357869_ + ".png"));
                    abstracttexture.setFilter(flag, false);
                    list.add(new PostPass.TextureInput(s2, abstracttexture, i, j));
                    break;
                case PostChainConfig.TargetInput(String s, ResourceLocation resourcelocation1, boolean flag1, boolean flag2):
                    list.add(new PostPass.TargetInput(s, resourcelocation1, flag1, flag2));
                    break;
                default:
                    throw new MatchException(null, null);
            }
        }

        return new PostPass(renderpipeline, pPass.outputTarget(), pPass.uniforms(), list);
    }

    public void addToFrame(FrameGraphBuilder pFrameGraphBuilder, int pWidth, int pHeight, PostChain.TargetBundle pTargetBundle) {
        GpuBufferSlice gpubufferslice = this.projectionMatrixBuffer.getBuffer(pWidth, pHeight);
        Map<ResourceLocation, ResourceHandle<RenderTarget>> map = new HashMap<>(this.internalTargets.size() + this.externalTargets.size());

        for (ResourceLocation resourcelocation : this.externalTargets) {
            map.put(resourcelocation, pTargetBundle.getOrThrow(resourcelocation));
        }

        for (Entry<ResourceLocation, PostChainConfig.InternalTarget> entry : this.internalTargets.entrySet()) {
            ResourceLocation resourcelocation1 = entry.getKey();
            PostChainConfig.InternalTarget postchainconfig$internaltarget = entry.getValue();
            RenderTargetDescriptor rendertargetdescriptor = new RenderTargetDescriptor(
                postchainconfig$internaltarget.width().orElse(pWidth),
                postchainconfig$internaltarget.height().orElse(pHeight),
                true,
                postchainconfig$internaltarget.clearColor()
            );
            if (postchainconfig$internaltarget.persistent()) {
                RenderTarget rendertarget = this.getOrCreatePersistentTarget(resourcelocation1, rendertargetdescriptor);
                map.put(resourcelocation1, pFrameGraphBuilder.importExternal(resourcelocation1.toString(), rendertarget));
            } else {
                map.put(resourcelocation1, pFrameGraphBuilder.createInternal(resourcelocation1.toString(), rendertargetdescriptor));
            }
        }

        for (PostPass postpass : this.passes) {
            postpass.addToFrame(pFrameGraphBuilder, map, gpubufferslice);
        }

        for (ResourceLocation resourcelocation2 : this.externalTargets) {
            pTargetBundle.replace(resourcelocation2, map.get(resourcelocation2));
        }
    }

    @Deprecated
    public void process(RenderTarget pTarget, GraphicsResourceAllocator pGraphicsResourceAllocator) {
        FrameGraphBuilder framegraphbuilder = new FrameGraphBuilder();
        PostChain.TargetBundle postchain$targetbundle = PostChain.TargetBundle.of(MAIN_TARGET_ID, framegraphbuilder.importExternal("main", pTarget));
        this.addToFrame(framegraphbuilder, pTarget.width, pTarget.height, postchain$targetbundle);
        framegraphbuilder.execute(pGraphicsResourceAllocator);
    }

    private RenderTarget getOrCreatePersistentTarget(ResourceLocation pName, RenderTargetDescriptor pDescriptor) {
        RenderTarget rendertarget = this.persistentTargets.get(pName);
        if (rendertarget == null || rendertarget.width != pDescriptor.width() || rendertarget.height != pDescriptor.height()) {
            if (rendertarget != null) {
                rendertarget.destroyBuffers();
            }

            rendertarget = pDescriptor.allocate();
            pDescriptor.prepare(rendertarget);
            this.persistentTargets.put(pName, rendertarget);
        }

        return rendertarget;
    }

    @Override
    public void close() {
        this.persistentTargets.values().forEach(RenderTarget::destroyBuffers);
        this.persistentTargets.clear();

        for (PostPass postpass : this.passes) {
            postpass.close();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface TargetBundle {
        static PostChain.TargetBundle of(final ResourceLocation pId, final ResourceHandle<RenderTarget> pHandle) {
            return new PostChain.TargetBundle() {
                private ResourceHandle<RenderTarget> handle = pHandle;

                @Override
                public void replace(ResourceLocation p_368607_, ResourceHandle<RenderTarget> p_369595_) {
                    if (p_368607_.equals(pId)) {
                        this.handle = p_369595_;
                    } else {
                        throw new IllegalArgumentException("No target with id " + p_368607_);
                    }
                }

                @Nullable
                @Override
                public ResourceHandle<RenderTarget> get(ResourceLocation p_364302_) {
                    return p_364302_.equals(pId) ? this.handle : null;
                }
            };
        }

        void replace(ResourceLocation pId, ResourceHandle<RenderTarget> pHandle);

        @Nullable
        ResourceHandle<RenderTarget> get(ResourceLocation pId);

        default ResourceHandle<RenderTarget> getOrThrow(ResourceLocation pId) {
            ResourceHandle<RenderTarget> resourcehandle = this.get(pId);
            if (resourcehandle == null) {
                throw new IllegalArgumentException("Missing target with id " + pId);
            } else {
                return resourcehandle;
            }
        }
    }
}