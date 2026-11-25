package com.example.domain.rpg

import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.TaskDocument
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import kotlin.math.roundToInt

data class RpgOperationResult(
    val state: RpgWorkspaceState,
    val bannerNote: String = "",
    val unlockedAchievementIds: List<String> = emptyList(),
    val completedQuestIds: List<String> = emptyList(),
    val leveledUpTo: Int? = null
)

object RpgProgressionEngine {

    val skillCatalog: List<SkillDefinition> = listOf(
        // =========================================================================
        // 1. ФИЗИЧЕСКИЙ ТРУД И ПОЛЕВОЙ МОНТАЖ (УЭСАиТМ / ОГЭ / ОГМ / ОЭН)
        // =========================================================================
        SkillDefinition(
            id = "sk_phys_montage",
            branch = SkillBranch.PROFESSIONAL,
            titleRu = "Монтаж без мата и кувалды (Шкафы САиТМ)",
            descriptionRu = "+2% к XP за физический труд: сборка шкафов автоматики, прокладка кабельных лотков и установка датчиков КИПиА",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null,
            serviceTitlesRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "Монтаж без мата и кувалды (Шкафы САиТМ)",
                ServiceSpecialization.OGE to "Укротитель силовых муфт 10 кВ (ОГЭ)",
                ServiceSpecialization.OGM to "Повелитель фланцев и шпилек М36 (ОГМ)",
                ServiceSpecialization.OEN to "Богатырь линейной задвижки (ОЭН)"
            ),
            serviceDescriptionsRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "+2% к XP за физический монтаж шкафов САиТМ, импульсных линий и датчиков давления",
                ServiceSpecialization.OGE to "+2% к XP за разделку бронированного силового кабеля и протяжку шин в РУ-6/10 кВ",
                ServiceSpecialization.OGM to "+2% к XP за физический демонтаж/монтаж задвижек, насосов МНА и замену торцевых уплотнений",
                ServiceSpecialization.OEN to "+2% к XP за шурфовку, ремонт изоляции трубы и работу на камерах пуска-приёма СОД"
            )
        ),
        SkillDefinition(
            id = "sk_phys_cable",
            branch = SkillBranch.PROFESSIONAL,
            titleRu = "Кабельный ниндзя (Прозвонка с закрытыми глазами)",
            descriptionRu = "+3% к XP за скоростную расшивку клеммников КИПиА и обжим НШВИ без единого КЗ",
            maxLevel = 5,
            xpBonusPercentPerLevel = 3,
            prerequisiteSkillId = "sk_phys_montage",
            serviceTitlesRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "Кабельный ниндзя (Прозвонка с закрытыми глазами)",
                ServiceSpecialization.OGE to "Гроза кабельных эстакад и фидеров (ОГЭ)",
                ServiceSpecialization.OGM to "Виртуоз подшипников и сальников (ОГМ)",
                ServiceSpecialization.OEN to "Мастер запасовки скребка КПП СОД (ОЭН)"
            ),
            serviceDescriptionsRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "+3% к XP за быструю расшивку многожильных контрольных кабелей в кроссовых шкафах АСУТП",
                ServiceSpecialization.OGE to "+3% к XP за безупречную прокладку силовых линий и подключение ячеек КРУН",
                ServiceSpecialization.OGM to "+3% к XP за переборку редукторов и маслосистем без «лишних деталей» после сборки",
                ServiceSpecialization.OEN to "+3% к XP за подготовку камер пуска-приёма СОД и опрессовку технологических трубопроводов"
            )
        ),
        SkillDefinition(
            id = "sk_phys_novice_grind",
            branch = SkillBranch.PROFESSIONAL,
            titleRu = "Подай-принеси и мастер болгарки 150 мм (Старт)",
            descriptionRu = "Юмор начального этапа (+2% к XP): таскание бухт КВВГ, резка листов металла, засыпка еврокубов песком и поиск «ведра компрессии»",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null,
            isHumorousOrPenaltySkill = true,
            serviceTitlesRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "Подай-принеси и мастер болгарки 150 мм (Старт САиТМ)",
                ServiceSpecialization.OGE to "Носитель диэлектрических бот и заземлений (Старт ОГЭ)",
                ServiceSpecialization.OGM to "Младший помощник кувалды и ключа на 46 (Старт ОГМ)",
                ServiceSpecialization.OEN to "Обходчик-марафонец с лопатой (Старт ОЭН)"
            )
        ),

        // =========================================================================
        // 2. УМСТВЕННЫЙ ТРУД: ЭЛЕКТРИКА, ЭЛЕКТРОНИКА И АСУТП
        // =========================================================================
        SkillDefinition(
            id = "sk_mind_plc",
            branch = SkillBranch.QUALITY,
            titleRu = "Заклинатель ПЛК, SCADA и Modbus",
            descriptionRu = "+2% к XP за умственный труд: диагностика контроллеров АСУТП, СДКУ, радиосвязи МПВ и пусконаладка защит",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null,
            serviceTitlesRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "Заклинатель ПЛК, SCADA и Modbus (УЭСАиТМ)",
                ServiceSpecialization.OGE to "Шаман микропроцессорной РЗА и АВР (ОГЭ)",
                ServiceSpecialization.OGM to "Аналитик вибродиагностики и термографии МНА (ОГМ)",
                ServiceSpecialization.OEN to "Повелитель гидравлических режимов МН (ОЭН)"
            ),
            serviceDescriptionsRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "+2% к XP за наладку ПЛК, опрос радиостанций, ССВД и маскирование защит МНА на холостом ходу",
                ServiceSpecialization.OGE to "+2% к XP за расчёт уставок релейной защиты, настройку АВР и анализ осциллограмм",
                ServiceSpecialization.OGM to "+2% к XP за спектральный анализ вибрации насосных агрегатов и диагностику торцевых уплотнений",
                ServiceSpecialization.OEN to "+2% к XP за контроль профиля давлений, расхода и герметичности магистрального нефтепровода"
            )
        ),
        SkillDefinition(
            id = "sk_mind_logic",
            branch = SkillBranch.QUALITY,
            titleRu = "Осциллограф головного мозга (Петля 4–20 мА / HART)",
            descriptionRu = "+3% к XP за поиск плавающей наводки в цепях Метранов, пожарных извещателей и датчиков уровня КНС",
            maxLevel = 5,
            xpBonusPercentPerLevel = 3,
            prerequisiteSkillId = "sk_mind_plc",
            serviceTitlesRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "Осциллограф головного мозга (Петля 4–20 мА / HART)",
                ServiceSpecialization.OGE to "Телепат гармоник, фазировки и cos φ (ОГЭ)",
                ServiceSpecialization.OGM to "Магистр лазерной центровки валов (ОГМ)",
                ServiceSpecialization.OEN to "Дешифратор внутритрубной дефектоскопии ВИП (ОЭН)"
            ),
            serviceDescriptionsRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "+3% к XP за устранение ошибок ССВД МНС, калибровку датчиков Метран и настройку токовых петель",
                ServiceSpecialization.OGE to "+3% к XP за поиск однофазных замыканий на землю и испытания изоляции повышенным напряжением",
                ServiceSpecialization.OGM to "+3% к XP за выверку соосности валов насоса и электродвигателя с точностью до 0,02 мм",
                ServiceSpecialization.OEN to "+3% к XP за точную идентификацию дефектов стенки трубы по данным дефектоскопа"
            )
        ),

        // =========================================================================
        // 3. ДОКУМЕНТАЦИЯ, СХЕМЫ И ИНЖЕНЕРНАЯ БЮРОКРАТИЯ
        // =========================================================================
        SkillDefinition(
            id = "sk_doc_bureau",
            branch = SkillBranch.TEAM,
            titleRu = "Властелин ГОСТов, ПУЭ и кабельных журналов",
            descriptionRu = "+2% к XP за работу с документацией: журналы распоряжений, сводные таблицы радиостанций, планы на месяц и письма",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null,
            serviceTitlesRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "Властелин ГОСТов, ПУЭ и кабельных журналов (САиТМ)",
                ServiceSpecialization.OGE to "Архивариус однолинейных схем и бланков переключений (ОГЭ)",
                ServiceSpecialization.OGM to "Летописец формуляров МНА и дефектных ведомостей (ОГМ)",
                ServiceSpecialization.OEN to "Хранитель паспортов РВС и градуировочных таблиц (ОЭН)"
            )
        ),
        SkillDefinition(
            id = "sk_doc_act",
            branch = SkillBranch.TEAM,
            titleRu = "Печать одобрения (Акты поверки и наряды с 1-го раза)",
            descriptionRu = "+3% к XP за подготовку графиков поверки Метранов, актов приёмки АСУТП и исполнительных схем без единого возврата",
            maxLevel = 5,
            xpBonusPercentPerLevel = 3,
            prerequisiteSkillId = "sk_doc_bureau",
            serviceTitlesRu = mapOf(
                ServiceSpecialization.ASUTP_UESAITM to "Печать одобрения (Акты поверки и схемы АСУТП с 1-го раза)",
                ServiceSpecialization.OGE to "Мастер наряда-допуска в электроустановках (ОГЭ)",
                ServiceSpecialization.OGM to "Гуру актов гидроиспытаний и ревизии арматуры (ОГМ)",
                ServiceSpecialization.OEN to "Маэстро актов пуска-приёма СОД и техкарт (ОЭН)"
            )
        ),

        // =========================================================================
        // 4. ТБ, ВЫЖИВАНИЕ И ИРОНИЧНЫЕ НАВЫКИ (ЗА КОСЯКИ, ВОЗВРАТЫ И НАЧАЛЬНЫЙ ЭТАП)
        // =========================================================================
        SkillDefinition(
            id = "sk_safe_pue",
            branch = SkillBranch.SAFETY,
            titleRu = "Выжил после 380В (Инстинкт самосохранения)",
            descriptionRu = "+2% к XP за соблюдение ПУЭ и проверку отсутствия напряжения перед тем, как лезть отвёрткой в шкаф",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null
        ),
        SkillDefinition(
            id = "sk_safe_humor",
            branch = SkillBranch.SAFETY,
            titleRu = "Магистр синей изоленты и стяжек",
            descriptionRu = "Тонкий инженерный юмор (+3% к XP): если держится на синей изоленте — значит по ГОСТу, но за клеммы WAGO и бирки дают бонус",
            maxLevel = 5,
            xpBonusPercentPerLevel = 3,
            prerequisiteSkillId = "sk_safe_pue",
            isHumorousOrPenaltySkill = true
        ),
        SkillDefinition(
            id = "sk_irony_excuse",
            branch = SkillBranch.SAFETY,
            titleRu = "Гуру объяснительных: «Оно само сработало!»",
            descriptionRu = "Ирония за возвраты отчётов (+1% утешительного XP): автоматически качается при отклонении отчёта или замечаниях («Это наводка в линии, а не мой косяк!»)",
            maxLevel = 5,
            xpBonusPercentPerLevel = 1,
            prerequisiteSkillId = null,
            isHumorousOrPenaltySkill = true
        ),
        SkillDefinition(
            id = "sk_irony_blame",
            branch = SkillBranch.SAFETY,
            titleRu = "Стрелочник 80-го уровня («Это смежники и магнитные бури!»)",
            descriptionRu = "Не очень тонкий юмор за переносы задач и просрочки (+1% XP): автоматически качается при переносе невыполненных объёмов на завтра",
            maxLevel = 5,
            xpBonusPercentPerLevel = 1,
            prerequisiteSkillId = null,
            isHumorousOrPenaltySkill = true
        ),

        // =========================================================================
        // 5. СПЕЦСЛУЖБЫ (УЭСАиТМ / ОГЭ / ОГМ / ОЭН) И НАДЗОР ИТР
        // =========================================================================
        SkillDefinition(
            id = "sk_spec_asutp",
            branch = SkillBranch.LEADERSHIP,
            titleRu = "Спецкурс УЭСАиТМ: Телемеханика, ССВД и МПВ",
            descriptionRu = "+2% к XP за профильные задачи участка средств автоматики и телемеханики (АСУТП, пожарная автоматика, связь)",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null
        ),
        SkillDefinition(
            id = "sk_spec_oge",
            branch = SkillBranch.LEADERSHIP,
            titleRu = "Спецкурс ОГЭ: Энергия трансформаторов и РУ",
            descriptionRu = "+2% к XP за задачи отдела главного энергетика (электроснабжение, АВР, ЗРУ, освещение и кабельные сети)",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null
        ),
        SkillDefinition(
            id = "sk_spec_ogm",
            branch = SkillBranch.LEADERSHIP,
            titleRu = "Спецкурс ОГМ: Механика МНА и запорной арматуры",
            descriptionRu = "+2% к XP за задачи отдела главного механика (насосы, приводы задвижек, КНС, откачка и механообработка)",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null
        ),
        SkillDefinition(
            id = "sk_spec_oen",
            branch = SkillBranch.LEADERSHIP,
            titleRu = "Спецкурс ОЭН: Эксплуатация нефтепроводов и РВС",
            descriptionRu = "+2% к XP за задачи отдела эксплуатации нефтепроводов (линейная часть, технологические трубопроводы, еврокубы и откачка)",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null
        ),
        SkillDefinition(
            id = "sk_lead_review",
            branch = SkillBranch.LEADERSHIP,
            titleRu = "Око Технадзора (Оперативная приёмка ИТР)",
            descriptionRu = "+2% к XP за быструю проверку фотоотчётов подчинённых и жёсткий отсев халтуры",
            maxLevel = 5,
            xpBonusPercentPerLevel = 2,
            prerequisiteSkillId = null,
            minRoleWeight = CorporateRole.ITR.hierarchyWeight
        ),
        SkillDefinition(
            id = "sk_lead_mentor",
            branch = SkillBranch.LEADERSHIP,
            titleRu = "Дедлайн-терапевт смены (Разгон спящих бригад)",
            descriptionRu = "+3% к XP за координацию участка, проведение планёрок и закрытие сменных заданий без переносов",
            maxLevel = 5,
            xpBonusPercentPerLevel = 3,
            prerequisiteSkillId = "sk_lead_review",
            minRoleWeight = CorporateRole.ITR.hierarchyWeight
        )
    )

    val achievementCatalog: List<Achievement> = listOf(
        Achievement(
            id = "ach_first_approved",
            titleRu = "Первый наряд сдан",
            descriptionRu = "Получить первое подтверждение выполненной задачи",
            rarity = AchievementRarity.COMMON,
            xpReward = 15,
            badgeItemId = "item_badge_starter"
        ),
        Achievement(
            id = "ach_streak_5",
            titleRu = "Стабильная смена (5 дней)",
            descriptionRu = "Поддерживать серию выполнения 5 дней подряд",
            rarity = AchievementRarity.COMMON,
            xpReward = 25,
            badgeItemId = "item_badge_streak5"
        ),
        Achievement(
            id = "ach_streak_10",
            titleRu = "Дисциплина 10/10",
            descriptionRu = "Серия выполнения 10 дней подряд без срывов",
            rarity = AchievementRarity.RARE,
            xpReward = 50,
            badgeItemId = "item_frame_cobalt"
        ),
        Achievement(
            id = "ach_photo_master",
            titleRu = "Эталонный фотоконтроль",
            descriptionRu = "Сдать 5 задач с обязательным фотоотчётом без замечаний",
            rarity = AchievementRarity.RARE,
            xpReward = 45,
            badgeItemId = "item_badge_lens"
        ),
        Achievement(
            id = "ach_emergency_hero",
            titleRu = "Аварийный резерв",
            descriptionRu = "Закрыть задачу с приоритетом «Аварийный» или «Критический»",
            rarity = AchievementRarity.EPIC,
            xpReward = 65,
            badgeItemId = "item_title_guardian"
        ),
        Achievement(
            id = "ach_reputation_90",
            titleRu = "Безупречная репутация",
            descriptionRu = "Достичь индекса репутации 90 и выше",
            rarity = AchievementRarity.EPIC,
            xpReward = 60,
            badgeItemId = "item_frame_gold"
        ),
        Achievement(
            id = "ach_streak_20",
            titleRu = "Железная смена (20 дней)",
            descriptionRu = "Серия 20 дней подряд на максимальном множителе ×1.5",
            rarity = AchievementRarity.LEGENDARY,
            xpReward = 120,
            badgeItemId = "item_title_legend"
        ),
        Achievement(
            id = "ach_prestige_1",
            titleRu = "Ветеран Престижа",
            descriptionRu = "Достичь максимального уровня и активировать Престиж",
            rarity = AchievementRarity.LEGENDARY,
            xpReward = 150,
            badgeItemId = "item_badge_prestige"
        )
    )

    val questCatalog: List<Quest> = listOf(
        Quest(
            id = "q_daily_2_tasks",
            titleRu = "Ударная смена",
            descriptionRu = "Завершить и подтвердить 2 задачи за текущий день",
            cadence = QuestCadence.DAILY,
            metricType = QuestMetricType.COMPLETE_TASKS,
            targetCount = 2,
            xpReward = 20,
            reputationReward = 1
        ),
        Quest(
            id = "q_daily_photo",
            titleRu = "Чистая фотофиксация",
            descriptionRu = "Сдать 1 задачу с подтверждённым фотоотчётом",
            cadence = QuestCadence.DAILY,
            metricType = QuestMetricType.PHOTO_VERIFIED_TASKS,
            targetCount = 1,
            xpReward = 15,
            reputationReward = 1
        ),
        Quest(
            id = "q_weekly_5_first_try",
            titleRu = "Без возвратов",
            descriptionRu = "Сдать 5 задач с 1-го предъявления за неделю",
            cadence = QuestCadence.WEEKLY,
            metricType = QuestMetricType.FIRST_TRY_APPROVALS,
            targetCount = 5,
            xpReward = 60,
            reputationReward = 3,
            rewardItemId = "item_badge_streak5"
        ),
        Quest(
            id = "q_weekly_high_prio",
            titleRu = "Приоритетный фронт",
            descriptionRu = "Выполнить 3 задачи с приоритетом Высокий/Критический/Аварийный",
            cadence = QuestCadence.WEEKLY,
            metricType = QuestMetricType.HIGH_OR_ABOVE_TASKS,
            targetCount = 3,
            xpReward = 70,
            reputationReward = 3
        ),
        Quest(
            id = "q_seasonal_25",
            titleRu = "Опора объекта (Сезон)",
            descriptionRu = "Выполнить и подтвердить 25 задач в течение квартального сезона",
            cadence = QuestCadence.SEASONAL,
            metricType = QuestMetricType.COMPLETE_TASKS,
            targetCount = 25,
            xpReward = 180,
            reputationReward = 5,
            rewardItemId = "item_frame_gold"
        ),
        Quest(
            id = "q_story_itr_review",
            titleRu = "Точный контроль ИТР",
            descriptionRu = "Провести приёмку 10 отчётов подчинённых без просрочек",
            cadence = QuestCadence.STORY,
            metricType = QuestMetricType.SUPERVISOR_REVIEWS,
            targetCount = 10,
            xpReward = 90,
            reputationReward = 4,
            minRoleWeight = CorporateRole.ITR.hierarchyWeight
        )
    )

    val inventoryCatalog: List<InventoryItem> = listOf(
        InventoryItem(
            id = "item_badge_starter",
            nameRu = "Знак допуска",
            descriptionRu = "Выдаётся за первый подтверждённый отчёт",
            category = InventoryItemCategory.BADGE,
            rarity = AchievementRarity.COMMON,
            accentHexLong = 0xFF10B981
        ),
        InventoryItem(
            id = "item_badge_streak5",
            nameRu = "Искры смены ×5",
            descriptionRu = "За серию 5 дней без срывов",
            category = InventoryItemCategory.BADGE,
            rarity = AchievementRarity.COMMON,
            accentHexLong = 0xFFF59E0B
        ),
        InventoryItem(
            id = "item_badge_lens",
            nameRu = "Объектив ОТК",
            descriptionRu = "За эталонную фотофиксацию работ",
            category = InventoryItemCategory.BADGE,
            rarity = AchievementRarity.RARE,
            accentHexLong = 0xFF0284C7
        ),
        InventoryItem(
            id = "item_frame_cobalt",
            nameRu = "Кобальтовый контур",
            descriptionRu = "Рамка профиля за 10-дневную серию",
            category = InventoryItemCategory.AVATAR_FRAME,
            rarity = AchievementRarity.RARE,
            accentHexLong = 0xFF2563EB
        ),
        InventoryItem(
            id = "item_frame_gold",
            nameRu = "Золотой стандарт",
            descriptionRu = "Рамка профиля за безупречную репутацию (90+)",
            category = InventoryItemCategory.AVATAR_FRAME,
            rarity = AchievementRarity.EPIC,
            accentHexLong = 0xFFD97706
        ),
        InventoryItem(
            id = "item_title_guardian",
            nameRu = "Ликвидатор ЧС",
            descriptionRu = "Почётный титул за выполнение аварийных задач",
            category = InventoryItemCategory.TITLE,
            rarity = AchievementRarity.EPIC,
            accentHexLong = 0xFFDC2626
        ),
        InventoryItem(
            id = "item_title_legend",
            nameRu = "Легенда объекта",
            descriptionRu = "Легендарный титул за серию 20 дней подряд",
            category = InventoryItemCategory.TITLE,
            rarity = AchievementRarity.LEGENDARY,
            accentHexLong = 0xFF7C3AED
        ),
        InventoryItem(
            id = "item_badge_prestige",
            nameRu = "Звезда Престижа",
            descriptionRu = "Отличительный знак ветерана Престижа",
            category = InventoryItemCategory.BADGE,
            rarity = AchievementRarity.LEGENDARY,
            accentHexLong = 0xFFD97706
        )
    )

    val statusCatalog: List<StatusDefinition> = listOf(
        // Worker statuses
        StatusDefinition(1, CorporateRole.WORKER.hierarchyWeight, 1, "Стажёр", "Базовый допуск к нарядам"),
        StatusDefinition(2, CorporateRole.WORKER.hierarchyWeight, 3, "Рабочий", "+2% доверия при самоконтроле"),
        StatusDefinition(3, CorporateRole.WORKER.hierarchyWeight, 7, "Мастер", "Приоритет в сложных нарядах"),
        StatusDefinition(4, CorporateRole.WORKER.hierarchyWeight, 12, "Бригадир", "Старший в звене"),
        // ITR statuses
        StatusDefinition(1, CorporateRole.ITR.hierarchyWeight, 1, "Инженер", "Контроль смен и бригад"),
        StatusDefinition(2, CorporateRole.ITR.hierarchyWeight, 5, "Старший ИТР", "Расширенная координация участка"),
        StatusDefinition(3, CorporateRole.ITR.hierarchyWeight, 10, "Ведущий инженер", "Технический арбитр участка"),
        StatusDefinition(4, CorporateRole.ITR.hierarchyWeight, 15, "Главный специалист", "Высший инженерный статус"),
        // HEAD / ADMIN statuses
        StatusDefinition(1, CorporateRole.HEAD.hierarchyWeight, 1, "Руководитель", "Управление участком и приёмкой"),
        StatusDefinition(2, CorporateRole.HEAD.hierarchyWeight, 6, "Начальник участка", "Полная координация потоков"),
        StatusDefinition(3, CorporateRole.HEAD.hierarchyWeight, 12, "Руководитель проекта", "Межучастковая стратегия"),
        StatusDefinition(4, CorporateRole.HEAD.hierarchyWeight, 17, "Директор", "Высший управленческий ранг")
    )

    fun resolveUserLevel(totalXp: Int, config: GamificationConfig): UserLevel {
        val safeXp = totalXp.coerceAtLeast(0)
        var level = 1
        while (level < config.maxLevel && safeXp >= config.xpRequiredForLevel(level + 1)) {
            level++
        }
        val floorXp = config.xpRequiredForLevel(level)
        val isMax = level >= config.maxLevel
        val nextXp = if (isMax) floorXp else config.xpRequiredForLevel(level + 1)
        val progress = if (isMax || nextXp <= floorXp) {
            1f
        } else {
            ((safeXp - floorXp).toFloat() / (nextXp - floorXp).toFloat()).coerceIn(0f, 1f)
        }
        return UserLevel(
            level = level,
            totalXp = safeXp,
            currentLevelFloorXp = floorXp,
            nextLevelXp = nextXp,
            progressFraction = progress,
            isMaxLevel = isMax
        )
    }

    fun resolveUserStatus(role: CorporateRole, level: Int): UserStatus {
        val bucketWeight = when {
            role.isAdminOrHead -> CorporateRole.HEAD.hierarchyWeight
            role == CorporateRole.ITR -> CorporateRole.ITR.hierarchyWeight
            else -> CorporateRole.WORKER.hierarchyWeight
        }
        val match = statusCatalog
            .filter { it.minRoleWeight == bucketWeight && level >= it.minLevel }
            .maxByOrNull { it.minLevel }
            ?: statusCatalog.first { it.minRoleWeight == bucketWeight }
        return UserStatus(
            titleRu = match.titleRu,
            perkRu = match.perkRu,
            rankIndex = match.rankIndex
        )
    }

    fun streakMultiplierFor(streakDays: Int, config: GamificationConfig): Double = when {
        streakDays >= config.streakTier3Days -> config.streakTier3Multiplier
        streakDays >= config.streakTier2Days -> config.streakTier2Multiplier
        streakDays >= config.streakTier1Days -> config.streakTier1Multiplier
        else -> 1.0
    }

    fun resolveEmployeeServiceSpecialization(
        employee: CorporateEmployee? = null,
        task: TaskDocument? = null
    ): ServiceSpecialization {
        val combined = listOfNotNull(
            employee?.siteName,
            employee?.groupName,
            employee?.position,
            task?.geoLocation,
            task?.groupName,
            task?.title
        ).joinToString(" ").lowercase()

        return when {
            combined.contains("огэ") ||
                combined.contains("главного энергетика") ||
                combined.contains("энерго") ||
                combined.contains("зру") ||
                combined.contains("трансформатор") -> ServiceSpecialization.OGE
            combined.contains("огм") ||
                combined.contains("главного механика") ||
                combined.contains("механик") && !combined.contains("электромеханик") && !combined.contains("телемехан") -> ServiceSpecialization.OGM
            combined.contains("оэн") ||
                combined.contains("эксплуатации нефтепроводов") ||
                combined.contains("трубопровод") ||
                combined.contains("кпп сод") -> ServiceSpecialization.OEN
            else -> ServiceSpecialization.ASUTP_UESAITM
        }
    }

    /**
     * Определяет характер работ по содержанию назначенной задачи:
     * физический труд, умственный труд в электрике/электронике/АСУТП, документация, ТБ/ирония и спецслужба.
     */
    fun classifyTaskWorkNatures(task: TaskDocument): List<TaskWorkNature> {
        val text = buildString {
            append(task.title).append(' ')
            append(task.description).append(' ')
            append(task.geoLocation).append(' ')
            task.visibleTags.forEach { append(it).append(' ') }
            task.decodedSubtasks().forEach { append(it.title).append(' ') }
        }.lowercase()

        val natures = mutableSetOf<TaskWorkNature>()

        val isPhysical = listOf(
            "монтаж", "демонтаж", "отпилить", "распил", "болгарк", "перевести", "перенести",
            "отнести", "установить", "еврокуб", "песк", "песком", "лист", "металл", "шкаф",
            "крышк", "трактор", "кабел", "задвиж", "кнс", "откачк", "фгу", "проссвет", "мешк"
        ).any { text.contains(it) }
        if (isPhysical) {
            natures.add(TaskWorkNature.PHYSICAL_LABOR)
        }

        val isMentalAutomationOrElectrical = listOf(
            "асутп", "саитм", "уэсаитм", "кипиа", "датчик", "метран", "извещател", "ссвд",
            "мнс", "плк", "scada", "сигнализац", "пожарн", "радиостанц", "радиосвяз", "мпв",
            "автоматик", "телемехан", "схем", "питани", "ошибк", "устранить", "пуск", "мна",
            "холостом", "маскиров", "защит", "контроллер", "авр", "рза", "ток", "напряж"
        ).any { text.contains(it) }
        if (isMentalAutomationOrElectrical) {
            natures.add(TaskWorkNature.MENTAL_AUTOMATION_ELECTRICAL)
        }

        val isDocumentation = listOf(
            "журнал", "распечатать", "план", "таблиц", "переч", "письм", "совещани", "планерк",
            "планёрк", "акт", "поверк", "отчет", "отчёт", "паспорт", "наряд", "сводн", "график",
            "документ", "ведомост", "протокол", "согласова"
        ).any { text.contains(it) }
        if (isDocumentation) {
            natures.add(TaskWorkNature.DOCUMENTATION_AND_SCHEMES)
        }

        val isSafety = listOf(
            "защит", "пожар", "тб", "пуэ", "безопас", "исправн", "провер", "блокиров", "допуск"
        ).any { text.contains(it) }
        if (isSafety) {
            natures.add(TaskWorkNature.SAFETY_AND_IRONY)
        }

        natures.add(TaskWorkNature.SERVICE_SPECIALIZATION)
        if (natures.size == 1) {
            // Если по тексту не сработал узкий фильтр, для УЭСАиТМ по умолчанию распределяем в АСУТП/Электронику
            natures.add(TaskWorkNature.MENTAL_AUTOMATION_ELECTRICAL)
        }
        return natures.toList()
    }

    /**
     * Подбирает конкретные навыки дерева для автоматической прокачки от выполнения (или провала) задачи.
     */
    fun classifyTaskAutoSkillIds(
        task: TaskDocument,
        employee: CorporateEmployee,
        profile: EmployeeRpgProfile,
        isPoorExecution: Boolean = false,
        currentLevel: Int = 1
    ): List<String> {
        val currentSkillMap = profile.skills.associate { it.skillId to it.level }
        val resultIds = mutableListOf<String>()

        if (isPoorExecution) {
            // За плохое выполнение (возврат, просрочка, перенос) автоматически качаются ироничные навыки!
            resultIds.add("sk_irony_excuse")
            if (task.carryoverCount > 0L || task.taskStatus == TaskStatus.OVERDUE) {
                resultIds.add("sk_irony_blame")
            }
            if (currentLevel <= 4) {
                resultIds.add("sk_phys_novice_grind")
            }
            return resultIds.distinct()
        }

        val natures = classifyTaskWorkNatures(task)
        val service = resolveEmployeeServiceSpecialization(employee, task)

        if (TaskWorkNature.PHYSICAL_LABOR in natures) {
            if (currentLevel <= 3 && (currentSkillMap["sk_phys_novice_grind"] ?: 0) < 2) {
                resultIds.add("sk_phys_novice_grind")
            }
            val montageLvl = currentSkillMap["sk_phys_montage"] ?: 0
            if (montageLvl < 1) {
                resultIds.add("sk_phys_montage")
            } else {
                resultIds.add(if (montageLvl <= (currentSkillMap["sk_phys_cable"] ?: 0)) "sk_phys_montage" else "sk_phys_cable")
            }
        }

        if (TaskWorkNature.MENTAL_AUTOMATION_ELECTRICAL in natures) {
            val plcLvl = currentSkillMap["sk_mind_plc"] ?: 0
            if (plcLvl < 1) {
                resultIds.add("sk_mind_plc")
            } else {
                resultIds.add(if (plcLvl <= (currentSkillMap["sk_mind_logic"] ?: 0)) "sk_mind_plc" else "sk_mind_logic")
            }
        }

        if (TaskWorkNature.DOCUMENTATION_AND_SCHEMES in natures) {
            val docLvl = currentSkillMap["sk_doc_bureau"] ?: 0
            if (docLvl < 1) {
                resultIds.add("sk_doc_bureau")
            } else {
                resultIds.add(if (docLvl <= (currentSkillMap["sk_doc_act"] ?: 0)) "sk_doc_bureau" else "sk_doc_act")
            }
        }

        if (TaskWorkNature.SAFETY_AND_IRONY in natures) {
            val pueLvl = currentSkillMap["sk_safe_pue"] ?: 0
            if (pueLvl < 1) {
                resultIds.add("sk_safe_pue")
            } else {
                resultIds.add(if (pueLvl <= (currentSkillMap["sk_safe_humor"] ?: 0)) "sk_safe_pue" else "sk_safe_humor")
            }
        }

        // Автоматическая прокачка профильного навыка службы (УЭСАиТМ / ОГЭ / ОГМ / ОЭН)
        val specSkillId = when (service) {
            ServiceSpecialization.ASUTP_UESAITM -> "sk_spec_asutp"
            ServiceSpecialization.OGE -> "sk_spec_oge"
            ServiceSpecialization.OGM -> "sk_spec_ogm"
            ServiceSpecialization.OEN -> "sk_spec_oen"
        }
        resultIds.add(specSkillId)

        if (employee.role.hierarchyWeight >= CorporateRole.ITR.hierarchyWeight) {
            val revLvl = currentSkillMap["sk_lead_review"] ?: 0
            resultIds.add(if (revLvl < 1) "sk_lead_review" else "sk_lead_mentor")
        }

        return resultIds.distinct()
    }

    private fun autoAdvanceProfileSkills(
        profile: EmployeeRpgProfile,
        employee: CorporateEmployee,
        skillIdsToAdvance: List<String>,
        maxAutoGainCount: Int = 2
    ): Pair<EmployeeRpgProfile, List<String>> {
        val skillMap = profile.skills.associate { it.skillId to it.level }.toMutableMap()
        val advancedTitles = mutableListOf<String>()
        val service = resolveEmployeeServiceSpecialization(employee)

        for (skillId in skillIdsToAdvance) {
            if (advancedTitles.size >= maxAutoGainCount) break
            val def = skillCatalog.firstOrNull { it.id == skillId } ?: continue
            if (employee.role.hierarchyWeight < def.minRoleWeight) continue
            if (def.prerequisiteSkillId != null && (skillMap[def.prerequisiteSkillId] ?: 0) < 1) continue
            val curLvl = skillMap[skillId] ?: 0
            if (curLvl < def.maxLevel) {
                val nextLvl = curLvl + 1
                skillMap[skillId] = nextLvl
                advancedTitles.add("${def.titleForService(service)} (ур. $nextLvl)")
            }
        }

        if (advancedTitles.isEmpty()) return profile to emptyList()
        val updatedSkills = skillMap.entries.map { (k, v) -> UserSkill(skillId = k, level = v) }
        return profile.copy(skills = updatedSkills) to advancedTitles
    }

    fun skillMultiplierForTask(
        profile: EmployeeRpgProfile,
        priority: TaskPriority,
        hasPhoto: Boolean,
        firstTry: Boolean,
        isMultiAssignee: Boolean
    ): Double {
        var bonusPct = 0
        profile.skills.forEach { us ->
            if (us.level <= 0) return@forEach
            val def = skillCatalog.firstOrNull { it.id == us.skillId } ?: return@forEach
            val applies = when (def.id) {
                "sk_phys_montage", "sk_phys_cable", "sk_phys_novice_grind",
                "sk_mind_plc", "sk_mind_logic" -> true
                "sk_doc_bureau", "sk_doc_act" -> firstTry
                "sk_safe_pue", "sk_safe_humor", "sk_irony_excuse", "sk_irony_blame" -> true
                "sk_spec_asutp", "sk_spec_oge", "sk_spec_ogm", "sk_spec_oen",
                "sk_lead_review", "sk_lead_mentor" -> true
                else -> false
            }
            if (applies) {
                bonusPct += def.xpBonusPercentPerLevel * us.level
            }
        }
        return 1.0 + (bonusPct.coerceAtMost(40) / 100.0)
    }

    /**
     * Расчёт ожидаемого XP по формуле раздела 13.3:
     * XP_task = max(0, round(base(p) * (1 + bonuses - penalties) * streakMult * skillMult * prestigeMult))
     */
    fun calculateTaskXpBreakdown(
        priority: TaskPriority,
        config: GamificationConfig,
        profile: EmployeeRpgProfile,
        isOnTime: Boolean = true,
        isEarly: Boolean = false,
        hasCleanPhoto: Boolean = false,
        isOverdue: Boolean = false,
        wasRejectedBefore: Boolean = false,
        isMultiAssignee: Boolean = false,
        includeApprovalBonus: Boolean = true
    ): Pair<Int, String> {
        val rule = config.ruleFor(priority)
        val rawBase = rule.baseXp + if (includeApprovalBonus) rule.approvalBonusXp else 0

        var bonusRate = 0.0
        val tags = mutableListOf<String>()
        tags.add("База(${priority.titleRu})=$rawBase")

        if (!isOverdue && isEarly) {
            bonusRate += config.earlyBonusRate
            tags.add("Раньше срока +${(config.earlyBonusRate * 100).roundToInt()}%")
        } else if (!isOverdue && isOnTime) {
            bonusRate += config.onTimeBonusRate
            tags.add("В срок +${(config.onTimeBonusRate * 100).roundToInt()}%")
        }
        if (hasCleanPhoto) {
            bonusRate += config.cleanPhotoBonusRate
            tags.add("Фото +${(config.cleanPhotoBonusRate * 100).roundToInt()}%")
        }

        var penaltyRate = 0.0
        if (isOverdue) {
            penaltyRate += config.overduePenaltyRate
            tags.add("Просрочка -${(config.overduePenaltyRate * 100).roundToInt()}%")
        }
        if (wasRejectedBefore) {
            penaltyRate += config.rejectedPenaltyRate
            tags.add("Возврат -${(config.rejectedPenaltyRate * 100).roundToInt()}%")
        }

        val modifierFactor = (1.0 + bonusRate - penaltyRate).coerceAtLeast(0.0)
        val streakMult = streakMultiplierFor(profile.streak.currentDays, config)
        if (streakMult > 1.0) tags.add("Серия ×$streakMult")

        val skillMult = skillMultiplierForTask(
            profile = profile,
            priority = priority,
            hasPhoto = hasCleanPhoto,
            firstTry = !wasRejectedBefore,
            isMultiAssignee = isMultiAssignee
        )
        if (skillMult > 1.0) tags.add("Навыки ×${String.format(java.util.Locale.US, "%.2f", skillMult)}")

        val prestigeMult = 1.0 + profile.prestige.permanentXpMultiplierBonus
        if (prestigeMult > 1.0) tags.add("Престиж ×${String.format(java.util.Locale.US, "%.2f", prestigeMult)}")

        val totalXp = (rawBase * modifierFactor * streakMult * skillMult * prestigeMult)
            .roundToInt()
            .coerceAtLeast(0)
        return totalXp to tags.joinToString(" • ")
    }

    fun estimateExpectedTaskXp(
        task: TaskDocument,
        state: RpgWorkspaceState,
        employeeId: String,
        currentEpochDay: Long
    ): Int {
        val profile = state.profileForId(employeeId)
        val effectivePriority = task.lockedPriorityAtCompletion ?: task.taskPriority
        val isOverdue = task.taskStatus == TaskStatus.OVERDUE || currentEpochDay > task.dueDateEpochDay || task.carryoverCount > 0L
        val isEarly = currentEpochDay < task.dueDateEpochDay && task.carryoverCount == 0L
        val hasPhoto = task.photoItems.isNotEmpty() || task.requiresPhotoReport
        return calculateTaskXpBreakdown(
            priority = effectivePriority,
            config = state.config,
            profile = profile,
            isOnTime = !isOverdue,
            isEarly = isEarly,
            hasCleanPhoto = hasPhoto,
            isOverdue = isOverdue,
            wasRejectedBefore = !task.rejectionReason.isNullOrBlank(),
            isMultiAssignee = task.allAssigneeIds.size > 1,
            includeApprovalBonus = true
        ).first
    }

    fun ensureProfile(state: RpgWorkspaceState, employeeId: String, email: String = ""): RpgWorkspaceState {
        if (employeeId.isBlank()) return state
        val existing = state.profileForId(employeeId, email).copy(employeeId = employeeId)
        if (state.profiles[employeeId] == existing) return state
        return state.copy(
            profiles = state.profiles + (employeeId to existing)
        )
    }

    fun canViewProgression(viewer: CorporateEmployee, target: CorporateEmployee): Boolean {
        if (viewer.id == target.id) return true
        if (viewer.role.isAdminOrHead) return true
        if (viewer.role == CorporateRole.ITR) {
            return target.role == CorporateRole.WORKER &&
                (target.groupId == viewer.groupId || target.groupName.equals(viewer.groupName, ignoreCase = true))
        }
        return false
    }

    fun canAdjustProgression(actor: CorporateEmployee): Boolean = actor.role.isAdminOrHead

    /**
     * Начисление XP за завершение/подтверждение задачи с защитой идемпотентности (taskId + type)
     * и фиксацией приоритета на момент выполнения.
     */
    fun recordTaskCompletionOrApproval(
        state: RpgWorkspaceState,
        task: TaskDocument,
        assignees: List<CorporateEmployee>,
        supervisor: CorporateEmployee?,
        currentEpochDay: Long,
        isApprovalStep: Boolean
    ): RpgOperationResult {
        var working = state
        val effectivePriority = task.lockedPriorityAtCompletion ?: task.taskPriority
        val txType = if (isApprovalStep) XpTransactionType.SUPERVISOR_APPROVAL else XpTransactionType.TASK_COMPLETION
        val nowMillis = System.currentTimeMillis()
        val isOverdue = task.carryoverCount > 0L || currentEpochDay > task.dueDateEpochDay
        val isEarly = !isOverdue && currentEpochDay < task.dueDateEpochDay
        val hasPhoto = task.photoItems.isNotEmpty()
        val wasRejected = !task.rejectionReason.isNullOrBlank()
        val firstTry = !wasRejected && task.carryoverCount == 0L
        val isMulti = assignees.size > 1

        val allUnlocked = mutableListOf<String>()
        val allQuests = mutableListOf<String>()
        var latestLevelUp: Int? = null
        val notes = mutableListOf<String>()

        assignees.forEach { emp ->
            working = ensureProfile(working, emp.id)
            val idempKey = "${task.id}_${txType.code}_${emp.id}"
            val alreadyProcessed = working.transactions.any {
                it.idempotencyKey == idempKey && !it.isRolledBack
            }
            if (!alreadyProcessed) {
                val profile = working.profiles.getValue(emp.id)
                val oldLevel = resolveUserLevel(profile.totalXp, working.config).level

                val (xpEarned, multSummary) = calculateTaskXpBreakdown(
                    priority = effectivePriority,
                    config = working.config,
                    profile = profile,
                    isOnTime = !isOverdue,
                    isEarly = isEarly,
                    hasCleanPhoto = hasPhoto,
                    isOverdue = isOverdue,
                    wasRejectedBefore = wasRejected,
                    isMultiAssignee = isMulti,
                    includeApprovalBonus = isApprovalStep || !task.requiresApproval
                )

                val repDelta = when {
                    wasRejected -> 1
                    isOverdue -> 0
                    firstTry && (effectivePriority == TaskPriority.CRITICAL || effectivePriority == TaskPriority.EMERGENCY) -> 3
                    firstTry -> 2
                    else -> 1
                }

                val updatedStreak = advanceStreak(profile.streak, currentEpochDay)
                val autoSkillTargets = classifyTaskAutoSkillIds(
                    task = task,
                    employee = emp,
                    profile = profile,
                    isPoorExecution = isOverdue || wasRejected,
                    currentLevel = oldLevel
                )
                val (profileWithAutoSkills, autoAdvancedLabels) = autoAdvanceProfileSkills(
                    profile = profile,
                    employee = emp,
                    skillIdsToAdvance = autoSkillTargets,
                    maxAutoGainCount = 2
                )
                val workNaturesSummary = classifyTaskWorkNatures(task)
                    .filter { it != TaskWorkNature.SERVICE_SPECIALIZATION }
                    .joinToString(", ") { it.titleRu.substringBefore(":") }
                val autoSkillNote = if (autoAdvancedLabels.isNotEmpty()) {
                    " • Авто-прокачка (${workNaturesSummary.ifBlank { "АСУТП" }}): ${autoAdvancedLabels.joinToString(", ")}"
                } else {
                    " • Профиль задачи: ${workNaturesSummary.ifBlank { "АСУТП" }}"
                }

                val tx = XpTransaction(
                    id = "xp_${nowMillis}_${emp.id.take(6)}_${(100..999).random()}",
                    employeeId = emp.id,
                    employeeName = emp.shortName,
                    taskId = task.id,
                    taskTitle = task.title,
                    type = txType,
                    xpDelta = xpEarned,
                    reputationDelta = repDelta,
                    priorityAtCompletion = effectivePriority,
                    appliedMultipliersSummary = "$multSummary$autoSkillNote",
                    reason = "${txType.titleRu}: ${task.title}",
                    actorId = supervisor?.id ?: emp.id,
                    actorName = supervisor?.shortName ?: emp.shortName,
                    idempotencyKey = idempKey,
                    createdAtMillis = nowMillis
                )

                working = working.copy(profiles = working.profiles + (emp.id to profileWithAutoSkills))
                working = applyTransactionToProfile(working, emp.id, tx)
                val afterTxProfile = working.profiles.getValue(emp.id).copy(streak = updatedStreak)
                working = working.copy(profiles = working.profiles + (emp.id to afterTxProfile))

                // Update Quests (anti-fraud: track countedTaskIds per periodKey)
                val questResult = updateQuestsForTaskApproval(
                    state = working,
                    employee = emp,
                    task = task,
                    priority = effectivePriority,
                    hasPhoto = hasPhoto,
                    firstTry = firstTry,
                    currentEpochDay = currentEpochDay,
                    isSupervisorAction = false
                )
                working = questResult.first
                allQuests.addAll(questResult.second)

                // Check Achievements
                val achResult = evaluateAchievements(
                    state = working,
                    employee = emp,
                    priority = effectivePriority,
                    hasPhoto = hasPhoto,
                    firstTry = firstTry
                )
                working = achResult.first
                allUnlocked.addAll(achResult.second)

                val finalProfile = working.profiles.getValue(emp.id)
                val newLevel = resolveUserLevel(finalProfile.totalXp, working.config).level
                if (newLevel > oldLevel) {
                    val gainedSp = newLevel - oldLevel
                    val leveledProfile = finalProfile.copy(
                        skillPointsAvailable = finalProfile.skillPointsAvailable + gainedSp
                    )
                    working = working.copy(profiles = working.profiles + (emp.id to leveledProfile))
                    latestLevelUp = newLevel
                }
                val skillBannerPart = if (autoAdvancedLabels.isNotEmpty()) " [Навык: ${autoAdvancedLabels.first()}]" else ""
                notes.add("${emp.shortName}: +$xpEarned XP$skillBannerPart")
            }
        }

        // Supervisor review XP & quest when supervisor approves someone else's task
        if (isApprovalStep && supervisor != null && assignees.none { it.id == supervisor.id }) {
            working = ensureProfile(working, supervisor.id)
            val supIdempKey = "${task.id}_REVIEWER_APPROVAL_${supervisor.id}"
            if (working.transactions.none { it.idempotencyKey == supIdempKey && !it.isRolledBack }) {
                val supProfile = working.profiles.getValue(supervisor.id)
                val oldSupLevel = resolveUserLevel(supProfile.totalXp, working.config).level
                val reviewXp = (5 + effectivePriority.weight * 2 +
                    (supProfile.skills.firstOrNull { it.skillId == "sk_lead_review" }?.level ?: 0) * 2)
                val supTx = XpTransaction(
                    id = "xp_${nowMillis}_sup_${(100..999).random()}",
                    employeeId = supervisor.id,
                    employeeName = supervisor.shortName,
                    taskId = task.id,
                    taskTitle = task.title,
                    type = XpTransactionType.SUPERVISOR_APPROVAL,
                    xpDelta = reviewXp,
                    reputationDelta = 1,
                    priorityAtCompletion = effectivePriority,
                    appliedMultipliersSummary = "Приёмка работы ($effectivePriority)",
                    reason = "Контроль и подтверждение: ${task.title}",
                    actorId = supervisor.id,
                    actorName = supervisor.shortName,
                    idempotencyKey = supIdempKey,
                    createdAtMillis = nowMillis
                )
                working = applyTransactionToProfile(working, supervisor.id, supTx)
                val supQuestRes = updateQuestsForTaskApproval(
                    state = working,
                    employee = supervisor,
                    task = task,
                    priority = effectivePriority,
                    hasPhoto = hasPhoto,
                    firstTry = firstTry,
                    currentEpochDay = currentEpochDay,
                    isSupervisorAction = true
                )
                working = supQuestRes.first
                allQuests.addAll(supQuestRes.second)

                val supAfter = working.profiles.getValue(supervisor.id)
                val newSupLevel = resolveUserLevel(supAfter.totalXp, working.config).level
                if (newSupLevel > oldSupLevel) {
                    working = working.copy(
                        profiles = working.profiles + (supervisor.id to supAfter.copy(
                            skillPointsAvailable = supAfter.skillPointsAvailable + (newSupLevel - oldSupLevel)
                        ))
                    )
                }
            }
        }

        return RpgOperationResult(
            state = working,
            bannerNote = if (notes.isNotEmpty()) notes.joinToString(", ") else "",
            unlockedAchievementIds = allUnlocked.distinct(),
            completedQuestIds = allQuests.distinct(),
            leveledUpTo = latestLevelUp
        )
    }

    /**
     * Отклонение отчёта: откатываем незафиксированные награды за задачу (если были выданы) + штраф репутации и небольшой штраф XP.
     */
    fun recordTaskRejection(
        state: RpgWorkspaceState,
        task: TaskDocument,
        assignees: List<CorporateEmployee>,
        supervisor: CorporateEmployee,
        reason: String
    ): RpgOperationResult {
        var working = rollbackTaskRewardsInternal(
            state = state,
            taskId = task.id,
            rollbackType = XpTransactionType.ROLLBACK_REJECTION,
            actor = supervisor,
            reason = "Возврат на доработку: $reason"
        )
        val nowMillis = System.currentTimeMillis()
        assignees.forEach { emp ->
            working = ensureProfile(working, emp.id)
            val idempKey = "${task.id}_REJECT_PENALTY_${emp.id}_${task.revisionTimestamp}"
            if (working.transactions.none { it.idempotencyKey == idempKey }) {
                val curProfile = working.profiles.getValue(emp.id)
                val curLevel = resolveUserLevel(curProfile.totalXp, working.config).level
                val (profileWithIrony, ironyUpgraded) = autoAdvanceProfileSkills(
                    profile = curProfile,
                    employee = emp,
                    skillIdsToAdvance = listOf("sk_irony_excuse", if (curLevel <= 3) "sk_phys_novice_grind" else "sk_irony_blame"),
                    maxAutoGainCount = 1
                )
                working = working.copy(profiles = working.profiles + (emp.id to profileWithIrony))
                val ironyNote = if (ironyUpgraded.isNotEmpty()) " • Ироничный навык: ${ironyUpgraded.first()}" else ""
                val base = working.config.ruleFor(task.taskPriority).baseXp
                val xpPenalty = -(base * working.config.rejectedPenaltyRate).roundToInt().coerceAtLeast(2)
                val tx = XpTransaction(
                    id = "xp_${nowMillis}_rej_${(100..999).random()}",
                    employeeId = emp.id,
                    employeeName = emp.shortName,
                    taskId = task.id,
                    taskTitle = task.title,
                    type = XpTransactionType.REJECTION_PENALTY,
                    xpDelta = xpPenalty,
                    reputationDelta = -4,
                    priorityAtCompletion = task.taskPriority,
                    appliedMultipliersSummary = "Штраф за возврат$ironyNote",
                    reason = "Отклонено: $reason",
                    actorId = supervisor.id,
                    actorName = supervisor.shortName,
                    idempotencyKey = idempKey,
                    createdAtMillis = nowMillis
                )
                working = applyTransactionToProfile(working, emp.id, tx)
            }
        }
        return RpgOperationResult(state = working, bannerNote = "Репутация скорректирована (-4), прокачан ироничный навык за возврат")
    }

    /**
     * Полный откат начисленного XP и квестовых зачётов при отмене или удалении задачи (анти-абуз).
     */
    fun rollbackTaskOnCancellationOrDelete(
        state: RpgWorkspaceState,
        task: TaskDocument,
        actor: CorporateEmployee,
        reason: String
    ): RpgOperationResult {
        val next = rollbackTaskRewardsInternal(
            state = state,
            taskId = task.id,
            rollbackType = XpTransactionType.ROLLBACK_CANCELLATION,
            actor = actor,
            reason = reason
        )
        return RpgOperationResult(state = next, bannerNote = "Транзакции XP по задаче откачены")
    }

    private fun rollbackTaskRewardsInternal(
        state: RpgWorkspaceState,
        taskId: String,
        rollbackType: XpTransactionType,
        actor: CorporateEmployee,
        reason: String
    ): RpgWorkspaceState {
        var working = state
        val nowMillis = System.currentTimeMillis()
        val eligibleToRollback = working.transactions.filter {
            it.taskId == taskId &&
                !it.isRolledBack &&
                (it.type == XpTransactionType.TASK_COMPLETION || it.type == XpTransactionType.SUPERVISOR_APPROVAL)
        }
        eligibleToRollback.forEach { origTx ->
            val markedList = working.transactions.map {
                if (it.id == origTx.id) it.copy(isRolledBack = true) else it
            }
            working = working.copy(transactions = markedList)
            val rollbackTx = XpTransaction(
                id = "xp_${nowMillis}_rb_${(100..999).random()}",
                employeeId = origTx.employeeId,
                employeeName = origTx.employeeName,
                taskId = taskId,
                taskTitle = origTx.taskTitle,
                type = rollbackType,
                xpDelta = -origTx.xpDelta,
                reputationDelta = -origTx.reputationDelta,
                priorityAtCompletion = origTx.priorityAtCompletion,
                appliedMultipliersSummary = "Откат #${origTx.id.takeLast(6)}",
                reason = reason,
                actorId = actor.id,
                actorName = actor.shortName,
                idempotencyKey = "${origTx.id}_ROLLED_BACK",
                createdAtMillis = nowMillis
            )
            working = applyTransactionToProfile(working, origTx.employeeId, rollbackTx)
        }

        // Anti-fraud: remove taskId from unrewarded or tracked quest progress
        val updatedProfiles = working.profiles.mapValues { (_, profile) ->
            val cleanedQuests = profile.questProgress.map { qp ->
                if (taskId in qp.countedTaskIds && !qp.rewarded) {
                    val remaining = qp.countedTaskIds.filterNot { it == taskId }
                    qp.copy(
                        countedTaskIds = remaining,
                        currentCount = remaining.size
                    )
                } else {
                    qp
                }
            }
            profile.copy(questProgress = cleanedQuests)
        }
        return working.copy(profiles = updatedProfiles)
    }

    fun processCarryoverPenalties(
        state: RpgWorkspaceState,
        movedTasks: List<TaskDocument>,
        employees: List<CorporateEmployee>,
        currentEpochDay: Long
    ): RpgWorkspaceState {
        var working = state
        val affectedEmpIds = mutableSetOf<String>()
        movedTasks.forEach { t ->
            t.allAssigneeIds.forEach { if (it.isNotBlank()) affectedEmpIds.add(it) }
        }
        val nowMillis = System.currentTimeMillis()
        affectedEmpIds.forEach { empId ->
            val emp = employees.firstOrNull { it.id == empId }
            working = ensureProfile(working, empId)
            val idempKey = "CARRYOVER_RISK_${empId}_$currentEpochDay"
            if (working.transactions.none { it.idempotencyKey == idempKey }) {
                val prof = working.profiles.getValue(empId)
                val fallbackEmp = emp ?: CorporateEmployee(
                    id = empId,
                    fullName = empId,
                    shortName = empId,
                    position = "Инженер по КИПиА",
                    role = CorporateRole.WORKER,
                    groupId = "grp_1",
                    groupName = "ЛПДС \"Быково\"",
                    siteId = "site_1",
                    siteName = "УЭСАиТМ",
                    supervisorId = null,
                    phone = "",
                    email = ""
                )
                val (profWithBlameSkill, blameUpgraded) = autoAdvanceProfileSkills(
                    profile = prof,
                    employee = fallbackEmp,
                    skillIdsToAdvance = listOf("sk_irony_blame", "sk_irony_excuse"),
                    maxAutoGainCount = 1
                )
                working = working.copy(profiles = working.profiles + (empId to profWithBlameSkill))
                val ironySummary = if (blameUpgraded.isNotEmpty()) " • Ирония: ${blameUpgraded.first()}" else ""
                val riskedStreak = if (profWithBlameSkill.streak.currentDays > 0) profWithBlameSkill.streak.copy(isAtRisk = true) else profWithBlameSkill.streak
                val tx = XpTransaction(
                    id = "xp_${nowMillis}_ovd_${(100..999).random()}",
                    employeeId = empId,
                    employeeName = emp?.shortName ?: empId,
                    type = XpTransactionType.OVERDUE_PENALTY,
                    xpDelta = 0,
                    reputationDelta = -2,
                    appliedMultipliersSummary = "Перенос невыполненных задач$ironySummary",
                    reason = "Автоперенос незакрытых задач на следующий день",
                    idempotencyKey = idempKey,
                    createdAtMillis = nowMillis
                )
                working = applyTransactionToProfile(working, empId, tx)
                val afterProf = working.profiles.getValue(empId).copy(streak = riskedStreak)
                working = working.copy(profiles = working.profiles + (empId to afterProf))
            }
        }
        return working
    }

    fun upgradeSkill(
        state: RpgWorkspaceState,
        employee: CorporateEmployee,
        skillId: String
    ): Result<RpgWorkspaceState> {
        val def = skillCatalog.firstOrNull { it.id == skillId }
            ?: return Result.failure(IllegalArgumentException("Навык не найден"))
        if (employee.role.hierarchyWeight < def.minRoleWeight) {
            return Result.failure(IllegalStateException("Навык доступен с роли ИТР и выше"))
        }
        var working = ensureProfile(state, employee.id)
        val profile = working.profiles.getValue(employee.id)
        if (profile.skillPointsAvailable <= 0) {
            return Result.failure(IllegalStateException("Недостаточно очков навыков (SP)"))
        }
        if (def.prerequisiteSkillId != null) {
            val prereqLevel = profile.skills.firstOrNull { it.skillId == def.prerequisiteSkillId }?.level ?: 0
            if (prereqLevel < 1) {
                val prereqName = skillCatalog.firstOrNull { it.id == def.prerequisiteSkillId }?.titleRu ?: def.prerequisiteSkillId
                return Result.failure(IllegalStateException("Сначала изучите базовый навык: «$prereqName»"))
            }
        }
        val currentLevel = profile.skills.firstOrNull { it.skillId == skillId }?.level ?: 0
        if (currentLevel >= def.maxLevel) {
            return Result.failure(IllegalStateException("Навык уже развит до максимального уровня (${def.maxLevel})"))
        }
        val updatedSkills = profile.skills.filterNot { it.skillId == skillId } + UserSkill(skillId, currentLevel + 1)
        val updatedProfile = profile.copy(
            skillPointsAvailable = profile.skillPointsAvailable - 1,
            skillPointsSpent = profile.skillPointsSpent + 1,
            skills = updatedSkills
        )
        working = working.copy(profiles = working.profiles + (employee.id to updatedProfile))
        return Result.success(working)
    }

    fun resetSkillsForSeason(
        state: RpgWorkspaceState,
        employee: CorporateEmployee
    ): Result<RpgWorkspaceState> {
        var working = ensureProfile(state, employee.id)
        val profile = working.profiles.getValue(employee.id)
        if (profile.seasonSkillResetDoneForSeasonId == working.currentSeason.id) {
            return Result.failure(IllegalStateException("Сброс навыков уже использован в текущем сезоне"))
        }
        if (profile.skillPointsSpent <= 0) {
            return Result.failure(IllegalStateException("Нет распределённых очков навыков для сброса"))
        }
        val penalty = working.config.seasonalResetSpPenalty.coerceAtLeast(0)
        val refunded = (profile.skillPointsSpent - penalty).coerceAtLeast(0)
        val updatedProfile = profile.copy(
            skillPointsAvailable = profile.skillPointsAvailable + refunded,
            skillPointsSpent = 0,
            skills = emptyList(),
            seasonSkillResetDoneForSeasonId = working.currentSeason.id
        )
        val tx = XpTransaction(
            id = "xp_${System.currentTimeMillis()}_skreset",
            employeeId = employee.id,
            employeeName = employee.shortName,
            type = XpTransactionType.SEASON_RESET,
            xpDelta = 0,
            reason = "Сезонный сброс навыков (штраф: -$penalty SP, возвращено: $refunded SP)",
            actorId = employee.id,
            actorName = employee.shortName
        )
        working = working.copy(
            profiles = working.profiles + (employee.id to updatedProfile),
            transactions = (listOf(tx) + working.transactions).take(400)
        )
        return Result.success(working)
    }

    fun useStreakRecovery(
        state: RpgWorkspaceState,
        employee: CorporateEmployee,
        currentEpochDay: Long
    ): Result<RpgWorkspaceState> {
        var working = ensureProfile(state, employee.id)
        val profile = working.profiles.getValue(employee.id)
        if (profile.inventory.streakRecoveryTokens <= 0) {
            return Result.failure(IllegalStateException("Нет доступных жетонов восстановления серии"))
        }
        if (!profile.streak.isAtRisk && profile.streak.currentDays > 0) {
            return Result.failure(IllegalStateException("Серия не находится под угрозой сброса"))
        }
        val restoredStreak = profile.streak.copy(
            isAtRisk = false,
            lastActivityEpochDay = currentEpochDay,
            recoveryUsedCount = profile.streak.recoveryUsedCount + 1
        )
        val updatedProfile = profile.copy(
            streak = restoredStreak,
            inventory = profile.inventory.copy(streakRecoveryTokens = profile.inventory.streakRecoveryTokens - 1)
        )
        return Result.success(working.copy(profiles = working.profiles + (employee.id to updatedProfile)))
    }

    fun equipInventoryItem(
        state: RpgWorkspaceState,
        employeeId: String,
        itemId: String
    ): RpgWorkspaceState {
        val working = ensureProfile(state, employeeId)
        val profile = working.profiles.getValue(employeeId)
        if (itemId !in profile.inventory.ownedItemIds) return working
        val item = inventoryCatalog.firstOrNull { it.id == itemId } ?: return working
        val nextInv = when (item.category) {
            InventoryItemCategory.BADGE -> profile.inventory.copy(
                equippedBadgeId = if (profile.inventory.equippedBadgeId == itemId) null else itemId
            )
            InventoryItemCategory.AVATAR_FRAME -> profile.inventory.copy(
                equippedFrameId = if (profile.inventory.equippedFrameId == itemId) null else itemId
            )
            InventoryItemCategory.TITLE -> profile.inventory.copy(
                equippedTitleId = if (profile.inventory.equippedTitleId == itemId) null else itemId
            )
            InventoryItemCategory.THEME -> profile.inventory
        }
        return working.copy(profiles = working.profiles + (employeeId to profile.copy(inventory = nextInv)))
    }

    fun activatePrestige(
        state: RpgWorkspaceState,
        employee: CorporateEmployee
    ): Result<RpgWorkspaceState> {
        var working = ensureProfile(state, employee.id)
        val profile = working.profiles.getValue(employee.id)
        val levelInfo = resolveUserLevel(profile.totalXp, working.config)
        if (!levelInfo.isMaxLevel) {
            return Result.failure(IllegalStateException("Престиж открывается на максимальном ${working.config.maxLevel} уровне"))
        }
        val nextPrestigeLevel = profile.prestige.prestigeLevel + 1
        val nextBonus = nextPrestigeLevel * working.config.prestigePermanentBonusPerLevel
        val emblem = "Престиж $nextPrestigeLevel"
        val updatedInv = profile.inventory.copy(
            ownedItemIds = (profile.inventory.ownedItemIds + "item_badge_prestige").distinct(),
            streakRecoveryTokens = profile.inventory.streakRecoveryTokens + 1
        )
        val resetProfile = profile.copy(
            totalXp = 0,
            skillPointsAvailable = 2,
            skillPointsSpent = 0,
            skills = emptyList(),
            inventory = updatedInv,
            prestige = PrestigeState(
                prestigeLevel = nextPrestigeLevel,
                permanentXpMultiplierBonus = nextBonus,
                emblemTitle = emblem,
                activatedAtMillis = System.currentTimeMillis()
            )
        )
        val tx = XpTransaction(
            id = "xp_${System.currentTimeMillis()}_prestige",
            employeeId = employee.id,
            employeeName = employee.shortName,
            type = XpTransactionType.PRESTIGE_RESET,
            xpDelta = 0,
            reason = "Активирован $emblem (+${(nextBonus * 100).roundToInt()}% постоянного XP)",
            actorId = employee.id,
            actorName = employee.shortName
        )
        working = working.copy(
            profiles = working.profiles + (employee.id to resetProfile),
            transactions = (listOf(tx) + working.transactions).take(400)
        )
        val achRes = evaluateAchievements(working, employee, TaskPriority.MEDIUM, hasPhoto = false, firstTry = true)
        return Result.success(achRes.first)
    }

    fun adjustManually(
        state: RpgWorkspaceState,
        actor: CorporateEmployee,
        target: CorporateEmployee,
        xpDelta: Int,
        reputationDelta: Int,
        reason: String
    ): Result<RpgWorkspaceState> {
        if (!canAdjustProgression(actor)) {
            return Result.failure(IllegalAccessException("Ручная корректировка XP доступна только Руководителю или Администратору"))
        }
        val cleanReason = reason.trim()
        if (cleanReason.isBlank()) {
            return Result.failure(IllegalArgumentException("Укажите обязательную причину корректировки"))
        }
        var working = ensureProfile(state, target.id)
        val profile = working.profiles.getValue(target.id)
        val oldLevel = resolveUserLevel(profile.totalXp, working.config).level
        val tx = XpTransaction(
            id = "xp_${System.currentTimeMillis()}_manual_${(100..999).random()}",
            employeeId = target.id,
            employeeName = target.shortName,
            type = XpTransactionType.MANUAL_ADJUSTMENT,
            xpDelta = xpDelta,
            reputationDelta = reputationDelta,
            appliedMultipliersSummary = "Ручная правка",
            reason = cleanReason,
            actorId = actor.id,
            actorName = actor.shortName,
            createdAtMillis = System.currentTimeMillis()
        )
        working = applyTransactionToProfile(working, target.id, tx)
        val updated = working.profiles.getValue(target.id)
        val newLevel = resolveUserLevel(updated.totalXp, working.config).level
        if (newLevel > oldLevel) {
            working = working.copy(
                profiles = working.profiles + (target.id to updated.copy(
                    skillPointsAvailable = updated.skillPointsAvailable + (newLevel - oldLevel)
                ))
            )
        }
        return Result.success(working)
    }

    fun setLeaderboardPrivacy(
        state: RpgWorkspaceState,
        employeeId: String,
        privacy: LeaderboardPrivacy
    ): RpgWorkspaceState {
        val working = ensureProfile(state, employeeId)
        val prof = working.profiles.getValue(employeeId)
        return working.copy(profiles = working.profiles + (employeeId to prof.copy(leaderboardPrivacy = privacy)))
    }

    fun buildLeaderboard(
        state: RpgWorkspaceState,
        viewer: CorporateEmployee,
        employees: List<CorporateEmployee>
    ): List<LeaderboardEntry> {
        return employees.mapNotNull { emp ->
            val prof = state.profileForEmployee(emp)
            val isSelf = emp.id == viewer.id
            val isSameGroup = emp.groupId == viewer.groupId ||
                emp.groupName.equals(viewer.groupName, ignoreCase = true)
            val visible = when {
                isSelf -> true
                viewer.role.isAdminOrHead -> true
                prof.leaderboardPrivacy == LeaderboardPrivacy.HIDDEN -> false
                prof.leaderboardPrivacy == LeaderboardPrivacy.GROUP -> isSameGroup
                else -> true
            }
            if (!visible) return@mapNotNull null
            val lvl = resolveUserLevel(prof.totalXp, state.config)
            val status = resolveUserStatus(emp.role, lvl.level)
            val titleName = prof.inventory.equippedTitleId?.let { id ->
                inventoryCatalog.firstOrNull { it.id == id }?.nameRu.orEmpty()
            }.orEmpty()
            val badgeName = prof.inventory.equippedBadgeId?.let { id ->
                inventoryCatalog.firstOrNull { it.id == id }?.nameRu.orEmpty()
            }.orEmpty()
            LeaderboardEntry(
                employeeId = emp.id,
                employeeName = emp.fullName,
                role = emp.role,
                groupId = emp.groupId,
                groupName = emp.groupName,
                rankTitle = status.titleRu,
                level = lvl.level,
                prestigeLevel = prof.prestige.prestigeLevel,
                seasonXp = prof.seasonXp,
                totalXp = prof.totalXp,
                reputation = prof.reputation.score,
                streakDays = prof.streak.currentDays,
                equippedTitle = titleName,
                equippedBadgeName = badgeName,
                privacy = prof.leaderboardPrivacy
            )
        }.sortedWith(
            compareByDescending<LeaderboardEntry> { it.seasonXp }
                .thenByDescending { it.totalXp }
                .thenByDescending { it.reputation }
        )
    }

    private fun applyTransactionToProfile(
        state: RpgWorkspaceState,
        employeeId: String,
        tx: XpTransaction
    ): RpgWorkspaceState {
        val working = ensureProfile(state, employeeId)
        val profile = working.profiles.getValue(employeeId)
        val nextTotalXp = (profile.totalXp + tx.xpDelta).coerceAtLeast(0)
        val nextSeasonXp = (profile.seasonXp + tx.xpDelta).coerceAtLeast(0)
        val nextRepScore = (profile.reputation.score + tx.reputationDelta).coerceIn(0, 100)
        val updatedRepHistory = if (tx.reputationDelta != 0) {
            (listOf(
                ReputationEntry(
                    id = "rep_${tx.id}",
                    delta = tx.reputationDelta,
                    resultingScore = nextRepScore,
                    reason = tx.reason,
                    createdAtMillis = tx.createdAtMillis
                )
            ) + profile.reputation.history).take(30)
        } else {
            profile.reputation.history
        }
        val nextProfile = profile.copy(
            totalXp = nextTotalXp,
            seasonXp = nextSeasonXp,
            reputation = Reputation(score = nextRepScore, history = updatedRepHistory)
        )
        return working.copy(
            profiles = working.profiles + (employeeId to nextProfile),
            transactions = (listOf(tx) + working.transactions).take(400)
        )
    }

    private fun advanceStreak(streak: Streak, epochDay: Long): Streak {
        if (streak.lastActivityEpochDay == epochDay && streak.currentDays > 0) {
            return streak.copy(isAtRisk = false)
        }
        val nextDays = when {
            streak.lastActivityEpochDay == 0L -> 1
            epochDay - streak.lastActivityEpochDay <= 1L -> streak.currentDays + 1
            else -> 1
        }
        return streak.copy(
            currentDays = nextDays,
            bestDays = maxOf(streak.bestDays, nextDays),
            lastActivityEpochDay = epochDay,
            isAtRisk = false
        )
    }

    private fun updateQuestsForTaskApproval(
        state: RpgWorkspaceState,
        employee: CorporateEmployee,
        task: TaskDocument,
        priority: TaskPriority,
        hasPhoto: Boolean,
        firstTry: Boolean,
        currentEpochDay: Long,
        isSupervisorAction: Boolean
    ): Pair<RpgWorkspaceState, List<String>> {
        var working = ensureProfile(state, employee.id)
        var profile = working.profiles.getValue(employee.id)
        val completedIds = mutableListOf<String>()
        val nowMillis = System.currentTimeMillis()
        val weekNum = currentEpochDay / 7L

        val nextProgressList = profile.questProgress.toMutableList()
        questCatalog.filter { employee.role.hierarchyWeight >= it.minRoleWeight }.forEach { quest ->
            val periodKey = when (quest.cadence) {
                QuestCadence.DAILY -> "D_$currentEpochDay"
                QuestCadence.WEEKLY -> "W_$weekNum"
                QuestCadence.SEASONAL -> "S_${working.currentSeason.id}"
                QuestCadence.STORY -> "STORY"
            }
            val metricMatches = when (quest.metricType) {
                QuestMetricType.COMPLETE_TASKS -> !isSupervisorAction
                QuestMetricType.PHOTO_VERIFIED_TASKS -> !isSupervisorAction && hasPhoto
                QuestMetricType.FIRST_TRY_APPROVALS -> !isSupervisorAction && firstTry
                QuestMetricType.HIGH_OR_ABOVE_TASKS -> !isSupervisorAction && priority.weight >= TaskPriority.HIGH.weight
                QuestMetricType.SUPERVISOR_REVIEWS -> isSupervisorAction
            }
            if (!metricMatches) return@forEach

            val existingIdx = nextProgressList.indexOfFirst { it.questId == quest.id && it.periodKey == periodKey }
            val currentQp = if (existingIdx >= 0) {
                nextProgressList[existingIdx]
            } else {
                UserQuestProgress(questId = quest.id, periodKey = periodKey)
            }
            if (task.id in currentQp.countedTaskIds || currentQp.rewarded) return@forEach

            val updatedIds = currentQp.countedTaskIds + task.id
            val nextCount = updatedIds.size
            val justCompleted = nextCount >= quest.targetCount
            val updatedQp = currentQp.copy(
                countedTaskIds = updatedIds,
                currentCount = nextCount,
                completedAtMillis = if (justCompleted) nowMillis else currentQp.completedAtMillis,
                rewarded = justCompleted
            )
            if (existingIdx >= 0) nextProgressList[existingIdx] = updatedQp else nextProgressList.add(updatedQp)

            if (justCompleted) {
                completedIds.add(quest.id)
                val qTx = XpTransaction(
                    id = "xp_${nowMillis}_q_${quest.id.take(6)}",
                    employeeId = employee.id,
                    employeeName = employee.shortName,
                    taskId = task.id,
                    taskTitle = quest.titleRu,
                    type = XpTransactionType.QUEST_REWARD,
                    xpDelta = quest.xpReward,
                    reputationDelta = quest.reputationReward,
                    appliedMultipliersSummary = quest.cadence.titleRu,
                    reason = "Завершён квест «${quest.titleRu}»",
                    actorId = employee.id,
                    actorName = employee.shortName,
                    idempotencyKey = "QUEST_${quest.id}_${periodKey}_${employee.id}",
                    createdAtMillis = nowMillis
                )
                working = applyTransactionToProfile(working, employee.id, qTx)
                profile = working.profiles.getValue(employee.id)
                if (quest.rewardItemId.isNotBlank() && quest.rewardItemId !in profile.inventory.ownedItemIds) {
                    profile = profile.copy(
                        inventory = profile.inventory.copy(
                            ownedItemIds = (profile.inventory.ownedItemIds + quest.rewardItemId).distinct()
                        )
                    )
                    working = working.copy(profiles = working.profiles + (employee.id to profile))
                }
            }
        }
        profile = working.profiles.getValue(employee.id).copy(questProgress = nextProgressList)
        working = working.copy(profiles = working.profiles + (employee.id to profile))
        return working to completedIds
    }

    private fun evaluateAchievements(
        state: RpgWorkspaceState,
        employee: CorporateEmployee,
        priority: TaskPriority,
        hasPhoto: Boolean,
        firstTry: Boolean
    ): Pair<RpgWorkspaceState, List<String>> {
        var working = ensureProfile(state, employee.id)
        var profile = working.profiles.getValue(employee.id)
        val unlockedNow = mutableListOf<String>()
        val nowMillis = System.currentTimeMillis()
        val alreadyOwned = profile.achievements.map { it.achievementId }.toSet()

        val approvedTxCount = working.transactions.count {
            it.employeeId == employee.id &&
                !it.isRolledBack &&
                (it.type == XpTransactionType.TASK_COMPLETION || it.type == XpTransactionType.SUPERVISOR_APPROVAL) &&
                it.xpDelta > 0
        }
        val photoTxCount = working.transactions.count {
            it.employeeId == employee.id &&
                !it.isRolledBack &&
                it.appliedMultipliersSummary.contains("Фото")
        }

        val candidates = mutableListOf<String>()
        if (approvedTxCount >= 1) candidates.add("ach_first_approved")
        if (profile.streak.currentDays >= 5) candidates.add("ach_streak_5")
        if (profile.streak.currentDays >= 10) candidates.add("ach_streak_10")
        if (profile.streak.currentDays >= 20) candidates.add("ach_streak_20")
        if (photoTxCount >= 5 || (hasPhoto && firstTry && approvedTxCount >= 5)) candidates.add("ach_photo_master")
        if (priority == TaskPriority.CRITICAL || priority == TaskPriority.EMERGENCY) candidates.add("ach_emergency_hero")
        if (profile.reputation.score >= 90) candidates.add("ach_reputation_90")
        if (profile.prestige.prestigeLevel >= 1) candidates.add("ach_prestige_1")

        candidates.filter { it !in alreadyOwned }.forEach { achId ->
            val achDef = achievementCatalog.firstOrNull { it.id == achId } ?: return@forEach
            unlockedNow.add(achId)
            val newAch = UserAchievement(
                achievementId = achId,
                unlockedAtMillis = nowMillis,
                progressNote = "Открыто"
            )
            val ownedItems = if (achDef.badgeItemId.isNotBlank()) {
                (profile.inventory.ownedItemIds + achDef.badgeItemId).distinct()
            } else {
                profile.inventory.ownedItemIds
            }
            profile = profile.copy(
                achievements = profile.achievements + newAch,
                inventory = profile.inventory.copy(ownedItemIds = ownedItems)
            )
            working = working.copy(profiles = working.profiles + (employee.id to profile))
            val achTx = XpTransaction(
                id = "xp_${nowMillis}_ach_${achId.takeLast(5)}",
                employeeId = employee.id,
                employeeName = employee.shortName,
                taskTitle = achDef.titleRu,
                type = XpTransactionType.ACHIEVEMENT_BONUS,
                xpDelta = achDef.xpReward,
                reputationDelta = 2,
                appliedMultipliersSummary = achDef.rarity.titleRu,
                reason = "Достижение «${achDef.titleRu}»",
                actorId = employee.id,
                actorName = employee.shortName,
                idempotencyKey = "ACH_${achId}_${employee.id}",
                createdAtMillis = nowMillis
            )
            working = applyTransactionToProfile(working, employee.id, achTx)
            profile = working.profiles.getValue(employee.id)
        }
        return working to unlockedNow
    }
}
