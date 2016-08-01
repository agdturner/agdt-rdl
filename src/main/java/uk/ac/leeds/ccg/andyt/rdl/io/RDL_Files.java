/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.leeds.ccg.andyt.rdl.io;

import java.io.File;

/**
 *
 * @author geoagdt
 */
public class RDL_Files {
    private static File dir;
    
    public static File getDir(){
        return dir;
    }
    
    public static File setDir(File dir1){
        if (!dir1.exists()) {
            dir1.mkdirs();
        }
        return dir = dir1;
    }
}
