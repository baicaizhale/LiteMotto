
<img width="256" height="256" alt="icon" src="https://github.com/user-attachments/assets/f3d0c38b-29f8-4b50-90ed-fb7b8c17e209" />


# LiteMotto

LiteMotto 是一个 **Minecraft Spigot 插件**，可以在玩家加入服务器时发送一句每日格言。格言由 **Cloudflare AI** 生成，支持自定义前缀、颜色代码以及提示词。

---

**您大可在以下位置更细致的了解本插件：**
- [LiteMotto文档库](https://baicaizhale.icu/notes/LiteMotto/) 
- [由AI生成的LiteMotto简介](https://litemotto.baicaizhale.icu/)

**以及对应的仓库：**
- https://github.com/baicaizhale/doc/tree/main/docs/notes/LiteMotto
- https://github.com/baicaizhale/LiteMotto/tree/web
## 📢 目前不知道怎么解决的问题，路过的大佬可以尝试帮忙解决
v3.0.0以后的版本为了防止ai生成重复内容会把最近已经生成过的内容提供给ai来让ai不再生成相同内容，但这也导致了ai会模仿前几次格言的语气，比如重启服务器后第一句如果是文言文那么后面生成的都会和前面一样是文言文

## ✨ 功能特点

- 🎭 **AI 生成每日格言**（使用 Cloudflare AI，支持多种模型，包括 @cf/openai/gpt-oss-120b 和 @cf/meta/llama-3-8b-instruct）  
- 🎨 **支持 & 颜色代码**（自动转换为 Minecraft 颜色格式）  
- ⚙ **可配置前缀 & 提示词**，自由定制消息风格  
- 🚀 **异步获取数据**，不会影响服务器性能  
- 🔄 **自动兼容 Cloudflare 多种模型返回格式**，无需手动切换

## 📦 安装方法

### 1. 下载插件
从 [Releases](https://github.com/baicaizhale/LiteMotto/releases) 页面下载 `LiteMotto.jar`，或自行编译源代码。

### 2. 放入服务器插件目录
将 `LiteMotto.jar` 复制到 `plugins/` 文件夹。

### 3. 启动服务器
启动服务器，插件会自动生成 `config.yml` 配置文件；或者您也可以使用 Plugman-X 进行热加载。

### 4. 配置插件
打开 `plugins/LiteMotto/config.yml` 并填入 Cloudflare API 信息（不会可无视）

```yaml
account-id: "你的 Cloudflare 账户 ID"
api-key: "你的 Cloudflare API Key"
model: "@cf/openai/gpt-oss-120b" # 或其它支持的模型，如 @cf/meta/llama-3-8b-instruct
prompt: "请直接返回一句有哲理的格言，不要思考，也不要包含任何前后缀、标点、额外的文字或解释。"
prefix: "§bLiteMotto §7> §f"
```

- **model 字段说明：**
  - 推荐使用 `@cf/openai/gpt-oss-120b`，也可用 `@cf/meta/llama-3-8b-instruct` 等。
  - 插件会自动识别并适配 Cloudflare 不同模型的 API 返回格式。

- **prompt 字段说明：**
  - 可自定义提示词，影响 AI 生成格言的风格。

- **prefix 字段说明：**
  - 支持 `&` 和 `§` 颜色符号，自动转换为 Minecraft 颜色代码。

## 🛠 常见问题

- **Q: 支持哪些 Cloudflare AI 模型？**
  - A: 推荐 `@cf/openai/gpt-oss-120b`，也支持 `@cf/meta/llama-3-8b-instruct` 等。插件会自动适配。

- **Q: gpt-oss-120b 为什么不能用 chat/completions 格式？**
  - A: 插件已自动适配 Cloudflare 的 run/input 新格式，无需手动切换。

- **Q: 格言内容重复怎么办？**
  - A: 插件会自动避免最近 10 条格言重复。

- **Q: 如何自定义格言风格？**
  - A: 修改 config.yml 的 prompt 字段即可。

## 📝 开发&贡献

- 源码地址：[https://github.com/baicaizhale/LiteMotto](https://github.com/baicaizhale/LiteMotto)
- 欢迎提交 issue 或 PR 反馈和改进建议！

---

本插件与 Cloudflare AI 官方无直接关联，仅为个人项目。
![Bstats](https://bstats.org/signatures/bukkit/LiteMotto.svg)
