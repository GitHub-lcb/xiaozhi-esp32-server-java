#!/usr/bin/env bash
# =============================================================================
# xiaozhi-esp32-server-java · 后端一体化脚本（单文件，可独立执行，不再 source 任何文件）
#
# 原 9 个脚本（scripts/_common.sh、download_base/stt/tts/models.sh、bin/_common.sh、
# server/dialogue/all.sh）已合并进本文件并删除，行为与原脚本保持一致：
#   scripts/_common.sh  scripts/download_base.sh  scripts/download_stt.sh
#   scripts/download_tts.sh  scripts/download_models.sh
#   bin/_common.sh  bin/server.sh  bin/dialogue.sh  bin/all.sh
#
# 一、一次性部署（下载模型/原生库 → 编译 → 启动，一条命令走完）
#   ./xiaozhi.sh init              # dev 环境
#   ./xiaozhi.sh init prod         # 生产环境
#
# 二、按需单独使用
#   ./xiaozhi.sh download [all|models|vad|jni|vosk-lib|libs|base]
#   ./xiaozhi.sh download stt [sensevoice|small|standard|none]
#   ./xiaozhi.sh download tts [vits-melo|matcha|none]
#   ./xiaozhi.sh clean    [all|models|stt|tts|base|libs]
#   ./xiaozhi.sh status                        # 模型 + 原生库 + 服务一起看
#   ./xiaozhi.sh build    [all|模块名]
#   ./xiaozhi.sh start|stop|restart [dev|prod]
#   ./xiaozhi.sh logs
#   ./xiaozhi.sh server   <start|stop|restart|status|logs> [dev|prod]
#   ./xiaozhi.sh dialogue <start|stop|restart|status|logs> [dev|prod]
#
# 三、环境变量
#   TARGET_PLATFORM=linux-x64      指定下载平台（默认自动探测）
#   DOWNLOAD_PROXY=http://...      下载走代理
#   JAVA_HOME / JAVA_BIN           指定 JDK 21+（宝塔等不在 PATH 的场景）
#   SPRING_PROFILES_ACTIVE=prod    默认运行环境
#   SKIP_PREFLIGHT=1               init/start 跳过启动前自检
#   SKIP_DOWNLOAD=1                init 跳过模型/原生库下载
#   SKIP_BUILD=1                   init 跳过编译
#   XIAOZHI_DEVICE_AUTH_SECRET     设备握手鉴权密钥（生产必设，server 与 dialogue 必须一致）
# =============================================================================

set -e

# ============================================================
# 路径与配置
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
ROOT_DIR="$PROJECT_DIR"
LOGS_DIR="$ROOT_DIR/logs"

# SHERPA_VERSION 是 native 库版本，需与 pom.xml 里 sherpa-onnx.version 的 Java 绑定版本保持兼容，
# 两边升级要一起做，否则 JNI 符号对不上会在运行时报 UnsatisfiedLinkError
SHERPA_VERSION="1.12.23"
ONNXRUNTIME_VERSION="1.23.2"
VOSK_VERSION="0.3.45"

LIB_DIR="$PROJECT_DIR/lib"
MODELS_DIR="$PROJECT_DIR/models"

# ---- 颜色 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ---- 日志：下载侧沿用 [INFO]/[WARN]/[ERROR] 风格 ----
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ---- 日志：服务侧沿用 [xiaozhi] 风格 ----
_log()  { echo -e "${GREEN}[xiaozhi]${NC} $*"; }
_info() { echo -e "${CYAN}[xiaozhi]${NC} $*"; }
_warn() { echo -e "${YELLOW}[xiaozhi]${NC} $*"; }
_err()  { echo -e "${RED}[xiaozhi]${NC} $*" >&2; }
_ok()   { echo -e "${GREEN}[xiaozhi]${NC} ${BOLD}$*${NC}"; }

die() { error "$*"; exit 1; }

# ============================================================
# 平台检测
# ============================================================
detect_platform() {
    if [ -n "$PLATFORM" ]; then
        return 0
    fi

    if [ -n "$TARGET_PLATFORM" ]; then
        PLATFORM="$TARGET_PLATFORM"
        info "使用指定平台: ${BLUE}${PLATFORM}${NC}"
    else
        local os arch
        os=$(uname -s | tr '[:upper:]' '[:lower:]')
        arch=$(uname -m)
        case "$os" in
            linux)
                case "$arch" in
                    x86_64|amd64)   PLATFORM="linux-x64" ;;
                    aarch64|arm64)  PLATFORM="linux-aarch64" ;;
                    *) error "不支持的 Linux 架构: $arch"; exit 1 ;;
                esac
                ;;
            darwin)
                case "$arch" in
                    x86_64)         PLATFORM="osx-x86_64" ;;
                    arm64|aarch64)  PLATFORM="osx-arm64" ;;
                    *) error "不支持的 macOS 架构: $arch"; exit 1 ;;
                esac
                ;;
            mingw*|msys*|cygwin*)
                PLATFORM="win-x64"
                ;;
            *)
                error "不支持的操作系统: $os (支持: linux, darwin, windows)"
                exit 1
                ;;
        esac
        info "检测到平台: ${BLUE}${PLATFORM}${NC}"
    fi

    # 平台相关的库文件扩展名和名称
    case "$PLATFORM" in
        linux-*)
            LIB_EXT="so"
            SHERPA_JNI_LIB="libsherpa-onnx-jni.${LIB_EXT}"
            SHERPA_CAPI_LIB="libsherpa-onnx-c-api.${LIB_EXT}"
            ONNXRT_LIB="libonnxruntime.${LIB_EXT}"
            VOSK_LIB="libvosk.${LIB_EXT}"
            ;;
        osx-*)
            LIB_EXT="dylib"
            SHERPA_JNI_LIB="libsherpa-onnx-jni.${LIB_EXT}"
            SHERPA_CAPI_LIB="libsherpa-onnx-c-api.${LIB_EXT}"
            ONNXRT_LIB="libonnxruntime.${LIB_EXT}"
            VOSK_LIB="libvosk.${LIB_EXT}"
            ;;
        win-*)
            LIB_EXT="dll"
            SHERPA_JNI_LIB="sherpa-onnx-jni.${LIB_EXT}"
            SHERPA_CAPI_LIB="sherpa-onnx-c-api.${LIB_EXT}"
            ONNXRT_LIB="onnxruntime.${LIB_EXT}"
            VOSK_LIB="vosk.${LIB_EXT}"
            ;;
    esac
}

