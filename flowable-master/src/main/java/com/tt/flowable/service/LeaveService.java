package com.tt.flowable.service;


import com.tt.flowable.pojo.ResponseBean;
import com.tt.flowable.pojo.leave.leaveApproveVo;
import com.tt.flowable.pojo.leave.leaveInfo;
import com.tt.flowable.pojo.leave.leaveRequestVo;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class LeaveService {

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    TaskService taskService;

    @Autowired
    HistoryService historyService;

    @Transactional
    public ResponseBean saveLeave(leaveRequestVo leaveRequestVo){
        Map<String,Object> leaves=new HashMap<>();
        leaves.put("name",leaveRequestVo.getName());
        leaves.put("days",leaveRequestVo.getDays());
        leaves.put("reason",leaveRequestVo.getReason());
        try {
            ProcessInstance a1=runtimeService.startProcessInstanceByKey("leaveprocess",leaveRequestVo.getName(),leaves);

           // ProcessInstance a1 = runtimeService.startProcessInstanceByKey("leaveprocess", leaves);
            Task task = taskService.createTaskQuery().processInstanceId(a1.getId()).singleResult();
            taskService.complete(task.getId());
            return ResponseBean.ok("请假申请提交成功");
        }catch (Exception e){
            e.printStackTrace();
        }
        return ResponseBean.error("请假申请提交失败");

    }

    /**
     *查询领导分组的任务
     * @return
     */
   public ResponseBean queryLeadTask(String identity) {

        //List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(identity).list();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("javaboy").list();
        List<Map<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            Map<String, Object> variables = taskService.getVariables(task.getId());
            variables.put("id", task.getId());
            list.add(variables);
        }
        return ResponseBean.ok("加载成功", list);

    }



    /**
     * 请假列表
     * @param name
     * @return
     */
    public ResponseBean searchResult(String name) {

        List<leaveInfo> leaveInfos = new ArrayList<>();
       // List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(name).
        // finished().orderByProcessInstanceEndTime().desc().list();//已完结的
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().
                processInstanceBusinessKey(name).orderByProcessInstanceStartTime().desc().list();

        for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
            leaveInfo leaveInfo = new leaveInfo();
            Date startTime = historicProcessInstance.getStartTime();
            Date endTime = historicProcessInstance.getEndTime();
            List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(historicProcessInstance.getId())
                    .list();
            for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
                String leaveName = historicVariableInstance.getVariableName();
                Object value = historicVariableInstance.getValue();
                if ("reason".equals(leaveName)) {
                    leaveInfo.setReason((String) value);
                } else if ("days".equals(leaveName)) {
                    leaveInfo.setDays(Integer.parseInt(value.toString()));
                } else if ("approved".equals(leaveName)) {
                    leaveInfo.setStatus((Boolean) value);
                } else if ("name".equals(leaveName)) {
                    leaveInfo.setName((String) value);
                }
            }
            leaveInfo.setStartTime(startTime);
            leaveInfo.setEndTime(endTime);
            leaveInfos.add(leaveInfo);
        }
        return ResponseBean.ok("ok", leaveInfos);
    }

    /**
     * 操作审批
     * @param leaveApproveVo
     * @return
     */
    public ResponseBean askForLeaveHandler(leaveApproveVo leaveApproveVo) {
        try {
            boolean approved = leaveApproveVo.getApproved();
            Map<String, Object> leaves = new HashMap<String, Object>();
            Task task = taskService.createTaskQuery().taskId(leaveApproveVo.getTaskId()).singleResult();
            leaves.put("approved", approved);
            leaves.put("employee", leaveApproveVo.getName());
            taskService.complete(task.getId(), leaves);
            if (approved) {
                //如果是同意，还需要继续走一步
                Task t = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                if(t==null){
                    return ResponseBean.ok("已完结");
                }
                taskService.complete(t.getId());
            }
            return ResponseBean.ok("操作成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseBean.error("操作失败");
    }
    @Autowired
    RepositoryService repositoryService;

    //串行任务【主动撤回】
    public void proBack(String instanceId,String canBackTaskId,String userId){
        /*
        1、先判断是否可撤回：上一个节点是当前用户审核的,并且下一节点审批人还未处理时,可撤回到上一节点;
        2、撤回步骤：保存并删除BpmnModel当前流程节点输出流;
        3、把当前流程节点输出流方向改成上一个节点;
        4、撤回任务完成后,再次恢复当前节点的原有输出流;
        */
        Map<String,Object> result = canBack(instanceId, canBackTaskId, userId);
        Boolean canBack=(Boolean)result.get("canBack");
        HistoricTaskInstance currentTask=(HistoricTaskInstance)result.get("currentTask");
        if(!canBack){
            System.out.println("流程："+instanceId+",任务："+canBackTaskId+"已经处理，不可撤回");
            return;
        }
        //流程撤回
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceId).singleResult();//历史的【流程】实例
        BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());//ProcessDefinitionId:leaveprocessmore:11:20f8063c-9641-11ee-b051-00ffb3ed8746
        //上一流程
        HistoricTaskInstance canBackTask = historyService.createHistoricTaskInstanceQuery().taskId(canBackTaskId).taskAssignee(userId).singleResult();
        if(canBackTask==null){
            System.out.println("未找到要回退的任务");
            return;
        }
        FlowNode canBackNode = (FlowNode)bpmnModel.getFlowElement(canBackTask.getTaskDefinitionKey());
        FlowNode currentNode=(FlowNode)bpmnModel.getFlowElement(currentTask.getTaskDefinitionKey());

        //记录原始输出流程
        List<SequenceFlow> sourceOuts =new ArrayList<>();
        sourceOuts.addAll(currentNode.getOutgoingFlows());

        //清理活动方向
        currentNode.getOutgoingFlows().clear();

        //创建新流
        List<SequenceFlow> newFlows=new ArrayList<>();
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentNode);//bossApproval老总
        newSequenceFlow.setTargetFlowElement(canBackNode);//上一环节targetRef=supApproval(主管)
        newFlows.add(newSequenceFlow);
        currentNode.setOutgoingFlows(newFlows);

        //记录备注信息
        Authentication.setAuthenticatedUserId(userId);
        taskService.addComment(currentTask.getId(), currentTask.getProcessInstanceId(), "撤回");

        //完成撤回任务
        taskService.complete(currentTask.getId());
        //恢复原方向
        currentNode.setOutgoingFlows(sourceOuts);

        //删除当前任务历史记录，根据业务需要是否删除
        historyService.deleteHistoricTaskInstance(currentTask.getId());
    }

    /**
     * 判断流程是否能够撤回
     * 上一个节点是当前用户审核，并且下一节点审批人还未处理时，可撤回到上一节点
     * @return
     */
    public Map<String,Object> canBack(String instanceId,String canBackTaskId,String userId){
        boolean canBack=false;
        Map<String,Object> map=new HashMap<>();
        //查看流程历史节点
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(instanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();
        boolean hasUserTask=false;
        HistoricTaskInstance currentTask=null;
        for(HistoricTaskInstance task:list) {
            if(task.getId().equalsIgnoreCase(canBackTaskId)
                    &&task.getAssignee().equalsIgnoreCase(userId)){
                //找到了处理人处理的任务，查看下一个任务是否已经处理
                hasUserTask=true;
                continue;
            }
            if(hasUserTask){
                //上一个任务是当前人处理的，查看当前任务是否已经被处理
                hasUserTask=false;
                if(null==task.getEndTime()){
                    canBack=true;
                    currentTask=task;
                    break;
                }
            }
        }
        if(canBack){
            System.out.println("未处理的流程可撤回，任务ID:"+currentTask.getId()+",任务接收人:"+currentTask.getAssignee());
        }
        map.put("canBack",canBack);
        map.put("currentTask",currentTask);
        //方法返回值为：是否可回退、当前任务
        return map;
    }

}
