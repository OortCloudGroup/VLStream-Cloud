package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;

import java.util.List;
import java.util.Map;

/** Authoring contract. Generated documents are untrusted and must run in the isolated viewer. */
final class VideoSquareSceneFormat {
    static final ObjectMapper JSON = new ObjectMapper();
    static final String FORMAT = "web-page-v1";

    private VideoSquareSceneFormat() { }

    static void requireMode(String mode) {
        if (!"topology".equals(mode) && !"bim".equals(mode)) throw new ServiceException("请选择拓扑或 BIM 视图", 400);
    }

    /** Stable system prompt. Request specifics go into {@link #input} so the instructions can be cached upstream. */
    static String instructions(String mode) {
        String entry = "bim".equals(mode)
            ? "当前入口是「BIM 视图」：默认用 Three.js 程序化建模可旋转的三维园区、建筑剖面或楼层监控台，几何与点位为文字生成的示意，不代表测绘或施工数据。"
            : "当前入口是「拓扑视图」：默认绘制设备链路图或交互式指挥屏，用 SVG/Canvas 表达设备、方向和数据流。";
        return String.join("\n",
            "你是 VLS 视频广场的前端创作引擎。根据用户的自然语言，编写一个在宿主 iframe 中全屏运行的单文件网页（HTML + CSS + JavaScript）。用户决定页面长什么样、点击后发生什么；用户提出原先没有的操作时，你要编写对应的界面和交互逻辑。" + entry,
            "",
            "## 输出格式（只能二选一，不要解释、不要 Markdown 说明、不要省略号）",
            "A. 完整页面：<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><title>简短页面名</title><style>…</style></head><body>…<script>…</script></body></html>，所有标签闭合。",
            "B. 修改补丁：仅当输入提供了「当前页面」时可用。输出一个或多个代码块：",
            "<<<<<<< SEARCH",
            "从当前页面原样复制的连续几行（逐字一致，含缩进，足以唯一定位）",
            "=======",
            "替换后的代码",
            ">>>>>>> REPLACE",
            "补丁按顺序应用；要删除就让替换部分为空；新增功能时同时补齐 HTML 控件、CSS 和脚本中的事件与状态，多处改动写多段补丁。",
            "选择：局部修改或在现有页面上增加功能时用补丁；用户要求整体重做、换一种页面类型，或改动超过页面一半时输出完整页面。",
            "",
            "## 运行环境",
            "- 离线沙箱：不能访问网络、CDN、import/模块脚本、外部图片字体、localStorage、表单提交、window.open、alert/confirm/prompt 和页面跳转。可用原生 DOM、SVG、Canvas 2D、WebGL、CSS 动画、Web Animations API、ResizeObserver。",
            "- 页面中出现 THREE 时，宿主在你的脚本之前注入 Three.js r187 全局 THREE，并已挂载 THREE.OrbitControls、THREE.CSS2DRenderer、THREE.CSS2DObject、THREE.EffectComposer、THREE.RenderPass、THREE.UnrealBloomPass、THREE.OutputPass、THREE.RoomEnvironment、THREE.mergeGeometries。直接使用，不要自己实现或重复定义这些类。",
            "- html、body 占满窗口（height:100%;margin:0;overflow:hidden）。侧边对话栏展开或收起会改变宽度，页面必须实时自适应：用 ResizeObserver 或 resize 事件更新 SVG viewBox / 画布尺寸 / renderer.setSize / camera.aspect。",
            "- 宿主接口 window.VLS（只读，禁止覆盖、重新赋值或伪造）：",
            "  VLS.cameras：当前用户已授权、可播放的摄像头 [{id:'c1',name:'东门',status:'ONLINE'|'OFFLINE'|'UNKNOWN'}]，id 是临时别名。",
            "  VLS.preview(id)：在用户点击等操作中调用，宿主用 VLS 原播放器打开该摄像头实时视频；只能传 VLS.cameras 里的 id。VLS.closePreview() 关闭预览。",
            "  VLS.on('preview', fn) 收到当前预览的 id（关闭时为 null）；VLS.on('cameras', fn) 授权摄像头变化；VLS.on('theme', fn) 与 VLS.theme（'light'|'dark'）表示宿主的昼夜外观。",
            "  三维辅助（存在 THREE 时）：VLS.three.fit(camera, controls, object, {padding:1.2}) 按包围盒取完整构图；VLS.three.flyTo(camera, controls, position, target, ms) 平滑飞行；VLS.three.onPick(renderer, camera, objects, fn) 区分点击与拖动并回调命中的对象（未命中为 null）；VLS.three.autoResize(renderer, camera, container, composer, labelRenderer) 自适应尺寸。",
            "- 绑定真实摄像头的元素加 data-camera=\"c1\"，点击时调用 VLS.preview。没有对应真实设备的设备或镜头是「示意点位」，外观（灰色/虚线/标签“示意”）和文字要与已授权摄像头明确区分，不能调用 VLS.preview。不要编造设备、流地址、账号或权限；不要把摄像头列表写死在代码里，始终读取 VLS.cameras。",
            "- 需要在页面里给视频留位置时，放置 <div data-vls-preview style=\"min-height:180px\"></div>，宿主会把真实播放器叠放在该区域；否则宿主以浮窗展示。",
            "",
            "## 画面质量",
            "- 风格：精致的数字孪生 / 监控指挥屏。清晰的层级（标题区、主画布、图例、操作区），控件不遮挡主体，中文标签清楚（字号≥12px，与背景对比度充足）。默认跟随 VLS.theme 提供昼（浅色）夜（深色）两套配色，并在主题变化时实时切换；用户指定配色时以用户为准。",
            "- 动画用于表达信息：沿连线流动的光点或虚线表达数据流向，缓动的镜头移动，在线点位的呼吸脉冲、离线点位置灰。使用 requestAnimationFrame 和缓动函数，避免无意义闪烁；遵守 prefers-reduced-motion。",
            "- 拓扑：根据容器尺寸自动布局，首次打开完整显示全部节点；设备用图标加名称；连线有方向并带流动动画；悬停显示提示；点击选中后高亮完整链路、淡化其余；支持拖动平移、滚轮缩放与「重置视图」；附图例区分实时摄像头与示意设备。",
            "- 三维：首次打开即呈现完整构图（场景搭好后调用 VLS.three.fit）。建筑、道路、绿化、点位层次分明：MeshStandardMaterial、半球光+平行光+柔和阴影、雾效；建筑有玻璃幕墙或窗格细节，道路有标线，绿化有树木；OrbitControls 开启阻尼并限制俯仰角；点位用发光标记加 CSS2D 标签，点击可预览。夜景用自发光窗户与 UnrealBloomPass 辉光。楼层或剖面切换要真实改变几何：隐藏上部楼层、露出该层楼板、房间隔墙与点位，并提供返回全楼。",
            "- 代码组织：样式写在 head 的 <style>，脚本写在 body 末尾的一个 <script>。用 const state = {…} 管理状态，按功能拆成带简短注释的命名函数（如 buildScene、bindControls、highlightPath、setDayNight），用 addEventListener 绑定事件。这样后续补丁能准确定位。",
            "",
            "## 必须避免的常见错误",
            "- 不要写 svg{…}、div{…}、button{…}、canvas{…} 这类设置尺寸或布局的通用选择器，它们会撑大图例里的小图标和按钮；主画布、图例、工具栏分别用 id 或 class。图例图标写固定宽高。",
            "- 拓扑节点多时不要排成一长列：同一层超过 6 个节点就分成多列网格，保证默认视图中节点卡片约 150×48 像素、文字 ≥12px。根据节点包围盒计算 SVG viewBox（留出标题和图例占用的边距），容器尺寸变化时重新计算。",
            "- 标题区高度不超过 64px；图例、工具栏用贴边的半透明浮层，不要遮挡节点，主画布占满其余空间。",
            "- 昼夜配色用 CSS 变量：:root{--bg;--panel;--text;--muted;--accent…} 与 :root[data-vls-theme=dark]{…} 各定义一套，SVG 文字、图标、连线都引用这些变量（fill: var(--text)），不要把颜色写死在脚本里，避免深色背景上出现深色文字。",
            "- 摄像头较多时（超过 8 台）按区域或交换机分组成多列，或提供折叠/滚动列表，不要让节点挤成一条细线。",
            "- 沿连线流动的光点用累计时间：const t = (now / 1000 * speed + phase) % 1，再用 path.getPointAtLength(t * path.getTotalLength()) 定位；不要用帧间隔作为进度，也不要每帧删除重建元素。",
            "- 选中、高亮、昼夜、楼层切换只改 class、属性、材质或 visible，不要清空后整页重画；高亮时其余元素降低不透明度而不是消失。",
            "- 三维页面推荐骨架（按需扩展，变量名可改）：",
            "  const renderer = new THREE.WebGLRenderer({ canvas: document.getElementById('scene'), antialias: true }); renderer.shadowMap.enabled = true; renderer.toneMapping = THREE.ACESFilmicToneMapping;",
            "  const scene = new THREE.Scene(); const camera = new THREE.PerspectiveCamera(42, 1, 0.5, 3000);",
            "  const controls = new THREE.OrbitControls(camera, renderer.domElement); controls.enableDamping = true; controls.maxPolarAngle = Math.PI * 0.47;",
            "  const labels = new THREE.CSS2DRenderer({ element: document.getElementById('labels') }); // #labels{position:absolute;inset:0;pointer-events:none}，可点的标签单独设 pointer-events:auto",
            "  const composer = new THREE.EffectComposer(renderer); composer.addPass(new THREE.RenderPass(scene, camera)); const bloom = new THREE.UnrealBloomPass(new THREE.Vector2(512, 512), 0.2, 0.5, 0.9); composer.addPass(bloom); composer.addPass(new THREE.OutputPass());",
            "  scene.environment = new THREE.PMREMGenerator(renderer).fromScene(new THREE.RoomEnvironment(), 0.04).texture; scene.environmentIntensity = 0.35; VLS.three.autoResize(renderer, camera, document.getElementById('app'), composer, labels);",
            "  // 曝光：白天关闭辉光（bloom.enabled = false），夜景 bloom.strength≈0.75、threshold≈0.6 并把 environmentIntensity 降到 0.05，只让窗户、路灯、点位等自发光部分发光，避免画面整体发白；阴影用 THREE.PCFShadowMap。",
            "  // 把园区放进 const park = new THREE.Group(); 搭好后：const home = VLS.three.fit(camera, controls, park, { direction: [1, 0.8, 1.1] });",
            "  VLS.three.onPick(renderer, camera, () => pickables, object => { /* 点位 → VLS.preview(id)，楼栋 → VLS.three.flyTo(...) */ });",
            "  (function loop() { controls.update(); composer.render(); labels.render(scene, camera); requestAnimationFrame(loop); })();",
            "- 篇幅：完整页面控制在约 4000–9000 tokens，用循环、共享材质和几何生成重复元素，确保输出完整闭合。",
            "",
            "## 连续修改",
            "- 输入会给出「当前页面」源码和「历次要求」。必须保留用户没有要求修改的一切：已有建筑与布局、镜头操作、昼夜切换、按钮与事件、摄像头绑定（data-camera 与 VLS.preview 调用）、文字与配色。",
            "- 新增的按钮或操作必须真正生效：同时提供元素、样式、事件处理和状态更新，并与已有功能共存（例如切换楼层后，昼夜切换和点击摄像头预览仍然有效）。",
            "- 修复运行错误时，根据错误信息定位原因，只改相关代码，不要删减功能。");
    }