# ============================================================
# 下载辅助（优先 curl，fallback wget）
# ============================================================
download_file() {
    local url="$1"
    local output="$2"
    if command -v curl &>/dev/null; then
        curl -f -L -C - --progress-bar -o "$output" "$url"
    elif command -v wget &>/dev/null; then
        wget -c "$url" -O "$output"
    else
        die "需要 wget 或 curl，请先安装其中之一"
    fi
}

# 下载阶段的前置检查
check_download_tools() {
    if ! command -v curl &>/dev/null && ! command -v wget &>/dev/null; then
        die "需要 curl 或 wget，请先安装其中之一"
    fi
    if ! command -v tar &>/dev/null; then
        die "需要 tar 解压模型包"
    fi
    if ! command -v unzip &>/dev/null; then
        warn "未找到 unzip，Vosk 模型 / Vosk 原生库将无法解压（用 SenseVoice 可不依赖 unzip）"
    fi
}

# ============================================================
# 模型与原生库配置
# ============================================================
# ---- VAD 模型（Silero VAD）----
VAD_MODEL_NAME="silero_vad.onnx"
VAD_MODEL_URL="https://github.com/snakers4/silero-vad/raw/v6.2.1/src/silero_vad/data/silero_vad.onnx"
VAD_MODEL_DIR="${MODELS_DIR}"

# ---- 原生库下载地址 ----
SHERPA_GITHUB="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}"
VOSK_GITHUB="https://github.com/alphacep/vosk-api/releases/download/v${VOSK_VERSION}"

# ---- SenseVoice（sherpa-onnx）----
# 服务端优先加载 models/sense-voice，目录名与 RuntimePathConfig.senseVoiceModelDir 一致
SENSE_VOICE_MODEL="sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17"
SENSE_VOICE_BASE_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models"
SENSE_VOICE_DIR_NAME="sense-voice"

# ---- Vosk 兜底 STT ----
# small: vosk-model-small-cn-0.22 (~50MB，速度快，适合低配设备)
# standard: vosk-model-cn-0.22 (~1.3GB，精度高)
VOSK_MODEL_SMALL="vosk-model-small-cn-0.22"
VOSK_MODEL_STANDARD="vosk-model-cn-0.22"
VOSK_BASE_URL="https://alphacephei.com/vosk/models"

STT_MODEL_DIR="${MODELS_DIR}"

# ---- TTS 模型 ----
TTS_MODEL_DIR="${MODELS_DIR}/tts"
SHERPA_TTS_BASE_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
TTS_VITS_MELO="vits-melo-tts-zh_en"
TTS_MATCHA="matcha-icefall-zh-en"

# ============================================================
# 下载：VAD 模型
# ============================================================
download_vad() {
    info "========== 下载 VAD 语音检测模型 =========="
    info "模型: ${VAD_MODEL_NAME} (~2.2MB)"

    mkdir -p "$VAD_MODEL_DIR"

    if [ -f "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}" ]; then
        info "VAD模型已存在: ${VAD_MODEL_DIR}/${VAD_MODEL_NAME}"
        info "如需重新下载，请先运行: $0 clean base"
        return 0
    fi

    info "正在下载..."
    download_file "$VAD_MODEL_URL" "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}"

    info "VAD模型下载完成！"
    echo ""
}

# ============================================================
# 下载：sherpa-onnx JNI 原生库（从 JNI tarball 提取）
# ============================================================
download_jni_lib() {
    detect_platform
    info "========== 下载 sherpa-onnx JNI 原生库 =========="
    info "版本: v${SHERPA_VERSION} (${PLATFORM})"

    mkdir -p "$LIB_DIR"

    if [ -f "${LIB_DIR}/${SHERPA_JNI_LIB}" ] && [ -f "${LIB_DIR}/${ONNXRT_LIB}" ]; then
        info "JNI原生库已存在: ${LIB_DIR}/"
        info "如需重新下载，请先运行: $0 clean base"
        return 0
    fi

    local tar_name="sherpa-onnx-v${SHERPA_VERSION}-${PLATFORM}-jni.tar.bz2"
    local tar_url="${SHERPA_GITHUB}/${tar_name}"

    local WORK_DIR
    WORK_DIR=$(mktemp -d)
    # Windows 上刚复制过的文件可能被占用，清理失败不能拖垮整个下载
    trap "rm -rf '$WORK_DIR' 2>/dev/null || true" EXIT

    info "正在下载 ${tar_name} ..."
    download_file "$tar_url" "${WORK_DIR}/${tar_name}"

    info "正在解压原生库..."
    tar xf "${WORK_DIR}/${tar_name}" -C "$WORK_DIR"

    local found=0
    while IFS= read -r -d '' lib_file; do
        cp "$lib_file" "$LIB_DIR/"
        info "  提取: $(basename "$lib_file")"
        found=$((found + 1))
    done < <(find "$WORK_DIR" -type f \( -name "*.${LIB_EXT}" -o -name "*.${LIB_EXT}.*" \) -print0)

    # macOS: 创建 libonnxruntime.dylib 符号链接
    if [[ "$PLATFORM" == osx-* ]] && [ ! -e "${LIB_DIR}/libonnxruntime.dylib" ]; then
        local versioned_ort
        versioned_ort=$(ls "${LIB_DIR}"/libonnxruntime.*.dylib 2>/dev/null | head -1 || true)
        if [ -n "$versioned_ort" ]; then
            ln -sf "$(basename "$versioned_ort")" "${LIB_DIR}/libonnxruntime.dylib"
            info "  创建符号链接: libonnxruntime.dylib -> $(basename "$versioned_ort")"
        fi
    fi

    rm -rf "$WORK_DIR" 2>/dev/null || true
    trap - EXIT

    if [ "$found" -eq 0 ]; then
        error "提取失败，未找到任何 .${LIB_EXT} 文件"
        return 1
    fi

    info "sherpa-onnx JNI 原生库下载完成！(共 ${found} 个文件)"
    echo ""
}

