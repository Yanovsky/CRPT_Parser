package ru.dreamkas.crpt;

import java.io.IOException;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {

    public static void main(String[] args) throws IOException {
        UIManager.put("FileChooser.readOnly", true);
        JFileChooser fc = new JFileChooser(Paths.get("D:", "SVNAll", "exchange").toFile());
        FileNameExtensionFilter crpt2Filter = new FileNameExtensionFilter("Уведомления ФФД 1.2", "crpt2");
        FileNameExtensionFilter crptFilter = new FileNameExtensionFilter("Уведомления ФФД 1.05", "crpt");
        fc.addChoosableFileFilter(crpt2Filter);
        fc.addChoosableFileFilter(crptFilter);
        fc.setFileFilter(crpt2Filter);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        UIManager.put("FileChooser.readOnly", UIManager.getBoolean("FileChooser.readOnly"));
        if (fc.showDialog(null, null) == JFileChooser.APPROVE_OPTION) {
            new Parser().parse(fc.getSelectedFile());
        }
    }
}
