#!/usr/bin/env bash
#
# Runs the whole stabilization regression suite - both standalone Java harnesses and the Python brain
# tests - and prints one combined pass/fail summary. Exit code is 0 only if every suite passes.
#
# Usage (from anywhere):
#   ./tests/run_all.sh
#
# Requirements: a JDK (javac/java on PATH; JDK 21+ is fine - the harnesses use no JDK-25 features) and
# Python 3 with the brain's deps installed (flask, openai, python-dotenv). Neither the game classpath
# nor Ant is needed: the tested classes depend only on the JDK/Python standard libraries.

set -u

# Resolve the project root (this script lives in <root>/tests) and work from there so all the relative
# paths below are stable no matter where the script is invoked from.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "${SCRIPT_DIR}")"
cd "${ROOT_DIR}" || exit 2

# Pick a Python interpreter (prefer python3).
if command -v python3 >/dev/null 2>&1; then
	PY="python3"
elif command -v python >/dev/null 2>&1; then
	PY="python"
else
	PY=""
fi

# The pure production classes under test, and the test classes that exercise them. To add a new Java
# harness later: drop its production class in PROD_SOURCES, its test in TEST_SOURCES, and the test's
# main class name in JAVA_MAIN_CLASSES.
PROD_SOURCES=(
	"java/org/l2jmobius/commons/util/Rnd.java"
	"java/org/l2jmobius/gameserver/managers/FakePlayerChatParsing.java"
	"java/org/l2jmobius/gameserver/managers/FakePlayerStorePricing.java"
	"java/org/l2jmobius/gameserver/managers/FakePlayerNameFactory.java"
)
JAVA_MAIN_CLASSES=(
	"FakePlayerChatParsingTest"
	"FakePlayerStorePricingTest"
	"FakePlayerNameFactoryTest"
)

failures=0
summary=()

record()
{
	# record <label> <ok:0|1>
	if [ "$2" -eq 0 ]; then
		summary+=("PASS  $1")
	else
		summary+=("FAIL  $1")
		failures=$((failures + 1))
	fi
}

echo "==============================================================="
echo " Living World regression suite"
echo " root: ${ROOT_DIR}"
echo "==============================================================="

# ---- Java harnesses -------------------------------------------------------
echo
echo ">>> Java: compiling standalone harnesses"
if ! command -v javac >/dev/null 2>&1; then
	echo "    javac not found on PATH - skipping Java harnesses."
	for main in "${JAVA_MAIN_CLASSES[@]}"; do
		record "java ${main} (skipped: no javac)" 1
	done
else
	rm -rf build/test-classes
	if javac -d build/test-classes "${PROD_SOURCES[@]}" tests/java/*.java; then
		for main in "${JAVA_MAIN_CLASSES[@]}"; do
			echo
			echo ">>> Java: ${main}"
			if java -cp build/test-classes "${main}"; then
				record "java ${main}" 0
			else
				record "java ${main}" 1
			fi
		done
	else
		echo "    Java compilation FAILED."
		for main in "${JAVA_MAIN_CLASSES[@]}"; do
			record "java ${main} (compile failed)" 1
		done
	fi
fi

# ---- Python brain tests ---------------------------------------------------
echo
echo ">>> Python: brain regression tests"
if [ -z "${PY}" ]; then
	echo "    No python interpreter found - skipping."
	record "python brain tests (skipped: no python)" 1
else
	# PROVIDER=ollama lets fpc_brain.py import without a DeepSeek key or a live LLM endpoint.
	py_out="$(PROVIDER=ollama "${PY}" -m unittest discover -s tests -p "test_*.py" -v 2>&1)"
	py_rc=$?
	echo "${py_out}"
	# Mirror CI's guard: a zero-test run reports "OK" but must not count as a pass.
	if [ "${py_rc}" -eq 0 ] && printf '%s' "${py_out}" | grep -Eq "Ran 0 tests"; then
		echo "    No Python tests were collected - treating as failure."
		record "python brain tests (0 collected)" 1
	else
		record "python brain tests" "${py_rc}"
	fi
fi

# ---- Summary --------------------------------------------------------------
echo
echo "==============================================================="
echo " Summary"
echo "==============================================================="
for line in "${summary[@]}"; do
	echo "  ${line}"
done
echo "---------------------------------------------------------------"
if [ "${failures}" -eq 0 ]; then
	echo "  ALL SUITES PASSED"
	echo "==============================================================="
	exit 0
fi
echo "  ${failures} SUITE(S) FAILED"
echo "==============================================================="
exit 1
