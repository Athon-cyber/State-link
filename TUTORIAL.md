# StateLink 零基础完整教程

> 从零开始，搭建USB数据线实时监控电脑状态的安卓APP

---

## 目录

1. [你需要的硬件](#1-你需要的硬件)
2. [电脑端：安装 Python 和运行监控服务](#2-电脑端安装-python-和运行监控服务)
3. [手机端：开启开发者选项和USB调试](#3-手机端开启开发者选项和usb调试)
4. [电脑端：安装 Android Studio 并导入项目](#4-电脑端安装-android-studio-并导入项目)
5. [运行测试：手机连接电脑看效果](#5-运行测试手机连接电脑看效果)
6. [打包安装：生成APK直接装到手机](#6-打包安装生成apk直接装到手机)
7. [切换主题](#7-切换主题)
8. [故障排查](#8-故障排查)

---

## 1. 你需要的硬件

| 物品 | 说明 |
|------|------|
| 安卓手机 ×1 | 系统版本 7.0 或以上（2017年后的手机都满足） |
| USB 数据线 ×1 | **必须支持数据传输**（不是只能充电的线） |
| Windows 或 Mac 电脑 ×1 | Windows 10/11 或 macOS 12+ |

---

## 2. 电脑端：安装 Python 和运行监控服务

### 2.1 安装 Python（Windows）

1. 打开浏览器，在地址栏输入：`https://www.python.org/downloads/`
2. 页面会自动显示适合你系统的下载按钮（黄色按钮），点击下载
3. 下载完成后，双击打开安装文件（文件名类似 `python-3.12.x-amd64.exe`）
4. **重要**：安装窗口打开后，**先勾选底部的 "Add Python to PATH"（把Python加入系统路径）**
5. 然后点击上方的 "Install Now"（立即安装）
6. 等待进度条走完，出现 "Setup was successful" 即安装成功
7. 关闭安装窗口

### 2.2 安装 Python（Mac）

1. 打开浏览器，在地址栏输入：`https://www.python.org/downloads/`
2. 页面会自动显示适合你系统的下载按钮，点击下载
3. 下载完成后，双击打开 `.pkg` 安装文件
4. 一路点击"继续" → "同意" → "安装"
5. 输入你的电脑登录密码确认安装
6. 安装完成后关闭窗口

### 2.3 安装 psutil 依赖包

1. **Windows**：点击屏幕左下角"开始"按钮，输入 `cmd`，点击打开"命令提示符"
2. **Mac**：打开 Finder（访达），在"应用程序" → "实用工具" 里找到"终端"，双击打开
3. 在弹出的黑色窗口中输入以下命令，然后按回车键：
   ```
   pip install psutil
   ```
4. 等待滚动文字停下来，看到 "Successfully installed psutil" 即成功
5. 关闭这个黑色窗口

### 2.4 下载并运行 StateLink 监控服务

1. 找到本项目文件夹中的 `desktop` 文件夹
2. 双击打开 `statelink_server.py` 文件（或者右键 → 用 Python 打开）
3. 一个黑色窗口会出现，显示：
   ```
   ╔══════════════════════════════════════════╗
   ║      StateLink PC Monitor Server v1.0    ║
   ╚══════════════════════════════════════════╝
   Server listening on port 8765
   ```
4. **这个窗口不要关**，它必须一直运行着

---

### 2.5 设置开机自启动（推荐，只需配置一次）

每次开机都要手动双击运行监控服务太麻烦了。配置开机自启动后，电脑开机/登录时监控服务会自动在后台运行，一劳永逸。

#### 操作步骤（Windows 和 Mac 完全相同）

1. 找到项目文件夹中的 `desktop` 文件夹
2. 双击 `setup_autostart.py` 文件（或者右键 → 用 Python 打开）
3. 一个黑色窗口会弹出，显示检测结果
4. 看到 "Auto-start is now ENABLED!" 表示配置成功
5. **下次重启电脑时**，监控服务会自动启动

#### 验证是否生效

| 系统 | 检查方法 |
|------|----------|
| Windows | 重启电脑后，按 `Ctrl+Shift+Esc` 打开任务管理器 → "详细信息" → 查找 `pythonw.exe` 或 `python.exe` |
| Mac | 重启电脑后，打开"活动监视器" → 搜索 `python`，应能看到 statelink_server 进程 |

#### 关闭自启动

如果以后不想自动启动了：
1. 再次双击 `setup_autostart.py`
2. 或者打开终端/命令提示符，进入 `desktop` 文件夹，输入：
   ```
   python setup_autostart.py --remove
   ```
3. 看到 "Auto-start has been DISABLED" 即取消成功

#### 自启动原理（好奇的话可以看看）

| 系统 | 实现方式 |
|------|----------|
| Windows | 在"启动"文件夹（`shell:startup`）里放入一个静默启动脚本，开机登录后自动执行 |
| macOS | 在 `~/Library/LaunchAgents/` 里注册一个后台服务，用户登录时自动运行 |

---

## 3. 手机端：开启开发者选项和USB调试

所有安卓手机的步骤基本相同，品牌不同菜单位置可能略有差异。

### 3.1 打开"开发者选项"

1. 打开手机的"**设置**"应用（齿轮图标）
2. 找到"**关于手机**"（通常在设置最底部，或"系统"里面）
3. 在"关于手机"页面找到"**版本号**"这一项
4. **连续快速点击"版本号" 7 次**
5. 手机会提示"你已处于开发者模式"或"开发者选项已开启"
6. 返回设置的上一级，你会在设置列表中看到"**开发者选项**"（可能在"系统"或"更多设置"里）

### 3.2 开启USB调试

1. 进入"**开发者选项**"
2. 找到"**USB调试**"这一项（大概在列表中段位置）
3. 点击右侧的开关，让它变成**蓝色/绿色**（开启状态）
4. 手机弹出提示"是否允许USB调试？"，点击"**确定**"

### 3.3 开启"通过USB安装应用"（部分手机需要）

1. 同样在"开发者选项"中
2. 找到"**USB安装**"或"**通过USB安装应用**"
3. 把开关打开
4. 部分小米手机：还需要打开"**USB调试（安全设置）**"

> **完成以上步骤后，开发者选项可以保持开启，不影响正常使用手机。**

---

## 4. 电脑端：安装 Android Studio 并导入项目

### 4.1 下载 Android Studio

1. 打开浏览器，输入：`https://developer.android.com/studio`
2. 点击页面中间的蓝色大按钮 "Download Android Studio"
3. 勾选同意条款，点击下载按钮
4. 下载文件较大（约1GB），请耐心等待

### 4.2 安装 Android Studio（Windows）

1. 双击下载的 `.exe` 文件
2. 一路点击 "Next >"（下一步），不用改任何设置
3. 安装完成后，勾选 "Start Android Studio"，点击 "Finish"
4. 首次启动会询问是否导入设置 → 选择 "Do not import settings"，点击 OK
5. 进入安装向导，一路点 "Next"，全部保持默认选项
6. 最后点击 "Finish"，等待下载SDK组件（约需5-15分钟，取决于网速）

### 4.3 安装 Android Studio（Mac）

1. 双击下载的 `.dmg` 文件
2. 把 Android Studio 图标拖到 Applications（应用程序）文件夹
3. 在"应用程序"中找到 Android Studio，双击打开
4. 首次启动会询问是否导入设置 → 选择 "Do not import settings"，点击 OK
5. 进入安装向导，一路点 "Next"，全部保持默认选项
6. 最后点击 "Finish"，等待下载SDK组件

### 4.4 导入 StateLink 项目

1. 打开 Android Studio
2. 在欢迎界面点击 "**Open**"（打开）按钮
3. 在弹出的文件选择窗口中，导航到本项目位置，选择 `android/StateLinkApp` 文件夹
4. 点击 "OK"（确定）
5. 右下角会显示进度条，正在下载依赖包（首次需要等几分钟）
6. 等进度条消失，底部状态栏显示 "Gradle sync completed" 即成功

---

## 5. 运行测试：手机连接电脑看效果

### 5.1 连接设备

1. 用USB数据线把手机连接到电脑
2. 手机屏幕会弹出"**是否允许USB调试？**"的提示
3. **勾选"始终允许使用这台计算机进行调试"**，然后点"**允许**"
4. 如果手机弹出"选择USB配置"或"USB用途"，选择"**传输文件**"或"**MTP**"

### 5.2 确认连接成功

1. 在 Android Studio 中，看顶部工具栏右侧
2. 应该会显示你的手机型号名称（如 "Xiaomi 14" 或 "SM-S9080"）
3. 如果显示 "No devices"，请检查USB线是否插好、手机上是否点了"允许"

### 5.3 运行电脑端监控程序

1. 回到 `desktop` 文件夹
2. 双击 `statelink_server.py` 运行
3. 确认黑色窗口中显示了 "Server listening on port 8765"
4. 如果显示了 "Found device(s): 1" 和 "ADB reverse tunnel..." 的绿色提示，说明USB连接正常

### 5.4 运行安卓APP

**如果顶部绿色三角形是灰色的（无法点击），按以下步骤操作：**

首先，确认项目已经加载完成：

1. 看 Android Studio 底部状态栏
2. 如果有 **进度条** 或显示 **"Gradle sync"** / **"Indexing"** / **"Download"** 字样 → 说明还在下载依赖，**耐心等它走完**（首次可能需要 5-20 分钟，取决于网速）
3. 等待底部状态栏出现 **"Gradle sync completed"** 绿色对勾

然后，设置运行配置：

1. 点击顶部菜单栏 **"Run"**（运行）
2. 在下拉菜单中点 **"Run..."**（运行…）
3. 弹出的窗口中，在搜索框里输入 **"app"**
4. 选择列表中的 **"app"**（图标是安卓小人）
5. 点击 **"OK"**

现在，顶部绿色三角形应该变为**可点击的绿色**了：

1. 点击 **绿色三角形 ▶ 按钮**（或者按快捷键 `Ctrl+R` / `Cmd+R`）
2. 等待编译（首次约1-3分钟），APP会自动安装到手机上并打开
3. 手机屏幕上会显示 StateLink 的主界面

**如果绿色三角形还是灰色的：**

- 确认顶部工具栏**右侧的设备选择下拉框**里显示了你的手机型号（不是 "No devices"）
- 如果显示 "No devices" → 回到 [5.1 连接设备](#51-连接设备) 重新检查
- 如果下拉框显示的是一串字母/数字（如 "Medium Phone API 34"）→ 那是虚拟模拟器，不是你的真实手机。点开下拉框，选择你的真实手机型号

### 5.5 查看监控数据

- 连接成功后，屏幕顶部显示"**● Connected**"（绿色圆点）
- CPU、内存、磁盘、网络数据会**每秒自动刷新**
- 如果是笔记本，还会显示电池电量
- 手机屏幕会**一直亮着，不会自动熄屏**

---

## 6. 打包安装：生成APK直接装到手机

如果你不想每次都通过 Android Studio 运行，可以把APP打包成一个APK安装文件，直接装到手机上。

### 6.1 生成 APK

1. 在 Android Studio 中，点击顶部菜单栏的 "**Build**"（构建）
2. 在下拉菜单中选择 "**Build Bundle(s) / APK(s)**"
3. 在子菜单中选择 "**Build APK(s)**"
4. 等待右下角进度条走完
5. 完成后，右下角会弹出提示 "APK(s) generated successfully"
6. 点击提示中的 "**locate**"（定位）链接，会打开APK所在的文件夹

### 6.2 安装到手机

**方法一：直接传输安装**
1. 把生成的 `app-debug.apk` 文件复制到手机存储中（通过USB、QQ、微信发送等方式）
2. 在手机上用"文件管理器"找到这个APK文件
3. 点击APK文件，手机会提示安装
4. 点击"安装"，等待完成

**方法二：通过USB安装**
1. 手机连接电脑，确保USB调试已开启
2. 把APK文件拖到手机存储的"Download"文件夹中
3. 在手机上打开"文件管理器" → "Download"文件夹
4. 找到 `app-debug.apk`，点击安装

> **注意**：安装时手机可能会提示"禁止安装未知来源应用"，点击"设置"→ 允许"来自此来源的应用"→ 返回继续安装即可。

---

## 7. 切换主题

APP内置了3套主题配色：

| 主题 | 风格 |
|------|------|
| **Light**（浅色） | 白色背景，蓝色点缀，适合白天使用 |
| **Dark**（深色） | 深灰背景，浅色文字，护眼舒适 |
| **Tech Blue**（科技蓝） | 深蓝背景，青色文字，科技感十足 |

切换方法：
1. 在APP主界面底部点击 "**CHANGE THEME**"（切换主题）按钮
2. 在弹出的对话框中选择你想要的主题
3. APP会自动刷新，应用新主题
4. 主题选择会被记住，下次打开APP还是你选择的主题

---

## 8. 故障排查

### 问题1：手机插上后APP显示"Waiting for PC…"（等待电脑）

| 检查项 | 操作 |
|--------|------|
| 电脑监控服务是否运行 | 确认电脑上 `statelink_server.py` 的黑色窗口没有关闭 |
| USB调试是否允许 | 拔掉USB线重新插，看手机是否弹出"允许USB调试"提示 |
| 数据线是否支持数据传输 | 换一根数据线试试（有些充电线不支持数据传输） |
| ADB是否识别手机 | 在电脑命令提示符/终端中输入 `adb devices`，看是否列出你的手机 |

### 问题2：APP闪退

| 原因 | 解决方法 |
|------|----------|
| 安卓版本过低 | 需要 Android 7.0 及以上（设置 → 关于手机 → Android版本 查看） |
| 安装包损坏 | 在 Android Studio 中重新 Build APK，重新安装 |

### 问题3：数据不更新（一直显示--）

| 原因 | 解决方法 |
|------|----------|
| 电脑服务没启动 | 确认电脑上 `statelink_server.py` 在运行 |
| USB线松动 | 重新插拔USB线 |
| 手机自动断开了USB调试 | 重新插拔USB线 → 手机上点"允许" |

### 问题4：屏幕还是会自动熄灭

| 原因 | 解决方法 |
|------|----------|
| 手机省电模式拦截 | 进入手机设置 → 电池 → 省电模式 → 关闭或把StateLink加入白名单 |
| 手机系统限制 | 部分品牌（华为/小米/OPPO）会自动杀死后台保持屏幕的应用，在"开发者选项"中确认"不锁定屏幕"已开启 |

### 问题5：主题切换后颜色没变

| 原因 | 解决方法 |
|------|----------|
| APP缓存问题 | 先切换到另一个主题再切回来 |
| 极端情况 | 强制停止APP（设置 → 应用 → StateLink → 强制停止），然后重新打开 |

### 问题6：电脑监控服务启动报错 "No module named psutil"

在命令提示符/终端中输入：
```
pip install psutil
```
安装成功后再重新运行 `statelink_server.py`。

### 问题7：Android Studio 导入项目后报错 / Gradle sync failed

**第一步：查看具体错误**

点击 Android Studio 底部的 **"Build Output"**（构建输出）标签页，找到红色错误信息。

**第二步：根据错误信息对号入座**

| 错误关键词 | 原因 | 解决方法 |
|-----------|------|----------|
| `Could not resolve com.android.tools.build:gradle` | 网络不通，无法下载Gradle插件 | 见下方"网络问题专项" |
| `Could not resolve androidx.appcompat` | 网络不通，无法下载依赖库 | 同上 |
| `SDK location not found` | 没指定SDK路径 | 点击弹窗中的 "Edit"，选择SDK路径重试 |
| `Failed to install SDK` / `Android SDK not found` | 缺少Android SDK | 点击菜单 **Tools → SDK Manager →** 勾选 **Android 14.0 (API 34) → Apply** |
| `Unsupported Java` / `Java 8 is required` | JDK版本不对 | 点击菜单 **File → Project Structure → SDK Location →** 确认JDK路径指向JDK 17（Android Studio自带） |
| `compileSdkVersion` / `compileSdk 34 not found` | 编译SDK版本未安装 | 同上：SDK Manager → 勾选 Android 14.0 (API 34) → Apply |
| `Timeout` / `Read timed out` | 下载超时 | 网络慢或防火墙问题，见下方 |

**网络问题专项（中国大陆用户最常见）**

Google 的仓库在国内可能无法直接访问，需要配置国内镜像：

1. 在 Android Studio 中打开项目根目录的 `build.gradle`（注意：是根目录那个，不是 app 里面的）
2. 把 `repositories` 这段替换为：
   ```gradle
   repositories {
       maven { url 'https://maven.aliyun.com/repository/google' }
       maven { url 'https://maven.aliyun.com/repository/central' }
       maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
       google()
       mavenCentral()
       gradlePluginPortal()
   }
   ```
3. 同样修改 `settings.gradle` 里的 repositories
4. 点击顶部菜单 **File → Sync Project with Gradle Files** 重新同步

**代理设置（如果用了梯子/代理）**

1. 点击菜单 **File → Settings**（Mac：**Android Studio → Preferences**）
2. 搜索 "HTTP Proxy"
3. 选择 **Manual proxy configuration**
4. 填入你的代理地址和端口（如 `127.0.0.1:7890`）
5. 点击 **Apply → OK**
6. 重新同步

**终极方法：让 Android Studio 自动修复**

1. 点击菜单 **File → Project Structure**
2. 在 **SDK Location** 标签页确认 SDK 和 JDK 路径
3. 点击 **OK**，Android Studio 会自动下载缺失的组件
4. 然后 **File → Sync Project with Gradle Files** 重新同步

### 问题8：顶部绿色三角形（Run按钮）是灰色的，点不了

| 原因 | 解决方法 |
|------|----------|
| Gradle 还在同步中 | 看 Android Studio 底部状态栏，如果有进度条就**耐心等待**，首次同步可能需要 5-20 分钟 |
| 没有配置运行方式 | 点击顶部菜单 **Run** → **Run...** → 搜索框输入 "app" → 选择 "app" → 点 OK |
| 没有选中手机设备 | 确认顶部工具栏右侧设备下拉框里显示的是你的手机型号，不是 "No devices" |
| 当前打开的不是项目文件夹 | 确认你打开的是 `android/StateLinkApp` 文件夹，不是上级目录 |

**最直接的解决方法：**

1. 等待底部状态栏所有进度条消失
2. 点击顶部菜单 **Run → Run...**（运行 → 运行…）
3. 选择 **app** → 点 OK
4. 绿色三角形就会变亮，然后点击它运行

---

> **总结核心操作流程**：
> 1. 电脑双击运行 `statelink_server.py`
> 2. 手机USB插电脑 → 弹出提示点"允许"
> 3. 手机打开 StateLink APP → 自动连接
> 4. 手机屏幕常亮，实时显示电脑CPU/内存/磁盘/网速/电池
>
> **就是这么简单！**
