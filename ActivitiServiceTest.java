package com.imooc.activiti.helloworld;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceBuilder;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @Description: 基本 api 使用
 * @Auther: wangwenjie
 * @CreateTime: 2020-01-18
 */
public class ActivitiServiceTest {

    private static Logger LOGGER = LoggerFactory.getLogger(ActivitiServiceTest.class);

    private static final ProcessEngine processEngine = HelloWorld.getProcessEngine();

    RuntimeService runtimeService = processEngine.getRuntimeService();

    TaskService taskService = processEngine.getTaskService();

    HistoryService historyService = processEngine.getHistoryService();

    private ProcessDefinition processDefinition;

    @Before
    public void before() {
        processDefinition = HelloWorld.getProcessDefinition(processEngine);
    }

    //通过key定义流程
    @Test
    public void createProcessByKey() {
        //开始流程实例
        Map<String, Object> map = Maps.newHashMap();
        map.put("key1", "value1");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("second_approve", map);
        LOGGER.info("processInstance = {}", processInstance); //ProcessInstance[5]
    }

    //通过流程实例id 开启
    @Test
    public void createProcessById() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        LOGGER.info("processInstance = {}", processInstance); //ProcessInstance[5]
    }

    //通过builder定义流程
    @Test
    public void createProcessByBuilder() {
        //开始流程实例
        Map<String, Object> map = Maps.newHashMap();
        map.put("key1", "value1");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .businessKey("order001")
                .processDefinitionKey(processDefinition.getKey())
                .variables(map)
                .start();
        LOGGER.info("processInstance = {}", processInstance);   //processInstance = ProcessInstance[5]

        //设置变量
        runtimeService.setVariable(processInstance.getId(), "key1", "value1111");

        //获取创建流程中的参数
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());

        LOGGER.info("variables = {}", variables);
    }

    //查询流程实例
    @Test
    public void queryProcessInstance() {
        //开始流程实例
        Map<String, Object> map = Maps.newHashMap();
        map.put("key1", "value1");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .businessKey("order001")
                .processDefinitionKey(processDefinition.getKey())
                .variables(map)
                .start();
        LOGGER.info("processInstance = {}", processInstance);   //processInstance = ProcessInstance[5]

        //通过实例id查询
        ProcessInstance processInstance1 = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        //通过业务单号
        ProcessInstance order1Instance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey("order001")
                .singleResult();
        LOGGER.info("query instance = {} , by businesskey = {}", processInstance1, order1Instance);

        //获取执行器信息
        List<Execution> executions = runtimeService.createExecutionQuery()
                .listPage(0, 100);
        for (Execution execution : executions) {
            LOGGER.info("获取的执行器信息 {}", execution);
            //获取的执行器信息 ProcessInstance[5]
            //获取的执行器信息 Execution[ id '7' ] - activity 'submitForm - parent '5'
        }

        /**
         * ProcessInstance表示一次工作流业务的实体数据
         * Execution表示流程实例中的具体执行路径
         */
    }

    @Test
    public void testTaskService() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        Task task = taskService.createTaskQuery().singleResult();
        LOGGER.info("task = {}", ToStringBuilder.reflectionToString(task, ToStringStyle.DEFAULT_STYLE));
    }

    @Test
    public void testHistoryService() {
        //发布流程
        Deployment deploy = processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("one_process.bpmn")
                .deploy();


        //开启流程定义builder
        Map<String, Object> variables = Maps.newHashMap();
        Map<String, Object> transientVariables = Maps.newHashMap();

        variables.put("key1", "value1");
        variables.put("key2", "value2");

        transientVariables.put("trans1", "transValue");
        ProcessInstanceBuilder builder = processEngine.getRuntimeService().createProcessInstanceBuilder();
        //开启的流程实例
        ProcessInstance processInstance = builder.processDefinitionKey("one_process")
                .variables(variables)
                .transientVariables(transientVariables)
                .start();

        runtimeService.setVariable(processInstance.getId(), "key1", "value_1_1");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        //formSerivce 完成task
        FormService formService = processEngine.getFormService();
        Map<String, Object> properties = Maps.newHashMap();
        properties.put("formkey1", "formValue1");
        properties.put("key2", "value2_2");
        properties.put("description", "同意");
//        taskService.complete(task.getId(), properties);

//        formService.submitTaskFormData(task.getId(), properties);

        //historyService 流程实例
        List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery().list();
        for (HistoricProcessInstance historicProcessInstance : list) {
            LOGGER.info("historicProcessInstance = {}", ToStringBuilder.reflectionToString(historicProcessInstance));
        }

        //流程执行节点
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().list();
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            LOGGER.info("historicActivityInstance = {}", historicActivityInstance);
            //HistoricActivityInstanceEntity[id=13, activityId=startevent1, activityName=开始]
            //HistoricActivityInstanceEntity[id=14, activityId=approve, activityName=老板审批]
            //HistoricActivityInstanceEntity[id=19, activityId=endevent1, activityName=结束]
        }

        //流程任务
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().list();
        for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
            Map<String, Object> taskLocalVariables = historicTaskInstance.getTaskLocalVariables();
            Map<String, Object> processVariables = historicTaskInstance.getProcessVariables();
            LOGGER.info(" processVariables = {}", processVariables);
            LOGGER.info(" taskLocalVariables = {}", taskLocalVariables);
            LOGGER.info(" historicTaskInstance= {}", ToStringBuilder.reflectionToString(historicTaskInstance));
        }

        //变量
        List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().list();
        for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
            LOGGER.info("historicVariableInstance = {}", ToStringBuilder.reflectionToString(historicVariableInstance));
        }

        //历史详情
        List<HistoricDetail> historicDetails = historyService.createHistoricDetailQuery().listPage(0, 100);
        for (HistoricDetail historicDetail : historicDetails) {
            LOGGER.info("historicDetail = {}", ToStringBuilder.reflectionToString(historicDetail));
        }

        //log
        ProcessInstanceHistoryLog processInstanceHistoryLog = historyService.createProcessInstanceHistoryLogQuery(processInstance.getId())
                .includeActivities()
                .includeComments()
                .includeFormProperties()
                .includeTasks()
                .includeVariables()
                .includeVariableUpdates()
                .singleResult();
        List<HistoricData> historicData = processInstanceHistoryLog.getHistoricData();
        for (HistoricData historicDatum : historicData) {
            LOGGER.info(" historicDatum= {}", ToStringBuilder.reflectionToString(historicDatum));
        }

        //删除流程实例
        historyService.deleteHistoricProcessInstance(processInstance.getId());

        HistoricProcessInstance historyInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        LOGGER.info("historyInstanceQuery = {}", historyInstanceQuery);
    }
}
