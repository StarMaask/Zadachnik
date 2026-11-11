package com.example.data.seed

import com.example.data.model.AuditLogDocument
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.ExportFormat
import com.example.data.model.PhotoReportItem
import com.example.data.model.RecurrenceRule
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.ReportType
import com.example.data.model.SheetLayout
import com.example.data.model.StatusHistoryEntry
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskCommentItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.WorkGroup

data class QuickTaskTemplate(
    val id: String,
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val requiresPhotoReport: Boolean,
    val minPhotoCount: Int,
    val recurrenceRule: RecurrenceRule,
    val defaultSubtasks: List<String>,
    val tags: List<String>
)

object CorporateSeedData {

    const val DEFAULT_ORG_NAME = "ООО «ГлавСтройИнжиниринг»"
    const val DEFAULT_SITE_ID = "site_1"
    const val DEFAULT_SITE_NAME = "Участок №1 — ПГС и Инженерные сети"
    const val BASE_EPOCH_DAY = 20768L // 2026-11-10

    val workGroups: List<WorkGroup> = listOf(
        WorkGroup(
            id = "grp_monolith",
            name = "Бригада №1 • Монолит и ЖБК",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_itr_1",
            supervisorName = "Соколов А.В.",
            specialization = "Армирование, опалубка, бетонирование конструкций"
        ),
        WorkGroup(
            id = "grp_mep",
            name = "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_itr_2",
            supervisorName = "Ермакова Е.И.",
            specialization = "Силовые кабельные линии, вентиляция, трубопроводы"
        ),
        WorkGroup(
            id = "grp_control",
            name = "Служба ИТР и стройконтроля",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_head_1",
            supervisorName = "Ковалёв Д.С.",
            specialization = "Геодезия, приёмка скрытых работ, охрана труда"
        )
    )

    val employees: List<CorporateEmployee> = listOf(
        CorporateEmployee(
            id = "emp_head_1",
            fullName = "Ковалёв Дмитрий Сергеевич",
            shortName = "Ковалёв Д.С.",
            position = "Начальник строительного участка",
            role = CorporateRole.HEAD,
            groupId = "grp_control",
            groupName = "Служба ИТР и стройконтроля",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = null,
            phone = "+7 (916) 440-10-01",
            email = "kovalev.ds@glavstroy.ru"
        ),
        CorporateEmployee(
            id = "emp_itr_1",
            fullName = "Соколов Андрей Викторович",
            shortName = "Соколов А.В.",
            position = "Производитель работ (Прораб СМР)",
            role = CorporateRole.ITR,
            groupId = "grp_monolith",
            groupName = "Бригада №1 • Монолит и ЖБК",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_head_1",
            phone = "+7 (916) 440-10-12",
            email = "sokolov.av@glavstroy.ru"
        ),
        CorporateEmployee(
            id = "emp_itr_2",
            fullName = "Ермакова Елена Игоревна",
            shortName = "Ермакова Е.И.",
            position = "Инженер ПТО и технадзора",
            role = CorporateRole.ITR,
            groupId = "grp_mep",
            groupName = "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_head_1",
            phone = "+7 (916) 440-10-15",
            email = "ermakova.ei@glavstroy.ru"
        ),
        CorporateEmployee(
            id = "emp_worker_1",
            fullName = "Морозов Илья Павлович",
            shortName = "Морозов И.П.",
            position = "Бригадир арматурщиков 6 разряда",
            role = CorporateRole.WORKER,
            groupId = "grp_monolith",
            groupName = "Бригада №1 • Монолит и ЖБК",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_itr_1",
            phone = "+7 (916) 440-20-21",
            email = "morozov.ip@glavstroy.ru"
        ),
        CorporateEmployee(
            id = "emp_worker_2",
            fullName = "Волков Артём Николаевич",
            shortName = "Волков А.Н.",
            position = "Бетонщик-монтажник 5 разряда",
            role = CorporateRole.WORKER,
            groupId = "grp_monolith",
            groupName = "Бригада №1 • Монолит и ЖБК",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_itr_1",
            phone = "+7 (916) 440-20-22",
            email = "volkov.an@glavstroy.ru"
        ),
        CorporateEmployee(
            id = "emp_worker_3",
            fullName = "Кузнецов Роман Олегович",
            shortName = "Кузнецов Р.О.",
            position = "Электромонтажник силовых сетей",
            role = CorporateRole.WORKER,
            groupId = "grp_mep",
            groupName = "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_itr_2",
            phone = "+7 (916) 440-30-31",
            email = "kuznetsov.ro@glavstroy.ru"
        ),
        CorporateEmployee(
            id = "emp_worker_4",
            fullName = "Белов Максим Денисович",
            shortName = "Белов М.Д.",
            position = "Монтажник систем ОВиК и ВК",
            role = CorporateRole.WORKER,
            groupId = "grp_mep",
            groupName = "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
            siteId = DEFAULT_SITE_ID,
            siteName = DEFAULT_SITE_NAME,
            supervisorId = "emp_itr_2",
            phone = "+7 (916) 440-30-35",
            email = "belov.md@glavstroy.ru"
        )
    )

