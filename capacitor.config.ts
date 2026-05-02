import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'io.ionic.mysteamyapp',
  appName: 'My Steamy App',
  webDir: 'www',
  server: {
    androidScheme: 'https'
  },
  plugins: {
    Preferences: {
      group: 'CapacitorStorage'
    }
  }
};

export default config;