from __future__ import annotations

from pathlib import Path
import tempfile
import unittest

from scripts import verify_android_policy as policy


class VerifyAndroidPolicyTest(unittest.TestCase):
    def test_allows_offline_dependencies_and_merged_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(root / "gradle" / "libs.versions.toml", 'room = "2.8.5"\n')
            self._write_valid_app_policy(root, include_merged_manifest=True)

            findings = policy.run_checks(
                root,
                require_merged_manifests=True,
                dependency_reports=[],
            )

            self.assertEqual([], findings)

    def test_rejects_forbidden_dependency_and_internet_permission(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(
                root / "app" / "build.gradle.kts",
                'implementation("com.squareup.okhttp3:okhttp:5.0.0")\n',
            )
            self._write_valid_app_policy(root, include_internet_permission=True)

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            reasons = {finding.reason for finding in findings}
            self.assertIn("network client SDK", reasons)
            self.assertIn("INTERNET permission", reasons)

    def test_allows_coarse_and_fine_foreground_location_permissions(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_valid_app_policy(root)
            manifest = root / "app" / "src" / "main" / "AndroidManifest.xml"
            self._write(
                manifest,
                manifest.read_text(encoding="utf-8").replace(
                    "  <application",
                    """  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  <application""",
                ),
            )

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            self.assertEqual([], findings)

    def test_rejects_background_and_foreground_service_location(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_valid_app_policy(root)
            manifest = root / "app" / "src" / "main" / "AndroidManifest.xml"
            self._write(
                manifest,
                manifest.read_text(encoding="utf-8")
                .replace(
                    "  <application",
                    """  <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
  <application""",
                )
                .replace(
                    "    android:dataExtractionRules=\"@xml/data_extraction_rules\" />",
                    """    android:dataExtractionRules="@xml/data_extraction_rules">
    <service
      android:name=".TrackingService"
      android:foregroundServiceType="dataSync|location" />
  </application>""",
                ),
            )

            reasons = {
                finding.reason
                for finding in policy.run_checks(
                    root,
                    require_merged_manifests=False,
                    dependency_reports=[],
                )
            }

            self.assertEqual(
                {
                    "background location permission",
                    "location foreground-service permission",
                    "location foreground-service declaration",
                },
                reasons,
            )

    def test_scans_resolved_dependency_report(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            report = root / "reports" / "runtime.txt"
            self._write(report, "+--- io.sentry:sentry-android:8.0.0\n")

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[report],
            )

            self.assertEqual(["analytics or telemetry SDK"], [item.reason for item in findings])

    def test_rejects_group_and_name_version_catalog_notation(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(
                root / "gradle" / "libs.versions.toml",
                'ktor = { group = "io.ktor", name = "ktor-client-core", version = "3.0.0" }\n',
            )

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            self.assertEqual(["network client SDK"], [item.reason for item in findings])

    def test_rejects_dynamic_range_and_preview_dependency_selectors(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(
                root / "gradle" / "libs.versions.toml",
                """[versions]
dynamic = "1.+"
range = "[1.0,2.0)"
candidate = "2.0.0-RC1"
milestone = "2.0-M1"
eap = "2.0-eap"
""",
            )
            self._write(
                root / "app" / "build.gradle.kts",
                'implementation("example:library:3.0.0-SNAPSHOT")\n',
            )

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            self.assertEqual(
                ["unstable or dynamic dependency selector"] * 6,
                [item.reason for item in findings],
            )

    def test_does_not_treat_application_version_name_as_dependency_selector(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(
                root / "app" / "build.gradle.kts",
                """android { defaultConfig { versionName = "0.1.0-alpha.1" } }
dependencies { implementation("example:library:1.2.3") }
""",
            )

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            self.assertEqual([], findings)

    def test_scans_build_feature_module(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(
                root / "feature" / "build" / "build.gradle.kts",
                'implementation("com.squareup.retrofit2:retrofit:3.0.0")\n',
            )

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            self.assertEqual(["network client SDK"], [item.reason for item in findings])

    def test_requires_generated_manifest_after_assembly(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            findings = policy.run_checks(
                root,
                require_merged_manifests=True,
                dependency_reports=[],
            )

            self.assertEqual(
                ["no generated merged or packaged Android manifest was found"],
                [item.reason for item in findings],
            )

    def test_rejects_manifest_and_backup_configuration_that_is_not_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_valid_app_policy(root)
            manifest = root / "app" / "src" / "main" / "AndroidManifest.xml"
            self._write(
                manifest,
                manifest.read_text(encoding="utf-8").replace(
                    'android:allowBackup="false"',
                    'android:allowBackup="true"',
                ),
            )
            self._write(
                root / "app" / "src" / "main" / "res" / "xml" / "backup_rules.xml",
                """<full-backup-content>
  <include domain="database" path="." />
  <exclude domain="root" path="." />
</full-backup-content>
""",
            )
            self._write(
                root
                / "app"
                / "src"
                / "main"
                / "res"
                / "xml"
                / "data_extraction_rules.xml",
                "<data-extraction-rules>",
            )

            reasons = {
                finding.reason
                for finding in policy.run_checks(
                    root,
                    require_merged_manifests=False,
                    dependency_reports=[],
                )
            }

            self.assertIn('app manifest must set android:allowBackup="false"', reasons)
            self.assertIn("backup rules must not include data", reasons)
            self.assertIn('backup rules must exclude database path "."', reasons)
            self.assertIn("malformed data extraction rules XML", reasons)

    def test_rejects_merged_manifest_that_reenables_backup(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_valid_app_policy(root, include_merged_manifest=True)
            merged_manifest = (
                root
                / "app"
                / "build"
                / "intermediates"
                / "merged_manifests"
                / "devDebug"
                / "processDevDebugManifest"
                / "AndroidManifest.xml"
            )
            self._write(
                merged_manifest,
                merged_manifest.read_text(encoding="utf-8").replace(
                    'android:allowBackup="false"',
                    'android:allowBackup="true"',
                ),
            )

            findings = policy.run_checks(
                root,
                require_merged_manifests=True,
                dependency_reports=[],
            )

            self.assertEqual(
                ['app manifest must set android:allowBackup="false"'],
                [finding.reason for finding in findings],
            )

    def test_requires_all_device_protected_storage_backup_exclusions(self) -> None:
        required_device_domains = {
            "device_root",
            "device_file",
            "device_database",
            "device_sharedpref",
        }
        self.assertTrue(required_device_domains.issubset(policy.BACKUP_DOMAINS))

        for domain in sorted(required_device_domains):
            with self.subTest(domain=domain), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                self._write_valid_app_policy(root)
                backup_rules = (
                    root
                    / "app"
                    / "src"
                    / "main"
                    / "res"
                    / "xml"
                    / "backup_rules.xml"
                )
                extraction_rules = (
                    root
                    / "app"
                    / "src"
                    / "main"
                    / "res"
                    / "xml"
                    / "data_extraction_rules.xml"
                )
                missing_exclusion = f'  <exclude domain="{domain}" path="." />\n'
                nested_missing_exclusion = (
                    f'    <exclude domain="{domain}" path="." />\n'
                )
                self._write(
                    backup_rules,
                    backup_rules.read_text(encoding="utf-8").replace(
                        missing_exclusion,
                        "",
                    ),
                )
                self._write(
                    extraction_rules,
                    extraction_rules.read_text(encoding="utf-8").replace(
                        nested_missing_exclusion,
                        "",
                    ),
                )

                reasons = {
                    finding.reason
                    for finding in policy.run_checks(
                        root,
                        require_merged_manifests=False,
                        dependency_reports=[],
                    )
                }

                self.assertIn(
                    f'backup rules must exclude {domain} path "."',
                    reasons,
                )
                self.assertIn(
                    f'cloud-backup must exclude {domain} path "."',
                    reasons,
                )
                self.assertIn(
                    f'device-transfer must exclude {domain} path "."',
                    reasons,
                )

    def test_rejects_sqlite_attach_only_in_production_source(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(
                root / "core" / "database" / "src" / "main" / "kotlin" / "Store.kt",
                'val sql = "ATTACH DATABASE ? AS verified_pack"\n',
            )
            self._write(
                root / "core" / "database" / "src" / "test" / "kotlin" / "StoreTest.kt",
                'val sql = "ATTACH DATABASE ? AS test_pack"\n',
            )

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            self.assertEqual(
                ["SQLite ATTACH is prohibited in production code"],
                [finding.reason for finding in findings],
            )

    def test_rejects_feature_persistence_implementation_access(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(
                root / "feature" / "now" / "src" / "main" / "kotlin" / "NowStore.kt",
                """import androidx.room.Dao
import io.github.gilnetizen.aseh.core.database.OperationalStore
@Dao interface NowDao
""",
            )
            self._write(
                root / "feature" / "now" / "build.gradle.kts",
                'implementation(project(path = ":core:database"))\n',
            )

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            self.assertEqual(
                [
                    "feature module references a persistence implementation",
                    "feature module references a persistence implementation",
                    "feature module references a persistence implementation",
                    "feature module depends directly on core database",
                ],
                [finding.reason for finding in findings],
            )

    def test_allows_isolated_fixture_without_app_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write(root / "gradle" / "libs.versions.toml", 'room = "2.8.5"\n')

            findings = policy.run_checks(
                root,
                require_merged_manifests=False,
                dependency_reports=[],
            )

            self.assertEqual([], findings)

    @staticmethod
    def _write(path: Path, content: str) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    @classmethod
    def _write_valid_app_policy(
        cls,
        root: Path,
        *,
        include_internet_permission: bool = False,
        include_merged_manifest: bool = False,
    ) -> None:
        permission = (
            '  <uses-permission android:name="android.permission.INTERNET" />\n'
            if include_internet_permission
            else ""
        )
        cls._write(
            root / "app" / "src" / "main" / "AndroidManifest.xml",
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
{permission}  <application
    android:allowBackup="false"
    android:fullBackupContent="@xml/backup_rules"
    android:dataExtractionRules="@xml/data_extraction_rules" />
</manifest>
""".format(permission=permission),
        )
        exclusions = "\n".join(
            f'  <exclude domain="{domain}" path="." />'
            for domain in sorted(policy.BACKUP_DOMAINS)
        )
        cls._write(
            root / "app" / "src" / "main" / "res" / "xml" / "backup_rules.xml",
            f"<full-backup-content>\n{exclusions}\n</full-backup-content>\n",
        )
        nested_exclusions = exclusions.replace("  <", "    <")
        cls._write(
            root
            / "app"
            / "src"
            / "main"
            / "res"
            / "xml"
            / "data_extraction_rules.xml",
            f"""<data-extraction-rules>
  <cloud-backup disableIfNoEncryptionCapabilities="true">
{nested_exclusions}
  </cloud-backup>
  <device-transfer>
{nested_exclusions}
  </device-transfer>
</data-extraction-rules>
""",
        )
        if include_merged_manifest:
            cls._write(
                root
                / "app"
                / "build"
                / "intermediates"
                / "merged_manifests"
                / "devDebug"
                / "processDevDebugManifest"
                / "AndroidManifest.xml",
                """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application
    android:allowBackup="false"
    android:fullBackupContent="@xml/backup_rules"
    android:dataExtractionRules="@xml/data_extraction_rules" />
</manifest>
""",
            )


if __name__ == "__main__":
    unittest.main()