    val quickTaskTemplates: List<QuickTaskTemplate> = listOf(
        QuickTaskTemplate(
            id = "tpl_task_rebar",
            title = "Приёмка армирования и защитного слоя бетона",
            description = "Проверить шаг стержней, фиксаторы защитного слоя (35 мм), выполнить фотофиксацию узлов.",
            priority = TaskPriority.HIGH,
            requiresPhotoReport = true,
            minPhotoCount = 2,
            recurrenceRule = RecurrenceRule.NONE,
            defaultSubtasks = listOf(
                "Проверить диаметр и шаг рабочей арматуры",
                "Установить пластиковые фиксаторы защитного слоя",
                "Сделать минимум 2 фото с привязкой к осям"
            ),
            tags = listOf("СкрытыеРаботы", "Монолит", "КЖ")
        ),
        QuickTaskTemplate(
            id = "tpl_task_safety",
            title = "Ежедневный обход ограждений опасных зон и ТБ",
            description = "Осмотр защитных козырьков, инвентарных ограждений проёмов, наличия СИЗ и огнетушителей.",
            priority = TaskPriority.CRITICAL,
            requiresPhotoReport = true,
            minPhotoCount = 1,
            recurrenceRule = RecurrenceRule.DAILY,
            defaultSubtasks = listOf(
                "Проверить ограждения лифтовых шахт",
                "Проверить заземление временных щитов ЩР",
                "Зафиксировать состояние сигнальных лент"
            ),
            tags = listOf("ОхранаТруда", "Регламент", "ТБ")
        ),
        QuickTaskTemplate(
            id = "tpl_task_cable",
            title = "Монтаж кабельных лотков и прокладка линий ВВГнг-LS",
            description = "Установка перфорированных лотков по отм. +4.200 с маркировкой трасс и замером сопротивления изоляции.",
            priority = TaskPriority.MEDIUM,
            requiresPhotoReport = true,
            minPhotoCount = 2,
            recurrenceRule = RecurrenceRule.NONE,
            defaultSubtasks = listOf(
                "Разметка трассы по лазерному нивелиру",
                "Монтаж консолей и лотков",
                "Биркование кабельных линий"
            ),
            tags = listOf("ЭОМ", "Кабель", "СекцияБ")
        ),
        QuickTaskTemplate(
            id = "tpl_task_hydro",
            title = "Гидравлическая опрессовка трубопроводов отопления",
            description = "Испытание контура давлением 1.0 МПа в течение 30 минут с фото манометра.",
            priority = TaskPriority.HIGH,
            requiresPhotoReport = true,
            minPhotoCount = 2,
            recurrenceRule = RecurrenceRule.WEEKLY,
            defaultSubtasks = listOf(
                "Заполнение стояков и спуск воздуха",
                "Подъём давления до 1.0 МПа",
                "Фото показаний поверенного манометра"
            ),
            tags = listOf("ОВиК", "Опрессовка", "Акт")
        )
    )

    fun findEmployee(id: String): CorporateEmployee =
        employees.firstOrNull { it.id == id } ?: employees.first()

    fun getAssigneesForActor(actor: CorporateEmployee): List<CorporateEmployee> {
        return when (actor.role) {
            CorporateRole.HEAD -> employees
            CorporateRole.ITR -> employees.filter {
                it.id == actor.id ||
                    (it.role == CorporateRole.WORKER && it.groupId == actor.groupId) ||
                    it.supervisorId == actor.id
            }
            CorporateRole.WORKER -> listOf(actor)
        }
    }

    fun getVisibleEmployeesForStats(actor: CorporateEmployee): List<CorporateEmployee> {
        return when (actor.role) {
            CorporateRole.HEAD -> employees
            CorporateRole.ITR -> employees.filter {
                it.groupId == actor.groupId || it.supervisorId == actor.id || it.id == actor.id
            }
            CorporateRole.WORKER -> listOf(actor)
        }
    }

