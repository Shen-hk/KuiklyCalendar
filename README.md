# KuiklyCalendar

基于 Kuikly `commonMain` 的跨端组合式月历示例工程。它提供稳定的 6×7 月视图、自然日选择状态、日程摘要与 Agenda，并可同时运行在 Android、iOS、鸿蒙和 H5 宿主中。

## 功能

- 单选、范围选择与多日期选择；日期边界、禁用规则及选择拒绝回调。
- 月份切换、横向手势、快速年月面板、回到今天、42 格稳定网格。
- 可组合的 Header、Weekday、Day、Marker、Footer 与空态 Slot。
- 农历/节假日等补充信息 Provider；受控日程摘要、可见范围回调与 `CalendarAgenda`。
- 简洁的日程 DSL：`events { event(...) }` 或可复用的 `calendarEvents { ... }`。

## 快速使用

```kotlin
Calendar {
    attr {
        selectionMode = CalendarSelectionMode.RANGE
        events {
            event("review", CalendarDate(2026, 9, 8)) { title = "设计评审" }
            event {
                id = "release"
                during(CalendarDate(2026, 9, 15), CalendarDate(2026, 9, 17))
                title = "版本发布窗口"
            }
        }
    }
}
```

详细接入和 API 说明请从 [文档索引](docs/issue-1476/README.md) 开始。

## 演示视频

| 内容 | 位置 | 状态 |
| --- | --- | --- |
| 完整交互演示 | [docs/assets/demo/README.md](docs/assets/demo/README.md) | 待上传 |

上传视频后，请将上表链接替换为视频地址或仓库内文件，并在该目录补充封面图。

## Release 包

| 产物 | 下载位置 | 状态 |
| --- | --- | --- |
| Android APK | [releases/README.md](releases/README.md) | 待发布 |
| H5 静态包 | [releases/README.md](releases/README.md) | 待发布 |
| 鸿蒙 HAR/HAP | [releases/README.md](releases/README.md) | 待发布 |

正式二进制请作为 GitHub/GitLab Release 附件发布，不直接提交到仓库。发布说明与文件命名规则见 [Release 占位说明](releases/README.md)。

## 验证

```powershell
.\gradlew.bat :KuiklyCalendar:allTests --no-daemon --console=plain
.\gradlew.bat :KuiklyCalendar:compileDebugKotlinAndroid --no-daemon --console=plain
```

各平台的最终交互和视觉效果仍应在对应宿主环境回归。
