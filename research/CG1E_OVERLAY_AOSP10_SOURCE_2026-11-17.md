# CG1E: overlay enforcement and enabling on AOSP Android 10 (source reading)

Date: 2026-11-17. Branch: `android10-release` (raw.githubusercontent.com, aosp-mirror/platform_frameworks_base); tag `android-10.0.0_r47` tried where the branch 404'd.

## Important limits of this report

- The fetch tool returned a model-written summary of each file, NOT the raw file. The "verbatim" quotes below are the summary's quotes of the code, not a byte-exact copy. Treat them as pointers to verify, not as citations.
- No line numbers could be obtained. Functions are named instead.
- Several URLs returned HTTP 404 on both the branch and the tag, on raw.githubusercontent.com and android.googlesource.com alike: `cmds/idmap2/libidmap2/ResourceMapping.cpp`, `core/res/res/values/overlayable.xml`, `services/.../OverlayManagerServiceImpl.java` (raw, branch), `services/.../IdmapManager.java` (raw), and `packages/SystemUI/res/values/overlayable.xml` (tag only, not tried on the branch).
- The GitHub directory listing of `core/res/res/values` on `android10-release` shows NO `overlayable.xml` and no file containing "overlay". The GitHub listing of `cmds/idmap2/libidmap2` on the same branch shows NO `ResourceMapping.cpp`. So the 404s are most likely real absences on this branch, not fetch failures.
- The `OverlayManagerServiceImpl.java` section and the `IdmapManager.java` section were fetched through googlesource (`?format=TEXT`, base64), which the summariser decoded. Those are the best-supported sources here.
- Nothing was run on a device. Everything below is reading, not a test.

## 1. `cmds/idmap2/libidmap2/ResourceMapping.cpp`

URL tried: `https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android10-release/cmds/idmap2/libidmap2/ResourceMapping.cpp` (404). Tag variant also 404. Directory listing of `cmds/idmap2/libidmap2` on the branch: file not present. The overlayable check lives in `Idmap.cpp` on this branch instead (section 2).

Reading: nothing to quote from this path on Android 10. Ignore the file name when reading later branches (it appears in later AOSP versions).

## 2. `cmds/idmap2/libidmap2/Idmap.cpp` and `Policies.cpp`

URLs: `.../android10-release/cmds/idmap2/libidmap2/Idmap.cpp`, `.../android10-release/cmds/idmap2/libidmap2/Policies.cpp`.

Default policy bitmask (`CheckOverlayable`, Idmap.cpp), as quoted by the summary:

```cpp
  static constexpr const PolicyBitmask sDefaultPolicies =
      PolicyFlags::POLICY_ODM_PARTITION | PolicyFlags::POLICY_OEM_PARTITION |
      PolicyFlags::POLICY_SYSTEM_PARTITION | PolicyFlags::POLICY_VENDOR_PARTITION |
      PolicyFlags::POLICY_PRODUCT_PARTITION | PolicyFlags::POLICY_SIGNATURE;
```

Note: `POLICY_PUBLIC` is NOT in the default bitmask.

Entry point and gating (Idmap.cpp, `Idmap::FromApkAssets`):

```cpp
bool enforce_overlayable) {
...
    if (enforce_overlayable) {
      Result<Unit> success =
          CheckOverlayable(*target_pkg, *overlay_info, fulfilled_policies, target_resid);
      if (!success) {
        // log and skip this resource (continue)
```

`fulfilled_policies` is a parameter; this file never sets it. Its caller is not in this file.

`CheckOverlayable` decision, as summarised:
- If the target package does not define any overlayable (`!target_package.DefinesOverlayable()`): allowed only if `(sDefaultPolicies & fulfilled_policies) != 0`.
- If the resource is declared overlayable: refused when `(overlayable_info->policy_flags & fulfilled_policies) == 0`.

Policy name table (`Policies.cpp`, `kStringToFlag`): odm, oem, public, product, signature, system, vendor. No "actor" entry on this branch. `PoliciesToBitmask` returns an error for an unknown name.

Reading: an overlay with only `POLICY_PUBLIC` fulfilled is refused for every resource of a target that defines no overlayable, because PUBLIC is not in the default mask. A resource is only overlayable for such an overlay if the target declares it with a `public` policy.

## 3. `services/core/java/com/android/server/om/IdmapManager.java`

URL: `https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android10-release/services/core/java/com/android/server/om/IdmapManager.java?format=TEXT` (base64, decoded by the summariser). Raw URL on the branch: 404.

Summary of `calculateFulfilledPolicies` (no line numbers):
- starts with `POLICY_PUBLIC`;
- adds `POLICY_SIGNATURE` if `signaturesMatching(...)` is true;
- adds `POLICY_VENDOR_PARTITION` if the package is vendor; `POLICY_PRODUCT_PARTITION` if product; ODM and OEM likewise; product services adds nothing extra;
- adds `POLICY_SYSTEM_PARTITION` if the package is a system app.

Summary of `enforceOverlayable`:
- if the target's targetSdkVersion >= Q (29), it returns true (enforce);
- for a vendor overlay on a pre-Q platform, returns `VENDOR_IS_Q_OR_LATER`;
- otherwise returns `!(ai.isSystemApp() || ai.isSignedWithPlatformKey())`.

Summary of `createIdmap` failure: logs `failed to generate idmap for ... : <message>` and returns false. `removeIdmap` does the same. Binder reconnect logic exists.

Reading: a user-installed overlay, not signed with the platform key and not a system app, gets fulfilled policies = {PUBLIC} only (per this summary). The summary does not say which package `enforceOverlayable` is called with, so whether enforcement applies to `android` and to SystemUI in this way is only partly determined: both have targetSdk 29, so the first branch suggests enforcement applies.