    fun buildInitialTasks(userId: String): List<TaskDocument> = listOf(
        // 1. ON_REVIEW task with 2 photos ready for ITR/Head approval
        TaskDocument(
            id = "seed_task_1",
            userId = userId,
            title = "Армирование плиты перекрытия на отм. +12.600 (оси 4–8 / А–Г)",
            description = "Выполнить вязку верхней и нижней сетки d16 А500С с шагом 200×200 мм. Обязательная фотофиксация выпусков колонн и фиксаторов защитного слоя.",
            creatorId = "emp_itr_1",
            creatorName = "Соколов Андрей Викторович",
            assigneeId = "emp_worker_1",
            assigneeName = "Морозов Илья Павлович",
            groupId = "grp_monolith",
            groupName = "Бригада №1 • Монолит и ЖБК",
            siteId = DEFAULT_SITE_ID,
            priority = TaskPriority.CRITICAL.code,
            status = TaskStatus.ON_REVIEW.code,
            dueDateEpochDay = BASE_EPOCH_DAY,
            originalDueDateEpochDay = BASE_EPOCH_DAY,
            deadlineTime = "16:00",
            carryoverCount = 0L,
            requiresPhotoReport = true,
            minPhotoCount = 2L,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.NONE.code,
            tags = listOf("Монолит", "СкрытыеРаботы", "Плита12600"),
            subtaskItems = listOf(
                SubtaskItem("s1", "Нижняя сетка d16 А500С шаг 200 мм", true).encode(),
                SubtaskItem("s2", "Установка фиксаторов «Стульчик 35 мм»", true).encode(),
                SubtaskItem("s3", "Усиление П-образными хомутами у торцов", true).encode()
            ),
            photoItems = listOf(
                PhotoReportItem(
                    id = "ph_seed_1",
                    uriString = "stamp://Узел армирования осей 4-А | Защитный слой 35 мм",
                    authorId = "emp_worker_1",
                    authorName = "Морозов И.П.",
                    capturedAtText = "10.11.2026 14:15",
                    gpsCoords = "55.7558° N, 37.6173° E",
                    comment = "Нижняя и верхняя сетки связаны, шаг 200 мм выдержан по рулетке"
                ).encode(),
                PhotoReportItem(
                    id = "ph_seed_2",
                    uriString = "stamp://Выпуски колонн К-14 и обрамление проёма шахты",
                    authorId = "emp_worker_1",
                    authorName = "Морозов И.П.",
                    capturedAtText = "10.11.2026 14:22",
                    gpsCoords = "55.7559° N, 37.6175° E",
                    comment = "Усиление проёма выполнено стержнями d20, мусор продули компрессором"
                ).encode()
            ),
            commentItems = listOf(
                TaskCommentItem(
                    id = "c1",
                    authorId = "emp_worker_1",
                    authorName = "Морозов И.П.",
                    authorRole = "WORKER",
                    text = "@Соколов А.В. Каркас полностью готов к геодезической съёмке и приёмке бетона.",
                    mentionedUser = "Соколов А.В.",
                    createdAtText = "14:25",
                    isRead = true
                ).encode()
            ),
            historyItems = listOf(
                StatusHistoryEntry("08:15", "Соколов А.В.", "NEW", "Наряд выдан на планёрке").encode(),
                StatusHistoryEntry("09:00", "Морозов И.П.", "IN_PROGRESS", "Приступили к вязке арматуры").encode(),
                StatusHistoryEntry("14:25", "Морозов И.П.", "ON_REVIEW", "Приложено 2 фото, отправлено на проверку ИТР").encode()
            ),
            geoLocation = "Секция А, отм. +12.600"
        ),

        // 2. IN_PROGRESS task requiring 2 photos (currently 1 photo attached — demonstrates photo validation guard!)
        TaskDocument(
            id = "seed_task_2",
            userId = userId,
            title = "Монтаж огнестойких кабельных лотков в электрощитовой №2",
            description = "Установить консольные кронштейны и перфорированные лотки 300×100 мм с шиной заземления ПВ-3. Требуется минимум 2 фото.",
            creatorId = "emp_itr_2",
            creatorName = "Ермакова Елена Игоревна",
            assigneeId = "emp_worker_3",
            assigneeName = "Кузнецов Роман Олегович",
            groupId = "grp_mep",
            groupName = "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
            siteId = DEFAULT_SITE_ID,
            priority = TaskPriority.HIGH.code,
            status = TaskStatus.IN_PROGRESS.code,
            dueDateEpochDay = BASE_EPOCH_DAY,
            originalDueDateEpochDay = BASE_EPOCH_DAY,
            deadlineTime = "18:00",
            carryoverCount = 0L,
            requiresPhotoReport = true,
            minPhotoCount = 2L,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.NONE.code,
            tags = listOf("ЭОМ", "Щитовая", "ОКЛ"),
            subtaskItems = listOf(
                SubtaskItem("s21", "Разметка трассы по лазерному уровню", true).encode(),
                SubtaskItem("s22", "Крепление анкеров М10 в перекрытие", true).encode(),
                SubtaskItem("s23", "Монтаж перемычек заземления лотков", false).encode()
            ),
            photoItems = listOf(
                PhotoReportItem(
                    id = "ph_seed_3",
                    uriString = "stamp://Трасса лотков 300×100 по оси В | Первый участок",
                    authorId = "emp_worker_3",
                    authorName = "Кузнецов Р.О.",
                    capturedAtText = "10.11.2026 11:40",
                    gpsCoords = "55.7556° N, 37.6170° E",
                    comment = "Смонтированы консоли и прямой участок 18 п.м. (нужно ещё 1 фото заземления)"
                ).encode()
            ),
            commentItems = emptyList(),
            historyItems = listOf(
                StatusHistoryEntry("08:30", "Ермакова Е.И.", "NEW", "Поставлена задача на смену").encode(),
                StatusHistoryEntry("09:10", "Кузнецов Р.О.", "IN_PROGRESS", "Взято в работу").encode()
            ),
            geoLocation = "Корпус 1, отм. -3.600, Пом. ЭЩ-2"
        ),

        // 3. APPROVED task (visually strikethrough and dimmed!)
        TaskDocument(
            id = "seed_task_3",
            userId = userId,
            title = "Гидравлические испытания стояков теплоснабжения Т1/Т2 (захватка 1)",
            description = "Опрессовка трубопроводов давлением 1.0 МПа с составлением акта и фото показаний манометра.",
            creatorId = "emp_head_1",
            creatorName = "Ковалёв Дмитрий Сергеевич",
            assigneeId = "emp_worker_4",
            assigneeName = "Белов Максим Денисович",
            groupId = "grp_mep",
            groupName = "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
            siteId = DEFAULT_SITE_ID,
            priority = TaskPriority.HIGH.code,
            status = TaskStatus.APPROVED.code,
            dueDateEpochDay = BASE_EPOCH_DAY,
            originalDueDateEpochDay = BASE_EPOCH_DAY,
            deadlineTime = "13:00",
            carryoverCount = 0L,
            requiresPhotoReport = true,
            minPhotoCount = 1L,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.NONE.code,
            tags = listOf("ОВиК", "Опрессовка", "Принято"),
            subtaskItems = listOf(
                SubtaskItem("s31", "Накачка давления 1.0 МПа", true).encode(),
                SubtaskItem("s32", "Выдержка 30 минут без падения давления", true).encode()
            ),
            photoItems = listOf(
                PhotoReportItem(
                    id = "ph_seed_4",
                    uriString = "stamp://Манометр МП3-У: 1.02 МПа стабильно (пломба №8841)",
                    authorId = "emp_worker_4",
                    authorName = "Белов М.Д.",
                    capturedAtText = "10.11.2026 12:10",
                    gpsCoords = "55.7560° N, 37.6180° E",
                    comment = "Падение давления за 30 мин — 0.00 МПа, течей в резьбовых и сварных стыках нет"
                ).encode()
            ),
            commentItems = listOf(
                TaskCommentItem(
                    id = "c3",
                    authorId = "emp_head_1",
                    authorName = "Ковалёв Д.С.",
                    authorRole = "HEAD",
                    text = "Отчёт принят без замечаний. Разрешаю теплоизоляцию стояков.",
                    mentionedUser = "Белов М.Д.",
                    createdAtText = "12:35",
                    isRead = true
                ).encode()
            ),
            historyItems = listOf(
                StatusHistoryEntry("08:00", "Ковалёв Д.С.", "NEW", "Задача создана").encode(),
                StatusHistoryEntry("12:12", "Белов М.Д.", "ON_REVIEW", "Отчёт отправлен на проверку").encode(),
                StatusHistoryEntry("12:35", "Ковалёв Д.С.", "APPROVED", "Подтверждено начальником участка").encode()
            ),
            approvedByName = "Ковалёв Дмитрий Сергеевич",
            geoLocation = "Секция Б, ИТП"
        ),

        // 4. REJECTED task with mandatory reason from supervisor
        TaskDocument(
            id = "seed_task_4",
            userId = userId,
            title = "Монтаж щитовой опалубки диафрагмы жёсткости ДЖ-3",
            description = "Выставить щиты по вертикали, установить двухъярусные подкосы и стяжные винты с шагом 600 мм.",
            creatorId = "emp_itr_1",
            creatorName = "Соколов Андрей Викторович",
            assigneeId = "emp_worker_2",
            assigneeName = "Волков Артём Николаевич",
            groupId = "grp_monolith",
            groupName = "Бригада №1 • Монолит и ЖБК",
            siteId = DEFAULT_SITE_ID,
            priority = TaskPriority.HIGH.code,
            status = TaskStatus.REJECTED.code,
            dueDateEpochDay = BASE_EPOCH_DAY,
            originalDueDateEpochDay = BASE_EPOCH_DAY - 1L,
            deadlineTime = "17:00",
            carryoverCount = 1L,
            requiresPhotoReport = true,
            minPhotoCount = 2L,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.NONE.code,
            tags = listOf("Опалубка", "Доработка", "ДЖ3"),
            subtaskItems = listOf(
                SubtaskItem("s41", "Смазка палубы эмульсолом", true).encode(),
                SubtaskItem("s42", "Установка стяжных тяжей и гаек", true).encode(),
                SubtaskItem("s43", "Доустановка контргаек на нижнем ряду (замечание ИТР)", false).encode()
            ),
            photoItems = listOf(
                PhotoReportItem(
                    id = "ph_seed_5",
                    uriString = "stamp://Опалубка ДЖ-3 общий вид со стороны оси 5",
                    authorId = "emp_worker_2",
                    authorName = "Волков А.Н.",
                    capturedAtText = "10.11.2026 13:05",
                    gpsCoords = "55.7557° N, 37.6172° E",
                    comment = "Щиты выставлены, подкосы раскреплены"
                ).encode()
            ),
            commentItems = listOf(
                TaskCommentItem(
                    id = "c4",
                    authorId = "emp_itr_1",
                    authorName = "Соколов А.В.",
                    authorRole = "ITR",
                    text = "@Волков А.Н. Отклонено: на нижнем поясе отсутствуют дублирующие шайбы и контргайки. Установить и приложить крупное фото узла!",
                    mentionedUser = "Волков А.Н.",
                    createdAtText = "13:30",
                    isRead = false
                ).encode()
            ),
            historyItems = listOf(
                StatusHistoryEntry("09.11 08:30", "Соколов А.В.", "NEW", "Создана задача").encode(),
                StatusHistoryEntry("09.11 23:59", "Система", "IN_PROGRESS", "Автоперенос на 10.11 (перенос №1)").encode(),
                StatusHistoryEntry("10.11 13:10", "Волков А.Н.", "ON_REVIEW", "Отправлено на проверку").encode(),
                StatusHistoryEntry("10.11 13:30", "Соколов А.В.", "REJECTED", "Отклонено: нет контргаек на нижнем поясе тяжей").encode()
            ),
            rejectionReason = "На нижнем поясе отсутствуют дублирующие шайбы и контргайки. Установить и приложить крупное фото узла.",
            geoLocation = "Секция А, ось 5/Б"
        ),

        // 5. OVERDUE task with 2 automatic carryovers
        TaskDocument(
            id = "seed_task_5",
            userId = userId,
            title = "Алмазное бурение гильз под транзитные воздуховоды ДУ250",
            description = "Пробурить 6 отверстий в стене венткамеры по согласованной схеме от инженера ПТО.",
            creatorId = "emp_itr_2",
            creatorName = "Ермакова Елена Игоревна",
            assigneeId = "emp_worker_4",
            assigneeName = "Белов Максим Денисович",
            groupId = "grp_mep",
            groupName = "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
            siteId = DEFAULT_SITE_ID,
            priority = TaskPriority.MEDIUM.code,
            status = TaskStatus.OVERDUE.code,
            dueDateEpochDay = BASE_EPOCH_DAY,
            originalDueDateEpochDay = BASE_EPOCH_DAY - 2L,
            deadlineTime = "15:00",
            carryoverCount = 2L,
            requiresPhotoReport = true,
            minPhotoCount = 1L,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.NONE.code,
            tags = listOf("ОВиК", "Просрочено", "АлмазноеБурение"),
            subtaskItems = listOf(
                SubtaskItem("s51", "Согласование привязки со службой КЖ", true).encode(),
                SubtaskItem("s52", "Бурение отверстий 1–6 и установка стальных гильз", false).encode()
            ),
            photoItems = emptyList(),
            commentItems = emptyList(),
            historyItems = listOf(
                StatusHistoryEntry("08.11 09:00", "Ермакова Е.И.", "NEW", "Первичная постановка").encode(),
                StatusHistoryEntry("08.11 23:59", "Система", "OVERDUE", "Автоперенос на 09.11 (перенос №1)").encode(),
                StatusHistoryEntry("09.11 23:59", "Система", "OVERDUE", "Автоперенос на 10.11 (перенос №2)").encode()
            ),
            geoLocation = "Техэтаж, венткамера ВК-1"
        ),

        // 6. DAILY RECURRING task for ITR
        TaskDocument(
            id = "seed_task_6",
            userId = userId,
            title = "Ежедневный утренний обход опасных зон, лесов и проверка нарядов-допусков",
            description = "Проверить закрытие шахт, заземление крановых путей, подписи в журнале целевого инструктажа на высоте.",
            creatorId = "emp_head_1",
            creatorName = "Ковалёв Дмитрий Сергеевич",
            assigneeId = "emp_itr_1",
            assigneeName = "Соколов Андрей Викторович",
            groupId = "grp_monolith",
            groupName = "Бригада №1 • Монолит и ЖБК",
            siteId = DEFAULT_SITE_ID,
            priority = TaskPriority.CRITICAL.code,
            status = TaskStatus.IN_PROGRESS.code,
            dueDateEpochDay = BASE_EPOCH_DAY,
            originalDueDateEpochDay = BASE_EPOCH_DAY,
            deadlineTime = "17:00",
            carryoverCount = 0L,
            requiresPhotoReport = true,
            minPhotoCount = 1L,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.DAILY.code,
            tags = listOf("ОхранаТруда", "Ежедневно", "НарядДопуск"),
            subtaskItems = listOf(
                SubtaskItem("s61", "Проверка ограждений горизонтальных проёмов", true).encode(),
                SubtaskItem("s62", "Проверка журнала инструктажа бригады №1", false).encode()
            ),
            photoItems = listOf(
                PhotoReportItem(
                    id = "ph_seed_6",
                    uriString = "stamp://Ограждение шахты лифта ЛШ-1 | Инвентарные щиты ГОСТ",
                    authorId = "emp_itr_1",
                    authorName = "Соколов А.В.",
                    capturedAtText = "10.11.2026 08:40",
                    gpsCoords = "55.7558° N, 37.6174° E",
                    comment = "Ограждения установлены в полном объёме, сигнальное освещение работает"
                ).encode()
            ),
            commentItems = emptyList(),
            historyItems = listOf(
                StatusHistoryEntry("08:00", "Система регламентов", "NEW", "Создан экземпляр ежедневной задачи").encode(),
                StatusHistoryEntry("08:35", "Соколов А.В.", "IN_PROGRESS", "Обход начат").encode()
            ),
            geoLocation = "Весь периметр участка №1"
        ),

        // 7. NEXT DAY PLAN task #1 (dueDateEpochDay = BASE_EPOCH_DAY + 1)
        TaskDocument(
            id = "seed_task_7",
            userId = userId,
            title = "Приёмка товарного бетона В25 F150 W8 на плиту отм. +12.600 (объём 94 м³)",
            description = "Обеспечить работу двух глубинных вибраторов, отбор контрольных кубиков в лабораторию и уход за бетоном.",
            creatorId = "emp_head_1",
            creatorName = "Ковалёв Дмитрий Сергеевич",
            assigneeId = "emp_worker_1",
            assigneeName = "Морозов Илья Павлович",
            groupId = "grp_monolith",
            groupName = "Бригада №1 • Монолит и ЖБК",
            siteId = DEFAULT_SITE_ID,
            priority = TaskPriority.CRITICAL.code,
            status = TaskStatus.NEW.code,
            dueDateEpochDay = BASE_EPOCH_DAY + 1L,
            originalDueDateEpochDay = BASE_EPOCH_DAY + 1L,
            deadlineTime = "19:00",
            carryoverCount = 0L,
            requiresPhotoReport = true,
            minPhotoCount = 3L,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.NONE.code,
            tags = listOf("ПланНаЗавтра", "Бетонирование", "Лаборатория"),
            subtaskItems = listOf(
                SubtaskItem("s71", "Проверить готовность автобетононасоса 42 м", false).encode(),
                SubtaskItem("s72", "Отобрать 2 серии контрольных образцов-кубов", false).encode(),
                SubtaskItem("s73", "Укрытие свежеуложенного бетона плёнкой и тепляками", false).encode()
            ),
            photoItems = emptyList(),
            commentItems = emptyList(),
            historyItems = listOf(
                StatusHistoryEntry("10.11 15:00", "Ковалёв Д.С.", "NEW", "Запланировано в суточно-месячный график").encode()
            ),
            geoLocation = "Секция А, отм. +12.600"
        ),

        // 8. NEXT DAY PLAN task #2 (dueDateEpochDay = BASE_EPOCH_DAY + 1)
        TaskDocument(
            id = "seed_task_8",
            userId = userId,
            title = "Расключение распределительного щита ЩО-3 и замер петли «фаза-нуль»",
            description = "Подключение отходящих групп освещения 3 этажа, маркировка автоматов и протокол замеров.",
            creatorId = "emp_itr_2",
            creatorName = "Ермакова Елена Игоревна",
            assigneeId = "emp_worker_3",
            assigneeName = "Кузнецов Роман Олегович",
            groupId = "grp_mep",
            groupName = "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
            siteId = DEFAULT_SITE_ID,
            priority = TaskPriority.HIGH.code,
            status = TaskStatus.NEW.code,
            dueDateEpochDay = BASE_EPOCH_DAY + 1L,
            originalDueDateEpochDay = BASE_EPOCH_DAY + 1L,
            deadlineTime = "17:30",
            carryoverCount = 0L,
            requiresPhotoReport = true,
            minPhotoCount = 2L,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.NONE.code,
            tags = listOf("ПланНаЗавтра", "ЭОМ", "ЩО3"),
            subtaskItems = listOf(
                SubtaskItem("s81", "Опрессовка наконечников НШВИ", false).encode(),
                SubtaskItem("s82", "Наклейка однолинейной схемы на дверь щита", false).encode()
            ),
            photoItems = emptyList(),
            commentItems = emptyList(),
            historyItems = listOf(
                StatusHistoryEntry("10.11 16:00", "Ермакова Е.И.", "NEW", "Включено в план работ на следующий день").encode()
            ),
            geoLocation = "Корпус 1, 3 этаж, ниша ЭОМ"
        )
    )

