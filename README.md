# IDV Tooth Pliers · 牙钳

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-3C8527.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.5%2B-DBB69B.svg)](https://fabricmc.net/)

将《第五人格》牙医（Dentist）的技能组移植到 Minecraft 的 Fabric 模组。当前版本为技能移植测试版，并对部分角色技能进行了修改以适配Minecraft游戏环境，适配 **Minecraft 1.21.1**。

> 本项目为非官方粉丝作品，与网易《第五人格》官方无关。相关游戏名称、角色、音效及美术资源版权归原权利人所有。

## 功能特性

### 牙钳

- 稀有度：稀有（RARE），最大堆叠 1，默认加入创造模式「战斗」物品栏。
- 基础耐久 500 点；耐久附魔每级额外增加 50 点，最高 650 点。
- 可使用铁锭、铁粒或紫水晶碎片在铁砧中修复。
- 支持耐久、锋利、横扫之刃、火焰附加、击退等附魔。
- 亡灵杀手与节肢杀手不会作用于牙钳。
- 手持牙钳时，原版左键挖掘/攻击和右键物品使用会被拦截，改由模组技能系统处理。

### 普通攻击

- 左键单击（松开时触发判定），攻击范围为身前 **2 格**、**90° 扇形**（左右各 45°）。
- 基础伤害 3.5 点，带有 4 tick 前摇和 6 tick 攻击间隔。
- 可命中末影水晶。
- 锋利会提升伤害；横扫之刃可攻击扇形范围内的其他目标；火焰附加与击退同样生效。
- 命中生物时消耗 1 点耐久；未命中时只有短暂后摇。

### 紫外辐射（UV）

- 默认按 **右键** 开启/关闭；开启时消耗 1 点耐久。
- 从准星方向投射紫外辐射区域，水平范围 4×4 格，伤害范围向上 4 格、向下 3 格。
- 攻击时对区域内其他生物造成 2.0 点基础「细菌群」范围伤害，并受锋利附魔加成。
- 被照射区域会显示紫色框、粒子与音效，附近玩家也能看到 UV 广播效果。
- UV 需通过「角度透镜」调整照射距离。

### 角度透镜

- 默认按 **M**、**逗号（,）**、**句号（.）** 切换三档透镜。
- 三档照射距离分别为 **10 格、20 格、30 格**，默认使用 30 格档。
- 屏幕快捷栏两侧的 HUD 会显示当前档位、按键提示和技能状态。

### 沉默现身

- 默认按 **G** 发动，需要先开启 UV 并锁定有效区域。
- 向 UV 照射位置传送，并自动搜索附近的安全落脚点。
- 第一次传送消耗 1 点耐久，后摇 6 tick。
- 第一次传送后 5 秒内可再次发动第二段传送，消耗 2 点耐久，后摇 20 tick。

### 迷雾区域

- 默认按 **R** 在准星方向最远 35 格处释放迷雾。
- 长按 R 达到 8 tick 进入预览模式：左右移动鼠标旋转方向，上下移动鼠标调整释放距离，左键确认释放。
- 迷雾为半径 20 格、高度 14 格的圆柱区域，持续 500 tick（约 25 秒）。
- 区域内非玩家生物移动速度降低 50%，尝试离开时会被推回。
- 当 UV 攻击命中迷雾区域内的目标时，会触发迷雾范围伤害。

### HUD 与反馈

- 快捷栏两侧显示牙钳技能图标：左侧为三档角度透镜，右侧为 UV、沉默现身和迷雾。
- 当前档位或激活中的技能会以紫色边框高亮，并显示对应按键。
- 内置技能音效、UV 粒子、紫色区域框和区域预览渲染。

## 操作说明

| 操作 | 默认按键 | 说明 |
| --- | --- | --- |
| 普通攻击 | 鼠标左键 | 点击后发动 2 格范围、90° 扇形攻击（左右各 45°） |
| 紫外辐射开关 | 鼠标右键 | 手持牙钳时，右键仅切换 UV，不触发原版物品使用 |
| 角度透镜 1 档 | M | 照射距离 10 格 |
| 角度透镜 2 档 | `,` | 照射距离 20 格 |
| 角度透镜 3 档 | `.` | 照射距离 30 格 |
| 沉默现身 | G | 需要 UV 已锁定有效区域 |
| 迷雾区域 | R | 短按释放；长按 8 tick 预览，左键释放 |

所有按键都可以在「选项 → 控制 → 按键绑定」的「牙钳」分类中重新绑定。手持牙钳时，数字键 1–9 会被屏蔽，避免误切换物品栏。

## 合成配方

| 左 | 中 | 右 |
| :---: | :---: | :---: |
|  | 铁锭 | 紫水晶碎片 |
| 铁锭 | 铁粒 | 铁锭 |
|  | 铁锭 |  |

- 无序摆放无效，必须按照上表形状合成。
- 配方进度随材料获取解锁。

## 兼容性与依赖

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| Fabric Loader | 0.19.5 或更高 |
| Fabric API | 0.116.17+1.21.1（或兼容 1.21.1 的版本） |
| Java | 21 |
| 模组版本 | 1.0.0-1.21.1 |

- 单人游戏：安装 Fabric Loader、Fabric API 和本模组即可。
- 多人游戏：**客户端与服务端都需要安装**本模组，否则自定义技能与网络包无法同步。
- 当前仅针对 Minecraft 1.21.1 构建，不保证兼容其他版本。

## 安装

1. 安装对应 Minecraft 1.21.1 的 Fabric Loader。
2. 将 [Fabric API](https://modrinth.com/mod/fabric-api) 放入 `mods` 文件夹。
3. 下载本模组的 jar 文件并放入 `mods` 文件夹。
4. 启动游戏。

## 从源码构建

需要 JDK 21。

```bash
# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

构建产物位于 `build/libs/`：

- `idv-tooth-pliers-1.0.0-1.21.1.jar`：模组本体
- `idv-tooth-pliers-1.0.0-1.21.1-sources.jar`：源码包

开发环境运行：

```bash
# 启动客户端
gradlew.bat runClient

# 运行数据生成（配方与进度）
gradlew.bat runDatagen
```

## 项目结构

```text
src/main/java/com/joseph/toothpliers/
├── client/       # 客户端按键、HUD、渲染、音效与客户端 Tick
├── server/       # 服务端技能判定、传送、迷雾区域管理
├── network/      # C2S / S2C 自定义网络包
├── item/         # 牙钳物品
├── component/    # 技能状态数据组件
├── config/       # 技能数值配置
├── datagen/      # 配方与进度数据生成
├── sound/        # 音效注册
├── state/        # 运行时技能状态
└── mixin/        # 原版攻击、视角与附魔逻辑拦截
```

主要技能数值集中定义在 `src/main/java/com/joseph/toothpliers/config/ToothPliersConfig.java`，修改后重新构建即可生效。当前版本没有游戏内配置界面。

## 许可证

本项目以 **GNU General Public License v3.0 or later**（`GPL-3.0-or-later`）发布，完整许可证文本见 [LICENSE](LICENSE)。

Copyright (C) Joseph and contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.

> 说明：上述许可证适用于本项目的源代码。仓库中包含的来自《第五人格》或其他第三方的音效、贴图等资源，其版权仍归原权利人所有，不在 GPLv3 授权范围内，请勿用于商业用途。

## 链接

- GitHub 仓库：<https://github.com/Naib-Subedar/idv-tooth-pliers>
- 作者 Bilibili：<https://space.bilibili.com/640320953>

## 免责声明

本项目仅用于学习、研究与技能移植测试，非官方作品，不代表《第五人格》官方立场。请在遵守 Minecraft 与《第五人格》相关用户协议及当地法律的前提下使用本项目。
