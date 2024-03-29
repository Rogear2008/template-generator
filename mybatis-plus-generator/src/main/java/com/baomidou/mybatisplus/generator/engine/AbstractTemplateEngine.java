/*
 * Copyright (c) 2011-2023, baomidou (jobob@qq.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.mybatisplus.generator.engine;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.util.FileUtils;
import com.baomidou.mybatisplus.generator.util.RuntimeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.*;


/**
 * 模板引擎抽象类
 *
 * @author hubin
 * @since 2018-01-10
 */
public abstract class AbstractTemplateEngine {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * 配置信息
     */
    private ConfigBuilder configBuilder;

    private static final String[] DEFAULT_FILE_NAME = {"controller.java.vm", "service.java.vm", "serviceImpl.java.vm", "mapper.java.vm", "mapper.xml.vm", "entity.java.vm", "entity.kt.vm"};

    /**
     * 模板引擎初始化
     */
    @NotNull
    public abstract AbstractTemplateEngine init(@NotNull ConfigBuilder configBuilder);

    /**
     * 输出自定义模板文件
     *
     * @param customFiles 自定义模板文件列表
     * @param tableInfo   表信息
     * @param objectMap   渲染数据
     * @since 3.5.3
     */
    protected void outputCustomFile(@NotNull List<CustomFile> customFiles, @NotNull TableInfo tableInfo, @NotNull Map<String, Object> objectMap) {
        String entityName = tableInfo.getEntityName();
        String parentPath = getPathInfo(OutputFile.parent);
        customFiles.forEach(file -> {
            String filePath = StringUtils.isNotBlank(file.getFilePath()) ? file.getFilePath() : parentPath;
            if (StringUtils.isNotBlank(file.getPackageName())) {
                filePath = filePath + File.separator + file.getPackageName().replaceAll("\\.", StringPool.BACK_SLASH + File.separator);
            }
            Function<TableInfo, String> formatNameFunction = file.getFormatNameFunction();
            String fileName = filePath + File.separator + (null != formatNameFunction ? formatNameFunction.apply(tableInfo) : entityName) + file.getFileName();
            outputFile(new File(fileName), objectMap, file.getTemplatePath(), file.isFileOverride());
        });
    }

    /**
     * 输出实体文件
     *
     * @param tableInfo 表信息
     * @param objectMap 渲染数据
     * @since 3.5.0
     */
    protected void outputEntity(@NotNull TableInfo tableInfo, @NotNull Map<String, Object> objectMap) {
        String entityName = tableInfo.getEntityName();
        String entityPath = getPathInfo(OutputFile.entity);
        if (StringUtils.isNotBlank(entityName) && StringUtils.isNotBlank(entityPath)) {
            getTemplateFilePath(template -> template.getEntity(getConfigBuilder().getGlobalConfig().isKotlin())).ifPresent((entity) -> {
                String entityFile = String.format((entityPath + File.separator + "%s" + suffixJavaOrKt()), entityName);
                outputFile(getOutputFile(entityFile, OutputFile.entity), objectMap, entity, getConfigBuilder().getStrategyConfig().entity().isFileOverride());
            });
        }
    }

    protected File getOutputFile(String filePath, OutputFile outputFile) {
        return getConfigBuilder().getStrategyConfig().getOutputFile().createFile(filePath, outputFile);
    }

    /**
     * 输出Mapper文件(含xml)
     *
     * @param tableInfo 表信息
     * @param objectMap 渲染数据
     * @since 3.5.0
     */
    protected void outputMapper(@NotNull TableInfo tableInfo, @NotNull Map<String, Object> objectMap) {
        // MpMapper.java
        String entityName = tableInfo.getEntityName();
        String mapperPath = getPathInfo(OutputFile.mapper);
        if (StringUtils.isNotBlank(tableInfo.getMapperName()) && StringUtils.isNotBlank(mapperPath)) {
            getTemplateFilePath(TemplateConfig::getMapper).ifPresent(mapper -> {
                String mapperFile = String.format((mapperPath + File.separator + tableInfo.getMapperName() + suffixJavaOrKt()), entityName);
                outputFile(getOutputFile(mapperFile, OutputFile.mapper), objectMap, mapper, getConfigBuilder().getStrategyConfig().mapper().isFileOverride());
            });
        }
        // MpMapper.xml
        String xmlPath = getPathInfo(OutputFile.xml);
        if (StringUtils.isNotBlank(tableInfo.getXmlName()) && StringUtils.isNotBlank(xmlPath)) {
            getTemplateFilePath(TemplateConfig::getXml).ifPresent(xml -> {
                String xmlFile = String.format((xmlPath + File.separator + tableInfo.getXmlName() + ConstVal.XML_SUFFIX), entityName);
                outputFile(getOutputFile(xmlFile, OutputFile.xml), objectMap, xml, getConfigBuilder().getStrategyConfig().mapper().isFileOverride());
            });
        }
    }

