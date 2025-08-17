package net.minecraft.world;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ContainerHelper {
    public static final String TAG_ITEMS = "Items";

    public static ItemStack removeItem(List<ItemStack> pStacks, int pIndex, int pAmount) {
        return pIndex >= 0 && pIndex < pStacks.size() && !pStacks.get(pIndex).isEmpty() && pAmount > 0
            ? pStacks.get(pIndex).split(pAmount)
            : ItemStack.EMPTY;
    }

    public static ItemStack takeItem(List<ItemStack> pStacks, int pIndex) {
        return pIndex >= 0 && pIndex < pStacks.size() ? pStacks.set(pIndex, ItemStack.EMPTY) : ItemStack.EMPTY;
    }

    public static void saveAllItems(ValueOutput pOutput, NonNullList<ItemStack> pItems) {
        saveAllItems(pOutput, pItems, true);
    }

    public static void saveAllItems(ValueOutput pOutput, NonNullList<ItemStack> pItems, boolean pAllowEmpty) {
        ValueOutput.TypedOutputList<ItemStackWithSlot> typedoutputlist = pOutput.list("Items", ItemStackWithSlot.CODEC);

        for (int i = 0; i < pItems.size(); i++) {
            ItemStack itemstack = pItems.get(i);
            if (!itemstack.isEmpty()) {
                typedoutputlist.add(new ItemStackWithSlot(i, itemstack));
            }
        }

        if (typedoutputlist.isEmpty() && !pAllowEmpty) {
            pOutput.discard("Items");
        }
    }

    public static void loadAllItems(ValueInput pInput, NonNullList<ItemStack> pItems) {
        for (ItemStackWithSlot itemstackwithslot : pInput.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            if (itemstackwithslot.isValidInContainer(pItems.size())) {
                pItems.set(itemstackwithslot.slot(), itemstackwithslot.stack());
            }
        }
    }

    public static int clearOrCountMatchingItems(Container pContainer, Predicate<ItemStack> pItemPredicate, int pMaxItems, boolean pSimulate) {
        int i = 0;

        for (int j = 0; j < pContainer.getContainerSize(); j++) {
            ItemStack itemstack = pContainer.getItem(j);
            int k = clearOrCountMatchingItems(itemstack, pItemPredicate, pMaxItems - i, pSimulate);
            if (k > 0 && !pSimulate && itemstack.isEmpty()) {
                pContainer.setItem(j, ItemStack.EMPTY);
            }

            i += k;
        }

        return i;
    }

    public static int clearOrCountMatchingItems(ItemStack pStack, Predicate<ItemStack> pItemPredicate, int pMaxItems, boolean pSimulate) {
        if (pStack.isEmpty() || !pItemPredicate.test(pStack)) {
            return 0;
        } else if (pSimulate) {
            return pStack.getCount();
        } else {
            int i = pMaxItems < 0 ? pStack.getCount() : Math.min(pMaxItems, pStack.getCount());
            pStack.shrink(i);
            return i;
        }
    }
}