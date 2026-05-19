package com.flashminat0.selectivekeepinv;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.items.IItemHandlerModifiable;
import xzeroair.trinkets.capabilities.InventoryContainerCapability.ITrinketContainerHandler;
import xzeroair.trinkets.capabilities.InventoryContainerCapability.TrinketContainerProvider;

/**
 * Trinkets and Baubles (modid "xat") soft-dependency helper. Every reference
 * to xzeroair.trinkets.* lives here so the class is never loaded (and never
 * triggers ClassNotFoundException) when Trinkets is absent. Callers must gate
 * access with Loader.isModLoaded("xat") before calling any method.
 */
public class TrinketsCompat {

    static final String NBT_KEY = "Trinkets";

    public static void saveTrinkets(EntityPlayer player, NBTTagCompound data) {
        ITrinketContainerHandler handler = player.getCapability(
                TrinketContainerProvider.containerCap, null);
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
     * Set every Trinkets slot to ItemStack.EMPTY. Call right after saveTrinkets
     * so Trinkets' own PlayerDropsEvent handler sees empty slots and drops
     * nothing.
     */
    public static void clearTrinkets(EntityPlayer player) {
        ITrinketContainerHandler handler = player.getCapability(
                TrinketContainerProvider.containerCap, null);
        if (handler == null) return;
        for (int i = 0; i < handler.getSlots(); i++) {
            ((IItemHandlerModifiable) handler).setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public static void restoreTrinkets(EntityPlayer player, NBTTagCompound data) {
        if (!data.hasKey(NBT_KEY)) return;

        ITrinketContainerHandler handler = player.getCapability(
                TrinketContainerProvider.containerCap, null);
        if (handler == null) return;

        NBTTagList list = data.getTagList(NBT_KEY, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 0xFF;
            if (slot >= handler.getSlots()) continue;

            if (tag.hasKey("id")) {
                ((IItemHandlerModifiable) handler).setStackInSlot(slot, new ItemStack(tag));
            } else {
                ((IItemHandlerModifiable) handler).setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}
