# Create: Pumpworks — 设计规格文档

> **模组名称**: Create: Pumpworks
> **目标平台**: Minecraft Java Edition 1.21.1 + NeoForge
> **依赖**: Create 6.0.10 (硬依赖), Sable 1.2.1 (软依赖, Aeronautics 兼容)
> **Java 版本**: 21
> **设计日期**: 2026-06-07
> **设计状态**: 已批准

---

## 1. 概述

### 1.1 愿景

Create: Pumpworks 是 Create (机械动力) 的附属模组，将真实的水泵与泵站工程引入 Minecraft。玩家可以使用离心泵、轴流泵、混流泵三种水泵方块，通过管道网络输送流体，体验基于真实水力学的工作点计算、汽蚀检测和水锤模拟。模组同时兼容 Create: Aeronautics (机械动力：航空学)，水泵方块可以被组装进飞行载具。

### 1.2 核心特色

- **三种水泵**：离心泵（高扬程小流量）、轴流泵（大流量低扬程）、混流泵（中等），各自拥有真实的 H-Q 性能曲线
- **动能驱动**：泵由 Create 动能网络驱动，转速决定性能，应力消耗反映真实功率
- **流体节点图**：自建流体网络引擎，支持压力、流量、扬程的物理计算
- **水锤模拟**：简化压力波传播模型，停泵/关阀触发四阶段水锤过程
- **防护设备**：调压井、空气阀、逆止阀、拍门、缓闭蝶阀
- **Create 兼容**：适配器方块连接 Create 原有管道/储罐
- **Aeronautics 兼容**：所有方块注册为 Sable 可移动结构

### 1.3 美术风格

延续 Create 的蒸汽朋克/工业风——铜质、黄铜、钢铁材质，齿轮外露，铆钉细节。小型泵偏铜/黄铜质感，大型泵站基础设施偏混凝土/钢铁质感。

---

## 2. 技术架构

### 2.1 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.x | 模组加载器 |
| Create | 6.0.10 | 硬依赖，提供动能系统和方块移动 |
| Sable | 1.2.1 | 软依赖 (compileOnly)，Aeronautics 的物理引擎 |
| Java | 21 | |
| Flywheel | 1.0.6 | 渲染 (Create 依赖) |
| Registrate | MC1.21-1.3.0+67 | 注册辅助 (Create 依赖) |

### 2.2 模组包结构

