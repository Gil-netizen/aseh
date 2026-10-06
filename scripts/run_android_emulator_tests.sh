#!/usr/bin/env bash
set -euo pipefail

# Git Bash otherwise rewrites Android device paths such as /sdcard before
# passing them to the native Windows adb.exe binary.
if [[ -n "${MSYSTEM:-}" ]]; then
  export MSYS_NO_PATHCONV=1
fi

emulator_label="${1:?usage: run_android_emulator_tests.sh <emulator-label>}"
artifact_dir="build/outputs/smoke-screenshots/${emulator_label}"
diagnostic_dir="build/reports/android-sdk/emulator-${emulator_label}"
mkdir -p "${artifact_dir}" "${diagnostic_dir}"

system_image="${ASEH_SYSTEM_IMAGE:?ASEH_SYSTEM_IMAGE must name the pinned emulator image}"
system_image_revision="${ASEH_SYSTEM_IMAGE_REVISION:?ASEH_SYSTEM_IMAGE_REVISION must pin the image revision}"
sdk_inventory="build/reports/android-sdk/installed-post-runner-${emulator_label}.txt"
mkdir -p "$(dirname "${sdk_inventory}")"

android_services_ready() {
  local activity_output
  local android_sdk
  local boot_completed
  local package_output
  local package_service_output
  local user_ce_available
  local user_state_output

  android_sdk="$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || true)"
  boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  user_ce_available="$(adb shell getprop sys.user.0.ce_available 2>/dev/null | tr -d '\r' || true)"
  user_state_output="$(adb shell am get-started-user-state 0 2>/dev/null | tr -d '\r' || true)"
  package_output="$(adb shell cmd package list packages 2>/dev/null | tr -d '\r' || true)"
  activity_output="$(adb shell service check activity 2>/dev/null | tr -d '\r' || true)"
  package_service_output="$(adb shell service check package 2>/dev/null | tr -d '\r' || true)"

  [[ "${android_sdk}" =~ ^[0-9]+$ ]] || return 1
  # The pinned API 26 image does not expose the CE property; the user-state
  # command is the cross-version signal that credential storage is usable.
  if (( 10#${android_sdk} >= 27 )) && [[ "${user_ce_available}" != "true" ]]; then
    return 1
  fi

  [[ "${boot_completed}" == "1" ]] \
    && [[ "${user_state_output}" == "RUNNING_UNLOCKED" ]] \
    && grep -q '^package:' <<<"${package_output}" \
    && grep -Eq '^Service activity: found[[:space:]]*$' <<<"${activity_output}" \
    && grep -Eq '^Service package: found[[:space:]]*$' <<<"${package_service_output}"
}

wait_for_android_services() {
  local consecutive=0
  for _ in $(seq 1 30); do
    if android_services_ready; then
      ((consecutive += 1))
      if (( consecutive >= 3 )); then
        return 0
      fi
    else
      consecutive=0
    fi
    sleep 2
  done
  echo "Android's boot, user credential storage, activity, and package services were not stable within 60 seconds." >&2
  return 1
}

configure_navigation_mode() {
  local navigation_mode="${ASEH_NAVIGATION_MODE:-default}"
  local overlay_inventory="${diagnostic_dir}/navigation-overlays.txt"

  if [[ "${navigation_mode}" == "default" ]]; then
    return 0
  fi

  local overlay_package="com.android.internal.systemui.navbar.${navigation_mode}"
  adb shell cmd overlay list 2>/dev/null | tr -d '\r' >"${overlay_inventory}"
  if ! grep -Fq "${overlay_package}" "${overlay_inventory}"; then
    echo "Required navigation overlay ${overlay_package} is unavailable." >&2
    return 1
  fi

  adb shell cmd overlay enable-exclusive "${overlay_package}" >/dev/null
  wait_for_android_services
  adb shell cmd overlay list 2>/dev/null | tr -d '\r' >"${overlay_inventory}"
  if ! grep -Fq "[x] ${overlay_package}" "${overlay_inventory}"; then
    echo "Navigation overlay ${overlay_package} did not become active." >&2
    return 1
  fi
}

task_snapshot_controller_state() {
  adb shell dumpsys window 2>/dev/null \
    | tr -d '\r' \
    | awk '
      /mSnapshotEnabled=/ { controller_state = $0; next }
      /SnapshotCache Task/ && controller_state != "" {
        sub(/^[[:space:]]+/, "", controller_state)
        print controller_state
        exit
      }
    '
}

assert_task_snapshot_mode() {
  local task_snapshot_mode="${ASEH_TASK_SNAPSHOT_MODE:-default}"
  if [[ "${task_snapshot_mode}" == "default" ]]; then
    return 0
  fi

  local controller_state
  controller_state="$(task_snapshot_controller_state || true)"
  printf '%s\n' "${controller_state}" >"${diagnostic_dir}/task-snapshot-controller.txt"
  if [[ "${controller_state}" != "mSnapshotEnabled=false" ]]; then
    echo "Android task snapshots are not disabled; found ${controller_state:-no controller state}." >&2
    return 1
  fi
}

configure_task_snapshot_mode() {
  local task_snapshot_mode="${ASEH_TASK_SNAPSHOT_MODE:-default}"
  case "${task_snapshot_mode}" in
    default)
      return 0
      ;;
    disabled)
      ;;
    *)
      echo "Unsupported task snapshot mode: ${task_snapshot_mode}." >&2
      return 1
      ;;
  esac

  local android_build_id
  local android_incremental
  local android_sdk
  android_build_id="$(adb shell getprop ro.build.id 2>/dev/null | tr -d '\r' || true)"
  android_incremental="$(adb shell getprop ro.build.version.incremental 2>/dev/null | tr -d '\r' || true)"
  android_sdk="$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || true)"
  if [[ "${android_sdk}" != "37" \
    || "${android_build_id}" != "CE2A.260420.019" \
    || "${android_incremental}" != "15611780" \
    || "${system_image}" != "system-images;android-37.0;google_apis;x86_64" \
    || "${system_image_revision}" != "6" ]]; then
    echo "Task snapshot suppression is validated only for pinned API 37 image revision 6 (CE2A.260420.019/15611780)." >&2
    return 1
  fi

  # Android 17 exposes no named wm shell command for this control. On the
  # exact image above, IWindowManager transaction 137 is
  # setTaskSnapshotEnabled(boolean). Disabling it bypasses capture and the
  # failing asynchronous persistence path without changing app behavior.
  if ! adb shell service call window 137 i32 0 \
    >"${diagnostic_dir}/task-snapshot-command.txt" 2>&1; then
    echo "Unable to disable Android task snapshots on the pinned API 37 image." >&2
    return 1
  fi
  wait_for_android_services
  assert_task_snapshot_mode
}

