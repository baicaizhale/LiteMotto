
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
## ⚠️ 已知问题

### 当前存在的问题
**v3.0.0 版本后的风格模仿问题**
- **问题描述**: 为了防止AI生成重复内容，插件会将最近已生成的格言提供给AI作为参考。但这导致AI会模仿前几次格言的语气风格。
- **具体表现**: 重启服务器后，如果第一条格言是文言文风格，后续生成的格言也会保持文言文风格。
- **影响范围**: 主要影响格言风格一致性。

### 寻求帮助
如果您有解决此问题的想法或方案，欢迎提交 [Issue](https://github.com/baicaizhale/LiteMotto/issues) 或 [Pull Request](https://github.com/baicaizhale/LiteMotto/pulls)！

## ✨ 功能特点

- 🎭 **AI 生成每日格言**（使用 Cloudflare AI，支持多种模型，包括 @cf/openai/gpt-oss-120b 和 @cf/meta/llama-3-8b-instruct）  
- 🎨 **支持 & 颜色代码**（自动转换为 Minecraft 颜色格式）  
- ⚙ **可配置前缀 & 提示词**，自由定制消息风格  
- 🚀 **异步获取数据**，不会影响服务器性能  
- 🔄 **自动兼容 Cloudflare 多种模型返回格式**，无需手动切换
- ☁️ **Cloudflare集成优化**: 添加通过API Key自动获取account-id功能

## 📦 安装方法

### 1. 下载插件
- **方式一（推荐）**: 从 [Releases](https://github.com/baicaizhale/LiteMotto/releases) 页面下载最新的 `LiteMotto.jar` 文件
- **方式二**: 自行编译源代码（需要 Maven 环境）
  ```bash
  git clone https://github.com/baicaizhale/LiteMotto.git
  cd LiteMotto
  mvn clean package
  ```

### 2. 放入服务器插件目录
- 将下载或编译得到的 `LiteMotto.jar` 文件复制到服务器的 `plugins/` 文件夹中
- 确保服务器有足够的权限读写插件目录

### 3. 启动服务器
- **首次启动**: 启动服务器，插件会自动生成默认的 `config.yml` 配置文件
- **热加载**: 如果使用 Plugman-X 等热加载工具，可以直接加载插件而无需重启服务器

### 4. 配置插件
打开 `plugins/LiteMotto/config.yml` 文件，根据您的需求进行配置：

```yaml
# Cloudflare AI 配置（可选，带有默认配置，不配置也能使用基础功能）
# 如果提供了api-key，account-id可以自动获取
account-id: "你的 Cloudflare 账户 ID"
api-key: "你的 Cloudflare API Key"
model: "@cf/openai/gpt-oss-120b"
prompt: "请直接返回一句有哲理的格言，不要思考，也不要包含任何前后缀、标点、额外的文字或解释。"
prefix: "§bLiteMotto §7> §f"
```

### 配置项详细说明

#### Cloudflare AI 配置（可选）
- **account-id**: 您的 Cloudflare 账户 ID
  - 获取方式：登录 Cloudflare 控制台 → 右上角账户头像 → 复制账户 ID
  - **注意**: 如果提供了 `api-key`，插件将尝试自动获取 `account-id`，此项可留空。
- **api-key**: Cloudflare API 密钥
  - 获取方式：Cloudflare 控制台 → 左侧菜单 "AI" → "管理 API 令牌" → 创建 API 令牌
  - **注意**: 提供此项后，插件将尝试自动获取 `account-id`。
- **model**: AI 模型选择
  - **推荐**: `@cf/openai/gpt-oss-120b`（效果最佳）
  - **备选**: `@cf/meta/llama-3-8b-instruct`（响应更快）
  - **特性**: 插件会自动适配不同模型的 API 返回格式

#### 格言生成配置
- **prompt**: AI 提示词
  - **默认值**: 简洁的哲理格言生成提示
  - **自定义示例**: 
    - `"请用文言文风格生成一句格言"`
    - `"请生成一句鼓励学习的格言"`
    - `"请用幽默的方式说一句人生感悟"`
- **prefix**: 消息前缀
  - **支持格式**: `&` 颜色代码和 `§` Minecraft 颜色代码
  - **自动转换**: 插件会自动将 `&` 转换为 `§`
  - **颜色代码参考**:
    - `&0`/`§0` - 黑色
    - `&1`/`§1` - 深蓝色
    - `&2`/`§2` - 深绿色
    - `&3`/`§3` - 湖蓝色
    - `&4`/`§4` - 深红色
    - `&5`/`§5` - 紫色
    - `&6`/`§6` - 金色
    - `&7`/`§7` - 灰色
    - `&8`/`§8` - 深灰色
    - `&9`/`§9` - 蓝色
    - `&a`/`§a` - 绿色
    - `&b`/`§b` - 天蓝色
    - `&c`/`§c` - 红色
    - `&d`/`§d` - 粉红色
    - `&e`/`§e` - 黄色
    - `&f`/`§f` - 白色
    - `&k`/`§k` - 随机字符
    - `&l`/`§l` - 粗体
    - `&m`/`§m` - 删除线
    - `&n`/`§n` - 下划线
    - `&o`/`§o` - 斜体
    - `&r`/`§r` - 重置

## 🛠 常见问题与故障排除

### 配置相关
- **Q: 支持哪些 Cloudflare AI 模型？**
  - A: 推荐 `@cf/openai/gpt-oss-120b`（效果最佳），也支持 `@cf/meta/llama-3-8b-instruct`（响应更快）等。插件会自动适配不同模型的 API 返回格式。

- **Q: gpt-oss-120b 为什么不能用 chat/completions 格式？**
  - A: Cloudflare 的 gpt-oss-120b 模型使用新的 `run/input` API 格式，插件已自动适配，无需手动切换。

- **Q: 如何自定义格言风格？**
  - A: 修改 `config.yml` 的 `prompt` 字段即可。例如：
    - 文言文风格：`"请用文言文生成一句哲理格言"`
    - 鼓励学习：`"请生成一句鼓励学生学习的格言"`
    - 幽默风格：`"请用幽默的方式说一句人生感悟"`

### 功能相关
- **Q: 格言内容重复怎么办？**
  - A: 插件会自动避免最近 10 条格言重复。如果发现重复，可以：
    1. 删除 `plugins/LiteMotto/recent_mottos.json` 文件重置历史记录
    2. 修改 `prompt` 字段增加随机性

- **Q: 插件不显示格言怎么办？**
  - A: 检查以下可能原因：
    1. **网络连接**: 确保服务器能正常访问 Cloudflare API
    2. **API 配置**: 检查 `account-id` 和 `api-key` 是否正确
    3. **权限问题**: 确保插件有足够的文件读写权限
    4. **控制台日志**: 查看服务器控制台是否有错误信息

- **Q: 颜色代码不生效怎么办？**
  - A: 确保：
    1. 使用正确的颜色代码格式（`&` 或 `§`）
    2. 玩家有权限查看格式化文本
    3. 服务器支持颜色代码显示

### 技术相关
- **Q: 插件启动失败怎么办？**
  - A: 检查：
    1. **版本兼容性**: 确保服务器版本与插件兼容
    2. **依赖冲突**: 检查是否有其他插件冲突
    3. **日志分析**: 查看详细的错误堆栈信息

- **Q: 如何调试插件问题？**
  - A: 建议步骤：
    1. 查看服务器控制台日志
    2. 检查配置文件是否正确
    3. 测试网络连接
    4. 联系开发者提供详细错误信息

- **Q: 插件性能如何？**
  - A: 插件采用异步设计：
    - AI 请求在后台线程执行，不影响主线程
    - 缓存机制减少重复 API 调用
    - 轻量级设计，内存占用低

## 🔌 API 调用说明

LiteMotto 提供了简单的 API 接口，供其他插件调用 AI 格言生成功能。

### API 类信息
- **包名**: `org.baicaizhale.litemotto.api.LiteMottoAPI`
- **方法**: `fetchMottoWithPrompt(String customPrompt)`
- **返回值**: 生成的格言字符串，失败时返回 `null`

### 使用示例

#### 1. 通过反射调用（推荐）
```java
// 检查LiteMotto插件是否可用
Plugin liteMottoPlugin = Bukkit.getPluginManager().getPlugin("LiteMotto");
if (liteMottoPlugin != null && liteMottoPlugin.isEnabled()) {
    try {
        // 加载API类
        Class<?> apiClass = Class.forName("org.baicaizhale.litemotto.api.LiteMottoAPI");
        
        // 创建API实例
        Object apiInstance = apiClass.newInstance();
        
        // 获取方法
        Method fetchMottoMethod = apiClass.getMethod("fetchMottoWithPrompt", String.class);
        
        // 调用方法（异步执行）
        Bukkit.getScheduler().runTaskAsynchronously(yourPlugin, () -> {
            try {
                String customPrompt = "请返回一句鼓励高考学生的格言，要简洁有力";
                String motto = (String) fetchMottoMethod.invoke(apiInstance, customPrompt);
                
                // 在主线程中处理结果
                Bukkit.getScheduler().runTask(yourPlugin, () -> {
                    if (motto != null) {
                        player.sendMessage("今日格言: " + motto);
                    } else {
                        // 处理失败情况
                        player.sendMessage("格言生成失败，请稍后再试");
                    }
                });
            } catch (Exception e) {
                // 处理异常
                e.printStackTrace();
            }
        });
    } catch (Exception e) {
        // 处理API调用异常
        e.printStackTrace();
    }
}
```

#### 2. 直接依赖调用
如果您的插件直接依赖 LiteMotto，可以直接导入 API 类：
```java
import org.baicaizhale.litemotto.api.LiteMottoAPI;

// 创建API实例
LiteMottoAPI api = new LiteMottoAPI();

// 检查插件是否可用
if (api.isAvailable()) {
    // 异步调用
    Bukkit.getScheduler().runTaskAsynchronously(yourPlugin, () -> {
        String motto = api.fetchMottoWithPrompt("自定义提示词");
        
        // 在主线程中处理结果
        Bukkit.getScheduler().runTask(yourPlugin, () -> {
            // 处理格言结果
        });
    });
}
```

### API 特性

- **自动配置**: API 会自动读取 LiteMotto 的配置文件（account-id、api-key、model 等）
- **重复避免**: 自动避免生成最近 10 条已生成的格言
- **模型适配**: 自动适配 Cloudflare 不同模型的 API 返回格式
- **颜色代码**: 返回的格言已自动处理 Minecraft 颜色代码
- **异常处理**: 提供完整的异常处理机制

### 注意事项

1. **异步调用**: API 调用涉及网络请求，务必使用异步方式执行
2. **线程安全**: 结果处理需要在主线程中进行
3. **错误处理**: 始终检查返回值是否为 null
4. **依赖检查**: 调用前检查 LiteMotto 插件是否可用

## 📝 开发与贡献

### 项目结构
```
LiteMotto/
├── src/main/java/
│   ├── org/baicaizhale/litemotto/
│   │   ├── LiteMotto.java          # 插件主类
│   │   ├── api/
│   │   │   └── LiteMottoAPI.java   # API 接口类
│   │   ├── managers/
│   │   │   ├── ConfigManager.java  # 配置管理
│   │   │   └── RecentMottoManager.java # 历史格言管理
│   │   └── utils/
│   │       └── ColorUtils.java     # 颜色工具类
│   └── listeners/
│       └── PlayerJoinListener.java # 玩家加入监听器
├── src/main/resources/
│   ├── plugin.yml                   # 插件描述文件
│   └── config.yml                   # 默认配置文件
└── pom.xml                         # Maven 构建配置
```

### 开发环境搭建
1. **克隆项目**
   ```bash
   git clone https://github.com/baicaizhale/LiteMotto.git
   cd LiteMotto
   ```

2. **安装依赖**
   ```bash
   mvn clean install
   ```

3. **编译插件**
   ```bash
   mvn clean package
   ```

### 贡献指南

#### 提交 Issue
- **Bug 报告**: 提供详细的错误信息、复现步骤、环境信息
- **功能建议**: 描述需求场景、预期效果、实现思路

#### 提交 Pull Request
1. **Fork 项目**到个人仓库
2. **创建功能分支**: `git checkout -b feature/your-feature`
3. **编写代码**: 编写相关代码
4. **提交 PR**: 描述修改内容和目的


---

**欢迎贡献代码！** 如果您有任何问题或建议，请通过以下方式联系：
- GitHub Issues: [提交问题](https://github.com/baicaizhale/LiteMotto/issues)
- Pull Requests: [贡献代码](https://github.com/baicaizhale/LiteMotto/pulls)
- 文档仓库: [LiteMotto 文档](https://github.com/baicaizhale/doc/tree/main/docs/notes/LiteMotto)

---

本插件与 Cloudflare AI 官方无直接关联，仅为个人项目。
![Bstats](https://bstats.org/signatures/bukkit/LiteMotto.svg)
