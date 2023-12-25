package com.tt.flowable.pojo.leave;

import lombok.Data;

@Data
public class leaveApproveVo {
    private String name;
    private Boolean approved;
    private String taskId;
}
