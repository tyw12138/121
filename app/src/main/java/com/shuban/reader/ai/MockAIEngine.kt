package com.shuban.reader.ai

data class Character(
    val name: String,
    val role: String,
    val relation: String,
    val personality: String = "",
    val avatar: String = ""
)

data class TimelineEvent(
    val chapter: String,
    val event: String,
    val emotion: String
)

data class BookRecommendation(
    val title: String,
    val author: String,
    val genre: String,
    val reason: String,
    val matchScore: Int
)

data class ReadingNote(
    val id: Long,
    val content: String,
    val timestamp: Long,
    val type: String
)

class MockAIEngine {

    private val characterDialogues = mapOf(
        "李明" to listOf(
            "我从不后悔做出的选择，即使前路再难，也要走下去。你呢？你会怎么选？",
            "有时候我也在想，如果当初走了另一条路，现在会是什么样？但人生没有如果。",
            "王芳她……一直是支撑我走下去的力量。只是我之前没有意识到。",
            "张伟那个人，虽然我们立场不同，但我能感受到他内心的挣扎。",
            "陈老师说得对，真正的成长不是变得更强，而是学会接受自己的软弱。"
        ),
        "王芳" to listOf(
            "李明总是冲在最前面，但我更担心的是他从不让别人看到他的脆弱。",
            "我选择站在他身边，不是因为义务，而是因为我相信他走的路。",
            "有时候安静地陪伴，比千言万语更有力量。",
            "张伟……他并不是坏人，只是被环境逼到了那个位置。",
            "每个人都有自己的战场，我的战场就是守护身边的人。"
        ),
        "张伟" to listOf(
            "别以为我是坏人，这世上哪有绝对的对错？只是立场不同罢了。",
            "我和李明，其实是一枚硬币的两面。如果换一个起点，也许我们就是朋友。",
            "权力……不过是用来填补内心空洞的工具罢了。",
            "你以为我不想放下吗？但有些东西一旦开始，就回不了头了。",
            "王芳她看人的眼光很准，她能看到别人看不到的东西。"
        ),
        "陈老师" to listOf(
            "年轻人，记住，真正的智慧不是知道所有答案，而是懂得提出正确的问题。",
            "李明这孩子，骨子里有一种不服输的劲，这既是他的优点，也是他的弱点。",
            "人生就像下棋，有时候需要退一步，才能看到全局。",
            "教育不是灌满一桶水，而是点燃一把火。",
            "你们这一代人面临的挑战比我们那时更大，但你们的潜力也更大。"
        ),
        "刘强" to listOf(
            "兄弟，有什么事尽管说，我刘强最讲义气！",
            "李明是我最好的兄弟，他做什么我都支持。",
            "别看我大大咧咧的，关键时刻我从不掉链子。",
            "这世道，能交到一个真心朋友比什么都重要。",
            "有时候我也想安静下来想想，但行动总比犹豫强！"
        )
    )

