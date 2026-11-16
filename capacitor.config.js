// Config de Capacitor. Al cargarse, si ya existe la carpeta android/, aplica el parche nativo (ecualizador del sistema).
const fs = require('fs'), path = require('path');
try {
  if (fs.existsSync(path.join(__dirname, 'android/app/src/main')) && fs.existsSync(path.join(__dirname, 'native/patch.js'))) {
    require('./native/patch.js')(__dirname);
  }
} catch (e) { console.warn('EQ Dime patch error:', e); }
module.exports = { appId: 'com.marshall.eqdime', appName: 'EQ Dime', webDir: 'www', backgroundColor: '#00000000' };
