import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'io.ionic.starter',
  appName: 'My Steamy App',
  webDir: 'www',
  server: {
    androidScheme: 'https'
  },
  plugins: {
    Preferences: {
      group: 'CapacitorStorage'
    },
    CapacitorHttp: {
      enabled: true
    }
  }
};

export default config;