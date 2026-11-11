# 运行验证记录

## 2026-11-02 / 1.3 统一选中体积

选中行星球体的体积统一为初始总览木星的 5 倍；直径约 18.3 cm，使用半径乘以立方根 5 的换算，而非半径乘 5。太阳半径和日星间距按选中行星的同一倍率增长，用户已明确同意同比展开间距。平移与缩放共用原有缓动，返回复原全部原位置与原尺寸。

39 项 JVM 测试覆盖所有八大行星的统一体积、日星大小比、过渡中间距与半径的同比关系、无重叠以及反复复位。物理引力计算未改动。当前证据使用 `reports/size-*`，旧版 `focus-*` 为 1.2 历史证据。

## 2026-11-02 / 1.2 同步平移与复位

删除太阳所有日冕、流动光点、辉光网格及其动画类；运行时 Sun GLB 仅含光球几何。选中行星与太阳始终保持原大小、同一 y=1.55 m 水平线和原间距，使用相同的 1.2 秒缓动位移，选中行星最终位于 (0,1.55,-2.1)。其余行星只在各自原位同时渐隐；返回时恢复初始位置和不透明度，反复选择不累积偏移。

38 项 JVM 测试覆盖全部天体、精确复位、中途反向、暂停及交互门禁。原生完整任务与跨行星复位测试保留在 `reports/focus-final-instrumentation.log`；针对隐藏仪表刷新的定向回归另见 `reports/focus-targeted-instrumentation.log`。开发时检查了原生位置读回；可交付的视觉证据为 `output/pair-*` 截图与录屏，最终门禁及版本信息为 `reports/focus-delivery-verification.json`。

附件面板分别在自己的 composition 中观察主 Activity 生命周期，避免隐藏/初始空面板停留在旧值；透明最小布局防止初始零尺寸。静态轨道参考线仅在目标更换时提交几何位置，避免每帧重复修改 120 个静态点。未进行 FPS 性能评分或全设备眼动/抓取验收。

1.1 的“放大任务视图”已按本次用户要求取消；小型类地行星在任务中也保持真实显示大小比。保持可用的不可见射线命中区域和显式开始任务控制。

## 2026-11-02 / 1.1 修复

Logo 前移到 1.15 m，并在绘制时去除源图透明空白；左右面板前向距离 1.35 m，中间信息 1.30 m。选择面板改为两列四行。总览共用真实太阳/行星半径比例，任务视图明确标注放大。太阳使用 12 组曲线磁拱细丝、48 个流动点和加法混合日冕辉光；原 PBR 透明材质残留反光已修复。

30 项 JVM 测试零失败，模拟器 2 项仪器测试通过（含完整绕行成功）。最后的辉光混合和间距微调经原生截图检查；物理流程不依赖这些渲染参数。统一机器门禁全部执行且 clean=true；托管 Codegen 构建/交付通过。已在实体 B3110 头显观察到实际任务画面和选择/飞行阶段日志；不能由此声称所有眼动/抓取手势均已验证。

Editor 经恢复项目路径、重新建立会话后，仍因官方授权提示服务连接失败在模型生成前终止；失败和替代范围见 `reports/corona-editor-fallback.json`。未绕过授权。日冕使用本地自有几何及官方 Spatial SDK 动画/加法材质，GLB 验证零错误、零警告。

本次证据：`reports/comfort-release-validation.log`、`reports/comfort-final-instrumentation.log`、`reports/comfort-release-codegen-result.json`、`reports/comfort-delivery-verification.json` 和 `output/comfort-overview-final.png`。

## 1.0 基线记录

验证日期：2026-10-31；PICO CLI 0.6.1；Spatial SDK BOM 6.1.9。

## 已通过

- 官方 Stage 工程构建与 APK 签名校验，包名 `com.example.orbitmission`，版本 1.0，arm64-v8a。
- 28 项 JVM 测试（27 项业务测试、1 项模板测试），零失败；覆盖八大行星引力、稳定整圈轨道、碰撞穿越、超时、越界、暂停、输入冲突/丢失、眼动轨道吸附、语言和任务状态。
- 2 项设备仪器测试（1 项完整任务测试、1 项模板包名测试），零失败。完整任务在原生 Stage 中实时积分，实测地球任务从 FLYING 到 SUCCESS 约 30 秒。
- 自绘仪表、太阳火焰、八大行星、火箭、轨道和轨迹通过模拟器显示截图检查。五张原始截图在 `output`。
- 九项统一机器检查全部执行，`.scratch/verification_summary.json` 为 `passed=true, clean=true`，无跳过；架构与设计样式检查通过。
- 官方托管 Codegen 工作流完成，源码 artifact、固定 Gradle 构建 evidence 与交付校验通过。前一次因自定义验收 ID 不被工具识别而阻断；改用工具支持的 `scene_generated` / `project_builds` 契约 ID，应用功能仍由独立测试和截图验证。
- CLI 环境 doctor 可用，项目上下文 doctor 成功。性能分析工具的依赖及可选资源联网检查仍有非阻断警告。

## 验证范围

目标为 `Pico_Emulator_6_1`，Android 16 / API 36，arm64，设备序列号 `emulator-5554`。仪器测试调用应用正常任务操作，并等待真实场景时钟推进；未伪造成功状态或代替物理积分。

模拟器不支持当前 CLI 的 `capture screenshot --spatial` 参数，因此原始证据使用普通设备显示截图，2160×2158。截图为模拟器投影输出，不代表头显单眼画面的像素密度。行星选择、放置、飞行、成功及繁体中文设置均已捕获。

实体 PICO 头显上的眼动权限/校准、射线抓取、扳机和空间音频方位听感尚未实测。模拟器中缺少实体手柄时，官方 ControllerTrackingProvider 可能报告启动失败。应用仍提供自绘行星选择及推荐位置发射操作。

## 修复及证据

- 原生 Stage 的不可见根 Compose surface 停止请求帧，导致开场计时停滞；任务更新改为主线程单调时钟，重新验证 5 秒 Logo 与行星生成流程。
- 行星原始纹理在切换饱和度时被 SDK 自动释放；依据官方资源管理契约将可复用的两套纹理持久化，销毁场景时统一释放。失败与修复后的原生端到端日志均保留在 `reports`。
- 暂停后恢复输入/音频、手柄丢失误发射、手眼目标不一致问题已修复并加入针对性测试。

主要证据：`reports/final-tests.log`、`reports/final-validation.log`、`reports/delivery-verification.json`、`reports/runtime-captures.json`、`reports/apk-signature.txt`。

源码未从 Figma 导入，JSON 设计保真检查为不适用；无 Figma 运行或截图保真分数声明。自定义界面遵循用户明确要求，组件白名单扫描对部分合法 ECS/API 和自绘控制发出建议警告，未发现禁止的 Material 组件。


## 1.3.1 安装图标与系统名称（2026-11-05）

用户确认深蓝底火箭与金色轨道符号，不含文字。替换 Android 自适应/普通图标与 PICO 空间单层纹理；保留原脚手架圆形 SDF 蒙版，移除默认前景图层。默认英文名称 Orbit Mission，中文 Hans/Hant 均显示用户指定的简体写法「轨道使命」。aapt2 检查 APK 的 zh-CN、zh-TW、zh-HK 及英文名称正确，旧前景图层不存在；构建与 APK v2 签名验证通过。此次未执行设备安装，启动器实际呈现待安装检查。