    fun buildInitialReportTemplates(
        userId: String,
        dateLabel: String = "10.11.2026",
        orgName: String = DEFAULT_ORG_NAME,
        siteName: String = DEFAULT_SITE_NAME
    ): List<ReportTemplateDocument> = listOf(
        ReportTemplateDocument(
            id = "tpl_rep_daily_split",
            userId = userId,
            name = "Отчет за текущий день ($dateLabel)",
            reportType = ReportType.DAILY_REPORT.code,
            sheetLayout = SheetLayout.SPLIT_SHEETS.code,
            exportFormat = ExportFormat.PDF.code,
            orgHeader = "$orgName • $siteName",
            defaultPeriodDays = 1L,
            columns = listOf("ФИО исполнителя", "Бригада", "Задача", "Статус", "Срок", "Фото", "Переносы"),
            groupByField = "ASSIGNEE",
            includeSignatures = true
        ),
        ReportTemplateDocument(
            id = "tpl_rep_next_day",
            userId = userId,
            name = "План-наряд работ на следующий день",
            reportType = ReportType.NEXT_DAY_PLAN.code,
            sheetLayout = SheetLayout.SINGLE_SHEET.code,
            exportFormat = ExportFormat.XLSX.code,
            orgHeader = "ООО «ГлавСтройИнжиниринг» • Наряд-задание по участку №1",
            defaultPeriodDays = 1L,
            columns = listOf("ФИО исполнителя", "Бригада", "Задача", "Приоритет", "Дедлайн", "Мин. фото"),
            groupByField = "GROUP",
            includeSignatures = true
        ),
        ReportTemplateDocument(
            id = "tpl_rep_period",
            userId = userId,
            name = "Сводный отчёт по персоналу за период",
            reportType = ReportType.PERIOD_REPORT.code,
            sheetLayout = SheetLayout.SPLIT_SHEETS.code,
            exportFormat = ExportFormat.DOCX.code,
            orgHeader = "ООО «ГлавСтройИнжиниринг» • Исполнительная ведомость за период",
            defaultPeriodDays = 7L,
            columns = listOf("ФИО исполнителя", "Бригада", "Задача", "Статус", "Фото", "Проверил"),
            groupByField = "ASSIGNEE",
            includeSignatures = true
        ),
        ReportTemplateDocument(
            id = "tpl_rep_photo_qc",
            userId = userId,
            name = "Контроль качества фотоотчётов",
            reportType = ReportType.PHOTO_QUALITY.code,
            sheetLayout = SheetLayout.SINGLE_SHEET.code,
            exportFormat = ExportFormat.PDF.code,
            orgHeader = "Служба строительного контроля • Реестр фотофиксации",
            defaultPeriodDays = 7L,
            columns = listOf("Задача", "ФИО исполнителя", "Кол-во фото", "Геометка", "Статус проверки"),
            groupByField = "STATUS",
            includeSignatures = true
        ),
        ReportTemplateDocument(
            id = "tpl_rep_overdue",
            userId = userId,
            name = "Реестр просрочек и автоматических переносов",
            reportType = ReportType.OVERDUE_CARRYOVER.code,
            sheetLayout = SheetLayout.SINGLE_SHEET.code,
            exportFormat = ExportFormat.CSV.code,
            orgHeader = "Аналитика соблюдения графиков производства работ",
            defaultPeriodDays = 14L,
            columns = listOf("Задача", "ФИО исполнителя", "Исходный срок", "Текущий срок", "Переносов", "Причина"),
            groupByField = "ASSIGNEE",
            includeSignatures = true
        ),
        ReportTemplateDocument(
            id = "tpl_rep_discipline",
            userId = userId,
            name = "Рейтинг исполнительской дисциплины и SLA ИТР",
            reportType = ReportType.DISCIPLINE_RATING.code,
            sheetLayout = SheetLayout.SINGLE_SHEET.code,
            exportFormat = ExportFormat.XLSX.code,
            orgHeader = "КПИ и исполнительская дисциплина участка №1",
            defaultPeriodDays = 30L,
            columns = listOf("ФИО исполнителя", "Роль / Бригада", "Назначено", "Подтверждено", "Отклонено", "Рейтинг %"),
            groupByField = "GROUP",
            includeSignatures = true
        )
    )

