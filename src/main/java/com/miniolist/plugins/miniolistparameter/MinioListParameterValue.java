package com.miniolist.plugins.miniolistparameter;

import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

public class MinioListParameterValue extends StringParameterValue {

    @DataBoundConstructor
    public MinioListParameterValue(String name, String value) {
        super(name, value);
    }

}
