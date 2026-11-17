This policy explains how HyperAlarm (the "module") handles your information. The short version: it collects nothing and uploads nothing. Your logs and settings stay on this device. The details follow.

## 0. Version and effective date

- 0.1. Policy version: 1.0
- 0.2. Effective date: 15 November 2026
- 0.3. This policy ships with every build.

## 1. What is stored on your device

- 1.1. Script logs: start time, exit code, standard output and standard error of each script run. Stored in the module's database and in its external storage directory.
- 1.2. App logs: the module's own runtime log, used for troubleshooting.
- 1.3. Crash records: short exception summaries used by safe mode, kept in the module's private directory.
- 1.4. Settings: language, log level, list filters and so on, kept in the module's preference file.
- 1.5. Lock screen password: written only if you enter one in Settings, mode 600, readable by root and by this module, used to unlock the screen before a silent task runs.
- 1.6. None of the above leaves your device unless you export it, share it, or post it in an issue yourself.

## 2. What we do not collect

- 2.1. No device identifiers (IMEI, Android ID, serial number).
- 2.2. No location data.
- 2.3. No contacts, messages or call logs.
- 2.4. No copies of your scripts.
- 2.5. The module contains no analytics, crash reporting or advertising SDK.

## 3. The two network requests

- 3.1. The About page calls public GitHub endpoints to read the author avatar and the contributor list. Requests go only to api.github.com and avatars.githubusercontent.com, carry GitHub's standard User-Agent, and carry no device or user identifier. GitHub therefore sees your IP address.
- 3.2. The setup wizard and the settings page fetch the text of the user agreement and this policy from GitHub so the documents can be updated independently of a release. These requests also only carry standard headers. If the fetch fails, a bundled copy is shown, or a failure notice if that is unavailable too.
- 3.3. Beyond these, the module makes no network requests.

## 4. Sharing with third parties

- 4.1. We share nothing with anyone, because we collect nothing.
- 4.2. The GitHub requests above are your own interaction with GitHub and are subject to GitHub's privacy policy.
- 4.3. When you export and share logs, the recipient is chosen by you; that is not a disclosure by us.

## 5. Retention and deletion

- 5.1. Logs are trimmed automatically to a fixed cap, oldest first.
- 5.2. You can clear the logs in the app, or delete the module's data outright.
- 5.3. Uninstalling the module deletes everything in its private directories.
- 5.4. Log copies mirrored to external storage have to be removed by you before or after uninstalling.

## 6. Your rights

- 6.1. You can view, export and erase everything the module stores, at any time.
- 6.2. You can refuse any permission; the matching feature stops working.
- 6.3. You can clear the lock screen password in Settings; the file is emptied.

## 7. Minors

- 7.1. The module assumes a user with some system knowledge. Minors should use it with a guardian's guidance.
- 7.2. Because it involves root and script execution, we do not recommend independent use by minors.

## 8. Changes to this policy

- 8.1. This policy may be revised with a release; the revised text ships with the new build and is published in the repository.
- 8.2. If a revision introduces new handling of information, the release notes say so explicitly.

## 9. Contact

- 9.1. For privacy questions, open an issue in the project repository.
- 9.2. The source code is public and auditable, so you can verify the statements above yourself.
