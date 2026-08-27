import React from 'react';
import { Stack } from 'expo-router';
import { Colors } from '@/constants/theme';

export default function FieldsLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: Colors.surface },
        headerTintColor: Colors.primary,
        headerTitleStyle: { fontWeight: '700', color: Colors.textPrimary },
      }}
    >
      <Stack.Screen name="index" options={{ title: 'Daftar Sawah' }} />
      <Stack.Screen name="create" options={{ title: 'Tambah Sawah Baru' }} />
      <Stack.Screen name="[id]" options={{ title: 'Detail Sawah' }} />
    </Stack>
  );
}
