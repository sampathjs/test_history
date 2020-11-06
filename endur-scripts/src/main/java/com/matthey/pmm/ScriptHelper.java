package com.matthey.pmm;

import com.olf.embedded.application.Context;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@SuppressWarnings("unused")
public class ScriptHelper {
    
    public static LocalDate getTradingDate(Context context) {
        return fromDate(context.getTradingDate());
    }
    
    public static LocalDate fromDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
