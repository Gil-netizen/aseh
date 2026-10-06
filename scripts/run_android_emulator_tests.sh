#!/usr/bin/env bash
set -euo pipefail

# Git Bash otherwise rewrites Android device paths such as /sdcard before
# passing them to the native Windows adb.exe binary.
if [[ -n "${MSYSTEM:-}" ]]; then
  export MSYS_NO_PATHCONV=1
fi

emulator_label="${1:?usage: run_android_emulator_tests.sh <emulator-label>}"
artifact_dir="build/outputs/smoke-screenshots/${emulator_label}"
mkdir -p "${artifact_dir}"

system_image="${ASEH_SYSTEM_IMAGE:?ASEH_SYSTEM_IMAGE must name the pinned emulator image}"
system_image_revision="${ASEH_SYSTEM_IMAGE_REVISION:?ASEH_SYSTEM_IMAGE_REVISION must pin the image revision}"
sdk_inventory="build/reports/android-sdk/installed-post-runner-${emulator_label}.txt"
mkdir -p "$(dirname "${sdk_inventory}")"

sdkmanager_path="$(command -v sdkmanager 2>/dev/null || true)"
if [[ -z "${sdkmanager_path}" ]]; then
  sdkmanager_path="${ANDROID_HOME:?ANDROID_HOME must be set}/cmdline-tools/latest/bin/sdkmanager.bat"
fi
if [[ ! -f "${sdkmanager_path}" ]]; then
  echo "Android SDK Manager is missing: ${sdkmanager_path}" >&2
  exit 1
fi
"${sdkmanager_path}" --list_installed >"${sdk_inventory}"

require_sdk_revision() {
  local package_name="$1"
  local revision="$2"
  if ! awk -F '|' -v package_name="${package_name}" -v revision="${revision}" '
    {
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", $1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2)
      if ($1 == package_name && $2 == revision) found = 1
    }
    END { exit(found ? 0 : 1) }
  ' "${sdk_inventory}"; then
    echo "Required SDK package ${package_name} revision ${revision} is not installed." >&2
    exit 1
  fi
}

require_sdk_revision "platform-tools" "37.0.1"
require_sdk_revision "build-tools;37.0.0" "37.0.0"
require_sdk_revision "platforms;android-37.0" "2"
require_sdk_revision "${system_image}" "${system_image_revision}"

font_scale="${ASEH_FONT_SCALE:-1.0}"
adb shell settings put system font_scale "${font_scale}"
adb shell am broadcast -a android.intent.action.CONFIGURATION_CHANGED >/dev/null 2>&1 || true

rm -rf -- \
  app/build/outputs/androidTest-results \
  app/build/outputs/connected_android_test_additional_output \
  core/database/build/outputs/androidTest-results

avd_name="$(adb emu avd name | tr -d '\r' | head -n 1)"
android_release="$(adb shell getprop ro.build.version.release | tr -d '\r')"
if [[ -z "${avd_name}" || -z "${android_release}" ]]; then
  echo "Unable to identify the running AVD for screenshot collection." >&2
  exit 1
fi
mkdir -p \
  "app/build/outputs/connected_android_test_additional_output/devDebugAndroidTest/connected/${avd_name}(AVD) - ${android_release}"

# TestStorage owns this FUSE-backed output root on newer Android images and can
# reject direct shell cleanup. Host outputs were cleared above, and the checks
# below still require this run to produce fresh, non-empty ASEH screenshots.
if ! adb shell rm -f \
  /sdcard/googletest/test_outputfiles/english-ltr.png \
  /sdcard/googletest/test_outputfiles/hebrew-rtl.png \
  >/dev/null 2>&1; then
  echo "TestStorage denied optional device-side screenshot cleanup; continuing with fail-closed host checks." >&2
fi

# The launch smoke test deliberately leaves the debug app installed. Remove
# that exact package so a repeated local run starts from the same state as CI.
adb uninstall io.github.gilnetizen.aseh.dev.debug >/dev/null 2>&1 || true

./gradlew \
  --no-daemon \
  --no-parallel \
  --stacktrace \
  --dependency-verification strict \
  :app:connectedDevDebugAndroidTest