    /**
     * 输出service文件
     *
     * @param tableInfo 表信息
     * @param objectMap 渲染数据
     * @since 3.5.0
     */
    protected void outputService(@NotNull TableInfo tableInfo, @NotNull Map<String, Object> objectMap) {
        // IMpService.java
        String entityName = tableInfo.getEntityName();
        // 判断是否要生成service接口
        if (tableInfo.isServiceInterface()) {
            String servicePath = getPathInfo(OutputFile.service);
            if (StringUtils.isNotBlank(tableInfo.getServiceName()) && StringUtils.isNotBlank(servicePath)) {
                getTemplateFilePath(TemplateConfig::getService).ifPresent(service -> {
                    String serviceFile = String.format((servicePath + File.separator + tableInfo.getServiceName() + suffixJavaOrKt()), entityName);
                    outputFile(getOutputFile(serviceFile, OutputFile.service), objectMap, service, getConfigBuilder().getStrategyConfig().service().isFileOverride());
                });
            }
        }
        // MpServiceImpl.java
        String serviceImplPath = getPathInfo(OutputFile.serviceImpl);
        if (StringUtils.isNotBlank(tableInfo.getServiceImplName()) && StringUtils.isNotBlank(serviceImplPath)) {
            getTemplateFilePath(TemplateConfig::getServiceImpl).ifPresent(serviceImpl -> {
                String implFile = String.format((serviceImplPath + File.separator + tableInfo.getServiceImplName() + suffixJavaOrKt()), entityName);
                outputFile(getOutputFile(implFile, OutputFile.serviceImpl), objectMap, serviceImpl, getConfigBuilder().getStrategyConfig().service().isFileOverride());
            });
        }
    }

    /**
     * 输出controller文件
     *
     * @param tableInfo 表信息
     * @param objectMap 渲染数据
     * @since 3.5.0
     */
    protected void outputController(@NotNull TableInfo tableInfo, @NotNull Map<String, Object> objectMap) {
        // MpController.java
        String controllerPath = getPathInfo(OutputFile.controller);
        if (StringUtils.isNotBlank(tableInfo.getControllerName()) && StringUtils.isNotBlank(controllerPath)) {
            getTemplateFilePath(TemplateConfig::getController).ifPresent(controller -> {
                String entityName = tableInfo.getEntityName();
                String controllerFile = String.format((controllerPath + File.separator + tableInfo.getControllerName() + suffixJavaOrKt()), entityName);
                outputFile(getOutputFile(controllerFile, OutputFile.controller), objectMap, controller, getConfigBuilder().getStrategyConfig().controller().isFileOverride());
            });
        }
    }

    /**
     * ??controller??
     *
     * @param tableInfo ???
     * @param objectMap ????
     * @since 3.5.0
     */
    protected void outputOther(@NotNull TableInfo tableInfo, @NotNull Map<String, Object> objectMap) {
        String otherPath = (String) objectMap.get("otherPath");
        String templateName = (String) objectMap.get("templateName");
        String templateFilePath = (String) objectMap.get("templateFilePath");
        this.getTemplateFilePath(templateFilePath).ifPresent((other) -> {
            String entityName = tableInfo.getEntityName();
            String otherFile = String.format(otherPath + File.separator + templateName + this.suffixJavaOrKt(), entityName);
            this.outputFile(this.getOutputFile(otherFile, OutputFile.other), objectMap, other, this.getConfigBuilder().getStrategyConfig().controller().isFileOverride());
        });
    }