# ============================================================
# 下载：Vosk 原生库（从 Maven JAR 中提取）
# ============================================================
download_vosk_lib() {
    detect_platform
    info "========== 下载 Vosk 原生库 =========="
    info "版本: v${VOSK_VERSION} (${PLATFORM})"

    mkdir -p "$LIB_DIR"

    if [ -f "${LIB_DIR}/${VOSK_LIB}" ]; then
        info "Vosk原生库已存在: ${LIB_DIR}/${VOSK_LIB}"
        info "如需重新下载，请先运行: $0 clean base"
        return 0
    fi

    # JAR 内的文件名和运行时实际加载的文件名不一定一致：
    # Windows 版是 libvosk.dll，而 LibVosk 内部是 System.loadLibrary("vosk")，落盘必须叫 vosk.dll
    local jar_subdir="" jar_lib=""
    case "$PLATFORM" in
        linux-x64)      jar_subdir="linux-x86-64";  jar_lib="libvosk.so" ;;
        linux-aarch64)
            warn "Vosk Maven JAR 不包含 linux-aarch64 原生库"
            warn "请从 ${VOSK_GITHUB}/vosk-linux-aarch64-${VOSK_VERSION}.zip 手动下载"
            return 0
            ;;
        osx-*)          jar_subdir="darwin";        jar_lib="libvosk.dylib" ;;
        win-x64)        jar_subdir="win32-x86-64";  jar_lib="libvosk.dll" ;;
    esac

    if ! command -v unzip &>/dev/null; then
        error "需要 unzip 解压 Maven JAR（apt install unzip / brew install unzip）"
        return 1
    fi

    local vosk_jar=""
    local WORK_DIR=""
    local m2_jar="${HOME}/.m2/repository/com/alphacephei/vosk/${VOSK_VERSION}/vosk-${VOSK_VERSION}.jar"
    if [ -f "$m2_jar" ]; then
        info "从本地 Maven 缓存提取..."
        vosk_jar="$m2_jar"
    else
        local jar_url="https://repo1.maven.org/maven2/com/alphacephei/vosk/${VOSK_VERSION}/vosk-${VOSK_VERSION}.jar"
        WORK_DIR=$(mktemp -d)
        vosk_jar="${WORK_DIR}/vosk-${VOSK_VERSION}.jar"
        info "正在从 Maven Central 下载 vosk-${VOSK_VERSION}.jar ..."
        download_file "$jar_url" "$vosk_jar"
    fi

    local EXTRACT_DIR
    EXTRACT_DIR=$(mktemp -d)
    unzip -q -o "$vosk_jar" "${jar_subdir}/*" -d "$EXTRACT_DIR" 2>/dev/null || true

    local SRC_DIR="${EXTRACT_DIR}/${jar_subdir}"
    if [ -f "${SRC_DIR}/${jar_lib}" ]; then
        cp "${SRC_DIR}/${jar_lib}" "${LIB_DIR}/${VOSK_LIB}"
        # Windows 的 libvosk.dll 依赖这三个 MSYS2 运行时，缺了加载时会报找不到 dll
        if [[ "$PLATFORM" == win-* ]]; then
            local dep
            for dep in libstdc++-6.dll libwinpthread-1.dll libgcc_s_seh-1.dll; do
                if [ -f "${SRC_DIR}/${dep}" ]; then
                    cp "${SRC_DIR}/${dep}" "$LIB_DIR/"
                fi
            done
        fi
        info "Vosk 原生库提取完成: ${VOSK_LIB}"
    else
        error "未在 JAR 中找到 ${jar_subdir}/${jar_lib}"
        rm -rf "$EXTRACT_DIR" "${WORK_DIR:-}" 2>/dev/null || true
        return 1
    fi

    rm -rf "$EXTRACT_DIR" "${WORK_DIR:-}" 2>/dev/null || true
    echo ""
}

# ============================================================
# 下载：原生库 / 基础依赖 聚合
# ============================================================
download_libs() {
    download_jni_lib
    download_vosk_lib
}

download_base_all() {
    download_vad
    download_libs
}

# ============================================================
# 下载：STT —— SenseVoice
# ============================================================
download_sense_voice() {
    info "========== 下载语音识别(STT)模型 =========="
    info "模型: ${SENSE_VOICE_MODEL}"
    info "说明: sherpa-onnx SenseVoice-Small (~230MB int8，中英日韩粤，带情绪标签)"

    mkdir -p "$STT_MODEL_DIR"
    if [ -d "${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}" ]; then
        info "SenseVoice 模型已存在: ${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}/"
        info "如需重新下载，请先运行: $0 clean stt"
        return 0
    fi

    local archive="${SENSE_VOICE_MODEL}.tar.bz2"
    info "正在下载 ${archive} ..."
    download_file "${SENSE_VOICE_BASE_URL}/${archive}" "${STT_MODEL_DIR}/${archive}"

    info "正在解压..."
    tar xf "${STT_MODEL_DIR}/${archive}" -C "$STT_MODEL_DIR"
    rm -f "${STT_MODEL_DIR}/${archive}"
    mv "${STT_MODEL_DIR}/${SENSE_VOICE_MODEL}" "${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}"

    # 服务端只加载 int8 量化模型，fp32 的 model.onnx 近 900MB 留着只占磁盘
    if [ -f "${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}/model.int8.onnx" ] \
       && [ -f "${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}/model.onnx" ]; then
        rm -f "${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}/model.onnx"
    fi

    info "STT模型下载完成！"
    info "路径: ${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}/"
    echo ""
}

# ============================================================
# 下载：STT —— Vosk
# ============================================================
download_vosk_model() {
    local size="${1:-small}"
    local model_name

    if [ "$size" = "small" ]; then
        model_name="$VOSK_MODEL_SMALL"
    else
        model_name="$VOSK_MODEL_STANDARD"
    fi

    info "========== 下载语音识别(STT)模型 =========="
    info "模型: ${model_name}"
    if [ "$size" = "small" ]; then
        info "说明: Vosk 中文小模型 (~50MB，速度快)"
    else
        info "说明: Vosk 中文标准模型 (~1.3GB，精度高)"
    fi

    if ! command -v unzip &>/dev/null; then
        error "需要 unzip 解压 Vosk 模型"
        return 1
    fi

    mkdir -p "$STT_MODEL_DIR"

    if [ -d "${STT_MODEL_DIR}/vosk-model" ]; then
        info "Vosk模型已存在: ${STT_MODEL_DIR}/vosk-model/"
        info "如需重新下载，请先运行: $0 clean stt"
        return 0
    fi

    local archive="${model_name}.zip"
    info "正在下载 ${archive} ..."
    download_file "${VOSK_BASE_URL}/${archive}" "${STT_MODEL_DIR}/${archive}"

    info "正在解压..."
    unzip -q -o "${STT_MODEL_DIR}/${archive}" -d "$STT_MODEL_DIR"

    # 重命名为统一目录名 vosk-model（与 VoskSttService.java 中的路径一致）
    mv "${STT_MODEL_DIR}/${model_name}" "${STT_MODEL_DIR}/vosk-model"
    rm -f "${STT_MODEL_DIR}/${archive}"

    info "STT模型下载完成！"
    info "路径: ${STT_MODEL_DIR}/vosk-model/"
    echo ""
}

