package net.minecraft.client.renderer.item;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelResolver {
    private final Function<ResourceLocation, ItemModel> modelGetter;
    private final Function<ResourceLocation, ClientItem.Properties> clientProperties;

    public ItemModelResolver(ModelManager pModelManager) {
        this.modelGetter = pModelManager::getItemModel;
        this.clientProperties = pModelManager::getItemProperties;
    }

    public void updateForLiving(ItemStackRenderState pRenderState, ItemStack pStack, ItemDisplayContext pDisplayContext, LivingEntity pEntity) {
        this.updateForTopItem(pRenderState, pStack, pDisplayContext, pEntity.level(), pEntity, pEntity.getId() + pDisplayContext.ordinal());
    }

    public void updateForNonLiving(ItemStackRenderState pRenderState, ItemStack pStack, ItemDisplayContext pDisplayContext, Entity pEntity) {
        this.updateForTopItem(pRenderState, pStack, pDisplayContext, pEntity.level(), null, pEntity.getId());
    }

    public void updateForTopItem(
        ItemStackRenderState pRenderState,
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        @Nullable Level pLevel,
        @Nullable LivingEntity pEntity,
        int pSeed
    ) {
        pRenderState.clear();
        if (!pStack.isEmpty()) {
            pRenderState.displayContext = pDisplayContext;
            this.appendItemLayers(pRenderState, pStack, pDisplayContext, pLevel, pEntity, pSeed);
        }
    }

    public void appendItemLayers(
        ItemStackRenderState pRenderState,
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        @Nullable Level pLevel,
        @Nullable LivingEntity pEntity,
        int pSeed
    ) {
        ResourceLocation resourcelocation = pStack.get(DataComponents.ITEM_MODEL);
        if (resourcelocation != null) {
            pRenderState.setOversizedInGui(this.clientProperties.apply(resourcelocation).oversizedInGui());
            this.modelGetter
                .apply(resourcelocation)
                .update(pRenderState, pStack, this, pDisplayContext, pLevel instanceof ClientLevel clientlevel ? clientlevel : null, pEntity, pSeed);
        }
    }

    public boolean shouldPlaySwapAnimation(ItemStack pStack) {
        ResourceLocation resourcelocation = pStack.get(DataComponents.ITEM_MODEL);
        return resourcelocation == null ? true : this.clientProperties.apply(resourcelocation).handAnimationOnSwap();
    }
}