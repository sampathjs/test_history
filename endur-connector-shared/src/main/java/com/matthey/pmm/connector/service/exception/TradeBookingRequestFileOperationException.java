package com.matthey.pmm.connector.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Class indicating a trade booking request can not be handled as there is already request
 * for the same filename in the input folder.
 * @author jwaechter
 * @version 1.0
 */

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "File Operation Error While Processing Request")
public class TradeBookingRequestFileOperationException extends RuntimeException {
	public TradeBookingRequestFileOperationException(String reason) {
		super(reason);
	}

}
