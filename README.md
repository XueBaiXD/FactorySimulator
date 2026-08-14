# FactorySimulator｜工厂模拟器

FactorySimulator 是一个面向 **Paper 1.12.2** 的轻量 RPG 工厂经营插件。玩家可以创建自己的工厂世界，摆放并连接生产设备，出售产物赚取资金，升级工厂地皮与设备，逐步建设自动化生产线。

> 当前版本：`1.0.0`  
> 作者：`XueBaiXD`  
> 官网：`play.xuebai.xyz`

## 目录

- [插件特色](#插件特色)
- [运行环境](#运行环境)
- [安装与首次使用](#安装与首次使用)
- [命令与权限](#命令与权限)
- [生产线与设备](#生产线与设备)
- [PlaceholderAPI 变量](#placeholderapi-变量)
- [前置插件与软依赖](#前置插件与软依赖)
- [配置文件](#配置文件)
- [数据存储](#数据存储)
- [目录结构与备份](#目录结构与备份)
- [常见问题](#常见问题)

## 插件特色

- **专属工厂世界**：玩家创建工厂后拥有独立的工厂世界，世界名称格式为 `fs_<去除短横线后的玩家UUID>`。
- **预设工厂场地**：世界由插件按 `world.yml` 生成，包含地板、中心标记和边界墙，地皮大小由 `factory.yml` 控制。
- **流水线生产**：基础采矿机、矿物采矿机、传送带、熔炼机、装配机、仓储箱和自动售货机可以组合成生产线。
- **RPG 成长**：资金达到配置的升级条件后提升工厂等级，解锁更多设备并升级地皮规模。
- **离线收益**：玩家离线期间会根据配置记录离线收益，回到服务器后计入工厂数据。
- **工厂资料**：支持自定义工厂名称、工厂信息查询、设备数量和工人数等资料统计。
- **成就数据**：工厂资料保存成就集合，并可在消息与 PlaceholderAPI 中使用统计信息。
- **排行榜接口**：支持按资金和工厂等级计算玩家名次，便于接入全息、记分板或其他展示插件。
- **多种存储**：支持 YAML、SQLite 和 MySQL 三种存储方式。
- **软依赖设计**：PlaceholderAPI、Vault、HolographicDisplays、Multiverse-Core 均为软依赖，不安装时插件仍可启动核心功能。
- **中文/繁中/英文语言**：通过 `messages.yml` 选择 `zh_cn`、`zh_tw` 或 `en` 语言文件。

## 运行环境

### 必需环境

- Minecraft `1.12.2`
- Paper `1.12.2`（推荐使用 Paper，不建议使用原版 Spigot）
- Java `8` 或与当前服务端兼容的 Java 运行环境

### 可选前置

| 插件 | 用途 | 是否必需 |
| --- | --- | --- |
| PlaceholderAPI | 提供工厂变量，供记分板、TAB、聊天、全息等插件调用 | 否 |
| Vault | 经济生态兼容入口及状态识别 | 否 |
| HolographicDisplays | 全息展示生态兼容入口及状态识别 | 否 |
| Multiverse-Core | 多世界生态兼容入口及状态识别 | 否 |

插件已经在 `plugin.yml` 中将上述插件声明为 `softdepend`。缺少任意一个插件不会阻止 FactorySimulator 启动。

## 安装与首次使用

1. 将 `FactorySimulator.jar` 放入服务端的 `plugins/` 目录。
2. 启动一次服务器，等待插件生成默认配置和语言文件。
3. 如需使用变量展示，额外安装 PlaceholderAPI，并执行 `/papi ecloud download` 等方式安装所需的其他扩展。
4. 根据需要编辑 `plugins/FactorySimulator/` 下的配置文件。
5. 重启服务器，或使用 `/fs reload` 重新加载可热重载配置。
6. 玩家进入服务器后执行：

   ```text
   /fs create
   /fs enter
   ```

创建工厂时会获得默认启动设备、工厂拆卸镐和教程书。具体数量由 `factory.yml` 的 `starter` 节点控制。

### 推荐的新手流程

1. 使用 `/fs create` 创建工厂。
2. 使用 `/fs enter` 进入专属工厂世界。
3. 摆放基础采矿机、传送带和自动售货机。
4. 将采矿机接入传送带，并将传送带末端连接到自动售货机。
5. 等待生产并出售产物，使用 `/fs info` 查看资金、等级和设备数量。
6. 使用 `/fs buy <设备ID>` 购买更高级设备。
7. 使用 `/fs upgrade` 扩大地皮，继续扩建生产区域。

## 命令与权限

主命令为 `/fs`，别名为 `/factory` 和 `/factorysimulator`。`/factorysimulator` 也注册为独立命令入口，可用于执行主命令的查询功能。

### 玩家命令

| 命令 | 说明 |
| --- | --- |
| `/fs` | 查看帮助 |
| `/fs help` | 查看帮助 |
| `/fs version` | 查看插件版本 |
| `/fs create` | 创建自己的工厂 |
| `/fs enter` | 进入自己的工厂世界 |
| `/fs menu` | 查看教程菜单提示；教程内容通过教程书打开 |
| `/fs rename <工厂名称>` | 修改自己的工厂名称 |
| `/fs upgrade` | 升级工厂地皮 |
| `/fs buy <设备ID>` | 购买一件指定设备并放入背包 |
| `/fs info` | 查看自己的工厂信息 |
| `/fs info <玩家名或UUID>` | 查看指定玩家的工厂信息 |
| `/fs info server` | 查看服务器状态 |
| `/fs status` | 查看服务器状态 |
| `/fs server` | 查看服务器状态 |

### 管理员命令

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/fs reload` | 重新加载插件配置与语言 | `factorysimulator.admin` |
| `/fs machine give <设备ID> [数量]` | 给自己发放设备 | 通常建议仅管理员使用 |
| `/fs machine give <玩家> <设备ID> [数量]` | 给指定在线玩家发放设备 | 通常建议仅管理员使用 |

`factorysimulator.admin` 默认仅授予 OP。插件当前只在 `/fs reload` 中显式检查该权限；服务器管理员应通过权限插件或 OP 管理其他测试命令的使用权限。

## 生产线与设备

设备物品通过隐藏 Lore 标识识别。不要手动删除设备 Lore，否则插件可能无法识别该物品。

| 设备 ID | 名称 | 默认材质 | 默认价格 | 解锁等级 |
| --- | --- | --- | ---: | ---: |
| `basic_miner` | 基础采矿机 | `DISPENSER` | 100 | 1 |
| `coal_miner` | 煤炭采矿机 | `DROPPER` | 300 | 2 |
| `iron_miner` | 铁矿采矿机 | `DISPENSER` | 800 | 4 |
| `gold_miner` | 黄金采矿机 | `DISPENSER` | 1600 | 6 |
| `redstone_miner` | 红石采矿机 | `DROPPER` | 2200 | 7 |
| `diamond_miner` | 钻石采矿机 | `DISPENSER` | 5000 | 10 |
| `conveyor` | 传送带 | `HOPPER` | 50 | 1 |
| `smelter` | 工业熔炼机 | `FURNACE` | 500 | 2 |
| `assembler` | 自动装配机 | `DROPPER` | 1200 | 5 |
| `electric_furnace` | 电力熔炉 | `FURNACE` | 1800 | 6 |
| `fuel_generator` | 燃料发电机 | `REDSTONE_BLOCK` | 1500 | 6 |
| `storage` | 物流仓储箱 | `CHEST` | 350 | 2 |
| `seller` | 自动售货机 | `CHEST` | 250 | 2 |

价格与解锁等级都可以在 `factory.yml` 的 `shop.items.<设备ID>` 中调整。

### 推荐连接方式

```text
采矿机 → 传送带 → 物流仓储箱 → 传送带 → 自动售货机
铁矿采矿机 → 工业熔炼机 → 自动装配机 → 自动售货机
煤炭采矿机 → 传送带 → 物流仓储箱 → 自动售货机
```

- `conveyor` 用于连接采矿机、加工设备、仓储箱和售货机。
- `storage` 适合作为生产区与加工区之间的缓冲。
- `smelter`、`assembler`、`electric_furnace` 用于加工矿物或生产更高价值产物。
- `seller` 建议放在线路末端，用于自动出售工厂产出。
- `fuel_generator` 建议与采矿区、加工区分区摆放。

设备是否在拆除时掉落，以及设备物品标识由 `factory.yml` 的 `machines` 节点控制。工厂拆卸镐仅用于拆除自己工厂内的设备。

### 默认售卖价格

| 产物 | 默认价格 |
| --- | ---: |
| `iron_ore` | 20.0 |
| `coal` | 12.0 |
| `iron_ingot` | 35.0 |
| `gold_ore` | 45.0 |
| `gold_ingot` | 75.0 |
| `redstone` | 18.0 |
| `diamond` | 180.0 |
| `factory_product` | 120.0 |
| `advanced_component` | 300.0 |

基础产品价格由 `production.basic-sell-price` 控制，具体产物价格由 `production.sell-prices` 控制。

## PlaceholderAPI 变量

安装并启用 PlaceholderAPI，且 `features.yml` 中 `hooks.placeholderapi` 为 `true` 后，插件会注册扩展标识：

```text
factorysimulator
```

变量格式：

```text
%factorysimulator_<变量名>%
```

### 全部变量

| 变量 | 返回内容 |
| --- | --- |
| `%factorysimulator_money%` | 当前玩家工厂资金，保留两位小数 |
| `%factorysimulator_balance%` | `money` 的别名 |
| `%factorysimulator_level%` | 当前玩家工厂等级 |
| `%factorysimulator_factory_level%` | `level` 的别名 |
| `%factorysimulator_factory_name%` | 当前玩家工厂名称 |
| `%factorysimulator_name%` | `factory_name` 的别名 |
| `%factorysimulator_plot_size%` | 当前玩家地皮边长 |
| `%factorysimulator_size%` | `plot_size` 的别名 |
| `%factorysimulator_workers%` | 当前工人数 |
| `%factorysimulator_worker_count%` | `workers` 的别名 |
| `%factorysimulator_machines%` | 当前已放置设备数量 |
| `%factorysimulator_machine_count%` | `machines` 的别名 |
| `%factorysimulator_created%` | 是否已创建工厂，返回 `true` 或 `false` |
| `%factorysimulator_rank_money%` | 按资金排序的当前玩家名次 |
| `%factorysimulator_rank_level%` | 按工厂等级排序的当前玩家名次 |

### 使用示例

在支持 PlaceholderAPI 的记分板、TAB、聊天格式或全息插件中填写：

```text
资金：%factorysimulator_money%
等级：%factorysimulator_level%
工厂：%factorysimulator_factory_name%
地皮：%factorysimulator_plot_size%x%factorysimulator_plot_size%
工人：%factorysimulator_workers%
设备：%factorysimulator_machines%
资金排名：#%factorysimulator_rank_money%
```

使用 PlaceholderAPI 的 `/papi parse` 检查变量：

```text
/papi parse me %factorysimulator_money%
/papi parse me %factorysimulator_factory_name%
```

如果变量原样显示，依次检查 PlaceholderAPI 是否安装、插件是否启用、`features.yml` 是否开启 `hooks.placeholderapi`，以及重启服务器让软依赖重新检测。

## 前置插件与软依赖

### PlaceholderAPI

PlaceholderAPI 是最推荐安装的前置，用于把工厂数据展示到其他插件中。FactorySimulator 自带 `factorysimulator` 扩展，不需要另外下载同名扩展。

安装后：

1. 将 PlaceholderAPI 放入 `plugins/`。
2. 确认 `features.yml`：

   ```yaml
   hooks:
     placeholderapi: true
   ```

3. 重启服务器。
4. 使用上方变量配置记分板、TAB、聊天或全息插件。

### Vault

Vault 是经济插件的通用桥接前置。当前 FactorySimulator 的核心工厂资金由自身工厂资料和存储系统管理；Vault 在本版本中作为可选兼容入口和状态识别，不是创建工厂或生产线运行的硬性要求。

如服务器已有 Vault，可保持：

```yaml
hooks:
  vault: true
```

如不使用 Vault，可关闭该开关：

```yaml
hooks:
  vault: false
```

### HolographicDisplays

HolographicDisplays 用于服务器外部的全息展示生态。FactorySimulator 提供自身变量，管理员可以在 HolographicDisplays 的全息文本中直接使用 PlaceholderAPI 变量，例如：

```text
&b工厂排行榜
&f资金：%factorysimulator_money%
&f等级：%factorysimulator_level%
```

全息插件本身的创建、编辑和删除命令请以对应版本的 HolographicDisplays 文档为准。FactorySimulator 不要求必须安装该插件。

### Multiverse-Core

Multiverse-Core 是多世界管理生态的可选前置。FactorySimulator 会自行管理玩家工厂世界的生成和加载；Multiverse-Core 不是创建工厂世界的必需组件。安装 Multiverse-Core 后可继续使用其世界管理、传送和世界列表功能，但不要重复导入或手动删除 FactorySimulator 正在使用的工厂世界。

### 关闭可选接入

所有可选接入均可在 `plugins/FactorySimulator/features.yml` 关闭：

```yaml
hooks:
  placeholderapi: false
  vault: false
  holographic-displays: false
  multiverse-core: false
```

关闭后请重启服务器；部分软依赖状态是在插件启动时检测的。

## 配置文件

插件不会把所有配置堆在一个文件中，各文件职责如下：

| 文件 | 用途 |
| --- | --- |
| `config.yml` | 全局调试、资源预加载、运行库回退开关 |
| `factory.yml` | 初始资金、地皮、等级、设备商店、生产、工人、成就和机器设置 |
| `world.yml` | 工厂世界生成、环境、地板、墙壁和出生点设置 |
| `storage.yml` | YAML、SQLite、MySQL 存储配置 |
| `features.yml` | 软依赖开关和排行榜开关 |
| `messages.yml` | 当前语言和回退语言选择 |
| `lang/zh_cn.yml` | 简体中文消息 |
| `lang/zh_tw.yml` | 繁体中文消息 |
| `lang/en.yml` | 英文消息 |

### 常用 `factory.yml` 配置

```yaml
plot:
  initial-size: 32
  growth-per-level: 8
  max-size: 256
  upgrade-base-cost: 2500

economy:
  starting-money: 1000
  offline-income-minute-step: 5
  offline-income-rate: 0.25

production:
  tick-interval: 20
  basic-process-ticks: 100
  basic-sell-price: 8.0
```

- `plot.initial-size`：玩家创建工厂时的初始地皮边长。
- `plot.growth-per-level`：升级后增加的地皮边长。
- `plot.max-size`：地皮最大边长。
- `economy.starting-money`：初始资金。
- `economy.offline-income-minute-step`：离线收益结算时间粒度，单位为分钟。
- `economy.offline-income-rate`：离线收益倍率，`1.0` 表示基础收益，默认 `0.25`。
- `production.tick-interval`：生产扫描间隔，20 ticks 约等于 1 秒。
- `production.basic-process-ticks`：基础设备处理时间，100 ticks 约等于 5 秒。

### 语言与消息

`messages.yml` 只负责选择语言：

```yaml
language: zh_cn
fallback: en
```

所有实际文案位于 `lang/` 目录。消息中可以使用 `{prefix}` 代替统一前缀；其他常用消息占位符包括 `{label}`、`{target}`、`{type}`、`{amount}`、`{name}`、`{level}`、`{plotSize}`、`{money}`、`{workers}`、`{machines}`、`{offlineHours}` 和 `{achievements}`。

### 配置修改后的生效方式

- `/fs reload` 需要 `factorysimulator.admin` 权限。
- 修改语言、工厂数值、设备价格等配置后建议执行 `/fs reload`。
- 修改存储类型、数据库连接、世界生成方式或软依赖开关后建议完整重启服务器。
- 修改配置前请先备份原文件。

## 数据存储

存储类型在 `plugins/FactorySimulator/storage.yml` 中设置：

```yaml
storage:
  type: YAML
```

支持的值：

- `YAML`：适合单机、小型服务器和测试环境，每个玩家一份数据文件。
- `SQLITE`：适合单服长期运行，数据存储在插件目录中的 `.db` 文件。
- `MYSQL`：适合多人服务器或需要集中管理数据的环境。

### SQLite 示例

```yaml
storage:
  type: SQLITE
  sqlite:
    file: factorysimulator.db
```

### MySQL 示例

```yaml
storage:
  type: MYSQL
  mysql:
    host: 127.0.0.1
    port: 3306
    database: factorysimulator
    user: factorysimulator
    password: '请修改为数据库密码'
    params: useSSL=false&characterEncoding=utf8&autoReconnect=true&useUnicode=true
  pool-size: 8
```

使用 MySQL 前请先创建数据库，并确保服务器可以访问数据库主机。默认打包驱动面向 Java 8、MySQL 5.x 和 MariaDB 5.x；使用 MySQL 8 时请根据实际驱动和连接参数进行适配，不要直接混用不兼容的旧驱动配置。

### 切换存储注意事项

FactorySimulator 不建议直接修改 `storage.yml` 后期待数据自动从一种存储迁移到另一种存储。切换前应完整备份 `plugins/FactorySimulator/`，并根据实际情况完成数据迁移或保留原存储作为回滚副本。

## 目录结构与备份

正常运行后，插件数据目录大致如下：

```text
plugins/
└─ FactorySimulator/
   ├─ config.yml
   ├─ factory.yml
   ├─ features.yml
   ├─ messages.yml
   ├─ storage.yml
   ├─ world.yml
   ├─ lang/
   ├─ world/
   │  └─ <玩家专属工厂世界>/
   ├─ data/
   │  └─ <YAML玩家工厂数据文件>/
   └─ factorysimulator.db
```

- `world/`：玩家专属工厂世界，默认存放在插件目录内，不直接散落在服务端根目录。
- `data/`：YAML 存储时的玩家工厂资料。
- `*.db`：SQLite 存储时生成的数据库文件，文件名由 `storage.sqlite.file` 决定。
- MySQL 存储时，工厂资料保存在数据库表中。

建议在停服状态下备份整个 `plugins/FactorySimulator/` 目录；使用 MySQL 时还应同时备份数据库。

## 常见问题

### 玩家执行 `/fs enter` 提示先创建工厂

先执行 `/fs create`。`/fs enter` 不会替玩家自动创建工厂。

### PlaceholderAPI 变量没有解析

确认 PlaceholderAPI 已安装并启用，`features.yml` 中 `hooks.placeholderapi` 为 `true`，然后重启服务器。可以使用：

```text
/papi parse me %factorysimulator_money%
```

检查变量是否正常返回。

### 设备买不到

检查设备 ID 拼写、工厂等级和资金。设备价格及解锁等级位于 `factory.yml` 的 `shop.items` 节点。

### 设备被破坏后无法识别

不要删除设备 Lore 中的 `FS_MACHINE:<设备ID>` 标识。建议使用插件发放的工厂拆卸镐拆除设备。

### 改了数据库配置但没有生效

检查 `storage.type` 是否为大写的 `YAML`、`SQLITE` 或 `MYSQL`，确认数据库、账号和权限正确，并完整重启服务器。数据库切换前请备份原数据。

### 是否必须安装 Vault、全息或多世界插件

不必须。它们都是软依赖。只有需要对应生态功能时才安装；PlaceholderAPI 是使用工厂变量展示到其他插件时需要安装的前置。

## 反馈与问题报告

提交问题时请附上：

1. Minecraft/Paper 版本和 Java 版本。
2. FactorySimulator 版本。
3. 服务器启动日志中与 FactorySimulator 相关的内容。
4. 使用的存储类型及相关报错。
5. 复现问题的命令、操作步骤和配置片段。

请不要公开 MySQL 密码、服务器密钥或其他敏感信息。
