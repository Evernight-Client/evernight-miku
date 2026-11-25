package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import evernight.client.ClientContext;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.render.esp.FeatureBoxRenderer;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.ColorSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.utils.world.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.awt.Color;

/** Highlights map-art item frames and banners. Ported from Tanghulu's {@code VanityESP}. */
public final class VanityEspModule extends MikuModule {
    private final Setting<Boolean> itemFrames;
    private final Setting<Boolean> banners;
    private final Setting<Double> range;
    private final Setting<Color> fillColor;
    private final Setting<Color> outlineColor;
    private final Setting<Boolean> fade;
    private final Setting<Float> fadeTime;

    private int cooldown;

    public VanityEspModule() {
        super("VanityESP", "Highlights map-art item frames and banners.", MikuCategories.TWEAK, "mikuvanity");
        this.itemFrames = this.add(new BooleanSetting("ItemFrames", true));
        this.banners = this.add(new BooleanSetting("Banners", true));
        this.range = this.add(new NumberSetting<>("Range", 48.0, 8.0, 128.0));
        this.fillColor = this.add(new ColorSetting("FillColor", new Color(255, 170, 0, 60)));
        this.outlineColor = this.add(new ColorSetting("OutlineColor", new Color(255, 170, 0, 200)));
        this.fade = this.add(new BooleanSetting("Fade", true));
        this.fadeTime = this.add(new NumberSetting<>("FadeTime", 1.5F, 0.1F, 5.0F));
        if (ClientContext.featureBoxRenderer != null) {
            ClientContext.featureBoxRenderer.register(new FeatureBoxRenderer.RenderConfiguration(
                    this, this.fillColor, this.outlineColor, () -> 1.2F, this.fadeTime,
                    () -> false, this.fade, 350));
        }
    }

    @Override
    public void onEnable() {
        this.cooldown = 0;
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame() || ClientContext.featureBoxRenderer == null) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        this.cooldown = 5;
        double maxDistance = this.range.getValue() * this.range.getValue();
        if (this.itemFrames.getValue()) {
            for (Entity entity : mc().level.entitiesForRendering()) {
                if (!(entity instanceof ItemFrame frame) || frame.isRemoved()) {
                    continue;
                }
                if (!frame.getItem().is(Items.FILLED_MAP)) {
                    continue;
                }
                if (mc().player.distanceToSqr(frame) > maxDistance) {
                    continue;
                }
                ClientContext.featureBoxRenderer.addBox(this, frame.getBoundingBox());
            }
        }
        if (this.banners.getValue()) {
            for (BlockEntity blockEntity : WorldUtils.getLoadedBlockEntities()) {
                if (!(blockEntity instanceof BannerBlockEntity)) {
                    continue;
                }
                BlockPos position = blockEntity.getBlockPos();
                if (mc().player.position().distanceToSqr(position.getCenter()) > maxDistance) {
                    continue;
                }
                ClientContext.featureBoxRenderer.addBox(this, new AABB(position));
            }
        }
    }
}
