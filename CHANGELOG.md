# Changelog

## 1.1.0 — 2026-11-24

适配 Evernight 1.0.3。

### 新增

- 3 个插件分类：`Miku` / `Miku 建设` / `Miku 特调`（`AddonContext.registerCategory`）
- `PotCountHud`：溅射药水数量 + 抗性剩余时间
- 设置项 tooltip 的中英描述，修复显示原始语言键的问题

### 变更

- 依赖升级到 `evernight-1.0.3.jar`（`depends.evernight >= 1.0.3`）
- 模块集合：18 个，覆盖建造、移动特调、实用工具与 HUD
- 源码树不再提交第三方 jar 与映射表，改由 `tools/prepare.py` 生成

### 修复

- 重映射脚本 `named2int.tiny` 的成员列顺序错误导致运行时 `NoSuchMethodError`
- `tools/build.py` 的重映射 classpath 改用源命名空间 jar

## 1.0.0 — 2026-11-23

首个版本：对接 Evernight 1.0.1。
