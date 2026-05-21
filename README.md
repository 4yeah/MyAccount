# 记账笔记 App

本地记账与笔记管理应用，采用多模块架构。

## 技术栈

- UI：Jetpack Compose + Material3 + Navigation Compose
- 数据：Room + Kotlin Coroutines/Flow
- 架构：MVVM + 多模块 + 手动依赖注入
- 构建：Gradle Kotlin DSL + Version Catalogs + KSP

## 模块架构

```
app/                  应用入口、导航、底部栏
core/
  common/             通用工具（图标、主题配置、颜色解析）
  database/           Room 数据库（Entity、DAO、Database）
  data/               数据模型、Repository、Preferences、备份管理
feature/
  accounting/         记账功能（首页、记一笔、分类选择器、批量操作）
  note/               笔记功能（列表、编辑）
  statistics/         统计功能（月度汇总、分类占比饼状图）
  settings/           设置功能（主题切换、预算管理、分类管理、云备份配置）
```

## 功能

- **记账**：记录收入/支出，选择一级/二级分类，查看收支汇总与历史记录，支持编辑和批量删除
- **笔记**：纯文本笔记，支持搜索、新建、编辑、删除
- **统计**：按月统计收支，查看分类占比饼状图
- **预算管理**：设置月度预算，首页显示剩余预算进度条
- **分类管理**：支持一级/二级分类的增删改查、拖拽排序，自定义图标和颜色；删除时弹窗提示关联记录并确认，受规则保护（默认一级分类不可删、每个一级分类下至少保留一个二级分类）
- **主题切换**：5 套可爱主题（默认/草莓粉/薄荷绿/蓝莓紫/奶油黄），实时切换
- **云备份**：支持阿里云 OSS 云备份与恢复（需配置 AccessKey）

## 数据模型

- `Transaction`：交易记录（金额、类型、分类、日期、备注）
- `Category`：收支分类（名称、图标、颜色）
- `Note`：笔记（标题、内容、可选关联交易）

## 构建

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## 预置分类

首次启动自动插入 10 条默认分类（6 支出 + 4 收入）。

## 变更日志

| 日期 | 变动 |
|------|------|