# 学习通提交截图拍摄清单（前端）

适用对象：前端同学（微信小程序）

## 拍摄前准备

1. 打开微信开发者工具并成功编译项目。
2. 确认后端可用（推荐）或使用前端 Mock 兜底。
3. 清理页面调试信息，确保截图内容清晰。

## 截图文件命名建议

- 01-git-log.png
- 02-login-page.png
- 03-home-page.png
- 04-restaurants-list-page.png
- 05-detail-page.png
- 06-mine-page.png
- 07-spin-page.png
- 08-swipe-page.png
- 09-api-success.png

## 页面截图操作路径（按顺序拍）

1. 登录/注册页面
- 进入路径：启动小程序 -> pages/index/index
- 需展示内容："微信一键登录"按钮、登录说明文案
- 建议文件：02-login-page.png

2. 首页
- 操作路径：点击登录 -> 自动跳转首页 pages/home/home
- 需展示内容：统计卡片、热门推荐、功能入口（大转盘/摇一摇/卡片滑）
- 建议文件：03-home-page.png

3. 餐厅列表页
- 操作路径：首页 -> 点击"查看全部" -> pages/restaurants/restaurants
- 需展示内容：筛选栏（分类/价位/排序）+ 列表卡片
- 建议文件：04-restaurants-list-page.png

4. 详情页
- 操作路径：餐厅列表页 -> 点击任意卡片 -> pages/detail/detail
- 需展示内容：餐厅图片、基本信息、标签、拉黑按钮
- 建议文件：05-detail-page.png

5. 个人中心页
- 操作路径：首页 -> 点击"添加新餐厅"或进入 pages/mine/mine
- 需展示内容：用户信息、统计区、菜单项（退出登录/关于）
- 建议文件：06-mine-page.png

6. 其他业务页面（至少 1-2 个）
- 大转盘页路径：首页 -> 大转盘 -> pages/spin/spin
- 卡片滑选路径：首页 -> 卡片滑 -> pages/swipe/swipe
- 需展示内容：对应页面核心交互界面
- 建议文件：07-spin-page.png、08-swipe-page.png

## API 对接截图（学习通必交）

1. 打开微信开发者工具 -> 调试器 -> Network 面板。
2. 触发接口请求（推荐在首页或列表页触发 nearby 查询）。
3. 截图需包含：
- 请求 URL（示例：/api/v1/restaurants/nearby）
- 状态码（200）
- 返回体中 data 字段
4. 建议文件：09-api-success.png

## Git 提交记录截图（学习通必交）

1. 在项目根目录终端执行：

```bash
git log --author="林家涛" --oneline
```

2. 若未匹配到记录，可改为：

```bash
git log --author="2312190316" --oneline
```

3. 截图需包含：命令与输出结果同屏。
4. 建议文件：01-git-log.png

## 最终提交核对

- 至少 5 张页面运行截图（建议 7 张以上）
- 1 张 API 成功截图
- 1 张 git log 截图
- 截图文件统一放在 docs/design/screenshots/
