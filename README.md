# LiteMotto

LiteMotto 是一个 **Minecraft Spigot 插件**，可以在玩家加入服务器时发送一句每日格言。格言由 **Cloudflare AI** 生成，支持自定义前缀、颜色代码以及提示词。

## ✨ 功能特点

- 🎭 **AI 生成每日格言**（使用 Cloudflare AI）  
- 🎨 **支持 & 颜色代码**（自动转换为 Minecraft 颜色格式）  
- ⚙ **可配置前缀 & 提示词**，自由定制消息风格  
- 🚀 **异步获取数据**，不会影响服务器性能  

## 📦 安装方法

### **1. 下载插件**
从 **[Releases](https://github.com/baicaizhale/LiteMotto/releases)** 页面下载 `LiteMotto.jar`，或自行编译源代码。

### **2. 放入服务器插件目录**
将 `LiteMotto.jar` 复制到 `plugins/` 文件夹。

### **3. 启动服务器**
启动服务器，插件会自动生成 `config.yml` 配置文件；或者您也可以使用 `Plugman-X` 进行热加载。

### **4. 配置插件**
打开 `plugins/LiteMotto/config.yml` 并填入 Cloudflare API 信息（不会可无视）