```
create-pumpworks/
├── build.gradle
├── gradle.properties
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── java/com/pumpworks/
│   │   │   ├── PumpworksMod.java                    # 主入口 (@Mod)
│   │   │   ├── PumpworksClient.java                 # 客户端初始化
│   │   │   ├── AllBlocks.java                       # 方块注册
│   │   │   ├── AllBlockEntityTypes.java             # BlockEntity 注册
│   │   │   ├── AllItems.java                        # 物品注册
│   │   │   ├── AllFluids.java                       # 流体注册 (预留)
│   │   │   ├── AllCreativeTabs.java                 # 创造模式标签页
│   │   │   ├── AllPackets.java                      # 网络包注册
│   │   │   ├── AllSoundEvents.java                  # 音效注册
│   │   │   ├── content/
│   │   │   │   ├── pumps/
│   │   │   │   │   ├── base/
│   │   │   │   │   │   ├── AbstractPumpBlock.java           # 泵方块基类
│   │   │   │   │   │   ├── AbstractPumpBlockEntity.java     # 泵 BlockEntity 基类
│   │   │   │   │   │   ├── AbstractPumpRenderer.java        # 泵渲染器基类
│   │   │   │   │   │   └── PumpData.java                    # 泵参数数据
│   │   │   │   │   ├── centrifugal/
│   │   │   │   │   │   ├── CentrifugalPumpBlock.java
│   │   │   │   │   │   ├── CentrifugalPumpBlockEntity.java
│   │   │   │   │   │   └── CentrifugalPumpRenderer.java
│   │   │   │   │   ├── axial/
│   │   │   │   │   │   ├── AxialPumpBlock.java
│   │   │   │   │   │   ├── AxialPumpBlockEntity.java
│   │   │   │   │   │   └── AxialPumpRenderer.java
│   │   │   │   │   └── mixed/
│   │   │   │   │       ├── MixedFlowPumpBlock.java
│   │   │   │   │       ├── MixedFlowPumpBlockEntity.java
│   │   │   │   │       └── MixedFlowPumpRenderer.java
│   │   │   │   ├── pipes/
│   │   │   │   │   ├── PipeBlock.java                       # 管道方块
│   │   │   │   │   ├── PipeBlockEntity.java                 # 管道 BlockEntity
│   │   │   │   │   ├── PipeType.java                        # 管道类型枚举 (铜/铁/钢)
│   │   │   │   │   ├── PipeConnection.java                  # 管道连接逻辑
│   │   │   │   │   └── PipeRenderer.java                    # 管道渲染
│   │   │   │   ├── valves/
│   │   │   │   │   ├── base/
│   │   │   │   │   │   ├── AbstractValveBlock.java
│   │   │   │   │   │   └── AbstractValveBlockEntity.java
│   │   │   │   │   ├── gate/                                # 闸阀
│   │   │   │   │   ├── check/                               # 逆止阀
│   │   │   │   │   ├── butterfly/                           # 缓闭蝶阀
│   │   │   │   │   └── flap/                                # 拍门
│   │   │   │   ├── tanks/
│   │   │   │   │   ├── IntakeTankBlock.java                 # 进水池/储罐
│   │   │   │   │   ├── IntakeTankBlockEntity.java
│   │   │   │   │   ├── OutletTankBlock.java                 # 出水池/储罐
│   │   │   │   │   └── OutletTankBlockEntity.java
│   │   │   │   ├── protection/
│   │   │   │   │   ├── SurgeTankBlock.java                  # 调压井
│   │   │   │   │   ├── SurgeTankBlockEntity.java
│   │   │   │   │   ├── AirValveBlock.java                   # 空气阀
│   │   │   │   │   └── AirValveBlockEntity.java
│   │   │   │   ├── gauges/
│   │   │   │   │   ├── PressureGaugeBlock.java              # 压力表
│   │   │   │   │   ├── PressureGaugeBlockEntity.java
│   │   │   │   │   ├── FlowMeterBlock.java                  # 流量计
│   │   │   │   │   └── FlowMeterBlockEntity.java
│   │   │   │   └── adapter/
│   │   │   │       ├── FluidAdapterBlock.java               # Create 流体适配器
│   │   │   │       └── FluidAdapterBlockEntity.java
│   │   │   ├── fluid/
│   │   │   │   ├── FluidGraph.java                  # 流体节点图核心
│   │   │   │   ├── FluidNode.java                   # 节点
│   │   │   │   ├── FluidEdge.java                   # 边
│   │   │   │   ├── FluidGraphManager.java           # 图管理器 (每世界一个)
│   │   │   │   ├── PumpCurve.java                   # H-Q 曲线数学模型
│   │   │   │   ├── OperatingPoint.java              # 工作点求解器
│   │   │   │   ├── WaterHammer.java                 # 水锤压力波
│   │   │   │   ├── CavitationCalc.java              # 汽蚀余量计算
│   │   │   │   └── PipeResistance.java              # 管道阻力计算
│   │   │   ├── compat/
│   │   │   │   ├── create/
│   │   │   │   │   ├── CreateFluidBridge.java       # Create IFluidHandler 桥接
│   │   │   │   │   └── CreateContraptionCompat.java # 方块移动注册
│   │   │   │   └── aeronautics/
│   │   │   │       └── SableCompat.java             # Sable 可移动注册 (软依赖)
│   │   │   ├── ui/
│   │   │   │   ├── PumpScreen.java                  # 泵 GUI 界面
│   │   │   │   ├── PumpMenu.java                    # 泵容器菜单
│   │   │   │   ├── PerformanceChartItem.java        # 性能图表物品
│   │   │   │   └── GoggleInfoProvider.java          # 护目镜信息提供
│   │   │   └── foundation/
│   │   │       ├── config/
│   │   │       │   └── PumpworksConfig.java         # 配置类
│   │   │       ├── data/
│   │   │       │   ├── PumpDataProvider.java         # 泵数据提供者
│   │   │       │   └── recipe/
│   │   │       └── utility/
│   │   │           └── MathUtils.java               # 数学工具
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── neoforge.mods.toml
│   │       ├── assets/pumpworks/
│   │       │   ├── blockstates/
│   │       │   ├── lang/
│   │       │   │   ├── en_us.json
│   │       │   │   └── zh_cn.json
│   │       │   ├── models/
│   │       │   ├── textures/
│   │       │   └── sounds.json
│   │       └── data/pumpworks/
│   │           ├── recipe/
│   │           └── tags/
│   └── test/                                        # 单元测试
│       └── java/com/pumpworks/
│           └── fluid/
│               ├── PumpCurveTest.java
│               ├── OperatingPointTest.java
│               └── FluidGraphTest.java
```

