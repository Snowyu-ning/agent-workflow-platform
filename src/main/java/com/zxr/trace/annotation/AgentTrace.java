package com.zxr.trace.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentTrace {
    /**
     * 节点名称
     */
    String node();

    /**
     *节点类型：LLM / TOOL / ROUTER
     */
    String type() default "LLM";
}
