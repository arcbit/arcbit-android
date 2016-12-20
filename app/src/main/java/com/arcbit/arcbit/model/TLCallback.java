package com.arcbit.arcbit.model;

public interface TLCallback {
    void onSuccess(Object obj);
    void onFail(Integer status, String error);
}
