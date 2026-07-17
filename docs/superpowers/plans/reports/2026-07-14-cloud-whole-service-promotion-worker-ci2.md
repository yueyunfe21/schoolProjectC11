# W-696-PROMOTE-COHORT-I1

- Worker: CI2
- Status: CLAIMED
- Scope: 6 个 Cloud active Service 文件
- Preflight: 6/6 通过
- 规则：替换前 active blob 同时匹配 manifest `activeBlob` 与 `preservedBlob`；baseline 文件 blob 匹配 manifest `baselineBlob`，并匹配 DHXY `696a12b0:<path>`。
- 替换：使用 `migration-baseline/696a12b0` 对应完整源逐字节替换。
- 编译/tests/runtime: 未执行（按任务要求）。
- Git mutation: 未执行。

| path | before | preserved | baseline | after | SHA256 | BAD |
|---|---|---|---|---|---|---:|
| `src/main/java/com/bot/dhxy/service/AutoCombatPanelService.java` | `de152bf5256839832c9d692e927e4f7fa0d6a126` | `de152bf5256839832c9d692e927e4f7fa0d6a126` | `bf63d2c78873afd8a0781d97f080a59b2b327942` | `bf63d2c78873afd8a0781d97f080a59b2b327942` | `72b5846d250e5389336abd8cb416c0e0a5877f1eb39d4e970f032c1b838e4d67` | 0 |
| `src/main/java/com/bot/dhxy/service/AutoCombatService.java` | `86918d3eb0d97ca2e41d8c947afe415ff2c39875` | `86918d3eb0d97ca2e41d8c947afe415ff2c39875` | `b1c2d48e89ed6b2ca90b1639df841dd7a97d691a` | `b1c2d48e89ed6b2ca90b1639df841dd7a97d691a` | `b4828408ce624b0f7c7b656cf73a76103f059371ad2be02598929e5aa328a24d` | 0 |
| `src/main/java/com/bot/dhxy/service/BattleRadarService.java` | `fa62ba0b55d1ad2f65522ebcbb53e384a1ed6826` | `fa62ba0b55d1ad2f65522ebcbb53e384a1ed6826` | `c5840e599795f9c6905d692884cd38265e653b6f` | `c5840e599795f9c6905d692884cd38265e653b6f` | `f224f83a723a9b0741b909301bed030bddf31c7de25a4f3db65b0fec4856dd29` | 0 |
| `src/main/java/com/bot/dhxy/service/CommonBoxService.java` | `df2b9390dedad7fe4d16d0f5aa86a6b6e464a0a2` | `df2b9390dedad7fe4d16d0f5aa86a6b6e464a0a2` | `195c1dbfef052ddaf87ff40c6c85cba862be91f6` | `195c1dbfef052ddaf87ff40c6c85cba862be91f6` | `f49a6ec634a918aa9b4ba72735c055df099cbec76e0218c7a32c211fd26f4892` | 0 |
| `src/main/java/com/bot/dhxy/service/LeftTopStatusSwitchService.java` | `e17fd99bac759c590f4d69a0750fe0fa3d4ce41b` | `e17fd99bac759c590f4d69a0750fe0fa3d4ce41b` | `a46fde69e7d11bca315b75600fd737ef7f924912` | `a46fde69e7d11bca315b75600fd737ef7f924912` | `3bf00fc550017b98f2384ce597e4dcd2e8a04045c9998a1e153b489ccd01e982` | 0 |
| `src/main/java/com/bot/dhxy/service/MapNameCanonicalizer.java` | `4342636913315f6dbdd182254a7a927b8581d72d` | `4342636913315f6dbdd182254a7a927b8581d72d` | `25ec2713784ee9e2bb2b1aa121cf018ee8ba37b4` | `25ec2713784ee9e2bb2b1aa121cf018ee8ba37b4` | `6a8e3aaa6d4fb343ae562d15ebbd01e05b53f9663b210d6518e28bdd49936c36` | 0 |

结论：BAD=0；除本报告外未写其它文件，未执行 Git mutation。

## Parent Copy Review #1 - 2026-07-14T11:36:30-04:00

**APPROVED，P0/P1/P2=0。** 父级重新计算六个 active target 的 `git hash-object`，逐一等于
`696a12b0:<path>` 与 mirror baseline blob；全树 exact 计数由 16 增至 22。替换前六个 active blob 均与
preservation snapshot 一致，未踩并行新写入。此结论只批准原字节 promotion，不代表 Cloud 已可编译或 Service
完成迁移计数。
