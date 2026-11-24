package com.github.mikumiku.evernight.modules.hud;

import com.github.mikumiku.evernight.MikuHudModule;
import evernight.client.modules.hud.HudTextListRenderer;
import evernight.client.modules.hud.HudTextRow;
import evernight.client.settings.core.Setting;
import evernight.client.settings.options.HudAnchor;
import evernight.client.settings.types.BooleanSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows splash potion count and resistance time. Ported from {@code PotCountHud}. */
public final class PotCountHudModule extends MikuHudModule {
    private final Setting<Boolean> showSplashPotCount;
    private final Setting<Boolean> showResistanceTime;

    public PotCountHudModule() {
        super("PotCountHud", "mikupot");
        this.showSplashPotCount = this.add(new BooleanSetting("ShowSplashPotCount", true));
        this.showResistanceTime = this.add(new BooleanSetting("ShowResistanceTime", true));

        List<HudTextRow> rows = new ArrayList<>(2);
        rows.add(new HudTextRow(this::potionText, () -> this.showSplashPotCount.getValue()));
        rows.add(new HudTextRow(this::resistanceText, () -> this.showResistanceTime.getValue()));
        this.setRenderer(new HudTextListRenderer(this, rows));
        this.getRenderer().setAnchor(HudAnchor.TOP_RIGHT);
        this.setToggled(true);
    }

    private Component potionText() {
        return Component.literal("POT: " + this.countSplashPotions());
    }

    private Component resistanceText() {
        float seconds = this.resistanceSeconds();
        String text = seconds > 0.0F
                ? String.format(Locale.ROOT, "Time: %.1fs", seconds)
                : "Time: --";
        return Component.literal(text);
    }

    private int countSplashPotions() {
        if (mc().player == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc().player.getInventory().getItem(slot);
            if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private float resistanceSeconds() {
        if (mc().player == null) {
            return 0.0F;
        }
        MobEffectInstance effect = mc().player.getEffect(MobEffects.RESISTANCE);
        if (effect == null) {
            return 0.0F;
        }
        return effect.getDuration() / 20.0F;
    }
}
