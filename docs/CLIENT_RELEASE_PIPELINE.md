# 客户端 Gamma → Prod 发布流水线（G184）

## [BR-RELEASE-006] 私有源码与授权 R2 自动更新分发

正式仓库转为私有后，源码、构建历史和长期归档继续保存在私有 GitHub；面向用户的 Prod 成品不再依赖
GitHub Release 匿名下载，而是保存到私有 Cloudflare R2。R2 只保存已验收 Gamma 原包及其 RSA 签名清单，
绝不保存源码、授权码或客户端长期下载凭据。D1 只保存推荐版本、精确 R2 对象键、大小、哈希和政策 revision，
不保存 ZIP 字节。对象键必须带不可变版本号，禁止覆盖 `latest.zip`；生产指针只能在对象上传、远端核验和签名完成后
通过 D1 revision CAS 切换。R2 默认保留最近三个 Prod，至少经过安全观察期才清理更旧对象；承担旧客户端迁移的
桥接版必须延长保留，GitHub 私有 Release 可继续保存完整历史。

客户端公开的版本检查接口只返回政策和固定的更新票据入口，不返回 GitHub 下载页。用户点击一次更新后，客户端使用
首次成功授权时已由 Windows DPAPI 按当前用户保护并持久化的授权码，连同设备指纹、客户端身份、当前版本和目标版本，
自动向 Worker 请求一次短期更新票据；后续更新不得再次要求用户输入授权码。首次从未成功授权、凭据无法解密、授权失效
或设备不匹配时必须明确要求重新授权，不能降级成公开下载。更新票据是独立准入，不依赖运行中 session：被禁旧版仍须能
下载维护者当前允许的推荐版，但只能下载政策中精确目标。Worker 验证许可、设备和目标政策后返回签名清单及短期 R2
预签名 URL；客户端不向用户展示该 URL，直接下载、复核包内固定公钥签名、大小和 SHA256，再沿 BR-RELEASE-003 的
外部安装器切换并重启。预签名 URL 是短期 bearer，日志、UI、异常和持久化文件都不得输出或保存完整 URL。

GitHub Free 私有仓库不使用 Environment Secret/保护规则。Prod 使用 `workflow_dispatch`、固定 `master`、精确 Gamma
run/attempt、触发者和显式确认词门禁；仓库 Secret 仅注入作业内存。签名密钥迁移到 repository secret 后才能移除旧
`client-production` Environment 依赖。Gamma artifact 只作短期晋级中转，默认保留三天；Prod 仍必须晋级同一个已验收
Gamma 字节，禁止重建。正式切私有前必须先公开一个同时支持旧 GitHub 下载和新 Worker/R2 下载的桥接客户端，确认用户
已迁移后才能关闭旧公共入口；不得先私有化导致现有客户端永远拿不到桥接版。

## [BR-RELEASE-005] 客户端 JVM 按需伸缩与轻量内存可观测性

客户端是长驻桌面程序，不按服务端常驻大堆预热。开发启动与 jpackage 成品必须从同一份受版本控制的
JVM 选项真源构建，当前批准值为 `-Xms512m`、`-Xmx6g`、`-XX:G1HeapRegionSize=16m`、
`-XX:G1PeriodicGCInterval=300000`。512MiB只定义启动已提交堆，6GiB是尚未取得真实live-heap峰值前的
过渡安全上限；不得把系统工作集或private bytes直接当成live heap，也不得仅凭一次空闲采样擅自压低上限。
16MiB region保留现有大帧对象不进入humongous分配的保护，5分钟周期只允许G1执行并发周期以便空闲时归还
未使用堆；业务代码和任务热路径不得调用 `System.gc()`。

