/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel
 * Apr 15, 2015
 */
package pt.lsts.neptus.mra.exporters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.io.FileUtils;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
/**
 * @author Manuel R.
 *
 */
@PluginDescription

public class MRAExporterFilter implements MRAExporter {

    private IMraLogGroup source;
    private ProgressMonitor pmonitor;
    private ArrayList<String> defaultLogs = new ArrayList<String>();
    private int progress;
    private Task processTask;
    private File outputFile;
    /**
     * @wbp.parser.entryPoint
     */
    public MRAExporterFilter(IMraLogGroup source) {
        super();
        this.source = source;

        defaultLogs.add("EstimatedState");
        defaultLogs.add("Temperature");
        defaultLogs.add("Salinity");
        defaultLogs.add("Conductivity");
        defaultLogs.add("Pressure");

    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    private File chooseSaveFile(String path) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(path.concat("/Data_filtered.lsf")));
        fileChooser.setFileView(new NeptusFileView());
        fileChooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("Log files"),
                new String[] { FileUtil.FILE_TYPE_LSF, FileUtil.FILE_TYPE_LSF_COMPRESSED }));

        fileChooser.setAcceptAllFileFilterUsed(false);

        int status = fileChooser.showSaveDialog(null);
        String fileName = null;

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                fileName = selectedFile.getCanonicalPath();
                if (fileName.endsWith(".gz")) {
                    return selectedFile = new File(fileName);
                }
                if (!fileName.endsWith(".lsf")) {
                    return selectedFile = new File(fileName + ".lsf");
                }
                if (fileName.endsWith(".lsf")) {
                    return selectedFile = new File(fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pPmonitor) {
        progress = 0;
        pmonitor = pPmonitor;
        pmonitor.setMinimum(0);
        pmonitor.setMaximum(100);

        //list of messages in this log source
        String[] logs = source.listLogs();

        //create JFrame with default logs selected and the rest of available logs
        FilterList window = new FilterList(defaultLogs, logs);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {

            }
        } );

        while ((window.isShowing() || progress < 100 ) && !pmonitor.isCanceled()) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (pmonitor.isCanceled()) {
            outputFile.delete();
            return "Cancelled by the user";
        }
        return ((progress == 100 ) && (!pmonitor.isCanceled()) ? I18n.text("Exported filtered log successfully.") : I18n.text("Filtered log not exported successfully."));

    }

    @Override
    public String getName() {
        return I18n.text("Export filtered log");
    }


    private void applyFilter(FilterList filter) {
        LsfIndex index = source.getLsfIndex();

        String path = source.getFile("Data.lsf").getParent();

        File outputFile = chooseSaveFile(path);
        FileOutputStream fos = null;
        GZIPOutputStream gzipOS = null;
        if(outputFile == null)
            return;

        try {
            //create file
            outputFile.createNewFile();
            fos = new FileOutputStream(outputFile, true);

            //GZIP if user selected .gz
            if (outputFile.getName().toLowerCase().endsWith(FileUtil.FILE_TYPE_LSF_COMPRESSED))  {
                gzipOS = new GZIPOutputStream(fos);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.outputFile = outputFile;
        processTask = new Task(index, fos, gzipOS, path, outputFile);
        processTask.execute();

        pmonitor.close();
        filter.setVisible(false);
        filter.dispose();
    }

    class Task extends SwingWorker<Void, Void> {
        private LsfIndex index;
        private FileOutputStream fos;
        private GZIPOutputStream gzipOS;
        private String path;
        private File outputFile;
        /**
         * @param index2
         * @param fos2
         * @param gzipOS2
         */
        public Task(LsfIndex index, FileOutputStream fos, GZIPOutputStream gzipOS, String path, File outputFile) {
            this.index = index;
            this.fos = fos;
            this.gzipOS = gzipOS;
            this.path = path;
            this.outputFile = outputFile;
        }

        @Override
        public Void doInBackground() {

            writeToStream(index, fos, gzipOS);
            if (pmonitor.isCanceled())
                return null;
            copyCheck(path.toString(), outputFile.getParentFile().getAbsolutePath());

            return null;
        }

        @Override
        public void done() {

        }
    }


    private void writeToStream(LsfIndex index, FileOutputStream fos, GZIPOutputStream gzipOutputStream) {
        pmonitor.setNote(I18n.text("Filtering"));
        int count = 0;
        for (String logName : source.listLogs()) {
            if (defaultLogs.contains(logName)) {
                count++;
            }
        }
        int x = 100 / count;
        for (String logName : source.listLogs()) {
            if (defaultLogs.contains(logName)) {
                if (pmonitor.isCanceled()){
                    break;
                }
                pmonitor.setNote(I18n.textf("Filtering %logname...", logName));
                pmonitor.setProgress(progress);
                progress = progress + x;
                int mgid = index.getDefinitions().getMessageId(logName);
                int firstPos = index.getFirstMessageOfType(mgid);
                int lastPos = index.getLastMessageOfType(mgid);
                int j = firstPos;
                try {
                    while (j < lastPos) {
                        //  IMCMessage entry = index.getMessage(j);
                        //  System.out.println(entry.toString());
                        //  System.out.println("pos "+ j);

                        //write msg bytes
                        byte[] by = index.getMessageBytes(j);
                        if (gzipOutputStream == null) {
                            fos.write(by);
                        }
                        else 
                            gzipOutputStream.write(by);

                        j = index.getNextMessageOfType(mgid, j);
                    }
                    //append last message
                    byte[] lastMsg = index.getMessageBytes(lastPos);
                    if (gzipOutputStream == null)
                        fos.write(lastMsg);
                    else 
                        gzipOutputStream.write(lastMsg);


                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        pmonitor.setProgress(progress);

        //close resources
        try {
            if (gzipOutputStream != null)
                gzipOutputStream.close();
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** Copies IMC.xml.gz and Output.txt to location2, if location1 != location2.
     * @param location1 
     * @param location2 
     * 
     */
    private void copyCheck(String location1, String location2) {
        progress = 99;
        pmonitor.setProgress(progress);
        if (!location1.equals(location2)){
            File sourceIMCXML = new File(location1+ "/IMC.xml.gz");
            File sourceOutputTxt = new File(location1+ "/Output.txt");
            File destIMC = new File(location2+"/IMC.xml.gz");
            File destOutputTxt = new File(location2+"/Output.txt");
            pmonitor.setNote(I18n.text("Copying additional files..."));
            try {
                FileUtils.copyFile(sourceIMCXML, destIMC);
                FileUtils.copyFile(sourceOutputTxt, destOutputTxt);
            } catch (IOException e) {
                e.printStackTrace();
            } 
        }
        progress = 100;
        pmonitor.setProgress(progress);

    }

    @SuppressWarnings("rawtypes")
    private class FilterList extends JFrame {
        private static final long serialVersionUID = 1L;
        protected JList m_list;
        private JTextField textField = null;

        @SuppressWarnings({ "unchecked", "serial" })
        public FilterList(ArrayList<String> defaultLogs, String[] logs) {

            super(I18n.text("MRA Exporter"));
            setType(Type.NORMAL);
            setSize(230, 300);
            getContentPane().setLayout(new MigLayout("", "[240px]", "[300px]"));

            ArrayList<LogItem> options = new ArrayList<>();

            for (String log : logs ) {
                if (defaultLogs.contains(log)) 
                    options.add(new LogItem(log, true));
                else
                    options.add(new LogItem(log, false));
            }
            Arrays.sort(options.toArray());

            m_list = new JList(options.toArray());
            CheckListCellRenderer renderer = new CheckListCellRenderer();
            m_list.setCellRenderer(renderer);
            m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            CheckListener lst = new CheckListener(this);
            m_list.addMouseListener(lst);
            m_list.addKeyListener(lst);

            JScrollPane ps = new JScrollPane();
            ps.setViewportView(m_list);
            ps.setMaximumSize(new Dimension(200, 200));
            ps.setMinimumSize (new Dimension (200,200));

            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(ps, BorderLayout.CENTER);
            p.setBorder(new TitledBorder(new EtchedBorder(), I18n.text("Filter messages")+":") );

            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

            final ActionListener entAct = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = m_list.getNextMatch(textField.getText(), 0, javax.swing.text.Position.Bias.Forward);
                    m_list.setSelectedIndex(index);
                    m_list.ensureIndexIsVisible(index);

                }
            };

            AbstractAction finder = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (textField != null) {
                        getContentPane().remove(textField);
                        textField = null;
                    } else {
                        textField = new JTextField();
                        textField.setColumns(10);
                        getContentPane().add(textField, "cell 0 0,alignx center");
                        textField.addActionListener(entAct);
                    }
                    getContentPane().revalidate();
                    getContentPane().repaint();
                }
            };


            p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "finder");
            p.getActionMap().put("finder", finder);

            getContentPane().add(p, "cell 0 1,alignx left,aligny top");


            JButton saveBtn = new JButton(I18n.text("Save File"));
            AbstractAction saveFileAct = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    // filterList.setVisible(false);
                    defaultLogs.addAll(getSelectedItems());
                    applyFilter(FilterList.this);
                }
            };
            saveBtn.addActionListener(saveFileAct);
            getContentPane().add(saveBtn, "cell 0 2,alignx center");
            setVisible(true);

            this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
            setResizable(false);

        }

        /**
         * @return 
         * 
         */
        private Collection<String> getSelectedItems() {
            List<String> selected = new ArrayList<>();

            int listSize = m_list.getModel().getSize();

            // Get all the selected items using the indices
            for (int i = 0; i < listSize; i++) {
                LogItem sel = (LogItem)m_list.getModel().getElementAt(i);
                if (sel.m_selected) {
                    selected.add(sel.logName);
                }

            }
            return selected;            
        }

        @SuppressWarnings("serial")
        class CheckListCellRenderer extends JCheckBox implements ListCellRenderer {

            protected Border m_noFocusBorder = new EmptyBorder(1, 1, 1, 1);

            public CheckListCellRenderer() {
                super();
                setOpaque(true);
                setBorder(m_noFocusBorder);
            }

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                setText(value.toString());
                setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
                LogItem data = (LogItem)value;
                setSelected(data.isSelected());
                setFont(list.getFont());
                setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : m_noFocusBorder);

                return this;

            }
        }

        class CheckListener implements MouseListener, KeyListener {

            protected JList m_list;
            public CheckListener(FilterList parent) {
                m_list = parent.m_list;
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getX() < 20)
                    doCheck();
            }

            public void mousePressed(MouseEvent e) {}

            public void mouseReleased(MouseEvent e) {}

            public void mouseEntered(MouseEvent e) {}

            public void mouseExited(MouseEvent e) {}

            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == ' ')
                    doCheck();
            }

            public void keyTyped(KeyEvent e) {}

            public void keyReleased(KeyEvent e) {}

            protected void doCheck() {

                int index = m_list.getSelectedIndex();
                if (index < 0)
                    return;
                LogItem data = (LogItem)m_list.getModel().getElementAt(index);
                data.invertSelected();
                m_list.repaint();
            }

        }

        class LogItem implements Comparable<LogItem> {

            protected String logName;

            protected boolean m_selected;

            public LogItem(String name, boolean selected) {

                logName = name;

                m_selected = selected;

            }

            @SuppressWarnings("unused")
            public String getName() { return logName; }

            @SuppressWarnings("unused")
            public void setSelected(boolean selected) {
                m_selected = selected;
            }

            public void invertSelected() { 
                m_selected = !m_selected; 
            }

            public boolean isSelected() { 
                return m_selected; 
            }

            public String toString() { 
                return logName; 
            }

            @Override
            public int compareTo(LogItem anotherLog) {
                return logName.compareTo(anotherLog.logName);
            }

        }
    }
}