    fun buildInitialAuditLogs(userId: String): List<AuditLogDocument> = listOf(
        AuditLogDocument(
            id = "audit_seed_1",
            userId = userId,
            actorId = "emp_head_1",
            actorName = "Ковалёв Дмитрий Сергеевич",
            actorRole = CorporateRole.HEAD.code,
            actionType = "APPROVE_REPORT",
            targetType = "TASK",
            targetId = "seed_task_3",
            details = "Подтверждён фотоотчёт «Гидравлические испытания стояков Т1/Т2» (исполнитель: Белов М.Д.)"
        ),
        AuditLogDocument(
            id = "audit_seed_2",
            userId = userId,
            actorId = "emp_itr_1",
            actorName = "Соколов Андрей Викторович",
            actorRole = CorporateRole.ITR.code,
            actionType = "REJECT_REPORT",
            targetType = "TASK",
            targetId = "seed_task_4",
            details = "Отклонён отчёт «Монтаж щитовой опалубки ДЖ-3» (причина: нет контргаек на нижнем поясе тяжей)"
        ),
        AuditLogDocument(
            id = "audit_seed_3",
            userId = userId,
            actorId = "emp_head_1",
            actorName = "Ковалёв Дмитрий Сергеевич",
            actorRole = CorporateRole.HEAD.code,
            actionType = "AUTO_CARRYOVER",
            targetType = "SYSTEM",
            targetId = "seed_task_5",
            details = "Автоматический перенос невыполненных задач на следующий день с сохранением originalDueDate"
        )
    )

    private val legacyFakeMemberIds = setOf(
        "emp_head_1",
        "emp_itr_1",
        "emp_itr_2",
        "emp_worker_1",
        "emp_worker_2",
        "emp_worker_3",
        "emp_worker_4",
        "pending_emp_1",
        "pending_emp_2"
    )

    fun isLegacyFakeSeedMemberId(id: String): Boolean {
        val clean = id.trim()
        return clean in legacyFakeMemberIds || clean.startsWith("pending_emp_")
    }

    fun isLegacyFakeSeedTaskId(id: String): Boolean {
        val clean = id.trim()
        return clean.startsWith("seed_task_") || clean.contains("_seed_task_")
    }

    fun buildInitialSiteMembers(): List<com.example.data.model.SiteMemberDocument> = emptyList()
}
