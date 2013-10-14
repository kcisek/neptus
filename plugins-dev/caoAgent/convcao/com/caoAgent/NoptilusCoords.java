/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Oct 6, 2013
 */
package convcao.com.caoAgent;

import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * This class is used to translate between noptilus-specific world coordinates and real-world coordinates
 * Properties for this conversion are stored in "conf/noptcoords.properties"
 * @author zp
 */
public class NoptilusCoords {
    
    @NeptusProperty 
    public LocationType squareCenter = new LocationType();
    
    @NeptusProperty 
    public double cellWidth = 5;
    
    @NeptusProperty 
    public int numRows = 40;
    
    @NeptusProperty 
    public int numCols = 40;
    
    {
        loadProps();
    }
    
    protected void loadProps() {
        try {
            PluginUtils.loadProperties("conf/noptcoords.properties", this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void saveProps() {
        try {
            PluginUtils.saveProperties("conf/noptcoords.properties", this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public LocationType convert(int row, int col) {
        if (col < 0 || col >= numCols)
            return null;
        
        if (row < 0 || col >= numRows)
            return null;
        
        LocationType loc = new LocationType(squareCenter);
        double transN = ((-numRows/2) + row) * cellWidth;
        double transE = ((-numCols/2) + col) * cellWidth;
        loc.translatePosition(transN, transE, 0);
        
        return loc;        
    }
    
    public int[] convert(LocationType loc) {
        LocationType sw = new LocationType(squareCenter);
        sw.translatePosition(-cellWidth * numRows/2, -cellWidth * numCols/2, 0);
        double[] offsets = loc.getOffsetFrom(sw);
        if (offsets[0] < 0 || offsets[0] > numRows)
            return null;
        if (offsets[1] < 0 || offsets[1] > numCols)
            return null;
        
        return new int[] {(int)offsets[0],(int)offsets[1]};
    }    
    
    public static void main(String[] args) {
        NoptilusCoords coords = new NoptilusCoords();
        PluginUtils.editPluginProperties(coords, true);
        coords.saveProps();
        System.out.println(coords.convert(20, 21));
    }
}