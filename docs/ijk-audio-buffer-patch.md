# ijkplayer 音频缓冲补丁（Android 5 蓝牙断续）

## 现象

目标设备 Android 5.0.2，连蓝牙耳机（A2DP）看 720P 在线视频：

- 画面正常，只有声音反复中断，听几句就没声，过一会儿自己恢复；
- 用外放播同一个视频完全正常；
- 蓝牙先连好再进播放页，也会出现。

## 定位过程

应用层不碰蓝牙：全工程没有音频焦点请求、没有音频设备变化广播的监听、没有设 AudioManager 模式、清单里没有蓝牙权限。声音完全交给 ijkplayer 输出，上层只有音量键调 `STREAM_MUSIC`。

播放器设置里「音频输出」有两个实现（OpenSL ES / AudioTrack），默认 AudioTrack。反汇编 `libijksdl.so` 的 `SDL_Android_AudioTrack_new_from_spec` 可见缓冲计算：

1. 请求的缓冲帧数不足 4000 帧就翻倍，直到至少 4000 帧；
2. 调 `AudioTrack.getMinBufferSize` 取系统最小缓冲；
3. 实际传给 AudioTrack 的 `bufferSizeInBytes` 是系统最小缓冲的 **2 倍**。

外放时系统最小缓冲小，2 倍够用。A2DP 链路的编码与传输缓冲更长，2 倍余量覆盖不住一个编码周期，写入周期性断供（underrun），表现为声音停几句、缓冲重新积累后又恢复，画面不受影响。

## 改动

把缓冲倍数从 2 倍改成 4 倍。只改两个 ARM ABI 的 `libijksdl.so`，x86 不涉及（目标设备是 ARM，且沙箱里的 binutils 不支持 i386 反汇编）。

| ABI | 文件偏移 | 原字节 | 新字节 | 指令 |
|---|---|---|---|---|
| armeabi-v7a | 0x1247E | `64 00` | `A4 00` | `lsls r4, r4, #1` → `lsls r4, r4, #2` |
| arm64-v8a | 0x1ADF8 | `18 7B 1F 53` | `18 77 1E 53` | `lsl w24, w24, #1` → `lsl w24, w24, #2` |

改动的是 `SDL_Android_AudioTrack_new_from_spec` 里紧跟在 `getMinBufferSize` 返回值之后的那一次左移，它算出的值直接作为 AudioTrack 构造函数的 `bufferSizeInBytes`。同函数里还有一处左移（把请求帧数抬到 4000 帧以上），**没有**改。

## 代价

音频输出多缓冲一倍，延迟增加约 2 × minBufferSize（几十毫秒量级）。看视频无感，但如果以后做需要低延迟的音频场景要重新权衡。

## 回退

```bash
git checkout -- app/libs/armeabi-v7a/libijksdl.so app/libs/arm64-v8a/libijksdl.so
```

原始文件另存于 `/workspace/tools/ijksdl-orig/`（沙箱内，不入库）。

## 验证方式

真机：连蓝牙耳机，播同一 720P 视频，观察是否还周期性断音。若仍断，下一步按顺序试：

1. 设置 → 内置播放器设置 → 音频输出 → 切到 OpenSles，对比；
2. 若两条路径都断，需要评估绕开 ijk 音频输出的方案（独立音频通道）。