客户端在应用就绪、远程任务开始/停止及每5分钟记录一条轻量结构化快照，至少包含heap used/committed/max、
non-heap、direct/mapped buffer、live线程、注册/运行窗口及输入队列长度。该诊断只能读取JVM/现有内存计数，
不得截图、扫描文件、编码图片、改变任务phase、取得输入权或触发GC。参数变更只在客户端重启后生效；是否从
6GiB进一步降到4GiB，必须由fresh运行期间的live heap峰值和无OOM证据决定。

2026-10-20 G187 已实施：`config/client-jvm.options` 是唯一参数真源，本地重启脚本和jpackage构建脚本均
逐行读取并拒绝非法空白参数；`ClientMemoryMetricsService` 只读MXBean和现有计数，在应用就绪、任务生命周期
边界及每5分钟记录 `[client-memory]`，且诊断异常不会影响任务生命周期。340主源码已在独立 `.release-build`
目录编译通过；未测试、未重启，
当前正在运行的旧JVM仍使用旧参数，不能把源码完成写成空闲内存fresh验收通过。

## 2026-09-21 当前裁决：无大脑也可安装、打开界面及更新

用户明确要求本机发布、另一台软件内升级，并要求可 fresh run。客户端更新使用 GitHub 成品和已部署 Worker 公告，不等待购买大脑服务器。未配置大脑时启动器选择显式 `dhxy.update-only=true`：不启动截图组件、不连接输入设备、不允许游戏任务或运行会话；可以打开界面、查询公告、下载并确认更新。正常已配置的启动/驱动要求不变。此裁决覆盖下方早期“缺配置连界面都退出”的限制；不是假地址/token 授权，也不是取消正式运行授权。重开依然经根启动器。

## [BR-RELEASE-004] 本地客户端与云端大脑一对一服务

用户明确裁定：“一个本地客户端对应的是一个大脑，就是一对一的映射关系，一对一的服务关系。”
独立服务的单位是本地客户端，不是笼统的用户账号，也不是单个游戏窗口。每个客户端有专属大脑运行实例；
该实例只消费所绑定客户端的截图、配置、任务和回执，不能混用其他客户端的运行状态、记忆或数据目录。
同一个本地客户端可以管理多个游戏窗口，这些窗口继续由它的专属大脑处理，内部仍须按设备/窗口/请求区分。
不同大脑可以使用同一份程序版本、部署在同一台由维护者管理的服务器；不要求每个客户端各买一台服务器，
也不把大脑程序发给用户。不采用多个客户端共用同一运行中大脑的模式。
后续正式授权、路由和实例归属须贯彻此映射，不能把客户端自报标识直接当作授权证明。
本节为已批准边界。专属配置/目录锁/HTTP绑定代码已隔离编译，维护者登记与运行会话API已实现；正式实例尚未部署，客户端/大脑的运行授权消费、撤销、重连和回收仍未闭环。

实施采用新增正式实例配置入口，保留当前开发启动入口。实例目录固定为注册根目录下的客户端UUID，
工作目录为该目录的work，配置/logs/data/tmp均私有；进程持有专属目录锁直到退出，第二份同绑定进程不得启动。
维护者分配独立高熵token与端口，HTTP先校验专属token和客户端ID后才进入既有截图/任务消费者。
客户端ID由已有配置cloud.turn.device-id承载，正式模式必须为注册UUID；新增请求头只表示绑定声明，不能单独授权。
该绑定门与license/版本会话门是两回事，完整发版仍需后者。OCR沿现有生命周期，正式模式使用独立端口及私有工作目录；只读程序/模型可共用。

## 2026-09-20 最新状态（覆盖下方早期阶段描述）

