package com.matthey.pmm.connector.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Class indicating a trade booking request can not be handled as there is already request
 * for the same filename in the input folder.
 * @author jwaechter
 * @version 1.0
 */

public class TradeBookingRequestErrorWhileProcessing extends ResponseStatusException {
	public TradeBookingRequestErrorWhileProcessing(String reason) {
		super(HttpStatus.BAD_REQUEST, reason);
	}

}
