package com.tt.flowable.controller;


import com.tt.flowable.pojo.ResponseBean;
import com.tt.flowable.pojo.leave.leaveApproveVo;
import com.tt.flowable.pojo.leave.leaveRequestVo;
import com.tt.flowable.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RequestMapping("leave")
@RestController
public class leaveController {

    @Autowired
    LeaveService leaveService;


    /**
     * 请假新增页面
     * @return
     */
    @GetMapping("/add")
    public ModelAndView add(){
        return new ModelAndView("leave");
    }

    /**
     * 请假条审批列表
     * @return
     */
    @GetMapping("/aList")
    public ModelAndView aList(){

        return new ModelAndView("list");
    }

    /**
     * 请假条查询列表(主管审批后可查到)
     * @return
     */
    @GetMapping("/sList")
    public ModelAndView sList(){
        return new ModelAndView("search");
    }


    /**
     * 保存新增请假申请
     * @param leaveRequestVo
     * @return
     */
    @PostMapping("/saveleave")
    public ResponseBean saveLeave(@RequestBody leaveRequestVo leaveRequestVo){
        return leaveService.saveLeave(leaveRequestVo);
    }

    /**
     * 主管获取待审批列表
     * @param identity
     * @return
     */
    @GetMapping("/list")
    public ResponseBean leaveList(String identity) {
        return leaveService.queryLeadTask(identity);
    }

    /**
     * 请假查询
     * @param name
     * @return
     */
    @GetMapping("/search")
    public ResponseBean searchResult(String name) {
        return leaveService.searchResult(name);
    }

    /**
     * 拒绝或同意请假
     * @param leaveApproveVo
     * @return
     */
    @PostMapping("/handler")
    public ResponseBean askForLeaveHandler(@RequestBody leaveApproveVo leaveApproveVo) {
        return leaveService.askForLeaveHandler(leaveApproveVo);
    }

    /**
     * 撤回
     * @param instanceId 流程id ACT_HI_PROCINST.PROC_INST_ID_
     * @param canBackTaskId 想撤回到节点的id  act_hi_taskinst.ID_
     * @param userId 撤回人
     */
    @GetMapping("/proBack")
    public void proBack(String instanceId,String canBackTaskId,String userId) {
        leaveService.proBack(instanceId,canBackTaskId,userId);
    }



    // 删除流程 :(data:删除原因)
   /* @DeleteMapping("/processInstanceId")
    public JsonResponseEntity<Object> deleteProcessInstanceId(@RequestParam("processInstanceId")String processInstanceId,@RequestParam("data")String data){
        runtimeService.deleteProcessInstance(processInstanceId,data);//删除流程
        return JsonResponseEntity.newJsonResult(ResultStatus.SUCCESS,null);
    }*/

}