- Client新增受信下载和外部安装代码，335源码隔离编译通过（未测试）：UI下载→RSA签名→SHA256/大小→再次确认退出→等待真实进程退出与安装锁→新版本目录→原子启动指针→重开。开发树/dirty本地包拒绝自动替换；配置/data复制，新版不开游戏任务。启动失败且已退出时恢复旧指针；新版进程仍活着却未就绪只报未确认，不强杀/不冒险回退。
- 固定公钥`src/main/resources/update-public-key.pem`；RSA3072私钥仅本机用户DPAPI加密存于`F:\CodexHome\dhxy-release-secrets\update-signing-key.dpapi`，不在Git。DER公钥SHA256 `cd4301a6684d50115419876e3e288b930c5281ec370aeec09742c72ffa8451a8`。不要重新生成来覆盖既有信任身份。
- G195 已把签名身份迁到 GitHub repository secret，并配置 `R2_ACCESS_KEY_ID`、`R2_SECRET_ACCESS_KEY`、`CLOUDFLARE_ACCOUNT_ID`、`CLIENT_POLICY_API_TOKEN`；Free 私有仓库不再依赖 `client-production` Environment 或 `CLIENT_PUBLIC_RELEASE_READY`。Secret 元数据已核存在，值不会从 GitHub 读回或打印。
- 发布链现在执行政策 revision 预检→签名 exact Gamma→不可变版本键上传私有 R2 并核远端大小/哈希 metadata→GitHub Release 归档→原子 CAS 公告 D1。相同键只允许精确同字节重试，绝不覆盖不同内容；`activate-version-policy.ps1`新增精确 manifest/package object key、SHA256 和大小，不清已有 allow/revoke/minimum/enforced。
- Cloudflare 已创建私有 `dhxy-client-updates`，D1 migration `0005_private_update_distribution.sql` 已备份后应用。Worker deployment `d5ba75e1-17e7-4601-9165-a5f692b96a22` 已加载 R2/D1 bindings 与两项加密 Secret；尚未公告任何 R2 成品，当前推荐版继续使用旧 GitHub 下载路径。
- **正式隔离边界已裁决**：按BR-RELEASE-004，一个本地客户端对应一个独立大脑，不是每账号一份或每游戏窗口一份。CloudBrainServer目前固定scope/host/runtime和开发bearer，还须落实客户端专属映射和正式授权；尚未改Cloud代码或部署。正式服务器和HTTPS入口仍待提供。
- G184保持未闭环。客户端没有重启，任何自动安装/签名消费/恢复/发布/禁版行为均未测试。此前276MB开发ZIP早于本轮代码且dirty，不能用来做正式Gamma或声称包含更新器。

### 新增脚本与使用限制

`initialize-update-key.ps1`仅一次性在owner机器生成签名身份；`configure-github-signing-key.ps1`只在Secret不存在时上传密文，已有Secret拒绝隐式替换。它使用本机已登录Git凭据，不另索取/打印个人token。Python PyNaCl1.6.2仅装在被忽略的`.release-build/g184-build-tools`供配置工具使用，不是客户端运行依赖。

正式启动入口始终是根`Start-DHXY.cmd`，不要绕过它直接运行各版本目录的DHXY.exe。根启动器持有`client-session.lock`直到Java/自己创建的截图组件退出；新程序在`versions/<版本>-<唯一id>/DHXY`。`current-version.json`只接受本安装的版本路径，`update-ready.json`仅作新版窗口就绪回执。`.updates/<唯一id>/installer-status.json`记录结果，旧版本和失败暂存保留，不自动删除用户文件。

## [BR-RELEASE-001] 客户端独立发布与同产物晋级

2026-09-18 用户批准先搭建发布基础。客户端是唯一用户下载安装对象，Cloud大脑独立部署。
2026-09-19 用户明确：dev是Alpha开发分支；合入master后形成Gamma候选；维护者确认Gamma验收后指定候选编号晋级Prod，不能重建或改写候选包。保留master为默认分支。
构建必须记录commit、唯一buildId及SHA256；日志、授权码、本机配置、Cloud源码/密钥不得入包。
Gamma与Prod使用同一成品，但连接配置在包外；测试配置不得复制到正式用户电脑。
没有正式分发、签名和版本政策消费者时，不允许声称用户自动更新已可用。

