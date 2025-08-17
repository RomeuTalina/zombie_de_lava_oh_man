package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PlayerEnderChestContainer extends SimpleContainer {
    @Nullable
    private EnderChestBlockEntity activeChest;

    public PlayerEnderChestContainer() {
        super(27);
    }

    public void setActiveChest(EnderChestBlockEntity pEnderChestBlockEntity) {
        this.activeChest = pEnderChestBlockEntity;
    }

    public boolean isActiveChest(EnderChestBlockEntity pEnderChest) {
        return this.activeChest == pEnderChest;
    }

    public void fromSlots(ValueInput.TypedInputList<ItemStackWithSlot> pInput) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, ItemStack.EMPTY);
        }

        for (ItemStackWithSlot itemstackwithslot : pInput) {
            if (itemstackwithslot.isValidInContainer(this.getContainerSize())) {
                this.setItem(itemstackwithslot.slot(), itemstackwithslot.stack());
            }
        }
    }

    public void storeAsSlots(ValueOutput.TypedOutputList<ItemStackWithSlot> pOutput) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemstack = this.getItem(i);
            if (!itemstack.isEmpty()) {
                pOutput.add(new ItemStackWithSlot(i, itemstack));
            }
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.activeChest != null && !this.activeChest.stillValid(pPlayer) ? false : super.stillValid(pPlayer);
    }

    @Override
    public void startOpen(Player pPlayer) {
        if (this.activeChest != null) {
            this.activeChest.startOpen(pPlayer);
        }

        super.startOpen(pPlayer);
    }

    @Override
    public void stopOpen(Player pPlayer) {
        if (this.activeChest != null) {
            this.activeChest.stopOpen(pPlayer);
        }

        super.stopOpen(pPlayer);
        this.activeChest = null;
    }
}