### 2.3 依赖关系

```
Create 6.0.10 ──────→ PumpworksMod ──────→ Sable 1.2.1 (optional, compileOnly)
  (kinetics, fluids)    (核心逻辑)           (contraption movement)
```

- **Create**：硬依赖 (`implementation`)，提供动能旋转 (`KineticBlockEntity`)、方块移动 (`ContraptionMovementSetting`)、护目镜信息 (`IHaveGoggleInformation`)、应力系统 (`BlockStressValues`)
- **Sable/Aeronautics**：软依赖 (`compileOnly`)，仅用于注册方块为 Sable 可移动。不安装时通过 `@Optional` 和 `ModList.get().isLoaded()` 条件加载，不影响核心功能

---

## 3. 水泵系统

### 3.1 三种泵方块

| 泵型 | 注册名 | 进/出方向 | H-Q 曲线特征 | P-Q 特征 | 启动方式 | 适用场景 |
|------|--------|----------|-------------|---------|---------|---------|
| 离心泵 | `centrifugal_pump` | 轴向进、径向出 | 随 Q 增大单调下降，较平缓 | 随 Q 增大而增大 | **闭阀启动** (关阀功率最小) | 高扬程灌溉、加压供水 |
| 轴流泵 | `axial_pump` | 轴向进、轴向出 | 陡降，有马鞍区 | 随 Q 增大而减小 | **开阀小角度启动** | 低扬程排涝、调水 |
| 混流泵 | `mixed_flow_pump` | 轴向进、斜向出 | 介于两者之间 | 介于两者之间 | 视具体 ns | 中等扬程流量 |

### 3.2 动能集成

每种泵的 `BlockEntity` 继承 Create 的 `KineticBlockEntity`：

```
转速 (RPM) ──→ 相似律换算 ──→ 查 H-Q 曲线 ──→ 当前扬程 H、流量 Q
                ↓
           应力消耗 (SU) = P_shaft / SU_factor
           P_shaft = ρ·g·Q·H / η
```

**关键方法**：

- `getSpeed()` — 从 Create 动能网络获取当前转速
- `calculateHead()` — 根据转速和流量计算扬程
- `getStressImpact()` — 返回应力消耗值

**应力消耗计算**：

```
有效功率: P_u = ρ · g · Q · H    (游戏单位换算)
轴功率:   P = P_u / η
应力 SU:  SU = P / STRESS_FACTOR  (STRESS_FACTOR 可配置)
```

**旋转动画**：叶轮随转速旋转，速度越快旋转越快。停止时叶轮静止。

### 3.3 H-Q 曲线数学模型

每种泵用分段多项式拟合 H-Q 曲线。关键参数存储在 `PumpData` 中：

```java
public class PumpData {
    float ratedSpeed;       // 额定转速 (RPM)
    float ratedFlow;        // 额定流量 (mB/tick)
    float ratedHead;        // 额定扬程 (方块高度)
    float ratedEfficiency;  // 额定效率
    float ns;               // 比转速
    float npshr;            // 必需汽蚀余量 (NPSHr)
    float[] curveQ;         // H-Q 曲线采样点 (流量)
    float[] curveH;         // H-Q 曲线采样点 (扬程)
    float[] curveP;         // P-Q 曲线采样点 (功率)
    float[] curveEta;       // η-Q 曲线采样点 (效率)
}
```

**曲线生成规则**（基于比转速 ns）：

- **离心泵** (ns < 300)：H = H₀ - a·Q²，单调下降，较平缓
- **混流泵** (300 ≤ ns ≤ 500)：过渡形态
- **轴流泵** (ns > 500)：陡降 + 马鞍区，4 段分段函数

**转速换算**（比例律）：

```
Q' / Q = n' / n
H' / H = (n' / n)²
P' / P = (n' / n)³
```

### 3.4 运行状态计算（每 tick）

1. 获取当前转速 `n`，若为 0 则停止
2. 按比例律将额定 H-Q 曲线换算到当前转速
3. 从流体网络获取装置需要扬程 `H_need = H_static + S·Q²`
4. 用 `OperatingPoint.solve()` 求工作点（H-Q 与 H_need 交点）
5. 从工作点读取 Q、H、η、P
6. 计算应力消耗，更新 Create 动能网络
7. 检查 NPSH 条件（汽蚀检测）
8. 更新流体网络中该泵节点的流量和压力

