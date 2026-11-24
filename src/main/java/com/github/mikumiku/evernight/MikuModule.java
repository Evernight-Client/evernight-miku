package com.github.mikumiku.evernight;

import evernight.addon.AddonModule;
import evernight.client.modules.ModuleCategory;
import evernight.client.utils.chat.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Shared base class for every ported Miku module.
 *
 * <p>Module names are the stable English identifiers used by commands, config keys and presets.
 * Human readable (Chinese) labels live in {@code assets/mikumiku/lang/zh_cn.json} under
 * {@code addons.mikumiku.names.modules.<Name>}.
 */
public abstract class MikuModule extends AddonModule {
    protected MikuModule(String name, String description, ModuleCategory category, String... aliases) {
        super(MikuAddon.ID, name, description, category, aliases);
    }

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    /** Sends a client side chat line tagged with this module. */
    protected void info(String message) {
        ChatUtils.printChat(Component.literal(message), ChatUtils.moduleSignature(this));
    }

    /** Sends a warning line tagged with this module. */
    protected void warn(String message) {
        ChatUtils.printChat(Component.literal(message).withStyle(ChatFormatting.RED),
                ChatUtils.moduleSignature(this));
    }

    /** Sends a line only when the player is in a world. */
    protected boolean inGame() {
        Minecraft client = mc();
        return client.player != null && client.level != null;
    }
}
