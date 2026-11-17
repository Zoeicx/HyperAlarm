HyperAlarm is a non-commercial enhancement module that runs on the Xposed framework and targets Mi Clock. It turns Mi Clock alarms into triggers for root scripts: when an alarm fires, a script you provide can run instead of, or in addition to, ringing. Please read this User Agreement before you continue.

## 0. Version and effective date

- 0.1. Agreement version: 1.0
- 0.2. Effective date: 15 November 2026
- 0.3. This agreement ships with every build; the current version lives in the project repository.

## 1. General

- 1.1. HyperAlarm is developed and maintained by the HyperAlarm Project (referred to below as "we").
- 1.2. The module requires a rooted device with the Xposed / LSPosed framework installed. Without both, it does not work.
- 1.3. The only scope of this module is Mi Clock (com.android.deskclock). We do not hook any other app.
- 1.4. By using the module you acknowledge that it makes alarms run a script you supplied, rather than only ringing.
- 1.5. You may stop using it at any time by uninstalling the module or deactivating it in the Xposed manager.

## 2. Licence

- 2.1. We grant you a personal, non-transferable, non-sublicensable and non-exclusive licence to install and use the module.
- 2.2. The source code is released under the GNU Affero General Public License v3.0 (AGPL-3.0). You may copy, modify and redistribute it within the terms of that licence.
- 2.3. Commercial preinstallation, bundling or distribution with a device requires our prior written permission.
- 2.4. Parts inherited from HyperCeiler remain under their original terms; see THIRD_PARTY_NOTICES.md in the repository.
- 2.5. The HyperAlarm name and icon are not covered by the source code licence.

## 3. Capabilities and your responsibility

- 3.1. Scripts run as root in the system's main mount namespace, so they can see other apps' data directories.
- 3.2. You supply, install and maintain the scripts. We are not responsible for what a script does or for any consequence of running it.
- 3.3. A silent task suppresses the alarm screen; if the script is missing, rejected or cannot be rescheduled, the module keeps the alarm audible instead.
- 3.4. This module is not a dependable reminder. Do not use it for anything where a missed alarm could cause injury or significant loss, including medical, fire-safety or on-call duty.
- 3.5. Modifying system behaviour carries risk, including but not limited to a device that will not boot, data loss, or a voided warranty.

## 4. Information handling

- 4.1. Script logs, app logs, crash records, settings and the lock screen password stay on your device. We do not upload, collect or sell any of it.
- 4.2. The module makes network requests in exactly two places: the About page reads the author avatar and contributor list from public GitHub endpoints, and the agreement text is fetched from GitHub. Both only expose your IP address to GitHub and carry no device identifier.
- 4.3. The details are in the HyperAlarm Privacy Policy published alongside this agreement.

## 5. Permissions we ask for

- 5.1. Root: to run scripts, read and write the script directory, mirror logs and change global settings. This is the core of the module.
- 5.2. Installed app list: to show apps in the scope picker.
- 5.3. Notifications: to report running and finished scripts, and why a run failed.
- 5.4. Network: only for the two cases described in 4.2.
- 5.5. You may refuse any of them; the matching feature stops working and nothing else is affected.

## 6. Acceptable use

- 6.1. You must not remove copyright or licence notices from the module or its copies.
- 6.2. You must not use the module to harm network security, violate anyone's privacy, or break the law.
- 6.3. You must not install or use the module on someone else's device without their consent.
- 6.4. You must not use the module to bypass payment, crack software or evade access controls.

## 7. Disclaimer and limitation of liability

- 7.1. The module is provided "as is", without warranty of any kind, express or implied, including merchantability, fitness for a particular purpose and non-infringement.
- 7.2. We have verified it only against Mi Clock 18.28.0. A clock or system update may break or change parts of it.
- 7.3. Problems caused by your actions, your scripts, your root environment, the Xposed framework or a system update are yours to bear.
- 7.4. To the maximum extent permitted by law, we are not liable for indirect, incidental, special or punitive damages arising from the use of, or inability to use, this module.
- 7.5. If any clause is held invalid, the remaining clauses stay in force.

## 8. Changes and termination

- 8.1. We may revise this agreement with a release; the revised text ships with the new build and is published in the repository.
- 8.2. Continuing to use the module after a revision takes effect means you accept it. If you do not, please uninstall.
- 8.3. You may end this agreement at any time by uninstalling the module.

## 9. Governing law and disputes

- 9.1. This agreement is governed by the laws of the People's Republic of China.
- 9.2. Disputes are to be settled through good-faith negotiation first; failing that, by the competent court at our principal place of business.

## 10. Contact

- 10.1. For anything about this agreement, open an issue in the project repository.
- 10.2. If you believe a clause does not match what the module actually does, please tell us so we can correct it rather than treating it as binding.
