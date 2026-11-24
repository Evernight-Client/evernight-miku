package com.github.mikumiku.evernight;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import evernight.client.commands.Command;
import evernight.client.utils.chat.ChatUtils;
import net.minecraft.network.chat.Component;

/** {@code ;miku} - shows port information for the addon. */
public final class MikuCommand extends Command {
    public MikuCommand() {
        super("miku");
        this.setAliases("mikumiku");
    }

    @Override
    public void exec(LiteralArgumentBuilder root) {
        root.executes(context -> {
            ChatUtils.printChat(Component.literal("Miku Addon for Evernight - " + MikuModules.count() + " modules loaded."),
                    ChatUtils.DEFAULT_SIGNATURE);
            return 1;
        });
    }
}