## 4. `services/core/java/com/android/server/om/OverlayManagerServiceImpl.java`

URL used: googlesource `?format=TEXT` (base64, decoded). Raw URL on the branch: 404.

Summary of `updateState`: if the target package exists and the overlay exists and (target is not `android` or the overlay is static), it calls `mIdmapManager.createIdmap(...)`, then `calculateNewState`.

Summary of `calculateNewState` (order):
1. target-being-replaced / overlay-being-replaced flags return the matching `*_BEING_REPLACED` state;
2. `targetPackage == null` -> `STATE_MISSING_TARGET`;
3. `!idmapExists(overlay)` -> `STATE_NO_IDMAP`;
4. static overlay -> `STATE_ENABLED_STATIC`;
5. otherwise `enabled ? STATE_ENABLED : STATE_DISABLED` from the stored enabled flag.

Reading: for a non-static overlay with an existing target and an existing idmap, the state follows the stored enabled flag only. The summary shows no check on the target's name (`com.android.systemui` or `android`) and no check on the overlay's signature or install location in this function. Any refusal must come from the idmap step or from the package manager.

## 5. `core/res/res/values/overlayable.xml` (framework-res, android10-release)

Raw and googlesource URLs: 404. Directory listing of `core/res/res/values` on `android10-release`: no `overlayable.xml`, no file containing "overlay" (the list ends with a "View all files" link, so it may be partial, but this file name would show near `public.xml`).

Reading: on Android 10 as read here, framework-res declares NO overlayable block. `android:drawable/ic_mode_edit` is therefore NOT listed in any overlayable block on this branch. The `public` policy question does not arise from this file. Under `CheckOverlayable`, a target with no overlayable definitions allows only policies in `sDefaultPolicies`, which does not include PUBLIC.

## 6. `packages/SystemUI/res/values/overlayable.xml`

Tag `android-10.0.0_r47`: 404. Branch not tried for this path.

Reading: NOT VERIFIED on this branch. The tag 404 is the only evidence. Do not assume SystemUI has no overlayable declarations until the branch listing of `packages/SystemUI/res/values` is checked.

## 7. `services/core/java/com/android/server/om/OverlayManagerService.java`

URL: googlesource `?format=TEXT` (base64, decoded). Raw URL on the branch: 404 in the first attempt; this decode succeeded.

Summary (no line numbers):
```java
private void enforceChangeOverlayPackagesPermission(@NonNull final String message) {
    getContext().enforceCallingOrSelfPermission(
            android.Manifest.permission.CHANGE_OVERLAY_PACKAGES, message);
}
```
- `setEnabled(packageName, enable, userId)` calls `enforceChangeOverlayPackagesPermission("setEnabled")`, then `handleIncomingUser`, returns false if the package name is null, otherwise calls `mImpl.setEnabled`.
- `setEnabledExclusive` does the same with `"setEnabledExclusive"`, and returns false when `enable` is false.
- The file has no overlayable or protected check. Signature checks are not called here (they are in the Impl class).

Reading: the only gate in this file is the CHANGE_OVERLAY_PACKAGES permission. Whether the calling identity holds it is decided by the platform permission grant (see section 8).

## 8. `packages/Shell/AndroidManifest.xml` (optional)

URL: `https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-10.0.0_r47/packages/Shell/AndroidManifest.xml` (tag). The summary reports:

`<uses-permission android:name="android.permission.CHANGE_OVERLAY_PACKAGES" />`

Reading: the shell package declares CHANGE_OVERLAY_PACKAGES. That is the permission the `cmd overlay` path needs. Whether a shell-uid process actually holds it at run time (protection level, signature/system grant) was NOT read here. The tag was used, not the branch.

---

## CONCLUSION (cannot be fully determined from these reads; the reasons are in the sections above)

1. `cmd overlay enable` for a non-static overlay is accepted by OverlayManagerServiceImpl once the target exists and an idmap exists (sections 4, 7, 8). The shell declares CHANGE_OVERLAY_PACKAGES (section 8). So enabling is likely accepted for `com.android.systemui` and for `android`, provided the idmap step succeeds. Not verified on a device.
2. The idmap step runs the overlayable check for an overlay whose fulfilled policies are {PUBLIC} only (section 3, sections 2 and 5).
3. For target `android`: framework-res on android10-release declares no `<overlayable>` (section 5), and the default policy mask does not contain PUBLIC (section 2). So a user overlay that is not platform-signed and not a system app is refused for every framework resource. The enable can be accepted while no resource is replaced. Conclusion: cannot overlay any `android:` resource, including `android:drawable/ic_mode_edit`, unless the overlay is platform-signed or a system app. Medium confidence (the enforceOverlayable argument is not confirmed).
4. For target `com.android.systemui`: the SystemUI overlayable declarations were NOT verified on the branch (section 6). If SystemUI declares no overlayable, the same reasoning gives no mapped resources. Cannot determine until `packages/SystemUI/res/values` on android10-release is listed.
5. `ic_mode_edit` is not in any overlayable block on this branch (section 5): not listed.
6. Whether `pm install` by the shell user gives the overlay an "approved" state is NOT read here (the Javadoc mentions approval; the code path was not fetched).
7. Whether `cmd overlay enable --user 0` as the shell user reaches setEnabled with the permission held at run time: cannot determine from these reads (section 8 covers only the manifest declaration).
8. The idmap-creation failure path (`createIdmap` returns false) is logged and returns false; whether a failed idmap leaves the state at NO_IDMAP was read in section 4 only as the calculateNewState order.
9. Device test needed: install a test overlay with `pm install`, run `cmd overlay enable --user 0 <pkg>`, then `cmd overlay list` and `dumpsys overlay`, for both targets, and check the resource result.
