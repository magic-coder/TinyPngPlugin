package cn.deemons.tinypng;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.squareup.okhttp.*;
import gherkin.deps.com.google.gson.Gson;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class TingPngAction extends AnAction {
    static Logger logger = Logger.getLogger("UploadFileAction");

    static String url = "https://tinypng.com/web/shrink";

    private static int currentIndex = 0;
    private ArrayList<File> pictureFiles = new ArrayList<>();
    private Project project;
    private static boolean cancelTiny = false;

    private String parantPath = "";

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();


//        ChooseKeyDialog dialog = new ChooseKeyDialog(e.getProject());
//        dialog.setSize(600, 400);
//        dialog.setLocationRelativeTo(null);
//        dialog.setVisible(true);

//        dialog.setEnterButtonListener(new ChooseKeyDialog.ButtonListener() {
//            @Override
//            public void onClick(String api) {
//
//                if (TextUtils.isEmpty(api)) {
//                    return;
//                }
//                Tinify.setKey(api);
//
//                chooseFile();
//            }
//        });

        chooseFile();

    }

    private void chooseFile() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true);
        VirtualFile[] selectedFiles = FileChooser.chooseFiles(descriptor, project, project.getBaseDir());
        if (selectedFiles.length == 0) {
            return;
        }

        try {
            if (selectedFiles.length == 1) {
                VirtualFile file = selectedFiles[0];
                if (file.isDirectory()) {
                    parantPath = file.getPath() + "/";
                } else {
                    parantPath = file.getParent().getPath() + "/";
                }
            } else {
                parantPath = selectedFiles[0].getParent().getPath() + "/";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        pictureFiles.clear();
        filterAllPictures(selectedFiles);


//        tinyFiles();

        tinyPng();
    }


    private void filterAllPictures(VirtualFile[] selectedFiles) {
        for (int i = 0; i < selectedFiles.length; i++) {
            VirtualFile selectedFile = selectedFiles[i];
            if (selectedFile.isDirectory()) {
                if (!selectedFile.getName().equals("build")) {
                    VirtualFile[] directoryChildren = selectedFile.getChildren();
                    filterAllPictures(directoryChildren);
                }
            } else if (selectedFile.getName().endsWith(".jpg") || selectedFile.getName().endsWith(".png")) {
                logger.info("path=" + selectedFile.getPath());
                pictureFiles.add(new File(selectedFile.getPath()));
                if (i >= selectedFiles.length - 1) {
                    return;
                }
            }
        }
    }

//    private void tinyFiles() {
//        Progress dialog = new Progress();
//        dialog.setTitle("上传进度");
//        dialog.setMax(pictureFiles.size());
//        dialog.setValue(0);
//        dialog.pack();
//
//        dialog.setCancelListener(() -> cancelTiny = true);
//
//        cancelTiny = false;
//        currentIndex = 0;
//        for (int i = 0; i < pictureFiles.size(); i++) {
//            File virtualFile = pictureFiles.get(i);
//            new Thread(() -> {
//                Source source = null;
////                    logger.info("path=" + virtualFile.getPath());
//                try {
//                    if (!cancelTiny) {
//                        source = Tinify.fromFile(virtualFile.getPath());
//                        Result result = source.result();
//                        logger.info("result size=" + result.size() + " mediaType=" + result.mediaType());
//                        source.toFile(virtualFile.getPath());
//
//                        currentIndex++;
//                        SwingUtilities.invokeLater(() -> {
//                            dialog.setValue(currentIndex);
//                            dialog.addString(virtualFile.getPath(), virtualFile.length(), result.size(), 0);
//                        });
//
//                    }
//                } catch (Exception e1) {
////                        logger.warning(e1.toString());
//                    e1.printStackTrace();
//                    if (e1.toString().contains("AccountException")) {
//                        cancelTiny = true;
//                        dialog.showError("");
//                    }
//                }
//            }).start();
//        }
//
//        dialog.setSize(600, 400);
//        dialog.setLocationRelativeTo(null);
//        dialog.setVisible(true);
//    }


    private void tinyPng() {
        if (pictureFiles == null || pictureFiles.size() == 0) return;

        Progress dialog = new Progress();
        dialog.setTitle("上传进度");
        dialog.setMax(pictureFiles.size());
        dialog.setValue(0);
        dialog.pack();

        dialog.setCancelListener(() -> cancelTiny = true);
        dialog.addString("ParentFile Path:  " + parantPath);
        dialog.addString("");

        cancelTiny = false;
        currentIndex = 0;

        //获取可用线程数量
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(availableProcessors * 4);

        for (int i = 0; i < pictureFiles.size(); i++) {
            File file = pictureFiles.get(i);

            fixedThreadPool.execute(() -> {


                try {
                    if (!cancelTiny) {
                        UploadBean uploadBean = uploadImage(file);


                        SwingUtilities.invokeLater(() -> {

                            currentIndex++;
                            dialog.setValue(currentIndex);

                            dialog.addString(file.getPath().replace(parantPath, ""),
                                    uploadBean == null ? file.length() : uploadBean.getInput().getSize(),
                                    uploadBean == null ? file.length() : uploadBean.getOutput().getSize(),
                                    uploadBean == null ? 0 : 1 - uploadBean.getOutput().getRatio()
                            );
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        currentIndex++;
                        dialog.showError(file.getAbsolutePath() + "  " + e.getMessage());
                    });
                }

            });
        }

        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * 上传
     *
     * @param sourceFile
     * @return
     * @throws IOException
     */
    public UploadBean uploadImage(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) return null;


        Request request = new Request.Builder()
                .addHeader("Content-Type", "image/png")
                .url(url)
                .post(RequestBody.create(MediaType.parse("image/png"), sourceFile))
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();
        Response response = okHttpClient.newCall(request).execute();

        if (!response.isSuccessful()) return null;

        UploadBean uploadBean = new Gson().fromJson(response.body().string(), UploadBean.class);

        if (uploadBean == null || uploadBean.getOutput() == null || uploadBean.getOutput().getUrl().isEmpty())
            return null;

        InputStream inputStream = okHttpClient.newCall(new Request.Builder()
                .get()
                .url(uploadBean.getOutput().getUrl())
                .build()
        ).execute().body().byteStream();


        saveToFile(sourceFile.getAbsolutePath(), inputStream);

        return uploadBean;
    }


    /**
     * 保存图片
     *
     * @param fileName
     * @param in
     * @throws IOException
     */
    private void saveToFile(String fileName, InputStream in) throws IOException {

        int BUFFER_SIZE = 1024;
        byte[] buf = new byte[BUFFER_SIZE];
        int size = 0;

        BufferedInputStream bis = new BufferedInputStream(in);
        FileOutputStream fos = new FileOutputStream(fileName);

        //保存文件
        while ((size = bis.read(buf)) != -1) {
            fos.write(buf, 0, size);
        }

        fos.close();
        bis.close();
    }


    public static void main(String[] args) {


        try {
            new TingPngAction().uploadImage(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}