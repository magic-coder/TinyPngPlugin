package cn.deemons.tinypng;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Progress extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JProgressBar progressBar;
    private JTextArea textArea;

    private long diffTotal = 0;
    private CancelListener listener;
    private int max = 0;
    private boolean isFinish = false;
    private StringBuilder stringBuilder = new StringBuilder();

    public Progress() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        initView();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initView() {
        buttonOK.setEnabled(false);

        progressBar.setOrientation(SwingConstants.HORIZONTAL);
        progressBar.setMinimum(0);

        progressBar.setString("0/0");
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        stringBuilder.append("uploading....\n");
        textArea.setText(stringBuilder.toString());
        progressBar.setVisible(true);
        textArea.setVisible(true);


    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        if (listener != null) {
            listener.onCancel();
        }
        dispose();
    }

    public void setValue(int value) {
        progressBar.setString(value + "/" + max);
        progressBar.setValue(value);

        isFinish = value == max;
    }

    public void addString(String path, long source, long result) {

        stringBuilder.append("\n");
        stringBuilder.append(progressBar.getString());
        stringBuilder.append(" ====>>> ");
        stringBuilder.append("path=").append(path);
        stringBuilder.append("\n \t\tsource = ").append(source);
        stringBuilder.append("\n \t\tresult = ").append(result);
        long diff = source - result;
        stringBuilder.append("\n \t\tdiff   = " + diff);
        if (diff >= 0) {
            diffTotal += diff;
        } else {
            stringBuilder.append("\n \t******* exception *****");
        }

        if (isFinish) {
            stringBuilder.append("\n\n=============================================");
            stringBuilder.append("\n\nFinish");
            stringBuilder.append("\nTotal compressed size：").append(diffTotal);
            buttonOK.setEnabled(true);
        }

        textArea.setText(stringBuilder.toString());
    }


    public void setMax(int value) {
        max = value;
        progressBar.setMaximum(value);
    }

    public void showError() {
        stringBuilder.append("\n \n");
        stringBuilder.append("出错：每月500张图片限制已用完,请输入新KEY");
        textArea.setText(stringBuilder.toString());
    }


    public void setCancelListener(CancelListener listener) {
        this.listener = listener;
    }

    public interface CancelListener {
        void onCancel();
    }

    public static void main(String[] args) {
        Progress dialog = new Progress();
        dialog.setMax(50);
        dialog.setValue(0);
        dialog.pack();
        new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.print("i=" + i);
                int finalI = i;
                SwingUtilities.invokeLater(() -> {
                    dialog.setValue(finalI);

                });
            }
            dialog.showError();
        }).start();

        dialog.setVisible(true);
        System.exit(0);
    }
}
