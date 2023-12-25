package com.tt.flowable.pojo;

import lombok.Data;

@Data
public class VacationRequestVo {
    private String name;

    private Integer days;

    private String reason;
}