2026-09-19 继续实施授权：正式成品允许公开下载，软件运行仍需授权。文件分发采用同仓GitHub Releases；上传时先保存draft，包与元数据全部验完整才公开，不把发布文件等同于启用强制政策。当前允许先实现普通可选版的文件发布，required在服务器准入没有接通前明确阻断，不能只修改布尔值假装旧版已禁用。本轮不实际创建Release，不替用户验收Gamma。GitHub正式发布开关需维护者明确启用；下载/安装/签名和服务端准入仍是独立待接通环节。

## 本次实施范围

### [BR-RELEASE-003] 用户确认后的受信安装与旧版保留

2026-09-20 用户要求继续做完整自动更新链路。此次增量替代此前“仅打开下载页”的阶段边界：
生产包允许用户主动下载并确认更新，开发源码工作树不自动替换。维护者在受控Prod作业签名精确Gamma原包的清单，
客户端只信任包内固定公钥；原始清单载荷先验RSA/SHA256签名，再按固定产品/版本/平台/仓库与包大小/哈希验包。
私钥不能入Git/客户端。下载有体积和时间边界，不将任意URL或未签名清单作为执行授权。
独立安装器等待真实父进程退出并独占安装锁，解到新版本目录，保留旧程序与配置，不覆盖运行中DLL/JAR。
确认解包与配置复制完成才原子切换启动指针；失败保留原指针，不能强杀用户进程。重开仍禁止自动启动任务。
安装/退出功能只在用户明确点击确认时运行；未获测试授权仍仅隔离编译，不能据此宣布Gamma通过。
文档未规定实现格式，本轮选择“根启动器+versions版本目录+current-version指针”；不触及游戏业务时序。

### [BR-RELEASE-002] 授权服务版本政策与测试码兼容

运行准入实施细节（授权API已部署，Cloud/Client消费未接通）：正式实例由维护者登记clientId、专属token哈希与正式license归属；
客户端在已有授权电脑绑定完成后请求OPEN，许可/设备/版本/专属实例都匹配才签发256位随机会话凭证。
每客户端只保留一个当前会话，新登录替换旧会话；凭证数据库仅存哈希，客户端运行内存保存，不随安装包分发。
CHECK联结读取主库当前绑定、许可和政策，不缓存“允许”；版本固定在会话里，不能在后续请求换版本逃避禁版。
许可到期/禁用、解绑、实例停用、会话替换及版本撤销均不得通过。没有另外添加等一轮完成的会话宽限。
接口校验并不证明客户端二进制不可伪造；它不是远程设备证明。真正禁版闭环还需Cloud消费者和正在执行/排队动作的停止链。

会话消费实施采用既有LicenseAuthService登录入口和两条HTTP传输，不另建登录页面：正式实例UUID配置或安装包模式
成功verify且设备明确匹配后再OPEN，保存内存凭证；后续请求带X-DHXY-Session，Cloud专属入口CHECK成功才进入handler。
旧开发device-id仍保留本机开发流程，正式Cloud没有此旁路。前述新入口不是完整运行中撤销，不能据此启用required。
运行turn循环对明确的403授权/绑定/版本拒绝退出并保留错误给UI；未知403及普通网络失败不擅改其原有重试。
不得将授权拒绝伪造为任务FAILED/致命子任务事件来触发自动热重启。此单窗退出还须补即时全客户端停止与在途栅栏。
客户端即时撤权实施边界：先同步清内存许可，再由独立撤权线程广播全窗口撤销启动/恢复/旧handle/loop；网络线程不发送游戏输入。
撤权线程不得与被拒的transport worker互相等待；清理完成前不得激活新登录，清理失败则要求重启。输入队列先清，再收回窗口运行。
队列提交固化授权代际，Worker执行时绑定它；实际HID非中立报告在发送前校验许可及代际，旧输入不能借新登录恢复。
任务loop在创建时固化代际，执行线程及其观察、上传、维护子线程继承这一代际；旧生产者不得在新登录后读取新token或提交新代际输入。
撤权已禁止执行后，启动预检和已进入的窗口启动须在锁外确认退出，才允许再次登录；清理最多等待30秒，失败要求重启，期间不放行业务。
键鼠全松开/中立报告用于清理，撤权不阻止它们。撤权事件不含secret，过期请求的403只能撤回自己的会话，不能伤及新登录。
普通开发模式不新增许可限制；正式安装/专属UUID模式没有未登录输入旁路。
被撤销的较新版本也必须提示需更新到维护者当前允许的推荐成品，不能因版本号比较显示“没有更新”。
推荐目标若本身被禁或没有可信下载页，则明确说明缺可用包，不提供无效安装。用户可暂不下载，但不恢复被撤销的运行权限。