# 与 Dockerfile 的统一入口：sensevoice | small | standard | none
download_stt() {
    case "${1:-sensevoice}" in
        sensevoice) download_sense_voice ;;
        small|standard) download_vosk_model "$1" ;;
        # 与 TTS 的 none 对齐：SenseVoice 就位时 Vosk 永远轮不到，允许显式不下载
        none) info "跳过 Vosk 兜底模型下载" ;;
        *) error "未知的 STT 模型: $1"; return 1 ;;
    esac
}

# ============================================================
# 下载：TTS 模型（sherpa-onnx）
# ============================================================
download_tts() {
    local tts_type="${1:-vits-melo}"
    local model_name

    case "$tts_type" in
        vits-melo|vits|melo|${TTS_VITS_MELO})
            model_name="$TTS_VITS_MELO"
            info "========== 下载语音合成(TTS)模型 =========="
            info "模型: ${model_name} (VITS MeloTTS 中英文，~163MB)"
            ;;
        matcha|matcha-icefall|${TTS_MATCHA})
            model_name="$TTS_MATCHA"
            info "========== 下载语音合成(TTS)模型 =========="
            info "模型: ${model_name} (Matcha-Icefall 中英文)"
            ;;
        none)
            info "跳过 TTS 模型下载 (none)"
            return 0
            ;;
        *)
            error "未知的 TTS 模型类型: $tts_type (支持: vits-melo, matcha, none)"
            return 1
            ;;
    esac

    mkdir -p "$TTS_MODEL_DIR"

    if [ -f "${TTS_MODEL_DIR}/${model_name}/model.onnx" ]; then
        info "TTS模型已存在: ${TTS_MODEL_DIR}/${model_name}/"
        info "如需重新下载，请先运行: $0 clean tts"
        return 0
    fi

    local archive="${model_name}.tar.bz2"
    info "正在下载..."
    download_file "${SHERPA_TTS_BASE_URL}/${archive}" "${TTS_MODEL_DIR}/${archive}"

    info "正在解压..."
    tar xf "${TTS_MODEL_DIR}/${archive}" -C "$TTS_MODEL_DIR"
    rm -f "${TTS_MODEL_DIR}/${archive}"

    info "TTS模型下载完成！"
    info "路径: ${TTS_MODEL_DIR}/${model_name}/"
    echo ""
}

# 下载全部模型：SenseVoice + TTS(vits-melo) + VAD
download_all_models() {
    download_stt "sensevoice"
    download_tts "vits-melo"
    download_vad
}

# ============================================================
# 清理
# ============================================================
clean_base() {
    warn "========== 清理基础依赖 =========="

    if [ -f "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}" ]; then
        rm -f "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}"
        info "已删除 VAD 模型: ${VAD_MODEL_NAME}"
    fi

    local cleaned=0 f pattern
    for pattern in \
        "libsherpa-onnx-jni.*" "libsherpa-onnx-c-api.*" "libsherpa-onnx-cxx-api.*" \
        "sherpa-onnx-jni.*" "sherpa-onnx-c-api.*" \
        "libonnxruntime*" "onnxruntime.*" \
        "libvosk.*" "vosk.dll"; do
        for f in "${LIB_DIR}"/${pattern}; do
            if [ -f "$f" ] || [ -L "$f" ]; then
                rm -f "$f"
                info "已删除: $(basename "$f")"
                cleaned=$((cleaned + 1))
            fi
        done
    done

    if [ "$cleaned" -eq 0 ]; then
        info "lib/ 目录无需清理"
    fi
    info "清理完成！"
}

clean_stt() {
    warn "========== 清理 STT 模型 =========="
    local cleaned=0
    if [ -d "${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}" ]; then
        rm -rf "${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}"
        info "已删除 SenseVoice STT 模型"
        cleaned=1
    fi
    if [ -d "${STT_MODEL_DIR}/vosk-model" ]; then
        rm -rf "${STT_MODEL_DIR}/vosk-model"
        info "已删除 Vosk STT 模型"
        cleaned=1
    fi
    if [ "$cleaned" = "0" ]; then
        info "无需清理"
    fi
    info "清理完成！"
}

clean_tts() {
    warn "========== 清理 TTS 模型 =========="
    local tts_model
    for tts_model in "$TTS_VITS_MELO" "$TTS_MATCHA"; do
        if [ -d "${TTS_MODEL_DIR}/${tts_model}" ]; then
            rm -rf "${TTS_MODEL_DIR}/${tts_model}"
            info "已删除 TTS 模型: ${tts_model}"
        fi
        rm -f "${TTS_MODEL_DIR}/${tts_model}.tar.bz2" 2>/dev/null || true
    done
    info "清理完成！"
}

# ============================================================
# 状态：模型与原生库
# ============================================================
show_base_status() {
    detect_platform

    if [ -f "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}" ]; then
        echo -e "  VAD (语音检测):    ${GREEN}✓ 已下载${NC} - ${VAD_MODEL_NAME}"
    else
        echo -e "  VAD (语音检测):    ${RED}✗ 未下载${NC} - ${VAD_MODEL_NAME}"
    fi

    if [ -f "${LIB_DIR}/${SHERPA_JNI_LIB}" ]; then
        echo -e "  sherpa-onnx JNI:   ${GREEN}✓ 已存在${NC} - ${SHERPA_JNI_LIB}"
    else
        echo -e "  sherpa-onnx JNI:   ${RED}✗ 不存在${NC} - ${SHERPA_JNI_LIB}"
    fi

    if [ -f "${LIB_DIR}/${ONNXRT_LIB}" ] || ls "${LIB_DIR}"/libonnxruntime.*.${LIB_EXT} &>/dev/null; then
        echo -e "  onnxruntime:       ${GREEN}✓ 已存在${NC} - ${ONNXRT_LIB}"
    else
        echo -e "  onnxruntime:       ${RED}✗ 不存在${NC} - ${ONNXRT_LIB}"
    fi

    if [ -f "${LIB_DIR}/${VOSK_LIB}" ]; then
        echo -e "  Vosk 原生库:       ${GREEN}✓ 已存在${NC} - ${VOSK_LIB}"
    else
        echo -e "  Vosk 原生库:       ${RED}✗ 不存在${NC} - ${VOSK_LIB}"
    fi
}

