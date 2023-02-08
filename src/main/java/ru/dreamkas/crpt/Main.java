package ru.dreamkas.crpt;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;

public class Main {

    public static void main(String[] args) {
        Map<String, String> properties = Arrays.stream(args).collect(Collectors.toMap(a -> StringUtils.substringBefore(a, "="), a -> StringUtils.substringAfter(a, "=")));
        File file = null;
        if (properties.containsKey("-file")) {
            file = Paths.get(properties.get("-file")).toFile();
        }
        if (file == null || !file.exists()) {
            UIManager.put("FileChooser.readOnly", true);
            JFileChooser fc = new JFileChooser(Paths.get(".").normalize().toAbsolutePath().toFile());
            fc.addChoosableFileFilter(new FileNameExtensionFilter("Уведомления ФФД 1.2", "crpt2"));
            //fc.addChoosableFileFilter(new FileNameExtensionFilter("Уведомления ФФД 1.05", "crpt"));
            fc.setFileFilter(fc.getChoosableFileFilters()[1]);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setDialogType(JFileChooser.OPEN_DIALOG);
            if (fc.showDialog(null, null) == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
            } else {
                return;
            }
        }
        if (file == null || !file.exists()) {
            JOptionPane.showMessageDialog(null, "Файл не найден", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            File resultFile = Parser.INSTANCE.parse(file);
            JOptionPane.showMessageDialog(null, "Сформирован файл " + resultFile.getAbsolutePath(), "Информация", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}