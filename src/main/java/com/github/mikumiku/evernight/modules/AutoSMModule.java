package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.modules.ModuleCategory;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.utils.chat.ChatUtils;

/** Sends social messages on a timer. Ported from {@code AutoSM}. */
public final class AutoSMModule extends MikuModule {
    private final Setting<Integer> interval;
    private final Setting<Boolean> onlyWhenPlayersNearby;

    private int ticks;
    private int index;

    private static final String[] MESSAGES = {
            "miku miku oo ee oo",
            "今天也是元气满满的一天",
            "有人一起玩吗",
            "这个服务器真好玩",
            "ヾ(≧▽≦*)o",
    };

    public AutoSMModule() {
        super("AutoSM", "Sends a friendly chat message every now and then.", MikuCategories.MIKU, "mikusm");
        this.interval = this.add(new NumberSetting<>("Interval", 600, 20, 6000));
        this.onlyWhenPlayersNearby = this.add(new BooleanSetting("OnlyWhenPlayersNearby", true));
    }

    @Override
    public void onEnable() {
        this.ticks = 0;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (this.onlyWhenPlayersNearby.getValue() && mc().level.players().size() <= 1) {
            return;
        }
        if (++this.ticks < this.interval.getValue()) {
            return;
        }
        this.ticks = 0;
        ChatUtils.sendChatOrCommand(MESSAGES[this.index++ % MESSAGES.length]);
    }
}