### 3.5 汽蚀检测（简化）

```java
// NPSH_a = 吸水侧压力 - 汽化压力 - 吸水高度损失
float npsha = calcNPSHa(intakeNode, pumpNode);

// NPSH_r = 泵自身属性（查曲线或固定值）
float npshr = pumpData.getNPSHr(currentFlow);

if (npsha < npshr) {
    // 汽蚀发生
    float severity = 1.0 - (npsha / npshr);  // 0~1，越大越严重
    applyCavitationEffects(severity);
    // 效果：效率下降、振动粒子、警告文字
    // 严重时：流量断裂下降
}
```

**汽蚀效果**：
- 轻度 (severity < 0.3)：效率降低 5~15%，轻微振动粒子
- 中度 (0.3 ≤ severity < 0.7)：效率降低 15~40%，明显振动 + 噪音
- 重度 (severity ≥ 0.7)：流量断裂下降，强烈振动 + 警告文字

### 3.6 轴流泵特殊机制

- **马鞍区警告**：当工作点落入马鞍区时，触发不稳定工作警告（振动 + 效率骤降）
- **闭阀启动保护**：若在闭阀状态下启动轴流泵，触发过载警告（P-Q 曲线在 Q=0 时功率最大）
- **变角调节**（v2.0 计划）：如果安装了支持变角的系统，叶片安装角可调节，影响整条 H-Q 曲线的位置

---

## 4. 流体网络系统

### 4.1 核心架构：节点图

每个流体相关方块在图中是一个 `FluidNode`，相邻方块之间形成 `FluidEdge`。

```
[储罐] ──edge── [管道] ──edge── [泵] ──edge── [管道] ──edge── [出口]
  N1              N2            N3             N4              N5
 (水源)        (传输)        (加压)          (传输)          (输出)
```

### 4.2 FluidNode

```java
public class FluidNode {
    BlockPos pos;
    NodeType type;           // SOURCE / PIPE / PUMP / VALVE / SINK / TANK / GAUGE
    float pressure;          // 当前压力 (mB 水柱高度)
    int capacity;            // 容量 (mB)
    int currentFluid;        // 当前流体量 (mB)
    FluidStack fluid;        // 流体类型
    int elevation;           // Y 坐标（海拔高度）
    boolean active;          // 是否参与计算
}
```

### 4.3 FluidEdge

```java
public class FluidEdge {
    FluidNode from;
    FluidNode to;
    float flowRate;          // 流量 (mB/tick)，正为 from→to
    float resistance;        // 流动阻力
    float inertia;           // 水流惯性系数（水锤用）
    float maxFlow;           // 最大流量（管径限制）
    float maxPressure;       // 最大耐压
}
```

### 4.4 FluidGraphManager

每个世界维护一个 `FluidGraphManager`，管理所有连通分量：

```java
public class FluidGraphManager {
    Map<BlockPos, FluidNode> nodes;
    Map<Pair<BlockPos, BlockPos>, FluidEdge> edges;
    List<FluidComponent> components;  // 连通分量列表

    void onBlockPlaced(BlockPos pos, BlockEntity be);
    void onBlockRemoved(BlockPos pos);
    void onSpeedChanged(BlockPos pos);
    void onValveChanged(BlockPos pos, boolean open);
    void tick(Level level);
}
```

### 4.5 每 tick 计算流程

```
1. 检测图变化（方块放置/破坏/阀门开关）→ 重建受影响的连通分量
2. 对每个连通分量：
   a. 收集所有泵节点 → 按 H-Q 曲线 + 当前转速计算可提供扬程
   b. 计算管路总阻抗 S（各边阻力串联/并联计算）
   c. 计算净扬程 H_static = max(outlet.elevation) - min(inlet.elevation)
   d. 求解工作点：H_pump(Q) = H_static + S·Q²
      → 使用二分法 (5~10 次迭代收敛)
   e. 分配各边流量（按阻力比例分配并联路径）
   f. 更新各节点压力和流体量
   g. 执行流体传输（从源到汇）
3. 检查所有泵节点的汽蚀条件
4. 检查阀门状态变化 → 触发水锤事件
5. 检查管道过压 → 触发破损
```

### 4.6 工作点求解

