# W-696-PROMOTE-COHORT-I2

## Result

- Role: Internal Worker CJ2 (implementer, not reviewer)
- Status: CLAIMED
- Scope: active whole-Service promotion, phase 1
- Preflight: 6/6 passed
- Replacement: 6/6 Cloud active files replaced byte-for-byte from `migration-baseline/696a12b0`
- Final verification: `BAD=0`
- Compile/tests/runtime: not run, per task instruction
- Git mutation: none

## Hash Report

| path | before activeBlob | preservedBlob | baselineBlob | after blob | SHA256 | BAD |
|---|---|---|---|---|---|---|
| `src/main/java/com/bot/dhxy/service/MemoryService.java` | `9a349c5091db65b9beb7d65c7fc0d1572cd4caec` | `9a349c5091db65b9beb7d65c7fc0d1572cd4caec` | `51cf0508179c9eb9b31d09284b58588903bb73f5` | `51cf0508179c9eb9b31d09284b58588903bb73f5` | `c227e353b597f67fce6544de1046559e8a233814132fac1dc0115055dcbbb94b` | `0` |
| `src/main/java/com/bot/dhxy/service/PlayerStateService.java` | `7e76c6581a3f1a2adce55b8e1eb4a5d2decb2990` | `7e76c6581a3f1a2adce55b8e1eb4a5d2decb2990` | `096d8917b0372422b3ed141300419f9b71c1392c` | `096d8917b0372422b3ed141300419f9b71c1392c` | `a0aa395bec716e6dff6ee45d42ba1cac198504af280907eeba73a7335f546ff9` | `0` |
| `src/main/java/com/bot/dhxy/service/ReturnItemPrescanService.java` | `f76196b7562060c95346e4b143ebb5e57f948f75` | `f76196b7562060c95346e4b143ebb5e57f948f75` | `c8f6d0b3a8c99357eff7391af5c90aaffcd4b057` | `c8f6d0b3a8c99357eff7391af5c90aaffcd4b057` | `f91110edbf2f75310c7ff62229e78d0ddf043382eb82b77fa3bc008f3355bcbf` | `0` |
| `src/main/java/com/bot/dhxy/service/SummonSkillService.java` | `2fa710da9b5fa11a5eb674ebd34f376c4097b7d6` | `2fa710da9b5fa11a5eb674ebd34f376c4097b7d6` | `d8afb9e2f97aba9522393bd9a21d0cc4c48ed324` | `d8afb9e2f97aba9522393bd9a21d0cc4c48ed324` | `935003ce2b8a167e6d2628830bc7dad7e2d78738e56cdc037821fec9e6e6cc2a` | `0` |
| `src/main/java/com/bot/dhxy/service/TeamReturnService.java` | `6c4a9cbf984e1d24006a84254da734a20022106a` | `6c4a9cbf984e1d24006a84254da734a20022106a` | `286c5a85f01d010e883f8c4321ea1793776c932f` | `286c5a85f01d010e883f8c4321ea1793776c932f` | `d3f8c5c1c152d267a4f098f2bebced99162b09433212cb18c1b5fc91ecc8af7d` | `0` |
| `src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java` | `d0594100f9dbb009f29beb17ed996446f5ae4f97` | `d0594100f9dbb009f29beb17ed996446f5ae4f97` | `43571303fea2fcbc35a99b16818d5cd0408be282` | `43571303fea2fcbc35a99b16818d5cd0408be282` | `a64c0b417c7cd9b4e7beea2a33c8347bb44c036f22dfe65e087a4449d9de1024` | `0` |

Each final `after blob` was also verified equal to `git rev-parse 696a12b0:<path>` in `D:\mavenProject\DHXY`.

## Parent Copy Review #1 - 2026-07-14T11:36:30-04:00

**APPROVED，P0/P1/P2=0。** 父级重新计算六个 active target 的 `git hash-object`，逐一等于
`696a12b0:<path>` 与 mirror baseline blob；合并 cohort I1/I2 后 active exact 为 `28/32`。替换前六个 active
blob 均与 preservation snapshot 一致，未踩并行新写入。此结论只批准原字节 promotion，不代表 Cloud 已可编译
或 Service 完成迁移计数。