2026-09-20 用户裁定强制更新行为：普通发布继续允许仍受支持的旧版运行；维护者明确禁用某版本时，
立即撤销该版本的新请求与既有任务/会话权限，不等当前轮完成，不人为增加宽限期。客户端收到明确禁版结果后，
停止任务与后续输入、取消待执行动作，不按普通断网无限重连；保留界面及更新下载入口，显示“此版本已停用，请更新”。
服务端必须执行限制，不能只依赖客户端弹窗。更新通过后重新授权，任务不自动恢复。
正式大脑的运行撤权接线：同实例只保留一个当前运行会话，许可撤销后不可复活；新登录替换旧会话前先停止旧运行并撤回未决命令。
每个HTTP入口及最终回复重新CHECK；后台单路每2秒检查已有会话，用于没有新请求时发现明确撤销，不作“允许”缓存。
明确拒绝触发停止；检查网络不可用时回复仍拒绝执行，不把普通网络错误冒称已禁版。网络传播/检查耗时应在后续授权验收时实测。
这里的立即是撤销生效后不再允许业务执行，不承诺跨网络零延迟，也不能撤回已经完成的物理输入。
无需为了禁用旧版停掉允许版本或所有大脑；全站维护停服与版本撤销分开。该决定已批准，但运行中撤销接线尚未完成，
当前仍不启用禁版、不发布未验收Gamma。

2026-09-19 用户确认只有本人测试，并允许修改/部署授权服务。升级前3条测试码保留值、绑定和有效期，
加软件归属后显式标记legacy-test，仅用于延续历史测试用法；正式码按dhxy或dhxy-auto-battle分开。
版本政策按软件独立保存，默认关闭、无推荐版本、无禁版，不影响目前测试。允许配置最低版本、精确撤销、
允许版本清单与推荐版本链接；撤销优先，启用时缺版本/格式错误拒绝，legacy-test先明确正式归属才可通过。
verify/status均消费同一政策；状态刷新也需客户端版本，不能只在首次验证检查。解绑不因旧版本被挡。
客户端说明页手动检查、授权验证成功后的新正式版本提示共用真实政策读取；允许稍后，开发版不自动提示。
当前只打开固定仓库对应版本的下载页，绝不将此入口标成已实现自动下载安装/重启或强制任务门。
管理只走维护者已认证的Cloudflare CLI，不新开无认证管理HTTP。当前不发明Cloud会话/任务入口已接通的事实；
授权API拒绝旧版不等于能阻止绕过验证的旧客户端运行。自助renew本地旧代码不随本次上线，保持线上无此路由。

**最新实施（2026-09-19）：** `client-gamma.yml`的push/手动分支门、`prepare-promotion.ps1`的来源门、Prod执行门均已对齐master。新增`publish-client.ps1`与独立contents:write作业：维护者勾选公开发布后才运行。普通准备作业仍只读；required正式发布明确拒绝。本轮尚未合并、提交、推送或运行GitHub Actions。Prod是发布渠道，无需新增Git分支。

