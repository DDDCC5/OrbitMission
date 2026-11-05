# 轨道使命 / Orbit Mission — 第一阶段素材审阅

应用包名：`com.example.orbitmission`。本次仅交付素材、设计稿及制作工具；按用户要求，等待全部素材确认后才创建 Kotlin / Compose / PICO Spatial 应用并构建 APK。

## 审阅入口

双击 `assets/index.html`，离线查看模型渲染、动效、全部界面设计和全部音频。模型文件位于 `assets/models/`；这些是实际可导入的 GLB，不是用图片代替的模型。

| 素材 | 文件与内容 |
| --- | --- |
| 太阳和八大行星 | 9 个独立 GLB，内嵌贴图、独立旋转节点；土星含环 |
| 载人火箭 | `crew_rocket.glb`，分段主箭体、逃逸塔、4 个助推器及喷管，原创中国航天涂装 |
| 太阳动效 | 32 个发光网格粒子节点，GLB 中有连续脉动动画；`sun-earth-motion.gif` 展示材质自转和表面粒子 |
| UI | 原创 SVG Logo、面板边框、按钮 4 状态、图标、凝视标记、倒计时仪表、开关；无原生圆角矩形样式 |
| 画面设计 | 开场、选择行星、教程、放置、飞行、成功、失败、三语设置、三语排版 |
| 三语文字 | `assets/locales/{zh-Hans,zh-Hant,en}.json`，三语键完全一致，各 47 项 |
| 背景音乐 | `deep_space_loop_60s.wav`，原创合成深空氛围音乐，24 kHz / 16-bit / stereo |
| 语音 | 3 语言 × 6 提示 = 18 WAV，24 kHz / 16-bit / mono |
| 音效 | 点击、引擎、成功、失败、碰撞 5 WAV，24 kHz / 16-bit / mono |

## 需要在确认时了解的素材状态

- 火箭是参考长征系列特征的原创建模资产，并非特定型号的精密尺寸复刻。预览为材质和外形审阅。
- 语音由本机 TTS 生成：简体 `Binbin (Enhanced)`、繁体 `Eddy (中文（台湾）)`、英语 `Daniel`。语速 145；请试听确认是否符合严肃专业的播报要求。
- 太阳当前交付的是带动画的发光网格粒子素材。PICO 运行时粒子系统、光效、空间音频和交互属于第二阶段，尚未在设备上验证。
- GLB 自转预览为便于观看的 24 秒一圈；应用中的真实恒星自转周期单独记录于 `assets/models/manifest.json`，不得把预览动画速度当成真实速度。
- 天体贴图采用真实数据衍生的可视化贴图，不能视为所有区域的严格测绘真值；气态行星展示可见大气。金星另附地表和大气贴图。
- `ui-concept.png` 为 AI 视觉方向探索，存在“发射倒计时”等概念稿文案，不作为实现规格。最终文案以三语 JSON 和编号界面稿为准。其他模型图为本地网格/材质渲染；所有界面均是设计稿，不是运行截图。

## PICO 环境配置结果

已执行用户指定的四步：Node/npm/Git 预检；`npm install -g @picoxr/pico-cli`；指定参数的 `setup`；两项 JSON doctor。

- Node 22.23.2 / npm 10.9.8 / Git 2.54.0。
- CLI 0.6.1 (public)，本次安装路径：`/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/bin/pico-cli`。
- 系统还存在旧的 `/Users/chanzungsan/.local/bin/pico-cli`（0.5.0）；本次配置和检查均显式优先新路径。后续终端使用前应确认 PATH，避免误调用旧版。未改动用户全局 shell 配置。
- Spatial 插件 0.6.1；知识库 6.1；Emulator 6.1.0；Editor 6.1.9；SDK 状态 `no-project`（尚未创建应用）。
- `doctor` 为 `PARTIAL`：仅性能分析工具依赖有非阻塞警告。`project context doctor` 为 `SUCCESS`。原始结果见 `reports/doctor.json` 和 `reports/context-doctor.json`。
- setup 安装了项目本地技能及 MCP 配置，当前聊天未原生加载这些新增 MCP。已通过本机 CLI 网关验证 Editor 启动；后续原生 MCP 使用应重新打开项目聊天/重启宿主后检查，不宣称当前宿主已经热加载。

## Editor 工作流阻塞与替代路径

正式启动过受管理工作流 `run-60ea13c2-cfb2-4dd7-812b-e73e79fcaeb0`。Editor 启动成功，场景规格与模型计划已提交；在 `scene_preparation` 阶段失败，10 个模型均未开始生成。

错误：`WORKFLOW_EXECUTION_FAILED`，`Failed to get execute_script auth token before lifecycle switch`，无法连接官方 `mcp_prompts` 提示服务；直接连通性检查得到 HTTP 404。终态及错误保存于 `reports/editor-models.json`。未绕过授权执行 Editor 脚本。

因此本包采用本地网格生成和软件渲染，不是 Editor 管理工作流的成功交付，不包含伪造的 `.bundle` 或工作流验收证明。GLB 是否能在最终选择的 PICO 渲染路径直接消费，以及是否需转换/打包，将在第二阶段按实际 SDK 公共能力处理。

## 第二阶段实现约定

确认素材后再实现：5 秒开场、顺序生成行星、50% 去饱和与悬停恢复、眼动/手柄输入、抓取释放、推荐轨道、引力积分、推力、姿态、参数、90 秒判定、完整一圈、重试、教程、三语及音频；最后构建 debug APK 和验证运行截图。

物理使用 SI 单位，显示尺度独立。太阳和行星同时影响测试火箭属于受限三体近似。每个引力项使用 `G M m / r²`；地球约 9.8 m/s² 作校验，不能只按质量比例忽略半径。真实飞行周期与 90 秒玩法需明确统一的模拟时间倍率。以上是实现前的工程说明，未编写应用代码。

## 来源与许可

- 行星、太阳、土星环原始贴图：[Solar System Scope / INOVE](https://www.solarsystemscope.com/textures/)，[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)。贴图基于 NASA 数据，原作者说明未测绘区域可能采用拟合地形。地球贴图在本包中合成了同源云层。下载清单见 `reports/texture-downloads.json`。
- 自转周期依据 [NASA Planetary Fact Sheet](https://nssdc.gsfc.nasa.gov/planetary/factsheet/) 的汇总值及 [Sun Fact Sheet](https://nssdc.gsfc.nasa.gov/planetary/factsheet/sunfact.html)。汇总值有舍入，金星/天王星保留逆行符号。
- 几何、SVG、配乐、音效由本项目制作；语音为上述系统声线合成。
- UI 概念图由内置 imagegen 生成。提示词见 `reports/imagegen-prompt.txt`，未使用 CLI/API 图像生成回退。
- 官方配置参考：[Install and initialize PICO CLI](https://developer.picoxr.com/document/pico-cli/quickstart-common/)。
