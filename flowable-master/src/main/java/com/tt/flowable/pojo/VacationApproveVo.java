package com.tt.flowable.pojo;

import lombok.Data;

@Data
public class VacationApproveVo {

    private String taskId;

    private Boolean approve;

    private String name;
}
