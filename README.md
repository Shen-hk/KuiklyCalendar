# KuiklyCalendar

面向 Kuikly 的跨端组合式月历组件。它用 `ViewContainer` 扩展函数提供 `Calendar { attr {} event {} }` DSL，内部用纯 Kotlin 自然日算法生成稳定的月份网格，避免 Android、iOS、鸿蒙和 H5 因平台日期控件或时区处理产生不一致。

本仓库对应 [KuiklyUI Issue #1476](https://github.com/Tencent-TDS/KuiklyUI/issues/1476)。基础月历、月份切换、日期选择和事件回调已完成，并继续扩展了范围/多选、插槽、年月面板、日程摘要、农历/节假日和 Agenda 列表。

## 特性

| 能力 | 说明 |
| --- | --- |
| 稳定月视图 | 固定 6 x 7 网格，月份切换时布局不抖动 |
| 跨端一致 | 日期算法在 `commonMain`，不依赖 `java.time` 或平台日期控件 |
| 选择完整 | 支持单选、范围、多选、禁用日期、边界和拒绝原因回调 |
| 导航灵活 | 支持前后月、相邻月日期跳转、横向滑月、年月面板、回到今天 |
| 日程摘要 | 支持单日/跨日事件、彩点、数量角标、自定义标记和远程回填 |
| 可组合 UI | Header、Weekday、Day、Marker、Footer、空态均可通过 Slot 替换 |
| 本地化扩展 | 可接入农历、节日、调休 Provider；仓库提供中国大陆 2026 示例数据 |
| Agenda 联动 | 独立 `CalendarAgenda` 组件展示所选日期的日程列表 |
| 演示与发布 | 已接入演示视频入口，并提供多端 Release 包清单 |

## 演示视频

Calendar 综合体验演示视频已放在 [docs/assets/demo/kuikly-calendar-demo.mp4](docs/assets/demo/kuikly-calendar-demo.mp4)。该视频用于展示单选、范围/多选、滑月、年月面板、农历/节假日、日程摘要、Slot 与 Agenda 组合能力。

更多演示素材说明见 [docs/assets/demo/README.md](docs/assets/demo/README.md)。

## 项目结构

```text
KuiklyCalendar/
├── KuiklyCalendar/                 # KMM 共享模块，Calendar 组件源码与测试
│   ├── src/commonMain/
│   │   ├── kotlin/com/kuikly/kuiklycalendar/
│   │   │   ├── calendar/            # Calendar、Agenda、日期模型、农历与节假日实现
│   │   │   ├── base/                # Demo 页基础封装与桥接工具
│   │   │   ├── CalendarDemoPage.kt  # 组件综合演示页
│   │   │   └── RouterPage.kt        # Demo 路由入口
│   │   └── assets/                  # commonMain 共享资源
│   └── src/commonTest/              # 日期算法、选择状态、日程索引等测试
├── androidApp/                      # Android 宿主工程
├── iosApp/                          # iOS 宿主工程与 CocoaPods 配置
├── h5App/                           # H5 宿主工程
├── ohosApp/                         # 鸿蒙宿主工程
├── docs/
│   ├── issue-1476/                  # 需求、设计、使用、验证和版本文档
│   └── assets/demo/                 # 演示素材占位
├── releases/                        # APK、H5 包、HAR/HAP 等发布产物占位
├── buildSrc/                        # Gradle 版本与构建辅助配置
├── gradle/                          # Gradle Wrapper 文件
├── build.gradle.kts                 # 根 Gradle 配置
├── settings.gradle.kts              # Android/iOS/H5 模块配置
├── build.ohos.gradle.kts            # 鸿蒙构建配置
├── settings.ohos.gradle.kts         # 鸿蒙模块配置
└── package.json                     # H5/鸿蒙相关脚本与前端依赖入口
```

最常看的代码集中在 [calendar](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar) 目录：

| 文件 | 作用 |
| --- | --- |
| [CalendarView.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/CalendarView.kt) | 月历组件、`Calendar {}` DSL 入口、属性和事件定义 |
| [CalendarModels.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/CalendarModels.kt) | 日期模型、选择状态、日程模型、事件 DSL、日期算法 |
| [CalendarAgendaView.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/CalendarAgendaView.kt) | Agenda 日程列表组件与 `CalendarAgenda {}` DSL 入口 |
| [ChinaLunarCalendar.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/ChinaLunarCalendar.kt) | 中国农历示例 Provider |
| [ChinaMainlandHolidayCalendar.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/ChinaMainlandHolidayCalendar.kt) | 中国大陆节假日和调休示例数据 |

## 快速开始

组件源码位于 [KuiklyCalendar/src/commonMain](KuiklyCalendar/src/commonMain)，可直接放入任意 Kuikly `Pager.body()` 的 `ViewContainer` 中。

```kotlin
import com.kuikly.kuiklycalendar.calendar.Calendar
import com.kuikly.kuiklycalendar.calendar.CalendarDate
import com.kuikly.kuiklycalendar.calendar.CalendarMonth

Calendar {
    attr {
        initialMonth = CalendarMonth(2026, 9)
        selectedDate = CalendarDate(2026, 9, 16)
        minDate = CalendarDate(2026, 9, 1)
        maxDate = CalendarDate(2026, 12, 31)
    }
    event {
        dateSelected { date ->
            viewModel.selectDate(date.format())
        }
        monthChanged { month ->
            viewModel.loadMonth(month.year, month.month)
        }
    }
}
```

`CalendarDate` 表示无时区的自然日，`format()` 输出 `yyyy-MM-dd`。`selectedDate` 保留为单选兼容入口；范围和多选建议使用统一的 `selection` API。

## DSL 实现位置

项目里已经实现了 Kuikly 风格 DSL，不只是 README 示例。核心代码在：

| DSL | 文件 |
| --- | --- |
| `Calendar {}` 组件入口 | [CalendarView.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/CalendarView.kt) |
| `attr {}` 属性承载 | [CalendarView.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/CalendarView.kt) |
| `event {}` 回调承载 | [CalendarView.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/CalendarView.kt) |
| `events {}` 日程声明 DSL | [CalendarModels.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/CalendarModels.kt) |
| `CalendarAgenda {}` 组件入口 | [CalendarAgendaView.kt](KuiklyCalendar/src/commonMain/kotlin/com/kuikly/kuiklycalendar/calendar/CalendarAgendaView.kt) |

组件入口是 `ViewContainer` 扩展函数，业务侧才能直接写 `Calendar { ... }`：

```kotlin
fun ViewContainer<*, *>.Calendar(init: CalendarView.() -> Unit) {
    addChild(CalendarView(), init)
}
```

`attr {}` 和 `event {}` 来自 Kuikly `ComposeView<CalendarAttr, CalendarEvent>` 的组合组件模型。项目里通过 `CalendarAttr` 暴露可配置属性，通过 `CalendarEvent` 暴露回调注册方法。核心结构如下：

```kotlin
class CalendarAttr : ComposeAttr() {
    var initialMonth: CalendarMonth? = null
    var selection: CalendarSelection? = null
    var selectionMode: CalendarSelectionMode = CalendarSelectionMode.SINGLE
    var events: List<CalendarEventSummary> = emptyList()

    fun events(init: CalendarEventsDsl.() -> Unit) {
        events = calendarEvents(init)
    }
}

class CalendarEvent : ComposeEvent() {
    private var dateSelectedListener: ((CalendarDate) -> Unit)? = null
    private var monthChangedListener: ((CalendarMonth) -> Unit)? = null
    private var selectionChangedListener: ((CalendarSelection) -> Unit)? = null
    private var visibleRangeChangedListener: ((CalendarVisibleRange) -> Unit)? = null

    fun dateSelected(listener: (CalendarDate) -> Unit) {
        dateSelectedListener = listener
    }

    fun monthChanged(listener: (CalendarMonth) -> Unit) {
        monthChangedListener = listener
    }

    fun selectionChanged(listener: (CalendarSelection) -> Unit) {
        selectionChangedListener = listener
    }

    fun visibleRangeChanged(listener: (CalendarVisibleRange) -> Unit) {
        visibleRangeChangedListener = listener
    }
}
```

日程 DSL 也在项目里单独实现了 `@DslMarker`，既可写在 `Calendar.attr.events {}` 内，也可用顶层 `calendarEvents {}` 先创建列表快照：

```kotlin
@DslMarker
annotation class CalendarDslMarker

fun calendarEvents(init: CalendarEventsDsl.() -> Unit): List<CalendarEventSummary> =
    CalendarEventsDsl().apply(init).build()

@CalendarDslMarker
class CalendarEventsDsl {
    private val items = mutableListOf<CalendarEventSummary>()

    fun event(
        id: String,
        startDate: CalendarDate,
        endDate: CalendarDate = startDate,
        init: CalendarEventDsl.() -> Unit = {}
    ) {
        items += CalendarEventDsl(id, startDate, endDate).apply(init).build()
    }

    fun build(): List<CalendarEventSummary> = items.toList()
}
```

## 基础 DSL

`Calendar` 使用 Kuikly 推荐的组合组件模式：属性集中写在 `attr {}`，交互回调集中写在 `event {}`。

```kotlin
Calendar {
    attr {
        initialMonth = CalendarMonth(2026, 9)
        showAdjacentMonths = true
        firstDayOfWeek = CalendarWeekday.MONDAY
        weekdayLabels = listOf("日", "一", "二", "三", "四", "五", "六")

        swipeMonthEnabled = true
        monthPickerEnabled = true
        yearRange = 2020..2030
        yearPickerStep = 12
        showTodayAction = true

        theme = CalendarTheme(
            selectedBackgroundColor = 0xFF3D7EFF,
            rangeBackgroundColor = 0x333D7EFF
        )
    }
    event {
        dateSelected { date -> /* 单选兼容回调 */ }
        selectionChanged { selection -> /* 单选、范围、多选统一回调 */ }
        selectionRejected { reason, date -> /* 禁用、超限等拒绝原因 */ }
        monthChanged { month -> /* 月份改变 */ }
        visibleRangeChanged { range -> /* 当前 42 格可见范围改变 */ }
    }
}
```

常用属性：

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `initialMonth` | 当前月 | 首次显示的月份 |
| `selectedDate` | `null` | v1.0 单选兼容属性 |
| `selection` | `null` | 统一选择状态；非空时优先于 `selectedDate` |
| `selectionMode` | `SINGLE` | `NONE`、`SINGLE`、`RANGE`、`MULTIPLE` |
| `minDate` / `maxDate` | `null` | 可选日期闭区间 |
| `disabledDates` | 空集合 | 明确禁用的日期 |
| `isDateSelectable` | `null` | 业务自定义可选规则 |
| `showAdjacentMonths` | `false` | 是否显示本月外补位日期 |
| `events` | 空列表 | 日程摘要数据 |
| `eventDisplayMode` | `DOTS` | `DOTS`、`COUNT_BADGE`、`CUSTOM` |
| `supplementaryProvider` | `null` | 农历、节假日、调休等补充信息 |
| `slots` | 空插槽 | 自定义 Header、日期格、标记、Footer 等 |

常用回调：

| 回调 | 触发时机 |
| --- | --- |
| `dateSelected(date)` | 单选模式成功选择日期 |
| `selectionChanged(selection)` | 任意选择模式发生有效变化 |
| `selectionRejected(reason, date)` | 日期不可选、范围跨禁用日、超出上限或关闭选择 |
| `monthChanged(month)` | 按钮、滑动、年月面板或相邻月日期导致月份改变 |
| `visibleRangeChanged(range)` | 首次显示或 42 格可见范围变化；相同范围自动去重 |
| `monthPickerVisibilityChanged(visible)` | 年月面板打开或关闭 |
| `todayActionClick()` | 点击默认“回到今天”动作 |

## 选择模式

选择状态统一由 `CalendarSelection` 表示。

```kotlin
sealed class CalendarSelection {
    object None
    data class Single(val date: CalendarDate)
    data class Range(val start: CalendarDate, val end: CalendarDate? = null)
    data class Multiple(val dates: Set<CalendarDate>)
}
```

范围选择示例：

```kotlin
Calendar {
    attr {
        selectionMode = CalendarSelectionMode.RANGE
        selection = CalendarSelection.Range(CalendarDate(2026, 9, 10))
        maxRangeDays = 10
        rangeOverflowPolicy = RangeOverflowPolicy.SWAP
        allowRangeAcrossDisabledDates = false
        disabledDates = setOf(CalendarDate(2026, 9, 15))
    }
    event {
        selectionChanged { selection -> viewModel.updateSelection(selection) }
        selectionRejected { reason, _ -> showSelectionError(reason) }
    }
}
```

多选示例：

```kotlin
Calendar {
    attr {
        selectionMode = CalendarSelectionMode.MULTIPLE
        maxSelectionCount = 5
    }
}
```

运行期可通过 `CalendarView` 进行受控同步。

```kotlin
private var calendar: CalendarView? = null

Calendar {
    calendar = this
    attr { selectionMode = CalendarSelectionMode.MULTIPLE }
}

calendar?.showMonth(CalendarMonth(2026, 10))
calendar?.showToday()
calendar?.setSelection(CalendarSelection.Multiple(setOf(CalendarDate(2026, 10, 1))))
calendar?.setSelectionMode(CalendarSelectionMode.SINGLE)
calendar?.clearSelection()
```

`setSelection`、`setSelectionMode` 和 `clearSelection` 默认不派发业务回调；需要同步通知外部时传入 `emitEvent = true`。

## 日程与远程数据

`CalendarEventSummary` 是月历和 Agenda 共用的轻量模型。`id` 必须非空，`endDate` 不能早于 `startDate`；跨日事件会出现在覆盖范围内的每一天。

```kotlin
val launchEvents = calendarEvents {
    event("review", CalendarDate(2026, 9, 8)) {
        title = "设计评审"
        color = 0xFF8B5CF6
        extra("projectId", "calendar")
    }

    event("release", CalendarDate(2026, 9, 15), CalendarDate(2026, 9, 17)) {
        title = "版本发布窗口"
        type = "release"
        color = 0xFFF97316
    }
}

Calendar {
    attr {
        events = launchEvents
        eventDisplayMode = CalendarEventDisplayMode.DOTS
    }
}
```

组件不内置网络、鉴权和缓存。推荐在 `visibleRangeChanged` 中加载当前可见 42 格范围的数据，再通过 `setEvents()` 回填。

```kotlin
Calendar {
    attr { eventDisplayMode = CalendarEventDisplayMode.COUNT_BADGE }
    event {
        visibleRangeChanged { range ->
            viewModel.loadEvents(range.start, range.end)
        }
    }
}

calendar?.setEvents(viewModel.events)
```

展示模式：

| 模式 | 行为 |
| --- | --- |
| `DOTS` | 显示低干扰彩点，适合常规月视图 |
| `COUNT_BADGE` | 显示当日事件数量 |
| `CUSTOM` | 不绘制默认日程视觉，交给 `slots.marker` 自定义 |

## 插槽

`CalendarSlots` 可替换完整区域或局部区域，同时保留组件内部的选择、边界和回调逻辑。

| Slot | 参数 | 用途 |
| --- | --- | --- |
| `header` | `CalendarHeaderState`、`CalendarActions` | 完全替换头部；优先级最高 |
| `headerTitle` / `headerLeading` / `headerTrailing` | 同上 | 替换默认头部的局部区域 |
| `weekday` | `CalendarWeekdayState` | 定制星期标题 |
| `dayContent` | `CalendarDayState` | 定制日期格内容 |
| `marker` | `CalendarDayState` | 定制日程或标记视觉 |
| `footer` | `CalendarFooterState`、`CalendarActions` | 放置已选信息、清除按钮等 |
| `emptyContent` | 无 | 当前月没有可选日期时的空态 |

```kotlin
Calendar {
    attr {
        slots = CalendarSlots().apply {
            headerTitle = { state, actions ->
                Text {
                    attr { text("${state.displayedMonth.title()} ▾") }
                    event { click { actions.showMonthPicker() } }
                }
            }
            marker = { state ->
                if (state.events.isNotEmpty()) {
                    Text { attr { text("${state.events.size} 项") } }
                }
            }
            footer = { _, actions ->
                Text {
                    attr { text("清除选择") }
                    event { click { actions.setSelection(CalendarSelection.None, emitEvent = true) } }
                }
            }
        }
    }
}
```

Slot 内建议通过 `CalendarActions` 调用 `showMonth`、`showToday`、`setSelection`、`setSelectionMode`、`showMonthPicker` 或 `hideMonthPicker`，不要依赖组件内部渲染状态。

## 农历与节假日

基础 Calendar 不绑定地区数据。业务可通过 `CalendarSupplementaryProvider` 接入农历、节气、节假日和调休信息。

```kotlin
Calendar {
    attr {
        showSupplementaryLabel = true
        supplementaryProvider = object : CalendarSupplementaryProvider {
            override fun supplementaryLabel(date: CalendarDate): String? {
                return lunarService.labelOf(date)
            }

            override fun isHoliday(date: CalendarDate): Boolean = holidayService.isHoliday(date)

            override fun isWorkdayOverride(date: CalendarDate): Boolean = holidayService.isWorkday(date)
        }
    }
}
```

仓库内置 `ChinaLunarCalendar` 与 `ChinaMainlandHolidayCalendar` 作为 2026 年中国大陆示例数据，生产环境可替换为业务自己的数据源。

## Agenda 联动

`CalendarAgenda` 是独立组合组件，负责展示所选日期的日程列表。它与 `Calendar` 松耦合，只需要同步 `selection` 和 `events`。

```kotlin
private var agenda: CalendarAgendaView? = null

Calendar {
    attr { events = launchEvents }
    event {
        selectionChanged { selection ->
            agenda?.setSelection(selection)
        }
    }
}

CalendarAgenda {
    agenda = this
    attr {
        selection = CalendarSelection.None
        events = launchEvents
        emptyText = "当天暂无日程"
    }
    event {
        eventClick { summary -> openEventDetail(summary.id, summary.extra) }
    }
}
```

事件列表更新后，同时调用 `calendar.setEvents(events)` 和 `agenda.setEvents(events)`。Agenda 还支持 `selectedDate`、`eventContent`、`emptyContent`、`titleFormatter`、`noSelectionText` 等定制入口。

## 构建与验证

常用验证命令：

```powershell
.\gradlew.bat :KuiklyCalendar:compileKotlinJs --no-daemon --console=plain
.\gradlew.bat :KuiklyCalendar:compileTestKotlinJs --no-daemon --console=plain
```

Windows 环境下 iOS 目标不可执行；Android、iOS、鸿蒙与 H5 的最终视觉和交互仍需在各自宿主环境回归。历史测试记录见 [docs/issue-1476/05-测试用例与结果.md](docs/issue-1476/05-测试用例与结果.md)。

## Release 包

多端 Release 包入口统一维护在 [releases/README.md](releases/README.md)。

| 平台 | 建议产物 | 状态 |
| --- | --- | --- |
| Android | `kuikly-calendar-<version>.apk` | 待发布 |
| H5 | `kuikly-calendar-<version>-h5.zip` | 待发布 |
| 鸿蒙 | `kuikly-calendar-<version>.har` / `.hap` | 待发布 |
| iOS | 按分发渠道提供 | 待发布 |

## 文档与演示

| 资源 | 位置 |
| --- | --- |
| 使用文档 | [docs/issue-1476/07-使用文档.md](docs/issue-1476/07-使用文档.md) |
| 范围与多选 | [docs/issue-1476/12-v1.1范围与多选使用说明.md](docs/issue-1476/12-v1.1范围与多选使用说明.md) |
| 插槽与快速导航 | [docs/issue-1476/13-v1.2插槽与快速导航使用说明.md](docs/issue-1476/13-v1.2插槽与快速导航使用说明.md) |
| 日程摘要与 Agenda | [docs/issue-1476/14-v1.3日程摘要与Agenda使用说明.md](docs/issue-1476/14-v1.3日程摘要与Agenda使用说明.md) |
| 完整体验指南 | [docs/issue-1476/15-完整日历体验块操作指南.md](docs/issue-1476/15-完整日历体验块操作指南.md) |
| 文档索引 | [docs/issue-1476/README.md](docs/issue-1476/README.md) |
| 演示视频 | [docs/assets/demo/kuikly-calendar-demo.mp4](docs/assets/demo/kuikly-calendar-demo.mp4) |
| 演示素材说明 | [docs/assets/demo/README.md](docs/assets/demo/README.md) |
| Release 包清单 | [releases/README.md](releases/README.md) |
