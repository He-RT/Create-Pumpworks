# Create: Pumpworks — v1.0 完整实施蓝图

> **本文档是给 AI 编码助手（或人类开发者）的一步步操作指南。**
> **即使你不理解 Minecraft 模组开发，只要按照本文档的每一步执行，就能构建出可运行的模组。**
>
> **前置要求**: Java 21, NeoForge MDK, Create 6.0.10 源码（作为参考）
> **设计规格**: `docs/superpowers/specs/2026-06-07-create-pumpworks-design.md`

---

## 目录

- [Phase 0: 项目骨架搭建](#phase-0-项目骨架搭建)
- [Phase 1: 模组初始化与注册系统](#phase-1-模组初始化与注册系统)
- [Phase 2: 流体图引擎（纯逻辑，无 MC 依赖）](#phase-2-流体图引擎纯逻辑无-mc-依赖)
- [Phase 3: 管道方块](#phase-3-管道方块)
- [Phase 4: 水泵方块](#phase-4-水泵方块)
- [Phase 5: 储罐方块](#phase-5-储罐方块)
- [Phase 6: 阀门方块](#phase-6-阀门方块)
- [Phase 7: 防护设备](#phase-7-防护设备)
- [Phase 8: 水锤系统](#phase-8-水锤系统)
- [Phase 9: 仪表方块](#phase-9-仪表方块)
- [Phase 10: 流体适配器（Create 桥接）](#phase-10-流体适配器create-桥接)
- [Phase 11: 汽蚀系统](#phase-11-汽蚀系统)
- [Phase 12: UI 与信息显示](#phase-12-ui-与信息显示)
- [Phase 13: Aeronautics 兼容性](#phase-13-aeronautics-兼容性)
- [Phase 14: 配方与语言文件](#phase-14-配方与语言文件)
- [Phase 15: 测试与调试](#phase-15-测试与调试)
- [附录 A: 关键 API 参考](#附录-a-关键-api-参考)
- [附录 B: 常见坑与排错](#附录-b-常见坑与排错)
- [附录 C: 文件清单](#附录-c-文件清单)

---

## Phase 0: 项目骨架搭建

### 目标
创建一个能编译、能启动 MC 的最小 NeoForge 模组项目。

### Step 0.1: 创建目录结构

```
create-pumpworks/                       # 项目根目录（即 /Users/hert/Projects/pump/）
├── build.gradle
├── gradle.properties
├── settings.gradle.kts
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── pumpworks/
        └── resources/
            ├── META-INF/
            │   └── neoforge.mods.toml
            ├── assets/
            │   └── pumpworks/
            │       ├── blockstates/
            │       ├── lang/
            │       ├── models/
            │       │   ├── block/
            │       │   └── item/
            │       └── textures/
            │           ├── block/
            │           └── item/
            └── data/
                └── pumpworks/
                    └── recipe/
```

### Step 0.2: Gradle Wrapper

从 NeoForge MDK 模板获取 Gradle Wrapper 文件，或使用以下命令生成：

```bash
# 如果本地有 gradle：
gradle wrapper --gradle-version 8.8

# 或从 NeoForge MDK 复制 gradle/, gradlew, gradlew.bat
```

**gradle-wrapper.properties** (`gradle/wrapper/gradle-wrapper.properties`):
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### Step 0.3: settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.neoforged.net/releases") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "create-pumpworks"
```

### Step 0.4: gradle.properties

```properties
# Gradle
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.caching=true

# Mod Info
mod_id=pumpworks
mod_name=Create: Pumpworks
mod_version=0.1.0
mod_group_id=com.pumpworks
mod_author=YourName
mod_description=Realistic water pumps and pumping stations for Create
mod_license=MIT

# Minecraft & NeoForge
minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.22)
neo_version=21.1.219
neo_version_range=[21.1,)
loader_version_range=[4,)

# Mappings
parchment_minecraft_version=1.21.1
parchment_mappings_version=2024.11.17-1.21.1

# Dependencies
create_version=6.0.10-280
flywheel_version=1.0.6
registrate_version=MC1.21-1.3.0+67
ponder_version=1.0.82
```

### Step 0.5: build.gradle

**这是最关键的文件。** 参考 Create 官方和 Aeronautics 的构建配置：

```groovy
plugins {
    id 'java'
    id 'idea'
    id 'net.neoforged.moddev' version '2.0.141'
}

version = mod_version
group = mod_group_id

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = "https://maven.createmod.net" }           // Create, Ponder, Flywheel
    maven { url = "https://maven.ithundxr.dev/snapshots" }  // Registrate
    maven { url = "https://raw.githubusercontent.com/Fuzss/modresources/main/maven" } // ForgeConfigAPIPort
    maven { url = "https://api.modrinth.com/maven" }        // Modrinth mods
    maven { url = "https://maven.ryanhcode.dev/releases" }   // Sable (Aeronautics physics)
}

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = project.neo_version

    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }

    runs {
        client {
            client()
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }
        server {
            server()
            programArgument '--nogui'
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }
        data {
            data()
            programArguments.addAll '--mod', project.mod_id,
                '--all',
                '--output', file('src/generated/resources/').absolutePath,
                '--existing', file('src/main/resources/').absolutePath
        }

        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            loggingLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
        }
    }
}

// Generated resources
sourceSets.main.resources {
    srcDir 'src/generated/resources'
}

configurations {
    // 让 Create 的依赖传递到运行时
    runtimeClasspath.extendsFrom localRuntime
}

dependencies {
    // === Create (compileOnly = 编译时有 API，运行时由玩家提供) ===
    compileOnly("com.simibubi.create:create-${minecraft_version}:${create_version}")

    // === Registrate (Create 的注册辅助库) ===
    implementation("com.tterrag.registrate:Registrate:${registrate_version}")

    // === Sable (Aeronautics 物理引擎, 软依赖, 编译时可用但运行时可选) ===
    compileOnly("maven.modrinth:sable:1.2.1")
    // 注意: Sable 的 maven 坐标可能需要根据实际发布调整
    // 如果找不到, 可以暂时注释掉这行, 后续 Phase 13 再添加

    // === 测试 ===
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

idea {
    module {
        downloadSources = true
        downloadJavadoc = true
    }
}
```

**⚠️ 重要注意事项**:
- Create 用 `compileOnly` 而非 `implementation`，因为运行时 Create 由玩家自己安装
- 如果 Sable 的 maven 坐标找不到，Phase 13 再处理，先注释掉
- 运行 `./gradlew build` 验证编译通过后再继续

### Step 0.6: neoforge.mods.toml

`src/main/resources/META-INF/neoforge.mods.toml`:

```toml
modLoader = "javafml"
loaderVersion = "${loader_version_range}"
license = "${mod_license}"

[[mods]]
modId = "${mod_id}"
version = "${version}"
displayName = "${mod_name}"
authors = "${mod_author}"
description = '''${mod_description}'''

# NeoForge 依赖 (必须)
[[dependencies.${mod_id}]]
modId = "neoforge"
type = "required"
versionRange = "${neo_version_range}"
ordering = "NONE"
side = "BOTH"

# Minecraft 依赖 (必须)
[[dependencies.${mod_id}]]
modId = "minecraft"
type = "required"
versionRange = "${minecraft_version_range}"
ordering = "NONE"
side = "BOTH"

# Create 依赖 (必须)
[[dependencies.${mod_id}]]
modId = "create"
type = "required"
versionRange = "[6.0.10,)"
ordering = "AFTER"
side = "BOTH"

# Sable/Aeronautics 依赖 (可选)
[[dependencies.${mod_id}]]
modId = "sable"
type = "optional"
versionRange = "[1.2.1,)"
ordering = "AFTER"
side = "BOTH"
```

### Step 0.7: 验证骨架

```bash
cd /Users/hert/Projects/pump
./gradlew build
```

**预期结果**: BUILD SUCCESSFUL（可能有警告但无错误）

**如果失败**:
- 检查 Java 版本: `java -version` 应为 21+
- 检查网络/代理: Gradle 需要下载依赖
- 检查 NeoForge 版本: 去 https://projects.neoforged.net/neoforged/neoforge 确认版本号

---

## Phase 1: 模组初始化与注册系统

### 目标
创建模组主类、Registrate 实例、以及所有注册容器类。

### Step 1.1: 主类 `PumpworksMod.java`

路径: `src/main/java/com/pumpworks/PumpworksMod.java`

```java
package com.pumpworks;

import com.pumpworks.foundation.config.PumpworksConfig;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(PumpworksMod.ID)
public class PumpworksMod {

    public static final String ID = "pumpworks";
    public static final String NAME = "Create: Pumpworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Registrate 实例 —— 所有方块/物品/BlockEntity 都通过它注册
    // 参考 Create 的 CreateRegistrate 模式，我们使用标准的 Registrate
    private static final Registrate REGISTRATE = Registrate.create(ID);

    public PumpworksMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing {}", NAME);

        // 注册配置
        modContainer.registerConfig(ModConfig.Type.SERVER, PumpworksConfig.SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, PumpworksConfig.CLIENT_SPEC);

        // 注册 Registrate 的事件监听器
        REGISTRATE.registerEventListeners(modEventBus);

        // 触发所有注册类的静态初始化
        // 这些类的 static {} 块会调用 REGISTRATE.block() 等方法
        AllBlocks.register();
        AllItems.register();
        AllBlockEntityTypes.register();
        AllCreativeTabs.register();

        // 通用设置
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册 contraption movement settings (Aeronautics 兼容)
            CompatSetup.registerContraptionMovement();
        });
    }

    public static Registrate registrate() {
        return REGISTRATE;
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
```

**⚠️ 注意**: Create 使用自定义的 `CreateRegistrate`，但作为附属模组，我们直接用标准的 `Registrate`。这完全兼容——`CreateRegistrate` 只是加了一些便利方法。

### Step 1.2: 注册容器类模板

所有注册类遵循同一模式：一个 `register()` 静态方法触发类加载。

#### `AllBlocks.java`

路径: `src/main/java/com/pumpworks/AllBlocks.java`

```java
package com.pumpworks;

import com.tterrag.registrate.util.entry.BlockEntry;

// 此文件将逐步添加方块注册
// 每个 Phase 会告诉你在此文件添加什么

public class AllBlocks {

    // ===== 泵 =====
    // Phase 4 添加:
    // public static final BlockEntry<CentrifugalPumpBlock> CENTRIFUGAL_PUMP = ...
    // public static final BlockEntry<AxialPumpBlock> AXIAL_PUMP = ...
    // public static final BlockEntry<MixedFlowPumpBlock> MIXED_FLOW_PUMP = ...

    // ===== 管道 =====
    // Phase 3 添加

    // ===== 阀门 =====
    // Phase 6 添加

    // ===== 储罐 =====
    // Phase 5 添加

    // ===== 防护设备 =====
    // Phase 7 添加

    // ===== 仪表 =====
    // Phase 9 添加

    // ===== 适配器 =====
    // Phase 10 添加

    public static void register() {
        // 此方法存在的唯一目的是触发类加载
        // 当此类被加载时，上面的 static final 字段会依次执行注册
    }
}
```

#### `AllItems.java`

路径: `src/main/java/com/pumpworks/AllItems.java`

```java
package com.pumpworks;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class AllItems {

    // Phase 4 添加叶轮物品
    // Phase 14 添加管道扳手、性能图表

    public static void register() {}
}
```

#### `AllBlockEntityTypes.java`

路径: `src/main/java/com/pumpworks/AllBlockEntityTypes.java`

```java
package com.pumpworks;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class AllBlockEntityTypes {

    // 每个方块类型注册后，在这里添加对应的 BlockEntity 类型
    // Phase 3+ 逐步添加

    public static void register() {}
}
```

#### `AllCreativeTabs.java`

路径: `src/main/java/com/pumpworks/AllCreativeTabs.java`

```java
package com.pumpworks;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AllCreativeTabs {

    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PumpworksMod.ID);

    public static final RegistryEntry<CreativeModeTab> MAIN_TAB = TABS.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.pumpworks.main"))
            // iconSupplier 在 AllBlocks 有内容后设置
            .displayItems((params, output) -> {
                // 在 Phase 14 填充物品列表
            })
            .build());

    public static void register() {
        // DeferredRegister 需要在 mod event bus 上注册
        // 但 Registrate 模式下由 REGISTRATE.registerEventListeners 处理
        // 这里用另一种方式：
    }
}
```

**⚠️ Creative Tab 注册方式**: Registrate 和 DeferredRegister 两种方式不能混用。如果你用 Registrate 注册方块，Creative Tab 也需要通过 Registrate 或手动在事件监听中注册。后面 Phase 14 会给出完整方案。

### Step 1.3: 客户端类 `PumpworksClient.java`

路径: `src/main/java/com/pumpworks/PumpworksClient.java`

```java
package com.pumpworks;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = PumpworksMod.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PumpworksClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 客户端渲染器注册将在后续 Phase 中添加
            PumpworksMod.LOGGER.info("Pumpworks client setup complete");
        });
    }
}
```

### Step 1.4: 配置类

路径: `src/main/java/com/pumpworks/foundation/config/PumpworksConfig.java`

```java
package com.pumpworks.foundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class PumpworksConfig {

    // ========== SERVER CONFIG ==========
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    // ========== CLIENT CONFIG ==========
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CLIENT = new Client(builder);
        CLIENT_SPEC = builder.build();
    }

    public static class Server {
        // 流体网络
        public final ModConfigSpec.IntValue maxNodesPerComponent;
        public final ModConfigSpec.IntValue calcIntervalLargeNetwork;
        public final ModConfigSpec.IntValue largeNetworkThreshold;

        // 水锤
        public final ModConfigSpec.BooleanValue enableWaterHammer;
        public final ModConfigSpec.DoubleValue waterHammerDecay;
        public final ModConfigSpec.IntValue waveSpeedBlocksPerTick;
        public final ModConfigSpec.BooleanValue enablePipeDamage;

        // 汽蚀
        public final ModConfigSpec.BooleanValue enableCavitation;
        public final ModConfigSpec.DoubleValue cavitationSeverityMultiplier;

        // 应力
        public final ModConfigSpec.DoubleValue stressFactor;

        // 管道耐压
        public final ModConfigSpec.DoubleValue copperPipeMaxPressure;
        public final ModConfigSpec.DoubleValue ironPipeMaxPressure;
        public final ModConfigSpec.DoubleValue steelPipeMaxPressure;

        public Server(ModConfigSpec.Builder builder) {
            builder.push("fluid_network");
            maxNodesPerComponent = builder
                .comment("Maximum nodes in a single connected fluid network")
                .defineInRange("maxNodesPerComponent", 256, 16, 1024);
            calcIntervalLargeNetwork = builder
                .comment("Calculation interval (in ticks) for large networks")
                .defineInRange("calcIntervalLargeNetwork", 2, 1, 10);
            largeNetworkThreshold = builder
                .comment("Node count threshold to be considered a 'large' network")
                .defineInRange("largeNetworkThreshold", 64, 8, 256);
            builder.pop();

            builder.push("water_hammer");
            enableWaterHammer = builder
                .comment("Enable water hammer simulation")
                .define("enableWaterHammer", true);
            waterHammerDecay = builder
                .comment("Pressure wave decay per pipe segment (0.0-1.0)")
                .defineInRange("waterHammerDecay", 0.90, 0.5, 1.0);
            waveSpeedBlocksPerTick = builder
                .comment("How many pipe segments the pressure wave travels per tick")
                .defineInRange("waveSpeedBlocksPerTick", 3, 1, 8);
            enablePipeDamage = builder
                .comment("Whether pipes can be damaged by overpressure")
                .define("enablePipeDamage", true);
            builder.pop();

            builder.push("cavitation");
            enableCavitation = builder
                .comment("Enable cavitation detection in pumps")
                .define("enableCavitation", true);
            cavitationSeverityMultiplier = builder
                .comment("Multiplier for cavitation severity effects")
                .defineInRange("cavitationSeverityMultiplier", 1.0, 0.1, 5.0);
            builder.pop();

            builder.push("stress");
            stressFactor = builder
                .comment("Multiplier for pump stress consumption")
                .defineInRange("stressFactor", 1.0, 0.1, 10.0);
            builder.pop();

            builder.push("pipes");
            copperPipeMaxPressure = builder
                .comment("Maximum pressure (in blocks of water head) for copper pipes")
                .defineInRange("copperPipeMaxPressure", 64.0, 8.0, 512.0);
            ironPipeMaxPressure = builder
                .comment("Maximum pressure for iron pipes")
                .defineInRange("ironPipeMaxPressure", 128.0, 16.0, 1024.0);
            steelPipeMaxPressure = builder
                .comment("Maximum pressure for steel pipes")
                .defineInRange("steelPipeMaxPressure", 256.0, 32.0, 2048.0);
            builder.pop();
        }
    }

    public static class Client {
        public final ModConfigSpec.BooleanValue showDebugOverlay;

        public Client(ModConfigSpec.Builder builder) {
            builder.push("debug");
            showDebugOverlay = builder
                .comment("Show fluid network debug overlay")
                .define("showDebugOverlay", false);
            builder.pop();
        }
    }
}
```

### Step 1.5: 验证

```bash
./gradlew build
```

**预期**: BUILD SUCCESSFUL

**然后测试启动**:
```bash
./gradlew runClient
```

Minecraft 应该能启动，在模组列表中看到 "Create: Pumpworks"（还没有任何物品/方块）。

---

## Phase 2: 流体图引擎（纯逻辑，无 MC 依赖）

### 目标
实现流体网络的核心计算引擎。**这是纯 Java 逻辑，不依赖 Minecraft 任何类**，可以单独单元测试。

### 设计原理

流体网络是一个**有向图**：
- **节点 (FluidNode)**: 代表一个方块位置（管道、泵、储罐等）
- **边 (FluidEdge)**: 代表两个相邻方块之间的流体通道
- **图 (FluidGraph)**: 管理所有节点和边，执行每 tick 的物理计算

### Step 2.1: FluidNode

路径: `src/main/java/com/pumpworks/fluid/FluidNode.java`

```java
package com.pumpworks.fluid;

/**
 * 流体网络中的一个节点，对应 MC 世界中的一个方块。
 *
 * 关键概念:
 * - pressure: 当前压力，单位是"方块水柱高度"（类似真实物理的 mH₂O）
 * - elevation: Y 坐标，影响重力势能
 * - capacity: 该节点能存储的流体量（mB，即 millibuckets）
 * - currentFluid: 当前存储的流体量
 */
public class FluidNode {

    public enum NodeType {
        /** 流体源（进水池、储罐入口），始终提供流体 */
        SOURCE,
        /** 管道，仅传输 */
        PIPE,
        /** 泵，提供扬程（压力） */
        PUMP,
        /** 阀门，控制通断 */
        VALVE,
        /** 流体汇（出水池），接收流体 */
        SINK,
        /** 储罐，有容量 */
        TANK,
        /** 仪表，仅测量 */
        GAUGE,
        /** 适配器（连接 Create 流体） */
        ADAPTER
    }

    // 标识此节点的唯一键（在 MC 中是 BlockPos.asLong()）
    private final long posKey;

    private final NodeType type;

    /** Y 坐标（海拔），影响重力势能 */
    private int elevation;

    /** 当前压力（方块水柱高度，正数=正压，负数=负压） */
    private float pressure;

    /** 容量（mB）。管道根据管径有不同容量，储罐容量很大 */
    private int capacity;

    /** 当前存储的流体量（mB） */
    private int currentFluid;

    /** 流体类型标识（"water", "lava", "create:milk" 等） */
    private String fluidType = "";

    /** 是否启用（阀门关闭时节点仍存在但不参与流量计算） */
    private boolean active = true;

    public FluidNode(long posKey, NodeType type, int elevation, int capacity) {
        this.posKey = posKey;
        this.type = type;
        this.elevation = elevation;
        this.capacity = capacity;
        this.currentFluid = 0;
        this.pressure = 0;
    }

    // Getters
    public long getPosKey() { return posKey; }
    public NodeType getType() { return type; }
    public int getElevation() { return elevation; }
    public float getPressure() { return pressure; }
    public int getCapacity() { return capacity; }
    public int getCurrentFluid() { return currentFluid; }
    public String getFluidType() { return fluidType; }
    public boolean isActive() { return active; }

    // Setters
    public void setPressure(float pressure) { this.pressure = pressure; }
    public void setCurrentFluid(int currentFluid) {
        this.currentFluid = Math.max(0, Math.min(currentFluid, capacity));
    }
    public void setFluidType(String fluidType) { this.fluidType = fluidType; }
    public void setActive(boolean active) { this.active = active; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setElevation(int elevation) { this.elevation = elevation; }

    /** 添加流体（正数=灌入，负数=抽出），返回实际变化量 */
    public int addFluid(int amount) {
        int before = currentFluid;
        currentFluid = Math.max(0, Math.min(currentFluid + amount, capacity));
        return currentFluid - before;
    }

    /** 计算该节点的水头（压力 + 海拔） */
    public float getTotalHead() {
        return pressure + elevation;
    }

    @Override
    public String toString() {
        return String.format("FluidNode[pos=%d, type=%s, P=%.1f, Q=%d/%d, elev=%d]",
            posKey, type, pressure, currentFluid, capacity, elevation);
    }
}
```

### Step 2.2: FluidEdge

路径: `src/main/java/com/pumpworks/fluid/FluidEdge.java`

```java
package com.pumpworks.fluid;

/**
 * 连接两个 FluidNode 的边，代表管道中一段流体通道。
 *
 * 关键概念:
 * - flowRate: 流量（mB/tick），正值=从 from 流向 to
 * - resistance: 流动阻力，管径越小阻力越大
 * - inertia: 惯性系数，管道越长水锤惯性越大
 * - maxFlow: 管径限制的最大流量
 * - maxPressure: 管道耐压
 */
public class FluidEdge {

    private final FluidNode from;
    private final FluidNode to;

    /** 当前流量（mB/tick），正值 from→to，负值 to→from */
    private float flowRate;

    /** 流动阻力系数（与管径、粗糙度有关） */
    private float resistance;

    /** 水流惯性系数（与管长有关，水锤计算用） */
    private float inertia;

    /** 最大允许流量（mB/tick） */
    private float maxFlow;

    /** 最大耐压（方块水柱高度） */
    private float maxPressure;

    /** 管道类型标识 */
    private String pipeType; // "copper", "iron", "steel"

    public FluidEdge(FluidNode from, FluidNode to, float resistance,
                     float inertia, float maxFlow, float maxPressure, String pipeType) {
        this.from = from;
        this.to = to;
        this.resistance = resistance;
        this.inertia = inertia;
        this.maxFlow = maxFlow;
        this.maxPressure = maxPressure;
        this.pipeType = pipeType;
        this.flowRate = 0;
    }

    // Getters
    public FluidNode getFrom() { return from; }
    public FluidNode getTo() { return to; }
    public float getFlowRate() { return flowRate; }
    public float getResistance() { return resistance; }
    public float getInertia() { return inertia; }
    public float getMaxFlow() { return maxFlow; }
    public float getMaxPressure() { return maxPressure; }
    public String getPipeType() { return pipeType; }

    // Setters
    public void setFlowRate(float flowRate) {
        // 限制在最大流量范围内
        this.flowRate = Math.max(-maxFlow, Math.min(flowRate, maxFlow));
    }

    /** 获取边中点处的压力（用于水锤计算） */
    public float getMidPressure() {
        return (from.getPressure() + to.getPressure()) / 2.0f;
    }

    /** 检查边是否过压 */
    public boolean isOverPressured() {
        float p = Math.max(Math.abs(from.getPressure()), Math.abs(to.getPressure()));
        return p > maxPressure;
    }

    @Override
    public String toString() {
        return String.format("FluidEdge[%s→%s, Q=%.1f, R=%.3f]",
            from.getPosKey(), to.getPosKey(), flowRate, resistance);
    }
}
```

### Step 2.3: PumpCurve

路径: `src/main/java/com/pumpworks/fluid/PumpCurve.java`

```java
package com.pumpworks.fluid;

/**
 * 水泵的 H-Q 性能曲线。
 *
 * 用分段线性插值表示。曲线上的点是 (流量, 扬程) 的采样对。
 *
 * 三种泵的典型曲线形状:
 * - 离心泵: H = H₀ - a·Q² （单调下降，平缓）
 * - 轴流泵: 陡降 + 马鞍区（中间有凹陷）
 * - 混流泵: 介于两者之间
 *
 * 同时存储 P-Q（功率-流量）和 η-Q（效率-流量）曲线。
 */
public class PumpCurve {

    /** 流量采样点（mB/tick），从小到大排列 */
    private final float[] flowPoints;

    /** 对应的扬程值（方块水柱高度） */
    private final float[] headPoints;

    /** 对应的功率值（相对值，最终乘以应力系数） */
    private final float[] powerPoints;

    /** 对应的效率值（0.0 ~ 1.0） */
    private final float[] efficiencyPoints;

    /** 最大流量（曲线末端） */
    public final float maxFlow;

    /** 关死点扬程（Q=0 时的扬程） */
    public final float shutoffHead;

    public PumpCurve(float[] flowPoints, float[] headPoints,
                     float[] powerPoints, float[] efficiencyPoints) {
        if (flowPoints.length != headPoints.length
            || flowPoints.length != powerPoints.length
            || flowPoints.length != efficiencyPoints.length) {
            throw new IllegalArgumentException("All arrays must have the same length");
        }
        if (flowPoints.length < 2) {
            throw new IllegalArgumentException("Need at least 2 points");
        }

        this.flowPoints = flowPoints.clone();
        this.headPoints = headPoints.clone();
        this.powerPoints = powerPoints.clone();
        this.efficiencyPoints = efficiencyPoints.clone();
        this.maxFlow = flowPoints[flowPoints.length - 1];
        this.shutoffHead = headPoints[0];
    }

    /**
     * 给定流量 Q，用线性插值返回扬程 H。
     * Q 超出范围时返回 0。
     */
    public float getHead(float flow) {
        if (flow <= 0) return shutoffHead;
        if (flow >= maxFlow) return 0;
        return interpolate(flowPoints, headPoints, flow);
    }

    /** 给定流量 Q，返回功率 P（相对值） */
    public float getPower(float flow) {
        if (flow <= 0) return powerPoints[0];
        if (flow >= maxFlow) return 0;
        return interpolate(flowPoints, powerPoints, flow);
    }

    /** 给定流量 Q，返回效率 η */
    public float getEfficiency(float flow) {
        if (flow <= 0) return 0;
        if (flow >= maxFlow) return 0;
        return interpolate(flowPoints, efficiencyPoints, flow);
    }

    /**
     * 将曲线按相似律（比例律）换算到新转速。
     *
     * 比例律:
     *   Q' = Q × (n'/n)
     *   H' = H × (n'/n)²
     *   P' = P × (n'/n)³
     *
     * @param speedRatio 新转速 / 额定转速
     * @return 新的 PumpCurve
     */
    public PumpCurve scaleBySpeed(float speedRatio) {
        if (speedRatio <= 0) {
            // 零转速 → 无曲线
            return new PumpCurve(
                new float[]{0, 0.001f},
                new float[]{0, 0},
                new float[]{0, 0},
                new float[]{0, 0}
            );
        }

        float rQ = speedRatio;
        float rH = speedRatio * speedRatio;
        float rP = rH * speedRatio;

        float[] newFlow = new float[flowPoints.length];
        float[] newHead = new float[headPoints.length];
        float[] newPower = new float[powerPoints.length];
        float[] newEta = efficiencyPoints.clone(); // 效率不随转速变

        for (int i = 0; i < flowPoints.length; i++) {
            newFlow[i] = flowPoints[i] * rQ;
            newHead[i] = headPoints[i] * rH;
            newPower[i] = powerPoints[i] * rP;
        }

        return new PumpCurve(newFlow, newHead, newPower, newEta);
    }

    /** 线性插值 */
    private static float interpolate(float[] xs, float[] ys, float x) {
        for (int i = 0; i < xs.length - 1; i++) {
            if (x >= xs[i] && x <= xs[i + 1]) {
                float t = (x - xs[i]) / (xs[i + 1] - xs[i]);
                return ys[i] + t * (ys[i + 1] - ys[i]);
            }
        }
        return ys[ys.length - 1];
    }

    // ========== 工厂方法：预设曲线 ==========

    /**
     * 创建离心泵曲线。
     * 典型参数: 额定流量 1000 mB/tick, 额定扬程 48 blocks
     */
    public static PumpCurve centrifugal(float ratedFlow, float ratedHead) {
        // 离心泵: H = H₀ - a·Q², H₀ ≈ 1.2 × ratedHead
        float h0 = ratedHead * 1.2f;
        float a = (h0) / (ratedFlow * ratedFlow * 1.5f); // 使 Q=1.22×rated 时 H≈0

        int n = 8; // 8个采样点
        float[] q = new float[n];
        float[] h = new float[n];
        float[] p = new float[n];
        float[] eta = new float[n];

        float maxQ = ratedFlow * 1.5f;
        for (int i = 0; i < n; i++) {
            q[i] = maxQ * i / (n - 1);
            h[i] = Math.max(0, h0 - a * q[i] * q[i]);
            // 离心泵功率随流量增大
            p[i] = ratedHead * ratedFlow * (0.3f + 0.7f * (q[i] / ratedFlow));
            // 效率曲线: 峰值在额定流量附近
            float ratio = q[i] / ratedFlow;
            eta[i] = ratio <= 0 ? 0 :
                (float)(0.85 * Math.exp(-2.0 * Math.pow(ratio - 1.0, 2)));
        }

        return new PumpCurve(q, h, p, eta);
    }

    /**
     * 创建轴流泵曲线。
     * 典型参数: 额定流量 3000 mB/tick, 额定扬程 12 blocks
     * 特征: 陡降 + 马鞍区
     */
    public static PumpCurve axial(float ratedFlow, float ratedHead) {
        float h0 = ratedHead * 1.3f;
        int n = 10;
        float[] q = new float[n];
        float[] h = new float[n];
        float[] p = new float[n];
        float[] eta = new float[n];

        float maxQ = ratedFlow * 1.4f;
        for (int i = 0; i < n; i++) {
            q[i] = maxQ * i / (n - 1);
            float ratio = q[i] / ratedFlow;

            // 轴流泵 H-Q: 陡降，中间有马鞍区凹陷
            float baseHead = h0 * (1.0f - 1.2f * ratio * ratio);
            // 马鞍区: 在 ratio ≈ 0.4~0.6 处凹陷
            float saddleDip = 0;
            if (ratio > 0.3f && ratio < 0.7f) {
                saddleDip = ratedHead * 0.15f
                    * (float)Math.exp(-20 * Math.pow(ratio - 0.5, 2));
            }
            h[i] = Math.max(0, baseHead - saddleDip);

            // 轴流泵功率随流量减小（与离心泵相反！）
            p[i] = ratedHead * ratedFlow * (1.2f - 0.5f * ratio);

            // 效率: 峰值窄
            eta[i] = ratio <= 0 ? 0 :
                (float)(0.82 * Math.exp(-5.0 * Math.pow(ratio - 1.0, 2)));
        }

        return new PumpCurve(q, h, p, eta);
    }

    /**
     * 创建混流泵曲线。
     * 介于离心和轴流之间。
     */
    public static PumpCurve mixedFlow(float ratedFlow, float ratedHead) {
        float h0 = ratedHead * 1.25f;
        int n = 8;
        float[] q = new float[n];
        float[] h = new float[n];
        float[] p = new float[n];
        float[] eta = new float[n];

        float maxQ = ratedFlow * 1.4f;
        for (int i = 0; i < n; i++) {
            q[i] = maxQ * i / (n - 1);
            float ratio = q[i] / ratedFlow;

            // 混流泵: 比离心陡但比轴流平缓
            h[i] = Math.max(0, h0 * (1.0f - 0.8f * ratio * ratio));

            // 功率: 介于两者之间
            p[i] = ratedHead * ratedFlow * (0.6f + 0.3f * ratio);

            // 效率: 中等宽度
            eta[i] = ratio <= 0 ? 0 :
                (float)(0.83 * Math.exp(-3.0 * Math.pow(ratio - 1.0, 2)));
        }

        return new PumpCurve(q, h, p, eta);
    }
}
```

### Step 2.4: OperatingPoint（工作点求解器）

路径: `src/main/java/com/pumpworks/fluid/OperatingPoint.java`

```java
package com.pumpworks.fluid;

/**
 * 工作点求解器。
 *
 * 工作点 = 泵的 H-Q 曲线 与 管路需要扬程曲线的交点。
 *
 * 泵提供扬程: H_pump(Q) — 查 PumpCurve
 * 管路需要扬程: H_need(Q) = H_static + S × Q²
 *   - H_static: 净扬程 = 出水口海拔 - 进水口海拔（静态提升高度）
 *   - S: 管路总阻抗（所有管道阻力之和）
 *
 * 交点求解用二分法:
 *   在 [0, maxFlow] 区间找 H_pump(Q) = H_need(Q) 的 Q 值
 */
public class OperatingPoint {

    public final float flow;       // 工作流量 (mB/tick)
    public final float head;       // 工作扬程 (blocks)
    public final float efficiency; // 工作效率 (0~1)
    public final float power;      // 轴功率 (相对值)

    public OperatingPoint(float flow, float head, float efficiency, float power) {
        this.flow = flow;
        this.head = head;
        this.efficiency = efficiency;
        this.power = power;
    }

    /**
     * 求解单泵工作点。
     *
     * @param pumpCurve 泵的 H-Q 曲线（已换算到当前转速）
     * @param hStatic   净扬程 = 出水高度 - 进水高度（可以为 0 或负数）
     * @param pipeS     管路总阻抗系数
     * @return 工作点，如果无解（H_static 超过关死点扬程）返回流量为 0 的点
     */
    public static OperatingPoint solve(PumpCurve pumpCurve, float hStatic, float pipeS) {
        // 确保 hStatic 不为负（泵不能"倒灌"产生负需求）
        float hNeed0 = Math.max(0, hStatic);

        // 检查关死点扬程是否足够
        if (pumpCurve.shutoffHead <= hNeed0) {
            // 泵的扬程不够，无法供水
            return new OperatingPoint(0, pumpCurve.shutoffHead, 0, pumpCurve.getPower(0));
        }

        // 二分法求解 H_pump(Q) = H_need(Q)
        float lo = 0;
        float hi = pumpCurve.maxFlow;

        for (int i = 0; i < 20; i++) { // 20次迭代，精度到 maxFlow/2^20
            float mid = (lo + hi) / 2.0f;
            float hPump = pumpCurve.getHead(mid);
            float hNeed = hNeed0 + pipeS * mid * mid;

            if (hPump > hNeed) {
                lo = mid; // 泵提供的多，流量可以更大
            } else {
                hi = mid; // 泵提供的不够，流量要减小
            }
        }

        float q = (lo + hi) / 2.0f;
        float h = pumpCurve.getHead(q);
        float eta = pumpCurve.getEfficiency(q);
        float p = pumpCurve.getPower(q);

        return new OperatingPoint(q, h, eta, p);
    }

    /**
     * 求解多泵并联工作点（相同型号的泵）。
     *
     * 并联原理: 等扬程下叠加流量。
     * N 台相同泵并联 → 合成曲线: 同一 H 下 Q_total = N × Q_single
     *
     * @param singlePumpCurve 单泵曲线
     * @param pumpCount       并联泵数量
     * @param hStatic         净扬程
     * @param pipeS           管路总阻抗（总管）
     * @return 并联总工作点（flow 为总流量）
     */
    public static OperatingPoint solveParallel(PumpCurve singlePumpCurve, int pumpCount,
                                                float hStatic, float pipeS) {
        // 合成曲线: 每个流量点乘以 pumpCount
        // 但更简单的方法: 用等效阻抗
        // N 台相同泵并联 → 每台泵的流量 = Q_total / N
        // 泵提供的扬程: H(Q/N)
        // 管路需要: H_static + S × Q²
        // 令 q = Q/N: H_pump(q) = H_static + S × (N×q)² = H_static + S×N²×q²
        float equivalentS = pipeS * pumpCount * pumpCount;
        OperatingPoint singlePoint = solve(singlePumpCurve, hStatic, equivalentS);

        return new OperatingPoint(
            singlePoint.flow * pumpCount,
            singlePoint.head,
            singlePoint.efficiency,
            singlePoint.power * pumpCount
        );
    }

    @Override
    public String toString() {
        return String.format("OperatingPoint[Q=%.1f mB/t, H=%.1f blocks, η=%.0f%%, P=%.1f]",
            flow, head, efficiency * 100, power);
    }
}
```

### Step 2.5: FluidGraph（核心图引擎）

路径: `src/main/java/com/pumpworks/fluid/FluidGraph.java`

```java
package com.pumpworks.fluid;

import java.util.*;

/**
 * 流体网络图引擎。
 *
 * 职责:
 * 1. 维护节点和边的拓扑结构
 * 2. 检测连通分量
 * 3. 对每个连通分量执行每 tick 的物理计算
 *
 * 线程安全: 此类不是线程安全的。MC 服务端 tick 是单线程的，所以不需要。
 */
public class FluidGraph {

    private final Map<Long, FluidNode> nodes = new HashMap<>();
    private final Map<Long, Map<Long, FluidEdge>> adjacency = new HashMap<>(); // posKey → {neighborKey → edge}
    private List<FluidComponent> components = new ArrayList<>();
    private boolean topologyDirty = true;

    // ========== 图的增删 ==========

    public void addNode(FluidNode node) {
        nodes.put(node.getPosKey(), node);
        adjacency.putIfAbsent(node.getPosKey(), new HashMap<>());
        topologyDirty = true;
    }

    public void removeNode(long posKey) {
        nodes.remove(posKey);
        // 移除所有相关边
        Map<Long, FluidEdge> neighbors = adjacency.remove(posKey);
        if (neighbors != null) {
            for (Long neighborKey : neighbors.keySet()) {
                adjacency.getOrDefault(neighborKey, Collections.emptyMap()).remove(posKey);
            }
        }
        topologyDirty = true;
    }

    public FluidNode getNode(long posKey) {
        return nodes.get(posKey);
    }

    public void addEdge(FluidEdge edge) {
        long fromKey = edge.getFrom().getPosKey();
        long toKey = edge.getTo().getPosKey();
        adjacency.computeIfAbsent(fromKey, k -> new HashMap<>()).put(toKey, edge);
        adjacency.computeIfAbsent(toKey, k -> new HashMap<>()).put(fromKey, edge);
        topologyDirty = true;
    }

    public void removeEdge(long fromKey, long toKey) {
        adjacency.getOrDefault(fromKey, Collections.emptyMap()).remove(toKey);
        adjacency.getOrDefault(toKey, Collections.emptyMap()).remove(fromKey);
        topologyDirty = true;
    }

    public FluidEdge getEdge(long fromKey, long toKey) {
        return adjacency.getOrDefault(fromKey, Collections.emptyMap()).get(toKey);
    }

    public Collection<FluidNode> getAllNodes() { return nodes.values(); }

    // ========== 连通分量检测 ==========

    /**
     * 重建连通分量列表。
     * 使用 BFS 遍历。只在拓扑变化时调用。
     */
    public void rebuildComponents() {
        components.clear();
        Set<Long> visited = new HashSet<>();

        for (FluidNode node : nodes.values()) {
            if (visited.contains(node.getPosKey())) continue;
            if (!node.isActive()) continue;

            // BFS
            FluidComponent component = new FluidComponent();
            Queue<Long> queue = new LinkedList<>();
            queue.add(node.getPosKey());
            visited.add(node.getPosKey());

            while (!queue.isEmpty()) {
                long currentKey = queue.poll();
                FluidNode current = nodes.get(currentKey);
                if (current == null || !current.isActive()) continue;
                component.addNode(current);

                Map<Long, FluidEdge> neighbors = adjacency.getOrDefault(currentKey, Collections.emptyMap());
                for (Map.Entry<Long, FluidEdge> entry : neighbors.entrySet()) {
                    long neighborKey = entry.getKey();
                    FluidNode neighbor = nodes.get(neighborKey);
                    if (neighbor != null && neighbor.isActive() && !visited.contains(neighborKey)) {
                        visited.add(neighborKey);
                        component.addEdge(entry.getValue());
                        queue.add(neighborKey);
                    }
                }
            }

            if (!component.getNodes().isEmpty()) {
                component.buildPumpList();
                components.add(component);
            }
        }

        topologyDirty = false;
    }

    // ========== 每 tick 计算 ==========

    /**
     * 每 tick 调用。对所有连通分量执行物理计算。
     */
    public void tick() {
        if (topologyDirty) {
            rebuildComponents();
        }

        for (FluidComponent component : components) {
            tickComponent(component);
        }
    }

    /**
     * 对单个连通分量执行物理计算。
     */
    private void tickComponent(FluidComponent component) {
        List<FluidNode> pumps = component.getPumps();
        if (pumps.isEmpty()) {
            // 没有泵 → 没有流动（流体静止）
            // 但仍然更新压力（静水压力 = elevation 差）
            updateStaticPressures(component);
            return;
        }

        // 1. 计算管路总阻抗 S
        float totalS = calculateTotalResistance(component);

        // 2. 计算净扬程 H_static
        float hStatic = calculateStaticHead(component);

        // 3. 对每个泵求解工作点
        //    (简化: 假设同一分量内所有泵型号和转速相同)
        //    (多型号并联在 v2.0 处理)
        for (FluidNode pumpNode : pumps) {
            // 获取泵的当前曲线 (由 PumpBlockEntity 设置)
            PumpCurve curve = getPumpCurve(pumpNode);
            if (curve == null) continue;

            OperatingPoint op = OperatingPoint.solve(curve, hStatic, totalS);

            // 更新泵的流量和压力
            pumpNode.setPressure(op.head);

            // 将流量分配到泵连接的边
            distributePumpFlow(pumpNode, op.flow);
        }

        // 4. 更新所有边的流量
        updateEdgeFlows(component);

        // 5. 执行流体传输
        transferFluids(component);

        // 6. 更新非泵节点的压力（基于流体静力学）
        updateNodePressures(component);
    }

    private float calculateTotalResistance(FluidComponent component) {
        float totalS = 0;
        for (FluidEdge edge : component.getEdges()) {
            // 串联阻力相加（简化：假设所有边串联）
            // v2.0 需要区分串并联
            totalS += edge.getResistance();
        }
        return Math.max(totalS, 0.001f); // 防止除零
    }

    private float calculateStaticHead(FluidComponent component) {
        // 净扬程 = 最高 SINK 海拔 - 最低 SOURCE 海拔
        int minSourceElev = Integer.MAX_VALUE;
        int maxSinkElev = Integer.MIN_VALUE;

        for (FluidNode node : component.getNodes()) {
            if (node.getType() == FluidNode.NodeType.SOURCE) {
                minSourceElev = Math.min(minSourceElev, node.getElevation());
            }
            if (node.getType() == FluidNode.NodeType.SINK) {
                maxSinkElev = Math.max(maxSinkElev, node.getElevation());
            }
        }

        if (minSourceElev == Integer.MAX_VALUE || maxSinkElev == Integer.MIN_VALUE) {
            return 0; // 没有源或汇
        }

        return Math.max(0, maxSinkElev - minSourceElev);
    }

    private PumpCurve getPumpCurve(FluidNode pumpNode) {
        // 实际实现中，泵曲线由 PumpBlockEntity 存储在节点的附加数据中
        // 这里用接口回调
        if (pumpDataProvider != null) {
            return pumpDataProvider.getPumpCurve(pumpNode.getPosKey());
        }
        return null;
    }

    private void distributePumpFlow(FluidNode pump, float totalFlow) {
        Map<Long, FluidEdge> neighbors = adjacency.getOrDefault(pump.getPosKey(), Collections.emptyMap());
        if (neighbors.isEmpty()) return;

        // 简化: 流量均匀分配到出口方向的边
        // (实际应该根据出口方向确定)
        int outCount = 0;
        for (FluidEdge edge : neighbors.values()) {
            if (edge.getTo() == pump || edge.getFrom() == pump) {
                outCount++;
            }
        }

        if (outCount > 0) {
            float flowPerEdge = totalFlow / outCount;
            for (FluidEdge edge : neighbors.values()) {
                if (edge.getFrom() == pump) {
                    edge.setFlowRate(flowPerEdge);
                } else if (edge.getTo() == pump) {
                    edge.setFlowRate(-flowPerEdge); // 反向
                }
            }
        }
    }

    private void updateEdgeFlows(FluidComponent component) {
        // 基于节点压力差更新边流量
        for (FluidEdge edge : component.getEdges()) {
            FluidNode from = edge.getFrom();
            FluidNode to = edge.getTo();
            float dP = from.getTotalHead() - to.getTotalHead();
            // Q = dP / R (简化，实际是 Q² ∝ dP)
            float newFlow = (float) Math.signum(dP) *
                (float) Math.sqrt(Math.abs(dP) / Math.max(edge.getResistance(), 0.001f));
            edge.setFlowRate(newFlow);
        }
    }

    private void transferFluids(FluidComponent component) {
        for (FluidEdge edge : component.getEdges()) {
            float flow = edge.getFlowRate();
            if (Math.abs(flow) < 0.01f) continue;

            FluidNode source, target;
            float amount;
            if (flow > 0) {
                source = edge.getFrom();
                target = edge.getTo();
                amount = flow;
            } else {
                source = edge.getTo();
                target = edge.getFrom();
                amount = -flow;
            }

            // 从源抽出
            int withdrawn = source.addFluid(-(int) amount);
            // 注入目标
            target.addFluid(-withdrawn); // withdrawn 是负数（抽出量），所以取反
        }
    }

    private void updateNodePressures(FluidComponent component) {
        // 基于连通器原理更新压力
        for (FluidNode node : component.getNodes()) {
            if (node.getType() == FluidNode.NodeType.PUMP) continue; // 泵压力由工作点决定
            if (node.getType() == FluidNode.NodeType.SOURCE) {
                // 源节点压力 = 水面高度 - 节点高度
                node.setPressure(Math.max(0, node.getCurrentFluid() / Math.max(node.getCapacity(), 1) * 10.0f));
                continue;
            }

            // 其他节点: 压力基于相邻节点传递
            float avgNeighborPressure = 0;
            int count = 0;
            Map<Long, FluidEdge> neighbors = adjacency.getOrDefault(node.getPosKey(), Collections.emptyMap());
            for (Map.Entry<Long, FluidEdge> entry : neighbors.entrySet()) {
                FluidNode neighbor = nodes.get(entry.getKey());
                if (neighbor != null && neighbor.isActive()) {
                    // 压力传递考虑高度差和管道损失
                    float neighborP = neighbor.getPressure();
                    float elevDiff = neighbor.getElevation() - node.getElevation();
                    avgNeighborPressure += (neighborP + elevDiff);
                    count++;
                }
            }
            if (count > 0) {
                node.setPressure(avgNeighborPressure / count);
            }
        }
    }

    private void updateStaticPressures(FluidComponent component) {
        // 无泵时，压力由静水力学决定
        updateNodePressures(component);
    }

    // ========== 泵数据提供者接口 ==========

    /** 由 MC 侧实现，将 BlockEntity 中的泵曲线提供给图引擎 */
    public interface PumpDataProvider {
        PumpCurve getPumpCurve(long posKey);
        float getPumpSpeed(long posKey);
    }

    private PumpDataProvider pumpDataProvider;

    public void setPumpDataProvider(PumpDataProvider provider) {
        this.pumpDataProvider = provider;
    }

    public void markDirty() {
        topologyDirty = true;
    }
}
```

### Step 2.6: FluidComponent

路径: `src/main/java/com/pumpworks/fluid/FluidComponent.java`

```java
package com.pumpworks.fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * 流体图中的一个连通分量。
 * 一组互相连通的节点和边。
 */
public class FluidComponent {

    private final List<FluidNode> nodes = new ArrayList<>();
    private final List<FluidEdge> edges = new ArrayList<>();
    private List<FluidNode> pumps; // 缓存的泵节点列表

    public void addNode(FluidNode node) {
        nodes.add(node);
    }

    public void addEdge(FluidEdge edge) {
        if (!edges.contains(edge)) {
            edges.add(edge);
        }
    }

    public void buildPumpList() {
        pumps = new ArrayList<>();
        for (FluidNode node : nodes) {
            if (node.getType() == FluidNode.NodeType.PUMP) {
                pumps.add(node);
            }
        }
    }

    public List<FluidNode> getNodes() { return nodes; }
    public List<FluidEdge> getEdges() { return edges; }
    public List<FluidNode> getPumps() { return pumps; }
    public int size() { return nodes.size(); }

    public boolean hasPumps() {
        return pumps != null && !pumps.isEmpty();
    }
}
```

### Step 2.7: CavitationCalc

路径: `src/main/java/com/pumpworks/fluid/CavitationCalc.java`

```java
package com.pumpworks.fluid;

/**
 * 汽蚀余量计算。
 *
 * NPSH_a (有效汽蚀余量) = 吸水侧压力 - 汽化压力 - 吸水高度损失
 * NPSH_r (必需汽蚀余量) = 泵自身属性（由 PumpData 提供）
 *
 * 不汽蚀条件: NPSH_a ≥ [NPSH] = 1.3 × NPSH_r
 *
 * 在 MC 中的简化:
 * - "大气压力" = 10 blocks 水柱 (约等于真实大气压 10.33m)
 * - "汽化压力" = 0.24 blocks (20°C 时)
 * - "吸水高度" = 泵 Y 坐标 - 水源 Y 坐标
 * - "吸水管损失" = 吸水侧管道的阻力之和 × Q²
 */
public class CavitationCalc {

    /** MC 中的大气压（方块水柱高度） */
    public static final float ATMOSPHERIC_PRESSURE = 10.0f;

    /** 20°C 水的汽化压力（方块水柱高度） */
    public static final float VAPOR_PRESSURE = 0.24f;

    /**
     * 计算有效汽蚀余量 NPSH_a。
     *
     * @param sourceNode 水源节点（进水池）
     * @param pumpNode   泵节点
     * @param suctionPipeS 吸水侧管道总阻抗
     * @param flow       当前流量 (mB/tick)
     * @return NPSH_a（方块水柱高度）
     */
    public static float calcNPSHa(FluidNode sourceNode, FluidNode pumpNode,
                                   float suctionPipeS, float flow) {
        // 水源表面压力（如果是开敞水池 = 大气压）
        float p0 = ATMOSPHERIC_PRESSURE;

        // 安装高度 = 泵海拔 - 水源海拔
        float hsz = pumpNode.getElevation() - sourceNode.getElevation();

        // 吸水管损失
        float hLoss = suctionPipeS * flow * flow;

        return p0 - VAPOR_PRESSURE - hsz - hLoss;
    }

    /**
     * 判断是否会发生汽蚀。
     *
     * @param npsha 有效汽蚀余量
     * @param npshr 必需汽蚀余量（泵属性）
     * @return 汽蚀严重程度 (0=无汽蚀, 0~1=轻微, >1=严重)
     */
    public static float cavitationSeverity(float npsha, float npshr) {
        float allowed = npshr * 1.3f; // [NPSH] = 1.3 × NPSH_r
        if (npsha >= allowed) return 0;
        if (allowed <= 0) return 1;
        return 1.0f - (npsha / allowed);
    }
}
```

### Step 2.8: WaterHammer

路径: `src/main/java/com/pumpworks/fluid/WaterHammer.java`

```java
package com.pumpworks.fluid;

import java.util.*;

/**
 * 水锤模拟引擎。
 *
 * 当泵突然停止或阀门突然关闭时，触发水锤事件。
 * 压力波沿管道传播，每 tick 传播 N 个管道段，逐渐衰减。
 *
 * 简化模型（非特征线法），但保留了核心的四阶段过程：
 * 1. 水泵工况衰减 (tick 0~3): 降压波
 * 2. 制动工况 (tick 3~6): 倒流开始
 * 3. 水轮机工况 (tick 6~10): 最大正压（最危险）
 * 4. 飞逸稳定 (tick 10+): 波动衰减
 */
public class WaterHammer {

    /**
     * 水锤事件。一个事件从触发点开始，逐 tick 向外传播。
     */
    public static class HammerEvent {
        /** 触发类型 */
        public enum TriggerType {
            PUMP_SHUTDOWN,    // 事故停泵
            VALVE_CLOSURE,    // 关阀
            PUMP_STARTUP      // 启动（轻微）
        }

        public final long originPosKey;   // 触发位置
        public final TriggerType trigger; // 触发类型
        public final float initialDeltaP; // 初始压力变化量
        public int tickCount;             // 已经过 tick 数
        public boolean active;            // 是否仍在传播

        /** 当前波前的压力值 */
        public float currentPressure;

        /** 已经过的管道段（避免重复处理） */
        public final Set<Long> visitedEdges = new HashSet<>();

        public HammerEvent(long originPosKey, TriggerType trigger, float initialDeltaP) {
            this.originPosKey = originPosKey;
            this.trigger = trigger;
            this.initialDeltaP = initialDeltaP;
            this.currentPressure = initialDeltaP;
            this.tickCount = 0;
            this.active = true;
        }
    }

    /** 当前活跃的水锤事件列表 */
    private final List<HammerEvent> activeEvents = new ArrayList<>();

    /** 配置参数（从 PumpworksConfig 读取） */
    private float decayFactor = 0.90f;
    private int waveSpeedBlocksPerTick = 3;
    private boolean enabled = true;

    public void setConfig(float decay, int speed, boolean enabled) {
        this.decayFactor = decay;
        this.waveSpeedBlocksPerTick = speed;
        this.enabled = enabled;
    }

    /**
     * 触发一个水锤事件。
     *
     * @param graph    流体图
     * @param posKey   触发位置（泵或阀门的 posKey）
     * @param trigger  触发类型
     * @param flowRate 触发前的流量（用于计算初始压力变化）
     */
    public void trigger(FluidGraph graph, long posKey,
                        HammerEvent.TriggerType trigger, float flowRate) {
        if (!enabled) return;

        // 计算初始压力变化 ΔP = ρ × c × v（简化为与流量成正比）
        float waveSpeed = 100.0f; // 水锤波速（方块/tick，简化值）
        float deltaP = waveSpeed * flowRate * 0.01f;

        if (trigger == HammerEvent.TriggerType.PUMP_SHUTDOWN) {
            deltaP = -deltaP; // 停泵 → 降压波
        } else if (trigger == HammerEvent.TriggerType.VALVE_CLOSURE) {
            deltaP = deltaP;  // 关阀 → 升压波
        } else {
            deltaP = deltaP * 0.3f; // 启动水锤，较弱
        }

        HammerEvent event = new HammerEvent(posKey, trigger, deltaP);
        activeEvents.add(event);
    }

    /**
     * 每 tick 调用。传播所有活跃的水锤事件。
     *
     * @param graph 流体图
     * @return 本 tick 中损坏的管道 posKey 列表（过压损坏）
     */
    public List<Long> tick(FluidGraph graph) {
        List<Long> damagedPipes = new ArrayList<>();

        Iterator<HammerEvent> it = activeEvents.iterator();
        while (it.hasNext()) {
            HammerEvent event = it.next();
            if (!event.active) {
                it.remove();
                continue;
            }

            // 四阶段压力计算
            float pressure = calculatePhasePressure(event);
            event.currentPressure = pressure;

            // 传播到相邻管道
            propagateWave(graph, event, damagedPipes);

            event.tickCount++;

            // 压力衰减到可忽略时结束
            if (Math.abs(event.currentPressure) < 0.1f) {
                event.active = false;
            }
        }

        return damagedPipes;
    }

    /**
     * 根据四阶段模型计算当前 tick 的压力。
     */
    private float calculatePhasePressure(HammerEvent event) {
        int t = event.tickCount;
        float baseP = event.initialDeltaP;

        if (event.trigger == HammerEvent.TriggerType.PUMP_SHUTDOWN) {
            // 停泵水锤四阶段
            if (t < 3) {
                // 阶段①: 降压波
                return baseP; // baseP 是负值
            } else if (t < 6) {
                // 阶段②: 压力回升
                float progress = (t - 3) / 3.0f;
                return baseP * (1.0f - 1.5f * progress); // 从负压回升到正压
            } else if (t < 10) {
                // 阶段③: 最大正压（最危险！）
                float progress = (t - 6) / 4.0f;
                return Math.abs(baseP) * (1.5f - 0.5f * progress); // 正压峰值
            } else {
                // 阶段④: 波动衰减
                float decay = (float) Math.exp(-0.3 * (t - 10));
                float oscillation = (float) Math.cos(0.5 * (t - 10));
                return Math.abs(baseP) * decay * oscillation;
            }
        } else {
            // 关阀水锤: 简单的升压后衰减
            float decay = (float) Math.exp(-0.2 * t);
            float oscillation = (float) Math.cos(0.8 * t);
            return baseP * decay * oscillation;
        }
    }

    /**
     * 将压力波传播到相邻管道段。
     */
    private void propagateWave(FluidGraph graph, HammerEvent event, List<Long> damagedPipes) {
        // 简化传播: 每 tick 传播 waveSpeedBlocksPerTick 层
        // 从 originPosKey 开始 BFS，深度 = tickCount × waveSpeed

        // 注意: 实际实现时需要从 graph 获取 adjacency 信息
        // 这里只提供框架，具体在 FluidGraph 中实现
    }

    public boolean hasActiveEvents() {
        return !activeEvents.isEmpty();
    }
}
```

### Step 2.9: 单元测试

创建以下测试文件来验证纯逻辑代码的正确性：

#### `src/test/java/com/pumpworks/fluid/PumpCurveTest.java`

```java
package com.pumpworks.fluid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PumpCurveTest {

    @Test
    void testCentrifugalCurveShape() {
        PumpCurve curve = PumpCurve.centrifugal(1000, 48);
        // 关死点扬程应该最大
        assertTrue(curve.shutoffHead > curve.getHead(500));
        // 流量越大扬程越低
        assertTrue(curve.getHead(200) > curve.getHead(800));
        // 最大流量处扬程接近 0
        assertTrue(curve.getHead(curve.maxFlow) < 1.0f);
    }

    @Test
    void testAxialCurveSaddle() {
        PumpCurve curve = PumpCurve.axial(3000, 12);
        // 马鞍区: 在 Q ≈ 0.4~0.6 × ratedFlow 处有凹陷
        float h40 = curve.getHead(1200); // 0.4 × 3000
        float h50 = curve.getHead(1500); // 0.5 × 3000
        float h60 = curve.getHead(1800); // 0.6 × 3000
        // 马鞍区中间 (h50) 应该低于两侧
        assertTrue(h50 < h40 || h50 < h60, "Saddle dip should exist");
    }

    @Test
    void testSpeedScaling() {
        PumpCurve curve = PumpCurve.centrifugal(1000, 48);
        PumpCurve half = curve.scaleBySpeed(0.5f);
        // 比例律: 半速时 Q' = 0.5Q, H' = 0.25H
        float originalHead = curve.getHead(500);
        float scaledHead = half.getHead(250); // Q'=0.5×500
        assertEquals(originalHead * 0.25f, scaledHead, 2.0f);
    }
}
```

#### `src/test/java/com/pumpworks/fluid/OperatingPointTest.java`

```java
package com.pumpworks.fluid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperatingPointTest {

    @Test
    void testBasicSolve() {
        PumpCurve curve = PumpCurve.centrifugal(1000, 48);
        // 无净扬程、无阻力 → 流量应该接近 maxFlow
        OperatingPoint op = OperatingPoint.solve(curve, 0, 0.001f);
        assertTrue(op.flow > 0, "Should have positive flow");
        assertTrue(op.head > 0, "Should have positive head");
    }

    @Test
    void testHighStaticHead() {
        PumpCurve curve = PumpCurve.centrifugal(1000, 48);
        // 净扬程超过关死点 → 流量为 0
        OperatingPoint op = OperatingPoint.solve(curve, 100, 0.01f);
        assertEquals(0, op.flow, 0.01f);
    }

    @Test
    void testParallel() {
        PumpCurve curve = PumpCurve.centrifugal(1000, 48);
        OperatingPoint single = OperatingPoint.solve(curve, 20, 0.01f);
        OperatingPoint parallel = OperatingPoint.solveParallel(curve, 2, 20, 0.01f);
        // 并联总流量应该大于单泵
        assertTrue(parallel.flow > single.flow);
        // 但小于 2 倍单泵（因为管路阻力随流量增大）
        assertTrue(parallel.flow < single.flow * 2);
    }
}
```

### Step 2.10: 验证

```bash
./gradlew test
```

**预期**: 所有测试通过

---

## Phase 3: 管道方块

### 目标
实现铜管、铁管、钢管三种管道方块。管道是流体网络的传输通道。

### Step 3.1: PipeType 枚举

路径: `src/main/java/com/pumpworks/content/pipes/PipeType.java`

```java
package com.pumpworks.content.pipes;

/**
 * 管道类型枚举。
 * 不同材质 = 不同管径、阻力、耐压。
 */
public enum PipeType {
    COPPER("copper", 0.05f, 400, 0.008f, 64.0f),
    IRON  ("iron",   0.02f, 800, 0.004f, 128.0f),
    STEEL ("steel",  0.01f, 1600, 0.002f, 256.0f);

    public final String name;
    /** 每段管道的阻力系数 */
    public final float resistance;
    /** 容量 (mB) */
    public final int capacity;
    /** 惯性系数 */
    public final float inertia;
    /** 最大耐压 */
    public final float maxPressure;

    PipeType(String name, float resistance, int capacity, float inertia, float maxPressure) {
        this.name = name;
        this.resistance = resistance;
        this.capacity = capacity;
        this.inertia = inertia;
        this.maxPressure = maxPressure;
    }
}
```

### Step 3.2: PipeBlock

路径: `src/main/java/com/pumpworks/content/pipes/PipeBlock.java`

```java
package com.pumpworks.content.pipes;

import com.pumpworks.AllBlockEntityTypes;
import com.pumpworks.foundation.block.PumpworksBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 管道方块。
 *
 * 有 6 个方向的连接属性（类似 Create 的 FluidPipeBlock）。
 * 当相邻有管道/泵/阀门/储罐时自动连接。
 *
 * 方块状态:
 * - NORTH, SOUTH, EAST, WEST, UP, DOWN: 布尔值，表示该方向是否连接
 * - DAMAGED: 布尔值，表示管道是否破损
 * - PIPE_TYPE: 枚举，铜/铁/钢（不用 BlockState，用 Block 类区分）
 */
public class PipeBlock extends Block implements IBE<PipeBlockEntity> {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");

    private static final BooleanProperty[] DIRECTION_PROPERTIES = {
        DOWN, UP, NORTH, SOUTH, WEST, EAST
    };

    public final PipeType pipeType;

    public PipeBlock(Properties properties, PipeType pipeType) {
        super(properties);
        this.pipeType = pipeType;
        registerDefaultState(stateDefinition.any()
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
            .setValue(DAMAGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, DAMAGED);
    }

    /**
     * 获取某个方向的连接属性。
     */
    public static BooleanProperty getConnectionProperty(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    /**
     * 判断某个方向的相邻方块是否可以连接。
     * 可连接的方块: 其他管道、泵、阀门、储罐、适配器。
     */
    public static boolean canConnectTo(Level level, BlockPos neighborPos, Direction fromDirection) {
        BlockState neighborState = level.getBlockState(neighborPos);
        Block block = neighborState.getBlock();
        return block instanceof PipeBlock
            || block instanceof com.pumpworks.content.pumps.base.AbstractPumpBlock
            || block instanceof com.pumpworks.content.valves.base.AbstractValveBlock
            || block instanceof com.pumpworks.content.tanks.IntakeTankBlock
            || block instanceof com.pumpworks.content.tanks.OutletTankBlock
            || block instanceof com.pumpworks.content.adapter.FluidAdapterBlock;
    }

    /**
     * 更新方块的连接状态。
     * 在 onPlace 和 neighborChanged 时调用。
     */
    public BlockState updateConnections(BlockState state, Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            boolean connected = canConnectTo(level, neighborPos, dir);
            state = state.setValue(getConnectionProperty(dir), connected);
        }
        return state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide) return;

        BlockState newState = updateConnections(state, level, pos);
        if (newState != state) {
            level.setBlockAndUpdate(pos, newState);
        }

        // 通知流体图：新方块加入
        notifyFluidGraph(level, pos, true);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                 Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        if (level.isClientSide) return;

        BlockState newState = updateConnections(state, level, pos);
        if (newState != state) {
            level.setBlockAndUpdate(pos, newState);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                notifyFluidGraph(level, pos, false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * 通知流体图管理器此位置有方块变化。
     */
    private void notifyFluidGraph(Level level, BlockPos pos, boolean added) {
        // 将在 FluidGraphManager 中实现
        // FluidGraphManager.get(level).onPipeChanged(pos, added);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                CollisionContext context) {
        // 简化: 中心一个 6×6×6 的方块 + 连接方向的管道
        // 实际应该根据连接状态生成更精细的形状
        double min = 5.0 / 16.0;
        double max = 11.0 / 16.0;
        VoxelShape shape = Block.box(min * 16, min * 16, min * 16, max * 16, max * 16, max * 16);

        for (Direction dir : Direction.values()) {
            if (state.getValue(getConnectionProperty(dir))) {
                shape = Shapes.or(shape, getDirectionShape(dir));
            }
        }
        return shape;
    }

    private VoxelShape getDirectionShape(Direction dir) {
        double min = 5.0 / 16.0;
        double max = 11.0 / 16.0;
        return switch (dir) {
            case DOWN -> Block.box(min * 16, 0, min * 16, max * 16, min * 16, max * 16);
            case UP -> Block.box(min * 16, max * 16, min * 16, max * 16, 16, max * 16);
            case NORTH -> Block.box(min * 16, min * 16, 0, max * 16, max * 16, min * 16);
            case SOUTH -> Block.box(min * 16, min * 16, max * 16, max * 16, max * 16, 16);
            case WEST -> Block.box(0, min * 16, min * 16, min * 16, max * 16, max * 16);
            case EAST -> Block.box(max * 16, min * 16, min * 16, 16, max * 16, max * 16);
        };
    }

    // IBE 接口实现
    @Override
    public Class<PipeBlockEntity> getBlockEntityClass() {
        return PipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PipeBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.PIPE.get();
    }
}
```

### Step 3.3: PipeBlockEntity

路径: `src/main/java/com/pumpworks/content/pipes/PipeBlockEntity.java`

```java
package com.pumpworks.content.pipes;

import com.pumpworks.fluid.FluidNode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 管道的 BlockEntity。
 * 主要职责:
 * 1. 维护对应的 FluidNode
 * 2. 处理破损/修复状态
 * 3. 同步客户端渲染数据
 */
public class PipeBlockEntity extends BlockEntity {

    /** 此管道对应的流体节点 */
    private FluidNode fluidNode;

    /** 管道类型 */
    private PipeType pipeType = PipeType.IRON;

    /** 是否破损 */
    private boolean damaged = false;

    public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof PipeBlock pipeBlock) {
            this.pipeType = pipeBlock.pipeType;
        }
    }

    /**
     * 获取或创建对应的 FluidNode。
     */
    public FluidNode getOrCreateFluidNode() {
        if (fluidNode == null) {
            fluidNode = new FluidNode(
                worldPosition.asLong(),
                FluidNode.NodeType.PIPE,
                worldPosition.getY(),
                pipeType.capacity
            );
        }
        return fluidNode;
    }

    public PipeType getPipeType() { return pipeType; }
    public boolean isDamaged() { return damaged; }

    public void setDamaged(boolean damaged) {
        this.damaged = damaged;
        if (level != null && !level.isClientSide) {
            level.setBlockAndUpdate(worldPosition,
                getBlockState().setValue(PipeBlock.DAMAGED, damaged));
            setChanged();
        }
    }

    /**
     * 修复破损管道（玩家使用管道扳手时调用）。
     */
    public void repair() {
        setDamaged(false);
    }

    // NBT 序列化
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("PipeType", pipeType.name());
        tag.putBoolean("Damaged", damaged);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("PipeType")) {
            pipeType = PipeType.valueOf(tag.getString("PipeType"));
        }
        damaged = tag.getBoolean("Damaged");
    }
}
```

### Step 3.4: 注册管道方块

在 `AllBlocks.java` 中添加:

```java
// ===== 管道 =====
public static final BlockEntry<PipeBlock> COPPER_PIPE = PumpworksMod.registrate()
    .block("copper_pipe", p -> new PipeBlock(p, PipeType.COPPER))
    .initialProperties(() -> Blocks.COPPER_BLOCK)
    .properties(p -> p
        .strength(1.0f, 6.0f)
        .sound(SoundType.COPPER)
        .noOcclusion())
    .item()
    .build()
    .register();

public static final BlockEntry<PipeBlock> IRON_PIPE = PumpworksMod.registrate()
    .block("iron_pipe", p -> new PipeBlock(p, PipeType.IRON))
    .initialProperties(() -> Blocks.IRON_BLOCK)
    .properties(p -> p
        .strength(2.0f, 6.0f)
        .sound(SoundType.METAL)
        .noOcclusion())
    .item()
    .build()
    .register();

public static final BlockEntry<PipeBlock> STEEL_PIPE = PumpworksMod.registrate()
    .block("steel_pipe", p -> new PipeBlock(p, PipeType.STEEL))
    .initialProperties(() -> Blocks.IRON_BLOCK)
    .properties(p -> p
        .strength(3.0f, 10.0f)
        .sound(SoundType.NETHERITE_BLOCK)
        .noOcclusion())
    .item()
    .build()
    .register();
```

在 `AllBlockEntityTypes.java` 中添加:

```java
public static final BlockEntityEntry<PipeBlockEntity> PIPE =
    PumpworksMod.registrate()
        .blockEntity("pipe", PipeBlockEntity::new)
        .validBlocks(AllBlocks.COPPER_PIPE, AllBlocks.IRON_PIPE, AllBlocks.STEEL_PIPE)
        .register();
```

### Step 3.5: 资源文件（最小可运行）

每个方块需要以下资源文件:

#### BlockState JSON

`src/main/resources/assets/pumpworks/blockstates/copper_pipe.json`:
```json
{
  "variants": {
    "": { "model": "pumpworks:block/copper_pipe" }
  }
}
```
（iron_pipe 和 steel_pipe 同理，只需改名称）

#### Block Model JSON

`src/main/resources/assets/pumpworks/models/block/copper_pipe.json`:
```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "pumpworks:block/copper_pipe"
  }
}
```

#### Item Model JSON

`src/main/resources/assets/pumpworks/models/item/copper_pipe.json`:
```json
{
  "parent": "pumpworks:block/copper_pipe"
}
```

#### 纹理文件

需要准备（或暂时使用占位符）:
- `src/main/resources/assets/pumpworks/textures/block/copper_pipe.png` (16×16)
- `src/main/resources/assets/pumpworks/textures/block/iron_pipe.png` (16×16)
- `src/main/resources/assets/pumpworks/textures/block/steel_pipe.png` (16×16)

**临时占位符**: 可以用纯色 PNG 文件代替，后续替换为正式纹理。

### Step 3.6: 验证

```bash
./gradlew build
./gradlew runClient
```

**预期**: MC 启动后，在创造模式搜索 "pumpworks" 应看到铜管/铁管/钢管。放置后能互相连接。

---

## Phase 4: 水泵方块

### 目标
实现离心泵、轴流泵、混流泵。泵继承 Create 的 `KineticBlockEntity`，由动能网络驱动。

### 这是最关键的 Phase——泵是整个模组的核心。

### Step 4.1: 泵基类 AbstractPumpBlock

路径: `src/main/java/com/pumpworks/content/pumps/base/AbstractPumpBlock.java`

```java
package com.pumpworks.content.pumps.base;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 水泵方块基类。
 *
 * 继承 Create 的 DirectionalKineticBlock:
 * - 自动处理 FACING 属性（泵的朝向 = 旋转轴方向）
 * - 自动接入 Create 的动能网络
 *
 * 子类需要指定:
 * - BlockEntity 类型
 * - 方块碰撞形状
 */
public abstract class AbstractPumpBlock extends DirectionalKineticBlock
    implements IBE<AbstractPumpBlockEntity> {

    public AbstractPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        // 旋转轴 = 朝向轴（泵沿轴向旋转）
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(net.minecraft.world.level.LevelReader world,
                                    BlockPos pos, BlockState state, Direction face) {
        // 泵在 FACING 的两个方向都有轴（入口端和出口端）
        return face.getAxis() == state.getValue(FACING).getAxis();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                         BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // 通知流体图
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                // 通知流体图
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
```

### Step 4.2: 泵 BlockEntity 基类 AbstractPumpBlockEntity

路径: `src/main/java/com/pumpworks/content/pumps/base/AbstractPumpBlockEntity.java`

```java
package com.pumpworks.content.pumps.base;

import com.pumpworks.fluid.CavitationCalc;
import com.pumpworks.fluid.FluidNode;
import com.pumpworks.fluid.OperatingPoint;
import com.pumpworks.fluid.PumpCurve;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import java.util.List;

/**
 * 水泵 BlockEntity 基类。
 *
 * 继承 Create 的 KineticBlockEntity:
 * - 自动获取动能网络的转速 (getSpeed())
 * - 自动参与应力计算
 * - onSpeedChanged() 在转速变化时回调
 *
 * 核心每 tick 流程:
 * 1. 获取转速 → 按比例律换算 H-Q 曲线
 * 2. 从流体图获取装置参数（H_static, S）
 * 3. 求解工作点 → 得到 Q, H, η, P
 * 4. 更新流体节点
 * 5. 检测汽蚀
 */
public abstract class AbstractPumpBlockEntity extends KineticBlockEntity
    implements IHaveGoggleInformation {

    /** 泵的额定性能曲线 */
    protected PumpCurve ratedCurve;

    /** 当前转速下的换算曲线 */
    protected PumpCurve currentCurve;

    /** 当前工作点 */
    protected OperatingPoint currentOperatingPoint;

    /** 对应的流体节点 */
    protected FluidNode fluidNode;

    /** 汽蚀严重程度 (0=无汽蚀) */
    protected float cavitationSeverity;

    /** 运行时间统计 */
    protected long runTicks;

    /** 额定转速 (RPM) */
    protected float ratedSpeed;

    /** 额定流量 (mB/tick) */
    protected float ratedFlow;

    /** 额定扬程 (blocks) */
    protected float ratedHead;

    /** 必需汽蚀余量 */
    protected float npshr;

    public AbstractPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        initPumpParameters();
    }

    /**
     * 初始化泵的参数。子类必须覆写。
     */
    protected abstract void initPumpParameters();

    /**
     * 子类提供额定曲线。
     */
    protected abstract PumpCurve createRatedCurve();

    /**
     * 在构造函数末尾调用，初始化曲线。
     * 注意: 不能在构造函数中直接调用抽象方法（Java 限制），
     * 所以在第一次 tick 时延迟初始化。
     */
    private boolean initialized = false;

    private void ensureInitialized() {
        if (!initialized) {
            initPumpParameters();
            ratedCurve = createRatedCurve();
            currentCurve = ratedCurve;
            initialized = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        ensureInitialized();

        float currentSpeed = getSpeed();

        // 1. 转速为 0 → 泵停止
        if (Math.abs(currentSpeed) < 1.0f) {
            currentOperatingPoint = null;
            cavitationSeverity = 0;
            return;
        }

        // 2. 按比例律换算曲线
        float speedRatio = Math.abs(currentSpeed) / ratedSpeed;
        currentCurve = ratedCurve.scaleBySpeed(speedRatio);

        // 3. 获取装置参数（从流体图）
        float hStatic = getStaticHead();
        float pipeS = getPipeResistance();

        // 4. 求解工作点
        currentOperatingPoint = OperatingPoint.solve(currentCurve, hStatic, pipeS);

        // 5. 更新应力消耗
        updateStressConsumption(currentOperatingPoint);

        // 6. 更新流体节点
        updateFluidNode(currentOperatingPoint);

        // 7. 检测汽蚀
        checkCavitation();

        runTicks++;
    }

    @Override
    protected void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        // 转速变化时标记流体图需要重算
    }

    /**
     * 获取净扬程（从流体图管理器）。
     * 简化版: 暂时返回一个固定值，后续与 FluidGraphManager 集成。
     */
    protected float getStaticHead() {
        // TODO: 从 FluidGraphManager 获取
        return 10.0f; // 临时: 假设净扬程 10 blocks
    }

    /**
     * 获取管路总阻抗。
     */
    protected float getPipeResistance() {
        // TODO: 从 FluidGraphManager 获取
        return 0.01f; // 临时值
    }

    /**
     * 更新应力消耗。
     *
     * 有效功率 P_u = ρ·g·Q·H（游戏单位换算后）
     * 轴功率 P = P_u / η
     * SU = P × stressFactor
     */
    protected void updateStressConsumption(OperatingPoint op) {
        if (op == null || op.efficiency <= 0) {
            // 无效工作点 → 最小应力
            return;
        }

        // 应力计算 (简化):
        // Create 的应力单位大致是 1 SU = 约 1 RPM 下的负载
        // 我们用 P = Q × H / η 然后乘以系数
        float effectivePower = op.flow * op.head;
        float shaftPower = op.efficiency > 0 ? effectivePower / op.efficiency : effectivePower;
        // 转换为 SU (具体系数需要调参)
        float su = shaftPower * 0.01f;
        // 通过 Create 的应力系统设置
        // this.stress = su;  // 具体 API 在集成时确认
    }

    /**
     * 更新流体节点的压力和流量。
     */
    protected void updateFluidNode(OperatingPoint op) {
        if (fluidNode == null) {
            fluidNode = new FluidNode(
                worldPosition.asLong(),
                FluidNode.NodeType.PUMP,
                worldPosition.getY(),
                0 // 泵本身不存储流体
            );
        }

        if (op != null) {
            fluidNode.setPressure(op.head);
        }
    }

    /**
     * 检测汽蚀。
     */
    protected void checkCavitation() {
        // 简化: 假设水源在泵下方 5 blocks
        float sourceElev = worldPosition.getY() - 5;
        float npsha = CavitationCalc.ATMOSPHERIC_PRESSURE
            - CavitationCalc.VAPOR_PRESSURE
            - (worldPosition.getY() - sourceElev)
            - 0.5f; // 吸水损失估计值

        cavitationSeverity = CavitationCalc.cavitationSeverity(npsha, npshr);
    }

    // ========== 护目镜信息 ==========

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ensureInitialized();

        // 使用 Create 的 LangBuilder 格式
        // tooltip.add(Component.literal("§6--- " + getPumpName() + " ---"));
        // tooltip.add(Component.literal("§7Speed: §f" + String.format("%.0f", getSpeed()) + " RPM"));

        if (currentOperatingPoint != null) {
            // tooltip.add(Component.literal("§7Flow: §f" + String.format("%.0f", currentOperatingPoint.flow) + " mB/t"));
            // tooltip.add(Component.literal("§7Head: §f" + String.format("%.1f", currentOperatingPoint.head) + " blocks"));
            // tooltip.add(Component.literal("§7Efficiency: §f" + String.format("%.0f%%", currentOperatingPoint.efficiency * 100)));
        } else {
            // tooltip.add(Component.literal("§7Status: §cStopped"));
        }

        if (cavitationSeverity > 0) {
            // tooltip.add(Component.literal("§c⚠ Cavitation! Severity: " + String.format("%.0f%%", cavitationSeverity * 100)));
        }

        return true;
    }

    /**
     * 获取泵的显示名称。子类覆写。
     */
    protected abstract String getPumpName();

    // ========== Getters ==========

    public OperatingPoint getCurrentOperatingPoint() { return currentOperatingPoint; }
    public float getCavitationSeverity() { return cavitationSeverity; }
    public PumpCurve getRatedCurve() { ensureInitialized(); return ratedCurve; }
    public PumpCurve getCurrentCurve() { ensureInitialized(); return currentCurve; }
    public float getRatedSpeed() { return ratedSpeed; }
    public float getRatedFlow() { return ratedFlow; }
    public float getRatedHead() { return ratedHead; }
    public FluidNode getFluidNode() { return fluidNode; }

    // ========== NBT ==========

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("RunTicks", runTicks);
        tag.putFloat("CavitationSeverity", cavitationSeverity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        runTicks = tag.getLong("RunTicks");
        cavitationSeverity = tag.getFloat("CavitationSeverity");
    }
}
```

### Step 4.3: 三种具体泵

#### CentrifugalPumpBlockEntity

路径: `src/main/java/com/pumpworks/content/pumps/centrifugal/CentrifugalPumpBlockEntity.java`

```java
package com.pumpworks.content.pumps.centrifugal;

import com.pumpworks.content.pumps.base.AbstractPumpBlockEntity;
import com.pumpworks.fluid.PumpCurve;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CentrifugalPumpBlockEntity extends AbstractPumpBlockEntity {

    public CentrifugalPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void initPumpParameters() {
        ratedSpeed = 256.0f;  // RPM
        ratedFlow = 1000.0f;  // mB/tick
        ratedHead = 48.0f;    // blocks
        npshr = 4.0f;         // 必需汽蚀余量
    }

    @Override
    protected PumpCurve createRatedCurve() {
        return PumpCurve.centrifugal(ratedFlow, ratedHead);
    }

    @Override
    protected String getPumpName() {
        return "Centrifugal Pump";
    }
}
```

#### AxialPumpBlockEntity

路径: `src/main/java/com/pumpworks/content/pumps/axial/AxialPumpBlockEntity.java`

```java
package com.pumpworks.content.pumps.axial;

import com.pumpworks.content.pumps.base.AbstractPumpBlockEntity;
import com.pumpworks.fluid.PumpCurve;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AxialPumpBlockEntity extends AbstractPumpBlockEntity {

    public AxialPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void initPumpParameters() {
        ratedSpeed = 128.0f;   // RPM (轴流泵通常低速)
        ratedFlow = 3000.0f;   // mB/tick (大流量)
        ratedHead = 12.0f;     // blocks (低扬程)
        npshr = 6.0f;          // NPSHr (轴流泵通常更高)
    }

    @Override
    protected PumpCurve createRatedCurve() {
        return PumpCurve.axial(ratedFlow, ratedHead);
    }

    @Override
    protected String getPumpName() {
        return "Axial Flow Pump";
    }
}
```

#### MixedFlowPumpBlockEntity

路径: `src/main/java/com/pumpworks/content/pumps/mixed/MixedFlowPumpBlockEntity.java`

```java
package com.pumpworks.content.pumps.mixed;

import com.pumpworks.content.pumps.base.AbstractPumpBlockEntity;
import com.pumpworks.fluid.PumpCurve;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MixedFlowPumpBlockEntity extends AbstractPumpBlockEntity {

    public MixedFlowPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void initPumpParameters() {
        ratedSpeed = 192.0f;   // RPM
        ratedFlow = 2000.0f;   // mB/tick
        ratedHead = 24.0f;     // blocks
        npshr = 5.0f;
    }

    @Override
    protected PumpCurve createRatedCurve() {
        return PumpCurve.mixedFlow(ratedFlow, ratedHead);
    }

    @Override
    protected String getPumpName() {
        return "Mixed Flow Pump";
    }
}
```

### Step 4.4: 泵方块类

三种泵的 Block 类非常相似，主要区别在形状和 BlockEntity 类型：

#### CentrifugalPumpBlock

路径: `src/main/java/com/pumpworks/content/pumps/centrifugal/CentrifugalPumpBlock.java`

```java
package com.pumpworks.content.pumps.centrifugal;

import com.pumpworks.AllBlockEntityTypes;
import com.pumpworks.content.pumps.base.AbstractPumpBlock;
import com.pumpworks.content.pumps.base.AbstractPumpBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CentrifugalPumpBlock extends AbstractPumpBlock {

    // 泵的碰撞形状（比管道大）
    private static final VoxelShape SHAPE_NS = Block.box(0, 2, 2, 16, 14, 14);
    private static final VoxelShape SHAPE_EW = Block.box(2, 2, 0, 14, 14, 16);
    private static final VoxelShape SHAPE_UD = Block.box(2, 0, 2, 14, 16, 14);

    public CentrifugalPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> SHAPE_EW;
            case Z -> SHAPE_NS;
            case Y -> SHAPE_UD;
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<AbstractPumpBlockEntity> getBlockEntityClass() {
        return (Class<AbstractPumpBlockEntity>)(Class<?>)CentrifugalPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AbstractPumpBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.CENTRIFUGAL_PUMP.get();
    }
}
```

AxialPumpBlock 和 MixedFlowPumpBlock 同理（改类名和 BlockEntity 引用）。

### Step 4.5: 注册泵

在 `AllBlocks.java` 中添加:

```java
// ===== 泵 =====
public static final BlockEntry<CentrifugalPumpBlock> CENTRIFUGAL_PUMP =
    PumpworksMod.registrate()
        .block("centrifugal_pump", CentrifugalPumpBlock::new)
        .initialProperties(() -> net.minecraft.world.level.blocks.Blocks.COPPER_BLOCK)
        .properties(p -> p.strength(2.0f, 6.0f).sound(SoundType.COPPER).noOcclusion())
        .item()
        .build()
        .register();

public static final BlockEntry<AxialPumpBlock> AXIAL_PUMP =
    PumpworksMod.registrate()
        .block("axial_pump", AxialPumpBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion())
        .item()
        .build()
        .register();

public static final BlockEntry<MixedFlowPumpBlock> MIXED_FLOW_PUMP =
    PumpworksMod.registrate()
        .block("mixed_flow_pump", MixedFlowPumpBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(p -> p.strength(2.5f, 6.0f).sound(SoundType.METAL).noOcclusion())
        .item()
        .build()
        .register();
```

在 `AllBlockEntityTypes.java` 中添加:

```java
public static final BlockEntityEntry<CentrifugalPumpBlockEntity> CENTRIFUGAL_PUMP =
    PumpworksMod.registrate()
        .blockEntity("centrifugal_pump", CentrifugalPumpBlockEntity::new)
        .validBlock(AllBlocks.CENTRIFUGAL_PUMP)
        .register();

public static final BlockEntityEntry<AxialPumpBlockEntity> AXIAL_PUMP =
    PumpworksMod.registrate()
        .blockEntity("axial_pump", AxialPumpBlockEntity::new)
        .validBlock(AllBlocks.AXIAL_PUMP)
        .register();

public static final BlockEntityEntry<MixedFlowPumpBlockEntity> MIXED_FLOW_PUMP =
    PumpworksMod.registrate()
        .blockEntity("mixed_flow_pump", MixedFlowPumpBlockEntity::new)
        .validBlock(AllBlocks.MIXED_FLOW_PUMP)
        .register();
```

---

## Phase 5~15 概要

由于篇幅限制，后续 Phase 给出**文件清单 + 关键实现提示**，具体代码模式与 Phase 3/4 相同。

### Phase 5: 储罐方块

**文件**:
- `content/tanks/IntakeTankBlock.java` — 进水池，继承 `Block`，实现 `IBE`
- `content/tanks/IntakeTankBlockEntity.java` — 实现 `IFluidHandler`（NeoForge），作为 SOURCE 节点
- `content/tanks/OutletTankBlock.java` — 出水池，作为 SINK 节点
- `content/tanks/OutletTankBlockEntity.java`

**关键**:
- 储罐实现 NeoForge 的 `IFluidHandler` 接口，可以接收/提供流体
- 储罐容量建议: 16000 mB (16 buckets)
- 使用 `FluidTank` (NeoForge 提供) 存储流体

### Phase 6: 阀门方块

**文件**:
- `content/valves/base/AbstractValveBlock.java` — 基类，有 OPEN/CLOSED 状态
- `content/valves/base/AbstractValveBlockEntity.java`
- `content/valves/gate/GateValveBlock.java` — 闸阀，手动/红石控制
- `content/valves/check/CheckValveBlock.java` — 逆止阀，自动单向
- `content/valves/butterfly/ButterflyValveBlock.java` — 缓闭蝶阀，两阶段关闭
- `content/valves/flap/FlapGateBlock.java` — 拍门

**关键**:
- 阀门有 `BooleanProperty OPEN`
- 关闭时设 `FluidNode.setActive(false)` → 图引擎自动断开该分量
- 缓闭蝶阀: 关闭时启动定时器，先 2 tick 快关到 70°，再 6 tick 慢关

### Phase 7: 防护设备

**文件**:
- `content/protection/SurgeTankBlock.java` — 调压井（大储罐，放在管道旁）
- `content/protection/AirValveBlock.java` — 空气阀（放在管道高点）

**关键**:
- 调压井: 大容量的 TANK 节点，水锤时吸收压力波
- 空气阀: 正压时打开排气，负压时打开进气

### Phase 8: 水锤系统集成

将 Phase 2 的 `WaterHammer` 类与 MC 世界集成:

**文件**:
- `fluid/FluidGraphManager.java` — 每个 Level 一个管理器
  - 管理 FluidGraph 的生命周期
  - 管理 WaterHammer 事件
  - 在 `LevelTickEvent` 中调用 `graph.tick()` 和 `waterHammer.tick()`

**关键**:
```java
// 在 LevelTickEvent.ServerPost 中:
FluidGraphManager.get(level).tick();
```

- 泵停转时触发 `waterHammer.trigger(PUMP_SHUTDOWN)`
- 阀门关闭时触发 `waterHammer.trigger(VALVE_CLOSURE)`
- 损坏管道: `pipeBlockEntity.setDamaged(true)`

### Phase 9: 仪表

**文件**:
- `content/gauges/PressureGaugeBlock.java` — 压力表
- `content/gauges/FlowMeterBlock.java` — 流量计

**关键**:
- 实现 `IHaveGoggleInformation`
- 从 `FluidNode` 读取压力/流量
- 渲染器显示指针角度

### Phase 10: Create 流体适配器

**文件**:
- `content/adapter/FluidAdapterBlock.java`
- `content/adapter/FluidAdapterBlockEntity.java` — 实现 `IFluidHandler`

**关键**:
- 一端是 FluidNode，另一端是 `IFluidHandler`
- `IFluidHandler.fill()` → 向 FluidNode 灌流体
- `IFluidHandler.drain()` → 从 FluidNode 抽流体

### Phase 11: 汽蚀系统集成

已在 `AbstractPumpBlockEntity.checkCavitation()` 中实现基础检测。

**增强**:
- 汽蚀时产生粒子效果: `level.addParticle(...)`
- 汽蚀时发出声音: `level.playSound(...)`
- 严重汽蚀时降低效率: 在 `OperatingPoint` 结果上乘以 `(1 - severity × 0.5)`

### Phase 12: UI

**文件**:
- `ui/PumpScreen.java` — 泵 GUI (继承 `AbstractContainerScreen`)
- `ui/PumpMenu.java` — 泵菜单 (继承 `AbstractContainerMenu`)
- `ui/PerformanceChartItem.java` — 性能图表物品 (右键显示曲线)

**关键**:
- GUI 中绘制 H-Q 曲线用 `GuiGraphics.fill()` 画折线
- 当前工作点用红色标记

### Phase 13: Aeronautics 兼容

**文件**:
- `compat/CompatSetup.java`
- `compat/aeronautics/SableCompat.java`

**关键**:
```java
// CompatSetup.java
public static void registerContraptionMovement() {
    // 注册所有 Pumpworks 方块为可移动
    ContraptionMovementSetting.REGISTRY.register(
        AllBlocks.COPPER_PIPE.get(),
        () -> ContraptionMovementSetting.MOVABLE
    );
    // ... 对所有方块重复
}
```

- Sable 兼容用 `ModList.get().isLoaded("sable")` 条件加载

### Phase 14: 配方与语言

**配方**: JSON 文件放在 `data/pumpworks/recipe/` 下

**语言**:
- `assets/pumpworks/lang/en_us.json`
- `assets/pumpworks/lang/zh_cn.json`

### Phase 15: 测试

1. 单元测试: `./gradlew test` (测试流体图引擎)
2. 集成测试: `./gradlew runGameTestServer` (MC 内测试)
3. 手动测试: `./gradlew runClient` (在 MC 中放置方块验证)

---

## 附录 A: 关键 API 参考

### Create KineticBlockEntity 继承关系

```
BlockEntity (MC)
 └── SmartBlockEntity (Create)
      └── KineticBlockEntity (Create)
           └── AbstractPumpBlockEntity (Pumpworks)
                ├── CentrifugalPumpBlockEntity
                ├── AxialPumpBlockEntity
                └── MixedFlowPumpBlockEntity
```

**KineticBlockEntity 关键方法**:
- `getSpeed()` → `float` — 当前转速 (RPM)
- `onSpeedChanged(float previousSpeed)` — 转速变化回调
- `isOverStressed()` → `boolean` — 是否过载

### Create Block 继承关系

```
Block (MC)
 └── KineticBlock (Create, implements IRotate)
      └── DirectionalKineticBlock (Create, 有 FACING 属性)
           └── AbstractPumpBlock (Pumpworks)
```

**DirectionalKineticBlock 关键方法**:
- `getRotationAxis(BlockState)` → `Axis` — 旋转轴
- `hasShaftTowards(...)` → `boolean` — 该方向是否有轴连接
- `FACING` — 静态属性，方块朝向

### Registrate 注册模式

```java
// 方块注册
BlockEntry<MyBlock> MY_BLOCK = registrate()
    .block("my_block", MyBlock::new)      // 名称 + 构造器
    .initialProperties(() -> Blocks.STONE) // 基础属性
    .properties(p -> p.strength(2.0f))    // 自定义属性
    .item()                               // 生成物品
    .build()
    .register();

// BlockEntity 注册
BlockEntityEntry<MyBlockEntity> MY_BE = registrate()
    .blockEntity("my_block", MyBlockEntity::new)
    .validBlock(MY_BLOCK)
    .register();
```

### IHaveGoggleInformation 实现

```java
@Override
public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
    // 用 Create 的 LangBuilder 添加格式化文本
    CreateLang.translate("pumpworks.tooltip.speed")
        .add(CreateLang.number(getSpeed()))
        .forGoggles(tooltip);
    return true;
}
```

---

## 附录 B: 常见坑与排错

### B.1: 编译错误 "找不到符号"

**原因**: Create 的类路径变化频繁。
**解决**: 确认 `create_version=6.0.10-280` 在 gradle.properties 中。去 Create 的 GitHub 确认当前版本的包路径。

### B.2: 运行时 "Mod 'create' not found"

**原因**: 开发环境没有加载 Create。
**解决**: 在 `build.gradle` 的 `runs.client` 和 `runs.server` 中添加:
```groovy
dependencies {
    // 运行时加载 Create（用于开发测试）
    localRuntime("com.simibubi.create:create-1.21.1:6.0.10-280")
}
```

### B.3: BlockEntity tick 不执行

**原因**: NeoForge 需要显式注册 ticker。
**解决**: 在 `BlockEntityBuilder` 中添加 `.ticker()` 或在 Block 中实现 `getTicker()` 方法。

### B.4: 方块放置后没有物品

**原因**: 缺少物品注册。
**解决**: Registrate 的 `.item().build()` 会自动创建对应的 BlockItem。

### B.5: 流体不流动

**原因**: FluidGraph 没有被 tick。
**解决**: 确保 FluidGraphManager 在 `ServerTickEvent.Post` 中被调用。

### B.6: 应力不消耗

**原因**: 没有正确注册应力值。
**解决**: 使用 `BlockStressValues.IMPACTS.register(block, () -> value)` 注册应力消耗。

### B.7: 方块在 contraption 中不能移动

**原因**: 没有注册 `ContraptionMovementSetting`。
**解决**: 在 `FMLCommonSetupEvent` 中注册（见 Phase 13）。

---

## 附录 C: 文件清单

以下是 v1.0 需要创建的所有文件:

### Java 源文件 (src/main/java/com/pumpworks/)

```
PumpworksMod.java
PumpworksClient.java
AllBlocks.java
AllItems.java
AllBlockEntityTypes.java
AllCreativeTabs.java

foundation/config/PumpworksConfig.java
foundation/block/PumpworksBlock.java (可选基类)

fluid/FluidNode.java
fluid/FluidEdge.java
fluid/FluidGraph.java
fluid/FluidComponent.java
fluid/FluidGraphManager.java
fluid/PumpCurve.java
fluid/OperatingPoint.java
fluid/WaterHammer.java
fluid/CavitationCalc.java
fluid/PipeResistance.java

content/pipes/PipeType.java
content/pipes/PipeBlock.java
content/pipes/PipeBlockEntity.java

content/pumps/base/AbstractPumpBlock.java
content/pumps/base/AbstractPumpBlockEntity.java
content/pumps/centrifugal/CentrifugalPumpBlock.java
content/pumps/centrifugal/CentrifugalPumpBlockEntity.java
content/pumps/axial/AxialPumpBlock.java
content/pumps/axial/AxialPumpBlockEntity.java
content/pumps/mixed/MixedFlowPumpBlock.java
content/pumps/mixed/MixedFlowPumpBlockEntity.java

content/tanks/IntakeTankBlock.java
content/tanks/IntakeTankBlockEntity.java
content/tanks/OutletTankBlock.java
content/tanks/OutletTankBlockEntity.java

content/valves/base/AbstractValveBlock.java
content/valves/base/AbstractValveBlockEntity.java
content/valves/gate/GateValveBlock.java
content/valves/gate/GateValveBlockEntity.java
content/valves/check/CheckValveBlock.java
content/valves/check/CheckValveBlockEntity.java
content/valves/butterfly/ButterflyValveBlock.java
content/valves/butterfly/ButterflyValveBlockEntity.java
content/valves/flap/FlapGateBlock.java
content/valves/flap/FlapGateBlockEntity.java

content/protection/SurgeTankBlock.java
content/protection/SurgeTankBlockEntity.java
content/protection/AirValveBlock.java
content/protection/AirValveBlockEntity.java

content/gauges/PressureGaugeBlock.java
content/gauges/PressureGaugeBlockEntity.java
content/gauges/FlowMeterBlock.java
content/gauges/FlowMeterBlockEntity.java

content/adapter/FluidAdapterBlock.java
content/adapter/FluidAdapterBlockEntity.java

compat/CompatSetup.java
compat/aeronautics/SableCompat.java

ui/PumpScreen.java
ui/PumpMenu.java
ui/PerformanceChartItem.java
```

### 资源文件 (src/main/resources/)

```
META-INF/neoforge.mods.toml

assets/pumpworks/blockstates/  (每个方块一个 JSON)
assets/pumpworks/models/block/  (每个方块一个 JSON)
assets/pumpworks/models/item/   (每个物品一个 JSON)
assets/pumpworks/textures/block/ (每个方块一个 PNG)
assets/pumpworks/textures/item/  (每个物品一个 PNG)
assets/pumpworks/lang/en_us.json
assets/pumpworks/lang/zh_cn.json
assets/pumpworks/sounds.json

data/pumpworks/recipe/  (每个配方一个 JSON)
```

### 测试文件 (src/test/java/com/pumpworks/)

```
fluid/PumpCurveTest.java
fluid/OperatingPointTest.java
fluid/FluidGraphTest.java
fluid/CavitationCalcTest.java
fluid/WaterHammerTest.java
```
