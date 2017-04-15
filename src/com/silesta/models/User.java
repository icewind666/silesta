package com.silesta.models;

import com.silesta.interfaces.IIdentifyable;

/**
 * Class for identifying SILESTA user.
 *
 * Created by icewind on 15.04.17.
 */
public class User implements IIdentifyable {
    private String id;


    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
}
