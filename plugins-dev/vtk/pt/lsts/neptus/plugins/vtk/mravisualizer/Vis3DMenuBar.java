/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * Jan 24, 2014
 */
package pt.lsts.neptus.plugins.vtk.mravisualizer;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.vtk.Vtk;
import pt.lsts.neptus.plugins.vtk.utils.File3DUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author hfq
 *
 */
public class Vis3DMenuBar extends JMenuBar {

    private static final long serialVersionUID = 1L;

    private Vtk vtkInit;

    private JMenu fileMenu, editMenu, viewMenu, toolsMenu, helpMenu;

    // File Menu
    private AbstractAction saveFile, saveFileAsPointCloud, saveFileAsMesh;
    // Edit Menu
    private AbstractAction configs;

    // View Menu
    private AbstractAction resetViewportCamera;
    //    , incrementPointSize, decrementPointSize, colorGradX, colorGradY,
    //    colorGradZ, viewPointCloud, viewMesh, pointBasedRep, wireframeRep, surfaceRep, displayLookUpTable,
    //    displayScaleGrid, displayInfoPointcloud;

    // Tools Menu
    // private AbstractAction exaggerateZ, performMeshing, performSmoothing;

    // Help Menu
    private AbstractAction help;

    public Vis3DMenuBar(Vtk vtkInit) {
        this.vtkInit = vtkInit;
    }

    public void createMultibeamMenuBar() {
        setUpFileMenu();
        setUpEditMenu();
        setUpViewMenu();
        setUpToolsMenu();
        setUpHelpMenu();

        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(toolsMenu);
        add(helpMenu);
    }

    /**
     * 
     */
    @SuppressWarnings("serial")
    private void setUpFileMenu() {
        fileMenu = new JMenu(I18n.text("File"));

        // FIXME - is it necessary?
        saveFile = new VisAction(I18n.text("Save file"), ImageUtils.getIcon("images/menus/save.png"), I18n.text("Save file"), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true))
        {

            @Override
            public void actionPerformed(ActionEvent e) {


            }
        };

        saveFileAsPointCloud = new VisAction(I18n.text("Save pointcloud as") + "...", ImageUtils.getIcon("images/menus/saveas.png"),
                I18n.text("Save a pointcloud to a file") +".", KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, true))
        {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(vtkInit.getLog().getFile("Data.lsf").getParentFile());

                //                FileFilter filefilter = GuiUtils.getCustomFileFilter(I18n.text("3D files ")  + "*.vtk" + ", *.stl"
                //                        + ", *.ply" + ", *.obj" + ", *.wrl" + " *.x3d", new String[] { "X3D", "VTK", "STL", "PLY", "OBJ", "WRL" });

                FileFilter filefilter = GuiUtils.getCustomFileFilter(I18n.text("3D files ")  + "*.vtk" + ", *.stl"
                        + ", *.ply" + ", *.obj" + ", *.wrl" + " *.x3d", File3DUtils.TYPES_3D_FILES);

                chooser.setFileFilter((FileFilter) filefilter);

                int ans = chooser.showDialog(vtkInit, I18n.text("Save as") + "...");
                if (ans == JFileChooser.APPROVE_OPTION) {
                    if (chooser.getSelectedFile().exists()) {
                        ans = JOptionPane.showConfirmDialog(vtkInit, I18n.text("Are you sure you want to overwrite existing file") + "?",
                                I18n.text("Save file as.."), JOptionPane.YES_OPTION);
                        if (ans != JOptionPane.YES_OPTION)
                            return;
                    }
                    File dst = chooser.getSelectedFile();

                }
            }
        };

    }

    /**
     * 
     */
    private void setUpEditMenu() {
        editMenu = new JMenu(I18n.text("Edit"));

    }

    /**
     * 
     */
    private void setUpViewMenu() {
        viewMenu = new JMenu(I18n.text("View"));
    }

    /**
     * 
     */
    private void setUpToolsMenu() {
        toolsMenu = new JMenu(I18n.text("Tools"));

    }

    /**
     * 
     */
    private void setUpHelpMenu() {
        helpMenu = new JMenu(I18n.text("Help"));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphic2d = (Graphics2D) g;
        Color color1 = getBackground();
        Color color2 = Color.GRAY;
        GradientPaint gradPaint = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
        graphic2d.setPaint(gradPaint);
        graphic2d.fillRect(0, 0, getWidth(), getHeight());
    }
}