# 打工人小账本 · 原生安卓版

一个**真正的安卓 App**（独立图标、常驻后台）：付款通知一弹就自动记账，
**用手机自己的流量 / WiFi 都行，不依赖电脑、不联网、数据全在手机本地**。

技术构成：原生 `NotificationListenerService` 监听支付宝 / 微信 / 银行支付通知 →
正则抓金额 → 写入本地文件；界面复用已验证的金色手账 WebView。

---

## 你只需要做一件事：有 GitHub 账号（免费）

全程网页点击，**不用装 Android Studio、不用敲命令**。
编译由 GitHub 免费的云服务器自动完成，编译好的 APK 你直接下载安装。

> 没有 GitHub？去 https://github.com 用邮箱注册一个即可（免费）。

---

## 三步拿到 APK

### 第 1 步：新建仓库
1. 登录 GitHub，右上角 `+` → **New repository**
2. Repository name 填 `dagongren-ledger`，选 **Public**
3. 点 **Create repository**

### 第 2 步：把代码传上去
1. 在仓库页面找到 **“uploading an existing file”** 链接
2. 把我给你的 `ledger-android` 文件夹**整个拖进上传区**（GitHub 会保留目录结构，包括 `.github`）
3. 拉到底点 **Commit changes**

> 传完后，仓库里应当能看到：`app/`、`.github/`、`build.gradle`、`settings.gradle` 等。

### 第 3 步：等它自动编译
1. 点仓库顶部的 **Actions** 标签
2. 会看到一条 **Build APK** 工作流在跑（黄色转圈）
3. 等 **2~3 分钟**变成绿色 ✅
4. 点进这条记录 → 最下方 **Artifacts** → 下载 **ledger-debug-apk**（是个 ZIP）
5. 解压得到 `app-debug.apk`

---

## 手机安装与授权

1. 把 `app-debug.apk` 传到手机（微信文件传输 / 数据线 / 网盘都行）
2. 手机「设置 → 安全 / 隐私」里允许**「未知来源」安装**（不同品牌名字略不同）
3. 点 APK 安装
4. 首次打开，App 会**自动跳到「通知读取」授权页**：
   找到 **「打工人账本·通知记账」** → 打开开关
   （这是系统正规授权，不是偷偷获取；不给这个权限就无法自动记账）

---

## 开始用

- 打开 App，就是熟悉的金色手账界面（记账 / 记录 / 月度 / 基金 / 今日）
- 用**支付宝或微信付一笔钱** → 手机弹通知 → 切回 App（或下拉刷新）
  → **记录里立刻多出这笔自动账**
- 金额自动抓；分类按商户关键词尽力归类（餐饮 / 交通 / 购物…）
- 商户名常常只能拿到 App 名（如“微信支付”），想写清的消费项目可在 App 里随手改备注
- 数据存在手机本地，**不上任何云**，最隐私

---

## 想增删监听的银行 App？

打开 `app/src/main/java/com/ledger/app/LedgerNotificationService.kt`，
在 `pkgWhitelist` 里加 / 删包名，重新走一遍上面的编译即可。
常见包名已预置：支付宝、微信、云闪付、工行、中行、招行、浦发、建行、交行、民生、兴业。

---

## 常见问题

- **iPhone 能用吗？** 不能。iOS 不允许任何 App 读通知，这条路只有安卓有。
- **APK 是 debug 签名，安全吗？** 能正常安装使用，只是没上架商店。本地自用完全够；
  以后想上架 Google Play 再聊正式签名。
- **编译失败？** 多半是漏传了 `.github` 目录或文件结构不对。检查仓库根目录是否有
  `.github/workflows/build.yml`。
- **换手机？** 重装 App 后重新授权通知读取即可；旧数据在旧手机本地，可先用 App 内
  「导出 JSON」备份，再在新手机导入。

---

## 文件结构（给你了解，不用改）

```
ledger-android/
├── .github/workflows/build.yml   # 云端自动编译配置
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml   # 权限与服务声明
│       ├── java/com/ledger/app/
│       │   ├── MainActivity.kt            # WebView 宿主 + 授权引导
│       │   ├── LedgerNotificationService.kt  # 通知监听 + 抓金额
│       │   ├── LedgerData.kt              # 本地文件读写
│       │   └── BootReceiver.kt            # 开机初始化
│       ├── res/...                        # 布局 / 主题 / 图标
│       └── assets/
│           ├── ledger-phone.html          # 复用的手账界面
│           └── ledger_data.js             # 自动抓取写入这里
├── build.gradle / settings.gradle        # 构建配置
```