show_stt_status() {
    if [ -d "${STT_MODEL_DIR}/${SENSE_VOICE_DIR_NAME}" ]; then
        echo -e "  STT (语音识别):    ${GREEN}✓ 已下载${NC} - ${SENSE_VOICE_DIR_NAME} (sherpa-onnx SenseVoice)"
    else
        echo -e "  STT (语音识别):    ${RED}✗ 未下载${NC} - ${SENSE_VOICE_DIR_NAME} (sherpa-onnx SenseVoice)"
    fi
    if [ -d "${STT_MODEL_DIR}/vosk-model" ]; then
        echo -e "  STT (Vosk 兜底):   ${GREEN}✓ 已下载${NC} - vosk-model"
    else
        echo -e "  STT (Vosk 兜底):   ${RED}✗ 未下载${NC} - vosk-model"
    fi
}

show_tts_status() {
    if [ -f "${TTS_MODEL_DIR}/${TTS_VITS_MELO}/model.onnx" ]; then
        echo -e "  TTS (vits-melo):   ${GREEN}✓ 已下载${NC} - ${TTS_VITS_MELO}"
    else
        echo -e "  TTS (vits-melo):   ${RED}✗ 未下载${NC} - ${TTS_VITS_MELO}"
    fi

    if [ -f "${TTS_MODEL_DIR}/${TTS_MATCHA}/model.onnx" ]; then
        echo -e "  TTS (matcha):      ${GREEN}✓ 已下载${NC} - ${TTS_MATCHA}"
    else
        echo -e "  TTS (matcha):      ${YELLOW}○ 未下载${NC} - ${TTS_MATCHA} (可选)"
    fi
}

show_assets_status() {
    detect_platform
    echo ""
    info "========== 模型和原生库状态 (${PLATFORM}) =========="
    echo -e "  ${BLUE}── 模型 ──${NC}"
    show_stt_status
    show_tts_status
    echo -e "  ${BLUE}── 基础依赖 ──${NC}"
    show_base_status
    echo ""
}

# ============================================================
# 服务：控制台日志轮转
# ============================================================
# Logback 自己的 FILE/ERROR_FILE 有 rollingPolicy，但 nohup 重定向出去的 .out 是纯 append，
# 长期运行会无限增长。这里只在每次启动时做一次按大小轮转，够用且不引入 logrotate 依赖。
rotate_console_log() {
  local file="$1" max_bytes=$((50 * 1024 * 1024)) keep=5
  [[ -f "$file" ]] || return 0
  local size
  size=$(wc -c < "$file" 2>/dev/null || echo 0)
  if (( size > max_bytes )); then
    mv "$file" "$file.$(date +%Y%m%d%H%M%S)"
    # 只留最近 $keep 份，按文件名（含时间戳）倒序排，多出来的删掉
    ls -1 "$file".* 2>/dev/null | sort -r | tail -n +$((keep + 1)) | xargs -r rm -f
  fi
}

# ============================================================
# 服务：部署模式与 Java 解析
# ============================================================
# 部署模式：$ROOT_DIR 下没有 pom.xml（纯 jar 部署）或没有 mvn 命令
# 此时跳过编译，直接使用现成的 jar
is_deploy_mode() {
  [[ ! -f "$ROOT_DIR/pom.xml" ]] && return 0
  ! command -v mvn >/dev/null 2>&1 && return 0
  return 1
}

# 优先级: $JAVA_BIN > $JAVA_HOME/bin/java > PATH 中的 java
# 适配宝塔/独立安装 JDK 不在 PATH 的场景（例如 /www/server/java/jdk-21.0.2/bin/java）
resolve_java() {
  if [[ -n "$JAVA_BIN" && -x "$JAVA_BIN" ]]; then
    echo "$JAVA_BIN"; return 0
  fi
  if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]]; then
    echo "$JAVA_HOME/bin/java"; return 0
  fi
  if command -v java >/dev/null 2>&1; then
    command -v java; return 0
  fi
  return 1
}

# ============================================================
# 服务：启动前自检
# ============================================================
# 检查 JDK 版本、模型与原生库、中间件连通性，SKIP_PREFLIGHT=1 跳过
preflight() {
  [[ "${SKIP_PREFLIGHT:-0}" == "1" ]] && return 0

  local failed=0

  # 1) JDK 21+
  local java_bin java_major
  if ! java_bin="$(resolve_java)"; then
    _err "未找到 java。请安装 JDK 21+，或设置 JAVA_HOME / JAVA_BIN"
    failed=1
  else
    java_major="$("$java_bin" -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')"
    if [[ ! "$java_major" =~ ^[0-9]+$ ]] || (( java_major < 21 )); then
      _err "JDK 版本过低（检测到 ${java_major:-未知}），本项目需要 21 及以上：$java_bin"
      failed=1
    fi
  fi

  # 2) 原生库与 VAD 模型
  if [[ ! -d "$ROOT_DIR/lib" ]] || [[ -z "$(ls -A "$ROOT_DIR/lib" 2>/dev/null)" ]]; then
    _err "缺少原生库目录 lib/，先执行：$0 download base"
    failed=1
  fi
  if [[ ! -f "$ROOT_DIR/models/silero_vad.onnx" ]]; then
    _err "缺少 VAD 模型 models/silero_vad.onnx，先执行：$0 download base"
    failed=1
  fi
  if [[ ! -d "$ROOT_DIR/models/sense-voice" ]]; then
    _warn "未检测到本地语音识别模型 models/sense-voice"
    _warn "  用云端 STT 可以忽略；想用本地识别执行：$0 download stt"
  fi

  # 3) 中间件连通性
  check_tcp "MySQL" "$(datasource_host)" "$(datasource_port)" \
    "docker compose -f docker-compose-db.yml up -d" || failed=1
  check_tcp "Redis" "${SPRING_DATA_REDIS_HOST:-localhost}" "${SPRING_DATA_REDIS_PORT:-6379}" \
    "docker compose -f docker-compose-db.yml up -d" || failed=1

  if (( failed )); then
    echo ""
    _err "启动前检查未通过，按上面的提示处理后重试（确认无误可用 SKIP_PREFLIGHT=1 跳过）"
    return 1
  fi
  _log "启动前检查通过"
}

