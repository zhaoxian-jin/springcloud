package com.tt.flowable.lister;


import org.flowable.engine.RuntimeService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 委派任务
 * 监听器类型：任务监听器
 * 主管审批动态分配候选人
 */
public class MyUserTaskListener implements TaskListener {


    @Override
    public void notify(DelegateTask delegateTask) {

        delegateTask.addCandidateUser("丽丽");
        delegateTask.addCandidateUser("呦呦");
        System.out.println("任务监听器:"+delegateTask.getId()+";"+delegateTask.getName());
    }
}
