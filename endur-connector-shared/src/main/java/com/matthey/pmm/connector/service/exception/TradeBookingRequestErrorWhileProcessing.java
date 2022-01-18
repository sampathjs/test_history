package com.matthey.pmm.connector.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Class indicating a trade booking request can not be handled as there is already request
 * for the same filename in the input folder.
 * @author jwaechter
 * @version 1.0
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Error while processing trade booking request")
public class TradeBookingRequestErrorWhileProcessing extends RuntimeException {
	public TradeBookingRequestErrorWhileProcessing(String reason) {
		super(reason);
	}

}
