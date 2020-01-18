package com.imooc.activiti.helloworld;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HelloWorld {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorld.class);

    public static void main(String args[]) throws ParseException {
        LOGGER.info("启动程序");
        //创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        //部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        //启动运行流程
        ProcessInstance processInstance = startProcess(processEngine, processDefinition);

        //处理流程任务
        handlerTask(processEngine, processInstance);

        LOGGER.info("结束程序");
    }

    /**
     * taskService处理任务
     * @param processEngine
     * @param processInstance
     * @throws ParseException
     */
    public static void handlerTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            LOGGER.info("待处理任务数量 = [{}]", list.size());

            for (Task task : list) {
                LOGGER.info("待处理任务 [{}]", task);
                FormService formService = processEngine.getFormService();
                Map<String, Object> variables = getStringObjectMap(scanner, task, formService);
                //完成task
                taskService.complete(task.getId(), variables);
                //同步最新的流程实例
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .singleResult();
            }
        }
    }

    /**
     * 组织数据
     * @param scanner
     * @param task
     * @param formService
     * @return
     * @throws ParseException
     */
    public static Map<String, Object> getStringObjectMap(Scanner scanner, Task task, FormService formService) throws ParseException {
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        //存储task property和值的map
        Map<String, Object> variables = Maps.newHashMap();
        for (FormProperty property : formProperties) {
            LOGGER.info("当前属性名称 [{}] , 类型 [{}]", property.getId(), property.getType());
            String line = null;
            if (StringFormType.class.isInstance(property.getType())) {
                LOGGER.info("请输入 {} ?", property.getName());
                line = scanner.nextLine();
                LOGGER.info("您输入的内容是 [{}]", line);
                variables.put(property.getId(), line);
            } else if (DateFormType.class.isInstance(property.getType())) {
                LOGGER.info("请输入 {} ? 格式 （yyyy-MM-dd）", property.getName());
                line = scanner.nextLine();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date date = format.parse(line);
                variables.put(property.getId(), date);
            } else {
                LOGGER.info("输入的类型 [{}] 不合法", property.getType());
            }
        }
        return variables;
    }

    /**
     * runtimeService启动流程实例
     * @param processEngine
     * @param processDefinition
     * @return
     */
    public static ProcessInstance startProcess(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        LOGGER.info("启动流程 [{}] ", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    /**
     * 获取流程定义对象
     * @param processEngine
     * @return
     */
    public static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String id = deployment.getId();

        //获取流程定义对象
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .deploymentId(id)
                .singleResult();
        LOGGER.info("流程定义文件 [{}],流程定义id [{}], 发布 id = [{}]，流程定义版本[{}],流程定义key[{}]", processDefinition.getResourceName(), processDefinition.getId(), id,processDefinition.getVersion(),processDefinition.getKey());
        //流程定义文件 [second_approve.bpmn20.xml],流程定义id [second_approve:1:4], 发布 id = [1]，流程定义版本1,流程定义key second_approve
        return processDefinition;
    }

    /**
     * 和获取流程引擎
     * @return
     */
    public static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;
        LOGGER.info("流程引擎名称{},版本{}", name, version);
        return processEngine;
    }
}
