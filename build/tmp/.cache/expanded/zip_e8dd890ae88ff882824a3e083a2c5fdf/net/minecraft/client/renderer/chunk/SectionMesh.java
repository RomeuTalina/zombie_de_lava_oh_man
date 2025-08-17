package net.minecraft.client.renderer.chunk;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SectionMesh extends AutoCloseable {
    default boolean isDifferentPointOfView(TranslucencyPointOfView pPointOfView) {
        return false;
    }

    default boolean hasRenderableLayers() {
        return false;
    }

    default boolean hasTranslucentGeometry() {
        return false;
    }

    default boolean isEmpty(ChunkSectionLayer pLayer) {
        return true;
    }

    default List<BlockEntity> getRenderableBlockEntities() {
        return Collections.emptyList();
    }

    boolean facesCanSeeEachother(Direction pFace1, Direction pFace2);

    @Nullable
    default SectionBuffers getBuffers(ChunkSectionLayer pLayer) {
        return null;
    }

    @Override
    default void close() {
    }
}