ZIP是自动构建后成品的运输格式，不是要求维护者先手工打ZIP再上传源码。源码仍正常push到Git；流水线负责编译、组装程序/运行时/资源、打包及上传成品。可另选安装器格式，自动更新器也可在后台下载解包；单独上传Java源码不会让用户获得可运行客户端。

已编写客户端构建、候选留存、人工晋级准备及公开成品分发。运行时UI更新提示、下载/替换/重开、
授权API版本政策已部署，Client检查提示已编译；自动安装/签名与Cloud任务/会话准入是后续接线，不修改正在运行的任务。
当前仓库默认分支为master、工作分支为dev；网页手动按钮需要工作流存在于默认分支。
不自动合并dev到master、不改默认分支、不改变仓库可见性、不发布Prod。

## 尚待确定

- 用户已确认正式包公开下载、运行仍需授权；源码仓可见性保持原样。
- 用户已确认大脑目前只在本地，正式Cloud服务器尚未部署；不得将127.0.0.1打成正式服务地址。
- 用户确认只有本人测试后，完整备份再迁移，3条码标legacy-test保留原用法；授权Worker已部署。政策默认关闭，无禁版。
- 文件发布用同仓Actions短期GITHUB_TOKEN，不给客户端GitHub凭据。授权API已有版本政策，但Cloud任务/会话门仍未接；成品签名、自动安装未接，公开ZIP不意味着自动更新已完成。
- 保留禁止自动测试要求；本轮仅静态审查/语法与隔离编译，不能标为Gamma已验收。

## 已写入的入口

| 文件 | 职责 |
|---|---|
| `.github/workflows/client-gamma.yml` | master的生产源码/模板/发布配置push自动构建；也可手动选择master；dev不生成Gamma |
| `.github/workflows/client-prod.yml` | master手动运行；选择Gamma runId/attempt、明确验收；默认只准备，勾选公开发布才进入写权限作业 |
| `scripts/release/build-client.ps1` | Java21隔离编译、同盘新目录、JAR依赖、jpackage运行时、生产模板、WGC组件、统一生成包内两处版本、SHA256 |
| `scripts/release/prepare-promotion.ps1` | 校验同仓master/指定工作流/成功run及attempt、精确artifactId、manifest身份、包大小/哈希；不解压执行候选 |
| `scripts/release/publish-client.ps1` | 再核同一run/attempt/来源/原包哈希；验证仓库公开；新建draft，完整上传三文件且核对GitHub资产digest，最后公开并置latest；不改授权 |
| `release/version.txt` | 人类版本号；构建ID=`runId.attempt`，重跑不冒用旧包 |
| `release/requirements-build.txt` | 固定截图组件运行依赖与打包工具，不在用户机器现场pip安装 |
| `release/Start-DHXY.cmd` / `.ps1` | 仅启动客户端及本包截图组件，固定工作目录；禁止自动启动任务/本地Cloud；退出仅收回自己创建的截图进程 |

当前提供完整目录ZIP（portable app-image），不是已签名MSI安装程序。jpackage含Java运行时，WGC以
PyInstaller onedir打包；无需用户另装Java/Python，但仍需用户事先安装已批准的FakerInput驱动。
输入后端/匹配算法/任务语义均未改动。客户端堆参数现由 `BR-RELEASE-005` 的共用真源管理，不再保留旧2GiB初始堆；16MiB G1 region继续保留。
连接地址及每用户token在外部config内配置；没有Cloud地址/token时启动包装脚本明确报错，不偷偷启动本地大脑。
开发者本机config/application.properties、日志、data、帧截图不入包；包内三个受控JSON为Git跟踪的既有配置种子。

## GitHub Free 启用与桥接步骤

1. 审核并提交本卡发布文件到dev。不要把其它卡的dirty改动自动混入这次提交。
2. 开发完成且维护者明确同意进入Gamma后，将相应dev代码合入master，包含全部发布配置与构建输入。
   默认分支仍是master；本轮没有替用户决定何时合入，也不能只把workflow挪过去就假称旧master业务代码已更新。
