package com.flashminat0.selectivekeepinv;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Baubles (modid "baubles") soft-dependency helper. Every Baubles API reference
 * lives here so the class is never loaded (and never triggers
 * ClassNotFoundException) when Baubles is absent. Callers must gate access
 * with Loader.isModLoaded("baubles") before calling any method.
 */
public class BaublesCompat {

    static final String NBT_KEY = "Baubles";

    public static void saveBaubles(EntityPlayer player, NBTTagCompound data) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;

        NBTTagList list = new NBTTagList();
        for (int i = 0; i < handler.getSlots(); i++) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("Slot", (byte) i);
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.writeToNBT(tag);
            }
            list.appendTag(tag);
        }
        data.setTag(NBT_KEY, list);
    }

    /**
     * Set every Baubles slot to ItemStack.EMPTY. Call right after saveBaubles
     * so vanilla / Baubley Elytra / other PlayerDropsEvent handlers see empty
     * slots and drop nothing.
     */
    public static void clearBaubles(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public static void restoreBaubles(EntityPlayer player, NBTTagCompound data) {
        if (!data.hasKey(NBT_KEY)) return;

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;

        NBTTagList list = data.getTagList(NBT_KEY, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 0xFF;
            if (slot >= handler.getSlots()) continue;

            if (tag.hasKey("id")) {
                handler.setStackInSlot(slot, new ItemStack(tag));
            } else {
                handler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}
