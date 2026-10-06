#!/usr/bin/env python3
"""Fail closed when the Android project declares online or telemetry surfaces.

The walking skeleton is intentionally offline and unauthenticated. This checker
scans Gradle declarations, dependency verification/lock files, resolved Gradle
dependency reports, source manifests, and generated merged manifests. It emits
only dependency identifiers and paths; it never prints file contents.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
import re
import sys
from typing import Iterable, Sequence
import xml.etree.ElementTree as ET


INTERNET_PERMISSION = re.compile(r"android\.permission\.INTERNET\b")
ANDROID_ATTRIBUTE = "{http://schemas.android.com/apk/res/android}"
APP_MANIFEST = Path("app/src/main/AndroidManifest.xml")
BACKUP_RULES = Path("app/src/main/res/xml/backup_rules.xml")
DATA_EXTRACTION_RULES = Path("app/src/main/res/xml/data_extraction_rules.xml")
BACKUP_DOMAINS = frozenset({"root", "file", "database", "sharedpref", "external"})

SQLITE_ATTACH = re.compile(
    r"\bATTACH\s+(?:DATABASE\b|[^;\r\n]*\s+AS\b)",
    re.IGNORECASE,
)
FEATURE_PERSISTENCE_REFERENCES: tuple[tuple[str, re.Pattern[str]], ...] = (
    (
        "feature module references a persistence implementation",
        re.compile(
            r"(?:\bio\.github\.gilnetizen\.aseh\.core\.database(?:\.|\b)"
            r"|\bio\.github\.gilnetizen\.aseh\.AppGraph(?:\.|\b)"
            r"|\b(?:android\.database\.sqlite|androidx\.(?:room|datastore|sqlite))\."
            r"|\b(?:RoomDatabase|SupportSQLiteDatabase|SQLiteDatabase|SQLiteOpenHelper)\b"
            r"|\b[A-Za-z_][A-Za-z0-9_]*Dao\b"
            r"|\b(?:getDatabasePath|openOrCreateDatabase|deleteDatabase)\s*\()"
        ),
    ),
)
FEATURE_DATABASE_DEPENDENCY = re.compile(
    r"(?:project\s*\(\s*(?:path\s*=\s*)?[\"']:core:database[\"']\s*\)"
    r"|projects\.core\.database)"
)

# Match Maven groups/artifacts and Gradle plugin ids. The expressions are kept
# coordinate-specific so repository URLs and ordinary product prose do not
# trigger the gate.
FORBIDDEN_DEPENDENCIES: tuple[tuple[str, re.Pattern[str]], ...] = (
    (
        "network client SDK",
        re.compile(
            r"(?:com\.squareup\.(?:okhttp3|retrofit2|okhttp)|"
            r"io\.ktor|com\.android\.volley|org\.chromium\.net|"
            r"com\.apollographql\.apollo(?:3)?|com\.github\.kittinunf\.fuel)",
            re.IGNORECASE,
        ),
    ),
    (
        "authentication SDK",
        re.compile(
            r"(?:androidx\.credentials|com\.google\.android\.libraries\.identity\.googleid|"
            r"com\.google\.android\.gms[^\n]*play-services-auth|com\.auth0\.android|"
            r"net\.openid[^\n]*appauth|com\.amplifyframework[^\n]*aws-auth|"
            r"com\.amazonaws[^\n]*aws-android-sdk-cognito)",
            re.IGNORECASE,
        ),
    ),
    (
        "analytics or telemetry SDK",
        re.compile(
            r"(?:com\.google\.firebase|com\.google\.gms\.google-services|"
            r"com\.google\.android\.gms:play-services-analytics|"
            r"com\.android\.installreferrer|com\.segment\.analytics|"
            r"com\.amplitude|com\.mixpanel|com\.appsflyer|com\.adjust\.sdk|"
            r"com\.braze|io\.sentry|com\.datadoghq|com\.microsoft\.appcenter|"
            r"com\.newrelic|com\.posthog|io\.opentelemetry)",
            re.IGNORECASE,
        ),
    ),
)

UNSTABLE_VERSION_SELECTOR = re.compile(
    r"""
    latest(?:\.[a-z]+)?
    |(?:^|\.)\+$
    |snapshot|alpha|beta|preview|milestone|eap
    |(?:^|[._-])rc(?:[._-]?\d+)?(?:$|[._-])
    |(?:^|[._-])m\d+(?:$|[._-])
    |(?:^|[._-])dev(?:\d+)?(?:$|[._-])
    |[\[\](),]
    """,
    re.IGNORECASE | re.VERBOSE,
)
CATALOG_VERSION_ASSIGNMENT = re.compile(
    r'^\s*[A-Za-z0-9_.-]+\s*=\s*"([^"]+)"\s*$'
)
WRAPPER_VERSION = re.compile(r"gradle-([^/]+?)-(?:bin|all)\.zip")
DEPENDENCY_COORDINATE = re.compile(r'''["']([^"'$\s]+:[^"'$\s]+:([^"']+))["']''')
PLUGIN_VERSION = re.compile(r'''\bversion\s*(?:=\s*)?["']([^"']+)["']''')

EXCLUDED_DIRECTORIES = {".git", ".gradle", ".idea", "dist"}
GRADLE_FILE_SUFFIXES = (".gradle", ".gradle.kts", ".lockfile")
GRADLE_FILE_NAMES = {
    "gradle.properties",
    "libs.versions.toml",
    "verification-metadata.xml",
}


@dataclass(frozen=True)
class Finding:
    path: Path
    line: int
    reason: str


def _is_excluded(path: Path, root: Path) -> bool:
    try:
        relative = path.relative_to(root)
    except ValueError:
        return False
    return any(part in EXCLUDED_DIRECTORIES for part in relative.parts[:-1])


def dependency_config_files(root: Path) -> Iterable[Path]:
    for path in root.rglob("*"):
        if not path.is_file() or _is_excluded(path, root):
            continue
        lower_name = path.name.lower()
        if lower_name in GRADLE_FILE_NAMES or lower_name.endswith(GRADLE_FILE_SUFFIXES):
            yield path


def source_manifests(root: Path) -> Iterable[Path]:
    for path in root.rglob("AndroidManifest.xml"):
        if not path.is_file() or _is_excluded(path, root):
            continue
        if "src" in path.relative_to(root).parts:
            yield path


def merged_manifests(root: Path) -> list[Path]:
    manifests: list[Path] = []
    for path in root.rglob("AndroidManifest.xml"):
        if not path.is_file():
            continue
        parts = path.relative_to(root).parts
        if "build" not in parts or "intermediates" not in parts:
            continue
        if any("merged_manifest" in part or "packaged_manifest" in part for part in parts):
            manifests.append(path)
    return sorted(manifests)


def production_source_files(root: Path) -> Iterable[Path]:
    for path in root.rglob("*"):
        if not path.is_file() or _is_excluded(path, root):
            continue
        parts = path.relative_to(root).parts
        if path.suffix.lower() not in {".java", ".kt", ".sql"}:
            continue
        if any(parts[index : index + 2] == ("src", "main") for index in range(len(parts) - 1)):
            yield path


def feature_source_files(root: Path) -> Iterable[Path]:
    for path in production_source_files(root):
        if path.relative_to(root).parts[:1] == ("feature",):
            yield path


def feature_build_files(root: Path) -> Iterable[Path]:
    feature_root = root / "feature"
    if not feature_root.is_dir():
        return
    for path in feature_root.rglob("build.gradle*"):
        if path.is_file() and not _is_excluded(path, root):
            yield path


def _scan_lines(path: Path, patterns: Sequence[tuple[str, re.Pattern[str]]]) -> list[Finding]:
    try:
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError as exc:
        return [Finding(path, 0, f"could not read file: {exc}")]

    findings: list[Finding] = []
    for line_number, line in enumerate(lines, start=1):
        for reason, pattern in patterns:
            if pattern.search(line):
                findings.append(Finding(path, line_number, reason))
    return findings


def _parse_xml(path: Path, description: str) -> tuple[ET.Element | None, list[Finding]]:
    try:
        return ET.parse(path).getroot(), []
    except ET.ParseError as exc:
        line = exc.position[0] if exc.position else 0
        return None, [Finding(path, line, f"malformed {description} XML")]
    except OSError as exc:
        return None, [Finding(path, 0, f"could not read {description}: {exc}")]


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _required_exclusion_findings(
    path: Path,
    parent: ET.Element,
    section: str,
) -> list[Finding]:
    exclusions = {
        (element.get("domain"), element.get("path"))
        for element in parent
        if _local_name(element.tag) == "exclude"
    }
    return [
        Finding(path, 0, f'{section} must exclude {domain} path "."')
        for domain in sorted(BACKUP_DOMAINS)
        if (domain, ".") not in exclusions
    ]


def _app_manifest_attribute_findings(
    app_manifest: Path,
    app_manifest_root: ET.Element,
) -> list[Finding]:
    applications = [
        element for element in app_manifest_root if _local_name(element.tag) == "application"
    ]
    if len(applications) != 1:
        return [Finding(app_manifest, 0, "app manifest must contain exactly one application")]

    findings: list[Finding] = []
    application = applications[0]
    required_attributes = {
        "allowBackup": "false",
        "fullBackupContent": "@xml/backup_rules",
        "dataExtractionRules": "@xml/data_extraction_rules",
    }
    for name, expected in required_attributes.items():
        if application.get(f"{ANDROID_ATTRIBUTE}{name}") != expected:
            findings.append(
                Finding(
                    app_manifest,
                    0,
                    f'app manifest must set android:{name}="{expected}"',
                )
            )
    return findings


def _backup_policy_findings(root: Path, app_manifest_root: ET.Element) -> list[Finding]:
    app_manifest = root / APP_MANIFEST
    findings = _app_manifest_attribute_findings(app_manifest, app_manifest_root)

    backup_rules_path = root / BACKUP_RULES
    if not backup_rules_path.is_file():
        findings.append(Finding(backup_rules_path, 0, "required backup rules resource is missing"))
    else:
        backup_root, parse_findings = _parse_xml(backup_rules_path, "backup rules")
        findings.extend(parse_findings)
        if backup_root is not None:
            if _local_name(backup_root.tag) != "full-backup-content":
                findings.append(
                    Finding(backup_rules_path, 0, "backup rules root must be full-backup-content")
                )
            if any(_local_name(element.tag) == "include" for element in backup_root.iter()):
                findings.append(Finding(backup_rules_path, 0, "backup rules must not include data"))
            findings.extend(
                _required_exclusion_findings(backup_rules_path, backup_root, "backup rules")
            )

    extraction_rules_path = root / DATA_EXTRACTION_RULES
    if not extraction_rules_path.is_file():
        findings.append(
            Finding(extraction_rules_path, 0, "required data extraction rules resource is missing")
        )
    else:
        extraction_root, parse_findings = _parse_xml(extraction_rules_path, "data extraction rules")
        findings.extend(parse_findings)
        if extraction_root is not None:
            if _local_name(extraction_root.tag) != "data-extraction-rules":
                findings.append(
                    Finding(
                        extraction_rules_path,
                        0,
                        "data extraction rules root must be data-extraction-rules",
                    )
                )
            if any(_local_name(element.tag) == "include" for element in extraction_root.iter()):
                findings.append(
                    Finding(extraction_rules_path, 0, "data extraction rules must not include data")
                )

            sections: dict[str, list[ET.Element]] = {
                name: [
                    element
                    for element in extraction_root
                    if _local_name(element.tag) == name
                ]
                for name in ("cloud-backup", "device-transfer")
            }
            for section_name, elements in sections.items():
                if len(elements) != 1:
                    findings.append(
                        Finding(
                            extraction_rules_path,
                            0,
                            f"data extraction rules must contain exactly one {section_name}",
                        )
                    )
                    continue
                findings.extend(
                    _required_exclusion_findings(
                        extraction_rules_path,
                        elements[0],
                        section_name,
                    )
                )

            cloud_sections = sections["cloud-backup"]
            if (
                len(cloud_sections) == 1
                and cloud_sections[0].get("disableIfNoEncryptionCapabilities") != "true"
            ):
                findings.append(
                    Finding(
                        extraction_rules_path,
                        0,
                        'cloud-backup must set disableIfNoEncryptionCapabilities="true"',
                    )
                )

    return findings


def _unstable_selector_findings(path: Path) -> list[Finding]:
    try:
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError as exc:
        return [Finding(path, 0, f"could not read file: {exc}")]

    findings: list[Finding] = []
    for line_number, line in enumerate(lines, start=1):
        selectors: list[str] = []
        if path.name == "libs.versions.toml":
            match = CATALOG_VERSION_ASSIGNMENT.match(line)
            if match:
                selectors.append(match.group(1))
        elif path.name == "gradle-wrapper.properties":
            selectors.extend(match.group(1) for match in WRAPPER_VERSION.finditer(line))
        elif path.name.endswith((".gradle", ".gradle.kts")):
            selectors.extend(match.group(2) for match in DEPENDENCY_COORDINATE.finditer(line))
            selectors.extend(match.group(1) for match in PLUGIN_VERSION.finditer(line))

        findings.extend(
            Finding(path, line_number, "unstable or dynamic dependency selector")
            for selector in selectors
            if UNSTABLE_VERSION_SELECTOR.search(selector)
        )
    return findings


def run_checks(
    root: Path,
    *,
    require_merged_manifests: bool,
    dependency_reports: Sequence[Path],
) -> list[Finding]:
    root = root.resolve()
    findings: list[Finding] = []

    for path in sorted(dependency_config_files(root)):
        findings.extend(_scan_lines(path, FORBIDDEN_DEPENDENCIES))
        findings.extend(_unstable_selector_findings(path))

    for report in dependency_reports:
        report_path = report if report.is_absolute() else root / report
        if not report_path.is_file():
            findings.append(Finding(report_path, 0, "dependency report is missing"))
            continue
        findings.extend(_scan_lines(report_path, FORBIDDEN_DEPENDENCIES))

    for path in sorted(production_source_files(root)):
        findings.extend(
            _scan_lines(path, (("SQLite ATTACH is prohibited in production code", SQLITE_ATTACH),))
        )

    for path in sorted(feature_source_files(root)):
        findings.extend(_scan_lines(path, FEATURE_PERSISTENCE_REFERENCES))

    for path in sorted(feature_build_files(root)):
        findings.extend(
            _scan_lines(
                path,
                (("feature module depends directly on core database", FEATURE_DATABASE_DEPENDENCY),),
            )
        )

    manifest_paths = sorted(source_manifests(root))
    generated_manifests = merged_manifests(root)
    manifest_paths.extend(generated_manifests)
    parsed_manifests: dict[Path, ET.Element] = {}
    for path in manifest_paths:
        findings.extend(_scan_lines(path, (("INTERNET permission", INTERNET_PERMISSION),)))
        manifest_root, parse_findings = _parse_xml(path, "Android manifest")
        findings.extend(parse_findings)
        if manifest_root is not None:
            parsed_manifests[path] = manifest_root

    app_manifest = root / APP_MANIFEST
    if app_manifest.is_file():
        app_manifest_root = parsed_manifests.get(app_manifest)
        if app_manifest not in manifest_paths:
            app_manifest_root, parse_findings = _parse_xml(app_manifest, "Android manifest")
            findings.extend(parse_findings)
        if app_manifest_root is not None:
            findings.extend(_backup_policy_findings(root, app_manifest_root))

    for path in generated_manifests:
        relative = path.relative_to(root)
        if relative.parts[:2] != ("app", "build") or any(
            "androidtest" in part.casefold() for part in relative.parts
        ):
            continue
        manifest_root = parsed_manifests.get(path)
        if manifest_root is not None:
            findings.extend(_app_manifest_attribute_findings(path, manifest_root))

    if require_merged_manifests and not generated_manifests:
        findings.append(
            Finding(
                root / "app" / "build" / "intermediates",
                0,
                "no generated merged or packaged Android manifest was found",
            )
        )

    return findings


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path.cwd(),
        help="repository root (default: current directory)",
    )
    parser.add_argument(
        "--require-merged-manifests",
        action="store_true",
        help="fail if assembly did not produce a merged or packaged manifest",
    )
    parser.add_argument(
        "--dependency-report",
        action="append",
        default=[],
        type=Path,
        help="resolved Gradle dependency report to scan; may be repeated",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    findings = run_checks(
        args.root,
        require_merged_manifests=args.require_merged_manifests,
        dependency_reports=args.dependency_report,
    )

    if findings:
        print("Android offline policy check failed:", file=sys.stderr)
        for finding in findings:
            location = f"{finding.path}:{finding.line}" if finding.line else str(finding.path)
            print(f"- {location}: {finding.reason}", file=sys.stderr)
        return 1

    checked = ["source declarations"]
    if args.dependency_report:
        checked.append("resolved dependencies")
    if args.require_merged_manifests:
        checked.append("merged manifests")
    print(f"Android offline policy check passed for {', '.join(checked)}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
