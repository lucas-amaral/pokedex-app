package com.example.tasks.service;

import com.example.tasks.entity.Task;
import com.example.tasks.entity.UserId;

public interface CreateTaskUseCase {

    /** @throws com.example.tasks.entity.InvalidFieldException when a field breaks a rule */
    Task create(UserId owner, NewTask newTask);
}
