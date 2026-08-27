import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  FlatList,
  Alert,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useFieldStore } from '@/stores/fieldStore';
import { FieldListItem } from '@/components/field/FieldListItem';
import { EmptyState } from '@/components/ui/EmptyState';
import { Button } from '@/components/ui/Button';
import { Colors, Typography, Spacing } from '@/constants/theme';
import { Strings } from '@/constants/strings';

export default function FieldListScreen() {
  const router = useRouter();
  const fields = useFieldStore((state) => state.fields);
  const activeFieldId = useFieldStore((state) => state.activeFieldId);
  const removeField = useFieldStore((state) => state.removeField);
  const [searchQuery, setSearchQuery] = useState('');

  const filteredFields = fields.filter((f) =>
    f.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleDeleteField = (id: string, name: string) => {
    if (Platform.OS === 'web') {
      const ok = window.confirm(Strings.fieldDeleteConfirmBody(name));
      if (ok) removeField(id);
    } else {
      Alert.alert(
        Strings.fieldDeleteConfirmTitle,
        Strings.fieldDeleteConfirmBody(name),
        [
          { text: Strings.cancel, style: 'cancel' },
          {
            text: Strings.delete,
            style: 'destructive',
            onPress: () => removeField(id),
          },
        ]
      );
    }
  };

  return (
    <View style={styles.container}>
      {/* Search and Add Header */}
      <View style={styles.header}>
        <TextInput
          style={styles.searchInput}
          placeholder={Strings.fieldSearch}
          placeholderTextColor={Colors.textDisabled}
          value={searchQuery}
          onChangeText={setSearchQuery}
        />
        <Button
          label="+ Tambah"
          onPress={() => router.push('/(tabs)/fields/create')}
          size="sm"
        />
      </View>

      {/* Field List or Empty State */}
      <FlatList
        data={filteredFields}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        renderItem={({ item }) => (
          <FieldListItem
            field={item}
            isActive={item.id === activeFieldId}
            onPress={() => router.push(`/(tabs)/fields/${item.id}`)}
            onDelete={() => handleDeleteField(item.id, item.name)}
          />
        )}
        ListEmptyComponent={
          <EmptyState
            icon="🌾"
            title={Strings.fieldListEmpty}
            description={Strings.fieldListEmptyDesc}
            actionLabel={Strings.fieldAddButton}
            onAction={() => router.push('/(tabs)/fields/create')}
          />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.md,
    gap: Spacing.sm,
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  searchInput: {
    flex: 1,
    height: 42,
    backgroundColor: Colors.surfaceVariant,
    borderRadius: 8,
    paddingHorizontal: 12,
    ...Typography.bodyMedium,
    color: Colors.textPrimary,
  },
  listContent: {
    padding: Spacing.md,
    flexGrow: 1,
  },
});
