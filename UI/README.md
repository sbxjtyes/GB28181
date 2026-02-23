# GB28181 设备管理 Web UI

基于 React + Vite 构建的 GB28181 设备管理前端界面。

## 功能特性

- 📹 **设备管理** - 查看、管理 GB28181 设备列表
- 🔄 **设备注册** - 支持单设备/批量强制重新注册
- ▶️ **推流控制** - 支持向 ZLMediaKit 媒体服务器推流
- 🎥 **媒体服务** - 查看和管理 ZLMediaKit 流列表

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 配置说明

首次使用时，点击右上角的 ⚙️ 设置按钮，配置以下参数：

- **SIP服务器地址** - GB28181 SIP 服务器 API 地址
- **媒体服务器IP** - 摄像头推流目标地址（通常为本机局域网IP）
- **ZLMediaKit 地址** - ZLMediaKit 媒体服务器 HTTP API 地址
- **ZLMediaKit 密钥** - ZLMediaKit API 访问密钥

## 技术栈

- React 19
- Vite 7
- Vanilla CSS

## 目录结构

```
UI/
├── src/
│   ├── api/           # API 封装模块
│   ├── components/    # React 组件
│   ├── assets/        # 静态资源
│   ├── App.jsx        # 主应用组件
│   ├── App.css        # 应用样式
│   └── main.jsx       # 入口文件
├── public/            # 公共资源
├── dist/              # 构建输出
└── index.html         # HTML 模板
```