capture_android_diagnostics() {
  local reason="$1"
  local exit_code="$2"
  local output_dir="${diagnostic_dir}/${reason}"
  mkdir -p "${output_dir}"

  adb get-state >"${output_dir}/adb-state.txt" 2>&1 || true
  adb shell getprop >"${output_dir}/properties.txt" 2>&1 || true
  adb shell service list >"${output_dir}/services.txt" 2>&1 || true
  adb shell service check activity >"${output_dir}/activity-service.txt" 2>&1 || true
  adb shell service check package >"${output_dir}/package-service.txt" 2>&1 || true
  adb shell am get-started-user-state 0 >"${output_dir}/user-0-state.txt" 2>&1 || true
  adb shell cmd overlay list >"${output_dir}/navigation-overlays.txt" 2>&1 || true
  adb shell cat /proc/meminfo >"${output_dir}/meminfo.txt" 2>&1 || true
  adb shell df -k >"${output_dir}/filesystems.txt" 2>&1 || true
  adb shell ps -A >"${output_dir}/processes.txt" 2>&1 || true
  adb shell dumpsys activity activities >"${output_dir}/activities.txt" 2>&1 || true
  task_snapshot_controller_state >"${output_dir}/task-snapshot-controller.txt" 2>&1 || true
  adb shell ls -la /data/tombstones >"${output_dir}/tombstones.txt" 2>&1 || true
  adb logcat -b all -d -v threadtime >"${output_dir}/logcat.txt" 2>&1 || true
  printf 'emulator_label=%s\nexit_code=%s\ngpu_mode=%s\nnavigation_mode=%s\ntask_snapshot_mode=%s\n' \
    "${emulator_label}" "${exit_code}" "${ASEH_GPU_MODE:-unknown}" \
    "${ASEH_NAVIGATION_MODE:-default}" "${ASEH_TASK_SNAPSHOT_MODE:-default}" \
    >"${output_dir}/runner-metadata.txt"
}

capture_on_failure() {
  local exit_code=$?
  trap - EXIT
  if (( exit_code != 0 )); then
    set +e
    capture_android_diagnostics "script-failure" "${exit_code}"
  fi
  exit "${exit_code}"
}
trap capture_on_failure EXIT

wait_for_android_services
configure_navigation_mode

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
configure_task_snapshot_mode

data_partition_report="build/reports/android-sdk/data-partition-${emulator_label}.txt"
adb shell df -k /data | tr -d '\r' | tee "${data_partition_report}"
data_available_kb="$(awk 'NR > 1 { print $(NF - 2); exit }' "${data_partition_report}")"
if [[ ! "${data_available_kb}" =~ ^[0-9]+$ ]] || (( data_available_kb < 2097152 )); then
  echo "The emulator must provide at least 2 GiB free on /data; found ${data_available_kb:-unknown} KiB." >&2
  exit 1
fi

run_connected_suite() {
  local task="$1"

  wait_for_android_services
  assert_task_snapshot_mode
  ./gradlew \
    --no-daemon \
    --no-parallel \
    --stacktrace \
    --dependency-verification strict \
    "${task}"
  wait_for_android_services
  assert_task_snapshot_mode
}

font_scale="${ASEH_FONT_SCALE:-1.0}"
wait_for_android_services
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
wait_for_android_services
adb uninstall io.github.gilnetizen.aseh.dev.debug >/dev/null 2>&1 || true

run_connected_suite :app:connectedDevDebugAndroidTest

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

run_connected_suite :core:database:connectedDebugAndroidTest

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

hierarchy_log="${artifact_dir}/uiautomator-hierarchy.txt"
if adb shell uiautomator dump /sdcard/aseh-window.xml >"${hierarchy_log}" 2>&1 \
  && adb pull /sdcard/aseh-window.xml "${artifact_dir}/window.xml" \
    >>"${hierarchy_log}" 2>&1; then
  :
else
  echo "UI Automator hierarchy capture was unavailable; the PNG evidence was retained." >&2
fi

printf 'application_id=%s\napk=%s\nfont_scale=%s\ngpu_mode=%s\nnavigation_mode=%s\ntask_snapshot_mode=%s\n' \
  "${application_id}" "${apk_path}" "${font_scale}" "${ASEH_GPU_MODE:-unknown}" \
  "${ASEH_NAVIGATION_MODE:-default}" "${ASEH_TASK_SNAPSHOT_MODE:-default}" \
  >"${artifact_dir}/launch-metadata.txt"
echo "Instrumentation and offline launcher smoke test passed on ${emulator_label}."
