package com.tt.flowable.pojo.leave;

import lombok.Data;

import java.util.Date;
@Data
public class leaveInfo {

    private String name;
    private Integer days;
    private String reason;
    private Boolean status;
    private Date startTime;
    private Date endTime;
}
