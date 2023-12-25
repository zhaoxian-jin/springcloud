import com.tt.flowable.FlowerApplication;
import com.tt.flowable.pojo.ResponseBean;
import com.tt.flowable.pojo.leave.leaveInfo;
import org.apache.commons.collections.CollectionUtils;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest(classes = FlowerApplication.class)
public class flowtest {
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    RepositoryService repositoryService;


    /**
     * 查询用户
     */
    @Test
    public void queryUser(){

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();//通过IdentityService完成相关的用户和组的管理
        IdentityService identityService = processEngine.getIdentityService();
       //使用userId
        User u=identityService.createUserQuery().userId("用户01").singleResult();
        List<User> list=identityService.createUserQuery().list();
        for(User user:list){
            System.out.println(user.getId()+";"+user.getFirstName());
        }

    }
    /**
     * 新增用户act_id_user
     */
    @Test
    public void createUser(){
             ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();//通过IdentityService完成相关的用户和组的管理
             IdentityService identityService = processEngine.getIdentityService();
             User user = identityService.newUser("0001");
             user.setFirstName("QQ");
             //user.setLastName("01");
             user.setEmail("6666666@qq.com");
             identityService.saveUser(user);

    }

    /**
     * 创建用户组 ACT_ID_GROUP
     */
    @Test
    public void createGroup(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        IdentityService identityService = processEngine.getIdentityService(); // 创建Group对象并指定相关的信息
        Group group = identityService.newGroup("G1");
        group.setName("研发部");
        group.setType("T1"); // 创建Group对应的表结构数据
        identityService.saveGroup(group);
    }

    /**
     * 将用户分配给对应的Group
     * ACT_ID_MEMBERSHIP
     * 用户和组是一个多对多的关联关联
     */
    @Test
    public void userGroup(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        IdentityService identityService = processEngine.getIdentityService(); // 根据组的编号找到对应的Group对象
        Group group = identityService.createGroupQuery().groupId("G1").singleResult();//查询用户组
        List<User> list = identityService.createUserQuery().list();
        for (User user : list) { // 将用户分配给对应的组
            if("用".equals(user.getFirstName())){
                identityService.createMembership(user.getId(),group.getId());
            }
         }
    }

    /**
     * 状态判断
     */
    @Test
    void testStatus(){
        boolean status=taskService.createTaskQuery().taskId("taskId").singleResult().isSuspended();//Suspended=2中止，=1正常
        if (status){//是否挂起（中止）

        }
    }

    /**
     * 终止删除流程、挂起(中止)
     */
   @Test
    void deletetask(){

       Map<String,Object> map=new HashMap<>();
       String processInstanceId="";
       List<Task> procInsId = taskService.createNativeTaskQuery().
               sql("select * from ACT_HI_TASKINST where PROC_INST_ID_ = #{procInsId} ORDER BY START_TIME_ desc")
               .parameter("procInsId", processInstanceId).list();
       if (!procInsId.get(1).getAssignee().equals("revokeVo.getUserCode()")){
           map.put("result",false);
           map.put("message","当前流程已不在下级节点!");
       }
       //作废-删除流程，删除流程历史记录
       runtimeService.deleteProcessInstance(processInstanceId,"不请了删除流程实例");//删除任务,无processInstanceId任务时会报错。
       historyService.deleteHistoricProcessInstance(processInstanceId);

       runtimeService.suspendProcessInstanceById(processInstanceId);//挂起
       runtimeService.activateProcessInstanceById(processInstanceId);;//激活

       //挂起与激活(二)
       ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
       // 获取流程引擎对象
       RepositoryService repositoryService = processEngine.getRepositoryService();
       ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
               .processDefinitionId("holiday-process:1:4")
               .singleResult();
       // 查看流程定义 是否被暂停
       boolean isSuspended = processDefinition.isSuspended();
       if(isSuspended){
           // 激活操作
           repositoryService.activateProcessDefinitionById("holiday-process:1:4",true,null);//过程定义processDefinitionId
       }else{
           // 暂停操作
           repositoryService.suspendProcessDefinitionById("holiday-process:1:4",true,null);
       }


   }


