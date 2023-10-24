package com.zyq.chirp.adviceserver.exception;

import com.zyq.chirp.adviceclient.dto.SiteMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendFailedException extends RuntimeException {
    private Integer code;
    private Collection<? extends SiteMessage> siteMessages;

    public SendFailedException(String message, Throwable cause, Integer code, Collection<? extends SiteMessage> siteMessages) {
        super(message, cause);
        this.code = code;
        this.siteMessages = siteMessages;
    }
}