3. 确认仓库Actions权限允许官方固定SHA的Actions。构建/准备只读；publish作业单独需要contents:write，
   使用GitHub自动提供的短期token。当前源码仓为public，不因本卡改变可见性。
4. Actions→客户端Gamma候选包→Run workflow→选择master。合入master也会按路径触发构建；dev不会。
   构建不会运行单测、客户端、截图组件或游戏；PyInstaller仅分析和打包脚本。
5. 在该运行页Artifacts下载`client-candidate-<attempt>`；解出内部客户端ZIP，再解压到新目录，
   按README填写Gamma连接配置。实机验收由用户另行启动，不因打包成功自动获准。
6. 验收后Actions→客户端Prod发布，在master填写候选runId/attempt，勾选人工确认。
   默认只输出`promotion-request.json`与原包，状态为PREPARED_ONLY，不会重新构建。
7. Prod 必须勾选正式晋级并输入精确确认词 `PUBLISH-ACCEPTED-GAMMA`；只有账号 `yueyunfe21`、master、exact Gamma provenance 同时成立才运行。required仍须另行批准，不因文件可下载自动禁旧版。
8. publish 将同一签名成品放入私有 R2 并保留 GitHub Release 归档；桥接期仓库仍公开，旧客户端继续使用 GitHub，新客户端看到 `authorized-r2` 后走 Worker update-ticket。D1 只在两端资产完整核验后 CAS 公告。
9. 桥接客户端完成迁移前不得把仓库改为 private。迁移完成后，用户无需 GitHub 账号；GitHub Release 仅供维护者历史归档，用户点击更新后由客户端自动提交 DPAPI 保护的既有授权并下载 R2。

## 2026-09-19 实际上线记录

授权地址`https://dhxy-license-worker.yueyunfe.workers.dev`，部署版本`79649e1f-0328-42da-b1af-7c1e19d27e7f`（100%）。
旧版及数据库完整备份位置见G184卡。0002/0003已迁移；两应用revision=0/enforced=0，无推荐版/禁版。
服务端src/index.ts与version-policy.ts、迁移、管理脚本实际位于`D:\mavenProject\dhxy-license-worker`，该目录尚非Git仓。
Client说明页检查更新/验证授权成功后提示已编译未重启：当前没正式版时明确显示“尚未公布”，不会谎称已是最新版。
管理脚本只做语法检查，没有运行政策apply；没有授权/更新行为测试。当前已运行的开发Client不受这次本地改动影响。

发布失败不删除或覆盖已有tag/Release；部分上传保留未公开draft，交维护者检查。重跑遇已有同名tag/Release明确中止，
不自动删除资产或强推tag。若最后公开请求超时，先在网站确认实际状态再处理，不盲目重复发布。
上传完整性依赖GitHub返回资产`digest=sha256:...`；缺失或不一致即拒绝公开，不把缺失当通过。

候选与准备制品保留30天（不能超过仓库的上限）；过期需重新构建并重新验收。
SHA256用于完整性核对，不是发布者签名；不能借此声称已具安全自动更新。
Prod准备中的required只记录维护者意图，公开作业拒绝该模式；不会在尚未实现的服务端假称禁用旧版。

## 当前验证边界

不新增/运行测试；Maven compile/JAR/jpackage/PyInstaller是编译打包，不启动产品。
有运行中的JVM也必须使用`.release-build/<唯一目录>`，Windows Maven输出与源码须同盘，
不能重写target/classes。本地`-AllowDirty`仅给打包检查，manifest标dirty=true，Prod准备会拒绝。
GitHub成功run的来源校验必须待工作流上传后才能实证，本轮未运行GitHub Actions。

依据：[GitHub制品与跨运行下载](https://docs.github.com/en/actions/tutorials/store-and-share-data)、
[Java应用打包](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html)、
[GitHub Release及资产接口](https://docs.github.com/en/rest/releases/releases)。
