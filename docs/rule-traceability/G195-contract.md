# G195 contract（未运行）

- 私有 GitHub 不是正式用户下载依赖；旧客户端迁移完成前保留公开桥接 Release。
- Prod 只晋级已验收 exact Gamma 字节；R2 远端核验成功后才允许 D1 revision CAS 公告。
- update-ticket 校验许可、设备和目标政策，不要求 runtime session；被禁旧版只可下载当前允许的推荐目标。
- 客户端首次成功授权后以 Windows DPAPI 保护更新凭据；无法解密或未曾授权时明确要求重新授权，不公开下载。
- 短期 R2 URL 不展示、不落盘、不记录；固定公钥签名、SHA256 和大小仍是安装信任边界。
- 本卡没有测试授权：`NOT RUN`。