    /**
     * 输出文件
     *
     * @param file         文件
     * @param objectMap    渲染信息
     * @param templatePath 模板路径
     * @param fileOverride 是否覆盖已有文件
     * @since 3.5.2
     */
    protected void outputFile(@NotNull File file, @NotNull Map<String, Object> objectMap, @NotNull String templatePath, boolean fileOverride) {
        if (isCreate(file, fileOverride)) {
            try {
                // 全局判断【默认】
                boolean exist = file.exists();
                if (!exist) {
                    File parentFile = file.getParentFile();
                    FileUtils.forceMkdir(parentFile);
                }
                writer(objectMap, templatePath, file);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    /**
     * 获取模板路径
     *
     * @param function function
     * @return 模板路径
     * @since 3.5.0
     */
    @NotNull
    protected Optional<String> getTemplateFilePath(@NotNull Function<TemplateConfig, String> function) {
        TemplateConfig templateConfig = getConfigBuilder().getTemplateConfig();
        String filePath = function.apply(templateConfig);
        if (StringUtils.isNotBlank(filePath)) {
            return Optional.of(templateFilePath(filePath));
        }
        return Optional.empty();
    }

    /**
     * 获取路径信息
     *
     * @param outputFile 输出文件
     * @return 路径信息
     */
    @Nullable
    protected String getPathInfo(@NotNull OutputFile outputFile) {
        return getConfigBuilder().getPathInfo().get(outputFile);
    }

    /**
     * 批量输出 java xml 文件
     */
    @NotNull
    public AbstractTemplateEngine batchOutput() {
        try {
            ConfigBuilder config = this.getConfigBuilder();
            List<TableInfo> tableInfoList = config.getTableInfoList();
            tableInfoList.forEach(tableInfo -> {
                tableInfo.setEntityNameLowerCamel(tableInfo.getEntityName().substring(0, 1).toLowerCase() + tableInfo.getEntityName().substring(1));
                Map<String, Object> objectMap = this.getObjectMap(config, tableInfo);
                Optional.ofNullable(config.getInjectionConfig()).ifPresent(t -> {
                    // 添加自定义属性
                    t.beforeOutputFile(tableInfo, objectMap);
                    // 输出自定义文件
                    outputCustomFile(t.getCustomFiles(), tableInfo, objectMap);
                });
                // entity
                outputEntity(tableInfo, objectMap);
                // mapper and xml
                outputMapper(tableInfo, objectMap);
                // service
                outputService(tableInfo, objectMap);
                // controller
                outputController(tableInfo, objectMap);

                String entityPath = this.getPathInfo(OutputFile.entity);
                String otherPath = entityPath.substring(0, entityPath.lastIndexOf("/"));
                objectMap.put("otherPath", otherPath);
                objectMap.put("parentPackage", config.getPackageConfig().getParent());
                objectMap.put("currentPackage", config.getPackageConfig().getParent());
                // 遍历生成其它文件
                String rootTemplatePath;
                rootTemplatePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "templates";
                String otherTemplatePath = config.getTemplateConfig().getOtherTemplatePath();
                if (StringUtils.isNotBlank(otherTemplatePath)) {
                    rootTemplatePath = otherTemplatePath;
                }
                objectMap.put("rootTemplatePath", rootTemplatePath);
                File file = new File(rootTemplatePath);
                output(tableInfo, file, objectMap);
            });
        } catch (Exception e) {
            throw new RuntimeException("无法创建文件，请检查配置信息！", e);
        }
        return this;
    }

    /**
     * 生成其它文件
     *
     * @param tableInfo
     * @param file
     * @param objectMap
     */
    private void output(TableInfo tableInfo, File file, Map<String, Object> objectMap) {
        if (file == null) {
            return;
        }
        List<File> directoryList = new ArrayList<>();
        List<File> fileList = new ArrayList<>();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File file1 : files) {
                if (file1.isDirectory()) {
                    directoryList.add(file1);
                } else {
                    fileList.add(file1);
                }
            }

            // 设置路径等信息
            String currentPackage = file.getPath().replace((String) objectMap.get("rootTemplatePath"), "").replace(File.separator, ".");
            if (currentPackage.startsWith(File.separator) || currentPackage.startsWith(".")) {
                currentPackage = currentPackage.substring(1);
            }
            String oldParentPackage = (String) objectMap.get("parentPackage");
            if (StringUtils.isNotBlank(currentPackage)) {
                objectMap.put("currentPackage", oldParentPackage + "." + currentPackage);
            }
            String oldOtherPath = (String) objectMap.get("otherPath");
            if (StringUtils.isNotBlank(currentPackage)) {
                objectMap.put("otherPath", oldOtherPath + File.separator + file.getName());
            }

            // 生成文件
            if (CollectionUtils.isNotEmpty(fileList)) {
                for (File file1 : fileList) {
                    output(tableInfo, file1, objectMap);
                }
            }

            // 第归处理文件夹
            if (CollectionUtils.isNotEmpty(directoryList)) {
                for (File file1 : directoryList) {
                    output(tableInfo, file1, objectMap);
                    if (file1.isDirectory() && file1.listFiles() != null && file1.listFiles().length > 0 && StringUtils.isNotBlank(currentPackage)) {
                        // ??????????
                        String otherPath = (String) objectMap.get("otherPath");
                        objectMap.put("otherPath", otherPath.substring(0, otherPath.lastIndexOf(File.separator)));
                    }
                }
            }
            return;
        }

        String name = file.getName();
        if (!name.endsWith(".vm") || Arrays.stream(DEFAULT_FILE_NAME).anyMatch(name::equals)) {
            return;
        }

        String entityName = tableInfo.getEntityName();
        String templateName = name.split("\\.")[0];
        templateName = templateName.substring(0, 1).toUpperCase() + templateName.substring(1);
        if (templateName.contains("[entity]")) {
            templateName = templateName.replace("[entity]", entityName);
        } else {
            templateName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1) + templateName;
        }
        String subStr = (file.getPath().substring(file.getPath().lastIndexOf(File.separator + "templates" + File.separator)));
        String templateFilePath = subStr.substring(0, subStr.lastIndexOf(".vm"));
        objectMap.put("fileName", templateName);
        objectMap.put("templateName", templateName);
        objectMap.put("templateFilePath", templateFilePath);
        this.outputOther(tableInfo, objectMap);

    }

