package com.github.reinert.jjschema.v1;

import java.util.List;

import com.github.reinert.jjschema.SchemaIgnore;

/**
 * 字段的类型及可选项定义。在具体使用中，采用的固定的方式获取实例化对象（即调用 getInstance() 方法），
 * 因此在实现中要求实现类必须 ：1）定义为静态且公开的（public static）； 2）重写并覆盖 getInstance()  静态方法。
 */
public class FieldType  {
    
    public enum FieldOptionsType {
          NULL      /* 无可选项可用, 即由用户自行输入的字段 */
        , STATIC    /* 静态可选项，即固定的几个可选值 */
        , DYNAMIC   /* 动态可选项，通常对应在后端某个关联表单中选择并添加 */
        ;
    };
    
    /**
     * 定义字段类型的名称， 默认值 Simple
     */
    @SchemaIgnore
    public String getTypeName() {
        return "String";
    }
    
    /**
     * 定义字段可选项的类型， 默认值 NULL
     */
    @SchemaIgnore
    public FieldOptionsType getOptionsType() {
        return FieldOptionsType.NULL;
    }
    
    /**
     * 定义静态可选项的选项列表。定义方式如下：<pre>
     *   1, 必须重写 getOptionsType 方法，返回  STATIC 
     *   
     *   2, 必须重写  getStaticOptions 方法，返回可用的可选项 ：
     *   
     * </pre>
     */
    @SchemaIgnore
    public List<? extends FieldOption> getStaticOptions() throws Exception {
        return null;
    }
    
    /**
     * 定义动态可选项的选项列表。定义方式如下：<pre>
     *   1, 必须重写 getOptionsType 方法，返回  DYNAMIC 
     *         
     *   2, 必须重写  getDynamicFilterFormClass 方法，返回查询条件的定义类
     *        
     *   3, 必须重写  queryDynamicOptions 方法，返回可用的可选项，(要求入参必须与步骤2定义的类保持一致) ：
     *        
     * </pre>
     */
    @SchemaIgnore
    public List<? extends FieldOption> queryDynamicOptions(FieldOptionsFilter filter) throws Exception {
        throw new RuntimeException("Not-Implemented");
    }
    
    /**
     * 定义动态可选项的查询条件定义类，最重要的目的在于动态创建查询条件使用
     */
    @SchemaIgnore
    public Class<? extends FieldOptionsFilter> getDynamicFilterFormClass() {
        return null;
    }
    
    /**
     * 动态创建表单模型定义。在一些场景下，字段的值为复杂的结构化对象，且必须人为输入而非可选择时，可以重写该方法返回创建视图模型定义类。
     */
    @SchemaIgnore
    public Class<?> getListItemCreationFormClass() {
        return null;
    }
    
    /**
     * 通过给定的选项值列表，获取选项的完整内容。
     */
    @SchemaIgnore
    public List<? extends FieldOption> queryDynamicValues(Object[] optionValues) throws Exception {
        throw new RuntimeException("Not-Implemented");
    }
}
