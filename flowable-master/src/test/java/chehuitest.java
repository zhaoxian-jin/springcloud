import liquibase.util.ObjectUtil;
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
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class chehuitest {
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    RepositoryService repositoryService;
    /**
     * 流程撤回：
     * 当前用户审批通过后(流程到下一节点后)，又想撤回时。撤回到上一节点；
     */
    @Test
    public void proBack(){
        //1、判断是否可撤回？ 上一个节点是当前用户审核的，并且下一节点审批人还未处理时，可撤回到上一节点；
        //2、如何撤回？动态修改BpmnModel当前流程节点输出流。
        //把当前流程节点输出流方向改成上一个节点，原有输出流删除；
        //审批通过后，再次恢复当前节点的原有输出流；
        //根据业务需求，判断是否删除上次审批的历史记录，不需删除时，不用处理；要求删除时，需删除撤回前的历史记录；
        String instanceId="86768e8c-98c5-11ee-bc77-00ffb3ed8746";
        String canBackTaskId="b4b62819-98c5-11ee-bc77-00ffb3ed8746";
        String userId="老赵";

        //Tuple result = canBack(instanceId, canBackTaskId, userId);
        Map<String,Object> result = canBack(instanceId, canBackTaskId, userId);


        boolean canBack=(boolean)result.get("canBack");
        HistoricTaskInstance currentTask=(HistoricTaskInstance)result.get("currentTask");
        if(!canBack){
            System.out.println("流程：{}-任务：{}，已经处理，不可撤回"+instanceId+"、"+canBackTaskId);
            return;
        }


        //流程撤回
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
        HistoricTaskInstance canBackTask = historyService.createHistoricTaskInstanceQuery().taskId(canBackTaskId).taskAssignee(userId).singleResult();

        if(canBack){
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
        newSequenceFlow.setSourceFlowElement(currentNode);
        newSequenceFlow.setTargetFlowElement(canBackNode);
        newFlows.add(newSequenceFlow);
        currentNode.setOutgoingFlows(newFlows);

        //记录备注信息
        Authentication.setAuthenticatedUserId(userId);
        taskService.addComment(currentTask.getId(), currentTask.getProcessInstanceId(), "撤回");

        //完成任务
        taskService.complete(currentTask.getId());
        //恢复原方向
        currentNode.setOutgoingFlows(sourceOuts);

        //删除当前任务历史记录
        historyService.deleteHistoricTaskInstance(currentTask.getId());
    }

    /**
     * 判断流程是否能够撤回
     * 上一个节点是当前用户审核的，并且下一节点审批人还未处理时，可撤回到上一节点；
     * @return
     */
    @Test
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
            System.out.println("未处理的流程可撤回，任务ID:{},任务接收人:{}"+currentTask.getId()+","+currentTask.getAssignee());
        }
        map.put("canBack",canBack);
        map.put("currentTask",currentTask);
        //方法返回多个值，是否可回退，当前任务
        return map;
    }

}
