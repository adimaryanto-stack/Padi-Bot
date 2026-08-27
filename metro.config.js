// Learn more https://docs.expo.io/guides/customizing-metro
const { getDefaultConfig } = require('expo/metro-config');

/** @type {import('expo/metro-config').MetroConfig} */
const config = getDefaultConfig(__dirname);

// Enable web support
config.resolver.sourceExts = [...config.resolver.sourceExts, 'mjs', 'cjs'];

// Support platform-specific extensions: .native.ts / .web.ts
config.resolver.platforms = ['native', 'web', 'android', 'ios'];

module.exports = config;
