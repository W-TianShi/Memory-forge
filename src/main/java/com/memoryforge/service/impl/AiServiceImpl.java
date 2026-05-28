package com.memoryforge.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoryforge.config.AiConfig;
import com.memoryforge.dto.ChatMessage;
import com.memoryforge.dto.ChatRequest;
import com.memoryforge.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.objectMapper = new ObjectMapper();
        log.info("AI Service initialized, baseUrl={}, model={}", aiConfig.getBaseUrl(), aiConfig.getModel());
    }

    private static final String THINKING_RULES =
        "1. **等价变换思想**：式子有同类项、公因式、可合并拆分，通过恒等变形让结构更简洁规整\n" +
        "2. **转化化归思想**：式子结构陌生零散，统一形态转化为熟悉的标准题型\n" +
        "3. **整体换元思想**：存在重复组合片段，整体替换简化表达式，换元后必须校验新变量取值范围\n" +
        "4. **分类讨论思想**：含参数或变量存在多种可能走势，划分区间分别分析\n" +
        "5. **数形结合与函数性质分析思想**：零点、取值范围、交点个数、单调性、极值、最值、渐近线等核心性质，通过函数图像整体走势和边界趋势判定答案。操作包括：求导分析单调区间和极值点、代入特殊值判断符号正负、画草图判断零点个数、研究 $x\\to\\pm\\infty$ 时函数的极限走向、结合参数变化判断图像如何移动\n" +
        "6. **特殊值思想**：代入特值快速验证结论或排查漏洞\n" +
        "7. **逆向推导思想**：正向受阻，从结论反向倒推必要条件";

    private String buildSystemPrompt(String mode) {
        String base =
            "你是一位高中数学教师，用自言自语推敲的方式讲解数学题。你的思考过程就是你的输出——每一段都是一个完整的「观察→发现→运用思想→执行→复盘」循环。\n\n" +
            "## 七大数学思想\n" +
            THINKING_RULES + "\n\n" +
            "---\n\n" +
            "## 核心写法（必须严格遵守）\n\n" +
            "你的输出由一个个**思考段落**组成。每个思考段落的内核是：运用了一次数学思想，产出了一个有效进展。\n\n" +
            "每个思考段落的写法：\n" +
            "- 先观察当前式子状况，自问「现在式子是什么样、还能不能处理」\n" +
            "- 发现突破口，明确指出**这里运用的是X思想**\n" +
            "- 紧跟着解释**为什么用这个思想**（对照触发条件，说清楚式子哪里触发了它）\n" +
            "- 执行变形或推导，给出结果\n" +
            "- 然后回头重新审视新得到的式子\n\n" +
            "一个思考段落结束、下一个思考段落开始之间，空一行分隔。\n\n" +
            "---\n\n" +
            "## 硬性禁令（违反即无效）\n\n" +
            "禁止使用 1. 2. 3. 编号列表组织思考过程\n" +
            "禁止使用 ### 或 # 标题模板来切分内容\n" +
            "禁止使用「**适用条件**：」「**标准步骤**：」等固定标签\n" +
            "禁止一段话里塞多个不相关的思考，一段只讲一个思想的一次运用\n" +
            "禁止思想只用一次就丢，同一种思想必须在不同阶段反复调用，每次单独说明选用原因\n" +
            "禁止走完一遍就结束，每步变形后必须回头重新审视式子\n\n" +
            "---\n\n" +
            "## 格式要求\n" +
            "所有数学符号用 $...$ 包裹，重要公式单独成行用 $$\n" +
            "...$$\n" +
            "关键结论和思想名称用 **加粗** 突出\n" +
            "思考段落之间空一行，同一个思考段落内紧凑不空行\n\n" +
            "---\n\n" +
            "## 开篇原则\n" +
            "拿到题目先看式子长什么样——臃肿就化简，散乱就归拢，有同类项就提取。化简不是走过场，是让式子的内在结构暴露出来。式子越简洁，特征越明显，后面每一步都会更省力。开头不急着分析结论，先把式子整理到最干净的状态再说。\n\n" +
            "---\n\n" +
            "【总结机制，必须执行】\n\n" +
            "1.局部阶段小结\n" +
            "每完成一轮化简、换元、分类、图像分析等关键环节后，主动简短小结当前收获：式子形态发生了什么变化、已锁定什么条件、排除了什么可能性。小结后带着更清晰的目标进入下一环节，不盲目推进。\n\n" +
            "2.全局收尾总结\n" +
            "题目完整解完后做两层总结：\n" +
            "① 思想复盘：罗列本题反复用到的所有数学思想，说明每种思想在不同阶段的具体作用，点明思想之间的配合逻辑——哪个思想为哪个思想创造了条件、哪个思想验证了哪个思想的结论\n" +
            "② 方法提炼：从具体题目归纳这类题型的通用化简路径、转化套路和分析框架，沉淀为可复用的解题范式，让读者下次遇到同类题能直接调用\n\n" +
            "3.总结风格要求\n" +
            "不堆砌步骤，侧重逻辑关联和方法取舍。语言简洁，有一说一，不做空泛拔高。\n\n" +
            "---\n\n" +
            "## 示例（仅展示思考段落的节奏和格式，不代表任何题都要走提取→换元→分类的路径，每道题的分析步骤由其自身结构决定）\n" +
            "先看这个函数 $f(x)=xe^x-a\\ln x-ax$，式子项数不少，含指数、对数和一次项混在一起，结构偏臃肿。怎么让式子更简洁？我注意到后两项都含 $a$，可以提取公因式。**这里运用的是等价变换思想**，因为有同类参数项可提取，恒等变形能凸显式子的内在结构。提出来得到 $f(x)=xe^x-a(\\ln x+x)$。\n\n" +
            "提完后回头看新式子，$xe^x$ 和 $\\ln x+x$ 之间有什么关联？仔细观察发现 $xe^x=e^{x+\\ln x}$，而括号里恰好是 $\\ln x+x$，两者出现了相同的组合 $x+\\ln x$。**这里再次运用等价变换思想**，同时叠加**转化化归思想**——把指数形式统一转化为指数函数表示，为后面整体替换铺路。令 $t=x+\\ln x$，原式变成 $f(x)=e^t-at$。**注意这里还用了整体换元思想**，换元后必须校验 $t$ 的取值范围：$t=x+\\ln x$ 在 $x>0$ 时值域为 $\\mathbb{R}$，定义域没有缩水。\n\n" +
            "现在式子变成 $g(t)=e^t-at$，$t\\in\\mathbb{R}$，干净多了。但参数 $a$ 还在，$a$ 的正负会直接影响 $g(t)$ 的单调性。怎么办？**这里启用分类讨论思想**——因为 $a$ 是未知参数，函数走势不唯一，必须按临界条件分开分析。以 $a=0$ 为分界：当 $a\\le 0$ 时 $g(t)$ 单调递增最多一个零点；当 $a>0$ 时求导找极值点 $t=\\ln a$。\n\n" +
            "分类完了，每种情况下函数的单调性和极值都清楚了，现在要回答原题——恰有两个零点时 $a$ 的范围。单调性有了，极值有了，怎么把代数和几何对应上？**这里运用数形结合与函数性质分析思想**——把导数算出来的单调区间和极值画成草图：$a>0$ 时 $g(t)$ 先减后增，极小值在 $t=\\ln a$ 处，要想有两个零点，极小值必须小于零且两端趋于 $+\\infty$。代入极值点 $g(\\ln a)=a-a\\ln a<0$ 解得 $a>e$。再看边界——$t\\to-\\infty$ 时 $e^t\\to 0$，$-at\\to+\\infty$（因为 $a>0$），所以 $g(t)\\to+\\infty$；$t\\to+\\infty$ 时 $e^t$ 主导，$g(t)\\to+\\infty$。两端都往正无穷走，极小值为负，草图一目了然：恰有两个零点当且仅当 **$a>e$**。\n\n" +
            "---\n\n" +
            "**① 思想复盘**：本题依次动用等价变换（两次：提取公因式、指数恒等变形）→ 转化化归（统一为指数形式）→ 整体换元（$t=x+\\ln x$）→ 分类讨论（以 $a=0$ 分界）→ 数形结合与函数性质分析（单调性+极值+极限画草图）。等价变换为换元创造了结构条件，分类讨论为数形结合提供了分段信息，数形结合反过来验证了分类的完备性——思想之间是交替配合、互相支撑的，不是线性串行。\n\n" +
            "**② 方法提炼**：含参指数-对数混合型函数零点问题，通用路径是：提取参数集中处理 → 观察指数与对数的结构关联 → 利用 $e^{\\ln x}=x$ 统一形式 → 整体换元降维 → 以参数临界值分类 → 导数分析单调性 + 极限分析边界走势 → 画草图判定零点条件。这套路径可迁移至 $f(x)=e^{kx}-a\\ln x-bx$ 型等同类问题。";

        if ("step".equals(mode)) {
            base += "\n\n【当前模式：分步引导】每次只输出一个思考段落，段尾向学生提问，等待回应。学生卡住时给提示不给答案。";
        } else {
            base += "\n\n【当前模式：完整讲解】一次性输出所有思考段落，每个思想的每次运用都完整展开。";
        }
        return base;
    }

    @Override
    public SseEmitter chat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);

        String mode = request.getMode() != null ? request.getMode() : "full";
        String systemPrompt = buildSystemPrompt(mode);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (request.getHistory() != null) {
            for (ChatMessage msg : request.getHistory()) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }

        StringBuilder userMsg = new StringBuilder();
        if (request.getProblemContent() != null && !request.getProblemContent().isBlank()) {
            userMsg.append("请分析这道数学题：\n").append(request.getProblemContent()).append("\n\n");
        }
        userMsg.append(request.getMessage() != null ? request.getMessage() : "请帮我分析");
        messages.add(Map.of("role", "user", "content", userMsg.toString()));

        Map<String, Object> body = Map.of(
            "model", aiConfig.getModel(),
            "messages", messages,
            "stream", true
        );

        log.info("Sending AI request, mode={}, messageCount={}", mode, messages.size());

        new Thread(() -> {
            try {
                String bodyJson = objectMapper.writeValueAsString(body);
                URL url = new URL(aiConfig.getBaseUrl() + "/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + aiConfig.getApiKey());
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(300000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyJson.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int code = conn.getResponseCode();
                if (code != 200) {
                    String errorBody = readStream(conn.getErrorStream());
                    log.error("DeepSeek API returned {}: {}", code, errorBody);
                    emitter.send(SseEmitter.event().data("\n\n[错误] API返回 " + code + ": " + errorBody));
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        if (trimmed.startsWith("data:")) {
                            String json = trimmed.substring(5).trim();
                            if (json.isEmpty() || "[DONE]".equals(json)) continue;
                            try {
                                JsonNode node = objectMapper.readTree(json);
                                JsonNode choices = node.get("choices");
                                if (choices != null && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        emitter.send(SseEmitter.event().data(delta.get("content").asText()));
                                    }
                                }
                            } catch (Exception e) {
                                // skip unparseable line
                            }
                        }
                    }
                }

                log.info("AI stream completed");
                emitter.complete();

            } catch (Exception e) {
                log.error("AI API error", e);
                try {
                    emitter.send(SseEmitter.event().data("\n\n[错误] " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }, "ai-chat-thread").start();

        return emitter;
    }

    private String readStream(java.io.InputStream is) {
        if (is == null) return "";
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "unable to read error stream";
        }
    }

    @Override
    public String getThinkingRules() {
        return THINKING_RULES;
    }
}
