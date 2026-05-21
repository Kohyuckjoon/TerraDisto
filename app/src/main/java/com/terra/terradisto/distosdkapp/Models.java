package com.terra.terradisto.distosdkapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Models {

    List<List<String>> leicaBle;

    public Models (){

        leicaBle = new ArrayList<List<String>>(
                Arrays.asList(
                        new ArrayList<String>(
                                Arrays.asList("D110", "D1", "D1-1", "D2", "D210", "E7100i")
                        ),
                        new ArrayList<String>(
                                new ArrayList<String>(
                                        Arrays.asList("S910", "D810")
                                )
                        ),
                        new ArrayList<String>(
                                new ArrayList<String>(
                                        Arrays.asList("D510", "0")
                                )
                        )
                )
        );



    }

    public List<List<String>> getLeicaBleModels(){

        return leicaBle;
    }

}