```java
public class OperatingPoint {
    float flow;       // 工作流量 (mB/tick)
    float head;       // 工作扬程
    float efficiency; // 效率
    float power;      // 轴功率

    static OperatingPoint solve(PumpCurve pumpCurve, float hStatic, float pipeS) {
        // 泵提供扬程: H_pump(Q) — 查曲线
        // 管路需要扬程: H_need(Q) = H_static + S · Q²
        // 工作点: H_pump(Q) = H_need(Q)
        // 二分法求解
        float lo = 0, hi = pumpCurve.maxFlow;
        for (int i = 0; i < 10; i++) {
            float mid = (lo + hi) / 2;
            float hp = pumpCurve.getHead(mid);
            float hn = hStatic + pipeS * mid * mid;
            if (hp > hn) lo = mid;
            else hi = mid;
        }
        float q = (lo + hi) / 2;
        return new OperatingPoint(q, pumpCurve.getHead(q), ...);
    }
}
```

### 4.7 管道方块

| 类型 | 注册名 | 管径 | 阻力系数 | 最大流速 | 最大耐压 |
|------|--------|------|---------|---------|---------|
| 铜管 | `copper_pipe` | 小 | 高 | 低 | 低 |
| 铁管 | `iron_pipe` | 中 | 中 | 中 | 中 |
| 钢管 | `steel_pipe` | 大 | 低 | 高 | 高 |

**管道连接**：自动检测相邻管道/泵/阀门/储罐，形成边。不同管径的管道可以连接，连接处取较小管径的限制。

**管道破损**：超过最大耐压时，方块变为 `damaged` 状态 → 漏水粒子 + 流体损失。需用 `pipe_wrench` 修复。

### 4.8 与 Create 流体交互

**适配器方块** `fluid_adapter`：
- 一端实现 Pumpworks 的 `FluidNode` 接口
- 另一端实现 NeoForge 的 `IFluidHandler` 接口
- 将 Pumpworks 的压力/流量转换为 Create 可理解的流体传输
- 双向支持：Pumpworks → Create 和 Create → Pumpworks

### 4.9 性能优化

| 策略 | 说明 |
|------|------|
| **脏标记** | 只在方块变化/速度变化时重建图结构 |
| **分量隔离** | 每个连通分量独立计算，互不影响 |
| **计算降频** | 大型网络 (>64 节点) 每 2 tick 计算一次 |
| **节点上限** | 单连通分量最多 256 个节点 |
| **缓存** | 管路阻抗 S 缓存，仅在拓扑变化时重算 |

### 4.10 流体类型支持

**v1.0 支持**：水、熔岩、Create 自带流体（牛奶、蜂蜜、巧克力、药水等）

**通用兼容**：所有 NeoForge `IFluidHandler` 流体均可通过适配器接入。流体属性（密度、粘度）从 NeoForge 流体属性读取，影响泵的做功效率：

```java
float densityFactor = fluid.getFluid().getFluidType().getDensity() / 1000f; // 以水为基准
// 密度越大，同等转速下扬程越低、应力消耗越大
```

---

## 5. 水锤系统

### 5.1 触发条件

| 事件 | 类型 | 影响级别 |
|------|------|---------|
| 动能突然断开（齿轮被拆、动力停止） | 事故停泵 | **最严重** |
| 阀门快速关闭 | 关阀水锤 | 严重 |
| 泵突然启动 | 启动水锤 | 轻微 |
| 逆止阀关闭 | 停泵后倒流截断 | 中等 |

### 5.2 简化压力波模型

不采用完整特征线法，而是用**离散段传播**模型：

```
触发时刻 t=0：
  计算压力变化 ΔP：
    停泵: ΔP = -ρ·c·v   (降压波，c 为波速，v 为流速)
    关阀: ΔP = +ρ·c·v   (升压波)

每 tick (传播一步)：
  压力波从当前管道段传播到相邻段：
    P_next = P_prev × decay
    decay = 0.85 + 0.10 × (pipeType == STEEL ? 1.0 : 0.5)
    // 钢管衰减慢（波传得远），铜管衰减快

  波速: 每 tick 传播 2~4 段管道（可配置）

  到达防护设备时：
    调压井 → 吸收压力波，波不再向该方向传播
    空气阀 → 负压时进气缓冲，正压时排气

  受影响的管道段：
    若 P > pipe.maxPressure → 管道损坏（泄漏 + 流量损失）
    若 P < 0 (负压) → 气穴形成 → 下一 tick 水柱弥合冲击
```

