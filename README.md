# KuiklyCalendar

面向 Kuikly 的跨端组合式月历组件。`Calendar` 以 Kuikly 推荐的 `ViewContainer` 扩展函数作为 DSL 入口，使用 `attr {}` 配置组件状态、使用 `event {}` 接收交互回调；内部以纯 Kotlin 自然日算法保持 Android、iOS、鸿蒙与 H5 的月份网格一致。

本仓库对应 [KuiklyUI Issue #1476](https://github.com/Tencent-TDS/KuiklyUI/issues/1476)：月份展示与切换、星期和日期网格、日期选择高亮、日期/月份事件回调均已实现；在此基础上补充了范围与多选、插槽、快速年月导航、日程摘要、农历/节假日和 Agenda。

## 目录

- [为什么使用它](#为什么使用它)
- [功能一览](#功能一览)
- [接入与最小示例](#接入与最小示例)
- [Calendar DSL](#calendar-dsl)
- [选择模式与受控 API](#选择模式与受控-api)
- [日程 DSL 与远程数据](#日程-dsl-与远程数据)
- [插槽与自定义渲染](#插槽与自定义渲染)
- [农历、节假日与 Agenda](#农历节假日与-agenda)
- [样式、平台与验证](#样式平台与验证)
- [演示与 Release](#演示与-release)

## 为什么使用它

Kuikly 将可复用 UI 封装为 `ComposeView`：组件公开属性类和事件类，并由 `ViewContainer` 扩展函数提供声明式入口。本项目延续该模式：

```kotlin
fun ViewContainer<*, *>.Calendar(init: CalendarView.() -> Unit) {
    addChild(CalendarView(), init)
}
```

因此，业务不需要使用平台日期控件，也不需要在 Android/iOS/H5 分别维护日期网格、禁用校验或范围选择状态机。`CalendarDate` 明确表示无时区的自然日，不会把同一天因 UTC 转换为相邻日期。

有关 Kuikly 组合组件、`attr` 和 `event` 的设计原则，可参阅官方 [ComposeView 指南](https://kuikly.tds.qq.com/DevGuide/compose-view.html) 与 [组件概述](https://kuikly.tds.qq.com/API/components/override.html)。

## 功能一览

| 分类 | 能力 | 说明 |
| --- | --- | --- |
| 基础月历 | 月份标题、前后切换、星期表头、稳定 6×7 网格 | 固定渲染 42 格，五周/六周月份切换时不会抖动 |
| 日期选择 | 单选、范围、多选、禁用和边界 | 范围/多选上限、反向选择策略、跨禁用日保护与拒绝原因回调 |
| 月份导航 | 相邻月点击、横向滑动、年月面板、回到今天 | 可限制可选年份与年份面板步长 |
| 日程摘要 | 单日/跨日事件、彩点、数量角标、自定义标记 | 仅索引可见的 42 格；支持受控远程加载 |
| 内容扩展 | Header、Weekday、Day、Marker、Footer、空态 Slot | 自定义内容仍可复用组件自身的受控操作 |
| 本地化 | 周起始日、星期文案、农历/节假日/调休 Provider | 内置中国大陆 2026 年节假日与农历示例数据 |
| Agenda | 独立日程列表、空态和事件点击回调 | 可与 Calendar 共用 `CalendarSelection` 和事件列表 |
| 多端 | Android、iOS、鸿蒙、H5 | 核心逻辑在 `commonMain`，不依赖 `java.time` 或平台日期控件 |

## 接入与最小示例

当前组件源码位于 `KuiklyCalendar/src/commonMain`，无需额外第三方运行时依赖。将其放入任意 `Pager.body()` 的 `ViewContainer` 中即可使用。

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
            // date 是自然日；例如 date.format() == "2026-09-16"
            viewModel.selectDate(date)
        }
        monthChanged { month ->
            viewModel.loadMonth(month.year, month.month)
        }
    }
}
```

`selectedDate` 是 v1.0 单选兼容属性。需要范围或多选时，请使用 `selection` 与 `selectionMode`，详见下文。

## Calendar DSL

一个 `Calendar` 块由三层组成：组件入口、`attr` 属性、`event` 回调。属性只定义初始渲染和视觉规则；运行时状态更新请通过下文的受控 API，而不是直接修改内部字段。

```kotlin
Calendar {
    attr {
        // 日期与选择
        initialMonth = CalendarMonth(2026, 9)
        showAdjacentMonths = true
        firstDayOfWeek = CalendarWeekday.MONDAY
        weekdayLabels = listOf("日", "一", "二", "三", "四", "五", "六")

        // 导航
        swipeMonthEnabled = true
        monthPickerEnabled = true
        yearRange = 2020..2030
        yearPickerStep = 12
        showTodayAction = true

        // 外观
        cornerRadius = 16f
        dayIndicatorSize = 42f
        dayIndicatorCornerRadius = 8f
        theme = CalendarTheme(
            selectedBackgroundColor = 0xFF3D7EFF,
            rangeBackgroundColor = 0x333D7EFF
        )
    }
    event {
        dateSelected { date -> /* v1.0 单选兼容回调 */ }
        selectionChanged { selection -> /* 所有选择模式统一回调 */ }
        selectionRejected { reason, date -> /* 禁用、超限等 */ }
        monthPickerVisibilityChanged { visible -> /* 年月面板开/关 */ }
        todayActionClick { /* 用户点击“回到今天” */ }
    }
}
```

### 常用属性

| 属性 | 类型/默认值 | 用途 |
| --- | --- | --- |
| `initialMonth` | `CalendarMonth?` | 首次显示月份；默认当前月 |
| `minDate` / `maxDate` | `CalendarDate?` | 可选日期的闭区间边界 |
| `disabledDates` | `Set<CalendarDate>` | 明确禁用的日期集合 |
| `isDateSelectable` | `(CalendarDate) -> Boolean` | 业务自定义可选日期规则 |
| `showAdjacentMonths` | `false` | 是否展示本月之外的补位日期 |
| `firstDayOfWeek` | `MONDAY` | 一周的起始日 |
| `markers` | `Map<CalendarDate, CalendarMarker>` | 兼容的轻量日期标记 |
| `events` | `List<CalendarEventSummary>` 或 DSL | 日程摘要数据源 |
| `eventDisplayMode` | `DOTS` | `DOTS`、`COUNT_BADGE` 或 `CUSTOM` |
| `supplementaryProvider` | `CalendarSupplementaryProvider?` | 农历、节日、调休等补充信息来源 |
| `slots` | `CalendarSlots` | 自定义渲染插槽集合 |

### 事件回调

| 回调 | 触发时机 |
| --- | --- |
| `dateSelected(date)` | 单选模式成功选择日期时；保留以兼容 v1.0 |
| `selectionChanged(selection)` | 单选、范围或多选状态发生有效变化时 |
| `selectionRejected(reason, date)` | 日期禁用、范围跨禁用日、超出上限或选择模式关闭时 |
| `monthChanged(month)` | 通过按钮、滑动或相邻月日期成功切换月份时 |
| `visibleRangeChanged(range)` | 初始显示或 42 格可见范围变化时；相同范围自动去重 |
| `monthPickerVisibilityChanged(visible)` | 年月面板打开/关闭时 |
| `todayActionClick()` | 点击默认“回到今天”动作时 |

## 选择模式与受控 API

选择状态统一由 `CalendarSelection` 表示：

```kotlin
sealed class CalendarSelection {
    object None
    data class Single(val date: CalendarDate)
    data class Range(val start: CalendarDate, val end: CalendarDate? = null)
    data class Multiple(val dates: Set<CalendarDate>)
}
```

### 范围与多选

```kotlin
Calendar {
    attr {
        selectionMode = CalendarSelectionMode.RANGE
        selection = CalendarSelection.Range(CalendarDate(2026, 9, 10))
        maxRangeDays = 10                 // 0 表示不限制
        rangeOverflowPolicy = RangeOverflowPolicy.SWAP
        allowRangeAcrossDisabledDates = false
        disabledDates = setOf(CalendarDate(2026, 9, 15))
    }
    event {
        selectionRejected { reason, _ -> showSelectionError(reason) }
    }
}
```

- `SINGLE`：选择一个日期。
- `RANGE`：第一次点击设置起始日，第二次点击设置结束日；`RESTART` 遇到更早日期时重置起点，`SWAP` 则自动交换起止日。
- `MULTIPLE`：再次点击已选日期会取消选择；`maxSelectionCount = 0` 表示不限数量。
- `NONE`：不允许用户选择；可通过 `selectionRejected` 获取原因。

运行期更新使用 `CalendarView` 的受控方法：

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

`setSelection` 和 `setSelectionMode` 默认不派发业务回调；传入 `emitEvent = true` 可在状态同步时主动通知外部。

## 日程 DSL 与远程数据

`CalendarEventSummary` 是月历和 Agenda 共用的轻量模型：`id` 非空、`endDate` 不得早于 `startDate`。跨天事件会出现在其覆盖的每一天；业务字段可放入 `extra` 原样透传。

### 在 Calendar 内声明固定日程

```kotlin
Calendar {
    attr {
        events {
            // 单日事件：最简形式
            event("review", CalendarDate(2026, 9, 8)) {
                title = "设计评审"
                color = 0xFF8B5CF6
                extra("projectId", "calendar")
            }

            // 跨日事件：传入起止日
            event("release", CalendarDate(2026, 9, 15), CalendarDate(2026, 9, 17)) {
                title = "版本发布窗口"
                type = "release"
                color = 0xFFF97316
            }

            // 完整声明：on/during 避免漏填日期
            event {
                id = "holiday"
                on(CalendarDate(2026, 10, 1))
                title = "国庆节"
                showsCalendarIndicator = false
            }
        }
        eventDisplayMode = CalendarEventDisplayMode.DOTS
    }
}
```

当 Calendar 与 Agenda 共享固定数据时，使用顶层 `calendarEvents {}` 创建列表快照：

```kotlin
val launchEvents = calendarEvents {
    event("kickoff", CalendarDate(2026, 9, 4)) { title = "项目启动会" }
    event("release", CalendarDate(2026, 9, 15), CalendarDate(2026, 9, 17)) {
        title = "版本发布窗口"
    }
}
```

`CalendarEventsDsl` 和 `CalendarEventDsl` 标注了 `@CalendarDslMarker`，嵌套事件块时不会意外把事件字段写到外层 DSL。`add(summary)` 可混合已有模型与 DSL 声明。

### 受控远程加载

组件不内置网络、鉴权和缓存。它在可见范围变化时通知业务，业务加载后回填事件，避免多年日程在组件内一次性展开。

```kotlin
Calendar {
    attr { eventDisplayMode = CalendarEventDisplayMode.COUNT_BADGE }
    event {
        visibleRangeChanged { range ->
            viewModel.loadEvents(range.start, range.end)
        }
    }
}

// 新响应到达后；建议由业务丢弃过期 range 的响应
calendar?.setEvents(viewModel.events)

// 若直接替换了 attr.events，则显式刷新索引
calendar?.refreshEvents()
```

`DOTS` 显示低干扰彩点，`COUNT_BADGE` 显示当日数量，`CUSTOM` 不绘制默认日程视觉，交由 `slots.marker` 按 `CalendarDayState.events` 自行渲染。事件存在时优先于兼容的 `markers`；没有事件时仍会渲染 `CalendarMarker`。

## 插槽与自定义渲染

`CalendarSlots` 可替换完整区域或局部区域，同时保留日历的选择、边界和回调逻辑。

| Slot | 参数 | 典型用途 |
| --- | --- | --- |
| `header` | `CalendarHeaderState`、`CalendarActions` | 完全替换头部；优先级最高 |
| `headerTitle` / `headerLeading` / `headerTrailing` | 同上 | 仅替换默认头部的一个区域 |
| `weekday` | `CalendarWeekdayState` | 定制工作日和周末标题 |
| `dayContent` | `CalendarDayState` | 定制日期格文本、农历或业务标签 |
| `marker` | `CalendarDayState` | 定制日程/标记视觉 |
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
            footer = { state, actions ->
                Text {
                    attr { text("清除选择") }
                    event { click { actions.setSelection(CalendarSelection.None, emitEvent = true) } }
                }
            }
        }
    }
}
```

完整 `header` 优先于任一 header 子 Slot；`slots.dayContent` 优先于兼容属性 `dayContent`。Slot 应通过 `CalendarActions` 调用 `showMonth`、`showToday`、`setSelection`、`setSelectionMode`、`showMonthPicker` 或 `hideMonthPicker`，而非依赖内部渲染状态。

## 农历、节假日与 Agenda

### 补充文案 Provider

基础 Calendar 不耦合地区数据。通过 `CalendarSupplementaryProvider` 可提供农历、节气、节假日与调休信息：

```kotlin
Calendar {
    attr {
        showSupplementaryLabel = true
        supplementaryProvider = object : CalendarSupplementaryProvider {
            override fun supplementaryLabel(date: CalendarDate): String? {
                return lunarService.labelOf(date) // 例如：初一、春节、休
            }

            override fun isHoliday(date: CalendarDate): Boolean = holidayService.isHoliday(date)
            override fun isWorkdayOverride(date: CalendarDate): Boolean = holidayService.isWorkday(date)
        }
    }
}
```

仓库示例提供 `ChinaLunarCalendar` 与 `ChinaMainlandHolidayCalendar`。地区数据保持可替换，业务可接入自身服务或更多年份的数据源。

### 与 CalendarAgenda 联动

`CalendarAgenda` 是独立组合组件，专门承担日程列表、空态和点击，不把复杂列表塞入月历网格。范围和多选默认取首个自然日筛选。

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

事件更新后，同时调用 `calendar.setEvents(events)` 和 `agenda.setEvents(events)`。Agenda 还支持 `eventContent`、`emptyContent`、`titleFormatter`、`noSelectionText` 等定制入口。

## 样式、平台与验证

`CalendarTheme` 使用 ARGB `Long`，例如 `0xFF3D7EFF`。除主题外，可通过 `headerHeight`、`weekdayHeight`、`dayHeight`、`dayFontSize`、`supplementaryFontSize`、`eventBadgeSize`、`horizontalPadding` 等属性调整尺寸。常规月视图建议使用默认的圆角方形选中态与 `DOTS` 事件提示，以保留日期、农历和日程点的可读空间。

核心算法位于 `commonMain`，已覆盖闰年、世纪年、跨月/跨年、星期、可见范围、选择状态机、日程裁剪与农历数据等测试。可使用：

```powershell
.\gradlew.bat :KuiklyCalendar:compileKotlinJs --no-daemon --console=plain
.\gradlew.bat :KuiklyCalendar:compileTestKotlinJs --no-daemon --console=plain
```

Windows 环境下 iOS 目标不可执行；Android、iOS、鸿蒙与 H5 的最终视觉和交互需在各自宿主环境回归。当前全量测试的已知环境阻断及历史结果见 [测试记录](docs/issue-1476/05-测试用例与结果.md)。

## 演示与 Release

`calendar_demo` 展示单选、范围、多选、滑月、年月面板、农历、节假日、日程摘要、Slot 与 Agenda 的组合用法；具体操作见 [完整体验指南](docs/issue-1476/15-完整日历体验块操作指南.md)。

| 资源 | 位置 | 状态 |
| --- | --- | --- |
| 演示视频与封面 | [docs/assets/demo/README.md](docs/assets/demo/README.md) | 待上传 |
| Android APK | [releases/README.md](releases/README.md) | 待发布 |
| H5 静态包 | [releases/README.md](releases/README.md) | 待发布 |
| 鸿蒙 HAR/HAP | [releases/README.md](releases/README.md) | 待发布 |

更多按版本组织的接入、设计与验证资料见 [Calendar 文档索引](docs/issue-1476/README.md)。
