package ru.javabegin.backend.todo.aop.exception;

public class IncorrectDataException extends RuntimeException{
    public IncorrectDataException(String message) {
        super(message);
    }
}
