package com.tt.flowable.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class VacationInfo {
    private String name;

    private Date startTime;

    private Date endTime;

    private String reason;

    private Integer days;

    private Boolean status;
}