    fun chat(text: String, question: String): String {
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
            question.contains("伏笔", ignoreCase = true) -> {
                "文本中隐藏了几处重要的伏笔：\n1. 主角反复出现的梦境暗示了他过去的创伤\n2. 神秘老人的话与后续剧情有重要关联\n3. 某个配角不经意间提到的地名，实际上是关键线索"
            }
            question.contains("主题", ignoreCase = true) -> {
                "这段文本探讨了几个深刻的主题：\n• 成长与选择——每个角色都在面对人生的十字路口\n• 信任与背叛——关系中的张力推动着故事发展\n• 理想与现实——角色们在两者之间寻找平衡"
            }
            else -> {
                "我理解你的问题。基于你提供的文本，这是一个很有深度的情节。作者通过细节描写，展现了人物的内心世界和故事的主题。你可以尝试问我关于主角、剧情、人物关系、伏笔或主题的问题，我会给你更详细的分析。"
            }
        }
    }

    fun chatWithCharacter(characterName: String, message: String): String {
        val dialogues = characterDialogues[characterName] ?: return "这个角色暂时无法对话，请选择其他角色。"
        val index = Math.abs(message.hashCode()) % dialogues.size
        return dialogues[index]
    }

    fun getAvailableCharacters(): List<String> {
        return characterDialogues.keys.toList()
    }

    fun summarize(text: String): String {
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

    fun analyzeEmotion(text: String): String {
        return """
            【情感分析】
            
            📊 整体情感倾向：紧张→希望→坚定
            
            🔴 紧张段落（占比 40%）
            主角面临重大抉择，内心充满矛盾与不安。作者通过环境描写和内心独白，营造出压抑的氛围。
            
            🟡 转折段落（占比 30%）
            关键人物的出现打破了僵局，带来新的可能性。情感从低谷开始回升。
            
            🟢 希望段落（占比 30%）
            主角做出决定后，内心变得坚定。与同伴的互动带来温暖，暗示着光明的未来。
            
            💡 情感关键词：挣扎、抉择、信任、成长、坚定
        """.trimIndent()
    }

    fun extractTimeline(text: String): List<TimelineEvent> {
        return listOf(
            TimelineEvent("第一章", "主角在小镇长大，与好友刘强形影不离", "平静"),
            TimelineEvent("第二章", "神秘事件发生，主角的平静生活被打破", "紧张"),
            TimelineEvent("第三章", "主角决定踏上旅途，王芳决定同行", "坚定"),
            TimelineEvent("第四章", "途中遭遇张伟的阻拦，双方发生冲突", "激烈"),
            TimelineEvent("第五章", "陈老师出现，揭示关键信息", "震撼"),
            TimelineEvent("第六章", "主角做出重大决定，关系重新洗牌", "转折")
        )
    }

    fun extractCharacters(text: String): List<Character> {
        return listOf(
            Character(
                name = "李明",
                role = "主角",
                relation = "故事的核心人物，性格坚韧，富有正义感",
                personality = "坚韧、正义、有担当，但有时过于固执",
                avatar = "🧑"
            ),
            Character(
                name = "王芳",
                role = "女主角",
                relation = "主角的挚友，后发展为恋人关系",
                personality = "温柔、聪慧、善解人意，关键时刻非常果断",
                avatar = "👩"
            ),
            Character(
                name = "张伟",
                role = "反派",
                relation = "主角的竞争对手，后成为盟友",
                personality = "野心勃勃、城府深，但内心有柔软的一面",
                avatar = "🧔"
            ),
            Character(
                name = "陈老师",
                role = "导师",
                relation = "主角的人生导师，提供关键指导",
                personality = "睿智、平和、洞察力强，说话充满哲理",
                avatar = "👨‍🏫"
            ),
            Character(
                name = "刘强",
                role = "配角",
                relation = "主角的好友，忠诚可靠",
                personality = "直爽、义气、行动派，有时冲动但心地善良",
                avatar = "💪"
            )
        )
    }

    fun getCharacterRelations(): List<String> {
        return listOf(
            "李明 ↔ 王芳：挚友→恋人，相互信任与支持",
            "李明 ↔ 张伟：对手→盟友，从对立到理解",
            "李明 ↔ 陈老师：师徒，亦师亦友",
            "李明 ↔ 刘强：挚友，生死之交",
            "王芳 ↔ 张伟：复杂情感，理解与警惕并存",
            "陈老师 ↔ 张伟：旧识，有过往恩怨"
        )
    }

    fun recommend(text: String): List<BookRecommendation> {
        return listOf(
            BookRecommendation(
                title = "长安的荔枝",
                author = "马伯庸",
                genre = "历史小说",
                reason = "如果你喜欢人物在困境中成长的故事，这本以小人物视角展开的历史小说会让你感同身受",
                matchScore = 95
            ),
            BookRecommendation(
                title = "三体",
                author = "刘慈欣",
                genre = "科幻小说",
                reason = "复杂的人物关系和宏大的世界观构建，与你当前阅读的叙事风格有相似之处",
                matchScore = 88
            ),
            BookRecommendation(
                title = "活着",
                author = "余华",
                genre = "现实主义",
                reason = "关于坚韧与成长的主题，与主角的性格特质高度契合",
                matchScore = 85
            ),
            BookRecommendation(
                title = "庆余年",
                author = "猫腻",
                genre = "网络小说",
                reason = "主角同样面临抉择与成长，人物关系错综复杂，剧情反转精彩",
                matchScore = 82
            )
        )
    }

    fun generateNote(text: String): String {
        return """
            【阅读笔记】
            
            📖 本章要点
            • 主角面临重大抉择，最终选择迎难而上
            • 新角色登场带来关键信息，推动剧情发展
            • 人物关系出现微妙变化，新的联盟正在形成
            
            💭 个人感悟
            这段情节让我想到：真正的勇气不是没有恐惧，而是在恐惧中依然前行。主角的选择虽然艰难，但正是这种选择定义了他是谁。
            
            🔖 标记段落
            "有时候，最难的路，才是对的路。" —— 本章金句
            
            ❓ 待解答疑问
            1. 神秘老人的真实身份是什么？
            2. 主角梦境中反复出现的场景有何含义？
            3. 张伟的最终立场会如何变化？
        """.trimIndent()
    }
}