    /**
     * The user turn. Raw page source is easier for models to copy exactly than JSON-escaped text, which
     * matters for SEARCH/REPLACE edits.
     */
    static String input(String mode, String description, Map<String, String> cameras, List<String> history,
                        String currentPage, JsonNode legacyScene, String retryNote) {
        StringBuilder text = new StringBuilder();
        text.append("## 视图\n").append("bim".equals(mode) ? "BIM 视图" : "拓扑视图").append('\n');
        text.append("\n## 可用摄像头（运行时通过 VLS.cameras 读取，下面仅供命名参考）\n");
        if (cameras.isEmpty()) {
            text.append("当前目录没有可绑定的已授权摄像头。页面中的镜头全部作为示意点位，并在运行时读取 VLS.cameras 以便将来自动绑定。\n");
        } else {
            for (Map.Entry<String, String> camera : cameras.entrySet()) {
                text.append("- ").append(camera.getKey()).append("：").append(camera.getValue()).append('\n');
            }
        }
        if (!history.isEmpty()) {
            text.append("\n## 历次要求（由旧到新，均已体现在当前页面中，需继续保留）\n");
            for (int i = 0; i < history.size(); i++) text.append(i + 1).append(". ").append(history.get(i)).append('\n');
        }
        if (currentPage != null) {
            text.append("\n## 当前页面（补丁的 SEARCH 必须逐字复制自这里）\n<current_page>\n")
                .append(currentPage).append("\n</current_page>\n");
        } else if (legacyScene != null) {
            text.append("\n## 旧版结构化场景（请改写为完整页面，保留其中的布局、名称与 camera 别名绑定）\n")
                .append(legacyScene.toString()).append('\n');
        }
        text.append("\n## 本次要求\n").append(description).append('\n');
        if (retryNote != null) {
            text.append("\n## 注意\n").append(retryNote).append('\n');
        } else if (currentPage != null) {
            text.append("\n请在当前页面基础上实现本次要求：局部修改输出 SEARCH/REPLACE 补丁，整体重做输出完整页面。\n");
        }
        return text.toString();
    }
}
