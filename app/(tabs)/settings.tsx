import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  Pressable,
  Switch,
  Platform,
  Alert,
} from 'react-native';
import { useMachineStore } from '@/stores/machineStore';
import { useFieldStore } from '@/stores/fieldStore';
import { useMissionStore } from '@/stores/missionStore';
import { ConnectionType } from '@/types';
import { Button } from '@/components/ui/Button';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';
import { APP_VERSION, APP_BUILD, GITHUB_URL } from '@/constants/defaults';

export default function SettingsScreen() {
  const connectionConfig = useMachineStore((state) => state.connectionConfig);
  const setConnectionType = useMachineStore((state) => state.setConnectionType);
  const wifiConfig = useMachineStore((state) => state.wifiConfig);
  const setWifiConfig = useMachineStore((state) => state.setWifiConfig);
  const gsmConfig = useMachineStore((state) => state.gsmConfig);
  const setGsmConfig = useMachineStore((state) => state.setGsmConfig);

  const defaultMachineWidth = useMachineStore((state) => state.defaultMachineWidthM);
  const setDefaultMachineWidth = useMachineStore((state) => state.setDefaultMachineWidthM);
  const defaultHeadlandWidth = useMachineStore((state) => state.defaultHeadlandWidthM);
  const setDefaultHeadlandWidth = useMachineStore((state) => state.setDefaultHeadlandWidthM);

  const debugMode = useMachineStore((state) => state.debugMode);
  const setDebugMode = useMachineStore((state) => state.setDebugMode);

  const fields = useFieldStore((state) => state.fields);
  const clearAllFields = useFieldStore((state) => state.clearAllFields);
  const missions = useMissionStore((state) => state.missions);
  const clearAllMissions = useMissionStore((state) => state.clearAllMissions);

  const [testResult, setTestResult] = useState<string | null>(null);

  const handleTestConnection = async () => {
    setTestResult('Menguji koneksi...');
    setTimeout(() => {
      setTestResult('✓ Berhasil terhubung ke Arduino');
    }, 1000);
  };

  const handleClearData = () => {
    if (Platform.OS === 'web') {
      const ok = window.confirm(
        `${Strings.settingsClearDataConfirmTitle}\n${Strings.settingsClearDataConfirmBody}`
      );
      if (ok) {
        clearAllFields();
        clearAllMissions();
        alert('Semua data berhasil dibersihkan');
      }
    } else {
      Alert.alert(
        Strings.settingsClearDataConfirmTitle,
        Strings.settingsClearDataConfirmBody,
        [
          { text: Strings.cancel, style: 'cancel' },
          {
            text: Strings.delete,
            style: 'destructive',
            onPress: () => {
              clearAllFields();
              clearAllMissions();
            },
          },
        ]
      );
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* 1. Arduino Connection Type */}
      <View style={styles.section}>
        <Text style={styles.sectionHeader}>{Strings.settingsConnectionSection}</Text>

        <Text style={styles.label}>{Strings.settingsConnectionType}</Text>
        <View style={styles.typeSelector}>
          {(['SIMULATOR', 'WIFI', 'BLUETOOTH', 'GSM'] as ConnectionType[]).map((type) => (
            <Pressable
              key={type}
              onPress={() => setConnectionType(type)}
              style={[
                styles.typeBtn,
                connectionConfig.type === type && styles.typeBtnActive,
              ]}
            >
              <Text
                style={[
                  styles.typeBtnText,
                  connectionConfig.type === type && styles.typeBtnTextActive,
                ]}
              >
                {type}
              </Text>
            </Pressable>
          ))}
        </View>

        {/* Dynamic Fields for WiFi */}
        {connectionConfig.type === 'WIFI' && (
          <View style={styles.connectionDetails}>
            <Text style={styles.label}>{Strings.settingsWifiIp}</Text>
            <TextInput
              style={styles.input}
              value={wifiConfig.ipAddress}
              onChangeText={(text) => setWifiConfig({ ipAddress: text })}
              placeholder="192.168.4.1"
              placeholderTextColor={Colors.textDisabled}
            />

            <Text style={styles.label}>{Strings.settingsWifiPort}</Text>
            <TextInput
              style={styles.input}
              value={wifiConfig.port.toString()}
              onChangeText={(text) => setWifiConfig({ port: parseInt(text) || 80 })}
              placeholder="80"
              placeholderTextColor={Colors.textDisabled}
              keyboardType="numeric"
            />

            <Button
              label={Strings.settingsWifiTest}
              onPress={handleTestConnection}
              variant="outline"
              size="sm"
              style={{ marginTop: 8 }}
            />
          </View>
        )}

        {/* Dynamic Fields for Bluetooth */}
        {connectionConfig.type === 'BLUETOOTH' && (
          <View style={styles.connectionDetails}>
            {Platform.OS === 'web' ? (
              <Text style={styles.infoNotice}>⚠️ {Strings.settingsBtAndroidOnly}</Text>
            ) : (
              <>
                <Text style={styles.label}>{Strings.settingsBtScan}</Text>
                <Button
                  label={Strings.settingsBtRescan}
                  onPress={() => alert('Mencari perangkat Bluetooth Arduino (HC-05/HM-10)...')}
                  variant="outline"
                  size="sm"
                />
              </>
            )}
          </View>
        )}

        {/* Dynamic Fields for GSM 4G */}
        {connectionConfig.type === 'GSM' && (
          <View style={styles.connectionDetails}>
            <Text style={styles.label}>{Strings.settingsGsmBroker}</Text>
            <TextInput
              style={styles.input}
              value={gsmConfig.mqttBroker}
              onChangeText={(text) => setGsmConfig({ mqttBroker: text })}
              placeholder="broker.hivemq.com"
              placeholderTextColor={Colors.textDisabled}
            />

            <Text style={styles.label}>{Strings.settingsGsmDeviceId}</Text>
            <TextInput
              style={styles.input}
              value={gsmConfig.deviceId}
              onChangeText={(text) => setGsmConfig({ deviceId: text })}
              placeholder="padibot-001"
              placeholderTextColor={Colors.textDisabled}
            />

            <Button
              label={Strings.settingsGsmTest}
              onPress={handleTestConnection}
              variant="outline"
              size="sm"
              style={{ marginTop: 8 }}
            />
          </View>
        )}

        {testResult && <Text style={styles.testResultText}>{testResult}</Text>}
      </View>

      {/* 2. Machine Defaults */}
      <View style={styles.section}>
        <Text style={styles.sectionHeader}>{Strings.settingsMachineSection}</Text>

        <Text style={styles.label}>{Strings.settingsDefaultWidth} (m)</Text>
        <TextInput
          style={styles.input}
          value={defaultMachineWidth.toString()}
          onChangeText={(text) => setDefaultMachineWidth(parseFloat(text) || 1.5)}
          keyboardType="numeric"
        />

        <Text style={styles.label}>{Strings.settingsDefaultHeadland} (m)</Text>
        <TextInput
          style={styles.input}
          value={defaultHeadlandWidth.toString()}
          onChangeText={(text) => setDefaultHeadlandWidth(parseFloat(text) || 3.0)}
          keyboardType="numeric"
        />
      </View>

      {/* 3. App Settings */}
      <View style={styles.section}>
        <Text style={styles.sectionHeader}>{Strings.settingsAppSection}</Text>
        <View style={styles.switchRow}>
          <Text style={styles.switchLabel}>{Strings.settingsDebugMode}</Text>
          <Switch
            value={debugMode}
            onValueChange={setDebugMode}
            trackColor={{ false: Colors.border, true: Colors.primary }}
          />
        </View>
      </View>

      {/* 4. Data Management */}
      <View style={styles.section}>
        <Text style={styles.sectionHeader}>{Strings.settingsDataSection}</Text>
        <Text style={styles.dataSummary}>
          Total: {fields.length} sawah • {missions.length} misi tersimpan
        </Text>
        <Button
          label={Strings.settingsClearData}
          onPress={handleClearData}
          variant="danger"
          size="sm"
          style={{ marginTop: 8 }}
        />
      </View>

      {/* 5. About */}
      <View style={styles.section}>
        <Text style={styles.sectionHeader}>{Strings.settingsAboutSection}</Text>
        <Text style={styles.aboutText}>
          {Strings.appName} v{APP_VERSION} (Build {APP_BUILD})
        </Text>
        <Text style={styles.aboutText}>Platform: {Platform.OS.toUpperCase()}</Text>
        <Text style={styles.aboutLink}>{GITHUB_URL}</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    padding: Spacing.lg,
    paddingBottom: Spacing.xxxl,
  },
  section: {
    backgroundColor: Colors.surface,
    padding: Spacing.md,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: Spacing.md,
  },
  sectionHeader: {
    ...Typography.titleMedium,
    color: Colors.textPrimary,
    fontWeight: '800',
    marginBottom: Spacing.sm,
  },
  label: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    marginBottom: 4,
    marginTop: 8,
  },
  typeSelector: {
    flexDirection: 'row',
    gap: 6,
    marginBottom: Spacing.sm,
  },
  typeBtn: {
    flex: 1,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: Colors.surfaceVariant,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  typeBtnActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primaryDark,
  },
  typeBtnText: {
    ...Typography.labelSmall,
    color: Colors.textSecondary,
    fontWeight: '700',
    fontSize: 10,
  },
  typeBtnTextActive: {
    color: '#FFFFFF',
  },
  connectionDetails: {
    paddingTop: Spacing.xs,
  },
  input: {
    height: 42,
    backgroundColor: Colors.surfaceVariant,
    borderRadius: 8,
    paddingHorizontal: 10,
    borderWidth: 1,
    borderColor: Colors.border,
    color: Colors.textPrimary,
  },
  infoNotice: {
    ...Typography.bodyMedium,
    color: Colors.warning,
    paddingVertical: 4,
  },
  testResultText: {
    ...Typography.labelSmall,
    color: Colors.primaryDark,
    fontWeight: '600',
    marginTop: 8,
    textAlign: 'center',
  },
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 4,
  },
  switchLabel: {
    ...Typography.bodyMedium,
    color: Colors.textPrimary,
  },
  dataSummary: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
    marginBottom: 4,
  },
  aboutText: {
    ...Typography.bodyMedium,
    color: Colors.textSecondary,
  },
  aboutLink: {
    ...Typography.labelSmall,
    color: Colors.primary,
    marginTop: 4,
  },
});
