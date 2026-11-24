package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.NumberSetting;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Swaps between elytra and chestplate automatically. Ported from {@code ElytraArmorSwitch}. */
public final class ElytraArmorSwitchModule extends MikuModule {
    private final Setting<Integer> delay;

    private long lastSwap;

    public ElytraArmorSwitchModule() {
        super("ElytraArmorSwitch", "Wears a chestplate on the ground and an elytra in the air.",
                MikuCategories.TWEAK, "mikuelytraswitch");
        this.delay = this.add(new NumberSetting<>("Delay", 200, 0, 2000));
    }

    @Override
    public void onEnable() {
        this.lastSwap = 0L;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame() || System.currentTimeMillis() - this.lastSwap < this.delay.getValue()) {
            return;
        }
        ItemStack chest = mc().player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        boolean wearingElytra = chest.is(Items.ELYTRA);
        boolean shouldFly = !mc().player.onGround() || mc().player.isFallFlying();
        if (shouldFly && !wearingElytra) {
            this.swapArmor(Items.ELYTRA);
        } else if (!shouldFly && wearingElytra) {
            this.swapArmor(null);
        }
    }

    private void swapArmor(net.minecraft.world.item.Item wanted) {
        int hotbar = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc().player.getInventory().getItem(slot);
            if (wanted != null && stack.is(wanted)) {
                hotbar = slot;
                break;
            }
            if (wanted == null && !stack.isEmpty() && !stack.is(Items.ELYTRA)
                    && evernight.client.utils.inventory.InventoryUtils.canEquip(
                            net.minecraft.world.entity.EquipmentSlot.CHEST, stack)) {
                hotbar = slot;
                break;
            }
        }
        if (hotbar == -1) {
            return;
        }
        mc().gameMode.handleInventoryMouseClick(mc().player.inventoryMenu.containerId, 6, hotbar,
                ClickType.SWAP, mc().player);
        this.lastSwap = System.currentTimeMillis();
    }
}
