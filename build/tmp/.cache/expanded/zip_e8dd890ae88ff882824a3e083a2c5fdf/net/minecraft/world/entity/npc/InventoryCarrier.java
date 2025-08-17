package net.minecraft.world.entity.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface InventoryCarrier {
    String TAG_INVENTORY = "Inventory";

    SimpleContainer getInventory();

    static void pickUpItem(ServerLevel pLevel, Mob pMob, InventoryCarrier pCarrier, ItemEntity pItemEntity) {
        ItemStack itemstack = pItemEntity.getItem();
        if (pMob.wantsToPickUp(pLevel, itemstack)) {
            SimpleContainer simplecontainer = pCarrier.getInventory();
            boolean flag = simplecontainer.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            pMob.onItemPickup(pItemEntity);
            int i = itemstack.getCount();
            ItemStack itemstack1 = simplecontainer.addItem(itemstack);
            pMob.take(pItemEntity, i - itemstack1.getCount());
            if (itemstack1.isEmpty()) {
                pItemEntity.discard();
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }
    }

    default void readInventoryFromTag(ValueInput pInput) {
        pInput.list("Inventory", ItemStack.CODEC).ifPresent(p_405544_ -> this.getInventory().fromItemList((ValueInput.TypedInputList<ItemStack>)p_405544_));
    }

    default void writeInventoryToTag(ValueOutput pOutput) {
        this.getInventory().storeAsItemList(pOutput.list("Inventory", ItemStack.CODEC));
    }
}