assert_connected_test_success() {
  local module_directory="$1"
  mapfile -t exit_code_files < <(
    find "${module_directory}/build/outputs/androidTest-results" \
      -type f -name 'test-result-exit-code.txt' -print 2>/dev/null
  )
  if [[ "${#exit_code_files[@]}" -eq 0 ]]; then
    echo "No connected-test exit code was produced for ${module_directory}." >&2
    exit 1
  fi
  for exit_code_file in "${exit_code_files[@]}"; do
    if ! grep -Fqx '0' "${exit_code_file}"; then
      echo "Connected tests failed according to ${exit_code_file}." >&2
      exit 1
    fi
  done
}

assert_connected_test_success "app"

./gradlew \
  --no-daemon \
  --no-parallel \
  --stacktrace \
  --dependency-verification strict \
  :core:database:connectedDebugAndroidTest

assert_connected_test_success "core/database"

apk_path="$({
  find app/build/outputs/apk/dev/debug -type f -name '*.apk' ! -name '*androidTest*' -print 2>/dev/null || true
} | sort | head -n 1)"

if [[ -z "${apk_path}" ]]; then
  echo "No devDebug application APK was found for the launch smoke test." >&2
  exit 1
fi

aapt_path="${ANDROID_HOME:?ANDROID_HOME must be set}/build-tools/37.0.0/aapt"
if [[ ! -x "${aapt_path}" && -x "${aapt_path}.exe" ]]; then
  aapt_path="${aapt_path}.exe"
fi
if [[ ! -x "${aapt_path}" ]]; then
  echo "Expected Android Asset Packaging Tool is missing: ${aapt_path}" >&2
  exit 1
fi

application_id="$(${aapt_path} dump badging "${apk_path}" | sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -n 1)"
if [[ -z "${application_id}" ]]; then
  echo "Unable to read the application id from ${apk_path}." >&2
  exit 1
fi

# AndroidX TestStorage copies the synthetic Compose captures to the host.
additional_output_dir="app/build/outputs/connected_android_test_additional_output"
mkdir -p "${artifact_dir}/compose"
for screenshot_name in english-ltr.png hebrew-rtl.png; do
  screenshot_path="$(find "${additional_output_dir}" -type f -name "${screenshot_name}" -size +0c -print -quit)"
  if [[ -z "${screenshot_path}" ]]; then
    echo "Missing non-empty ${screenshot_name} under ${additional_output_dir}." >&2
    exit 1
  fi
  cp "${screenshot_path}" "${artifact_dir}/compose/${screenshot_name}"
done

adb install -r "${apk_path}" >/dev/null

# Exercise the launcher with network radios disabled. The manifest policy also
# prevents the app itself from requesting network access.
adb shell svc wifi disable >/dev/null 2>&1 || true
adb shell svc data disable >/dev/null 2>&1 || true
adb shell am force-stop "${application_id}"
adb shell monkey -p "${application_id}" -c android.intent.category.LAUNCHER 1 \
  >"${artifact_dir}/launcher-command.txt"

process_started=false
for _ in $(seq 1 20); do
  if [[ -n "$(adb shell pidof "${application_id}" 2>/dev/null | tr -d '\r')" ]]; then
    process_started=true
    break
  fi
  sleep 1
done

if [[ "${process_started}" != "true" ]]; then
  echo "${application_id} did not remain running after the launcher intent." >&2
  exit 1
fi

adb exec-out screencap -p >"${artifact_dir}/launch.png"
if [[ ! -s "${artifact_dir}/launch.png" ]]; then
  echo "The emulator returned an empty launch screenshot." >&2
  exit 1
fi

if adb shell uiautomator dump /sdcard/aseh-window.xml >/dev/null 2>&1; then
  adb pull /sdcard/aseh-window.xml "${artifact_dir}/window.xml" >/dev/null
else
  echo "UI Automator hierarchy capture was unavailable; the PNG evidence was retained." >&2
fi

printf 'application_id=%s\napk=%s\nfont_scale=%s\n' "${application_id}" "${apk_path}" "${font_scale}" \
  >"${artifact_dir}/launch-metadata.txt"
echo "Instrumentation and offline launcher smoke test passed on ${emulator_label}."
