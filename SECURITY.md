# Security Policy

## Supported versions

Security fixes are applied to the latest published release and the default branch. Pre-release builds may change rapidly, but credible reports about them are still welcome.

## Reporting a vulnerability

Use GitHub's private security-advisory flow at [Report a vulnerability](https://github.com/Gil-netizen/aseh/security/advisories/new). Do not open a public issue for a suspected vulnerability or include live credentials, private case data, or exploit details in public discussion.

Include, when available:

- the affected version, commit, component, or content pack;
- reproduction steps and the security impact;
- prerequisites, device and Android version, and relevant configuration;
- a minimal proof of concept with secrets and personal data removed;
- any suggested mitigation or known workaround.

You should receive an acknowledgement after the report is seen. Assessment and remediation time depend on severity and complexity. The maintainer will coordinate disclosure with the reporter and will credit the reporter unless anonymity is requested.

## Sensitive areas

Reports are especially useful for issues involving local case-record encryption, API-key storage or disclosure, pack signature or checksum verification, archive extraction, WebView or print rendering, exported files, logs, backups, network transport, prompt injection, and bypasses of high-consequence case-preparation safeguards.

Do not test against other people's data or accounts, degrade a service, persist access, or publish an exploit before a fix and disclosure plan are agreed. Good-faith research that respects these limits will be handled constructively.

## Credentials found in the repository

Treat a committed credential as compromised even if it is quickly removed. Report it privately, revoke or rotate it at the provider, and remove it from current code. Rewriting Git history is not a substitute for rotation.