# check_tcp <名称> <主机> <端口> <修复提示>
check_tcp() {
  local name="$1" host="$2" port="$3" hint="$4"
  # macOS 默认没有 timeout，拿不到就不限时
  local limit=""
  if command -v timeout >/dev/null 2>&1; then
    limit="timeout 3"
  elif command -v gtimeout >/dev/null 2>&1; then
    limit="gtimeout 3"
  fi
  if $limit bash -c "exec 3<>/dev/tcp/${host}/${port}" 2>/dev/null; then
    return 0
  fi
  _err "$name 连不上（${host}:${port}）"
  _err "  没起的话执行：$hint"
  return 1
}

# 从 SPRING_DATASOURCE_URL 取主机/端口，没设则按 localhost:3306
datasource_host() {
  local url="${SPRING_DATASOURCE_URL:-}"
  if [[ -z "$url" ]]; then
    echo "localhost"; return
  fi
  echo "$url" | sed -E 's|^jdbc:mysql://([^:/?]+).*|\1|'
}

datasource_port() {
  local url="${SPRING_DATASOURCE_URL:-}"
  if [[ "$url" =~ ^jdbc:mysql://[^:/?]+:([0-9]+) ]]; then
    echo "${BASH_REMATCH[1]}"; return
  fi
  echo "3306"
}

# ============================================================
# 服务：编译与 jar 查找
# ============================================================
# build <module>  — 只编译该模块及其依赖
# build all       — 编译全部
build() {
  if is_deploy_mode; then
    _info "部署模式：跳过编译（未检测到 pom.xml 或 mvn 命令）"
    return 0
  fi

  local target="${1:-all}"
  if [[ "$target" == "all" ]]; then
    _info "编译所有模块..."
    mvn clean install -DskipTests -q -f "$ROOT_DIR/pom.xml"
  else
    _info "编译 $target 及其依赖..."
    mvn clean install -DskipTests -q -f "$ROOT_DIR/pom.xml" \
        -pl "$target" --also-make
  fi
  _log "编译完成"
}

# xiaozhi-dialogue 使用 classifier=exec，产出 *-exec.jar；其余模块用普通 jar
# 优先在 $ROOT_DIR 根目录查找（部署模式），找不到再回退到 $module/target/（开发模式）
find_jar() {
  local module="$1" jar=""
  if [[ "$module" == "xiaozhi-dialogue" ]]; then
    jar=$(ls "$ROOT_DIR/$module"-*-exec.jar 2>/dev/null | head -1 || true)
    [[ -z "$jar" ]] && jar=$(ls "$ROOT_DIR/$module/target/$module"-*-exec.jar 2>/dev/null | head -1 || true)
  else
    jar=$(ls "$ROOT_DIR/$module"-*.jar 2>/dev/null \
      | grep -v 'original' | grep -v '\-exec\.jar' | head -1 || true)
    [[ -z "$jar" ]] && jar=$(ls "$ROOT_DIR/$module/target/$module"-*.jar 2>/dev/null \
      | grep -v 'original' | grep -v '\-exec\.jar' | head -1 || true)
  fi
  echo "$jar"
}

# ============================================================
# 服务：进程管理
# ============================================================
pid_file() {
  echo "$LOGS_DIR/$1.pid"
}

is_running() {
  local pid_path
  pid_path="$(pid_file "$1")"
  [[ -f "$pid_path" ]] && kill -0 "$(cat "$pid_path")" 2>/dev/null
}

# resolve_profile [dev|prod] — 优先级：命令行参数 > 已导出的 SPRING_PROFILES_ACTIVE > dev
resolve_profile() {
  local profile="${1:-${SPRING_PROFILES_ACTIVE:-dev}}"
  case "$profile" in
    dev|prod) echo "$profile" ;;
    *) _err "不支持的运行环境: ${profile}（只支持 dev / prod）"; return 1 ;;
  esac
}

# start_service <name> <module> <port> <profile>
start_service() {
  local name="$1" module="$2" port="$3" profile="$4"

  if is_running "$name"; then
    _warn "$name 已在运行 (pid=$(cat "$(pid_file "$name")"))"
    return 0
  fi

  # 未设置时设备握手鉴权关闭，本地联调可用，生产部署必须导出
  if [[ -z "${XIAOZHI_DEVICE_AUTH_SECRET:-}" ]]; then
    _warn "未设置 XIAOZHI_DEVICE_AUTH_SECRET，$name 的设备握手鉴权处于关闭状态"
    _warn "  生产部署前导出：export XIAOZHI_DEVICE_AUTH_SECRET=\$(openssl rand -hex 16)"
    _warn "  server 与 dialogue 必须使用同一个值"
  fi

  local jar
  jar="$(find_jar "$module")"
  if [[ -z "$jar" ]]; then
    _err "$module jar 不存在，请先编译"
    return 1
  fi

  local java_bin
  if ! java_bin="$(resolve_java)"; then
    _err "未找到 java 可执行文件。请安装 JDK 21+ 或设置 JAVA_HOME / JAVA_BIN 环境变量"
    _err "  例如: export JAVA_HOME=/www/server/java/jdk-21.0.2"
    return 1
  fi

  _info "启动 $name (port $port, profile $profile)..."
  _info "  java: $java_bin"
  mkdir -p "$LOGS_DIR"
  rotate_console_log "$LOGS_DIR/$name.out"
  # 记下启动前 .out 的大小，follow_logs 据此只输出本次启动之后的内容
  printf -v "OUT_OFFSET_${name//-/_}" '%d' "$(( $(wc -c < "$LOGS_DIR/$name.out" 2>/dev/null || echo 0) ))"

  # cd 到 ROOT_DIR 启动，确保:
  #   1. Logback 配置中的 ./logs 写到 $ROOT_DIR/logs/
  #   2. application.yml 中 lib/, models/silero_vad.onnx 等相对路径解析正确
  ( cd "$ROOT_DIR" && exec nohup "$java_bin" \
      -Djava.library.path="$ROOT_DIR/lib" \
      -jar "$jar" \
      --spring.profiles.active="$profile" \
      >> "$LOGS_DIR/$name.out" 2>&1 ) &

  local pid=$!
  echo "$pid" > "$(pid_file "$name")"
  _ok "$name 已启动  pid=$pid  日志: logs/$name.log  控制台: logs/$name.out"
}

