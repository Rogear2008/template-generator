package com.baomidou.mybatisplus.generator;

import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;

import java.util.Collections;

/**
 * @author Rogear2008
 * @since 3/28/24
 */
public class TemplateGeneratorTest {

    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://127.0.0.1:3306/generator?serverTimezone=GMT%2b8&useUnicode=true", "root", "root")
            .globalConfig(builder -> {
                builder.author("Rogear") // 设置作者
//                    .enableSwagger() // 开启 swagger 模式
//                    .enableSpringdoc() // 开启 springdoc 模式
                    .dateType(DateType.ONLY_DATE)
                    .outputDir(System.getProperty("user.dir") + "/demo/src/main/java") // 指定输出目录
                    .disableOpenDir();
            })
            .packageConfig(builder -> {
                builder.parent("com.rogear.demo.test1") // 设置父包名
                    .moduleName(null) // 设置父模块名
                    // 设置mapperXml生成路径
                    .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/demo/src/main/resources/mapper/")); // 设置mapperXml生成路径
            })
            .strategyConfig(builder -> {
                builder.controllerBuilder().enableRestStyle();
//                builder.serviceBuilder().formatServiceFileName("%sService"); // 去掉service接口首字母I
//                builder.entityBuilder().enableLombok();
//                builder.mapperBuilder().enableBaseResultMap();
//                builder.addTablePrefix("ud_");
                builder.addInclude("book");
//                    builder.entityBuilder().superClass(BaseEntity.class);
//                    builder.entityBuilder().addIgnoreColumns("id", "archived", "update_time", "create_time", "version");
            })
            .templateConfig(builder -> {
//                builder.disable(TemplateType.CONTROLLER); // 不生成controller
//                builder.controller("controller123.java.vm");  // 使用制定模板生成controller
                builder.otherTemplatePath("/usr/local/workspace/template-generator/mybatis-plus-generator/src/main/resources/templates");
                // 不生成对应方法
//                builder.disable(TemplateType.CREATE_FUNCTION, TemplateType.UPDATE_FUNCTION, TemplateType.DELETE_FUNCTION, TemplateType.GET_BY_ID_FUNCTION, TemplateType.GET_LIST_FUNCTION, TemplateType.GET_PAGE_FUNCTION);
            })
            .execute();

    }
}
