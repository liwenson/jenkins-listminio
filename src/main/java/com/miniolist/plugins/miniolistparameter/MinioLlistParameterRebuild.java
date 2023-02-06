package com.miniolist.plugins.miniolistparameter;

import com.sonyericsson.rebuild.RebuildParameterPage;
import com.sonyericsson.rebuild.RebuildParameterProvider;
import hudson.Extension;
import hudson.model.ParameterValue;


public class MinioLlistParameterRebuild extends RebuildParameterProvider {
    @Override
    public RebuildParameterPage getRebuildPage(ParameterValue parameterValue) {

        if (parameterValue instanceof MinioListParameterValue) {
            return new RebuildParameterPage(parameterValue.getClass(), "value.jelly");
        } else {
            return null;
        }
    }
}
