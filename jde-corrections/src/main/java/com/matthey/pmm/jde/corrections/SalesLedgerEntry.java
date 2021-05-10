package com.matthey.pmm.jde.corrections;

import com.olf.openrisk.io.InvalidArgumentException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.immutables.value.Value.Derived;
import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
public interface SalesLedgerEntry extends LedgerEntry {
    
    static LocalDate getValueDate(String payload) {
        Pattern pattern = Pattern.compile(
                "<ns2:Item>.*?<ns2:Category>GeneralLedger</ns2:Category>.*?<ns2:ValueDate>(.+?)</ns2:ValueDate>.*?</ns2:Item>",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(payload);
        if (matcher.find()) {
            return LocalDate.parse(matcher.group(1));
        }
        throw new InvalidArgumentException("the payload doesn't include value date for SL:" +
                                           System.lineSeparator() +
                                           payload);
    }
    
    int endurDocNum();
    
    @Derived
    default int documentReference() {
        Pattern pattern = Pattern.compile(
                "<ns2:Header>.*?<ns2:DocumentReference>(\\d+?)</ns2:DocumentReference>.*?</ns2:Header>",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(payload());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new InvalidArgumentException("the payload doesn't include reference key one:" +
                                           System.lineSeparator() +
                                           payload());
    }
    
    @Derived
    default int docNum() {
        Pattern pattern = Pattern.compile(
                "<ns2:Header>.*?<ns2:ReferenceKeyOne>(\\d+?)</ns2:ReferenceKeyOne>.*?</ns2:Header>",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(payload());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
    
    @Derived
    default int referenceNum() {
        Pattern pattern = Pattern.compile(
                "<ns2:Header>.*?<ns2:DocumentReference>(\\d+?)</ns2:DocumentReference>.*?</ns2:Header>",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(payload());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
    
    Optional<Boolean> isForCurrentMonth();
}
