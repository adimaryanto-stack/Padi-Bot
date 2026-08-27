import React, { useEffect, useState } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { initDatabase } from '@/db';
import { useFieldStore } from '@/stores/fieldStore';
import { useMissionStore } from '@/stores/missionStore';
import { Colors } from '@/constants/theme';
import { View, ActivityIndicator, StyleSheet } from 'react-native';

export default function RootLayout() {
  const [isReady, setIsReady] = useState(false);
  const loadFields = useFieldStore((state) => state.loadFromDB);
  const loadMissions = useMissionStore((state) => state.loadFromDB);

  useEffect(() => {
    async function prepare() {
      try {
        await initDatabase();
        await Promise.all([loadFields(), loadMissions()]);
      } catch (e) {
        console.warn('Initialization error:', e);
      } finally {
        setIsReady(true);
      }
    }
    prepare();
  }, []);

  if (!isReady) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  return (
    <>
      <StatusBar style="dark" />
      <Stack
        screenOptions={{
          headerStyle: {
            backgroundColor: Colors.surface,
          },
          headerTintColor: Colors.primary,
          headerTitleStyle: {
            fontWeight: '700',
            color: Colors.textPrimary,
          },
          contentStyle: {
            backgroundColor: Colors.background,
          },
        }}
      >
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen
          name="mission/planting-settings"
          options={{ title: 'Pengaturan Tanam', headerBackTitle: 'Kembali' }}
        />
        <Stack.Screen
          name="mission/route-preview"
          options={{ title: 'Preview Jalur', headerBackTitle: 'Pengaturan' }}
        />
        <Stack.Screen
          name="mission/execution"
          options={{ title: 'Eksekusi Misi', headerShown: false }}
        />
        <Stack.Screen
          name="manual-control"
          options={{ title: 'Kontrol Manual', presentation: 'modal' }}
        />
      </Stack>
    </>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
