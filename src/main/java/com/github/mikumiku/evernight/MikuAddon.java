package com.github.mikumiku.evernight;

import evernight.addon.AddonContext;
import evernight.addon.EvernightAddon;

/**
 * Entry point of the Miku addon for Evernight.
 *
 * <p>Ports the Meteor Client addon "meteor-miku" to the Evernight module framework: every module
 * keeps its original behaviour and settings, but is rebuilt on Evernight's settings, events and
 * helpers.
 */
public final class MikuAddon extends EvernightAddon {
    public static final String ID = "mikumiku";

    @Override
    public void onInitialize(AddonContext context) {
        // Categories must exist before the modules are constructed: Module keeps the exact
        // ModuleCategory instance it was built with.
        MikuCategories.register(context);
        MikuModules.register(context);
        context.registerCommand(new MikuCommand());
    }
}
