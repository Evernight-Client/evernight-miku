package com.github.mikumiku.evernight.modules;

import com.github.mikumiku.evernight.MikuCategories;
import com.github.mikumiku.evernight.MikuModule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import evernight.client.events.core.EventHandler;
import evernight.client.events.ui.MinecraftTickEvent;
import evernight.client.settings.core.Setting;
import evernight.client.settings.types.BooleanSetting;
import evernight.client.settings.types.NumberSetting;
import evernight.client.settings.types.StringSetting;
import evernight.client.utils.chat.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Queries 3c3u.info playtime for players and flags newbies. Ported from Tanghulu's
 * {@code ThreeCNewbieCheck}; results are printed to chat (Evernight's name tag event does not expose
 * the vanilla text, so there is no name tag suffix yet).
 */
public final class ThreeCCheckModule extends MikuModule {
    private static final String ENDPOINT = "https://3c3u.info/api/getPlayer?name=";
    private static final long REQUEST_INTERVAL_MS = 2000L;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final Setting<String> player;
    private final Setting<Boolean> query;
    private final Setting<Integer> thresholdHours;
    private final Setting<Boolean> autoQuery;
    private final Setting<Double> range;
    private final Setting<Boolean> chatResult;

    private final Map<String, Long> results = new ConcurrentHashMap<>();
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean inFlight = new AtomicBoolean();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "miku-3c-query");
        thread.setDaemon(true);
        return thread;
    });

    private long lastRequest;
    private int scanCooldown;
    private ClientLevel lastLevel;

    public ThreeCCheckModule() {
        super("ThreeCCheck", "Looks up 3c3u.info playtime and flags newbies.",
                MikuCategories.MIKU, "miku3c", "3c");
        this.player = this.add(new StringSetting("Player", ""));
        this.query = this.add(new BooleanSetting("Query", false));
        this.thresholdHours = this.add(new NumberSetting<>("ThresholdHours", 24, 1, 240));
        this.autoQuery = this.add(new BooleanSetting("AutoQuery", false));
        this.range = this.add(new NumberSetting<>("AutoRange", 200.0, 8.0, 200.0));
        this.chatResult = this.add(new BooleanSetting("ChatResult", true));
    }

    @Override
    public void onEnable() {
        this.results.clear();
        this.pending.clear();
        this.queue.clear();
        this.scanCooldown = 0;
        this.lastLevel = this.inGame() ? mc().level : null;
    }

    @Override
    public void onDisable() {
        this.queue.clear();
    }

    @EventHandler
    private void onTick(MinecraftTickEvent event) {
        if (!this.inGame()) {
            return;
        }
        if (mc().level != this.lastLevel) {
            this.lastLevel = mc().level;
            this.results.clear();
            this.pending.clear();
            this.queue.clear();
        }
        if (this.query.getValue()) {
            this.query.setValue(false);
            String name = this.player.getValue();
            if (name != null && !name.isBlank()) {
                this.enqueue(name.trim());
            } else {
                this.warn("请先填写玩家名");
            }
        }
        if (this.autoQuery.getValue()) {
            if (this.scanCooldown > 0) {
                this.scanCooldown--;
            } else {
                this.scanCooldown = 20;
                this.scanNearby();
            }
        }
        this.pump();
    }

    private void scanNearby() {
        double maxDistance = this.range.getValue() * this.range.getValue();
        for (Player other : mc().level.players()) {
            if (other == mc().player || !other.isAlive()) {
                continue;
            }
            if (mc().player.distanceToSqr(other) > maxDistance) {
                continue;
            }
            String name = other.getName().getString();
            if (this.results.containsKey(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            this.enqueue(name);
        }
    }

    private void enqueue(String name) {
        String normalized = name.trim();
        if (!normalized.matches("\\|{0,2}[A-Za-z0-9_]{3,16}")) {
            return;
        }
        String key = normalized.toLowerCase(Locale.ROOT);
        if (this.results.containsKey(key) || this.pending.contains(key) || this.queue.contains(normalized)) {
            return;
        }
        this.queue.add(normalized);
    }

    private void pump() {
        if (this.queue.isEmpty() || this.inFlight.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastRequest < REQUEST_INTERVAL_MS) {
            return;
        }
        String name = this.queue.poll();
        if (name == null) {
            return;
        }
        String key = name.toLowerCase(Locale.ROOT);
        this.pending.add(key);
        this.inFlight.set(true);
        this.lastRequest = now;
        this.worker.submit(() -> {
            try {
                long seconds = this.fetch(name);
                mc().execute(() -> {
                    this.pending.remove(key);
                    this.inFlight.set(false);
                    this.results.put(key, seconds);
                    this.announce(name, seconds);
                });
            } catch (Exception exception) {
                mc().execute(() -> {
                    this.pending.remove(key);
                    this.inFlight.set(false);
                    this.warn("查询 " + name + " 失败：" + exception.getMessage());
                });
            }
        });
    }

    private long fetch(String name) throws Exception {
        URI uri = URI.create(ENDPOINT + URLEncoder.encode(name, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "MikuAddon/1.3.0 (ThreeCCheck)")
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 429) {
            throw new IllegalStateException("查询过于频繁，请稍后再试");
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("查询服务返回 HTTP " + response.statusCode());
        }
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        int code = root.get("code").getAsInt();
        if (code != 200) {
            String reason = root.has("msg") ? root.get("msg").getAsString() : "代码 " + code;
            throw new IllegalStateException("查询站未返回玩家数据：" + reason);
        }
        JsonObject data = root.getAsJsonObject("data");
        String returned = data.get("name").getAsString();
        if (!returned.equalsIgnoreCase(name)) {
            throw new IllegalStateException("查询站返回了其他玩家的数据");
        }
        return data.get("duration").getAsLong();
    }

    private void announce(String name, long seconds) {
        if (!this.chatResult.getValue()) {
            return;
        }
        boolean newbie = seconds < this.thresholdHours.getValue() * 3600L;
        String hours = String.format(Locale.ROOT, "%.1f", seconds / 3600.0);
        String verdict = newbie ? "萌新" : "高手";
        Component text = Component.literal(name + " 总在线 " + hours + " 小时 → " + verdict
                        + "（来源：3c3u.info）")
                .withStyle(newbie ? ChatFormatting.GREEN : ChatFormatting.RED);
        ChatUtils.printChat(text, ChatUtils.moduleSignature(this));
    }
}
