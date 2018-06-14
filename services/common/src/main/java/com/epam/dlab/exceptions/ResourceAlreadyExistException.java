package com.epam.dlab.exceptions;

public class ResourceAlreadyExistException extends ResourceConflictException {
    public ResourceAlreadyExistException(String message) {
        super(message);
    }
}
