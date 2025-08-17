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
    default boolean isDifferentPointOfView(TranslucencyPointOfView p_406250_) {
        return false;
    }

    default boolean hasRenderableLayers() {
        return false;
    }

    default boolean hasTranslucentGeometry() {
        return false;
    }

    default boolean isEmpty(ChunkSectionLayer p_410400_) {
        return true;
    }

    default List<BlockEntity> getRenderableBlockEntities() {
        return Collections.emptyList();
    }

    boolean facesCanSeeEachother(Direction p_407864_, Direction p_408147_);

    @Nullable
    default SectionBuffers getBuffers(ChunkSectionLayer p_409041_) {
        return null;
    }

    @Override
    default void close() {
    }
}