### 5.3 停泵水锤四阶段

```
阶段 ① 水泵工况衰减 (tick 0~3)：
  泵停止做功，出口压力骤降
  产生降压波向出水侧传播
  管道内水流因惯性继续向前，泵出口处出现低压

阶段 ② 制动工况 (tick 3~6)：
  出水侧高压水开始倒流
  倒流水对仍在正转的叶轮起制动作用
  泵出口压力开始回升

阶段 ③ 水轮机工况 (tick 6~10)：
  叶轮开始反转
  倒流量达到最大后减小
  泵出口压力急剧升高至最大正压 ← 最危险！
  若无防护，此处最易爆管

阶段 ④ 飞逸稳定 (tick 10+)：
  反转达到飞逸转速，输出转矩为零
  倒流量稳定
  压力在出水池静水头附近波动衰减
```

### 5.4 防护设备

| 方块 | 注册名 | 功能 | 放置位置 |
|------|--------|------|---------|
| **调压井** | `surge_tank` | 竖立水箱，吸收压力波，缩短传播距离 | 管道中段 |
| **空气阀** | `air_valve` | 正压排气、负压进气破真空，缓冲水锤 | 管道高点 |
| **逆止阀** | `check_valve` | 防止倒流保护泵（有冲击） | 泵出口 |
| **拍门** | `flap_gate` | 出水口单向阀，停机自动关闭 | 出水口末端 |
| **缓闭蝶阀** | `butterfly_valve` | 两阶段关闭（先快后慢），减少升压 | 泵出口或管道中 |

**缓闭蝶阀两阶段关闭**：
- 快关阶段：断电后 0~70° 在 2 tick 内完成，快速限流防倒转
- 慢关阶段：70°~90° 在 6 tick 内完成，避免升压过大

### 5.5 视觉效果

| 事件 | 效果 |
|------|------|
| 水锤发生 | 管道震动动画 + 金属敲击音效 (`water_hammer_hit`) |
| 管道过压 | 红色警告粒子 + 压力警告文字 |
| 管道破裂 | 水流喷射粒子 + 破损模型 + 漏水音效 |
| 虹吸破坏 | 真空破坏阀进气动画 + 吸气音效 |
| 汽蚀 | 泵体微振 + 气泡粒子 + 嘶嘶声 |

---

## 6. 阀门系统

### 6.1 闸阀 `gate_valve`

- 开关控制：右键或红石信号切换开/关
- 全开时阻力极小，全关时完全截断
- 开关有动画（阀门轮旋转）

### 6.2 逆止阀 `check_valve`

- 自动单向阀：只允许正向流动
- 反向流动时自动关闭，产生中等水锤冲击
- 常用于泵出口防止倒流

### 6.3 缓闭蝶阀 `butterfly_valve`

- 可手动/红石控制开关
- 关闭时自动执行两阶段关闭
- 减少水锤升压，兼具逆止功能

### 6.4 拍门 `flap_gate`

- 放置在出水口末端
- 运行时水流冲开，停机时重力自闭
- 防止外水倒灌

---

## 7. 储罐与仪表

### 7.1 进水池/储罐 `intake_tank`

- 作为流体源节点（SOURCE 类型）
- 可手动灌装或接收外部流体
- 水位可视化
- 提供 NPSH_a 计算的水源压力

### 7.2 出水池/储罐 `outlet_tank`

- 作为流体汇节点（SINK 类型）
- 接收管道输送的流体
- 水位可视化
- 满载时溢出（流体从顶部流出）

### 7.3 压力表 `pressure_gauge`

- 放置在管道上，显示当前节点压力
- 指针动画随压力变化
- 戴护目镜时显示精确数值

### 7.4 流量计 `flow_meter`

- 放置在管道上，显示当前边流量
- 涡轮旋转动画随流量变化
- 戴护目镜时显示精确数值

---

## 8. Aeronautics 兼容性

### 8.1 早期目标 (v1.0)：结构兼容

所有 Pumpworks 方块通过 Create 的 `ContraptionMovementSetting.REGISTRY` 注册为 `MOVABLE`：

```java
// CreateContraptionCompat.java
for (Block block : AllBlocks.ALL_BLOCKS) {
    ContraptionMovementSetting.REGISTRY.register(block, () -> ContraptionMovementSetting.MOVABLE);
}
```

