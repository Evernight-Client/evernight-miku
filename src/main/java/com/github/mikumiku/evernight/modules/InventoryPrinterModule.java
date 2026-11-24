package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import net.minecraft.world.item.ItemStack;

/** Prints the inventory contents to chat. Ported from {@code InventoryPrinter}. */
public final class InventoryPrinterModule extends MikuModule {
    private int ticks;

    public InventoryPrinterModule() {
        super("InventoryPrinter", "Prints your inventory to chat.", MikuCategories.MIKU, "mikuinvprint");
    }

    @Override
    public void onEnable() {
        this.ticks = 0;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame() || this.ticks++ > 1) {
            return;
        }
        this.info("--- Inventory ---");
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc().player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String area = slot < 9 ? "Hotbar " + slot : "Slot " + slot;
            this.info(area + ": " + stack.getHoverName().getString() + " x" + stack.getCount());
        }
        this.disable();
    }
}
