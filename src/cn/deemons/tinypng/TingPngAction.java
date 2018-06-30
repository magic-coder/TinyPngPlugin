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

import java.util.ArrayList;
import java.util.logging.Logger;

public class TingPngAction extends AnAction {
    static Logger logger = Logger.getLogger("UploadFileAction");
    private ProgressDialog progressDialog;
    private static int currentIndex = 0;
    private ArrayList<VirtualFile> pictureFiles = new ArrayList<>();
    private Project project;
    private static boolean cancelTiny = false;
    private String api;

    @Override
    public void actionPerformed(AnActionEvent e) {

        ChooseKeyDialog dialog = new ChooseKeyDialog(e.getProject());
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);




        if (TextUtils.isEmpty(api)) {
            return;
        }
        Tinify.setKey(api);



        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true);
        VirtualFile[] selectedFiles = FileChooser.chooseFiles(descriptor, project, null);
        if (selectedFiles.length == 0) {
            return;
        }

        pictureFiles.clear();
        filterAllPictures(selectedFiles);
        tinyFiles();

        progressDialog = new ProgressDialog(project);
        progressDialog.setTitle("上传进度");
        progressDialog.setMaxImages(pictureFiles.size());
        progressDialog.revalidate();

        progressDialog.show();
    }

    private void filterAllPictures(VirtualFile[] selectedFiles) {
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
        cancelTiny = false;
        for (int i = 0; i < pictureFiles.size(); i++) {
            currentIndex = 0;
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
                        progressDialog.setCurrentIndex(currentIndex);
                        progressDialog.revalidate();

                    }
                } catch (Exception e1) {
//                        logger.warning(e1.toString());
                    e1.printStackTrace();
                    if (e1.toString().contains("AccountException")) {
                        cancelTiny = true;
                        progressDialog.showError();
                    }
                }
            }).start();
        }
    }
}