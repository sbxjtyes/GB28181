package com.gb28181.sipserver.util;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.io.File;
import java.util.EnumSet;

/**
 * 数据库表结构生成工具
 * 
 * 用于生成MySQL数据库的DDL SQL文件
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
public class SchemaGenerator {

    public static void main(String[] args) {
        try {
            // 创建输出目录
            File outputDir = new File("sql");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // 配置Hibernate
            StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
                    .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect")
                    .applySetting("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver")
                    .applySetting("hibernate.connection.url", "jdbc:mysql://localhost:3306/gb28181_sip_server")
                    .applySetting("hibernate.connection.username", "root")
                    .applySetting("hibernate.connection.password", "123456")
                    .applySetting("hibernate.hbm2ddl.auto", "none")
                    .applySetting("hibernate.show_sql", "true")
                    .applySetting("hibernate.format_sql", "true");

            ServiceRegistry serviceRegistry = registryBuilder.build();

            // 添加实体类
            MetadataSources metadataSources = new MetadataSources(serviceRegistry);
            metadataSources.addAnnotatedClass(com.gb28181.sipserver.entity.DeviceInfo.class);

            MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();

            // 生成创建表的SQL
            SchemaExport schemaExport = new SchemaExport();
            schemaExport.setOutputFile("sql/create_tables.sql");
            schemaExport.setDelimiter(";");
            schemaExport.setFormat(true);
            schemaExport.createOnly(EnumSet.of(TargetType.SCRIPT), metadata);

            System.out.println("数据库表结构SQL文件已生成到: sql/create_tables.sql");

            // 生成删除表的SQL
            schemaExport.setOutputFile("sql/drop_tables.sql");
            schemaExport.drop(EnumSet.of(TargetType.SCRIPT), metadata);

            System.out.println("删除表SQL文件已生成到: sql/drop_tables.sql");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
