package com.Teletubbies.Apollo.monitor.exception;

import com.Teletubbies.Apollo.core.exception.ApolloException;
import com.Teletubbies.Apollo.core.exception.CustomErrorCode;

public class MonitorException extends ApolloException {
    public MonitorException(CustomErrorCode code, String logMessage) {
        super(code, logMessage);
    }
}
