/*
 * Copyright 2013 Volker Oth (0xdeadbeef) / Miklos Juhasz (mjuhasz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bdsup2sub.gui.support;

import bdsup2sub.core.Core;
import bdsup2sub.core.CoreThreadState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

import static bdsup2sub.gui.support.GuiUtils.centerRelativeToOwner;

public class Progress extends JDialog {

    private JPanel jContentPane;
    private JButton jButtonCancel;
    private JProgressBar jProgressBar;
    private JLabel jLabelProgress;

    private Timer timer;


    public Progress(Frame owner) {
        super(owner, true);
        initialize();
        centerRelativeToOwner(this);
    }

    private void initialize() {
        setMinimumSize(new Dimension(224, 139));
        setResizable(false);
        setBounds(new Rectangle(0, 0, 224, 139));
        setMaximumSize(new Dimension(224, 139));
        setContentPane(getJContentPane());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Core.cancel();
            }
        });
    }

    @Override
    public void setVisible(boolean b) {
        timer = new Timer();
        timer.schedule(new ProgressTimer(), 200, 200);
        super.setVisible(b);
    }

    public void setText(String s) {
        jLabelProgress.setText(s);
    }

    private JPanel getJContentPane() {
        if (jContentPane == null) {
            GridBagConstraints gridBagBtnCancel = new GridBagConstraints();
            gridBagBtnCancel.insets = new Insets(8, 8, 8, 8);
            gridBagBtnCancel.gridx = 0;
            gridBagBtnCancel.gridy = 2;
            GridBagConstraints gridBagProgressBar = new GridBagConstraints();
            gridBagProgressBar.gridy = 1;
            gridBagProgressBar.fill = GridBagConstraints.VERTICAL;
            gridBagProgressBar.insets = new Insets(8, 8, 8, 8);
            gridBagProgressBar.gridx = 0;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.fill = GridBagConstraints.VERTICAL;
            gridBagConstraints.insets = new Insets(8, 8, 8, 8);
            gridBagConstraints.gridy = 0;
            jLabelProgress = new JLabel();
            jLabelProgress.setText("Exporting SUB/IDX");
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            jContentPane.add(jLabelProgress, gridBagConstraints);
            jContentPane.add(getJProgressBar(), gridBagProgressBar);
            jContentPane.add(getJButtonCancel(), gridBagBtnCancel);
        }
        return jContentPane;
    }

    private JButton getJButtonCancel() {
        if (jButtonCancel == null) {
            jButtonCancel = new JButton();
            jButtonCancel.setText("Cancel");
            jButtonCancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Core.cancel();
                }
            });
        }
        return jButtonCancel;
    }

    private JProgressBar getJProgressBar() {
        if (jProgressBar == null) {
            jProgressBar = new JProgressBar();
            jProgressBar.setPreferredSize(new Dimension(200, 20));
            jProgressBar.setMinimumSize(new Dimension(200, 20));
            jProgressBar.setStringPainted(true);
        }
        return jProgressBar;
    }

    public void setProgress(final int val) {
        jProgressBar.setValue(val);
        jProgressBar.repaint();
    }

    private class ProgressTimer extends TimerTask {
        @Override
        public void run() {
            if (Core.getStatus() != CoreThreadState.ACTIVE) {
                timer.cancel();
                dispose();
            }
        }
    }
}