    protected @NotNull Optional<String> getTemplateFilePath(@NotNull String templatePath) {
        String filePath = templatePath;
        return StringUtils.isNotBlank(filePath) ? Optional.of(this.templateFilePath(filePath)) : Optional.empty();
    }

    /**
     * 将模板转化成为文件
     *
     * @param objectMap    渲染对象 MAP 信息
     * @param templatePath 模板文件
     * @param outputFile   文件生成的目录
     * @throws Exception 异常
     * @since 3.5.0
     */
    @NotNull
    public abstract void writer(@NotNull Map<String, Object> objectMap, @NotNull String templatePath, @NotNull File outputFile) throws Exception;

    /**
     * 打开输出目录
     */
    public void open() {
        String outDir = getConfigBuilder().getGlobalConfig().getOutputDir();
        if (StringUtils.isBlank(outDir) || !new File(outDir).exists()) {
            System.err.println("未找到输出目录：" + outDir);
        } else if (getConfigBuilder().getGlobalConfig().isOpen()) {
            try {
                RuntimeUtils.openDir(outDir);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 渲染对象 MAP 信息
     *
     * @param config    配置信息
     * @param tableInfo 表信息对象
     * @return ignore
     */
    @NotNull
    public Map<String, Object> getObjectMap(@NotNull ConfigBuilder config, @NotNull TableInfo tableInfo) {
        StrategyConfig strategyConfig = config.getStrategyConfig();
        Map<String, Object> controllerData = strategyConfig.controller().renderData(tableInfo);
        Map<String, Object> objectMap = new HashMap<>(controllerData);
        Map<String, Object> mapperData = strategyConfig.mapper().renderData(tableInfo);
        objectMap.putAll(mapperData);
        Map<String, Object> serviceData = strategyConfig.service().renderData(tableInfo);
        objectMap.putAll(serviceData);
        Map<String, Object> entityData = strategyConfig.entity().renderData(tableInfo);
        objectMap.putAll(entityData);
        objectMap.put("config", config);
        objectMap.put("package", config.getPackageConfig().getPackageInfo());
        GlobalConfig globalConfig = config.getGlobalConfig();
        objectMap.put("author", globalConfig.getAuthor());
        objectMap.put("kotlin", globalConfig.isKotlin());
        objectMap.put("swagger", globalConfig.isSwagger());
        objectMap.put("springdoc", globalConfig.isSpringdoc());
        objectMap.put("date", globalConfig.getCommentDate());
        // 启用 schema 处理逻辑
        String schemaName = "";
        if (strategyConfig.isEnableSchema()) {
            // 存在 schemaName 设置拼接 . 组合表名
            schemaName = config.getDataSourceConfig().getSchemaName();
            if (StringUtils.isNotBlank(schemaName)) {
                schemaName += ".";
                tableInfo.setConvert(true);
            }
        }
        objectMap.put("schemaName", schemaName);
        objectMap.put("table", tableInfo);
        objectMap.put("entity", tableInfo.getEntityName());
        return objectMap;
    }

    /**
     * 模板真实文件路径
     *
     * @param filePath 文件路径
     * @return ignore
     */
    @NotNull
    public abstract String templateFilePath(@NotNull String filePath);

    /**
     * 检查文件是否创建文件
     *
     * @param file         文件
     * @param fileOverride 是否覆盖已有文件
     * @return 是否创建文件
     * @since 3.5.2
     */
    protected boolean isCreate(@NotNull File file, boolean fileOverride) {
        if (file.exists() && !fileOverride) {
            LOGGER.warn("文件[{}]已存在，且未开启文件覆盖配置，需要开启配置可到策略配置中设置！！！", file.getName());
        }
        return !file.exists() || fileOverride;
    }

    /**
     * 文件后缀
     */
    protected String suffixJavaOrKt() {
        return getConfigBuilder().getGlobalConfig().isKotlin() ? ConstVal.KT_SUFFIX : ConstVal.JAVA_SUFFIX;
    }

    @NotNull
    public ConfigBuilder getConfigBuilder() {
        return configBuilder;
    }

    @NotNull
    public AbstractTemplateEngine setConfigBuilder(@NotNull ConfigBuilder configBuilder) {
        this.configBuilder = configBuilder;
        return this;
    }
}
