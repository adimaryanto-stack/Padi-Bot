import React from 'react';
import { Stack } from 'expo-router';
import { Colors } from '@/constants/theme';

export default function HistoryLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: Colors.surface },
        headerTintColor: Colors.primary,
        headerTitleStyle: { fontWeight: '700', color: Colors.textPrimary },
      }}
    >
      <Stack.Screen name="index" options={{ title: 'Riwayat Misi' }} />
      <Stack.Screen name="[id]" options={{ title: 'Detail Misi' }} />
    </Stack>
  );
}
