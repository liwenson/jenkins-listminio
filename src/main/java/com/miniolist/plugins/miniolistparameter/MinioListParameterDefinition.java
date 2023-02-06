package com.miniolist.plugins.miniolistparameter;


import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.cli.CLICommand;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.minio.MinioClient;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.Symbol;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.*;


import io.minio.messages.Item;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.miniolist.plugins.miniolistparameter.utils.CredentialsHelper.getCredentialsListBox;
import static com.miniolist.plugins.miniolistparameter.utils.MinioUtil.getClient;
import static com.miniolist.plugins.miniolistparameter.utils.MinioUtil.listMinio;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.sort;


//  ParameterDefinition 代表这是一个构建参数
public class MinioListParameterDefinition extends ParameterDefinition implements Comparable<MinioListParameterDefinition> {
    private static final long serialVersionUID = 8844393428160958228L;
    private static final Logger LOGGER = Logger.getLogger(MinioListParameterDefinition.class.getName());

    private static final String EMPTY_JOB_NAME = "EMPTY_JOB_NAME";
    private static final String DEFAULT_LIST_SIZE = "8";

    private static final Boolean DEFAULT_DEBUG = false;


    private final UUID uuid;

    private SelectedValue selectedValue;

    private String host;
    private String credentialsId;
    private String bucket;
    private String target;
    private String listSize;
    private Boolean debug;


    /**
     * 构造函数在构建项目配置构建参数时调用（The constructor is called when the build project configures build parameter）
     *
     * @param name        构建参数名称 （Build parameter name）
     * @param description 该构建参数的默认值，会随着每次用户选择Agent服务器而改变（The default value of this build parameter will change each time the user selects the Agent server）
     */
    @DataBoundConstructor
    public MinioListParameterDefinition(String name, String description, String host, String credentialsId, SelectedValue selectedValue, String bucket, String target, String listSize, Boolean debug) {
        super(name, description);
        this.uuid = UUID.randomUUID();
        this.host = host;
        this.credentialsId = credentialsId;
        this.bucket = bucket;
        this.target = target;
        this.selectedValue = selectedValue;
        this.listSize = listSize;
        this.debug = debug;
    }


    @DataBoundSetter
    public void setListSize(String listSize) {
        this.listSize = listSize;
    }

    public String getListSize() {
        return listSize == null ? DEFAULT_LIST_SIZE : listSize;
    }

    public UUID getUuid() {
        return uuid;
    }

    @DataBoundSetter
    public void setSelectedValue(SelectedValue selectedValue) {
        this.selectedValue = selectedValue;
    }

    @DataBoundSetter
    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public Boolean getDebug() {
        return debug == null ? DEFAULT_DEBUG : debug;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getBucket() {
        return bucket;
    }

    @DataBoundSetter
    public void setBucket(String bucket) {
        if (StringUtils.isEmpty(StringUtils.trim(bucket))) {
            bucket = "*";
        }

        this.bucket = bucket;
    }

    public String getHost() {
        return host;
    }

    @DataBoundSetter
    public void setHost(String host) {
        this.host = host;
    }

    public String getTarget() {
        return target;
    }

    @DataBoundSetter
    public void setTarget(String target) {
        this.target = target;
    }


    /**
     * 防止项目中有多个Agent Server Parameter，为每个构建参数的DIV元素创建唯一的ID值（Prevent multiple Agent Server Parameter in the project, create a unique ID value for each DIV element of the build parameter）
     *
     * @return DIV唯一ID（DIV unique ID）
     */
    public String getDivId() {
        return String.format("%s-%s", getName().replaceAll("\\W", "_"), this.uuid);
    }


    /**
     * 创建Agent服务器参数的参数结果对象（Create parameter result object for Agent server parameter）
     *
     * @param staplerRequest StaplerRequest对象（StaplerRequest object）
     * @param jsonObject     Agent服务器参数的结果对象，Json格式（Agent Server Parameter result object, Json format）
     * @return 参数结果对象 （Parameter result object）
     */
    @CheckForNull
    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
        Object value = jsonObject.get("value");
        StringBuilder strValue = new StringBuilder();
        if (value instanceof String) {
            strValue.append(value);
        } else if (value instanceof JSONArray) {
            JSONArray jsonValues = (JSONArray) value;
            for (int i = 0; i < jsonValues.size(); i++) {
                strValue.append(jsonValues.getString(i));
                if (i < jsonValues.size() - 1) {
                    strValue.append(",");
                }
            }
        }

        return new MinioListParameterValue(jsonObject.getString("name"), strValue.toString());
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String value[] = req.getParameterValues(getName());
        if (value == null || value.length == 0 || StringUtils.isBlank(value[0])) {
            return this.getDefaultParameterValue();
        } else {
            return new MinioListParameterValue(getName(), value[0]);
        }
    }

    @Override
    public ParameterValue createValue(CLICommand command, String value) {
        if (StringUtils.isNotEmpty(value)) {
            return new MinioListParameterValue(getName(), value);
        }
        return getDefaultParameterValue();
    }


    @Override
    public ParameterValue getDefaultParameterValue() {

        switch (getSelectedValue()) {
            case TOP:
                try {
                    ListBoxModel valueItems = getDescriptor().doFillValueItems(getParentJob(), getName());
                    if (valueItems.size() > 0) {
                        return new MinioListParameterValue(getName(), valueItems.get(0).value);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, getCustomJobName() + " " + " ", e);
                }
                break;
            case DEFAULT:
            case NONE:
            default:
                return super.getDefaultParameterValue();
        }

        return super.getDefaultParameterValue();
    }


