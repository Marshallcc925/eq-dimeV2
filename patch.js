const fs = require('fs'), path = require('path');
const PERMS = `    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>
`;
const SVC = `        <service android:name=".EqService" android:exported="false" android:foregroundServiceType="mediaPlayback"/>
`;
module.exports = function (root) {
  const main = path.join(root, 'android/app/src/main');
  const jdir = path.join(main, 'java/com/marshall/eqdime');
  fs.mkdirSync(jdir, { recursive: true });
  for (const f of fs.readdirSync(path.join(root, 'native'))) {
    if (f.endsWith('.java')) fs.copyFileSync(path.join(root, 'native', f), path.join(jdir, f));
  }
  const mp = path.join(main, 'AndroidManifest.xml');
  let m = fs.readFileSync(mp, 'utf8');
  if (!m.includes('EqService')) {
    m = m.replace('<application', PERMS + '    <application').replace('</application>', SVC + '    </application>');
    fs.writeFileSync(mp, m);
  }
  const sp = path.join(main, 'res/values/styles.xml');
  if (fs.existsSync(sp)) {
    let s = fs.readFileSync(sp, 'utf8');
    if (!s.includes('windowIsTranslucent')) {
      s = s.replace(/(<style name="AppTheme\.NoActionBar"[^>]*>)/, '$1\n        <item name="android:windowIsTranslucent">true</item>\n        <item name="android:windowBackground">@android:color/transparent</item>');
      fs.writeFileSync(sp, s);
    }
  }
};
