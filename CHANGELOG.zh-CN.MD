# 变更日志

## [1.11.1] - 2024-03-12

### 重大变更

* 由于 E 站强制要求人机验证，仅支持通过网页登录
* 默认支持 Android 6，不再提供 Marshmallow 变种
* 集成 Cronet 库，不再提供 GMS 变种

### 改进

* 支持在排序下载列表前按下载标签分组

### Bug 修复

* 下载画廊时崩溃 [#893](https://github.com/FooIbar/EhViewer/issues/893)


## [1.11.0] - 2024-03-09

### 重大变更

* 由于 E 站 Cookie 策略变更，不再支持 Cookie 登录
* 登录时自动选择 exhentai （如果可用）
* 不再提供默认下载路径
* 手动排序更改为以添加时间/上传时间/标题/下载标签/页数对下载列表排序
* 由于数据库结构变更，此版本导出的数据将无法被旧版本导入

### 新功能

* 下载画廊时生成 ComicInfo.xml 格式元数据
* 归档下载时生成 ComicInfo.xml 格式元数据 [#711](https://github.com/FooIbar/EhViewer/issues/711)
* 归档下载支持第三方下载管理器 [#79](https://github.com/FooIbar/EhViewer/issues/79)
* 保存下载结果为 CBZ 压缩包 [#660](https://github.com/FooIbar/EhViewer/issues/660)
* 清除 `igneous` Cookie （将于下次搜索时重新获取）
* 随机打开本地收藏/下载中的画廊 [#695](https://github.com/FooIbar/EhViewer/issues/695)
* 随机在搜索结果中跳转
* 以网格模式查看下载列表
* 本地收藏/历史/下载中的标签搜索 [#860](https://github.com/FooIbar/EhViewer/issues/860)

### 改进

* 搜索时优先显示已选择的类别卡片
* 多处性能优化

### Bug 修复

* 显示模式为 Thumbnail 时无法点击上传者卡片 [#653](https://github.com/FooIbar/EhViewer/issues/653)
* [Marshmallow] GIF 只显示第一帧
* 无法在 `定时请求新闻页面` 关闭时开启 `隐藏 HV 事件通知`
* 响应正文含有无效 UTF-8 字符导致解码失败 [#643](https://github.com/FooIbar/EhViewer/issues/643)
* 某些情况下应用崩溃 [#710](https://github.com/FooIbar/EhViewer/issues/710)
* 画廊名称包含非法路径字符时无法保存图片 [#815](https://github.com/FooIbar/EhViewer/issues/815)
* 无法解码超长图片 [#732](https://github.com/FooIbar/EhViewer/issues/732)
* E 站变更导致种子解析失败 [#880](https://github.com/FooIbar/EhViewer/issues/880)
* E 站不再支持相似扫描导致图片搜索无法使用
* 杂项修复

### 已知问题

* Android 14 上使用手势导航返回时过渡动画过早结束 [#775](https://github.com/FooIbar/EhViewer/issues/775)


## [1.10.3] - 2024-01-15

### 新功能

* [Marshmallow] 支持裁剪边缘、打开压缩包和 GIF

### 改进

* 由于部分设备上的非预期行为 [#578](https://github.com/FooIbar/EhViewer/issues/578) [#596](https://github.com/FooIbar/EhViewer/issues/596)，默认禁用预见式返回导航动画，可于 设置-高级 中开启
* 杂项优化

### Bug 修复

* 刷新后页面无响应 [#579](https://github.com/FooIbar/EhViewer/issues/579)
* 浏览中保存图片后点击操作被阻挡 [#581](https://github.com/FooIbar/EhViewer/issues/581)
* 在画廊详情页面切换至后台后点击评论闪退 [#580](https://github.com/FooIbar/EhViewer/issues/580)
* 已下载的画廊中未阅读缓存过的图片解码失败 [#592](https://github.com/FooIbar/EhViewer/issues/592)
* 在收藏页多选并下载会使得下载状态出现问题 [#603](https://github.com/FooIbar/EhViewer/issues/603)
* 未能在不重启情况下切换 Http 引擎 [#610](https://github.com/FooIbar/EhViewer/issues/610)
* 在有进度的画廊阅读界面无法直接回到第一页 [#614](https://github.com/FooIbar/EhViewer/issues/614)
* 请求新闻界面失效 [#605](https://github.com/FooIbar/EhViewer/issues/605)
* 收藏夹为空解析失败 [#630](https://github.com/FooIbar/EhViewer/issues/630)


## [1.10.2] - 2023-12-31

### 新功能

* [阅读器] 支持裁剪图片黑色/白色边缘

### 改进

* 切换 HTTP 引擎无需重启
* 优化过渡动画
* 杂项性能优化

### Bug 修复

* 修复搜索建议中出现关键词相同的条目时崩溃 [#533](https://github.com/FooIbar/EhViewer/issues/533) [#555](https://github.com/FooIbar/EhViewer/issues/555)
* 修复重命名下载标签时不显示原标签 [#534](https://github.com/FooIbar/EhViewer/issues/534)
* 修复调整下载项目顺序时卡顿 [#552](https://github.com/FooIbar/EhViewer/issues/552)
* 修复新增或修改评论后黑屏 [#554](https://github.com/FooIbar/EhViewer/issues/554)
* 修复剪贴板读取bug [#566](https://github.com/FooIbar/EhViewer/issues/566)
* 修复画廊信息对话框无法通过返回按钮/手势关闭 [#413](https://github.com/FooIbar/EhViewer/issues/413)


## [1.10.1] - 2023-12-19

### 重大变更

* 由于 Apache HttpClient 5 在 Android 10 以下无法使用，后备 HTTP 引擎恢复为 OkHttp [#514](https://github.com/FooIbar/EhViewer/issues/514)

### Bug 修复

* 修复获取 pToken 失败导致无限加载 [#508](https://github.com/FooIbar/EhViewer/issues/508)
* 修复阅读页快速滑动滑块导致下载延时过多累计 [#512](https://github.com/FooIbar/EhViewer/issues/512)
* 修复 Android 9 以下的设备上无法退出搜索栏 [#513](https://github.com/FooIbar/EhViewer/issues/513)


## [1.10.0] - 2023-12-14 [YANKED]

### 重大变更

* 后备 HTTP 引擎变更为 Apache HttpClient 5
* 类别和高级搜索选项移至搜索栏下方

### 新功能

* 支持在搜索时指定画廊语言

### 改进

* 在下载中阅读画廊时会更新历史记录
* 多处性能改进

### Bug 修复

* 修复无法由外部应用进入画廊页面 [#436](https://github.com/FooIbar/EhViewer/issues/436)
* 修复点击精确到页的画廊链接闪退 [#430](https://github.com/FooIbar/EhViewer/issues/430)
* 修复无法搜索本地收藏 [#491](https://github.com/FooIbar/EhViewer/issues/491)
* 修复 CI 版本不显示变更日志


## [1.9.0] - 2023-12-01

### 重大变更

* 除阅读器外全部迁移至 Compose

### 新功能

* 设置-高级 中增加实验性滑动手势敏感度设置
* 支持通过滑动手势打开右侧抽屉
* 支持预见式返回动画，相关设置位于 设置-高级

### 改进

* [阅读器] 画廊页数过多时隐藏进度条刻度并禁用触感反馈
* 优化原图下载重试策略
* 优化小屏设备上的搜索类别布局
* 优化过渡动画
* 优化历史记录删除动画
* 设置-EH 中的缩略图大小设置改为缩略图列数

### Bug 修复

* 修复点击通知栏的下载通知闪退 [#175](https://github.com/FooIbar/EhViewer/issues/175)
* 修复收藏状态显示错误 [#189](https://github.com/FooIbar/EhViewer/issues/189)
* 修复双击导航按钮后无法打开导航抽屉
* 修复某些情况下闪退 [#142](https://github.com/FooIbar/EhViewer/issues/142) [#208](https://github.com/FooIbar/EhViewer/issues/208)
* 修复收藏界面点击重试崩溃 [#193](https://github.com/FooIbar/EhViewer/issues/193)
* 修复本地收藏滚动位置丢失 [#192](https://github.com/FooIbar/EhViewer/issues/192)
* 修复手速过快页面切换过快使加载圈圈不自动消失 [#209](https://github.com/FooIbar/EhViewer/issues/209)
* 修复打开并关闭右侧抽屉后，状态栏一片白色 [#101](https://github.com/FooIbar/EhViewer/issues/101)
* 修复使用深色主题时，切换界面会闪过亮色界面 [#121](https://github.com/FooIbar/EhViewer/issues/121)
* [Marshmallow] 修复小内存设备上图片解码失败 [#229](https://github.com/FooIbar/EhViewer/issues/229) [#372](https://github.com/FooIbar/EhViewer/issues/372)
* 修复已舍弃画廊上传者解析

### 已知问题

* 画廊信息对话框无法通过返回关闭 [#413](https://github.com/FooIbar/EhViewer/issues/413)
