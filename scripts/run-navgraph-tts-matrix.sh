#!/usr/bin/env bash
set -euo pipefail

TASK_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${TASK_SCRIPT_DIR}/.." && pwd)"
ADB_BIN="${ADB:-adb}"
TEST_APP_PACKAGE="com.theustech.blindcheck_testeapp"
TEST_APP_COMPONENT="${TEST_APP_PACKAGE}/.MainActivity"
TRACKING_PACKAGE="com.theustech.blindcheck_tracking_app"
APPROACH_EXTRA="com.theustech.blindcheck_testeapp.NAVGRAPH_ACCESSIBILITY_APPROACH"
REPORT_DIR="${PROJECT_ROOT}/blindcheck-test-app/build/reports/navgraph-tts-spy"
SUMMARY_FILE="${REPORT_DIR}/summary.tsv"
RUNS="${NAVGRAPH_TTS_RUNS:-1}"

APPROACHES=(
  baseline
  unique-labels
  unique-node-ids
  recreated-semantics
  pane-title
  imperative-focus
  agnostic-focus-reset
  retire-leaving-screen
  unique-labels-pane-title
  legacy-combined-reset
)

die() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

broadcast() {
  local action="$1"
  "${ADB_BIN}" shell am broadcast -p "${TRACKING_PACKAGE}" -a "${action}" >/dev/null
}

focus_page_one_button() {
  local approach="$1"
  local expected_label="Continuar"
  if [[ "${approach}" == "unique-labels" || "${approach}" == "unique-labels-pane-title" ]]; then
    expected_label="continuar 1"
  fi

  "${ADB_BIN}" logcat -c
  for _ in 1 2 3 4 5; do
    broadcast "com.theustech.blindcheck.ACTION_NEXT"
    sleep 1
    local current_log
    current_log="$("${ADB_BIN}" logcat -d -s BlindCheckAnnounce:I)"
    if grep -q "FOCUS ${expected_label}, Botão" <<< "${current_log}"; then
      return 0
    fi
  done
  return 1
}

capture_approach() {
  local approach="$1"
  local run="$2"
  local log_file="${REPORT_DIR}/${approach}-run-${run}.log"
  local tree_file="${REPORT_DIR}/${approach}-run-${run}-window.xml"

  "${ADB_BIN}" shell am force-stop "${TEST_APP_PACKAGE}"
  "${ADB_BIN}" shell am start -W -n "${TEST_APP_COMPONENT}" \
    --es "${APPROACH_EXTRA}" "${approach}" >/dev/null
  sleep 2

  focus_page_one_button "${approach}" || die "Could not focus the page-one button for ${approach}."

  "${ADB_BIN}" logcat -c
  broadcast "com.theustech.blindcheck.ACTION_ACTIVATE"
  sleep 3
  "${ADB_BIN}" logcat -d -s BlindCheckAnnounce:I BlindCheckTracker:D > "${log_file}"
  "${ADB_BIN}" shell uiautomator dump /sdcard/blindcheck-navgraph-tts.xml >/dev/null
  "${ADB_BIN}" shell cat /sdcard/blindcheck-navgraph-tts.xml > "${tree_file}"

  local screen="NOT_TELA_2"
  if grep -q 'text="Tela 2"' "${tree_file}"; then
    screen="TELA_2"
  fi

  local tts_sequence
  tts_sequence="$(sed -n 's/.*BlindCheckAnnounce: TTS //p' "${log_file}" | paste -sd '|' -)"
  local final_focus
  final_focus="$(sed -n 's/.*BlindCheckAnnounce: FOCUS //p' "${log_file}" | tail -1)"
  local consecutive_duplicates
  consecutive_duplicates="$(sed -n 's/.*BlindCheckAnnounce: TTS //p' "${log_file}" | awk 'previous == $0 { count += 1 } { previous = $0 } END { print count + 0 }')"

  printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
    "${run}" "${approach}" "${screen}" "${consecutive_duplicates}" "${final_focus}" "${tts_sequence}" \
    >> "${SUMMARY_FILE}"

  [[ "${screen}" == "TELA_2" ]] || die "${approach} did not finish on Tela 2. See ${log_file}."
}

[[ "$("${ADB_BIN}" devices | awk 'NR > 1 && $2 == "device" { count += 1 } END { print count + 0 }')" == "1" ]] ||
  die "Exactly one connected Android device is required."

TEST_APP_PATH="$("${ADB_BIN}" shell pm path "${TEST_APP_PACKAGE}")"
grep -q '^package:' <<< "${TEST_APP_PATH}" ||
  die "The test app is not installed. Run 'make install-all' first."

[[ "$("${ADB_BIN}" shell settings get secure tts_default_synth | tr -d '\r')" == "${TRACKING_PACKAGE}" ]] ||
  die "BlindCheck TTS is not selected. Run 'make enable-tts' first."

ACCESSIBILITY_DUMP="$("${ADB_BIN}" shell dumpsys accessibility)"
grep -q 'label=TalkBack' <<< "${ACCESSIBILITY_DUMP}" ||
  die "TalkBack is not bound. Run 'make enable-tracker' first."

mkdir -p "${REPORT_DIR}"
[[ "${RUNS}" =~ ^[1-9][0-9]*$ ]] || die "NAVGRAPH_TTS_RUNS must be a positive integer."
printf 'run\tapproach\tscreen\tconsecutive_duplicate_tts\tfinal_focus\ttts_sequence\n' > "${SUMMARY_FILE}"

for run in $(seq 1 "${RUNS}"); do
  for approach in "${APPROACHES[@]}"; do
    printf 'Capturing run %s/%s: %s...\n' "${run}" "${RUNS}" "${approach}"
    capture_approach "${approach}" "${run}"
  done
done

printf '\nTTS matrix captured at %s\n' "${SUMMARY_FILE}"
column -t -s $'\t' "${SUMMARY_FILE}"
