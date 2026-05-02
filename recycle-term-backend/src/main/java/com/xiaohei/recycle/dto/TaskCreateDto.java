package com.xiaohei.recycle.dto;

import lombok.Data;

@Data
public class TaskCreateDto {
    private String phoneNumber;
    private String productId;
    private String userName;
    private String userAddress;
    private String area;
    private String engineerName;
    private String engineerPhone;
    private String detailDesc;
    private String terminals;
    private Integer expectedCount;
    private Integer fttrCount;
    private String accessRoom;
    private String category;
    private String devDept;
    private String devPerson;
    private Boolean needVisit;
    private String remark;
}
