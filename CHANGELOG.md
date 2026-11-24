# Changelog

## 1.1.0 — 2026-11-24

适配 Evernight 1.0.3，并精简为「本体没有的功能」。

### 新增

- 注册 3 个插件分类：`Miku` / `Miku 建设` / `Miku 特调`（`AddonContext.registerCategory`）
- `PotCountHud`：溅射药水数量 + 抗性剩余时间
- 设置项 tooltip 的中英描述（99 条），修复显示原始语言键的问题

### 变更

- 依赖升级到 `evernight-1.0.3.jar`（`depends.evernight >= 1.0.3`）
- 移除 30 个与本体重复的模块（AutoCrystal、Aura、Velocity、Criticals、Scaffold、Nuker、
  NoFall、Phase、ElytraFly、Trident、KeyPearl、AutoLog、HoleFill、FeetPlace、SelfTrap、
  SourceFill、AutoMount、AutoPot/AutoEat、AutoExp、InventoryCleaner、Announcer、
  ChorusControl、FastFall、Blocker、AutoMace、Metrics/Position 等）
- 保留 18 个互补模块：鱼竿糊脸、死亡指令、自动登录、自动社交、看门狗、靓仔转圈、
  库存打印、翻箱倒柜、铺半砖+、自动种树、脚下挖掘、自动消防、悬停、自动鞘翅切换、
  无限耐久鞘翅、跟随玩家、自动居中、药水计数
- 源码树中不再提交第三方 jar 与映射表，改由 `tools/prepare.py` 生成

### 修复

- 重映射脚本 `named2int.tiny` 的成员列顺序错误导致运行时 `NoSuchMethodError`
- `tools/build.py` 的重映射 classpath 改用源命名空间 jar

## 1.0.0 — 2026-11-23

首个版本：从 `meteor-miku` 移植 46 个模块到 Evernight 1.0.1。