stop_service() {
  local name="$1"
  local pid_path
  pid_path="$(pid_file "$name")"

  if ! is_running "$name"; then
    _warn "$name 未在运行"
    return 0
  fi

  local pid
  pid="$(cat "$pid_path")"
  _info "停止 $name (pid=$pid)..."
  kill "$pid" 2>/dev/null || true

  # 等待最多 15 秒
  local i=0
  while kill -0 "$pid" 2>/dev/null && (( i < 15 )); do
    sleep 1
    i=$((i + 1))
  done

  if kill -0 "$pid" 2>/dev/null; then
    _warn "未能正常关闭，强制结束..."
    kill -9 "$pid" 2>/dev/null || true
  fi

  rm -f "$pid_path"
  _ok "$name 已停止"
}

status_service() {
  local name="$1" port="$2"
  if is_running "$name"; then
    local pid
    pid="$(cat "$(pid_file "$name")")"
    echo -e "  ${GREEN}●${NC} ${BOLD}$name${NC}  pid=$pid  port=$port  日志: logs/$name.log"
  else
    echo -e "  ${RED}○${NC} ${BOLD}$name${NC}  未运行"
  fi
}

# follow_logs <name>... — 合并跟随多个服务的 logs/<name>.out，每行带服务名前缀
# 刚由 start_service 启动的服务只输出本次启动之后的内容，其余服务先回显最近 30 行
# Ctrl+C 只退出跟随，服务继续在后台运行
follow_logs() {
  local colors=("$CYAN" "$YELLOW" "$BLUE") pids=() name i=0
  for name in "$@"; do
    local file="$LOGS_DIR/$name.out"
    local var="OUT_OFFSET_${name//-/_}"
    local tag
    if [[ ! -f "$file" ]]; then
      _warn "$name 尚无控制台日志 (logs/$name.out)"
      continue
    fi
    tag="$(printf '%b' "${colors[i % ${#colors[@]}]}[${name#xiaozhi-}]${NC} ")"
    if [[ -n "${!var:-}" ]]; then
      tail -c "+$(( ${!var} + 1 ))" -F "$file" 2>/dev/null > >(awk -v tag="$tag" '{ print tag $0; fflush() }') &
    else
      tail -n 30 -F "$file" 2>/dev/null > >(awk -v tag="$tag" '{ print tag $0; fflush() }') &
    fi
    pids+=("$!")
    i=$((i + 1))
  done
  if (( ${#pids[@]} == 0 )); then
    return 1
  fi

  echo ""
  _info "正在跟随日志，Ctrl+C 退出（服务不受影响）"
  echo ""
  # kill 后在 trap 里就地 wait 回收，否则 bash 会额外打印一行 "Terminated: 15 tail ..."
  trap 'kill "${pids[@]}" 2>/dev/null; wait "${pids[@]}" 2>/dev/null; trap - INT TERM; echo ""; _info "已退出日志跟随，服务仍在后台运行"' INT TERM
  wait "${pids[@]}" 2>/dev/null || true
  trap - INT TERM
}

# follow_logs_if_tty <name>... — 只在交互终端里才跟随，管道/CI 等非交互场景直接返回
follow_logs_if_tty() {
  [[ -t 1 ]] || return 0
  follow_logs "$@"
}

# ============================================================
# 子命令实现
# ============================================================
SERVER_NAME="xiaozhi-server"
SERVER_MODULE="xiaozhi-server"
SERVER_PORT=8091
DIALOGUE_NAME="xiaozhi-dialogue"
DIALOGUE_MODULE="xiaozhi-dialogue"
DIALOGUE_PORT=8092

# 单服务操作: service_action <name> <port> <action> <profile>
service_action() {
  local name="$1" port="$2" action="$3" profile="$4"

  case "$action" in
    start)
      profile="$(resolve_profile "$profile")" || exit 1
      preflight || exit 1
      build "$name"
      start_service "$name" "$name" "$port" "$profile"
      follow_logs_if_tty "$name"
      ;;
    stop)
      stop_service "$name"
      ;;
    restart)
      profile="$(resolve_profile "$profile")" || exit 1
      preflight || exit 1
      stop_service "$name"
      sleep 1
      build "$name"
      start_service "$name" "$name" "$port" "$profile"
      follow_logs_if_tty "$name"
      ;;
    status)
      echo ""
      status_service "$name" "$port"
      echo ""
      ;;
    logs)
      follow_logs "$name"
      ;;
    *)
      _err "未知操作: ${action:-<空>}"
      show_usage
      exit 1
      ;;
  esac
}

# 全部服务操作: all_action <action> <profile>
all_action() {
  local action="$1" profile="$2"

  case "$action" in
    start)
      profile="$(resolve_profile "$profile")" || exit 1
      preflight || exit 1
      build all
      start_service "$SERVER_NAME"    "$SERVER_MODULE"    "$SERVER_PORT"    "$profile"
      start_service "$DIALOGUE_NAME"  "$DIALOGUE_MODULE"  "$DIALOGUE_PORT"  "$profile"
      echo ""
      _ok "全部启动完成"
      follow_logs_if_tty "$SERVER_NAME" "$DIALOGUE_NAME"
      ;;
    stop)
      stop_service "$SERVER_NAME"
      stop_service "$DIALOGUE_NAME"
      _ok "全部已停止"
      ;;
    restart)
      profile="$(resolve_profile "$profile")" || exit 1
      preflight || exit 1
      stop_service "$SERVER_NAME"
      stop_service "$DIALOGUE_NAME"
      sleep 1
      build all
      start_service "$SERVER_NAME"    "$SERVER_MODULE"    "$SERVER_PORT"    "$profile"
      start_service "$DIALOGUE_NAME"  "$DIALOGUE_MODULE"  "$DIALOGUE_PORT"  "$profile"
      echo ""
      _ok "全部重启完成"
      follow_logs_if_tty "$SERVER_NAME" "$DIALOGUE_NAME"
      ;;
    status)
      echo ""
      status_service "$SERVER_NAME"    "$SERVER_PORT"
      status_service "$DIALOGUE_NAME"  "$DIALOGUE_PORT"
      echo ""
      ;;
    logs)
      follow_logs "$SERVER_NAME" "$DIALOGUE_NAME"
      ;;
    *)
      _err "未知操作: ${action:-<空>}"
      show_usage
      exit 1
      ;;
  esac
}

cmd_download() {
  check_download_tools
  local what="${1:-all}"
  case "$what" in
    all)
      download_all_models
      download_libs
      show_assets_status
      info "所有模型和原生库下载完成！"
      ;;
    models)     download_all_models ;;
    vad)        download_vad ;;
    base)       download_base_all ;;
    stt)        download_stt "${2:-sensevoice}" ;;
    tts)        download_tts "${2:-vits-melo}" ;;
    jni)        download_jni_lib ;;
    vosk-lib)   download_vosk_lib ;;
    libs)       download_libs ;;
    status)     show_assets_status ;;
    *)
      _err "未知的下载目标: $what"
      show_usage
      exit 1
      ;;
  esac
}

