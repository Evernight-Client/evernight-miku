package com.github.mikumiku.evernight;

import evernight.addon.AddonContext;
import evernight.client.modules.ModuleCategory;

/**
 * Miku specific ClickGUI categories, matching the category layout of the original Meteor addon.
 *
 * <p>Evernight 1.0.3 lets addons register their own categories. Registration must happen before the
 * modules are constructed, so {@link MikuAddon#onInitialize(AddonContext)} calls
 * {@link #register(AddonContext)} first. The fields start out as built-in categories: if the client
 * refuses a registration (for example because another addon already uses the name) the port keeps
 * working and only the grouping changes.
 */
public final class MikuCategories {
    public static ModuleCategory BUILD = ModuleCategory.PLAYER;
    public static ModuleCategory TWEAK = ModuleCategory.MOVEMENT;
    public static ModuleCategory MIKU = ModuleCategory.MISC;

    private MikuCategories() {
    }

    public static void register(AddonContext context) {
        BUILD = registerSafely(context, "MikuBuild", BUILD);
        TWEAK = registerSafely(context, "MikuTweak", TWEAK);
        MIKU = registerSafely(context, "Miku", MIKU);
    }

    private static ModuleCategory registerSafely(
            AddonContext context, String name, ModuleCategory fallback) {
        try {
            return context.registerCategory(name);
        } catch (Throwable throwable) {
            context.logger().warn("Could not register category {}, falling back to {}",
                    name, fallback.getName(), throwable);
            return fallback;
        }
    }
}