    private String getCustomJobName() {
        Job job = getParentJob();
        String fullName = job != null ? job.getFullName() : EMPTY_JOB_NAME;

        return "[ " + fullName + " ]";
    }

    private Job getParentJob() {
        Job context = null;
        List<Job> jobs = Objects.requireNonNull(Jenkins.getInstance()).getAllItems(Job.class);

        for (Job job : jobs) {
            if (!(job instanceof TopLevelItem)) continue;

            ParametersDefinitionProperty property = (ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class);

            if (property != null) {
                List<ParameterDefinition> parameterDefinitions = property.getParameterDefinitions();

                if (parameterDefinitions != null) {
                    for (ParameterDefinition pd : parameterDefinitions) {
                        if (pd instanceof MinioListParameterDefinition && ((MinioListParameterDefinition) pd).compareTo(this) == 0) {
                            context = job;
                            break;
                        }
                    }
                }
            }
        }

        return context;
    }

    public SelectedValue getSelectedValue() {
        return selectedValue == null ? SelectedValue.TOP : selectedValue;
    }


    // 主要逻辑入口
    @Nonnull
    private Map<String, String> generateContents(Job job) throws IOException, InterruptedException {
        Map<String, String> paramList = new LinkedHashMap<String, String>();

        if (this.getDebug() == true) {
            System.out.println("start minio list plugin ");
            System.out.println("jobName: " + job.getFullName());
            System.out.println("target: " + getTarget());
        }

        try {
            MinioClient client = getClient(getHost(), credentialsId);
            paramList = listMinio(client, getBucket(), getTarget(), getListSize(),getDebug());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            paramList.clear();
            paramList.put(e.getMessage(), e.getMessage());
        }

        return paramList;

    }


    // DescriptorImpl
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public int compareTo(MinioListParameterDefinition o) {
        return o.uuid.equals(uuid) ? 0 : -1;
    }


    /**
     * 参数描述类，实现了与UI交互的方法。（The parameter description class implements the method of interacting with the UI.）
     * 保存的时候会调用验证name是否合法的方法，这个方法是在SlaveParameterDefinition类中定义一个名叫DescriptorImpl的内部静态类
     * 该类继承ParameterDescriptor扩展点，基本上和UI交互的方法都定义在这个类中了，就连实例化对象的方法也是定义这个类中，
     * 可以说Jenkins主要通过ParameterDescriptor类操作各种构建参数对象。
     */
    @Symbol("Miniolist")
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        // 插件任务在jenkins上面的显示名称
        @Override
        @Nonnull
        public String getDisplayName() {
            // 方法返回的是一个 String 类型的值，这个名称会用在 web 界面上显示的名称
            return ResourceBundleHolder.get(MinioListParameterDefinition.class).format("displayName");
        }

        // 验证 host 有效性
        public FormValidation doCheckHost(StaplerRequest req, @AncestorInPath Item context, @QueryParameter String value) {
            // 验证 host 格式是否有效
            String url = Util.fixEmptyAndTrim(value);

            if (url == null) {
                return FormValidation.error("Host is required");
            }

            if (url.indexOf('$') != -1) {
                return FormValidation.warning("This repository URL is parameterized, syntax validation skipped");
            }

            try {
                new URIish(value);
            } catch (URISyntaxException e) {
                return FormValidation.error("Repository URL is illegal");
            }

            return FormValidation.ok();
        }

        // 验证 bucket有效性
        public FormValidation doCheckBucket(StaplerRequest req, @AncestorInPath Item context, @QueryParameter String value) {
            // 验证 bucket 格式是否有效
            String url = Util.fixEmptyAndTrim(value);

            if (url == null) {
                return FormValidation.error("bucket is required");
            }

            if (url.indexOf('$') != -1) {
                return FormValidation.warning("This repository URL is parameterized, syntax validation skipped");
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item item,
                @QueryParameter String credentialsId,
                @QueryParameter String uri
        ) {
            return getCredentialsListBox((hudson.model.Item) item, credentialsId, uri);
        }


        /**
         * 在项目构建页面设置参数时调用，将所有可以用于构建的服务器名称，绑定到下拉菜单中让用户选择，方法名称必须是"doFill"+要绑定数据的页面元素field属性值+"Items"。（Called when setting parameters on the project construction page, bind all server names that can be used for construction to the drop-down menu for the user to choose, the method name must be "doFill" + field attribute value of the page element to be bound data + "Items ".）
         *
         * @param context 当前项目的构建任务。（The build task of the current project.）
         * @param param   Slave Server Parameter的名称。（The name of the Slave Server Parameter.）
         * @return Slave名称列表是Select元素，返回此元素的内容。（The Slave name list is the Select element, and returns the content of this element.）
         */
        public ListBoxModel doFillValueItems(@AncestorInPath Job<?, ?> context, @QueryParameter String param)
                throws IOException, InterruptedException {
            ListBoxModel items = new ListBoxModel();
            if (context != null) {
                ParametersDefinitionProperty prop = context.getProperty(ParametersDefinitionProperty.class);
                if (prop != null) {
                    ParameterDefinition def = prop.getParameterDefinition(param);
                    if (def instanceof MinioListParameterDefinition) {
                        try {
                            Map<String, String> paramList = ((MinioListParameterDefinition) def).generateContents(context);
                            for (Map.Entry<String, String> entry : paramList.entrySet()) {
                                items.add(entry.getKey(), entry.getValue());
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                        }

                    }
                }
            }
            return items;
        }

    }


}