cmd_clean() {
  local what="${1:-all}"
  case "$what" in
    all)        clean_stt; clean_tts; clean_base ;;
    models)     clean_stt; clean_tts ;;
    stt)        clean_stt ;;
    tts)        clean_tts ;;
    base|libs)  clean_base ;;
    *)
      _err "未知的清理目标: $what"
      show_usage
      exit 1
      ;;
  esac
}

cmd_status() {
  show_assets_status
  _info "========== 服务状态 =========="
  status_service "$SERVER_NAME"    "$SERVER_PORT"
  status_service "$DIALOGUE_NAME"  "$DIALOGUE_PORT"
  echo ""
}

cmd_build() {
  build "${1:-all}"
}

# 一次性部署：环境自检 → 下载 → 编译 → 启动
cmd_init() {
  local profile
  profile="$(resolve_profile "${1:-}")" || exit 1

  echo ""
  _info "========== xiaozhi 后端一次性初始化 (profile: $profile) =========="
  echo ""

  # 1) 下载模型与原生库
  if [[ "${SKIP_DOWNLOAD:-0}" == "1" ]]; then
    _info "SKIP_DOWNLOAD=1，跳过模型/原生库下载"
  else
    check_download_tools
    download_vad
    download_libs
    download_stt sensevoice
    download_tts vits-melo
    show_assets_status
  fi

  # 2) 启动前自检
  preflight || exit 1

  # 3) 编译
  if [[ "${SKIP_BUILD:-0}" == "1" ]]; then
    _info "SKIP_BUILD=1，跳过编译"
  else
    build all
  fi

  # 4) 启动服务
  start_service "$SERVER_NAME"    "$SERVER_MODULE"    "$SERVER_PORT"    "$profile"
  start_service "$DIALOGUE_NAME"  "$DIALOGUE_MODULE"  "$DIALOGUE_PORT"  "$profile"
  echo ""
  _ok "初始化完成：管理后台 http://localhost:8084（默认 admin / 123456）"
  _ok "OTA  http://<内网IP>:${SERVER_PORT}/api/device/ota"
  _ok "WS   ws://<内网IP>:${DIALOGUE_PORT}/ws/xiaozhi/v1/"
  follow_logs_if_tty "$SERVER_NAME" "$DIALOGUE_NAME"
}

show_usage() {
  echo ""
  echo -e "${BOLD}xiaozhi-esp32-server-java · 后端一体化脚本${NC}"
  echo ""
  echo "用法: $0 <command> [args]"
  echo ""
  echo -e "${BOLD}一次性部署${NC}"
  echo "  init [dev|prod]          下载模型/原生库 → 编译 → 启动（SKIP_DOWNLOAD=1 / SKIP_BUILD=1 可分段跳过）"
  echo ""
  echo -e "${BOLD}模型与原生库${NC}"
  echo "  download [all]           下载全部：SenseVoice + TTS(vits-melo) + VAD + 原生库（默认）"
  echo "  download models          只下载模型：SenseVoice + TTS(vits-melo) + VAD"
  echo "  download vad             下载 VAD 语音检测模型 (silero_vad)"
  echo "  download stt [类型]      sensevoice(默认) | small | standard | none"
  echo "  download tts [类型]      vits-melo(默认) | matcha | none"
  echo "  download jni             下载 sherpa-onnx JNI 原生库 (含 onnxruntime)"
  echo "  download vosk-lib        下载 Vosk 原生库"
  echo "  download libs            下载所有原生库 (JNI + Vosk)"
  echo "  download base            只下载基础依赖 (VAD + 原生库)，语音全用云端时够用"
  echo "  clean [all|models|stt|tts|base|libs]   清理已下载的模型/原生库"
  echo "  status                   查看模型、原生库与服务状态"
  echo ""
  echo -e "${BOLD}服务管理${NC}"
  echo "  start   [dev|prod]       编译并启动 server + dialogue，随后跟随日志"
  echo "  stop                     停止两个服务"
  echo "  restart [dev|prod]       停止后重新编译并启动，随后跟随日志"
  echo "  logs                     跟随两个服务的控制台日志（Ctrl+C 只退出跟随）"
  echo "  build   [all|模块名]     只编译，不启动"
  echo "  server   <start|stop|restart|status|logs> [dev|prod]     只操作 xiaozhi-server (8091)"
  echo "  dialogue <start|stop|restart|status|logs> [dev|prod]     只操作 xiaozhi-dialogue (8092)"
  echo ""
  echo "运行环境默认 dev；也可先 export SPRING_PROFILES_ACTIVE=prod"
  echo "平台自动探测，或 TARGET_PLATFORM=linux-x64 指定：linux-x64 linux-aarch64 osx-x86_64 osx-arm64 win-x64"
  echo "示例:"
  echo "  ./xiaozhi.sh init                         # 首次部署，一条命令走完"
  echo "  TARGET_PLATFORM=linux-x64 $0 download libs"
  echo "  ./xiaozhi.sh start prod                   # 生产环境启动"
  echo ""
}

# ============================================================
# 主入口
# ============================================================
main() {
  local cmd="${1:-}"

  case "$cmd" in
    init)
      cmd_init "${2:-}"
      ;;
    download|dl)
      cmd_download "${2:-all}" "${3:-}"
      ;;
    clean)
      cmd_clean "${2:-all}"
      ;;
    status)
      cmd_status
      ;;
    build)
      cmd_build "${2:-all}"
      ;;
    start)
      all_action start "${2:-}"
      ;;
    stop)
      all_action stop "${2:-}"
      ;;
    restart)
      all_action restart "${2:-}"
      ;;
    logs)
      all_action logs "${2:-}"
      ;;
    server)
      service_action "$SERVER_NAME" "$SERVER_PORT" "${2:-}" "${3:-}"
      ;;
    dialogue)
      service_action "$DIALOGUE_NAME" "$DIALOGUE_PORT" "${2:-}" "${3:-}"
      ;;
    help|-h|--help)
      show_usage
      ;;
    "")
      show_usage
      exit 1
      ;;
    *)
      _err "未知命令: $cmd"
      show_usage
      exit 1
      ;;
  esac
}

main "$@"
