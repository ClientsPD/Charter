package com.charter.rewards.exception;

/**
 * @author Prasanna Dupaguntla
 * @created 2/14/26 10:21 AM
 */

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) // This automatically returns 404
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id) {
        super("Customer not found with ID: " + id);
    }
}

