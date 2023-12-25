package com.tt.flowable.service;


import com.tt.flowable.pojo.ResponseBean;
import com.tt.flowable.pojo.VacationApproveVo;
import com.tt.flowable.pojo.VacationInfo;
import com.tt.flowable.pojo.VacationRequestVo;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class VacationService {

    //处理正在运行的流程
    @Autowired
    RuntimeService runtimeService;

    //对流程实例的各个节点的审批处理
    @Autowired
    TaskService taskService;

    //在用户发起审批后，会生成流程实例。historyService为处理流程实例的api，但是其中包括了已完成的和未完成的流程实例；
    @Autowired
    HistoryService historyService;

    //提供了在编辑和发布审批流程的api。主要是模型管理和流程定义的业务api
    @Autowired
    RepositoryService repositiryService;
    //主要执行自定名命令
    @Autowired
    ManagementService managementService;
    //身份信息获取、保存
    @Autowired
    IdentityService identityService;

    /**
     * 申请请假
     * @param vacationRequestVO
     * @return
     */
    @Transactional
    public ResponseBean askForLeave(VacationRequestVo vacationRequestVO) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", vacationRequestVO.getName());
        variables.put("days", vacationRequestVO.getDays());
        variables.put("reason", vacationRequestVO.getReason());
        try {
            //指定业务流程
            runtimeService.startProcessInstanceByKey("vacationRequest", vacationRequestVO.getName(), variables);
            return ResponseBean.ok("已提交请假申请");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseBean.error("提交申请失败");
    }

    /**
     * 审批列表
     * @param identity
     * @return
     */
    public ResponseBean leaveList(String identity) {
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(identity).list();
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
     * 操作审批
     * @param vacationVO
     * @return
     */
    public ResponseBean askForLeaveHandler(VacationApproveVo vacationVO) {
        try {
            boolean approved = vacationVO.getApprove();
            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("approved", approved);
            variables.put("employee", vacationVO.getName());
            Task task = taskService.createTaskQuery().taskId(vacationVO.getTaskId()).singleResult();
            taskService.complete(task.getId(), variables);
            if (approved) {
                //如果是同意，还需要继续走一步
                Task t = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                taskService.complete(t.getId());
            }
            return ResponseBean.ok("操作成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseBean.error("操作失败");
    }

    /**
     * 请假列表
     * @param name
     * @return
     */
    public ResponseBean searchResult(String name) {
        List<VacationInfo> vacationInfos = new ArrayList<>();
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(name).finished().orderByProcessInstanceEndTime().desc().list();
        for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
            VacationInfo vacationInfo = new VacationInfo();
            Date startTime = historicProcessInstance.getStartTime();
            Date endTime = historicProcessInstance.getEndTime();
            List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(historicProcessInstance.getId())
                    .list();
            for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
                String variableName = historicVariableInstance.getVariableName();
                Object value = historicVariableInstance.getValue();
                if ("reason".equals(variableName)) {
                    vacationInfo.setReason((String) value);
                } else if ("days".equals(variableName)) {
                    vacationInfo.setDays(Integer.parseInt(value.toString()));
                } else if ("approved".equals(variableName)) {
                    vacationInfo.setStatus((Boolean) value);
                } else if ("name".equals(variableName)) {
                    vacationInfo.setName((String) value);
                }
            }
            vacationInfo.setStartTime(startTime);
            vacationInfo.setEndTime(endTime);
            vacationInfos.add(vacationInfo);
        }
        return ResponseBean.ok("ok", vacationInfos);
    }
}