- 当 Sable 物理引擎组装子级别 (airship/vehicle) 时，泵方块可以被组装进去
- 泵的 `BlockEntity` 实现 Sable 的序列化接口，确保数据在组装/拆解时正确保存
- 泵在载具上时，流体网络临时暂停该分量的计算（v1.0 限制）

### 8.2 Sable 集成（软依赖）

```java
// SableCompat.java — 仅在 Sable 存在时加载
@Mod.EventBusSubscriber(modid = "pumpworks")
public class SableCompat {
    @SubscribeEvent
    public static void onGatherMovementBlocks(GatherMovableBlocksEvent event) {
        // 注册所有 Pumpworks 方块为 Sable 可移动
    }
}

// 加载条件
if (ModList.get().isLoaded("sable")) {
    // 注册 Sable 兼容事件监听
}
```

### 8.3 后期目标 (v2.0)

- **载具流体系统**：飞艇压载水（水箱配重影响飞行高度）
- **载具燃油输送**：引擎使用管道中的液体燃料
- **移动流体接口**：类似 Create 的 `PortableFluidInterface`，但支持压力传输
- **飞行中的泵运行**：泵在载具上也能接收动能并工作

---

## 9. UI 与信息显示

### 9.1 工程护目镜集成

所有 Pumpworks 方块实现 Create 的 `IHaveGoggleInformation` 接口：

**戴护目镜看泵**：
```
┌─ 离心泵 ──────────────┐
│ 转速: 256 RPM          │
│ 流量: 1200 mB/tick     │
│ 扬程: 48 blocks        │
│ 效率: 78%              │
│ 应力: 128 SU           │
│ NPSH: ✓ 安全 (余量 2.1m)│
└────────────────────────┘
```

**戴护目镜看管道**：
```
┌─ 铁管道 ──────────────┐
│ 压力: 32 blocks        │
│ 流量: 800 mB/tick →    │
│ 流体: Water            │
│ 耐压: 128 blocks       │
└────────────────────────┘
```

### 9.2 泵 GUI

右键点击泵打开配置面板（`PumpScreen`），包含：
- H-Q 曲线图（当前工作点用红色标记）
- P-Q 曲线图
- η-Q 效率曲线
- NPSH 状态指示
- 当前运行参数实时数值
- 轴流泵额外显示叶片角度（v2.0）

### 9.3 性能图表物品

`performance_chart` 物品：
- 右键使用弹出全屏 H-Q / P-Q / η-Q 曲线图
- 标注当前工作点位置
- 若有多台相同泵并联，显示合成曲线
- 纯客户端渲染，无 NBT 数据

---

## 10. 方块与物品清单

### 10.1 方块 (v1.0)

| 类别 | 方块 | 注册名 | 材质 |
|------|------|--------|------|
| **泵** | 离心泵 | `centrifugal_pump` | 铜/黄铜 |
| | 轴流泵 | `axial_pump` | 钢铁 |
| | 混流泵 | `mixed_flow_pump` | 铜/铁混合 |
| **管道** | 铜管 | `copper_pipe` | 铜 |
| | 铁管 | `iron_pipe` | 铁 |
| | 钢管 | `steel_pipe` | 钢 |
| **阀门** | 闸阀 | `gate_valve` | 铁 |
| | 逆止阀 | `check_valve` | 铁/铜 |
| | 缓闭蝶阀 | `butterfly_valve` | 钢铁 |
| | 拍门 | `flap_gate` | 铁 |
| **储罐** | 进水池 | `intake_tank` | 石/铜 |
| | 出水池 | `outlet_tank` | 石/铜 |
| **防护** | 调压井 | `surge_tank` | 石/钢 |
| | 空气阀 | `air_valve` | 铜 |
| **仪表** | 压力表 | `pressure_gauge` | 铜/玻璃 |
| | 流量计 | `flow_meter` | 铜/玻璃 |
| **接口** | 流体适配器 | `fluid_adapter` | 铜/安山合金 |

### 10.2 物品 (v1.0)

| 物品 | 注册名 | 用途 |
|------|--------|------|
| 离心泵叶轮 | `centrifugal_impeller` | 合成材料 |
| 轴流泵叶片 | `axial_blade` | 合成材料 |
| 混流泵叶轮 | `mixed_impeller` | 合成材料 |
| 管道扳手 | `pipe_wrench` | 修复破损管道 |
| 性能图表 | `performance_chart` | 查看泵曲线 |

---

## 11. 配方设计思路

### 11.1 泵

