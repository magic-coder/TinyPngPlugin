package cn.deemons.tinypng;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.tinify.Result;
import com.tinify.Source;
import com.tinify.Tinify;
import org.apache.http.util.TextUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class TingPngAction extends AnAction {
    static Logger logger = Logger.getLogger("UploadFileAction");
    private static int currentIndex = 0;
    private ArrayList<VirtualFile> pictureFiles = new ArrayList<>();
    private Project project;
    private static boolean cancelTiny = false;

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();


        ChooseKeyDialog dialog = new ChooseKeyDialog(e.getProject());
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        dialog.setEnterButtonListener(new ChooseKeyDialog.ButtonListener() {
            @Override
            public void onClick(String api) {

                if (TextUtils.isEmpty(api)) {
                    return;
                }
                Tinify.setKey(api);

                chooseFile();


            }
        });


    }

    private void chooseFile() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true);
        VirtualFile[] selectedFiles = FileChooser.chooseFiles(descriptor, project, null);
        if (selectedFiles.length == 0) {
            return;
        }


        filterAllPictures(selectedFiles);

        tinyFiles();


    }


    private void filterAllPictures(VirtualFile[] selectedFiles) {
        pictureFiles.clear();
        for (int i = 0; i < selectedFiles.length; i++) {
            VirtualFile selectedFile = selectedFiles[i];
            if (selectedFile.isDirectory()) {
                VirtualFile[] directoryChildren = selectedFile.getChildren();
                filterAllPictures(directoryChildren);
            } else if (selectedFile.getName().endsWith(".jpg") || selectedFile.getName().endsWith(".png")) {
                logger.info("path=" + selectedFile.getPath());
                pictureFiles.add(selectedFile);
                if (i >= selectedFiles.length - 1) {
                    return;
                }
            }
        }
    }

    private void tinyFiles() {
        Progress dialog = new Progress();
        dialog.setTitle("上传进度");
        dialog.setMax(pictureFiles.size());
        dialog.setValue(0);
        dialog.pack();

        dialog.setCancelListener(() -> cancelTiny = true);

        cancelTiny = false;
        currentIndex = 0;
        for (int i = 0; i < pictureFiles.size(); i++) {
            VirtualFile virtualFile = pictureFiles.get(i);
            new Thread(() -> {
                Source source = null;
//                    logger.info("path=" + virtualFile.getPath());
                try {
                    if (!cancelTiny) {
                        source = Tinify.fromFile(virtualFile.getPath());
                        Result result = source.result();
                        logger.info("result size=" + result.size() + " mediaType=" + result.mediaType());
                        source.toFile(virtualFile.getPath());

                        currentIndex++;
                        SwingUtilities.invokeLater(() -> {
                            dialog.setValue(currentIndex);
                            dialog.addString(virtualFile.getPath(), virtualFile.getLength(), result.size());
                        });

                    }
                } catch (Exception e1) {
//                        logger.warning(e1.toString());
                    e1.printStackTrace();
                    if (e1.toString().contains("AccountException")) {
                        cancelTiny = true;
                        dialog.showError();
                    }
                }
            }).start();
        }

        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

}