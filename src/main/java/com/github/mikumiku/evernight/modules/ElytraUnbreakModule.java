package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.utils.inventory.InventoryUtils;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Swaps a damaged elytra for a fresh one before it breaks. Ported from {@code ElytraUnbreak}. */
public final class ElytraUnbreakModule extends MikuModule {
    private final Setting<Integer> threshold;
    private final Setting<Integer> delay;
    private final Setting<Boolean> autoFirework;
    private final Setting<Integer> fireworkInterval;

    private long lastSwap;
    private long lastFirework;

    public ElytraUnbreakModule() {
        super("ElytraUnbreak", "Swaps elytras to avoid durability loss and boosts with fireworks.",
                MikuCategories.TWEAK, "mikuelytraunbreak");
        this.threshold = this.add(new NumberSetting<>("Threshold", 10, 1, 100));
        this.delay = this.add(new NumberSetting<>("Delay", 200, 0, 2000));
        this.autoFirework = this.add(new BooleanSetting("AutoFirework", false));
        this.fireworkInterval = this.add(new NumberSetting<>("FireworkInterval", 1000, 200, 5000));
    }

    @Override
    public void onEnable() {
        this.lastSwap = 0L;
        this.lastFirework = 0L;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (this.autoFirework.getValue() && mc().player.isFallFlying()
                && System.currentTimeMillis() - this.lastFirework >= this.fireworkInterval.getValue()) {
            this.useFirework();
        }
        ItemStack chest = mc().player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA)) {
            return;
        }
        int remaining = chest.getMaxDamage() - chest.getDamageValue();
        if (remaining > this.threshold.getValue()) {
            return;
        }
        if (System.currentTimeMillis() - this.lastSwap < this.delay.getValue()) {
            return;
        }
        int replacement = this.findElytra();
        if (replacement == -1) {
            this.warn("No spare elytra found.");
            return;
        }
        int hotbar = InventoryUtils.findHotbarSlot(Items.ELYTRA);
        if (hotbar != -1) {
            mc().gameMode.handleInventoryMouseClick(mc().player.inventoryMenu.containerId, 6, hotbar,
                    ClickType.SWAP, mc().player);
            this.lastSwap = System.currentTimeMillis();
        }
    }

    private int findElytra() {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc().player.getInventory().getItem(slot);
            if (stack.is(Items.ELYTRA) && stack.getMaxDamage() - stack.getDamageValue() > this.threshold.getValue()) {
                return slot;
            }
        }
        return -1;
    }

    private void useFirework() {
        int hotbar = InventoryUtils.findHotbarSlot(Items.FIREWORK_ROCKET);
        if (hotbar == -1) {
            return;
        }
        mc().player.getInventory().setSelectedSlot(hotbar);
        mc().gameMode.useItem(mc().player, net.minecraft.world.InteractionHand.MAIN_HAND);
        this.lastFirework = System.currentTimeMillis();
    }
}