```
离心泵 = 精密构件(Create) + 离心泵叶轮 + 铜机壳 + 安山合金轴
轴流泵 = 精密构件(Create) + 轴流泵叶片×3 + 铁机壳 + 钢轴
混流泵 = 精密构件(Create) + 混流泵叶轮 + 铜铁机壳 + 铁轴
```

### 11.2 管道

```
铜管 ×4 = 铜板 ×3 (卷制)
铁管 ×4 = 铁板 ×3
钢管 ×4 = 钢板 ×3 (Create 的钢来自高炉)
```

### 11.3 阀门

```
闸阀 = 铁板 ×2 + 红石粉 + 铁棍
逆止阀 = 铁板 + 铜板 + 弹簧(Create)
缓闭蝶阀 = 钢板 ×2 + 精密构件 + 红石粉
拍门 = 铁板 ×2 + 铰链
```

---

## 12. 版本规划

### v1.0 — 核心功能 (首版)

| 功能 | 优先级 |
|------|--------|
| 三种泵方块 + 动能集成 + H-Q 曲线 | P0 |
| 三种管道 + 流体节点图网络 | P0 |
| 四种阀门 | P0 |
| 简化水锤 + 五种防护设备 | P0 |
| 汽蚀检测 | P1 |
| 仪表 + 护目镜信息 | P1 |
| 泵 GUI + 性能图表物品 | P1 |
| Create 流体适配器 | P1 |
| Aeronautics 结构兼容 (方块可移动) | P1 |
| 中英文语言文件 | P2 |

### v2.0 — 泵站工程

| 功能 | 说明 |
|------|------|
| 前池、进水池、引渠 | 泵站进水系统基础设施方块 |
| 进水流道 | 肘形/钟形/簸箕形 |
| 虹吸式出水流道 | 含真空破坏阀 |
| 并联泵管理 | 多台泵合成 H-Q 曲线 |
| 载具流体系统 | 飞艇压载水、燃油输送 |
| 变角调节 | 轴流泵叶片角度调节 |
| 泵站监控 | 红石/CC:Tweaked 集成 |

### v3.0 — 高级仿真

| 功能 | 说明 |
|------|------|
| 特征线法水锤 | 可选项，精确水力过渡过程 |
| 泥沙淤积 | 前池/引渠泥沙模拟 |
| 多相流 | 气液混合流体 |
| 大型泵站规划 | 多站分级系统 |

---

## 13. 配置项

```java
public class PumpworksConfig {
    // 流体网络
    int maxNodesPerComponent = 256;       // 单连通分量最大节点数
    int calcIntervalLargeNetwork = 2;     // 大型网络计算间隔 (tick)
    int largeNetworkThreshold = 64;       // 大型网络阈值 (节点数)

    // 水锤
    boolean enableWaterHammer = true;     // 是否启用水锤
    float waterHammerDecay = 0.90f;       // 压力波衰减系数
    int waveSpeedBlocksPerTick = 3;       // 波速 (方块/tick)
    boolean enablePipeDamage = true;      // 是否启用管道过压损坏

    // 汽蚀
    boolean enableCavitation = true;      // 是否启用汽蚀
    float cavitationSeverityMultiplier = 1.0f;  // 汽蚀严重程度倍率

    // 应力
    float stressFactor = 1.0f;            // 应力消耗倍率

    // 管道
    float copperPipeMaxPressure = 64.0f;  // 铜管最大耐压
    float ironPipeMaxPressure = 128.0f;   // 铁管最大耐压
    float steelPipeMaxPressure = 256.0f;  // 钢管最大耐压
}
```

---

## 14. 关键设计决策记录

| 决策 | 选择 | 理由 |
|------|------|------|
| 流体网络 | 自建节点图 | Create 原版流体系统无扬程/压力概念，无法满足水泵物理模拟需求 |
| 水锤模拟 | 简化压力波传播 | 完整特征线法计算量大且对游戏来说过度仿真，简化模型保留核心体验 |
| 工作点求解 | 二分法 | 简单可靠，5~10 次迭代足够精度，无需导数 |
| H-Q 曲线 | 分段多项式 | 灵活拟合各种泵型，支持从比转速自动生成 |
| Aeronautics | 软依赖 | 不强制安装，降低用户门槛 |
| Create 流体 | 适配器桥接 | 松耦合，Create 更新时适配成本低 |
| 管道 | 独立方块系统 | 需要压力/耐压属性，Create 管道不支持 |