    /**
     * 获取下一节点信息(bpmn20.xml中的节点信息)
     * @param taskId
     */
    void getNextWork(String taskId){

        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult(); // 获取当前任务
        List<SequenceFlow> outgoingFlows =((FlowNode)(repositoryService.getBpmnModel(currentTask.getProcessDefinitionId())
                .getMainProcess()).getFlowElement(currentTask.getTaskDefinitionKey())).
                getOutgoingFlows();
        for (SequenceFlow outgoingFlow : outgoingFlows) {
            String nextTaskId = outgoingFlow.getTargetRef();//节点的id，非任务
            String nextTaskName = repositoryService.getBpmnModel(currentTask.getProcessDefinitionId()).getMainProcess().getFlowElement(nextTaskId).getName();
            System.out.println("Next task ID: " + nextTaskId + ", Next task name: " + nextTaskName);
           // taskService.setAssignee(nextTaskId,"三三");
            // 下一个审批节点
            FlowElement targetFlow = outgoingFlow.getTargetFlowElement();
            //如果是用户任务则获取待办人或组
            if (targetFlow instanceof UserTask)
            {
                // 如果下个审批节点为userTask
                UserTask userTask = (UserTask) targetFlow;
                userTask.setAssignee("主管审批");
            }
        }
    }
    // 删除流程 :(data:删除原因)
    @Test
    public void deleteProcessInstanceId(){
        try{
            String processInstanceId="a4b311ec-9e13-11ee-ac38-00ffb3ed8746";
            //需要添加这段代码，否则审批意见表ACT_HI_COMMENT审批的userid是空的
           // Authentication.setAuthenticatedUserId("老总");
            //审核意见
            //taskService.addComment("2122f113-9d70-11ee-bd37-00ffb3ed8746","5145648b-9b13-11ee-8409-00ffb3ed8746","批准");

            runtimeService.deleteProcessInstance(processInstanceId,"删除原因");//删除运行中流程
            historyService.deleteHistoricProcessInstance(processInstanceId);//删除历史流程
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 提交申请
     */
    @Test
    public void sub() {
        // 员工提交请假申请
        Map<String, Object> map = new HashMap<>();
        map.put("days", 4);
        map.put("name", "QQ");
        map.put("reason","调价");
        //map.put("userIds", "laozhao,laowang,lisi");//1、赋值审批分配人key
       try{
           //启动流程
           ProcessInstance a1=runtimeService.startProcessInstanceByKey("leaveprocessmore",map.get("name").toString(),map);
           Task task = taskService.createTaskQuery().processInstanceId(a1.getId()).singleResult();
           /* Map<String,Object> m=new HashMap<>();
            m.put("userIds", "老王,老赵");//2、赋值审批分配人key同1
            taskService.setVariables(task.getId(),m);*/
           //处理任务
           taskService.complete(task.getId());
       }catch (Exception e){
           e.printStackTrace();
       }
    }

    /**
     * 候选人为多个时需要先认领任务在处理，认领后其他人不能再操作此任务
     */
    @Test
    void taskClaim(){
        //认领
        List<Task> list = taskService.createTaskQuery().taskCandidateUser("QQ").list();
        for (Task task : list) {
            taskService.claim(task.getId(), "QQ");
        }
        //处理审批任务
        list = taskService.createTaskQuery().taskAssignee("QQ").list();
        for (Task task : list) {
            Map<String, Object> leaves = new HashMap<>();
            leaves.put("approved", true);
             Authentication.setAuthenticatedUserId("QQ");
            //审核意见
            taskService.addComment("54981962-9e15-11ee-8123-00ffb3ed8746","54122175-9e15-11ee-8123-00ffb3ed8746","主管批准");
            taskService.complete(task.getId(), leaves);
        }
    }

    /**
     * 认领任务
     */
    @Test
    void taskClaimUser() {
        //认领
        List<Task> list = taskService.createTaskQuery().taskCandidateUser("老赵").list();
        for (Task task : list) {
            taskService.claim(task.getId(), "老赵");
        }
    }
    /**
     * 放弃、取消已认领任务
     */
    @Test
    public void taskUnClaim() {
        List<Task> list = taskService.createTaskQuery().taskAssignee("老赵").list();
        //根据任务id查询
        //Task task= taskService.createTaskQuery().taskId("").singleResult();
        for (Task task : list) {
            taskService.unclaim(task.getId());//放弃认领
        }
    }

    /**
     * 更换认领人
     */
    @Test
    public void taskClaimUserChange() {
        //根据任务id查询
        Task task= taskService.createTaskQuery().taskId("").singleResult();
        taskService.setAssignee(task.getId(), "老王");
    }

    /**
     * 附件应用-保存
     * attachmenttype附件类型
     * attachmentdescription附件说明
     */
    @Test
    void saveAttavhment(){
        Attachment attachment=taskService.createAttachment("bpmn20.xml","0b0ffc62-9e16-11ee-8e0c-00ffb3ed8746",
                "54122175-9e15-11ee-8123-00ffb3ed8746","流程文件","记录流程文件",
                "E:\\springcloud\\flowable-master\\target\\classes\\processes\\请假more.bpmn20.xml");
        taskService.saveAttachment(attachment);
    }

    /**
     * 附件应用-查询
     */
    @Test
    void taskAttavhment(){
        Attachment attachment=taskService.getAttachment("a3a30f3a-9e18-11ee-aef3-00ffb3ed8746");//获取附件对象
        InputStream inputStream= taskService.getAttachmentContent("a3a30f3a-9e18-11ee-aef3-00ffb3ed8746");
        List<Attachment> attachmentList=taskService.getProcessInstanceAttachments("54122175-9e15-11ee-8123-00ffb3ed8746");
        for(Attachment att:attachmentList){
            System.out.println("url:"+att.getUrl()+";description:"+att.getDescription());
        }
    }
    /**
     * 老板审批(批量)
     */
    @Test
    void approveMore(){
        //查询所属任务
        List<Task> list = taskService.createTaskQuery().taskCandidateGroup("boss").list();
        Map<String,Object> leaves=new HashMap<>();
        leaves.put("bossapproved", true);
        for (Task task : list) {
            taskService.complete(task.getId(), leaves);
            if (true) {
                //如果是同意，还需要继续走一步
                Task t = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                if (t == null) {
                    System.out.println("工作流已完结！！！！");
                    break;
                }
                taskService.complete(t.getId());

            }
        }
    }
    /**
     * 老板审批（单任务）、记录审批意见
     */
    @Test
    void approveSingle(){
        //查询所属任务
        Task task = taskService.createTaskQuery().processInstanceId("5145648b-9b13-11ee-8409-00ffb3ed8746").singleResult();
        Map<String,Object> leaves=new HashMap<>();
        leaves.put("bossapproved", true);
        //需要添加这段代码，否则审批意见表ACT_HI_COMMENT审批的userid是空的
        Authentication.setAuthenticatedUserId("老总");
        //审核意见
        taskService.addComment("2122f113-9d70-11ee-bd37-00ffb3ed8746","5145648b-9b13-11ee-8409-00ffb3ed8746","批准");

        taskService.complete(task.getId(), leaves);
            if (true) {
                //如果是同意，还需要继续走一步
                Task t = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                if (t == null) {
                    System.out.println("工作流已完结！！！！");
                    return;
                }
                taskService.complete(t.getId());
            }
    }

    /**
     *查询审批意见
     */
    @Test
    void queryTaskComment(){
        List<Comment> taskComments = taskService.getTaskComments("2122f113-9d70-11ee-bd37-00ffb3ed8746");
        for (Comment taskComment : taskComments) {
            System.out.println("审批人：" + taskComment.getUserId());
            System.out.println("审批意见：" + taskComment.getFullMessage());
        }
    }


    /**
     * 设置候选人
     */
    @Test
    void setCandidate(){
        Map<String, Object> userIds = new HashMap<>();
        userIds.put("userIds", "老赵,老王,老李");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("leaveprocessmore",userIds);
        System.out.println("id:"+pi.getId()+";activityId:"+pi.getActivityId());

    }

    /**
     * 追加候选人
     */
    @Test
    void addCandidate(){
        List<Task> list = taskService.createTaskQuery().taskCandidateUser("老赵").list();
        List<Task> listt = taskService.createTaskQuery().taskId("taskId").list();
        for (Task task : list)
            taskService.addCandidateUser(task.getId(),"老李");
    }
    /**
     * 删除候选人
     */
    @Test
    void delCandidate() {
        List<Task> list = taskService.createTaskQuery().taskCandidateUser("老赵").list();
        for (Task task : list) {
            taskService.deleteCandidateUser(task.getId(), "老李");
        }
    }

    /**
     * 查询流程历史参与人记录
     */
    @Test
    void queryHistoryUser() {
        List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery().list();
        for (HistoricProcessInstance historicProcessInstance : list) {
            List<HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForProcessInstance(historicProcessInstance.getId());
            for (HistoricIdentityLink link : links)
                System.out.println("userId:,taskId:,type:,processInstanceId:"+ link.getUserId()+";"+
                        link.getTaskId()+";"+ link.getType()+";"+ link.getProcessInstanceId());
        }
    }

    /**
     * 查询一个 Task的历史候选人与处理人
     */
    void queryHistoryTaskUser() {
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().list();
        for (HistoricTaskInstance historicTaskInstance : list) {
            List<HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForTask(historicTaskInstance.getId());
            for (HistoricIdentityLink link : links) {
                System.out.println("userId:,taskId:,type:,processInstanceId:" + link.getUserId() + ";" +
                        link.getTaskId() + ";" + link.getType() + ";" + link.getProcessInstanceId());
            }
        }
    }
    /**
     * 查询分配人或候选用户人的任务
     */
    @Test
    void querytask(){

        //候选用户及多个候选用户时
        List<Task> task = taskService.createTaskQuery().taskCandidateUser("李思思").list();
        //分配人
        List<Task> tasks = taskService.createTaskQuery().taskAssignee("李思思").list();
        //候选用户或分配人
        List<Task> task2 =taskService.createTaskQuery().taskCandidateOrAssigned("lisi").list();

        System.out.println(task.size()+"=="+tasks.size());
    }

    /**
     * 获取历史流程
     */
    @Test
    void queryHistory() {
        List<ProcessInstance> processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("leave").orderByStartTime().desc().list();
        if (CollectionUtils.isEmpty(processInstance)) {
            System.out.println("------------------------------------------");
        }
        // 获取最近的一个流程
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.get(0).getId())
                // 只查询已经完成的活动
                .finished()
                // 按照结束时间排序
                .orderByHistoricActivityInstanceEndTime().desc()
                .list();
        List<String> collect = activities.stream().map(a -> "活动名称:" + a.getActivityName() + ";活动执行时间:" + a.getDurationInMillis() + "毫秒").collect(Collectors.toList());
        for (String s : collect) {
            System.out.println(s);
        }
    }

    /**
     * 请假列表(已完结、未完结)
     */
    @Test
    public void searchResult() {
        String name = "李思思";
        List<leaveInfo> leaveInfos = new ArrayList<>();
        //已完结的
       // List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(name).finished().orderByProcessInstanceEndTime().desc().list();
        //未完结的
        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().
                processInstanceBusinessKey(name).unfinished().orderByProcessInstanceStartTime().list();

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
        System.out.println(leaveInfos.size());
    }

}
