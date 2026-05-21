# StateLink

**USB数据线直连，手机实时监控电脑状态 —— 无需WiFi、无需网络、无需蓝牙**

## 这是什么

StateLink 让你用安卓手机通过**一根USB数据线**实时监控电脑的硬件状态：

- CPU 使用率
- 内存 使用率
- 磁盘 使用率
- 网络上传/下载速度
- 电池电量（笔记本）

手机插上数据线就能看到电脑的实时运行状态，屏幕一直亮着不会熄灭，3套主题配色随意切换。

## 为什么用它

| 场景 | 说明 |
|------|------|
| 打游戏全屏时 | 手机放旁边看CPU/GPU温度，不用切出去开任务管理器 |
| 渲染/编译时 | 随时瞄一眼CPU和内存是否跑满 |
| 下载大文件时 | 手机实时显示网速，不用切窗口 |
| 笔记本省电 | 合盖外接显示器时，手机上看电池状态 |
| 服务器/工控机 | 没有显示器时，插手机就能看运行状态 |

## 原理

```
┌──────────────┐    USB 数据线    ┌──────────────────┐
│  安卓手机     │ ◄──ADB 反向隧道─│  Windows/Mac 电脑 │
│              │   localhost:8765 │                  │
│  Java APP   │                  │  Python 服务     │
│  每秒轮询   │                  │  psutil 采集     │
│  卡片式展示 │                  │  CPU/MEM/DISK    │
│  屏幕常亮   │                  │  NET/BAT 数据    │
└──────────────┘                 └──────────────────┘
```

- 电脑端：Python脚本用 `psutil` 读取系统传感器，启动HTTP服务监听 `8765` 端口
- 通信链路：`adb reverse tcp:8765 tcp:8765` 把手机8765端口映射到电脑8765端口
- 手机端：Android APP每秒 `GET http://localhost:8765/stats` 获取JSON数据并展示
- 零网络依赖：全程走USB物理线路，不消耗流量，不需要WiFi/蓝牙

## 快速开始

### 1. 安装依赖（只做一次）

电脑上安装 Python 3.9+，然后打开终端/命令提示符运行：

```bash
pip install psutil
```

### 2. 运行监控服务

双击 `desktop/statelink_server.py`，或终端运行：

```bash
cd desktop
python statelink_server.py
```

看到 "Server listening on port 8765" 表示成功。**此窗口保持打开。**

### 3. 手机连接

1. 手机开启 **USB调试**（设置 → 开发者选项 → USB调试）
2. USB数据线连接电脑 → 手机上点"允许USB调试"
3. 用 Android Studio 打开 `android/StateLinkApp/` 项目，点 ▶ 运行

手机APP会自动连接，屏幕保持常亮，每秒刷新数据。

> 详细零基础教程（含截图步骤）见 **[TUTORIAL.md](TUTORIAL.md)**

## 开机自启动（推荐）

配置一次之后，电脑开机/登录时监控服务自动后台运行，再也不用手动双击脚本。

```bash
cd desktop
python setup_autostart.py
```

| 系统 | 实现方式 |
|------|----------|
| Windows | VBS 静默启动脚本 → Startup 文件夹 |
| macOS | LaunchAgent plist → `~/Library/LaunchAgents/` |

取消自启动：

```bash
python setup_autostart.py --remove
```

## 项目结构

```
StateLink/
├── desktop/                             # 电脑端监控服务
│   ├── statelink_server.py              # 主程序：采集 + HTTP服务
│   ├── setup_autostart.py               # 一键配置开机自启动
│   └── requirements.txt                 # Python 依赖
│
├── android/StateLinkApp/                # 安卓APP（Android Studio 打开）
│   └── app/src/main/
│       ├── java/com/statelink/app/
│       │   ├── MainActivity.java        # 主界面 + 轮询 + 常亮 + 主题
│       │   └── ThemeManager.java        # 主题持久化
│       └── res/
│           ├── layout/activity_main.xml # 卡片式监控面板
│           ├── values/styles.xml        # Light / Dark / Tech Blue 主题
│           └── drawable/                # 图标、指示器
│
├── TUTORIAL.md                          # 零基础完整教程
└── README.md                            # 本文件
```

## 主题

| Light | Dark | Tech Blue |
|-------|------|------------|
| 白色背景 | 深灰背景 | 深蓝背景 |
| 蓝色点缀 | 浅蓝点缀 | 青色点缀 |

点击APP底部 "Change Theme" 即可切换，选择会被记住。

## 兼容性

| 平台 | 版本 |
|------|------|
| Android | 7.0 及以上 |
| Windows | 10 / 11 |
| macOS | 12 及以上 |
| Python | 3.9 及以上 |

## 故障排查

| 症状 | 解决方法 |
|------|----------|
| APP显示 "Waiting for PC" | 1. 确认电脑端脚本在运行 2. 重新插拔USB线 3. 手机上点"允许USB调试" |
| 屏幕还是会熄灭 | 手机设置 → 电池 → 关闭省电模式 / 把StateLink加入白名单 |
| 服务启动报错 "No module named psutil" | 终端运行 `pip install psutil` |
| 电脑没装Python | 从 [python.org](https://python.org) 下载安装 |

更多问题见 [TUTORIAL.md 故障排查章节](TUTORIAL.md#8-故障排查)

## 许可

MIT License
