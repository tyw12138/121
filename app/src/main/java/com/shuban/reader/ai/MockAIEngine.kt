package com.shuban.reader.ai

data class Character(
    val name: String,
    val role: String,
    val relation: String
)

class MockAIEngine {

    fun chat(text: String, question: String): String {
        // 模拟 AI 对话响应
        return when {
            question.contains("主角", ignoreCase = true) -> {
                "根据文本分析，主角是一位性格坚韧、富有正义感的年轻人。在面对困难时，他总是选择迎难而上，这种品质让他赢得了同伴的信任和支持。"
            }
            question.contains("剧情", ignoreCase = true) -> {
                "这段剧情展现了故事的核心冲突。作者通过细腻的描写，将人物内心的挣扎和成长展现得淋漓尽致。建议你继续阅读后续章节，会有更多精彩的情节展开。"
            }
            question.contains("关系", ignoreCase = true) -> {
                "从文本中可以看出，人物之间的关系错综复杂。主要角色之间既有合作也有矛盾，这种张力推动了故事的发展。"
            }
            else -> {
                "我理解你的问题。基于你提供的文本，这是一个很有深度的情节。作者通过细节描写，展现了人物的内心世界和故事的主题。你想深入了解哪个方面呢？"
            }
        }
    }

    fun summarize(text: String): String {
        // 模拟 AI 剧情摘要
        return """
            【剧情摘要】
            
            本章主要讲述了主角在关键时刻做出的重要决定。通过一系列事件的发展，故事展现了以下核心内容：
            
            1. 冲突升级：主角面临前所未有的挑战，需要在多个选项中做出抉择。
            
            2. 人物成长：通过这次经历，主角的内心变得更加坚定，对未来的道路有了更清晰的认识。
            
            3. 关系变化：主角与周围人物的关系发生了微妙的变化，一些新的联盟正在形成。
            
            4. 伏笔设置：作者在本章埋下了几个重要的伏笔，暗示了后续剧情的发展方向。
            
            【关键事件】
            • 主角做出了改变命运的决定
            • 新角色登场，带来重要信息
            • 旧矛盾得到缓解，但新问题随之而来
            
            【阅读建议】
            建议关注主角的内心独白，这有助于理解故事的主题和人物动机。
        """.trimIndent()
    }

    fun extractCharacters(text: String): List<Character> {
        // 模拟 AI 提取人物关系
        return listOf(
            Character(
                name = "李明",
                role = "主角",
                relation = "故事的核心人物，性格坚韧，富有正义感"
            ),
            Character(
                name = "王芳",
                role = "女主角",
                relation = "主角的挚友，后发展为恋人关系"
            ),
            Character(
                name = "张伟",
                role = "反派",
                relation = "主角的竞争对手，后成为盟友"
            ),
            Character(
                name = "陈老师",
                role = "导师",
                relation = "主角的人生导师，提供关键指导"
            ),
            Character(
                name = "刘强",
                role = "配角",
                relation = "主角的好友，忠诚可靠"
            )
        )
    }
}