package com.xiaohei.recycle.service;

import com.xiaohei.recycle.dto.WorkOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WorkOrderQuickParseService {

    private static final Pattern WORK_ORDER_NO = Pattern.compile("(?<!\\d)(\\d{16,17})(?!\\d)");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1\\d{10})(?!\\d)");
    private static final Pattern ONU_SN = Pattern.compile("(?i)(?:[O0]NU\\s*SN号?|SN号?)[:：\\s]*([A-Z0-9-]{6,32})");
    private static final Pattern PRODUCT_ID = Pattern.compile("(?:产品(?:号|ID)?|宽带账号|业务号码(?:为)?)[:：\\s]*([A-Za-z0-9_-]{5,32})");
    private static final Pattern USER_NAME = Pattern.compile("(?:用户(?:姓名)?|客户(?:姓名)?|姓名)[:：]\\s*([\\u4e00-\\u9fa5A-Za-z]{2,20})");
    private static final Pattern ADDRESS = Pattern.compile("(?:地址|装机地址|用户地址)[:：\\s]*([^，。\\n]+)");
    private static final Pattern TYPE = Pattern.compile("(?:施工动作为|工单类型|类型|业务类型)[:：\\s]*([^，,。；;\\n]+)");
    private static final Pattern CVLAN = Pattern.compile("(?i)CVLAN[:：\\s]*([0-9]{1,5})");
    private static final Pattern SVLAN = Pattern.compile("(?i)SVLAN[:：\\s]*([0-9]{1,5})");
    private static final Pattern SPLITTER = Pattern.compile("(?:分光器|分纤箱|光分路器)[:：\\s]*([^\\n，,；;]+)");
    private static final Pattern SPLITTER_PORT = Pattern.compile("(?:分光器端口|端口)[:：\\s]*([^\\n，,；;]+)");
    private static final Pattern FULL_APPOINTMENT = Pattern.compile("客户预约时间为[:：]\\s*(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");
    private static final Pattern SHORT_APPOINTMENT = Pattern.compile("客户预约时间为[:：]\\s*(\\d{1,2})月(\\d{1,2})日\\s*(\\d{1,2})时(\\d{1,2})分");
    private static final DateTimeFormatter FULL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WorkOrderDto parse(String text) {
        if (!StringUtils.hasText(text)) {
            throw new RuntimeException("解析内容不能为空");
        }
        WorkOrderDto dto = new WorkOrderDto();
        dto.setRawSource(text);
        dto.setWorkOrderNo(find(WORK_ORDER_NO, text));
        dto.setContactPhone(find(PHONE, text));
        dto.setOnuSn(find(ONU_SN, text));
        dto.setProductId(find(PRODUCT_ID, text));
        dto.setUserName(find(USER_NAME, text));
        dto.setAddress(findLine(ADDRESS, text));
        dto.setWorkOrderType(resolveType(text));
        dto.setStatus(resolveStatus(text));
        dto.setAppointedAt(resolveAppointment(text));
        dto.setCvlan(find(CVLAN, text));
        dto.setSvlan(find(SVLAN, text));
        dto.setSplitter(find(SPLITTER, text));
        dto.setSplitterPort(find(SPLITTER_PORT, text));
        return dto;
    }

    private String find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String findLine(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String resolveType(String text) {
        String type = find(TYPE, text);
        if (StringUtils.hasText(type)) {
            return type.trim();
        }
        if (text.contains("修障") || text.contains("报障") || text.contains("故障现象")) {
            return "故障";
        }
        return null;
    }

    private String resolveStatus(String text) {
        if (text.contains("已履约")) {
            return "FULFILLING";
        }
        if (text.contains("待接单")) {
            return "ACCEPTED";
        }
        return null;
    }

    private LocalDateTime resolveAppointment(String text) {
        Matcher full = FULL_APPOINTMENT.matcher(text);
        if (full.find()) {
            try {
                return LocalDateTime.parse(full.group(1), FULL_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        Matcher shortDate = SHORT_APPOINTMENT.matcher(text);
        if (shortDate.find()) {
            int year = Year.now().getValue();
            int month = Integer.parseInt(shortDate.group(1));
            int day = Integer.parseInt(shortDate.group(2));
            int hour = Integer.parseInt(shortDate.group(3));
            int minute = Integer.parseInt(shortDate.group(4));
            return LocalDateTime.of(year, month, day, hour, minute);
        }
        return null;
    }
}
