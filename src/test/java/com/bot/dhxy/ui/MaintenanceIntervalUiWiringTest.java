package com.bot.dhxy.ui;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for the global heal-pet / repair-equipment maintenance UI.
 */
public class MaintenanceIntervalUiWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String source = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/ui/MainWindowController.java"), StandardCharsets.UTF_8);

        require(source.contains("xiuluoHealPetMaintenanceEnabledCheckBox"),
                "heal-pet maintenance must have an explicit enable checkbox");
        require(source.contains("xiuluoRepairEquipmentMaintenanceEnabledCheckBox"),
                "repair-equipment maintenance must have an explicit enable checkbox");
        require(source.contains("setXiuluoHealPetMaintenanceIntervalMs(xiuluoHealPetMaintenanceEnabledCheckBox.isSelected()"),
                "unchecked heal-pet maintenance must persist interval 0");
        require(source.contains("setXiuluoRepairEquipmentMaintenanceIntervalMs(xiuluoRepairEquipmentMaintenanceEnabledCheckBox.isSelected()"),
                "unchecked repair-equipment maintenance must persist interval 0");
        require(source.contains("buildMaintenanceIntervalComboBox"),
                "heal-pet and repair-equipment must not reuse the summon-skill interval choices");

        String maintenanceCombo = between(source,
                "private ComboBox<Integer> buildMaintenanceIntervalComboBox(",
                "private void bindMaintenanceIntervalToggle(");
        require(maintenanceCombo.contains("30, 60, 120, 240"),
                "maintenance interval choices must be 30/60/120/240 minutes");
        require(!maintenanceCombo.contains("3, 5, 10, 15, 20"),
                "maintenance interval choices must not include summon-skill short intervals");

        String normalizer = between(source,
                "private int normalizeMaintenanceIntervalMinutes(",
                "private String formatMaintenanceIntervalText(");
        require(normalizer.contains("return 30;")
                        && normalizer.contains("return 60;")
                        && normalizer.contains("return 120;")
                        && normalizer.contains("return 240;"),
                "maintenance interval normalization must support 30/60/120/240 minutes");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
